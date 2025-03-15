package com.lurenjia534.nextonedrivev3

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.lurenjia534.nextonedrivev3.ui.theme.NextOneDriveV3Theme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.lurenjia534.nextonedrivev3.data.model.DriveItem
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Slideshow
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material.icons.outlined.VideoFile
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.launch
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import android.content.Intent
import android.content.ClipboardManager
import android.content.ClipData
import android.content.Context
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.ui.unit.Dp
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.DataUsage
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Storage
import com.lurenjia534.nextonedrivev3.CloudViewModelManager.FileOperator.DeletingState
import com.lurenjia534.nextonedrivev3.CloudViewModelManager.FileOperator.MovingState
import com.lurenjia534.nextonedrivev3.CloudViewModelManager.FileOperator.ShareOption
import com.lurenjia534.nextonedrivev3.CloudViewModelManager.FileOperator.SharingState
import com.lurenjia534.nextonedrivev3.CloudViewModelManager.FileUploader.UploadingState
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DriveFileMove
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Share
import com.lurenjia534.nextonedrivev3.CloudViewModelManager.FileOperator

@AndroidEntryPoint
class CloudActivity : ComponentActivity() {

    private val cloudViewModel: CloudViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 从Intent中获取账户信息
        val accountId = intent.getStringExtra(EXTRA_ACCOUNT_ID) ?: ""
        val accountName = intent.getStringExtra(EXTRA_ACCOUNT_NAME) ?: ""
        val accountToken = intent.getStringExtra(EXTRA_ACCOUNT_TOKEN) ?: ""

        // 初始化ViewModel数据
        cloudViewModel.initializeAccount(accountId, accountName, accountToken)

        setContent {
            // 获取深色模式状态
            val isDarkMode by cloudViewModel.isDarkMode.observeAsState(false)
            
            NextOneDriveV3Theme(
                darkTheme = isDarkMode
            ) {
                CloudScreen(
                    viewModel = cloudViewModel,
                    accountName = accountName,
                    onBackPressed = { finish() }
                )
            }
        }
    }

    companion object {
        const val EXTRA_ACCOUNT_ID = "extra_account_id"
        const val EXTRA_ACCOUNT_NAME = "extra_account_name"
        const val EXTRA_ACCOUNT_TOKEN = "extra_account_token"
    }
}

/** 定义云盘底部导航项 */
sealed class CloudNavItem(val route: String, val icon: ImageVector, val label: String) {
    object Files : CloudNavItem("files", Icons.Default.Folder, "文件列表")
    object Profile : CloudNavItem("profile", Icons.Default.Person, "我的信息")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudScreen(
    viewModel: CloudViewModel,
    accountName: String,
    onBackPressed: () -> Unit
) {
    val navController = rememberNavController()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$accountName 的云盘") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        },
        bottomBar = {
            CloudBottomNavigationBar(navController = navController)
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = CloudNavItem.Files.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(CloudNavItem.Files.route) {
                FilesScreen(viewModel = viewModel)
            }
            composable(CloudNavItem.Profile.route) {
                ProfileScreen(viewModel = viewModel, accountName = accountName)
            }
        }
    }
}

