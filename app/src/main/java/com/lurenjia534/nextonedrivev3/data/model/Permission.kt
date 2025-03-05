package com.lurenjia534.nextonedrivev3.data.model

import com.google.gson.annotations.SerializedName

data class Permission(
    val id: String,
    val roles: List<String>,
    val link: ShareLink
)

data class ShareLink(
    val type: String,
    val scope: String?,
    val webUrl: String,
    val webHtml: String? = null,
    val application: ShareApplication? = null
)

data class ShareApplication(
    val id: String,
    val displayName: String
) 