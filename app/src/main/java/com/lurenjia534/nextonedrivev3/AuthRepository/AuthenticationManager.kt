// 文件: com.lurenjia534.nextonedrivev2.auth.AuthenticationManager.kt

package com.lurenjia534.nextonedrivev3.AuthRepository

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.lurenjia534.nextonedrivev3.AuthRepository.TokenRefreshWorker
import com.microsoft.identity.client.*
import com.microsoft.identity.client.IMultipleAccountPublicClientApplication
import com.microsoft.identity.client.exception.MsalException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import com.lurenjia534.nextonedrivev3.R
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthenticationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tokenManager: TokenManager,
    private val clientId: String?,
    private val accessTokenState: MutableState<String?>,
    private val isMsalInitializedState: MutableState<Boolean>
) {
    private lateinit var multipleAccountApp: IMultipleAccountPublicClientApplication
    /*
    I must clearly specify the scope of permissions here, as these are the permissions I specified during app registration.
    Please note that the scope of permissions here is an array that includes the permissions I specified during app registration.
    User.Read is a permission I specified during app registration to get basic user information.
    Files.Read.All is a permission I specified during app registration to access the user's OneDrive files.
    LicenseAssignment.Read.All is a permission I specified during app registration to access the user's license assignment information.
    The Home edition cannot access license assignment information, so this scope is only valid for the Business edition.
    Therefore, if you are a Home edition user, you can remove this scope and rebuild.
    The current client only supports self-built applications because MSAL mentioned the developer's SHA1 signature.
    I cannot expose my SHA1, so I can only provide this scope.
    我必须写清楚这里的权限范围，因为这是我在应用注册时指定的权限范围。
    请注意，这里的权限范围是一个数组，包含了我在应用注册时指定的权限范围。
    User.Read 是我在应用注册时指定的权限范围，用于获取用户的基本信息。
    Files.Read.All 是我在应用注册时指定的权限范围，用于获取用户的 OneDrive 文件。
    LincenseAssignment.Read.All 是我在应用注册时指定的权限范围，用于获取用户的许可证分配信息。
    家庭版不能获取许可证分配信息，因此这个权限范围只对商业版有效。所以，如果家庭版用户，可以删除这个权限范围并重新构建。
    当前的客户端仅支持自行构建的应用程序，因为msal提到了开发者签名的sha1,我不能暴漏我的sha1，所以我只能提供这个范围。
    */
    private val scopes = arrayOf(
        "Files.ReadWrite.All",
        "User.Read",
        "Files.Read.All",
    )
    private var firstAccount: IAccount? = null

    @Synchronized
    fun initializeMSAL() {
        if (isMsalInitializedState.value) {
            Log.d("MSAL Init", "MSAL 已经初始化，跳过初始化过程。")
            return
        }
        
        Log.d("MSAL Init", "开始初始化 MSAL。")
        PublicClientApplication.createMultipleAccountPublicClientApplication(
            context,
            R.raw.msal_config,
            object : IPublicClientApplication.IMultipleAccountApplicationCreatedListener {
                override fun onCreated(application: IMultipleAccountPublicClientApplication?) {
                    if (application != null) {
                        multipleAccountApp = application
                        isMsalInitializedState.value = true
                        Log.d("MSAL Init", "MSAL 初始化成功。")
                        loadToken()
                        scheduleTokenRefresh()
                    } else {
                        Log.d("MSAL Init", "MSAL 初始化返回了空实例。")
                    }
                }

                override fun onError(exception: MsalException?) {
                    if (exception != null) {
                        Log.e("MSAL Init Error", "创建 MSAL 应用时出错: ${exception.message}")
                    } else {
                        Log.e("MSAL Init Error", "MSAL 初始化过程中发生未知错误。")
                    }
                }
            }
        )
    }

    private fun loadToken() {
        accessTokenState.value = tokenManager.getAccessToken()
        val accountId = tokenManager.getAccountId()
        if (accessTokenState.value != null && accountId != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    multipleAccountApp.getAccount(accountId)?.let {
                        firstAccount = it
                        Log.d("Token Persistence", "成功加载令牌。")
                    }
                } catch (e: Exception) {
                    Log.e("Token Persistence", "加载账户失败: ${e.message}")
                }
            }
        } else {
            Log.d("Token Persistence", "SharedPreferences 中未找到令牌。")
        }
    }

    fun acquireTokenInteractive(activity: Activity, callback: AuthenticationCallback) {
        if (::multipleAccountApp.isInitialized) {
            Log.d("MSAL AcquireToken", "尝试进行交互式获取令牌。")
            multipleAccountApp.acquireToken(activity, scopes, callback)
        } else {
            Log.e("MSAL AcquireToken", "MSAL 应用尚未初始化。")
        }
    }

    fun acquireTokenSilent(callback: SilentAuthenticationCallback) {
        if (::multipleAccountApp.isInitialized && firstAccount != null) {
            Log.d("MSAL SilentAuth", "尝试静默获取令牌。")
            multipleAccountApp.acquireTokenSilentAsync(
                scopes,
                firstAccount!!,
                multipleAccountApp.configuration.defaultAuthority.authorityURL.toString(),
                callback
            )
        } else {
            Log.e("MSAL SilentAuth", "MSAL 应用未初始化或账户为空。")
        }
    }

    fun scheduleTokenRefresh() {
        // 构建输入数据，包含 clientId
        val inputData = Data.Builder()
            .putString("CLIENT_ID", clientId)
            .build()

        // 改为每45分钟运行一次，避免接近60分钟的令牌有效期
        val workRequest = PeriodicWorkRequestBuilder<TokenRefreshWorker>(
            45, TimeUnit.MINUTES
        )
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "TokenRefreshWorker",
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    fun saveAuthenticationResult(authenticationResult: IAuthenticationResult, accountName: String? = null) {
        accessTokenState.value = authenticationResult.accessToken
        firstAccount = authenticationResult.account
        tokenManager.saveAccessToken(authenticationResult.accessToken)
        tokenManager.saveAccountId(authenticationResult.account.id)
        if (accountName != null) {
            tokenManager.saveAccountName(accountName)
        }
        Log.d("Token Persistence", "令牌已成功保存。")
    }

    // 添加这个新方法，用于获取MSAL实例
    fun getMsalInstance(): IPublicClientApplication? {
        return if (::multipleAccountApp.isInitialized) {
            multipleAccountApp
        } else {
            Log.e("AuthenticationManager", "MSAL尚未初始化")
            null
        }
    }
}
