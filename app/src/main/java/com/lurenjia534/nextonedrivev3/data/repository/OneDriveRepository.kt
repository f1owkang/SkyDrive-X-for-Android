package com.lurenjia534.nextonedrivev3.data.repository

import com.lurenjia534.nextonedrivev3.data.api.OneDriveService
import com.lurenjia534.nextonedrivev3.data.model.DriveInfo
import com.lurenjia534.nextonedrivev3.data.model.DriveItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
} 