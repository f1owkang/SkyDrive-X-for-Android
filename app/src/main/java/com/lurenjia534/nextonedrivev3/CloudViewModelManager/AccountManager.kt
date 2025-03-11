package com.lurenjia534.nextonedrivev3.CloudViewModelManager

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.lurenjia534.nextonedrivev3.AuthRepository.AuthViewModel
import com.lurenjia534.nextonedrivev3.AuthRepository.TokenManager
import com.lurenjia534.nextonedrivev3.data.model.DriveInfo
import com.lurenjia534.nextonedrivev3.data.repository.OneDriveRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import android.util.Log

class AccountManager(
    override val parentViewModel: ViewModel,
    override val viewModelScope: CoroutineScope,
    private val oneDriveRepository: OneDriveRepository,
    private val authViewModel: AuthViewModel,
    private val tokenManager: TokenManager
) : BaseManager {

    // 账户信息
    private val _accountId = MutableLiveData<String>()
    val accountId: LiveData<String> = _accountId

    private val _accountName = MutableLiveData<String>()
    val accountName: LiveData<String> = _accountName

    private val _accountToken = MutableLiveData<String>()
    val accountToken: LiveData<String> = _accountToken

    // 云盘信息
    private val _driveInfo = MutableLiveData<DriveInfo?>()
    val driveInfo: LiveData<DriveInfo?> = _driveInfo

    // 云盘信息加载状态
    private val _isDriveInfoLoading = MutableLiveData<Boolean>(false)
    val isDriveInfoLoading: LiveData<Boolean> = _isDriveInfoLoading

    // 错误信息
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    /**
     * 初始化账户信息
     */
    fun initializeAccount(id: String, name: String, token: String, onInitialized: () -> Unit) {
        _accountId.value = id
        _accountName.value = name
        _accountToken.value = token

        // 获取云盘信息
        loadDriveInfo()

        // 回调通知初始化完成
        onInitialized()
    }

    /**
     * 获取当前令牌
     */
    fun getCurrentToken(): String? {
        return _accountToken.value
    }

    /**
     * 加载云盘信息
     */
    fun loadDriveInfo() {
        viewModelScope.launch {
            try {
                _isDriveInfoLoading.value = true

                val token = _accountToken.value ?: ""
                val result = oneDriveRepository.getDriveInfo(token)

                result.onSuccess { info ->
                    _driveInfo.value = info
                    _errorMessage.value = null
                }.onFailure { error ->
                    val errorMsg = "加载云盘信息失败: ${error.message}"
                    _errorMessage.value = errorMsg

                    // 检查令牌过期
                    if (error.message?.contains("token is expired") == true ||
                        error.message?.contains("InvalidAuthenticationToken") == true) {
                        Log.d("AccountManager", "检测到令牌过期，正在尝试刷新...")
                        refreshTokenAndRetry { loadDriveInfo() }
                    }
                }

                _isDriveInfoLoading.value = false
            } catch (e: Exception) {
                val errorMsg = "加载云盘信息失败: ${e.message}"
                _errorMessage.value = errorMsg

                // 检查异常中是否包含令牌过期信息
                if (e.message?.contains("token is expired") == true ||
                    e.message?.contains("InvalidAuthenticationToken") == true) {
                    Log.d("AccountManager", "捕获到令牌过期异常，正在尝试刷新...")
                    refreshTokenAndRetry { loadDriveInfo() }
                }

                _isDriveInfoLoading.value = false
            }
        }
    }

    /**
     * 刷新令牌并重试操作
     */
    fun refreshTokenAndRetry(retryAction: () -> Unit) {
        viewModelScope.launch {
            try {
                Log.d("AccountManager", "正在刷新令牌...")

                // 获取当前账户ID
                val accountId = tokenManager.getAccountId() ?: return@launch

                // 直接使用AuthViewModel的手动刷新方法
                authViewModel.refreshTokenManually(accountId) { success ->
                    if (success) {
                        Log.d("AccountManager", "令牌刷新成功，正在重试操作...")
                        // 更新本地令牌
                        _accountToken.value = authViewModel.accessTokenState.value
                        // 执行重试操作
                        retryAction()
                    } else {
                        Log.e("AccountManager", "令牌刷新失败")
                        _errorMessage.value = "令牌刷新失败，请尝试重新登录"
                    }
                }
            } catch (e: Exception) {
                Log.e("AccountManager", "刷新令牌过程中发生错误: ${e.message}")
                _errorMessage.value = "令牌刷新出错: ${e.message}"
            }
        }
    }
}