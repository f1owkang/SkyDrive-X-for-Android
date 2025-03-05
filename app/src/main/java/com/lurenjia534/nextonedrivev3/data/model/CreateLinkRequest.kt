package com.lurenjia534.nextonedrivev3.data.model

import com.google.gson.annotations.SerializedName

data class CreateLinkRequest(
    val type: String, // "view", "edit", or "embed"
    val scope: String? = null // "anonymous" or "organization"
) 