/** 云盘底部导航栏 */
@Composable
fun CloudBottomNavigationBar(navController: NavController) {
    val items = listOf(
        CloudNavItem.Files,
        CloudNavItem.Profile
    )
    
    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        
        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

/** 文件列表页面 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilesScreen(viewModel: CloudViewModel) {
    val driveItems by viewModel.driveItems.observeAsState(emptyList())
    val isLoading by viewModel.isLoading.observeAsState(false)
    val errorMessage by viewModel.errorMessage.observeAsState(null)
    val currentFolderStack by viewModel.currentFolderStack.observeAsState(mutableListOf())
    val context = LocalContext.current
    
    // SnackbarHostState控制Snackbar的显示
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    
    // 控制对话框显示的状态
    var showOptionsDialog by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    
    // 添加删除确认对话框状态
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<DriveItem?>(null) }
    
    // 添加共享选项对话框状态
    var showShareOptionsDialog by remember { mutableStateOf(false) }
    var itemToShare by remember { mutableStateOf<DriveItem?>(null) }
    var showShareResultDialog by remember { mutableStateOf(false) }
    var shareUrl by remember { mutableStateOf("") }
    
    // 注册启动多图片选择器的结果处理
    val multiplePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            // 上传选中的多张照片
            viewModel.uploadMultiplePhotos(context.contentResolver, uris)
        }
    }
    
    // 注册启动通用文件选择器的结果处理
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.uploadFile(context.contentResolver, it)
        }
    }
    
    // 多个权限请求启动器（用于Android 13+）
    val multiplePermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            // 所有权限都被授予
            filePickerLauncher.launch("*/*")
        } else {
            // 至少一个权限被拒绝
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = "需要存储权限才能上传文件",
                    duration = SnackbarDuration.Long
                )
            }
        }
    }
    
    // 单个权限请求启动器（用于Android 12及以下）
    val singlePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            filePickerLauncher.launch("*/*")
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = "需要存储权限才能上传文件",
                    duration = SnackbarDuration.Long
                )
            }
        }
    }
    
    // 通知权限请求
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // 权限已授予，可以显示通知
        } else {
            // 权限被拒绝，显示提示
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = "需要通知权限才能显示上传进度",
                    duration = SnackbarDuration.Long
                )
            }
        }
    }
    
    // 检查并请求通知权限
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
    
    // 请求存储权限并启动文件选择器的函数
    fun requestStoragePermissionAndPickFile() {
        when {
            // Android 13+ (API 33+)：请求媒体特定权限
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                multiplePermissionsLauncher.launch(
                    arrayOf(
                        Manifest.permission.READ_MEDIA_IMAGES,
                        Manifest.permission.READ_MEDIA_VIDEO,
                        Manifest.permission.READ_MEDIA_AUDIO
                    )
                )
            }
            // Android 6.0-12L：请求READ_EXTERNAL_STORAGE
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                when {
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED -> {
                        filePickerLauncher.launch("*/*")
                    }
                    else -> {
                        singlePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
                }
            }
            // Android 5.1及以下：不需要动态权限请求
            else -> {
                filePickerLauncher.launch("*/*")
            }
        }
    }
    
    // 监视上传状态
    val uploadingState by viewModel.uploadingState.collectAsState()
    
    // 监视删除状态
    val deletingState by viewModel.deletingState.collectAsState()
    
    // 监视共享状态
    val sharingState by viewModel.sharingState.collectAsState()
    
    // 显示上传进度对话框
    when (val state = uploadingState) {
        is UploadingState.Uploading -> {
            UploadProgressDialog(
                uploadingState = state,
                onDismiss = {}
            )
        }
        else -> { /* 其他状态不显示对话框 */ }
    }
    
    // 添加移动状态监听
    val movingState by viewModel.movingState.collectAsState()
    val availableFolders by viewModel.availableFolders.observeAsState(emptyList())
    val loadingFolders by viewModel.loadingFolders.observeAsState(false)

    // 添加移动对话框状态
    var showMoveBottomSheet by remember { mutableStateOf(false) }
    var selectedItemForMove by remember { mutableStateOf<DriveItem?>(null) }

    // 添加获取浏览路径的状态
    val browsePathStack by viewModel.browsePathStack.collectAsState()

    // 添加复制状态监听
    val copyingState by viewModel.copyingState.collectAsState()
    
    // 添加复制对话框状态
    var showCopyBottomSheet by remember { mutableStateOf(false) }
    var selectedItemForCopy by remember { mutableStateOf<DriveItem?>(null) }
    var showCopyNameDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 显示当前路径导航栏
            if (currentFolderStack.isNotEmpty()) {
                Surface(
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.navigateUp() }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "返回上级目录"
                            )
                        }

                        Text(
                            text = currentFolderStack.lastOrNull()?.name ?: "根目录",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        // 刷新按钮
                        IconButton(onClick = { viewModel.refreshCurrentFolder() }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "刷新"
                            )
                        }
                    }
                }
            }
            
            // 显示错误信息
            errorMessage?.let {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            // 显示加载指示器或内容
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (errorMessage == null) 0.dp else 8.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else if (driveItems.isEmpty() && errorMessage == null) {
                    // 空文件夹视图
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "此文件夹为空",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    // 文件列表
                    LazyColumn(
                        contentPadding = PaddingValues(
                            top = 8.dp,
                            bottom = 80.dp,
                            start = 0.dp,
                            end = 0.dp
                        ) // 添加底部padding，防止FAB遮挡内容
                    ) {
                        items(driveItems) { item ->
                            FileListItem(
                                driveItem = item,
                                onClick = {
                                    if (item.isFolder) {
                                        viewModel.openFolder(item)
                                    } else {
                                        // TODO: 处理文件点击
                                    }
                                },
                                onShareClick = {
                                    // 显示分享选项对话框
                                    itemToShare = item
                                    showShareOptionsDialog = true
                                },
                                onDeleteClick = {
                                    // 显示删除确认对话框
                                    itemToDelete = item
                                    showDeleteConfirmDialog = true
                                },
                                onDownloadClick = {
                                    // 处理下载操作
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = "功能开发中：下载 ${item.name}",
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                },
                                onMoveClick = {
                                    selectedItemForMove = item
                                    viewModel.loadAvailableFolders()
                                    showMoveBottomSheet = true
                                },
                                onCopyClick = {
                                    selectedItemForCopy = item
                                    viewModel.loadAvailableFolders()
                                    showCopyBottomSheet = true
                                }
                            )
                        }
                    }
                }
            }
        }
        
        // 添加浮动操作按钮(FAB)
        FloatingActionButton(
            onClick = { showOptionsDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "添加文件或文件夹"
            )
        }
        
        // 显示选项对话框
        if (showOptionsDialog) {
            FileOperationsDialog(
                onDismiss = { showOptionsDialog = false },
                onCreateFolder = {
                    showOptionsDialog = false
                    showCreateFolderDialog = true
                },
                onUploadPhoto = {
                    showOptionsDialog = false
                    // 启动多图片选择器
                    multiplePhotoPickerLauncher.launch("image/*")
                },
                onUploadFile = {
                    showOptionsDialog = false
                    requestStoragePermissionAndPickFile()
                }
            )
        }

        // 创建文件夹对话框
        if (showCreateFolderDialog) {
            CreateFolderDialog(
                onDismiss = { showCreateFolderDialog = false },
                onCreateFolder = { folderName ->
                    viewModel.createFolder(folderName)
                    showCreateFolderDialog = false
                }
            )
        }

        // 上传状态观察 - 使用Snackbar替代Toast
        LaunchedEffect(uploadingState) {
            when (val state = uploadingState) {
                is UploadingState.Success -> {
                    // 显示上传成功的提示
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            message = "文件 ${state.item.name} 上传成功",
                            duration = SnackbarDuration.Short
                        )
                    }
                }
                is UploadingState.Error -> {
                    // 显示错误提示
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            message = state.message,
                            duration = SnackbarDuration.Long,
                            actionLabel = "确定"
                        )
                    }
                }
                else -> { /* 其他状态不做处理 */ }
            }
        }
        
        // 删除确认对话框
        if (showDeleteConfirmDialog && itemToDelete != null) {
            AlertDialog(
                onDismissRequest = { 
                    showDeleteConfirmDialog = false
                    itemToDelete = null
                },
                title = { Text("确认删除") },
                text = { Text("确定要删除「${itemToDelete?.name}」吗？此操作会将项目移至回收站。") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            itemToDelete?.let { viewModel.deleteItem(it) }
                            showDeleteConfirmDialog = false
                            itemToDelete = null
                        },
//                        colors = ButtonDefaults.textButtonColors(
//                            MaterialTheme.colorScheme.error
//                        )
                    ) {
                        Text("删除")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { 
                            showDeleteConfirmDialog = false
                            itemToDelete = null
                        }
                    ) {
                        Text("取消")
                    }
                }
            )
        }
        
        // 监视共享状态并处理
        LaunchedEffect(sharingState) {
            when (val state = sharingState) {
                is SharingState.Success -> {
                    // 显示分享结果对话框
                    shareUrl = state.permission.link.webUrl
                    showShareResultDialog = true
                }
                is SharingState.Error -> {
                    // 显示错误提示
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            message = state.message,
                            duration = SnackbarDuration.Long,
                            actionLabel = "确定"
                        )
                    }
                }
                else -> { /* 其他状态不做处理 */ }
            }
        }
        
        // 分享选项对话框
        if (showShareOptionsDialog && itemToShare != null) {
            ShareOptionsDialog(
                onDismiss = { 
                    showShareOptionsDialog = false 
                    itemToShare = null 
                },
                onConfirm = { shareOption ->
                    // 执行分享操作
                    itemToShare?.let { item ->
                        viewModel.shareItem(
                            item = item,
                            type = shareOption.type,
                            scope = shareOption.scope
                        )
                    }
                    showShareOptionsDialog = false
                },
                shareOptions = viewModel.shareOptions
            )
        }
        
        // 分享结果对话框
        if (showShareResultDialog && shareUrl.isNotEmpty()) {
            ShareLinkDialog(
                url = shareUrl,
                onDismiss = { 
                    showShareResultDialog = false 
                    shareUrl = ""
                },
                onCopy = {
                    // 复制链接到剪贴板
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("共享链接", shareUrl)
                    clipboard.setPrimaryClip(clip)
                    
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            message = "链接已复制到剪贴板",
                            duration = SnackbarDuration.Short
                        )
                    }
                },
                onShare = {
                    // 使用系统分享
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, shareUrl)
                        type = "text/plain"
                    }
                    val shareIntent = Intent.createChooser(sendIntent, "分享链接")
                    context.startActivity(shareIntent)
                }
            )
        }
        
        // 添加SnackbarHost
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp) // 确保不被底部导航栏遮挡
        )

        // 使用底部表单替代对话框
        if (showMoveBottomSheet && selectedItemForMove != null) {
            ModalBottomSheet(
                onDismissRequest = { 
                    showMoveBottomSheet = false 
                    viewModel.resetFolderBrowsing()
                },
                sheetState = rememberModalBottomSheetState()
            ) {
                MoveItemBottomSheetContent(
                    item = selectedItemForMove!!,
                    folders = availableFolders,
                    isLoading = loadingFolders,
                    currentPath = browsePathStack,
                    onConfirm = { destinationFolderId ->
                        viewModel.moveItem(selectedItemForMove!!, destinationFolderId)
                        showMoveBottomSheet = false
                        viewModel.resetFolderBrowsing()
                    },
                    onFolderClick = { folder ->
                        viewModel.enterFolder(folder)
                    },
                    onNavigateUp = {
                        viewModel.navigateUpInMoveDialog()
                    }
                )
            }
        }

        // 添加复制进度对话框
        when (val state = copyingState) {
            is FileOperator.CopyingState.Copying -> {
                AlertDialog(
                    onDismissRequest = { /* 不允许用户取消复制进度对话框 */ },
                    title = { Text("正在复制") },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("正在复制: ${state.itemName}")
                            Spacer(modifier = Modifier.height(16.dp))
                            LinearProgressIndicator(
                                progress = { state.progress.toFloat() / 100f },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = "${state.progress.toInt()}%",
                                modifier = Modifier.padding(top = 8.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    },
                    confirmButton = {}, // 复制过程中没有确认按钮
                    dismissButton = {}   // 复制过程中没有取消按钮
                )
            }
            else -> { /* 其他状态不显示对话框 */ }
        }
        
        // 复制状态观察
        LaunchedEffect(copyingState) {
            when (val state = copyingState) {
                is FileOperator.CopyingState.Success -> {
                    // 显示复制成功的提示
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            message = "「${state.itemName}」已复制成功",
                            duration = SnackbarDuration.Short
                        )
                    }
                }
                is FileOperator.CopyingState.Error -> {
                    // 显示错误提示
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            message = state.message,
                            duration = SnackbarDuration.Long,
                            actionLabel = "确定"
                        )
                    }
                }
                else -> { /* 其他状态不做处理 */ }
            }
        }
        
        // 使用底部表单选择复制目标位置
        if (showCopyBottomSheet && selectedItemForCopy != null) {
            ModalBottomSheet(
                onDismissRequest = { 
                    showCopyBottomSheet = false 
                    viewModel.resetFolderBrowsing()
                },
                sheetState = rememberModalBottomSheetState()
            ) {
                CopyItemBottomSheetContent(
                    item = selectedItemForCopy!!,
                    folders = availableFolders,
                    isLoading = loadingFolders,
                    currentPath = browsePathStack,
                    onConfirm = { destinationFolderId ->
                        // 显示重命名对话框
                        showCopyBottomSheet = false
                        showCopyNameDialog = true
                        // 在重命名对话框中确认后执行复制操作
                    },
                    onDirectCopy = { destinationFolderId ->
                        // 直接复制，不重命名
                        viewModel.copyItem(selectedItemForCopy!!, destinationFolderId)
                        showCopyBottomSheet = false
                        viewModel.resetFolderBrowsing()
                    },
                    onFolderClick = { folder ->
                        viewModel.enterFolder(folder)
                    },
                    onNavigateUp = {
                        viewModel.navigateUpInMoveDialog()
                    }
                )
            }
        }
        
        // 复制时重命名对话框
        if (showCopyNameDialog && selectedItemForCopy != null) {
            RenameForCopyDialog(
                originalName = selectedItemForCopy!!.name,
                onDismiss = { 
                    showCopyNameDialog = false 
                    selectedItemForCopy = null
                },
                onConfirm = { newName, destinationFolderId ->
                    viewModel.copyItem(selectedItemForCopy!!, destinationFolderId, newName)
                    showCopyNameDialog = false
                    selectedItemForCopy = null
                }
            )
        }
    }
}

