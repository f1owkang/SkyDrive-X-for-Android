package com.lurenjia534.nextonedrivev3.CloudViewModelManager

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.lurenjia534.nextonedrivev3.data.model.DriveItem
import com.lurenjia534.nextonedrivev3.data.repository.OneDriveRepository
import com.lurenjia534.nextonedrivev3.notification.UploadNotificationService
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
    private val fileNavigator: FileNavigator
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

    /**
     * 上传文件
     */
    fun uploadFile(contentResolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
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
                uploadNotificationService.showUploadProgressNotification(fileName, 0)

                // 使用智能上传方法
                val inputStream = contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val result = oneDriveRepository.smartUploadFile(
                        token = token,
                        parentId = currentFolderId,
                        fileName = fileName,
                        inputStream = inputStream,
                        contentType = mimeType,
                        fileSize = fileSize,
                        onProgress = { progress ->
                            // 更新上传进度
                            _uploadingState.value = UploadingState.Uploading(progress, fileName, 1, 1)
                            uploadNotificationService.showUploadProgressNotification(fileName, progress)
                        }
                    )

                    result.onSuccess { item ->
                        Log.d("FileUploader", "上传成功: ${item.name}")
                        _uploadingState.value = UploadingState.Success(item)
                        uploadNotificationService.completeUploadNotification(fileName, true)
                        fileNavigator.refreshCurrentFolder()
                    }.onFailure { error ->
                        val errorMsg = "上传文件失败: ${error.message}"
                        Log.e("FileUploader", errorMsg)
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
                Log.e("FileUploader", errorMsg, e)
                _errorMessage.value = errorMsg
                _uploadingState.value = UploadingState.Error(errorMsg)
                uploadNotificationService.completeUploadNotification("文件", false)
            }
        }
    }

    /**
     * 上传照片
     */
    fun uploadPhoto(contentResolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            try {
                _uploadingState.value = UploadingState.Uploading(0, "", 0, 0)

                val token = accountManager.getCurrentToken() ?: ""
                val currentFolderId = fileNavigator.currentFolderId.value ?: "root"

                // 添加日志
                Log.d("FileUploader", "开始上传照片，当前文件夹ID: $currentFolderId")

                // 获取文件名和MIME类型
                val fileName = getFileNameFromUri(contentResolver, uri) ?: "photo_${System.currentTimeMillis()}.jpg"
                val mimeType = getMimeType(contentResolver, uri) ?: "image/jpeg"

                Log.d("FileUploader", "文件名: $fileName, MIME类型: $mimeType")

                // 打开输入流
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    Log.d("FileUploader", "成功打开输入流，准备上传")

                    val result = oneDriveRepository.uploadPhoto(
                        token = token,
                        parentId = currentFolderId,
                        fileName = fileName,
                        inputStream = inputStream,
                        mimeType = mimeType
                    )

                    result.onSuccess { item ->
                        Log.d("FileUploader", "上传成功: ${item.name}")
                        _uploadingState.value = UploadingState.Success(item)
                        // 刷新文件列表
                        fileNavigator.refreshCurrentFolder()
                    }.onFailure { error ->
                        val errorMsg = "上传照片失败: ${error.message}"
                        Log.e("FileUploader", errorMsg)
                        _errorMessage.value = errorMsg
                        _uploadingState.value = UploadingState.Error(errorMsg)
                    }
                } ?: run {
                    val errorMsg = "无法读取照片文件"
                    Log.e("FileUploader", errorMsg)
                    _errorMessage.value = errorMsg
                    _uploadingState.value = UploadingState.Error(errorMsg)
                }
            } catch (e: Exception) {
                val errorMsg = "上传照片时发生错误: ${e.message}"
                Log.e("FileUploader", errorMsg, e)
                _errorMessage.value = errorMsg
                _uploadingState.value = UploadingState.Error(errorMsg)
            }
        }
    }

    /**
     * 上传多个照片
     */
    fun uploadMultiplePhotos(contentResolver: ContentResolver, uris: List<Uri>) {
        viewModelScope.launch {
            try {
                val token = accountManager.getCurrentToken() ?: ""
                val currentFolderId = fileNavigator.currentFolderId.value ?: "root"

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

                        // 获取文件大小
                        val fileSize = getFileSizeFromUri(contentResolver, uri)

                        // 设置初始上传状态
                        _uploadingState.value = UploadingState.Uploading(0, fileName, index + 1, totalFiles)
                        uploadNotificationService.showUploadProgressNotification(
                            "$fileName (${index + 1}/$totalFiles)", 0
                        )

                        // 读取文件内容
                        contentResolver.openInputStream(uri)?.use { inputStream ->
                            // 使用智能上传方法
                            val result = oneDriveRepository.smartUploadFile(
                                token = token,
                                parentId = currentFolderId,
                                fileName = fileName,
                                inputStream = inputStream,
                                contentType = mimeType,
                                fileSize = fileSize,
                                onProgress = { progress ->
                                    // 更新上传进度
                                    _uploadingState.value = UploadingState.Uploading(progress, fileName, index + 1, totalFiles)
                                    uploadNotificationService.showUploadProgressNotification(
                                        "$fileName (${index + 1}/$totalFiles)", progress
                                    )
                                }
                            )

                            result.onSuccess {
                                successCount++
                                Log.d("FileUploader", "上传成功 ($successCount/$totalFiles): ${it.name}")

                                // 最后一个文件上传成功后更新UI
                                if (index == uris.size - 1) {
                                    _uploadingState.value = UploadingState.Success(it)
                                    uploadNotificationService.completeUploadNotification(
                                        "完成上传 $successCount 个文件，失败 $failCount 个", true
                                    )
                                    fileNavigator.refreshCurrentFolder()
                                }
                            }.onFailure { error ->
                                failCount++
                                val errorMsg = "上传失败 (${index + 1}/$totalFiles): ${error.message}"
                                Log.e("FileUploader", errorMsg)
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
                                    fileNavigator.refreshCurrentFolder()
                                }
                            }
                        } ?: run {
                            failCount++
                            Log.e("FileUploader", "无法读取图片文件 (${index + 1}/$totalFiles)")
                        }
                    } catch (e: Exception) {
                        failCount++
                        Log.e("FileUploader", "处理图片出错 (${index + 1}/$totalFiles): ${e.message}")
                    }
                }

            } catch (e: Exception) {
                val errorMsg = "批量上传图片时发生错误: ${e.message}"
                Log.e("FileUploader", errorMsg, e)
                _errorMessage.value = errorMsg
                _uploadingState.value = UploadingState.Error(errorMsg)
                uploadNotificationService.completeUploadNotification("批量上传失败", false)
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
}