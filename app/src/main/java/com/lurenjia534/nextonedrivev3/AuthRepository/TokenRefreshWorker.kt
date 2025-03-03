// 文件: com.lurenjia534.nextonedrivev2.workers.TokenRefreshWorker.kt

package com.lurenjia534.nextonedrivev2.workers

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.lurenjia534.nextonedrivev3.AuthRepository.TokenManager
import com.lurenjia534.nextonedrivev3.AuthRepository.AuthenticationManager
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.SilentAuthenticationCallback
import com.microsoft.identity.client.exception.MsalException

class TokenRefreshWorker(
    context: Context, 
    params: WorkerParameters,
    private val tokenManager: TokenManager
) : Worker(context, params) {

    override fun doWork(): Result {
        Log.d("TokenRefreshWorker", "执行静默令牌刷新")

        // 从输入数据中获取 clientId
        val clientId = inputData.getString("CLIENT_ID")
        if (clientId.isNullOrEmpty()) {
            Log.e("TokenRefreshWorker", "未找到 clientId，无法刷新令牌。")
            return Result.failure()
        }

        // 初始化 AuthenticationManager
        val authenticationManager = AuthenticationManager(
            context = applicationContext,
            tokenManager = tokenManager,
            clientId = clientId,
            accessTokenState = mutableStateOf(tokenManager.getAccessToken()),
            isMsalInitializedState = mutableStateOf(false) // Worker 中不需要 UI 状态
        )

        // 初始化 MSAL
        authenticationManager.initializeMSAL()

        // 获取账户 ID
        val accountId = tokenManager.getAccountId()
        if (accountId != null) {
            // 执行静默获取令牌
            authenticationManager.acquireTokenSilent(object : SilentAuthenticationCallback {
                override fun onSuccess(authenticationResult: IAuthenticationResult) {
                    tokenManager.saveAccessToken(authenticationResult.accessToken)
                    Log.d("TokenRefreshWorker", "刷新后的 Access Token: ${authenticationResult.accessToken}")
                }

                override fun onError(exception: MsalException) {
                    Log.e("TokenRefreshWorker", "刷新令牌时出错: ${exception.message}")
                }
            })
        } else {
            Log.e("TokenRefreshWorker", "未找到账户 ID，无法刷新令牌。")
            return Result.failure()
        }

        return Result.success()
    }
}
