package com.lurenjia534.nextonedrivev3.data.repository

import android.content.ContentResolver
import android.net.Uri
import com.lurenjia534.nextonedrivev3.data.api.OneDriveService
import com.lurenjia534.nextonedrivev3.data.model.DriveInfo
import com.lurenjia534.nextonedrivev3.data.model.DriveItem
import com.lurenjia534.nextonedrivev3.data.model.CreateLinkRequest
import com.lurenjia534.nextonedrivev3.data.model.Permission
import com.lurenjia534.nextonedrivev3.data.model.UploadSessionRequest
import com.lurenjia534.nextonedrivev3.data.model.UploadItemProperties
import com.lurenjia534.nextonedrivev3.data.model.UploadSessionResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream

@Singleton
class OneDriveRepository @Inject constructor(
    private val oneDriveService: OneDriveService
) {
    suspend fun getRootItems(token: String): Result<List<DriveItem>> = withContext(Dispatchers.IO) {
        try {
            val authToken = "Bearer $token"
            val response = oneDriveService.getRootItems(authToken)
            
            if (response.isSuccessful) {
                val items = response.body()?.items ?: emptyList()
                Result.success(items)
            } else {
                Result.failure(Exception("请求失败：${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getFolderItems(token: String, itemId: String): Result<List<DriveItem>> = withContext(Dispatchers.IO) {
        try {
            val authToken = "Bearer $token"
            val response = oneDriveService.getFolderItems(authToken, itemId)
            
            if (response.isSuccessful) {
                val items = response.body()?.items ?: emptyList()
                Result.success(items)
            } else {
                Result.failure(Exception("请求失败：${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getItemsByPath(token: String, path: String): Result<List<DriveItem>> = withContext(Dispatchers.IO) {
        try {
            val authToken = "Bearer $token"
            val response = oneDriveService.getItemsByPath(authToken, path)
            
            if (response.isSuccessful) {
                val items = response.body()?.items ?: emptyList()
                Result.success(items)
            } else {
                Result.failure(Exception("请求失败：${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getDriveInfo(token: String): Result<DriveInfo> = withContext(Dispatchers.IO) {
        try {
            val authToken = "Bearer $token"
            val response = oneDriveService.getDriveInfo(authToken)
            
            if (response.isSuccessful) {
                val driveInfo = response.body()
                if (driveInfo != null) {
                    Result.success(driveInfo)
                } else {
                    Result.failure(Exception("返回数据为空"))
                }
            } else {
                Result.failure(Exception("请求失败：${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 上传图片文件 - 修复API路径问题
     */
    suspend fun uploadPhoto(
        token: String,
        parentId: String,
        fileName: String,
        inputStream: InputStream,
        mimeType: String
    ): Result<DriveItem> = withContext(Dispatchers.IO) {
        try {
            val authToken = "Bearer $token"
            
            // 读取文件数据
            val bytes = inputStream.readBytes()
            val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
            
            // 根据parentId决定使用哪个API方法
            val response = if (parentId == "root") {
                // 如果是根目录，使用简化的路径
                oneDriveService.uploadFileToRoot(
                    authToken = authToken,
                    filename = fileName,
                    fileContent = requestBody,
                    conflictBehavior = "rename"
                )
            } else {
                // 否则使用文件夹ID
                oneDriveService.uploadNewFile(
                    authToken = authToken,
                    parentId = parentId,
                    filename = fileName,
                    fileContent = requestBody,
                    conflictBehavior = "rename"
                )
            }
            
            if (response.isSuccessful) {
                val item = response.body()
                if (item != null) {
                    Result.success(item)
                } else {
                    Result.failure(Exception("上传成功但返回数据为空"))
                }
            } else {
                // 增强错误信息
                val errorBody = response.errorBody()?.string() ?: ""
                val errorCode = when (response.code()) {
                    403 -> "权限不足"
                    404 -> "找不到指定的文件夹（ID可能无效）"
                    else -> response.code().toString()
                }
                Result.failure(Exception("上传失败($errorCode): ${response.message()}\n详情:$errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 创建文件夹
     */
    suspend fun createFolder(
        token: String, 
        parentId: String, 
        folderName: String
    ): Result<DriveItem> = withContext(Dispatchers.IO) {
        try {
            val authToken = "Bearer $token"
            
            // 创建文件夹的JSON请求体
            val jsonObject = JSONObject().apply {
                put("name", folderName)
                put("folder", JSONObject())
                put("@microsoft.graph.conflictBehavior", "rename")
            }
            
            val requestBody = jsonObject.toString()
                .toRequestBody("application/json".toMediaTypeOrNull())
            
            val response = oneDriveService.createFolder(
                authToken = authToken,
                parentId = parentId,
                folderInfo = requestBody
            )
            
            if (response.isSuccessful) {
                val item = response.body()
                if (item != null) {
                    Result.success(item)
                } else {
                    Result.failure(Exception("创建文件夹成功但返回数据为空"))
                }
            } else {
                // 增强错误信息
                val errorBody = response.errorBody()?.string() ?: ""
                val errorCode = if (response.code() == 403) "权限不足" else response.code().toString()
                Result.failure(Exception("创建文件夹失败($errorCode): ${response.message()}\n详情:$errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // 添加带进度的上传方法
    suspend fun uploadFileWithProgress(
        token: String,
        parentId: String,
        fileName: String,
        fileContent: RequestBody,
        mimeType: String
    ): Result<DriveItem> = withContext(Dispatchers.IO) {
        try {
            val authToken = "Bearer $token"
            
            // 根据parentId决定使用哪个API方法
            val response = if (parentId == "root") {
                oneDriveService.uploadFileToRoot(
                    authToken = authToken,
                    filename = fileName,
                    fileContent = fileContent,
                    conflictBehavior = "rename"
                )
            } else {
                oneDriveService.uploadNewFile(
                    authToken = authToken,
                    parentId = parentId,
                    filename = fileName,
                    fileContent = fileContent,
                    conflictBehavior = "rename"
                )
            }
            
            if (response.isSuccessful) {
                val item = response.body()
                if (item != null) {
                    Result.success(item)
                } else {
                    Result.failure(Exception("上传成功但返回数据为空"))
                }
            } else {
                // 增强错误信息
                val errorBody = response.errorBody()?.string() ?: ""
                val errorCode = when (response.code()) {
                    403 -> "权限不足"
                    404 -> "找不到指定的文件夹（ID可能无效）"
                    else -> response.code().toString()
                }
                Result.failure(Exception("上传失败($errorCode): ${response.message()}\n详情:$errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 删除文件或文件夹（移至回收站）
     */
    suspend fun deleteItem(
        token: String,
        itemId: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val authToken = "Bearer $token"
            
            val response = oneDriveService.deleteItem(
                authToken = authToken,
                itemId = itemId
            )
            
            if (response.isSuccessful) {
                // 204 No Content 表示删除成功
                Result.success(true)
            } else {
                // 增强错误信息
                val errorBody = response.errorBody()?.string() ?: ""
                val errorCode = when (response.code()) {
                    403 -> "权限不足"
                    404 -> "找不到指定的文件或文件夹"
                    412 -> "文件已被修改，刷新后重试"
                    else -> response.code().toString()
                }
                Result.failure(Exception("删除失败($errorCode): ${response.message()}\n详情:$errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 创建共享链接
     * @param token 访问令牌
     * @param itemId 要共享的文件或文件夹ID
     * @param linkType 链接类型，可以是"view"(只读),"edit"(可编辑)或"embed"(嵌入)
     * @param scope 链接范围，可以是"anonymous"(匿名)或"organization"(组织内)
     */
    suspend fun createShareLink(
        token: String,
        itemId: String,
        linkType: String = "view",
        scope: String = "anonymous"
    ): Result<Permission> = withContext(Dispatchers.IO) {
        try {
            val authToken = "Bearer $token"
            
            val linkRequest = CreateLinkRequest(
                type = linkType,
                scope = scope
            )
            
            val response = oneDriveService.createShareLink(
                authToken = authToken,
                itemId = itemId,
                linkRequest = linkRequest
            )
            
            if (response.isSuccessful) {
                val permission = response.body()
                if (permission != null) {
                    Result.success(permission)
                } else {
                    Result.failure(Exception("创建共享链接成功但返回数据为空"))
                }
            } else {
                // 增强错误信息
                val errorBody = response.errorBody()?.string() ?: ""
                val errorCode = when (response.code()) {
                    403 -> "权限不足"
                    404 -> "找不到指定的文件或文件夹"
                    else -> response.code().toString()
                }
                Result.failure(Exception("创建共享链接失败($errorCode): ${response.message()}\n详情:$errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 创建上传会话（用于大文件上传）
     */
    suspend fun createUploadSession(
        token: String,
        parentId: String,
        fileName: String
    ): Result<UploadSessionResponse> = withContext(Dispatchers.IO) {
        try {
            val authToken = "Bearer $token"
            
            // 创建请求体
            val requestBody = UploadSessionRequest(
                item = UploadItemProperties(
                    conflictBehavior = "rename",
                    name = fileName
                )
            )
            
            // 根据parentId选择适当的API
            val response = if (parentId == "root") {
                oneDriveService.createUploadSessionForRoot(
                    authToken = authToken,
                    filename = fileName,
                    request = requestBody
                )
            } else {
                oneDriveService.createUploadSession(
                    authToken = authToken,
                    parentId = parentId,
                    filename = fileName,
                    request = requestBody
                )
            }
            
            if (response.isSuccessful) {
                val sessionResponse = response.body()
                if (sessionResponse != null) {
                    Result.success(sessionResponse)
                } else {
                    Result.failure(Exception("创建上传会话成功但返回数据为空"))
                }
            } else {
                // 增强错误信息
                val errorBody = response.errorBody()?.string() ?: ""
                val errorCode = when (response.code()) {
                    403 -> "权限不足"
                    404 -> "找不到指定的文件夹（ID可能无效）"
                    else -> response.code().toString()
                }
                Result.failure(Exception("创建上传会话失败($errorCode): ${response.message()}\n详情:$errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 上传大文件 - 使用分片上传
     * @param uploadUrl 上传会话URL
     * @param inputStream 文件输入流
     * @param contentType 文件类型
     * @param totalSize 文件总大小
     * @param chunkSize 每个片段大小（建议为320KB的倍数，最大不超过60MB）
     * @param onProgress 进度回调
     */
    suspend fun uploadLargeFile(
        uploadUrl: String,
        inputStream: InputStream,
        contentType: String,
        totalSize: Long,
        chunkSize: Int = 10 * 1024 * 1024, // 默认10MB
        onProgress: (current: Long, total: Long) -> Unit
    ): Flow<Result<DriveItem>> = flow {
        var uploadedBytes = 0L
        val buffer = ByteArray(chunkSize)
        val bufferedInputStream = BufferedInputStream(inputStream)
        
        try {
            var bytesRead: Int
            
            while (bufferedInputStream.read(buffer).also { bytesRead = it } != -1) {
                if (bytesRead <= 0) break
                
                // 准备当前片段的字节数组
                val chunk = if (bytesRead < buffer.size) buffer.copyOf(bytesRead) else buffer
                
                // 计算范围
                val startByte = uploadedBytes
                val endByte = uploadedBytes + bytesRead - 1
                
                // 准备Content-Range头
                val contentRange = "bytes $startByte-$endByte/$totalSize"
                
                // 创建RequestBody
                val requestBody = chunk.toRequestBody(contentType.toMediaTypeOrNull())
                
                // 上传片段
                val response = oneDriveService.uploadFileFragment(
                    uploadUrl = uploadUrl,
                    contentRange = contentRange,
                    content = requestBody
                )
                
                if (response.isSuccessful) {
                    // 更新已上传字节数
                    uploadedBytes += bytesRead
                    
                    // 回调进度
                    onProgress(uploadedBytes, totalSize)
                    
                    // 如果是最后一个片段，response.body()将包含完整的DriveItem
                    if (uploadedBytes >= totalSize || response.code() == 201 || response.code() == 200) {
                        val driveItem = response.body()
                        if (driveItem != null) {
                            emit(Result.success(driveItem))
                            break
                        }
                    }
                } else {
                    // 处理错误
                    val errorBody = response.errorBody()?.string() ?: ""
                    val error = Exception("上传片段失败(${response.code()}): ${response.message()}\n详情:$errorBody")
                    emit(Result.failure(error))
                    break
                }
            }
            
            // 确保关闭流
            bufferedInputStream.close()
            inputStream.close()
            
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    /**
     * 获取上传会话状态
     */
    suspend fun getUploadSessionStatus(
        uploadUrl: String
    ): Result<UploadSessionResponse> = withContext(Dispatchers.IO) {
        try {
            val response = oneDriveService.getUploadSessionStatus(uploadUrl)
            
            if (response.isSuccessful) {
                val sessionStatus = response.body()
                if (sessionStatus != null) {
                    Result.success(sessionStatus)
                } else {
                    Result.failure(Exception("获取上传会话状态成功但返回数据为空"))
                }
            } else {
                val errorBody = response.errorBody()?.string() ?: ""
                Result.failure(Exception("获取上传会话状态失败(${response.code()}): ${response.message()}\n详情:$errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 取消上传会话
     */
    suspend fun cancelUploadSession(
        uploadUrl: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val response = oneDriveService.cancelUploadSession(uploadUrl)
            
            if (response.isSuccessful) {
                Result.success(true)
            } else {
                val errorBody = response.errorBody()?.string() ?: ""
                Result.failure(Exception("取消上传会话失败(${response.code()}): ${response.message()}\n详情:$errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 上传大文件的综合方法 - 处理整个上传流程
     * @param token 访问令牌
     * @param parentId 父文件夹ID
     * @param fileName 文件名
     * @param inputStream 文件输入流
     * @param contentType 文件MIME类型
     * @param fileSize 文件大小
     * @param onProgress 进度回调函数
     */
    suspend fun uploadLargeFileComplete(
        token: String,
        parentId: String,
        fileName: String,
        inputStream: InputStream,
        contentType: String,
        fileSize: Long,
        onProgress: (progress: Int) -> Unit
    ): Result<DriveItem> = withContext(Dispatchers.IO) {
        try {
            // 创建上传会话
            val sessionResult = createUploadSession(token, parentId, fileName)
            
            if (sessionResult.isSuccess) {
                val uploadSession = sessionResult.getOrThrow()
                val uploadUrl = uploadSession.uploadUrl
                
                // 开始分片上传
                var finalResult: Result<DriveItem>? = null
                
                uploadLargeFile(
                    uploadUrl = uploadUrl,
                    inputStream = inputStream,
                    contentType = contentType,
                    totalSize = fileSize,
                    onProgress = { current, total ->
                        val progressPercent = ((current.toDouble() / total) * 100).toInt()
                        onProgress(progressPercent)
                    }
                ).collect { result ->
                    finalResult = result
                }
                
                finalResult ?: Result.failure(Exception("上传过程中发生未知错误"))
            } else {
                Result.failure(sessionResult.exceptionOrNull() ?: Exception("创建上传会话失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 判断文件大小并使用适当的方法上传
     * @param token 访问令牌
     * @param parentId 父文件夹ID
     * @param fileName 文件名
     * @param inputStream 文件输入流
     * @param contentType 文件MIME类型  
     * @param fileSize 文件大小
     * @param onProgress 进度回调函数
     */
    suspend fun smartUploadFile(
        token: String,
        parentId: String,
        fileName: String,
        inputStream: InputStream,
        contentType: String,
        fileSize: Long,
        onProgress: (progress: Int) -> Unit
    ): Result<DriveItem> = withContext(Dispatchers.IO) {
        try {
            // 大文件阈值（4MB）
            val LARGE_FILE_THRESHOLD = 4 * 1024 * 1024 // 4MB
            
            if (fileSize > LARGE_FILE_THRESHOLD) {
                // 大文件上传
                Log.d("OneDriveRepository", "使用分片上传大文件: $fileName, 大小: $fileSize 字节")
                uploadLargeFileComplete(token, parentId, fileName, inputStream, contentType, fileSize, onProgress)
            } else {
                // 小文件上传 - 读取整个文件到内存
                Log.d("OneDriveRepository", "使用简单上传小文件: $fileName, 大小: $fileSize 字节")
                
                // 创建包含进度报告的RequestBody
                val bytes = inputStream.readBytes()
                
                // 创建自定义RequestBody来报告进度
                val requestBody = object : RequestBody() {
                    override fun contentType() = contentType.toMediaTypeOrNull()
                    override fun contentLength() = bytes.size.toLong()
                    
                    override fun writeTo(sink: okio.BufferedSink) {
                        var bytesWritten = 0L
                        val buffer = ByteArray(8192) // 8KB缓冲区
                        val inputStream = bytes.inputStream()
                        var read: Int
                        
                        while (inputStream.read(buffer).also { read = it } != -1) {
                            sink.write(buffer, 0, read)
                            bytesWritten += read
                            
                            // 报告进度
                            val progress = ((bytesWritten.toDouble() / bytes.size) * 100).toInt()
                            onProgress(progress)
                        }
                    }
                }
                
                // 使用现有的上传方法
                val result = if (parentId == "root") {
                    oneDriveService.uploadFileToRoot(
                        authToken = "Bearer $token",
                        filename = fileName,
                        fileContent = requestBody,
                        conflictBehavior = "rename"
                    )
                } else {
                    oneDriveService.uploadNewFile(
                        authToken = "Bearer $token",
                        parentId = parentId,
                        filename = fileName,
                        fileContent = requestBody,
                        conflictBehavior = "rename"
                    )
                }
                
                if (result.isSuccessful) {
                    val item = result.body()
                    if (item != null) {
                        Result.success(item)
                    } else {
                        Result.failure(Exception("上传成功但返回数据为空"))
                    }
                } else {
                    val errorBody = result.errorBody()?.string() ?: ""
                    Result.failure(Exception("上传失败(${result.code()}): ${result.message()}\n详情:$errorBody"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
} 