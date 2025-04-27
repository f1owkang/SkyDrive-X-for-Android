# SkyDrive X for Android

[English](README) | [中文](README_zh.md)

![Version](https://img.shields.io/badge/version-2.2.0-blue)
![Platform](https://img.shields.io/badge/platform-Android-brightgreen)
![License](https://img.shields.io/badge/license-MIT-green)
![Language](https://img.shields.io/badge/language-Kotlin-orange)
![Stars](https://img.shields.io/github/stars/lurenjia534/NextOneDrivev3)
![Issues](https://img.shields.io/github/issues/lurenjia534/NextOneDrivev3)
![Last Commit](https://img.shields.io/github/last-commit/lurenjia534/NextOneDrivev3)
![Lines of code](https://img.shields.io/tokei/lines/github/lurenjia534/NextOneDrivev3)

## 目前项目仅支持自行构建

```bash
git clone https://github.com/lurenjia534/SkyDrive-X-for-Android
```

你需要在本项目的 ```AndroidManifest.xml``` 找到

```
<activity android:name="com.microsoft.identity.client.BrowserTabActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.VIEW" />
                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.BROWSABLE" />
                <data
                    android:host="com.lurenjia534.nextonedrivev2"
                    android:path="/<YOUR_BASE64_ENCODED_PACKAGE_SIGNATURE>"
                    android:scheme="msauth" />
            </intent-filter>
        </activity>
```

其中的 ```<YOUR_BASE64_ENCODED_PACKAGE_SIGNATURE>``` 替换为您的应用程序的签名证书的 Base64 编码哈希值

### 下一步

```json
{
  "client_id": "<YOUR_PACKAGE_NAME>",
  "redirect_uri": "msauth://com.lurenjia534.nextonedrivev2/<YOUR_BASE64_URL_ENCODED_PACKAGE_SIGNATURE>",
  "broker_redirect_uri_registered": true
}

```

其中的```<YOUR_PACKAGE_NAME>```是您的客户端ID
其中的 ```<YOUR_BASE64_URL_ENCODED_PACKAGE_SIGNATURE>``` 替换为您的应用程序的签名证书的 Base64 安全编码哈希值

### 下一步您需要配置自己的 Azure 应用程序

## 项目简介

SkyDrive X 是一款专为 Android 设计的 OneDrive 客户端应用。它提供了丰富的云端文件管理功能，支持多账户管理，以及现代化的 Material 3 设计风格，为用户带来流畅、直观的云存储体验。

## 功能特性

### 多账户管理
- 支持添加多个 OneDrive 账户
- 账户令牌自动刷新与手动刷新
- 账户信息管理与编辑

### 文件管理
- 浏览文件和文件夹层级结构
- 创建新文件夹
- 上传文件（支持多种文件类型）
- 上传照片（支持多选）
- 下载文件到本地
- 移动文件/文件夹
- 复制文件/文件夹（支持重命名）
- 删除文件/文件夹

### 共享与协作
- 创建文件/文件夹的共享链接
- 多种共享权限设置（只读/可编辑）
- 共享范围控制（匿名/组织内）
- 复制或直接分享链接

### 云盘信息
- 查看存储空间使用情况
- 账户配额信息
- 剩余空间显示

### 用户体验
- Material 3 设计语言
- 深色模式支持
- 流畅的动画和过渡效果
- 直观的文件类型图标

## 技术栈

- **编程语言**: Kotlin
- **UI 框架**: Jetpack Compose
- **架构模式**: MVVM
- **依赖注入**: Hilt
- **异步处理**: Kotlin 协程 + Flow
- **状态管理**: LiveData + StateFlow
- **网络请求**: Retrofit
- **权限管理**: Android 动态权限
- **设计规范**: Material 3
-
## 系统要求

- Android 12.0 (API 级别 31) 或更高
- 需要网络连接
- 需要存储权限（用于文件上传和下载）
- 需要通知权限（用于上传进度通知）

## 使用指南

### 添加账户
1. 启动应用后，点击右下角的 "+" 按钮
2. 输入账户名称（用于在应用内区分不同账户）
3. 点击确认后，将跳转到 Microsoft 登录页面
4. 登录 Microsoft 账户并授权应用访问 OneDrive

### 浏览和管理文件
- 点击账户卡片进入该账户的云盘
- 点击文件夹进入该文件夹
- 点击文件操作图标（⋮）打开文件操作菜单
- 使用底部的导航栏切换"文件列表"和"我的信息"页面

### 上传文件
1. 在文件列表页面，点击右下角"+"按钮
2. 选择"上传照片"或"上传文件"
3. 从设备选择要上传的文件
4. 查看上传进度条

### 共享文件
1. 在文件列表中，点击文件右侧的操作菜单
2. 选择"分享"选项
3. 选择共享权限和范围
4. 复制或分享生成的链接

## 自定义与设置

- 在"设置"页面可切换深色模式
- 自定义账户名称可在账户详情页完成

## 贡献指南

欢迎提交 Pull Request 或创建 Issue 来帮助改进项目。请遵循以下步骤：

1. Fork 项目
2. 创建功能分支 (`git checkout -b feature/amazing-feature`)
3. 提交更改 (`git commit -m 'Add some amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 创建 Pull Request

## 许可证

本项目采用 Apache LicenseVersion 2.0 许可证 - 详情请参阅 [LICENSE](LICENSE) 文件

## 联系方式

GitHub: [lurenjia534](https://github.com/lurenjia534)

---

*注：此应用为第三方非官方 OneDrive 客户端，非 Microsoft 官方产品。*

