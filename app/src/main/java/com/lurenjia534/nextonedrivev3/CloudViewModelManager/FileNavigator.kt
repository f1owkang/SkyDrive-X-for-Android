package com.lurenjia534.nextonedrivev3.CloudViewModelManager

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.lurenjia534.nextonedrivev3.data.model.DriveItem
import com.lurenjia534.nextonedrivev3.data.repository.OneDriveRepository
import android.util.Log

class FileNavigator(
    override val parentViewModel: ViewModel,
    override val viewModelScope: CoroutineScope,
    private val oneDriveRepository: OneDriveRepository,
    private val accountManager: AccountManager
) : BaseManager {

    // 加载状态
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // 文件列表
    private val _driveItems = MutableLiveData<List<DriveItem>>()
    val driveItems: LiveData<List<DriveItem>> = _driveItems

    // 当前文件夹路径栈
    private val _currentFolderStack = MutableLiveData<MutableList<DriveItem>>(mutableListOf())
    val currentFolderStack: LiveData<MutableList<DriveItem>> = _currentFolderStack

    // 当前文件夹ID
    private val _currentFolderId = MutableLiveData<String?>(null)
    val currentFolderId: LiveData<String?> = _currentFolderId

    // 错误信息
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // 在移动对话框中浏览的文件夹相关状态
    private val _availableFolders = MutableLiveData<List<DriveItem>>(emptyList())
    val availableFolders: LiveData<List<DriveItem>> = _availableFolders

    private val _loadingFolders = MutableLiveData(false)
    val loadingFolders: LiveData<Boolean> = _loadingFolders

    private val _currentBrowseFolderId = MutableStateFlow<String>("root")
    val currentBrowseFolderId: StateFlow<String> = _currentBrowseFolderId.asStateFlow()

    private val _browsePathStack = MutableStateFlow<MutableList<DriveItem>>(mutableListOf())
    val browsePathStack: StateFlow<MutableList<DriveItem>> = _browsePathStack.asStateFlow()

    /**
     * 加载云盘文件
     */
    fun loadCloudFiles() {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                val token = accountManager.getCurrentToken() ?: ""
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

                    // 检查令牌过期
                    if (error.message?.contains("token is expired") == true ||
                        error.message?.contains("InvalidAuthenticationToken") == true) {
                        Log.d("FileNavigator", "检测到令牌过期，正在尝试刷新...")
                        accountManager.refreshTokenAndRetry { loadCloudFiles() }
                    }
                }

                _isLoading.value = false
            } catch (e: Exception) {
                val errorMsg = "加载云盘内容失败: ${e.message}"
                _errorMessage.value = errorMsg
                _isLoading.value = false

                // 检查异常中是否包含令牌过期信息
                if (e.message?.contains("token is expired") == true ||
                    e.message?.contains("InvalidAuthenticationToken") == true) {
                    Log.d("FileNavigator", "捕获到令牌过期异常，正在尝试刷新...")
                    accountManager.refreshTokenAndRetry { loadCloudFiles() }
                }
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
     * 返回上一级目录
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

        // 重新加载文件列表
        loadCloudFiles()

        return true
    }

    /**
     * 刷新当前文件夹
     */
    fun refreshCurrentFolder() {
        loadCloudFiles()
    }

    // 文件夹浏览相关方法，用于文件移动功能

    /**
     * 加载可用文件夹
     * @param folderId 要加载的文件夹ID，默认为根目录
     */
    fun loadAvailableFolders(folderId: String = "root") {
        viewModelScope.launch {
            try {
                _loadingFolders.value = true

                val token = accountManager.getCurrentToken() ?: ""

                // 根据文件夹ID选择不同的API调用
                val result = if (folderId == "root") {
                    oneDriveRepository.getRootItems(token)
                } else {
                    oneDriveRepository.getFolderItems(token, folderId)
                }

                result.onSuccess { items ->
                    // 过滤出文件夹
                    val folders = items.filter { it.folder != null }
                    _availableFolders.value = folders
                    _loadingFolders.value = false
                }.onFailure { error ->
                    val errorMsg = "加载文件夹失败: ${error.message}"
                    Log.e("FileNavigator", errorMsg)
                    _errorMessage.value = errorMsg
                    _loadingFolders.value = false

                    // 检查是否是token过期问题并处理
                    if (error.message?.contains("token is expired") == true ||
                        error.message?.contains("InvalidAuthenticationToken") == true) {
                        accountManager.refreshTokenAndRetry { loadAvailableFolders(folderId) }
                    }
                }
            } catch (e: Exception) {
                val errorMsg = "加载文件夹时发生错误: ${e.message}"
                Log.e("FileNavigator", errorMsg, e)
                _errorMessage.value = errorMsg
                _loadingFolders.value = false
            }
        }
    }

    /**
     * 在移动对话框中进入子文件夹
     */
    fun enterFolder(folder: DriveItem) {
        // 更新浏览路径栈
        val currentStack = _browsePathStack.value.toMutableList()
        currentStack.add(folder)
        _browsePathStack.value = currentStack

        // 加载子文件夹内容
        _currentBrowseFolderId.value = folder.id
        loadAvailableFolders(folder.id)
    }

    /**
     * 在文件移动对话框中返回上一级文件夹
     */
    fun navigateUpInMoveDialog() {
        val currentStack = _browsePathStack.value.toMutableList()

        if (currentStack.isNotEmpty()) {
            // 移除当前文件夹
            currentStack.removeAt(currentStack.size - 1)
            _browsePathStack.value = currentStack

            // 加载上一级文件夹内容
            val parentId = if (currentStack.isEmpty()) {
                "root"
            } else {
                currentStack.last().id
            }

            _currentBrowseFolderId.value = parentId
            loadAvailableFolders(parentId)
        } else {
            // 已经在根目录，重新加载根目录
            _currentBrowseFolderId.value = "root"
            loadAvailableFolders("root")
        }
    }

    /**
     * 重置移动浏览状态
     */
    fun resetFolderBrowsing() {
        _browsePathStack.value = mutableListOf()
        _currentBrowseFolderId.value = "root"
        _availableFolders.value = emptyList()
    }
}