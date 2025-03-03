package com.lurenjia534.nextonedrivev3.AuthRepository

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.lurenjia534.nextonedrivev2.AuthRepository.AuthenticationCallbackProvider
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.exception.MsalException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// 账户数据类
data class AccountInfo(
    val id: String,
    val name: String,
    val token: String
)

// 添加一个数据类来表示认证消息
data class AuthMessage(
    val message: String,
    val isError: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authenticationManager: AuthenticationManager,
    val accessTokenState: MutableState<String?>,
    val isMsalInitializedState: MutableState<Boolean>,
    private val tokenManager: TokenManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // 保存所有账户的LiveData
    private val _accounts = MutableLiveData<List<AccountInfo>>(emptyList())
    val accounts: LiveData<List<AccountInfo>> = _accounts
    
    // 当前正在认证的账户名称
    private var currentAuthAccount: String? = null

    // 添加一个LiveData来传递认证消息
    private val _authMessage = MutableLiveData<AuthMessage>()
    val authMessage: LiveData<AuthMessage> = _authMessage

    // 深色模式状态
    private val _isDarkMode = MutableLiveData<Boolean>()
    val isDarkMode: LiveData<Boolean> = _isDarkMode

    init {
        authenticationManager.initializeMSAL()
        // 加载已保存的账户
        loadSavedAccounts()
        
        // 加载深色模式偏好设置
        loadDarkModePreference()
    }

    private fun loadSavedAccounts() {
        // 首先尝试加载多账户信息
        val accounts = tokenManager.getMultipleAccounts()
        if (accounts.isNotEmpty()) {
            _accounts.value = accounts
            return
        }
        
        // 向后兼容：如果没有多账户信息，尝试加载单个账户
        val accountId = tokenManager.getAccountId()
        val token = tokenManager.getAccessToken()
        if (accountId != null && token != null) {
            val name = tokenManager.getAccountName() ?: "我的OneDrive" 
            _accounts.value = listOf(AccountInfo(accountId, name, token))
        }
    }

    fun initiateAuthFlow(activity: Activity, accountName: String) {
        currentAuthAccount = accountName
        
        // 检查MSAL是否已初始化
        if (!isMsalInitializedState.value) {
            Log.d("AuthViewModel", "MSAL未初始化，正在初始化...")
            authenticationManager.initializeMSAL()
            // 给MSAL初始化一些时间
            CoroutineScope(Dispatchers.Main).launch {
                delay(500) // 等待500毫秒
                proceedWithAuth(activity)
            }
        } else {
            proceedWithAuth(activity)
        }
    }

    fun updateAccount(account: AccountInfo) {
        val currentAccounts = _accounts.value ?: emptyList()
        val updatedAccounts = currentAccounts.map {
            if (it.id == account.id) account else it
        }
        _accounts.value = updatedAccounts
        tokenManager.saveMultipleAccounts(updatedAccounts)

        // 如果更新的是当前活跃账户，也更新单账户存储
        if (tokenManager.getAccountId() == account.id) {
            tokenManager.saveAccountName(account.name)
        }

        // 发送成功消息
        _authMessage.value = AuthMessage(
            message = "账户\"${account.name}\"已更新",
            isError = false
        )
    }

    private fun proceedWithAuth(activity: Activity) {
        // 新添加的账户直接进行交互式认证
        Log.d("AuthViewModel", "开始交互式认证流程...")
        acquireTokenInteractive(activity)
    }
    
    private fun acquireTokenInteractive(activity: Activity) {
        val callback = object : AuthenticationCallback {
            override fun onSuccess(authenticationResult: IAuthenticationResult) {
                // 使用当前账户名保存认证结果
                currentAuthAccount?.let { accountName ->
                    authenticationManager.saveAuthenticationResult(authenticationResult, accountName)
                } ?: run {
                    authenticationManager.saveAuthenticationResult(authenticationResult)
                }
                
                accessTokenState.value = authenticationResult.accessToken
                
                // 保存新账户
                currentAuthAccount?.let { accountName ->
                    val newAccount = AccountInfo(
                        id = authenticationResult.account.id,
                        name = accountName,
                        token = authenticationResult.accessToken
                    )
                    addNewAccount(newAccount)
                    
                    // 确保UI能立即更新
                    CoroutineScope(Dispatchers.Main).launch {
                        _accounts.value = _accounts.value  // 触发LiveData的更新
                    }
                }
                
                Log.d("MSAL Auth", "成功获取令牌: ${authenticationResult.accessToken}")
                
                // 添加成功消息
                _authMessage.value = AuthMessage(
                    message = "账户\"${currentAuthAccount ?: "新账户"}\"认证成功",
                    isError = false
                )
            }

            override fun onError(exception: MsalException) {
                Log.e("MSAL Auth Error", "认证失败: ${exception.message}")
                
                // 添加错误消息
                _authMessage.value = AuthMessage(
                    message = "认证失败: ${exception.localizedMessage ?: "未知错误"}",
                    isError = true
                )
            }

            override fun onCancel() {
                Log.d("MSAL Auth", "用户取消了认证")
                
                // 添加取消消息
                _authMessage.value = AuthMessage(
                    message = "认证已取消",
                    isError = true
                )
            }
        }

        authenticationManager.acquireTokenInteractive(activity, callback)
    }
    
    private fun acquireTokenSilent(activity: Activity, callbackProvider: AuthenticationCallbackProvider) {
        try {
            authenticationManager.acquireTokenSilent(callbackProvider.getSilentAuthCallback())
        } catch (e: Exception) {
            Log.e("AuthViewModel", "静默获取令牌失败，尝试交互式认证", e)
            acquireTokenInteractive(activity)
        }
    }
    
    private fun addNewAccount(accountInfo: AccountInfo) {
        val currentAccounts = _accounts.value ?: emptyList()
        // 检查账户是否已存在，如果已存在则更新
        val updatedAccounts = if (currentAccounts.any { it.id == accountInfo.id }) {
            currentAccounts.map { 
                if (it.id == accountInfo.id) accountInfo else it 
            }
        } else {
            currentAccounts + accountInfo
        }
        _accounts.value = updatedAccounts
        
        // 保存到TokenManager
        tokenManager.saveAccountId(accountInfo.id)
        tokenManager.saveAccessToken(accountInfo.token)
        tokenManager.saveAccountName(accountInfo.name)
        
        // 同时保存多账户信息
        tokenManager.saveMultipleAccounts(updatedAccounts)
        
        // 强制UI刷新
        _accounts.postValue(updatedAccounts)
    }

    // 可以添加一个方法来清除消息
    fun clearAuthMessage() {
        _authMessage.value = null
    }

    /**
     * 加载深色模式偏好设置
     */
    private fun loadDarkModePreference() {
        val sharedPreferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        _isDarkMode.value = sharedPreferences.getBoolean("dark_mode", false)
    }
    
    /**
     * 更新深色模式偏好设置
     */
    fun updateDarkMode(isDarkMode: Boolean) {
        _isDarkMode.value = isDarkMode
        
        // 保存到SharedPreferences
        val sharedPreferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        sharedPreferences.edit().putBoolean("dark_mode", isDarkMode).apply()
        
        // 发送深色模式已更新的消息
        _authMessage.value = AuthMessage(
            message = if (isDarkMode) "已切换到深色模式" else "已切换到浅色模式",
            isError = false
        )
    }
} 