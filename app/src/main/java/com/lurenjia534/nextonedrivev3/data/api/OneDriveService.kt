package com.lurenjia534.nextonedrivev3.data.api

import com.lurenjia534.nextonedrivev3.data.model.DriveInfo
import com.lurenjia534.nextonedrivev3.data.model.DriveResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

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
} 