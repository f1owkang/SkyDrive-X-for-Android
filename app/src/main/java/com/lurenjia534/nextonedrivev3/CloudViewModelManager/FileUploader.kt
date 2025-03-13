package com.lurenjia534.nextonedrivev3.CloudViewModelManager

import android.content.ComponentName
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.lurenjia534.nextonedrivev3.data.model.DriveItem
import com.lurenjia534.nextonedrivev3.data.repository.OneDriveRepository
import com.lurenjia534.nextonedrivev3.notification.UploadNotificationService
import com.lurenjia534.nextonedrivev3.services.FileUploadService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FileUploader(
    override val parentViewModel: ViewModel,
    override val viewModelScope: CoroutineScope,
    private val oneDriveRepository: OneDriveRepository,
    private val uploadNotificationService: UploadNotificationService,
    private val accountManager: AccountManager,
    private val fileNavigator: FileNavigator,
    private val context: Context // 添加Context参数，用于启动服务
) : BaseManager {

    // 上传状态封装类
    sealed class UploadingState {
        object Idle : UploadingState()
        data class Uploading(val progress: Int, val fileName: String, val current: Int, val total: Int) : UploadingState()
        data class Success(val item: DriveItem) : UploadingState()
        data class Error(val message: String) : UploadingState()
    }

    // 创建状态流来跟踪上传进度
    private val _uploadingState = MutableStateFlow<UploadingState>(UploadingState.Idle)
    val uploadingState: StateFlow<UploadingState> = _uploadingState.asStateFlow()

    // 错误信息
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // 上传服务连接
    private var uploadService: FileUploadService? = null
    private var serviceBound = false

    // 服务连接
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as FileUploadService.UploadBinder
            uploadService = binder.getService()
            serviceBound = true
            Log.d("FileUploader", "已连接到上传服务")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            uploadService = null
            serviceBound = false
            Log.d("FileUploader", "与上传服务断开连接")
        }
    }

    init {
        // 绑定服务
        bindUploadService()
    }

    private fun bindUploadService() {
        val intent = Intent(context, FileUploadService::class.java)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    /**
     * 上传文件 - 使用前台服务
     */
    fun uploadFile(contentResolver: ContentResolver, uri: Uri) {
        try {
            val token = accountManager.getCurrentToken() ?: ""
            val currentFolderId = fileNavigator.currentFolderId.value ?: "root"
            
            // 获取文件名和MIME类型
            val fileName = getFileNameFromUri(contentResolver, uri) ?: "file_${System.currentTimeMillis()}"
            val mimeType = getMimeType(contentResolver, uri) ?: "application/octet-stream"
            
            // 获取文件大小
            val fileSize = getFileSizeFromUri(contentResolver, uri)
            
            // 设置初始状态
            _uploadingState.value = UploadingState.Uploading(0, fileName, 1, 1)
            
            // 启动上传服务
            if (serviceBound && uploadService != null) {
                // 使用已绑定的服务
                uploadService?.uploadFile(
                    token = token,
                    parentId = currentFolderId,
                    uri = uri,
                    fileName = fileName,
                    mimeType = mimeType,
                    fileSize = fileSize,
                    onProgress = { progress ->
                        viewModelScope.launch {
                            _uploadingState.value = UploadingState.Uploading(progress, fileName, 1, 1)
                        }
                    },
                    onSuccess = { item ->
                        viewModelScope.launch {
                            _uploadingState.value = UploadingState.Success(item)
                            fileNavigator.refreshCurrentFolder()
                        }
                    },
                    onError = { error ->
                        viewModelScope.launch {
                            val errorMsg = "上传文件失败: $error"
                            _errorMessage.value = errorMsg
                            _uploadingState.value = UploadingState.Error(errorMsg)
                        }
                    }
                )
            } else {
                // 启动新的服务实例
                val intent = Intent(context, FileUploadService::class.java).apply {
                    action = FileUploadService.ACTION_UPLOAD_FILE
                    putExtra(FileUploadService.EXTRA_TOKEN, token)
                    putExtra(FileUploadService.EXTRA_PARENT_ID, currentFolderId)
                    putExtra(FileUploadService.EXTRA_FILE_URI, uri.toString())
                    putExtra(FileUploadService.EXTRA_FILE_NAME, fileName)
                    putExtra(FileUploadService.EXTRA_MIME_TYPE, mimeType)
                    putExtra(FileUploadService.EXTRA_FILE_SIZE, fileSize)
                }
                context.startForegroundService(intent)
                
                // 重新绑定服务以获取后续的进度更新
                if (!serviceBound) {
                    bindUploadService()
                }
            }
        } catch (e: Exception) {
            val errorMsg = "启动上传服务时发生错误: ${e.message}"
            Log.e("FileUploader", errorMsg, e)
            _errorMessage.value = errorMsg
            _uploadingState.value = UploadingState.Error(errorMsg)
        }
    }

    /**
     * 上传多个照片 - 使用前台服务
     */
    fun uploadMultiplePhotos(contentResolver: ContentResolver, uris: List<Uri>) {
        try {
            val token = accountManager.getCurrentToken() ?: ""
            val currentFolderId = fileNavigator.currentFolderId.value ?: "root"
            
            // 准备文件信息列表
            val fileInfoList = uris.mapIndexed { index, uri ->
                val fileName = getFileNameFromUri(contentResolver, uri) ?: "photo_${System.currentTimeMillis()}_$index.jpg"
                val mimeType = getMimeType(contentResolver, uri) ?: "image/jpeg"
                val fileSize = getFileSizeFromUri(contentResolver, uri)
                
                FileUploadService.FileInfo(
                    uri = uri,
                    fileName = fileName,
                    mimeType = mimeType,
                    fileSize = fileSize
                )
            }
            
            // 设置初始状态
            if (fileInfoList.isNotEmpty()) {
                _uploadingState.value = UploadingState.Uploading(0, fileInfoList[0].fileName, 1, fileInfoList.size)
            }
            
            // 启动上传服务
            if (serviceBound && uploadService != null) {
                // 使用已绑定的服务
                uploadService?.uploadMultipleFiles(
                    token = token,
                    parentId = currentFolderId,
                    fileInfoList = fileInfoList,
                    onProgress = { progress, fileName, current, total ->
                        viewModelScope.launch {
                            _uploadingState.value = UploadingState.Uploading(progress, fileName, current, total)
                        }
                    },
                    onAllCompleted = { successCount, failCount, lastItem ->
                        viewModelScope.launch {
                            if (lastItem != null && successCount > 0) {
                                _uploadingState.value = UploadingState.Success(lastItem)
                            } else {
                                _uploadingState.value = UploadingState.Error("上传完成：$successCount 成功，$failCount 失败")
                            }
                            fileNavigator.refreshCurrentFolder()
                        }
                    },
                    onError = { error ->
                        viewModelScope.launch {
                            _errorMessage.value = error
                        }
                    }
                )
            } else {
                // 启动新的服务实例，传递第一个文件，其余文件会在服务中处理
                val intent = Intent(context, FileUploadService::class.java).apply {
                    action = FileUploadService.ACTION_UPLOAD_MULTIPLE_FILES
                    putExtra(FileUploadService.EXTRA_TOKEN, token)
                    putExtra(FileUploadService.EXTRA_PARENT_ID, currentFolderId)
                    putExtra(FileUploadService.EXTRA_FILE_COUNT, fileInfoList.size)
                    
                    // 每个文件的信息都需要单独添加
                    fileInfoList.forEachIndexed { index, fileInfo ->
                        putExtra("${FileUploadService.EXTRA_FILE_URI}_$index", fileInfo.uri.toString())
                        putExtra("${FileUploadService.EXTRA_FILE_NAME}_$index", fileInfo.fileName)
                        putExtra("${FileUploadService.EXTRA_MIME_TYPE}_$index", fileInfo.mimeType)
                        putExtra("${FileUploadService.EXTRA_FILE_SIZE}_$index", fileInfo.fileSize)
                    }
                }
                context.startForegroundService(intent)
                
                // 重新绑定服务以获取后续的进度更新
                if (!serviceBound) {
                    bindUploadService()
                }
            }
        } catch (e: Exception) {
            val errorMsg = "启动批量上传服务时发生错误: ${e.message}"
            Log.e("FileUploader", errorMsg, e)
            _errorMessage.value = errorMsg
            _uploadingState.value = UploadingState.Error(errorMsg)
        }
    }

    /**
     * 上传照片 - 使用前台服务
     */
    fun uploadPhoto(contentResolver: ContentResolver, uri: Uri) {
        // 使用通用的文件上传方法
        uploadFile(contentResolver, uri)
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

    // 工具方法：获取文件大小
    private fun getFileSizeFromUri(contentResolver: ContentResolver, uri: Uri): Long {
        var fileSize: Long = 0

        try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val sizeIndex = cursor.getColumnIndex("_size")
                    if (sizeIndex != -1) {
                        fileSize = cursor.getLong(sizeIndex)
                    }
                }
            }

            // 如果通过ContentResolver无法获取大小，尝试通过打开流获取
            if (fileSize == 0L) {
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    fileSize = inputStream.available().toLong()
                }
            }
        } catch (e: Exception) {
            Log.e("FileUploader", "获取文件大小出错: ${e.message}")
        }

        return fileSize
    }
    
    /**
     * 清理资源，解绑服务
     */
    fun onCleared() {
        if (serviceBound) {
            try {
                context.unbindService(serviceConnection)
                serviceBound = false
            } catch (e: Exception) {
                Log.e("FileUploader", "解绑服务时发生错误: ${e.message}")
            }
        }
    }
}