@Composable
fun FileListItem(
    driveItem: DriveItem,
    onClick: () -> Unit,
    onShareClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onDownloadClick: () -> Unit = {},
    onMoveClick: () -> Unit = {},
    onCopyClick: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = { 
            Text(
                text = driveItem.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge
            )
        },
        supportingContent = {
            Text(
                text = if (driveItem.isFolder) {
                    "${driveItem.folder?.childCount ?: 0} 个项目 • ${formatDate(driveItem.lastModifiedDateTime)}"
                } else {
                    "${formatFileSize(driveItem.size)} • ${formatDate(driveItem.lastModifiedDateTime)}"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingContent = {
            Icon(
                imageVector = if (driveItem.isFolder) {
                    Icons.Default.Folder
                } else {
                    // 根据扩展名可以选择不同的图标
                    getFileIcon(driveItem.name)
                },
                contentDescription = null,
                tint = if (driveItem.isFolder) 
                    MaterialTheme.colorScheme.primary
                else 
                    MaterialTheme.colorScheme.secondary
            )
        },
        trailingContent = {
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "更多选项"
                    )
                }
                
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("分享") },
                        leadingIcon = { 
                            Icon(
                                imageVector = Icons.Rounded.Share,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            onShareClick()
                            showMenu = false
                        }
                    )
                    
                    DropdownMenuItem(
                        text = { Text("删除") },
                        leadingIcon = { 
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            onDeleteClick()
                            showMenu = false
                        }
                    )
                    
                    DropdownMenuItem(
                        text = { Text("下载") },
                        leadingIcon = { 
                            Icon(
                                imageVector = Icons.Rounded.Download,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            onDownloadClick()
                            showMenu = false
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("移动到") },
                        leadingIcon = { 
                            Icon(
                                imageVector = Icons.Rounded.DriveFileMove,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            onMoveClick()
                            showMenu = false
                        }
                    )
                    
                    DropdownMenuItem(
                        text = { Text("复制到") },
                        leadingIcon = { 
                            Icon(
                                imageVector = Icons.Rounded.ContentCopy,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            onCopyClick()
                            showMenu = false
                        }
                    )
                }
            }
        },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent
        ),
        modifier = Modifier
            .clickable(onClick = onClick)
    )
}

