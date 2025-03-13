package com.lurenjia534.nextonedrivev3

import android.content.Context
import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lurenjia534.nextonedrivev3.AuthRepository.AuthViewModel
import com.lurenjia534.nextonedrivev3.AuthRepository.TokenManager
import com.lurenjia534.nextonedrivev3.CloudViewModelManager.AccountManager
import com.lurenjia534.nextonedrivev3.CloudViewModelManager.FileNavigator
import com.lurenjia534.nextonedrivev3.CloudViewModelManager.FileOperator
import com.lurenjia534.nextonedrivev3.CloudViewModelManager.FileUploader
import com.lurenjia534.nextonedrivev3.CloudViewModelManager.UISettingsManager
import com.lurenjia534.nextonedrivev3.data.model.DriveInfo
import com.lurenjia534.nextonedrivev3.data.model.DriveItem
import com.lurenjia534.nextonedrivev3.data.repository.OneDriveRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import com.lurenjia534.nextonedrivev3.notification.UploadNotificationService

@HiltViewModel
class CloudViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val oneDriveRepository: OneDriveRepository,
    private val authViewModel: AuthViewModel,
    private val uploadNotificationService: UploadNotificationService,
    private val tokenManager: TokenManager
) : ViewModel() {

    // 各功能管理器
    private val accountManager = AccountManager(
        this,
        viewModelScope,
        oneDriveRepository,
        authViewModel,
        tokenManager
    )

    private val fileNavigator = FileNavigator(
        this,
        viewModelScope,
        oneDriveRepository,
        accountManager
    )

    private val fileUploader = FileUploader(
        this,
        viewModelScope,
        oneDriveRepository,
        uploadNotificationService,
        accountManager,
        fileNavigator,
        context  // 添加Context参数，用于启动服务
    )

    private val fileOperator = FileOperator(
        this,
        viewModelScope,
        oneDriveRepository,
        accountManager,
        fileNavigator
    )

    private val uiSettingsManager = UISettingsManager(
        this,
        viewModelScope,
        context
    )

    // 暴露各管理器的公共状态和方法

    // ---------- 账户信息 ----------
    val accountId: LiveData<String> = accountManager.accountId
    val accountName: LiveData<String> = accountManager.accountName
    val accountToken: LiveData<String> = accountManager.accountToken
    val driveInfo: LiveData<DriveInfo?> = accountManager.driveInfo
    val isDriveInfoLoading: LiveData<Boolean> = accountManager.isDriveInfoLoading

    // ---------- 文件导航 ----------
    val isLoading: LiveData<Boolean> = fileNavigator.isLoading
    val driveItems: LiveData<List<DriveItem>> = fileNavigator.driveItems
    val currentFolderStack: LiveData<MutableList<DriveItem>> = fileNavigator.currentFolderStack
    val currentFolderId: LiveData<String?> = fileNavigator.currentFolderId

    // 文件夹浏览（移动功能用）
    val availableFolders: LiveData<List<DriveItem>> = fileNavigator.availableFolders
    val loadingFolders: LiveData<Boolean> = fileNavigator.loadingFolders
    val currentBrowseFolderId: StateFlow<String> = fileNavigator.currentBrowseFolderId
    val browsePathStack: StateFlow<MutableList<DriveItem>> = fileNavigator.browsePathStack

    // ---------- 文件上传 ----------
    val uploadingState: StateFlow<FileUploader.UploadingState> = fileUploader.uploadingState

    // ---------- 文件操作 ----------
    val deletingState: StateFlow<FileOperator.DeletingState> = fileOperator.deletingState
    val sharingState: StateFlow<FileOperator.SharingState> = fileOperator.sharingState
    val movingState: StateFlow<FileOperator.MovingState> = fileOperator.movingState
    val shareOptions: List<FileOperator.ShareOption> = fileOperator.shareOptions

    // ---------- UI设置 ----------
    val isDarkMode: LiveData<Boolean> = uiSettingsManager.isDarkMode

    // ---------- 错误信息 ----------
    // 合并所有管理器的错误信息
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // 监听各管理器的错误信息
    init {
        accountManager.errorMessage.observeForever { error ->
            error?.let { _errorMessage.value = it }
        }

        fileNavigator.errorMessage.observeForever { error ->
            error?.let { _errorMessage.value = it }
        }

        fileUploader.errorMessage.observeForever { error ->
            error?.let { _errorMessage.value = it }
        }

        fileOperator.errorMessage.observeForever { error ->
            error?.let { _errorMessage.value = it }
        }
    }

    // ---------- 公开API方法（委托到各管理器） ----------

    /**
     * 初始化账户信息
     */
    fun initializeAccount(id: String, name: String, token: String) {
        accountManager.initializeAccount(id, name, token) {
            // 初始化后加载文件
            fileNavigator.loadCloudFiles()
        }
    }

    /**
     * 刷新当前文件夹
     */
    fun refreshCurrentFolder() {
        fileNavigator.refreshCurrentFolder()
    }

    /**
     * 刷新云盘信息
     */
    fun refreshDriveInfo() {
        accountManager.loadDriveInfo()
    }

    /**
     * 打开文件夹
     */
    fun openFolder(folder: DriveItem) {
        fileNavigator.openFolder(folder)
    }

    /**
     * 返回上一级目录
     * @return 是否有上级文件夹可返回
     */
    fun navigateUp(): Boolean {
        return fileNavigator.navigateUp()
    }

    /**
     * 上传照片
     */
    fun uploadPhoto(contentResolver: ContentResolver, uri: Uri) {
        fileUploader.uploadPhoto(contentResolver, uri)
    }

    /**
     * 上传文件
     */
    fun uploadFile(contentResolver: ContentResolver, uri: Uri) {
        fileUploader.uploadFile(contentResolver, uri)
    }

    /**
     * 上传多个照片
     */
    fun uploadMultiplePhotos(contentResolver: ContentResolver, uris: List<Uri>) {
        fileUploader.uploadMultiplePhotos(contentResolver, uris)
    }

    /**
     * 创建文件夹
     */
    fun createFolder(folderName: String) {
        fileOperator.createFolder(folderName)
    }

    /**
     * 删除文件或文件夹
     */
    fun deleteItem(item: DriveItem) {
        fileOperator.deleteItem(item)
    }

    /**
     * 创建文件或文件夹的共享链接
     */
    fun shareItem(
        item: DriveItem,
        type: String = "view",
        scope: String = "anonymous"
    ) {
        fileOperator.shareItem(item, type, scope)
    }

    /**
     * 移动文件或文件夹
     */
    fun moveItem(item: DriveItem, destinationFolderId: String) {
        fileOperator.moveItem(item, destinationFolderId)
    }

    /**
     * 加载可用文件夹（移动功能用）
     */
    fun loadAvailableFolders(folderId: String = "root") {
        fileNavigator.loadAvailableFolders(folderId)
    }

    /**
     * 进入子文件夹（移动功能用）
     */
    fun enterFolder(folder: DriveItem) {
        fileNavigator.enterFolder(folder)
    }

    /**
     * 在文件移动对话框中返回上一级文件夹
     */
    fun navigateUpInMoveDialog() {
        fileNavigator.navigateUpInMoveDialog()
    }

    /**
     * 重置移动浏览状态
     */
    fun resetFolderBrowsing() {
        fileNavigator.resetFolderBrowsing()
    }
    
    /**
     * 清理资源
     */
    override fun onCleared() {
        super.onCleared()
        
        // 清理上传服务连接
        fileUploader.onCleared()
    }
}