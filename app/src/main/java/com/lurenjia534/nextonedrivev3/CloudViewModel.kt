package com.lurenjia534.nextonedrivev3

import android.content.Context
import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lurenjia534.nextonedrivev3.AuthRepository.AuthViewModel
import com.lurenjia534.nextonedrivev3.AuthRepository.TokenManager
import com.lurenjia534.nextonedrivev3.data.model.DriveInfo
import com.lurenjia534.nextonedrivev3.data.model.DriveItem
import com.lurenjia534.nextonedrivev3.data.repository.OneDriveRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import android.util.Log
import com.lurenjia534.nextonedrivev3.notification.UploadNotificationService
import kotlinx.coroutines.Dispatchers
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okio.Buffer
import java.io.BufferedInputStream
import java.io.IOException

@HiltViewModel
class CloudViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val oneDriveRepository: OneDriveRepository,
    private val authViewModel: AuthViewModel,
    private val uploadNotificationService: UploadNotificationService,
    private val tokenManager: TokenManager
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

    // 创建状态流来跟踪上传进度
    private val _uploadingState = MutableStateFlow<UploadingState>(UploadingState.Idle)
    val uploadingState: StateFlow<UploadingState> = _uploadingState.asStateFlow()

    // 上传状态封装类
    sealed class UploadingState {
        object Idle : UploadingState()
        data class Uploading(val progress: Int, val fileName: String, val current: Int, val total: Int) : UploadingState()
        data class Success(val item: DriveItem) : UploadingState()
        data class Error(val message: String) : UploadingState()
    }

    // 添加删除状态流
    private val _deletingState = MutableStateFlow<DeletingState>(DeletingState.Idle)
    val deletingState: StateFlow<DeletingState> = _deletingState.asStateFlow()
    
    // 删除状态封装类
    sealed class DeletingState {
        object Idle : DeletingState()
        data class Deleting(val itemName: String) : DeletingState()
        data class Success(val itemName: String) : DeletingState()
        data class Error(val message: String) : DeletingState()
    }

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
                    
                    // 改进令牌过期检测
                    if (error.message?.contains("token is expired") == true || 
                        error.message?.contains("InvalidAuthenticationToken") == true) {
                        Log.d("CloudViewModel", "检测到令牌过期，正在尝试刷新...")
                        refreshTokenAndRetry { loadDriveInfo() }
                    }
                }
                
                _isDriveInfoLoading.value = false
            } catch (e: Exception) {
                val errorMsg = "加载云盘信息失败: ${e.message}"
                _errorMessage.value = errorMsg
                
                // 同样检查异常中是否包含令牌过期信息
                if (e.message?.contains("token is expired") == true || 
                    e.message?.contains("InvalidAuthenticationToken") == true) {
                    Log.d("CloudViewModel", "捕获到令牌过期异常，正在尝试刷新...")
                    refreshTokenAndRetry { loadDriveInfo() }
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
                    
                    // 改进令牌过期检测
                    if (error.message?.contains("token is expired") == true || 
                        error.message?.contains("InvalidAuthenticationToken") == true) {
                        Log.d("CloudViewModel", "检测到令牌过期，正在尝试刷新...")
                        refreshTokenAndRetry { loadCloudFiles() }
                    }
                }
                
                _isLoading.value = false
            } catch (e: Exception) {
                val errorMsg = "加载云盘内容失败: ${e.message}"
                _errorMessage.value = errorMsg
                _isLoading.value = false
                
                // 同样检查异常中是否包含令牌过期信息
                if (e.message?.contains("token is expired") == true || 
                    e.message?.contains("InvalidAuthenticationToken") == true) {
                    Log.d("CloudViewModel", "捕获到令牌过期异常，正在尝试刷新...")
                    refreshTokenAndRetry { loadCloudFiles() }
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

    /**
     * 上传照片 - 添加更多日志
     */
    fun uploadPhoto(contentResolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            try {
                _uploadingState.value = UploadingState.Uploading(0, "", 0, 0)
                
                val token = _accountToken.value ?: ""
                val currentFolderId = _currentFolderId.value ?: "root"
                
                // 添加日志
                Log.d("CloudViewModel", "开始上传照片，当前文件夹ID: $currentFolderId")
                
                // 获取文件名和MIME类型
                val fileName = getFileNameFromUri(contentResolver, uri) ?: "photo_${System.currentTimeMillis()}.jpg"
                val mimeType = getMimeType(contentResolver, uri) ?: "image/jpeg"
                
                Log.d("CloudViewModel", "文件名: $fileName, MIME类型: $mimeType")
                
                // 打开输入流
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    Log.d("CloudViewModel", "成功打开输入流，准备上传")
                    
                    val result = oneDriveRepository.uploadPhoto(
                        token = token,
                        parentId = currentFolderId,
                        fileName = fileName,
                        inputStream = inputStream,
                        mimeType = mimeType
                    )
                    
                    result.onSuccess { item ->
                        Log.d("CloudViewModel", "上传成功: ${item.name}")
                        _uploadingState.value = UploadingState.Success(item)
                        // 刷新文件列表
                        refreshCurrentFolder()
                    }.onFailure { error ->
                        val errorMsg = "上传照片失败: ${error.message}"
                        Log.e("CloudViewModel", errorMsg)
                        _errorMessage.value = errorMsg
                        _uploadingState.value = UploadingState.Error(errorMsg)
                        
                        // 检查是否是token过期问题并处理
                        authViewModel.checkAndHandleTokenExpiration(error.message) {
                            _accountToken.value = authViewModel.accessTokenState.value
                        }
                    }
                } ?: run {
                    val errorMsg = "无法读取照片文件"
                    Log.e("CloudViewModel", errorMsg)
                    _errorMessage.value = errorMsg
                    _uploadingState.value = UploadingState.Error(errorMsg)
                }
            } catch (e: Exception) {
                val errorMsg = "上传照片时发生错误: ${e.message}"
                Log.e("CloudViewModel", errorMsg, e)
                _errorMessage.value = errorMsg
                _uploadingState.value = UploadingState.Error(errorMsg)
            }
        }
    }

    /**
     * 创建文件夹
     */
    fun createFolder(folderName: String) {
        if (folderName.isBlank()) {
            _errorMessage.value = "文件夹名称不能为空"
            return
        }
        
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                val token = _accountToken.value ?: ""
                val currentFolderId = _currentFolderId.value ?: "root"
                
                val result = oneDriveRepository.createFolder(
                    token = token,
                    parentId = currentFolderId,
                    folderName = folderName
                )
                
                result.onSuccess { item ->
                    _errorMessage.value = null
                    // 刷新文件列表
                    refreshCurrentFolder()
                }.onFailure { error ->
                    val errorMsg = "创建文件夹失败: ${error.message}"
                    _errorMessage.value = errorMsg
                    
                    // 检查是否是token过期问题并处理
                    authViewModel.checkAndHandleTokenExpiration(error.message) {
                        _accountToken.value = authViewModel.accessTokenState.value
                    }
                }
                
                _isLoading.value = false
            } catch (e: Exception) {
                val errorMsg = "创建文件夹时发生错误: ${e.message}"
                _errorMessage.value = errorMsg
                _isLoading.value = false
            }
        }
    }

    // 工具方法：从Uri获取文件名
    private fun getFileNameFromUri(contentResolver: ContentResolver, uri: Uri): String? {
        var fileName: String? = null
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val displayNameIndex = cursor.getColumnIndex("_display_name")
                if (displayNameIndex != -1) {
                    fileName = cursor.getString(displayNameIndex)
                }
            }
        }
        return fileName
    }

    // 工具方法：获取MIME类型
    private fun getMimeType(contentResolver: ContentResolver, uri: Uri): String? {
        return contentResolver.getType(uri)
    }

    // 添加文件监听进度的请求体实现
    private inner class ProgressRequestBody(
        private val content: ByteArray,
        private val contentType: String,
        private val fileName: String,
        private val currentFileIndex: Int = 1,
        private val totalFiles: Int = 1
    ) : RequestBody() {
        override fun contentType() = contentType.toMediaTypeOrNull()
        
        override fun contentLength() = content.size.toLong()
        
        override fun writeTo(sink: okio.BufferedSink) {
            var uploadedBytes = 0L
            val totalBytes = contentLength()
            val buffer = Buffer()
            val chunk = 4096L // 每次写入4KB
            
            var offset = 0
            while (offset < content.size) {
                val chunkSize = minOf(chunk.toInt(), content.size - offset)
                buffer.write(content, offset, chunkSize)
                sink.write(buffer, chunkSize.toLong())
                
                offset += chunkSize
                uploadedBytes += chunkSize
                
                // 计算进度0-100
                val progress = ((uploadedBytes.toFloat() / totalBytes) * 100).toInt()
                
                // 更新UI和通知
                viewModelScope.launch(Dispatchers.Main) {
                    _uploadingState.value = UploadingState.Uploading(
                        progress, fileName, currentFileIndex, totalFiles
                    )
                    
                    val notificationText = if (totalFiles > 1) {
                        "$fileName (${currentFileIndex}/${totalFiles})"
                    } else {
                        fileName
                    }
                    
                    uploadNotificationService.showUploadProgressNotification(notificationText, progress)
                }
                
                buffer.clear()
            }
        }
    }
    
    // 修改uploadFile方法使用进度监听
    fun uploadFile(contentResolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            try {
                val token = _accountToken.value ?: ""
                val currentFolderId = _currentFolderId.value ?: "root"
                
                // 获取文件名和MIME类型
                val fileName = getFileNameFromUri(contentResolver, uri) ?: "file_${System.currentTimeMillis()}"
                val mimeType = getMimeType(contentResolver, uri) ?: "application/octet-stream"
                
                // 设置初始状态
                _uploadingState.value = UploadingState.Uploading(0, fileName, 0, 0)
                uploadNotificationService.showUploadProgressNotification(fileName, 0)
                
                // 读取文件内容以便使用自定义请求体
                val inputStream = contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val bytes = inputStream.readBytes()
                    inputStream.close()
                    
                    // 使用自定义请求体上传
                    val result = oneDriveRepository.uploadFileWithProgress(
                        token = token,
                        parentId = currentFolderId,
                        fileName = fileName,
                        fileContent = ProgressRequestBody(bytes, mimeType, fileName),
                        mimeType = mimeType
                    )
                    
                    result.onSuccess { item ->
                        Log.d("CloudViewModel", "上传成功: ${item.name}")
                        _uploadingState.value = UploadingState.Success(item)
                        uploadNotificationService.completeUploadNotification(fileName, true)
                        refreshCurrentFolder()
                    }.onFailure { error ->
                        val errorMsg = "上传文件失败: ${error.message}"
                        Log.e("CloudViewModel", errorMsg)
                        _errorMessage.value = errorMsg
                        _uploadingState.value = UploadingState.Error(errorMsg)
                        uploadNotificationService.completeUploadNotification(fileName, false)
                    }
                } else {
                    val errorMsg = "无法读取文件"
                    _errorMessage.value = errorMsg
                    _uploadingState.value = UploadingState.Error(errorMsg)
                    uploadNotificationService.completeUploadNotification(fileName, false)
                }
            } catch (e: Exception) {
                val errorMsg = "上传文件时发生错误: ${e.message}"
                Log.e("CloudViewModel", errorMsg, e)
                _errorMessage.value = errorMsg
                _uploadingState.value = UploadingState.Error(errorMsg)
                uploadNotificationService.completeUploadNotification("文件", false)
            }
        }
    }

    // 添加多文件上传函数
    fun uploadMultiplePhotos(contentResolver: ContentResolver, uris: List<Uri>) {
        viewModelScope.launch {
            try {
                val token = _accountToken.value ?: ""
                val currentFolderId = _currentFolderId.value ?: "root"
                
                // 总文件数
                val totalFiles = uris.size
                
                // 记录上传成功的文件数
                var successCount = 0
                var failCount = 0
                
                // 遍历每个URI进行上传
                uris.forEachIndexed { index, uri ->
                    try {
                        // 获取文件名和MIME类型
                        val fileName = getFileNameFromUri(contentResolver, uri) ?: "photo_${System.currentTimeMillis()}_$index.jpg"
                        val mimeType = getMimeType(contentResolver, uri) ?: "image/jpeg"
                        
                        // 设置初始上传状态
                        _uploadingState.value = UploadingState.Uploading(0, fileName, index + 1, totalFiles)
                        uploadNotificationService.showUploadProgressNotification(
                            "$fileName (${index + 1}/$totalFiles)", 0
                        )
                        
                        // 读取文件内容
                        contentResolver.openInputStream(uri)?.use { inputStream ->
                            val bytes = inputStream.readBytes()
                            
                            // 使用自定义请求体上传
                            val result = oneDriveRepository.uploadFileWithProgress(
                                token = token,
                                parentId = currentFolderId,
                                fileName = fileName,
                                fileContent = ProgressRequestBody(bytes, mimeType, fileName, index + 1, totalFiles),
                                mimeType = mimeType
                            )
                            
                            result.onSuccess {
                                successCount++
                                Log.d("CloudViewModel", "上传成功 ($successCount/$totalFiles): ${it.name}")
                                
                                // 最后一个文件上传成功后更新UI
                                if (index == uris.size - 1) {
                                    _uploadingState.value = UploadingState.Success(it)
                                    uploadNotificationService.completeUploadNotification(
                                        "完成上传 $successCount 个文件，失败 $failCount 个", true
                                    )
                                    refreshCurrentFolder()
                                }
                            }.onFailure { error ->
                                failCount++
                                val errorMsg = "上传失败 (${index + 1}/$totalFiles): ${error.message}"
                                Log.e("CloudViewModel", errorMsg)
                                _errorMessage.value = errorMsg
                                
                                // 如果是最后一个文件，无论成功失败都更新UI
                                if (index == uris.size - 1) {
                                    _uploadingState.value = UploadingState.Error(
                                        "上传完成：$successCount 成功，$failCount 失败"
                                    )
                                    uploadNotificationService.completeUploadNotification(
                                        "完成上传 $successCount 个文件，失败 $failCount 个", 
                                        successCount > 0
                                    )
                                    refreshCurrentFolder()
                                }
                            }
                        } ?: run {
                            failCount++
                            Log.e("CloudViewModel", "无法读取图片文件 (${index + 1}/$totalFiles)")
                        }
                    } catch (e: Exception) {
                        failCount++
                        Log.e("CloudViewModel", "处理图片出错 (${index + 1}/$totalFiles): ${e.message}")
                    }
                }
                
            } catch (e: Exception) {
                val errorMsg = "批量上传图片时发生错误: ${e.message}"
                Log.e("CloudViewModel", errorMsg, e)
                _errorMessage.value = errorMsg
                _uploadingState.value = UploadingState.Error(errorMsg)
                uploadNotificationService.completeUploadNotification("批量上传失败", false)
            }
        }
    }

    // 添加一个更可靠的令牌刷新和重试函数
    private fun refreshTokenAndRetry(retryAction: () -> Unit) {
        viewModelScope.launch {
            try {
                Log.d("CloudViewModel", "正在刷新令牌...")
                
                // 获取当前账户ID
                val accountId = tokenManager.getAccountId() ?: return@launch
                
                // 直接使用AuthViewModel的手动刷新方法
                authViewModel.refreshTokenManually(accountId) { success ->
                    if (success) {
                        Log.d("CloudViewModel", "令牌刷新成功，正在重试操作...")
                        // 更新本地令牌
                        _accountToken.value = authViewModel.accessTokenState.value
                        // 执行重试操作
                        retryAction()
                    } else {
                        Log.e("CloudViewModel", "令牌刷新失败")
                        _errorMessage.value = "令牌刷新失败，请尝试重新登录"
                    }
                }
            } catch (e: Exception) {
                Log.e("CloudViewModel", "刷新令牌过程中发生错误: ${e.message}")
                _errorMessage.value = "令牌刷新出错: ${e.message}"
            }
        }
    }

    /**
     * 删除文件或文件夹
     */
    fun deleteItem(item: DriveItem) {
        viewModelScope.launch {
            try {
                _deletingState.value = DeletingState.Deleting(item.name)
                
                val token = _accountToken.value ?: ""
                
                val result = oneDriveRepository.deleteItem(
                    token = token,
                    itemId = item.id
                )
                
                result.onSuccess {
                    Log.d("CloudViewModel", "删除成功: ${item.name}")
                    _deletingState.value = DeletingState.Success(item.name)
                    // 刷新文件列表
                    refreshCurrentFolder()
                }.onFailure { error ->
                    val errorMsg = "删除失败: ${error.message}"
                    Log.e("CloudViewModel", errorMsg)
                    _errorMessage.value = errorMsg
                    _deletingState.value = DeletingState.Error(errorMsg)
                    
                    // 检查是否是token过期问题并处理
                    if (error.message?.contains("token is expired") == true || 
                        error.message?.contains("InvalidAuthenticationToken") == true) {
                        refreshTokenAndRetry { deleteItem(item) }
                    }
                }
            } catch (e: Exception) {
                val errorMsg = "删除时发生错误: ${e.message}"
                Log.e("CloudViewModel", errorMsg, e)
                _errorMessage.value = errorMsg
                _deletingState.value = DeletingState.Error(errorMsg)
            }
        }
    }
} 