// 根据文件扩展名获取适当的图标
@Composable
fun getFileIcon(fileName: String): ImageVector {
    val extension = fileName.substringAfterLast('.', "").lowercase()
    
    return when (extension) {
        in listOf("jpg", "jpeg", "png", "gif", "bmp") -> Icons.Outlined.Image
        in listOf("mp4", "avi", "mov", "wmv") -> Icons.Outlined.VideoFile
        in listOf("mp3", "wav", "aac", "flac") -> Icons.Outlined.AudioFile
        in listOf("pdf") -> Icons.Outlined.PictureAsPdf
        in listOf("doc", "docx") -> Icons.Outlined.Description
        in listOf("xls", "xlsx") -> Icons.Outlined.TableChart
        in listOf("ppt", "pptx") -> Icons.Outlined.Slideshow
        in listOf("zip", "rar", "7z") -> Icons.Outlined.FolderZip
        else -> Icons.Outlined.InsertDriveFile
    }
}

// 工具函数保持不变
fun formatFileSize(size: Long): String {
    val kb = 1024.0
    val mb = kb * 1024
    val gb = mb * 1024
    
    return when {
        size < kb -> "$size B"
        size < mb -> String.format(Locale.US, "%.2f KB", size / kb)
        size < gb -> String.format(Locale.US, "%.2f MB", size / mb)
        else -> String.format(Locale.US, "%.2f GB", size / gb)
    }
}

