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
import dagger.hilt.android.AndroidEntryPoint
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
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.VideoLibrary
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import kotlin.text.toFloat
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import com.lurenjia534.nextonedrivev3.CloudViewModel.UploadingState
import kotlinx.coroutines.launch
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

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
        
        // 添加SnackbarHost
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp) // 确保不被底部导航栏遮挡
        )
    }
}

@Composable
fun FileListItem(
    driveItem: DriveItem,
    onClick: () -> Unit
) {
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
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent
        ),
        modifier = Modifier
            .clickable(onClick = onClick)
    )
//    Divider(
//        modifier = Modifier.padding(start = 56.dp),
//        thickness = 0.5.dp,
//        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
//    )
}

// 根据文件扩展名获取适当的图标
@Composable
fun getFileIcon(fileName: String): ImageVector {
    val extension = fileName.substringAfterLast('.', "").lowercase()
    
    return when {
        extension in listOf("jpg", "jpeg", "png", "gif", "bmp") -> 
            Icons.Outlined.Image
        extension in listOf("mp4", "avi", "mov", "wmv") -> 
            Icons.Outlined.VideoFile
        extension in listOf("mp3", "wav", "aac", "flac") -> 
            Icons.Outlined.AudioFile
        extension in listOf("pdf") -> 
            Icons.Outlined.PictureAsPdf
        extension in listOf("doc", "docx") -> 
            Icons.Outlined.Description
        extension in listOf("xls", "xlsx") -> 
            Icons.Outlined.TableChart
        extension in listOf("ppt", "pptx") -> 
            Icons.Outlined.Slideshow
        extension in listOf("zip", "rar", "7z") -> 
            Icons.Outlined.FolderZip
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
        size < mb -> String.format("%.2f KB", size / kb)
        size < gb -> String.format("%.2f MB", size / mb)
        else -> String.format("%.2f GB", size / gb)
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Profile card with user info
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(12.dp)
                            .size(32.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = accountName,
                        style = MaterialTheme.typography.titleLarge
                    )

                    Text(
                        text = driveInfo?.owner?.user?.email ?: "加载中...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                FilledIconButton(
                    onClick = { viewModel.refreshDriveInfo() },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "刷新"
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 错误信息
        AnimatedVisibility(visible = errorMessage != null) {
            errorMessage?.let {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // 主内容
        AnimatedVisibility(
            visible = !isLoading,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            if (driveInfo != null) {
                Column {
                    // 云盘基本信息卡片
                    OutlinedCard (
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Storage,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "云盘信息",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            InfoRow(
                                icon = Icons.Default.DriveFileRenameOutline,
                                label = "名称",
                                value = driveInfo!!.name
                            )
                            InfoRow(
                                icon = Icons.Default.Category,
                                label = "类型",
                                value = driveInfo!!.driveType
                            )

                            InfoRow(
                                icon = Icons.Default.Schedule,
                                label = "创建时间",
                                value = formatDate(driveInfo!!.createdDateTime ?: "")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 存储配额卡片
                    driveInfo!!.quota?.let { quota ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PieChart,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "存储配额",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                val usedPercentage = (quota.used.toFloat() / quota.total.toFloat()) * 100

                                // 进度指示器
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                        .height(32.dp)
                                ) {
                                    // 背景
                                    LinearProgressIndicator(
                                        progress = { 1f },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(16.dp)
                                            .align(Alignment.Center),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )

                                    // 前景
                                    LinearProgressIndicator(
                                        progress = { usedPercentage / 100 },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(16.dp)
                                            .align(Alignment.Center),
                                        color = when {
                                            usedPercentage > 90 -> MaterialTheme.colorScheme.error
                                            usedPercentage > 70 -> MaterialTheme.colorScheme.tertiary
                                            else -> MaterialTheme.colorScheme.primary
                                        },
                                    )

                                    // 百分比标签
                                    Text(
                                        text = "${String.format("%.1f", usedPercentage)}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontSize = 10.sp,
                                        modifier = Modifier
                                            .background(
                                                color = when {
                                                    usedPercentage > 90 -> MaterialTheme.colorScheme.error
                                                    usedPercentage > 70 -> MaterialTheme.colorScheme.tertiary
                                                    else -> MaterialTheme.colorScheme.primary
                                                },
                                                shape = CircleShape
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                            .align(Alignment.CenterEnd)
                                            .graphicsLayer {
                                                translationX = -16.dp.toPx()
                                            }
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // 存储信息
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    StorageInfoItem(
                                        label = "总容量",
                                        value = formatFileSize(quota.total),
                                        icon = Icons.Default.Storage,
                                        tint = MaterialTheme.colorScheme.primary
                                    )

                                    StorageInfoItem(
                                        label = "已使用",
                                        value = formatFileSize(quota.used),
                                        icon = Icons.Default.Save,
                                        tint = when {
                                            usedPercentage > 90 -> MaterialTheme.colorScheme.error
                                            usedPercentage > 70 -> MaterialTheme.colorScheme.tertiary
                                            else -> MaterialTheme.colorScheme.primary
                                        }
                                    )

                                    StorageInfoItem(
                                        label = "剩余空间",
                                        value = formatFileSize(quota.remaining),
                                        icon = Icons.Default.DataUsage,
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // 无数据状态
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
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

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { viewModel.refreshDriveInfo() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
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
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "正在加载云盘信息...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun InfoRow(icon: ImageVector, label: String, value: String?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.width(72.dp)
        )

        Text(
            text = value ?: "",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun StorageInfoItem(label: String, value: String, icon: ImageVector, tint: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = tint
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
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
            Button(
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