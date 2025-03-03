package com.lurenjia534.nextonedrivev3.data.model

import com.google.gson.annotations.SerializedName

data class DriveResponse(
    @SerializedName("@odata.context") val context: String,
    @SerializedName("value") val items: List<DriveItem>
)

data class DriveItem(
    val id: String,
    val name: String,
    val size: Long,
    val createdDateTime: String,
    val lastModifiedDateTime: String,
    val webUrl: String,
    val createdBy: Identity? = null,
    val lastModifiedBy: Identity? = null,
    val parentReference: ParentReference? = null,
    val folder: Folder? = null,
    val file: File? = null,
    val fileSystemInfo: FileSystemInfo? = null
) {
    val isFolder: Boolean get() = folder != null
    val isFile: Boolean get() = file != null
}

data class Identity(
    val user: User? = null,
    val application: Application? = null
)

data class User(
    val displayName: String,
    val id: String,
    val email: String? = null
)

data class Application(
    val id: String,
    val displayName: String
)

data class ParentReference(
    val driveId: String,
    val driveType: String? = null,
    val id: String,
    val path: String? = null,
    val name: String? = null,
    val siteId: String? = null
)

data class Folder(
    val childCount: Int
)

data class File(
    val mimeType: String? = null
)

data class FileSystemInfo(
    val createdDateTime: String,
    val lastModifiedDateTime: String
) 