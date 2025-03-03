// 文件: com.lurenjia534.nextonedrivev3.AuthRepository.TokenRefreshWorker.kt

package com.lurenjia534.nextonedrivev3.AuthRepository

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lurenjia534.nextonedrivev3.AuthRepository.AccountInfo
import com.lurenjia534.nextonedrivev3.AuthRepository.TokenManager
import com.lurenjia534.nextonedrivev3.AuthRepository.AuthenticationManager
import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.IMultipleAccountPublicClientApplication
import com.microsoft.identity.client.SilentAuthenticationCallback
import com.microsoft.identity.client.exception.MsalException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

class TokenRefreshWorker(
    context: Context,
    params: WorkerParameters,
    private val tokenManager: TokenManager
) : CoroutineWorker(context, params) {

    private val SCOPES = arrayOf("User.Read", "Files.Read.All", "LicenseAssignment.Read.All")

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d("TokenRefreshWorker", "执行令牌刷新任务")

        // 从输入数据中获取 clientId
        val clientId = inputData.getString("CLIENT_ID")
        if (clientId.isNullOrEmpty()) {
            Log.e("TokenRefreshWorker", "未找到 clientId，无法刷新令牌")
            return@withContext Result.failure()
        }

        // 初始化 AuthenticationManager
        val authenticationManager = AuthenticationManager(
            context = applicationContext,
            tokenManager = tokenManager,
            clientId = clientId,
            accessTokenState = mutableStateOf(tokenManager.getAccessToken()),
            isMsalInitializedState = mutableStateOf(false)
        )

        // 初始化 MSAL
        authenticationManager.initializeMSAL()

        // 获取所有保存的账户
        val savedAccounts = tokenManager.getMultipleAccounts()
        if (savedAccounts.isEmpty()) {
            // 如果没有多账户信息，则尝试刷新单个账户
            val accountId = tokenManager.getAccountId()
            if (accountId != null) {
                val success = refreshAccountToken(authenticationManager, accountId)
                return@withContext if (success) Result.success() else Result.retry()
            } else {
                Log.e("TokenRefreshWorker", "未找到任何账户信息，无法刷新令牌")
                return@withContext Result.failure()
            }
        }

        // 刷新所有账户的令牌
        var allSuccess = true
        for (account in savedAccounts) {
            val success = refreshAccountToken(authenticationManager, account.id)
            if (!success) allSuccess = false
        }

        return@withContext if (allSuccess) Result.success() else Result.retry()
    }

    private suspend fun refreshAccountToken(authenticationManager: AuthenticationManager, accountId: String): Boolean {
        return suspendCancellableCoroutine { continuation ->
            try {
                // 获取MSAL实例
                val msalApp = authenticationManager.getMsalInstance() as? IMultipleAccountPublicClientApplication

                if (msalApp == null) {
                    Log.e("TokenRefreshWorker", "MSAL实例不可用或未初始化")
                    continuation.resume(false) {
                        Log.d("TokenRefreshWorker", "令牌刷新协程已取消")
                    }
                    return@suspendCancellableCoroutine
                }

                // 获取账户
                msalApp.getAccount(accountId, object : IMultipleAccountPublicClientApplication.GetAccountCallback {
                    override fun onTaskCompleted(account: IAccount?) {
                        if (account == null) {
                            Log.e("TokenRefreshWorker", "找不到账户ID: $accountId")
                            continuation.resume(false) {
                                Log.d("TokenRefreshWorker", "令牌刷新协程已取消")
                            }
                            return
                        }

                        // 尝试静默刷新令牌
                        msalApp.acquireTokenSilentAsync(
                            SCOPES,
                            account,
                            msalApp.configuration.defaultAuthority.authorityURL.toString(),
                            object : SilentAuthenticationCallback {
                                override fun onSuccess(authenticationResult: IAuthenticationResult) {
                                    // 更新令牌
                                    val newToken = authenticationResult.accessToken
                                    Log.d("TokenRefreshWorker", "成功刷新账户 ${account.id} 的令牌")

                                    // 更新多账户存储
                                    updateTokenInMultiAccountStorage(accountId, newToken)

                                    // 如果是当前活跃账户，也更新单账户存储
                                    if (accountId == tokenManager.getAccountId()) {
                                        tokenManager.saveAccessToken(newToken)
                                    }

                                    continuation.resume(true) {
                                        Log.d("TokenRefreshWorker", "令牌刷新协程已取消")
                                    }
                                }

                                override fun onError(exception: MsalException) {
                                    Log.e("TokenRefreshWorker", "刷新令牌时出错: ${exception.message}")
                                    continuation.resume(false) {
                                        Log.e("TokenRefreshWorker", "令牌刷新失败，协程已取消: ${exception.message}")
                                    }
                                }
                            }
                        )
                    }

                    override fun onError(exception: MsalException?) {
                        Log.e("TokenRefreshWorker", "获取账户时出错: ${exception?.message}")
                        continuation.resume(false) {
                            Log.e("TokenRefreshWorker", "获取账户失败，协程已取消: ${exception?.message}")
                        }
                    }
                })
            } catch (e: Exception) {
                Log.e("TokenRefreshWorker", "刷新令牌过程中发生异常: ${e.message}")
                continuation.resume(false) {
                    Log.e("TokenRefreshWorker", "令牌刷新异常，协程已取消: ${e.message}")
                }
            }
        }
    }

    private fun updateTokenInMultiAccountStorage(accountId: String, newToken: String) {
        val accounts = tokenManager.getMultipleAccounts()
        val updatedAccounts = accounts.map { account ->
            if (account.id == accountId) {
                AccountInfo(id = accountId, name = account.name, token = newToken)
            } else {
                account
            }
        }
        tokenManager.saveMultipleAccounts(updatedAccounts)
    }
}
