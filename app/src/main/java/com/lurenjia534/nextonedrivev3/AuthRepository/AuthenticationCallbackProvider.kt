// 文件 com.lurenjia534.nextonedrivev2.AuthRepository.AuthenticationCallbackProvider.kt

package com.lurenjia534.nextonedrivev3.AuthRepository

import android.util.Log
import androidx.compose.runtime.MutableState
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.SilentAuthenticationCallback
import com.microsoft.identity.client.exception.MsalClientException
import com.microsoft.identity.client.exception.MsalException
import com.microsoft.identity.client.exception.MsalServiceException
import javax.inject.Inject

class AuthenticationCallbackProvider @Inject constructor(
    private val authenticationManager: AuthenticationManager,
    private val accessTokenState: MutableState<String?>,
    private val isMsalInitializedState: MutableState<Boolean>,
    private val onInteractiveRequest: () -> Unit
) {
    fun getAuthInteractiveCallback(): AuthenticationCallback {
        return object : AuthenticationCallback {
            override fun onSuccess(authenticationResult: IAuthenticationResult) {
                authenticationManager.saveAuthenticationResult(authenticationResult)
                accessTokenState.value = authenticationResult.accessToken
                Log.d("MSAL Auth", "获取到的 Access Token: ${authenticationResult.accessToken}")
                Log.d("时间", "${authenticationResult.expiresOn}")
            }

            override fun onError(exception: MsalException) {
                Log.e("MSAL Auth Error", "认证失败: ${exception.message}")
                // 根据异常类型进行处理
                when (exception) {
                    is MsalClientException -> {
                        Log.e("MSAL Auth Error", "客户端错误: ${exception.message}")
                        isMsalInitializedState.value = false
                        accessTokenState.value = null
                    }
                    is MsalServiceException -> {
                        Log.e("MSAL Auth Error", "服务错误: ${exception.message}")
                        isMsalInitializedState.value = false
                        accessTokenState.value = null
                    }
                    else -> {
                        Log.e("MSAL Auth Error", "其他错误: ${exception.message}")
                        isMsalInitializedState.value = false
                        accessTokenState.value = null
                    }
                }
            }

            override fun onCancel() {
                Log.d("MSAL Auth", "用户取消了认证。")
                // 清空 accessTokenState 或更新状态
                accessTokenState.value = null
            }
        }
    }

    fun getSilentAuthCallback(): SilentAuthenticationCallback {
        return object : SilentAuthenticationCallback {
            override fun onSuccess(authenticationResult: IAuthenticationResult) {
                authenticationManager.saveAuthenticationResult(authenticationResult)
                accessTokenState.value = authenticationResult.accessToken
                Log.d("MSAL SilentAuth", "新的 Access Token: ${authenticationResult.accessToken}")
            }

            override fun onError(exception: MsalException) {
                Log.e("MSAL SilentAuth Error", "静默获取令牌时出错: ${exception.message}")
                // 根据异常类型进行处理
                when (exception) {
                    is MsalClientException -> {
                        Log.e("MSAL SilentAuth Error", "客户端错误: ${exception.message}")
                        isMsalInitializedState.value = false
                        accessTokenState.value = null
                    }
                    is MsalServiceException -> {
                        Log.e("MSAL SilentAuth Error", "服务错误: ${exception.message}")
                        isMsalInitializedState.value = false
                        accessTokenState.value = null
                    }
                    else -> {
                        Log.e("MSAL SilentAuth Error", "其他错误: ${exception.message}")
                        isMsalInitializedState.value = false
                        accessTokenState.value = null
                    }
                }
                // 可选：在此处理重新认证的逻辑
                onInteractiveRequest()  // 调用回调函数，触发交互式认证
            }
        }
    }
}