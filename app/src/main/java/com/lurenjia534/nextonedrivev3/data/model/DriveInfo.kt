package com.lurenjia534.nextonedrivev3.data.model

import com.google.gson.annotations.SerializedName

data class DriveInfo(
    val id: String,
    val name: String,
    val description: String,
    val driveType: String,
    val createdDateTime: String,
    val lastModifiedDateTime: String,
    val webUrl: String,
    val createdBy: Identity? = null,
    val lastModifiedBy: Identity? = null,
    val owner: Identity? = null,
    val quota: Quota? = null
)

data class Quota(
    val total: Long,
    val used: Long,
    val remaining: Long,
    val deleted: Long,
    val state: String
) 