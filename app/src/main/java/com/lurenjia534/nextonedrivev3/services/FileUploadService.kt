package com.lurenjia534.nextonedrivev3.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.lurenjia534.nextonedrivev3.R
import com.lurenjia534.nextonedrivev3.data.model.DriveItem
import com.lurenjia534.nextonedrivev3.data.repository.OneDriveRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import androidx.core.net.toUri

/**
 * 文件上传前台服务
 * 
 * 负责在后台继续处理文件上传，即使应用程序不在前台
 */
@AndroidEntryPoint
class FileUploadService : Service() {

    companion object {
        private const val TAG = "FileUploadService"
        
        // 通知相关
        private const val NOTIFICATION_CHANNEL_ID = "file_upload_channel"
        private const val NOTIFICATION_ID = 1001
        
        // Intent动作
        const val ACTION_UPLOAD_FILE = "com.lurenjia534.nextonedrivev3.action.UPLOAD_FILE"
        const val ACTION_UPLOAD_MULTIPLE_FILES = "com.lurenjia534.nextonedrivev3.action.UPLOAD_MULTIPLE_FILES"
        
        // Intent额外数据键
        const val EXTRA_TOKEN = "extra_token"
        const val EXTRA_PARENT_ID = "extra_parent_id"
        const val EXTRA_FILE_URI = "extra_file_uri"
        const val EXTRA_FILE_NAME = "extra_file_name"
        const val EXTRA_MIME_TYPE = "extra_mime_type"
        const val EXTRA_FILE_SIZE = "extra_file_size"
        const val EXTRA_FILE_COUNT = "extra_file_count"
    }
    
    // 文件信息类
    data class FileInfo(
        val uri: Uri,
        val fileName: String,
        val mimeType: String,
        val fileSize: Long
    )
    
    // 绑定器
    inner class UploadBinder : Binder() {
        fun getService(): FileUploadService = this@FileUploadService
    }
    
    private val binder = UploadBinder()
    
    // 协程作用域和任务
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private val uploadJobs = ConcurrentHashMap<String, Job>()
    
    // 注入仓库
    @Inject
    lateinit var oneDriveRepository: OneDriveRepository
    
    // 活跃的上传任务数
    private var activeUploads = 0
    
