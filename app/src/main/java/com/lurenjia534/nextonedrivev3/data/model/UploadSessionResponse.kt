package com.lurenjia534.nextonedrivev3.data.model

import com.google.gson.annotations.SerializedName

data class UploadSessionResponse(
    @SerializedName("uploadUrl")
    val uploadUrl: String = "",
    
    @SerializedName("expirationDateTime")
    val expirationDateTime: String = "",
    
    @SerializedName("nextExpectedRanges")
    val nextExpectedRanges: List<String> = emptyList()
) 