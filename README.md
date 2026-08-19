# 密码本（Password Vault）

一款优先保护本地隐私的 Android 密码管理 App。V1 不需要账号、不连接服务器，密码库只以密文形式保存在手机内。

## V1 已实现

- 首次启动创建主密码，后续使用主密码解锁
- AES-256-GCM 加密整个密码库
- PBKDF2-HMAC-SHA256（310,000 次迭代）派生密钥
- 添加、编辑、删除和搜索密码条目
- 安全随机密码生成器
- 一键复制密码，30 秒后自动清空剪贴板
- 退到后台 2 分钟后自动锁定
- 禁止系统截图和最近任务缩略图泄露密码
- GitHub Actions 自动检查并生成 Debug APK

## 安全边界

- 主密码不会写入磁盘，也不会上传。
- 忘记主密码后，当前版本无法恢复密码库。
- 当前版本是本地单机版，尚未实现跨设备同步、备份恢复、浏览器自动填充和生物识别解锁。
- 这是个人项目的第一版，正式存放重要账号前应进行独立安全审计。

## 在 Android Studio 运行

1. 安装 Android Studio 和 Android SDK 36。
2. 克隆仓库并用 Android Studio 打开根目录。
3. 等待 Gradle 同步完成。
4. 连接 Android 8.0（API 26）或更高版本的手机，点击 **Run**。

也可以在安装了 JDK 17、Android SDK 36 和 Gradle 8.13 的终端中运行：

```bash
gradle assembleDebug
```

生成的 APK 位于：

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 从 GitHub 下载 APK

打开仓库的 **Actions** → 选择最新的 **Build Android APK** → 在页面底部下载 `password-vault-debug-apk` 构建产物。

## 技术栈

- Kotlin 2.4
- Jetpack Compose + Material 3
- Android Gradle Plugin 8.13
- 单 Activity、本地加密存储

## 下一阶段

- Android Keystore + 生物识别快速解锁
- 加密导出与恢复
- 系统 Autofill Service
- 文件夹、标签、收藏和密码健康检查
- 自动化测试与安全审计

