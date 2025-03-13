package com.lurenjia534.nextonedrivev3.data.model

import com.google.gson.annotations.SerializedName

/**
 * 复制项目请求的数据模型
 */
data class CopyItemRequest(
    /**
     * 引用在其中创建副本的父项
     */
    @SerializedName("parentReference")
    val parentReference: ParentReference? = null,
    
    /**
     * 副本的新名称
     * 如果未提供新名称，将使用原始名称
     */
    @SerializedName("name")
    val name: String? = null
) {
    /**
     * 父引用数据模型
     */
    data class ParentReference(
        /**
         * 目标驱动器ID
         */
        @SerializedName("driveId")
        val driveId: String? = null,
        
        /**
         * 目标文件夹ID
         */
        @SerializedName("id")
        val id: String
    )
} 