fun formatDate(dateTime: String): String {
    // 简单格式化示例，根据需要可以使用更复杂的日期格式化
    if (dateTime.length < 10) return dateTime
    return dateTime.substring(0, 10)
}

/** 我的信息页面 */
@Composable
fun ProfileScreen(viewModel: CloudViewModel, accountName: String) {
    val driveInfo by viewModel.driveInfo.observeAsState()
    val isLoading by viewModel.isDriveInfoLoading.observeAsState(false)
    val errorMessage by viewModel.errorMessage.observeAsState(null)
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // 用户信息部分
            ListItem(
                headlineContent = { 
                    Text(
                        text = accountName,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                supportingContent = {
                    Text(
                        text = driveInfo?.owner?.user?.email ?: "加载中...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                leadingContent = {
                    // 用户头像
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = accountName.firstOrNull()?.toString() ?: "?",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                },
                trailingContent = {
                    IconButton(onClick = { viewModel.refreshDriveInfo() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "刷新"
                        )
                    }
                },
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // 错误信息
            AnimatedVisibility(
                visible = errorMessage != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                errorMessage?.let {
                    ListItem(
                        headlineContent = { 
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                        )
                    )
                }
            }

            // 主内容 - 仅在非加载状态显示
            AnimatedVisibility(
                visible = !isLoading && driveInfo != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    // 展示存储使用情况
                    driveInfo?.quota?.let { quota ->
                        val usedPercentage = (quota.used.toFloat() / quota.total.toFloat()) * 100
                        
                        // 存储进度条项
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            // 显示已用空间和总空间
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "已用 ${formatFileSize(quota.used)}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                
                                Text(
                                    text = "总共 ${formatFileSize(quota.total)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // 进度指示器
                            LinearProgressIndicator(
                                progress = { usedPercentage / 100 },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp),
                                color = when {
                                    usedPercentage > 90 -> MaterialTheme.colorScheme.error
                                    usedPercentage > 70 -> MaterialTheme.colorScheme.tertiary
                                    else -> MaterialTheme.colorScheme.primary
                                }
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // 百分比显示
                            Text(
                                text = "已使用 ${String.format(Locale.US,"%.1f", usedPercentage)}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = when {
                                    usedPercentage > 90 -> MaterialTheme.colorScheme.error
                                    usedPercentage > 70 -> MaterialTheme.colorScheme.tertiary
                                    else -> MaterialTheme.colorScheme.primary
                                }
                            )
                        }
                        // 云盘详细信息项
                        ListItemWithIcon(
                            title = "总容量",
                            subtitle = formatFileSize(quota.total),
                            icon = Icons.Outlined.Storage,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        
                        ListItemWithIcon(
                            title = "已使用",
                            subtitle = formatFileSize(quota.used),
                            icon = Icons.Outlined.Save,
                            tint = when {
                                usedPercentage > 90 -> MaterialTheme.colorScheme.error
                                usedPercentage > 70 -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.primary
                            }
                        )
                        
                        ListItemWithIcon(
                            title = "剩余空间",
                            subtitle = formatFileSize(quota.remaining),
                            icon = Icons.Outlined.DataUsage,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                    // 云盘基本信息项
                    ListItemWithIcon(
                        title = "名称",
                        subtitle = driveInfo!!.name,
                        icon = Icons.Outlined.DriveFileRenameOutline,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    
                    ListItemWithIcon(
                        title = "类型",
                        subtitle = driveInfo!!.driveType,
                        icon = Icons.Outlined.Category,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    
                    ListItemWithIcon(
                        title = "创建时间",
                        subtitle = formatDate(driveInfo!!.createdDateTime),
                        icon = Icons.Outlined.Schedule,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // 加载指示器
            AnimatedVisibility(
                visible = isLoading,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "正在加载云盘信息...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // 无数据状态
            AnimatedVisibility(
                visible = !isLoading && driveInfo == null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "无法获取云盘信息",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "请检查网络连接并点击刷新按钮重试",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        FilledTonalButton(
                            onClick = { viewModel.refreshDriveInfo() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("重试")
                        }
                    }
                }
            }
            
            // 底部间距
            Spacer(modifier = Modifier.height(80.dp))
        }
        
        // Snackbar主机
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp) // 确保不被底部导航栏遮挡
        )
    }
}

@Composable
fun ListItemWithIcon(
    title: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color = MaterialTheme.colorScheme.primary,
    onClick: (() -> Unit)? = null
) {
    val modifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }
    
    ListItem(
        headlineContent = { 
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
        },
        supportingContent = {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint
            )
        },
        modifier = modifier
    )
}

@Composable
fun FileOperationsDialog(
    onDismiss: () -> Unit,
    onCreateFolder: () -> Unit,
    onUploadPhoto: () -> Unit,
    onUploadFile: () -> Unit
) {
    // 选择的选项
    var selectedOption by remember { mutableStateOf<String?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("文件操作") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // 新建文件夹选项
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedOption = "createFolder" }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedOption == "createFolder",
                        onClick = { selectedOption = "createFolder" }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "新建文件夹",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                
                // 上传照片选项
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedOption = "uploadPhoto" }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedOption == "uploadPhoto",
                        onClick = { selectedOption = "uploadPhoto" }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "上传照片",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                
                // 上传文件选项
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedOption = "uploadFile" }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedOption == "uploadFile",
                        onClick = { selectedOption = "uploadFile" }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "上传文件",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when (selectedOption) {
                        "createFolder" -> onCreateFolder()
                        "uploadPhoto" -> onUploadPhoto()
                        "uploadFile" -> onUploadFile()
                        else -> onDismiss()
                    }
                },
                enabled = selectedOption != null
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun CreateFolderDialog(
    onDismiss: () -> Unit,
    onCreateFolder: (String) -> Unit
) {
    var folderName by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建文件夹") },
        text = {
            Column {
                Text(
                    text = "请输入文件夹名称",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { 
                        folderName = it
                        isError = it.isBlank() 
                    },
                    label = { Text("文件夹名称") },
                    singleLine = true,
                    isError = isError,
                    supportingText = {
                        if (isError) {
                            Text(
                                text = "文件夹名称不能为空",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (folderName.isBlank()) {
                        isError = true
                    } else {
                        onCreateFolder(folderName)
                    }
                }
            ) {
                Text("创建")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun UploadProgressDialog(
    uploadingState: UploadingState,
    onDismiss: () -> Unit
) {
    if (uploadingState is UploadingState.Uploading) {
        AlertDialog(
            onDismissRequest = { /* 不允许用户取消上传进度对话框 */ },
            title = { 
                if (uploadingState.total > 1) {
                    Text("正在上传 (${uploadingState.current}/${uploadingState.total})")
                } else {
                    Text("正在上传")
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("正在上传: ${uploadingState.fileName}")
                    
                    if (uploadingState.total > 1) {
                        Text(
                            text = "文件 ${uploadingState.current} / ${uploadingState.total}",
                            modifier = Modifier.padding(top = 4.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    LinearProgressIndicator(
                        progress = { uploadingState.progress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                    )
                    
                    Text(
                        text = "${uploadingState.progress}%",
                        modifier = Modifier.padding(top = 8.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {},  // 上传过程中没有确认按钮
            dismissButton = {}   // 上传过程中没有取消按钮
        )
    }
}

// 添加分享选项对话框
@Composable
fun ShareOptionsDialog(
    onDismiss: () -> Unit,
    onConfirm: (ShareOption) -> Unit,
    shareOptions: List<ShareOption>
) {
    var selectedOption by remember { mutableStateOf<ShareOption?>(shareOptions.firstOrNull()) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择分享方式") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "选择要创建的共享链接类型：",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                shareOptions.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedOption = option }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedOption == option,
                            onClick = { selectedOption = option }
                        )
                        
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 8.dp)
                        ) {
                            Text(
                                text = option.label,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            
                            Text(
                                text = option.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    selectedOption?.let { onConfirm(it) }
                },
                enabled = selectedOption != null
            ) {
                Text("创建链接")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

// 添加共享链接对话框
@Composable
fun ShareLinkDialog(
    url: String,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("分享链接") },
        text = { 
            Column {
                Text("已创建共享链接，可供他人访问。")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "分享连接是永久且公开的，任何人都可以访问，请谨慎使用。",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(8.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                // 显示链接（自动省略过长的链接）
                Text(
                    text = url,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(8.dp)
                )
            }
        },
        confirmButton = {
            Row {
                // 复制按钮
                TextButton(
                    onClick = onCopy
                ) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("复制")
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // 分享按钮
                TextButton(
                    onClick = onShare
                ) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("分享")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoveItemBottomSheetContent(
    item: DriveItem,
    folders: List<DriveItem>,
    isLoading: Boolean,
    currentPath: List<DriveItem>,
    onConfirm: (destinationFolderId: String) -> Unit,
    onFolderClick: (folder: DriveItem) -> Unit,
    onNavigateUp: () -> Unit
) {
    var selectedFolderId by remember { mutableStateOf<String?>(null) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // 标题
        Text(
            text = "移动 ${item.name}",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // 显示当前路径
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "当前位置: ",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (currentPath.isEmpty()) "根目录" 
                       else currentPath.joinToString(" / ") { it.name },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
        // "向上"按钮
        if (currentPath.isNotEmpty()) {
            ListItem(
                headlineContent = { Text("") },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = "向上",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                modifier = Modifier.clickable { onNavigateUp() }
            )
        }
        
        // 加载状态
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (folders.isEmpty() && currentPath.isEmpty()) {
            // 空状态
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("没有可用的文件夹")
            }
        } else {
            // 文件夹列表
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // 添加根目录选项（仅在非根目录显示）
                if (currentPath.isEmpty()) {
                    item {
                        ListItem(
                            headlineContent = { Text("根目录") },
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingContent = {
                                RadioButton(
                                    selected = selectedFolderId == "root",
                                    onClick = { selectedFolderId = "root" }
                                )
                            },
                            modifier = Modifier.clickable { selectedFolderId = "root" }
                        )
                    }
                }
                
                // 添加当前文件夹选项
                if (currentPath.isNotEmpty()) {
                    item {
                        ListItem(
                            headlineContent = { Text("当前文件夹（${currentPath.last().name}）") },
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingContent = {
                                RadioButton(
                                    selected = selectedFolderId == currentPath.last().id,
                                    onClick = { selectedFolderId = currentPath.last().id }
                                )
                            },
                            modifier = Modifier.clickable { selectedFolderId = currentPath.last().id }
                        )
                    }
                }
                
                // 添加子文件夹
                items(folders) { folder ->
                    ListItem(
                        headlineContent = { 
                            Text(
                                text = folder.name,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingContent = {
                            Row {
                                // 选择该文件夹的单选按钮
                                RadioButton(
                                    selected = selectedFolderId == folder.id,
                                    onClick = { selectedFolderId = folder.id }
                                )
                                
                                // 进入文件夹按钮
                                IconButton(onClick = { onFolderClick(folder) }) {
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = "进入文件夹"
                                    )
                                }
                            }
                        },
                        modifier = Modifier.clickable { selectedFolderId = folder.id }
                    )
                }
            }
        }
        
        // 底部按钮
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = { 
                    onConfirm("root") 
                },
                enabled = false
            ) {
                Text("取消")
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            TextButton(
                onClick = {
                    selectedFolderId?.let { onConfirm(it) }
                },
                enabled = selectedFolderId != null
            ) {
                Text("移动")
            }
        }
        
        // 添加底部间距，避免导航栏遮挡
        Spacer(modifier = Modifier.height(32.dp))
    }
}

// 添加复制项目的底部表单内容
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CopyItemBottomSheetContent(
    item: DriveItem,
    folders: List<DriveItem>,
    isLoading: Boolean,
    currentPath: List<DriveItem>,
    onConfirm: (destinationFolderId: String) -> Unit,
    onDirectCopy: (destinationFolderId: String) -> Unit,
    onFolderClick: (folder: DriveItem) -> Unit,
    onNavigateUp: () -> Unit
) {
    var selectedFolderId by remember { mutableStateOf<String?>(null) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // 标题
        Text(
            text = "复制 ${item.name}",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // 显示当前路径
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "当前位置: ",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = if (currentPath.isEmpty()) "根目录" 
                       else currentPath.joinToString(" / ") { it.name },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
        
        // "向上"按钮
        if (currentPath.isNotEmpty()) {
            ListItem(
                headlineContent = { Text("") },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = "向上",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                modifier = Modifier.clickable { onNavigateUp() }
            )
        }
        
        // 加载状态
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (folders.isEmpty() && currentPath.isEmpty()) {
            // 空状态
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("没有可用的文件夹")
            }
        } else {
            // 文件夹列表
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // 添加根目录选项（仅在非根目录显示）
                if (currentPath.isEmpty()) {
                    item {
                        ListItem(
                            headlineContent = { Text("根目录") },
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingContent = {
                                RadioButton(
                                    selected = selectedFolderId == "root",
                                    onClick = { selectedFolderId = "root" }
                                )
                            },
                            modifier = Modifier.clickable { selectedFolderId = "root" }
                        )
                    }
                }
                
                // 添加当前文件夹选项
                if (currentPath.isNotEmpty()) {
                    item {
                        ListItem(
                            headlineContent = { Text("当前文件夹（${currentPath.last().name}）") },
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingContent = {
                                RadioButton(
                                    selected = selectedFolderId == currentPath.last().id,
                                    onClick = { selectedFolderId = currentPath.last().id }
                                )
                            },
                            modifier = Modifier.clickable { selectedFolderId = currentPath.last().id }
                        )
                    }
                }
                
                // 添加子文件夹
                items(folders) { folder ->
                    ListItem(
                        headlineContent = { 
                            Text(
                                text = folder.name,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingContent = {
                            Row {
                                // 选择该文件夹的单选按钮
                                RadioButton(
                                    selected = selectedFolderId == folder.id,
                                    onClick = { selectedFolderId = folder.id }
                                )
                                
                                // 进入文件夹按钮
                                IconButton(onClick = { onFolderClick(folder) }) {
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = "进入文件夹"
                                    )
                                }
                            }
                        },
                        modifier = Modifier.clickable { selectedFolderId = folder.id }
                    )
                }
            }
        }
        
        // 底部按钮
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = { 
                    onDirectCopy("root") 
                },
                enabled = false
            ) {
                Text("取消")
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // 直接复制按钮（不重命名）
            TextButton(
                onClick = {
                    selectedFolderId?.let { onDirectCopy(it) }
                },
                enabled = selectedFolderId != null
            ) {
                Text("直接复制")
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // 使用新名称复制按钮
            TextButton(
                onClick = {
                    selectedFolderId?.let { onConfirm(it) }
                },
                enabled = selectedFolderId != null
            ) {
                Text("复制并重命名")
            }
        }
        
        // 添加底部间距，避免导航栏遮挡
        Spacer(modifier = Modifier.height(32.dp))
    }
}

// 添加重命名对话框
@Composable
fun RenameForCopyDialog(
    originalName: String,
    onDismiss: () -> Unit,
    onConfirm: (newName: String, destinationFolderId: String) -> Unit
) {
    var newName by remember { mutableStateOf(generateCopyName(originalName)) }
    var isError by remember { mutableStateOf(false) }
    var destinationFolderId by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("重命名副本") },
        text = {
            Column {
                Text(
                    text = "请为副本输入一个新名称",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                OutlinedTextField(
                    value = newName,
                    onValueChange = { 
                        newName = it
                        isError = it.isBlank() 
                    },
                    label = { Text("副本名称") },
                    singleLine = true,
                    isError = isError,
                    supportingText = {
                        if (isError) {
                            Text(
                                text = "文件名不能为空",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (newName.isBlank()) {
                        isError = true
                    } else {
                        onConfirm(newName, destinationFolderId)
                    }
                }
            ) {
                Text("确认")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

// 生成副本名称的工具函数
fun generateCopyName(originalName: String): String {
    val extension = if (originalName.contains(".")) {
        "." + originalName.substringAfterLast(".")
    } else {
        ""
    }
    
    val nameWithoutExtension = if (extension.isNotEmpty()) {
        originalName.substringBeforeLast(".")
    } else {
        originalName
    }
    
    // 检查是否已经包含"的副本"
    return if (nameWithoutExtension.contains("的副本")) {
        val pattern = Regex("""的副本(\s\(\d+\))?$""")
        val result = pattern.find(nameWithoutExtension)
        
        if (result != null) {
            // 已有"的副本"或"的副本 (数字)"
            val countMatch = Regex("""\(\d+\)$""").find(nameWithoutExtension)
            
            if (countMatch != null) {
                // 有数字，递增
                val currentCount = countMatch.groupValues[1].toInt()
                val newCount = currentCount + 1
                val newName = nameWithoutExtension.replace(Regex("""\(\d+\)$"""), "($newCount)")
                "$newName$extension"
            } else {
                // 没有数字，添加 (2)
                "$nameWithoutExtension (2)$extension"
            }
        } else {
            // 没有"的副本"模式，添加
            "$nameWithoutExtension 的副本$extension"
        }
    } else {
        // 没有"的副本"，添加
        "$nameWithoutExtension 的副本$extension"
    }
}