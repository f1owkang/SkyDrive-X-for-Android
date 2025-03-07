package com.lurenjia534.nextonedrivev3.data.model

import com.google.gson.annotations.SerializedName

data class UploadSessionRequest(
    @SerializedName("item")
    val item: UploadItemProperties? = null
)

data class UploadItemProperties(
    @SerializedName("@microsoft.graph.conflictBehavior")
    val conflictBehavior: String = "rename",
    
    @SerializedName("name")
    val name: String? = null,
    
    @SerializedName("description")
    val description: String? = null
) 