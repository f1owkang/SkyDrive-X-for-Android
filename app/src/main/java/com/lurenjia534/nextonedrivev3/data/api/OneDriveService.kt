package com.lurenjia534.nextonedrivev3.data.api

import com.lurenjia534.nextonedrivev3.data.model.DriveInfo
import com.lurenjia534.nextonedrivev3.data.model.DriveItem
import com.lurenjia534.nextonedrivev3.data.model.DriveResponse
import com.lurenjia534.nextonedrivev3.data.model.Permission
import com.lurenjia534.nextonedrivev3.data.model.CreateLinkRequest
import com.lurenjia534.nextonedrivev3.data.model.UploadSessionResponse
import com.lurenjia534.nextonedrivev3.data.model.UploadSessionRequest
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.PUT
import retrofit2.http.DELETE
import retrofit2.http.Url
import retrofit2.http.Headers

interface OneDriveService {
    @GET("me/drive/root/children")
    suspend fun getRootItems(
        @Header("Authorization") authToken: String
    ): Response<DriveResponse>
    
    @GET("me/drive/items/{itemId}/children")
    suspend fun getFolderItems(
        @Header("Authorization") authToken: String,
        @Path("itemId") itemId: String
    ): Response<DriveResponse>
    
    @GET("me/drive/root:/{path}:/children")
    suspend fun getItemsByPath(
        @Header("Authorization") authToken: String,
        @Path("path") path: String
    ): Response<DriveResponse>
    
    @GET("me/drive")
    suspend fun getDriveInfo(
        @Header("Authorization") authToken: String
    ): Response<DriveInfo>
    
    // 上传新文件
    @PUT("me/drive/items/{parentId}:/{filename}:/content")
    suspend fun uploadNewFile(
        @Header("Authorization") authToken: String,
        @Path("parentId") parentId: String,
        @Path("filename") filename: String,
        @Body fileContent: RequestBody,
        @Query("@microsoft.graph.conflictBehavior") conflictBehavior: String = "rename"
    ): Response<DriveItem>
    
    // 添加直接上传到root文件夹的方法
    @PUT("me/drive/root:/{filename}:/content")
    suspend fun uploadFileToRoot(
        @Header("Authorization") authToken: String,
        @Path("filename") filename: String,
        @Body fileContent: RequestBody,
        @Query("@microsoft.graph.conflictBehavior") conflictBehavior: String = "rename"
    ): Response<DriveItem>
    
    // 创建文件夹
    @POST("me/drive/items/{parentId}/children")
    suspend fun createFolder(
        @Header("Authorization") authToken: String,
        @Path("parentId") parentId: String,
        @Body folderInfo: RequestBody
    ): Response<DriveItem>
    
    // 删除文件或文件夹
    @DELETE("me/drive/items/{itemId}")
    suspend fun deleteItem(
        @Header("Authorization") authToken: String,
        @Path("itemId") itemId: String
    ): Response<Void>
    
    // 创建共享链接
    @POST("me/drive/items/{itemId}/createLink")
    suspend fun createShareLink(
        @Header("Authorization") authToken: String,
        @Path("itemId") itemId: String,
        @Body linkRequest: CreateLinkRequest
    ): Response<Permission>
    
    // 创建上传会话 - 用于大文件上传
    @POST("me/drive/items/{parentId}:/{filename}:/createUploadSession")
    suspend fun createUploadSession(
        @Header("Authorization") authToken: String,
        @Path("parentId") parentId: String,
        @Path("filename") filename: String,
        @Body request: UploadSessionRequest
    ): Response<UploadSessionResponse>
    
    // 为根目录创建上传会话
    @POST("me/drive/root:/{filename}:/createUploadSession")
    suspend fun createUploadSessionForRoot(
        @Header("Authorization") authToken: String,
        @Path("filename") filename: String,
        @Body request: UploadSessionRequest
    ): Response<UploadSessionResponse>
    
    // 上传文件片段
    @PUT
    @Headers("Content-Type: application/octet-stream")
    suspend fun uploadFileFragment(
        @Url uploadUrl: String,
        @Header("Content-Range") contentRange: String,
        @Body content: RequestBody
    ): Response<DriveItem>
    
    // 获取上传会话状态
    @GET
    suspend fun getUploadSessionStatus(
        @Url uploadUrl: String
    ): Response<UploadSessionResponse>
    
    // 取消上传会话
    @DELETE
    suspend fun cancelUploadSession(
        @Url uploadUrl: String
    ): Response<Void>
} 