    // 通知管理器
    private lateinit var notificationManager: NotificationManager
    private lateinit var notificationBuilder: NotificationCompat.Builder
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "上传服务已创建")
        
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        
        // 创建基本通知
        notificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("文件上传")
            .setContentText("准备上传...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
        
        // 开始前台服务，添加服务类型
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14及以上，使用带类型的startForeground
            startForeground(
                NOTIFICATION_ID, 
                notificationBuilder.build(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            // Android 13及以下，使用普通的startForeground
            startForeground(NOTIFICATION_ID, notificationBuilder.build())
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "收到命令: ${intent?.action}")
        
        when (intent?.action) {
            ACTION_UPLOAD_FILE -> {
                intent.getStringExtra(EXTRA_TOKEN)?.let { token ->
                    val parentId = intent.getStringExtra(EXTRA_PARENT_ID) ?: "root"
                    val uriString = intent.getStringExtra(EXTRA_FILE_URI)
                    val fileName = intent.getStringExtra(EXTRA_FILE_NAME) ?: "file_${System.currentTimeMillis()}"
                    val mimeType = intent.getStringExtra(EXTRA_MIME_TYPE) ?: "application/octet-stream"
                    val fileSize = intent.getLongExtra(EXTRA_FILE_SIZE, 0)
                    
                    if (uriString != null) {
                        try {
                            val uri = uriString.toUri()
                            uploadFileForeground(
                                token = token,
                                parentId = parentId,
                                uri = uri,
                                fileName = fileName,
                                mimeType = mimeType,
                                fileSize = fileSize
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "解析URI失败: $uriString", e)
                        }
                    }
                }
            }
            ACTION_UPLOAD_MULTIPLE_FILES -> {
                intent.getStringExtra(EXTRA_TOKEN)?.let { token ->
                    val parentId = intent.getStringExtra(EXTRA_PARENT_ID) ?: "root"
                    val fileCount = intent.getIntExtra(EXTRA_FILE_COUNT, 0)
                    
                    if (fileCount > 0) {
                        val fileInfoList = ArrayList<FileInfo>(fileCount)
                        
                        // 从Intent中获取所有文件信息
                        for (i in 0 until fileCount) {
                            val uriString = intent.getStringExtra("${EXTRA_FILE_URI}_$i") ?: continue
                            val fileName = intent.getStringExtra("${EXTRA_FILE_NAME}_$i") ?: "file_${System.currentTimeMillis()}_$i"
                            val mimeType = intent.getStringExtra("${EXTRA_MIME_TYPE}_$i") ?: "application/octet-stream"
                            val fileSize = intent.getLongExtra("${EXTRA_FILE_SIZE}_$i", 0)
                            
                            try {
                                val uri = uriString.toUri()
                                fileInfoList.add(
                                    FileInfo(
                                        uri = uri,
                                        fileName = fileName,
                                        mimeType = mimeType,
                                        fileSize = fileSize
                                    )
                                )
                            } catch (e: Exception) {
                                Log.e(TAG, "解析URI失败: $uriString", e)
                            }
                        }
                        
                        if (fileInfoList.isNotEmpty()) {
                            uploadMultipleFilesForeground(
                                token = token,
                                parentId = parentId,
                                fileInfoList = fileInfoList
                            )
                        }
                    }
                }
            }
        }
        
        // 如果服务被系统杀死，重新启动
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder {
        return binder
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "上传服务销毁")
        
        // 取消所有上传任务
        uploadJobs.values.forEach { it.cancel() }
        uploadJobs.clear()
        
        // 取消协程作用域
        serviceScope.cancel()
    }
    
    /**
     * 创建通知渠道
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "文件上传服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "显示文件上传进度"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * 更新上传通知
     */
    private fun updateNotification(title: String, content: String, progress: Int = -1) {
        val notification = notificationBuilder.apply {
            setContentTitle(title)
            setContentText(content)
            if (progress >= 0) {
                setProgress(100, progress, false)
            } else {
                setProgress(0, 0, false)
            }
        }.build()
        
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    /**
     * 前台上传单个文件
     */
    fun uploadFile(
        token: String,
        parentId: String,
        uri: Uri,
        fileName: String,
        mimeType: String,
        fileSize: Long,
        onProgress: (Int) -> Unit = {},
        onSuccess: (DriveItem) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val uploadId = "upload_${System.currentTimeMillis()}_${fileName.hashCode()}"
        activeUploads++
        
        val job = serviceScope.launch {
            try {
                // 打开文件流
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    // 使用智能上传方法
                    val result = oneDriveRepository.smartUploadFile(
                        token = token,
                        parentId = parentId,
                        fileName = fileName,
                        inputStream = inputStream,
                        contentType = mimeType,
                        fileSize = fileSize,
                        onProgress = { progress ->
                            if (isActive) {
                                // 更新进度
                                onProgress(progress)
                                updateNotification("正在上传", fileName, progress)
                            }
                        }
                    )
                    
                    if (isActive) {
                        result.onSuccess { item ->
                            Log.d(TAG, "上传成功: ${item.name}")
                            onSuccess(item)
                            updateNotification("上传完成", "${fileName} 上传成功")
                        }.onFailure { error ->
                            val errorMsg = "上传文件失败: ${error.message}"
                            Log.e(TAG, errorMsg)
                            onError(errorMsg)
                            updateNotification("上传失败", errorMsg)
                        }
                    }
                } ?: run {
                    val errorMsg = "无法读取文件: $fileName"
                    Log.e(TAG, errorMsg)
                    onError(errorMsg)
                    updateNotification("上传失败", errorMsg)
                }
            } catch (e: Exception) {
                if (isActive) {
                    val errorMsg = "上传文件时发生错误: ${e.message}"
                    Log.e(TAG, errorMsg, e)
                    onError(errorMsg)
                    updateNotification("上传失败", errorMsg)
                }
            } finally {
                uploadJobs.remove(uploadId)
                activeUploads--
                
                // 如果没有活跃的上传任务，停止服务
                if (activeUploads <= 0 && uploadJobs.isEmpty()) {
                    stopSelf()
                }
            }
        }
        
        uploadJobs[uploadId] = job
    }
    
    /**
     * 前台上传多个文件
     */
    fun uploadMultipleFiles(
        token: String,
        parentId: String,
        fileInfoList: List<FileInfo>,
        onProgress: (Int, String, Int, Int) -> Unit = { _, _, _, _ -> },
        onAllCompleted: (Int, Int, DriveItem?) -> Unit = { _, _, _ -> },
        onError: (String) -> Unit = {}
    ) {
        if (fileInfoList.isEmpty()) return
        
        val groupId = "group_${System.currentTimeMillis()}"
        activeUploads += fileInfoList.size
        
        // 记录上传结果
        var successCount = 0
        var failCount = 0
        var lastSuccessItem: DriveItem? = null
        
        // 遍历每个文件进行上传
        fileInfoList.forEachIndexed { index, fileInfo ->
            val uploadId = "${groupId}_${index}"
            
            val job = serviceScope.launch {
                try {
                    // 打开文件流
                    contentResolver.openInputStream(fileInfo.uri)?.use { inputStream ->
                        // 更新通知
                        updateNotification(
                            "正在上传 (${index + 1}/${fileInfoList.size})",
                            fileInfo.fileName,
                            0
                        )
                        
                        // 使用智能上传方法
                        val result = oneDriveRepository.smartUploadFile(
                            token = token,
                            parentId = parentId,
                            fileName = fileInfo.fileName,
                            inputStream = inputStream,
                            contentType = fileInfo.mimeType,
                            fileSize = fileInfo.fileSize,
                            onProgress = { progress ->
                                if (isActive) {
                                    // 更新进度
                                    onProgress(progress, fileInfo.fileName, index + 1, fileInfoList.size)
                                    updateNotification(
                                        "正在上传 (${index + 1}/${fileInfoList.size})",
                                        fileInfo.fileName,
                                        progress
                                    )
                                }
                            }
                        )
                        
                        if (isActive) {
                            result.onSuccess { item ->
                                Log.d(TAG, "上传成功 (${index + 1}/${fileInfoList.size}): ${item.name}")
                                successCount++
                                lastSuccessItem = item
                                
                                // 如果是最后一个文件，调用完成回调
                                if (index == fileInfoList.size - 1 || successCount + failCount == fileInfoList.size) {
                                    onAllCompleted(successCount, failCount, lastSuccessItem)
                                    updateNotification(
                                        "上传完成",
                                        "已完成 $successCount/${fileInfoList.size} 个文件"
                                    )
                                }
                            }.onFailure { error ->
                                val errorMsg = "上传失败 (${index + 1}/${fileInfoList.size}): ${error.message}"
                                Log.e(TAG, errorMsg)
                                failCount++
                                onError(errorMsg)
                                
                                // 如果是最后一个文件，调用完成回调
                                if (index == fileInfoList.size - 1 || successCount + failCount == fileInfoList.size) {
                                    onAllCompleted(successCount, failCount, lastSuccessItem)
                                    updateNotification(
                                        "上传完成",
                                        "已完成 $successCount/${fileInfoList.size} 个文件，失败 $failCount 个"
                                    )
                                }
                            }
                        }
                    } ?: run {
                        val errorMsg = "无法读取文件: ${fileInfo.fileName}"
                        Log.e(TAG, errorMsg)
                        failCount++
                        onError(errorMsg)
                    }
                } catch (e: Exception) {
                    if (isActive) {
                        val errorMsg = "上传文件时发生错误: ${e.message}"
                        Log.e(TAG, errorMsg, e)
                        failCount++
                        onError(errorMsg)
                    }
                } finally {
                    uploadJobs.remove(uploadId)
                    activeUploads--
                    
                    // 如果没有活跃的上传任务，停止服务
                    if (activeUploads <= 0 && uploadJobs.isEmpty()) {
                        stopSelf()
                    }
                }
            }
            
            uploadJobs[uploadId] = job
        }
    }
    
    /**
     * 前台服务方式上传单个文件（通过startForegroundService调用）
     */
    private fun uploadFileForeground(
        token: String,
        parentId: String,
        uri: Uri,
        fileName: String,
        mimeType: String,
        fileSize: Long
    ) {
        uploadFile(
            token = token,
            parentId = parentId,
            uri = uri,
            fileName = fileName,
            mimeType = mimeType,
            fileSize = fileSize,
            onProgress = { /* 进度更新只在服务内处理 */ },
            onSuccess = { /* 成功后服务内处理 */ },
            onError = { /* 错误在服务内处理 */ }
        )
    }
    
    /**
     * 前台服务方式上传多个文件（通过startForegroundService调用）
     */
    private fun uploadMultipleFilesForeground(
        token: String,
        parentId: String,
        fileInfoList: List<FileInfo>
    ) {
        uploadMultipleFiles(
            token = token,
            parentId = parentId,
            fileInfoList = fileInfoList,
            onProgress = { _, _, _, _ -> /* 进度更新只在服务内处理 */ },
            onAllCompleted = { _, _, _ -> /* 完成后服务内处理 */ },
            onError = { /* 错误在服务内处理 */ }
        )
    }
} 