package com.lurenjia534.nextonedrivev3

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.VideoLibrary
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
import androidx.compose.ui.text.style.TextOverflow

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
                    contentPadding = PaddingValues(vertical = 8.dp)
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
    ) {
        // 顶部信息栏
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = accountName,
                    style = MaterialTheme.typography.titleLarge
                )
                
                Text(
                    text = driveInfo?.owner?.user?.email ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            IconButton(onClick = { viewModel.refreshDriveInfo() }) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "刷新"
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // 错误信息
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
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // 加载指示器或内容
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (driveInfo != null) {
            // 云盘基本信息卡片
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "云盘信息",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "名称: ${driveInfo?.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    
                    Text(
                        text = "类型: ${driveInfo?.driveType}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    
                    Text(
                        text = "创建时间: ${formatDate(driveInfo?.createdDateTime ?: "")}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 存储配额卡片
            driveInfo?.quota?.let { quota ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "存储配额",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        val usedPercentage = (quota.used.toFloat() / quota.total.toFloat()) * 100
                        
                        Text(
                            text = "已使用: ${formatFileSize(quota.used)} / ${formatFileSize(quota.total)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Text(
                            text = "剩余: ${formatFileSize(quota.remaining)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // 存储使用进度条
                        LinearProgressIndicator(
                            progress = { usedPercentage / 100 },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = if (usedPercentage > 90) {
                                MaterialTheme.colorScheme.error
                            } else if (usedPercentage > 70) {
                                MaterialTheme.colorScheme.tertiary
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = "已使用 ${String.format("%.1f", usedPercentage)}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            // 无数据状态
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "无法获取云盘信息",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
} 