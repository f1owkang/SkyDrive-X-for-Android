package com.lurenjia534.nextonedrivev3.data.model

import com.google.gson.annotations.SerializedName

/**
 * 复制作业响应的数据模型
 * 用于监控复制操作进度
 */
data class CopyJobResponse(
    /**
     * 复制作业状态
     */
    @SerializedName("status")
    val status: String? = null,
    
    /**
     * 操作百分比完成情况
     */
    @SerializedName("percentageComplete")
    val percentageComplete: Double? = null,
    
    /**
     * 复制完成后的DriveItem
     */
    @SerializedName("resourceId")
    val resourceId: String? = null,
    
    /**
     * 错误信息（如果有）
     */
    @SerializedName("error")
    val error: ErrorInfo? = null
) {
    /**
     * 错误信息数据模型
     */
    data class ErrorInfo(
        @SerializedName("code")
        val code: String? = null,
        
        @SerializedName("message")
        val message: String? = null
    )
} 