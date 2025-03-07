package com.lurenjia534.nextonedrivev3.data.model

import com.google.gson.annotations.SerializedName

data class MoveItemRequest(
    @SerializedName("parentReference")
    val parentReference: MoveParentReference,
    
    @SerializedName("name")
    val name: String? = null // 可选，如果需要同时重命名
) {
    // 创建一个内部类以避免冲突
    data class MoveParentReference(
        @SerializedName("id")
        val id: String
    )
} 