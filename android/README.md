# 辰萃写作 Android App

这是博客仓库的 Android 客户端，使用 Kotlin + Jetpack Compose 编写。网页编辑器仍然保留，Android 客户端作为同一仓库中的独立模块增量维护。

## 本地构建

需要 Android SDK（API 35）和 JDK 17：

```bash
cd android
./gradlew assembleDebug
```

APK 输出在 `app/build/outputs/apk/debug/app-debug.apk`。

## 使用

1. 安装 APK。
2. 点击“设置密钥”，填写 GitHub 用户名、目标仓库、分支和 Fine-grained token。
3. Token 只需首次输入；应用使用 Android Keystore 加密保存，不写入文章内容或网页存储。
4. 编写标题、描述、标签和 Markdown 正文，点击“发布到 GitHub”。

Token 建议只授予目标仓库的 `Contents: Read and write` 权限。应用直接调用 GitHub Contents API，把文章提交到 `src/content/posts/`。

## 自动 Release

`.github/workflows/android-release.yml` 在推送 `v*` tag 时运行，构建可直接安装的 debug APK，并自动创建 GitHub Release 上传 APK。例如：

```bash
git tag v0.1.0
git push origin v0.1.0
```

目前 Release 使用 debug 签名，适合个人安装和测试。若要提交 Google Play，需要后续把正式签名密钥放入 GitHub Actions Secrets，并增加 signed AAB 构建。
