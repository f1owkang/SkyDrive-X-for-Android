package com.lurenjia534.nextonedrivev3

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lurenjia534.nextonedrivev3.AuthRepository.AuthViewModel
import com.lurenjia534.nextonedrivev3.data.model.DriveInfo
import com.lurenjia534.nextonedrivev3.data.model.DriveItem
import com.lurenjia534.nextonedrivev3.data.repository.OneDriveRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CloudViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val oneDriveRepository: OneDriveRepository,
    private val authViewModel: AuthViewModel  // 注入 AuthViewModel
) : ViewModel() {

    // 账户信息
    private val _accountId = MutableLiveData<String>()
    val accountId: LiveData<String> = _accountId
    
    private val _accountName = MutableLiveData<String>()
    val accountName: LiveData<String> = _accountName
    
    private val _accountToken = MutableLiveData<String>()
    val accountToken: LiveData<String> = _accountToken
    
    // 加载状态
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    // 错误信息
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // 深色模式状态
    private val _isDarkMode = MutableLiveData<Boolean>()
    val isDarkMode: LiveData<Boolean> = _isDarkMode
    
    // 文件列表
    private val _driveItems = MutableLiveData<List<DriveItem>>()
    val driveItems: LiveData<List<DriveItem>> = _driveItems
    
    // 当前文件夹路径栈
    private val _currentFolderStack = MutableLiveData<MutableList<DriveItem>>(mutableListOf())
    val currentFolderStack: LiveData<MutableList<DriveItem>> = _currentFolderStack
    
    // 当前文件夹ID
    private val _currentFolderId = MutableLiveData<String?>(null)
    val currentFolderId: LiveData<String?> = _currentFolderId
    
    // 云盘信息
    private val _driveInfo = MutableLiveData<DriveInfo?>()
    val driveInfo: LiveData<DriveInfo?> = _driveInfo
    
    // 云盘信息加载状态
    private val _isDriveInfoLoading = MutableLiveData<Boolean>(false)
    val isDriveInfoLoading: LiveData<Boolean> = _isDriveInfoLoading

    init {
        // 加载深色模式偏好设置
        loadDarkModePreference()
    }

    /**
     * 初始化账户信息
     */
    fun initializeAccount(id: String, name: String, token: String) {
        _accountId.value = id
        _accountName.value = name
        _accountToken.value = token
        
        // 获取云盘数据
        loadCloudFiles()
        
        // 获取云盘信息
        loadDriveInfo()
    }
    
    /**
     * 加载云盘信息
     */
    private fun loadDriveInfo() {
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
                    
                    // 检查是否是 token 过期问题并处理
                    authViewModel.checkAndHandleTokenExpiration(error.message) {
                        // token 刷新成功后重新加载
                        _accountToken.value = authViewModel.accessTokenState.value
                        loadDriveInfo()
                    }
                }
                
                _isDriveInfoLoading.value = false
            } catch (e: Exception) {
                val errorMsg = "加载云盘信息失败: ${e.message}"
                _errorMessage.value = errorMsg
                
                // 检查是否是 token 过期问题并处理
                authViewModel.checkAndHandleTokenExpiration(e.message) {
                    // token 刷新成功后重新加载
                    _accountToken.value = authViewModel.accessTokenState.value
                    loadDriveInfo()
                }
                
                _isDriveInfoLoading.value = false
            }
        }
    }
    
    /**
     * 加载云盘文件
     */
    private fun loadCloudFiles() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                val token = _accountToken.value ?: ""
                val folderId = _currentFolderId.value
                
                val result = if (folderId == null) {
                    // 加载根目录
                    oneDriveRepository.getRootItems(token)
                } else {
                    // 加载指定文件夹
                    oneDriveRepository.getFolderItems(token, folderId)
                }
                
                result.onSuccess { items ->
                    _driveItems.value = items
                    _errorMessage.value = null
                }.onFailure { error ->
                    val errorMsg = "加载云盘内容失败: ${error.message}"
                    _errorMessage.value = errorMsg
                    
                    // 检查是否是 token 过期问题并处理
                    authViewModel.checkAndHandleTokenExpiration(error.message) {
                        // token 刷新成功后重新加载
                        _accountToken.value = authViewModel.accessTokenState.value
                        loadCloudFiles()
                    }
                }
                
                _isLoading.value = false
            } catch (e: Exception) {
                val errorMsg = "加载云盘内容失败: ${e.message}"
                _errorMessage.value = errorMsg
                
                // 检查是否是 token 过期问题并处理
                authViewModel.checkAndHandleTokenExpiration(e.message) {
                    // token 刷新成功后重新加载
                    _accountToken.value = authViewModel.accessTokenState.value
                    loadCloudFiles()
                }
                
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 打开文件夹
     */
    fun openFolder(folder: DriveItem) {
        if (!folder.isFolder) return
        
        val currentStack = _currentFolderStack.value ?: mutableListOf()
        currentStack.add(folder)
        _currentFolderStack.value = currentStack
        _currentFolderId.value = folder.id
        
        loadCloudFiles()
    }
    
    /**
     * 返回上一级文件夹
     * @return 是否有上级文件夹可返回
     */
    fun navigateUp(): Boolean {
        val currentStack = _currentFolderStack.value ?: mutableListOf()
        if (currentStack.isEmpty()) {
            return false
        }
        
        // 移除当前文件夹
        currentStack.removeAt(currentStack.size - 1)
        _currentFolderStack.value = currentStack
        
        // 更新当前文件夹ID
        _currentFolderId.value = if (currentStack.isEmpty()) {
            null // 回到根目录
        } else {
            currentStack.last().id
        }
        
        loadCloudFiles()
        return true
    }

    /**
     * 加载深色模式偏好设置
     */
    private fun loadDarkModePreference() {
        val sharedPreferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        _isDarkMode.value = sharedPreferences.getBoolean("dark_mode", false)
    }
    
    /**
     * 刷新当前文件夹
     */
    fun refreshCurrentFolder() {
        loadCloudFiles()
    }
    
    /**
     * 刷新云盘信息
     */
    fun refreshDriveInfo() {
        loadDriveInfo()
    }
} 