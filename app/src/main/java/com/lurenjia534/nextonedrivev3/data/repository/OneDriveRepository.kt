package com.lurenjia534.nextonedrivev3.data.repository

import android.content.ContentResolver
import android.net.Uri
import com.lurenjia534.nextonedrivev3.data.api.OneDriveService
import com.lurenjia534.nextonedrivev3.data.model.DriveInfo
import com.lurenjia534.nextonedrivev3.data.model.DriveItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

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
} 