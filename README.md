# 微信公众号文章编辑器

一款专为微信公众号内容创作者设计的移动应用，Android 端支持富文本编辑、多种排版模板和 HTML 导出；iOS 端已接入基础工程与 CI 产物构建。

## 功能特性

### 编辑器
- **Markdown 语法**支持（粗体、斜体、删除线、标题、列表、引用、代码块等）
- **富文本格式工具栏**，分为格式、段落、插入、模板四大功能区
- **撤销 / 重做**（最多 50 步历史记录）
- **字数统计**实时显示
- **实时 HTML 预览**（WebView 渲染）
- **一键复制 HTML** 代码到剪贴板，可直接粘贴到公众号后台

### 排版模板
| 模板 | 适用场景 |
|------|----------|
| 默认样式 | 简洁清晰的通用排版 |
| 优雅文艺 | 文学、情感类文章 |
| 科技简约 | 科技、资讯类文章 |
| 商务专业 | 商业、职场类文章 |
| 生活休闲 | 生活、美食类文章 |
| 教育学习 | 教育、知识类文章 |

### 文章管理
- 文章列表，支持关键词搜索
- 文章模板标签和字数显示
- 删除确认对话框

## 技术栈

- **语言**: Kotlin
- **UI 框架**: Jetpack Compose + Material Design 3
- **架构**: MVVM (ViewModel + StateFlow)
- **导航**: Navigation Compose
- **最低 SDK**: 24 (Android 7.0)
- **目标 SDK**: 34 (Android 14)
- **iOS 最低版本**: iOS 16.0

## Git 工作流与分支约定

在本地 **`main`** 与远程对齐（例如某次 PR 已合并进 `origin/main` 之后），再基于最新 `main` 新建功能分支：

```bash
git checkout main
git fetch origin
git pull origin main
```

新建分支时使用 **`cursor/`** 前缀，并以 **`-8153`** 作为后缀，中间为简短描述，例如：

```bash
git checkout -b cursor/work-from-main-8153
git push -u origin cursor/work-from-main-8153
```

说明：`git pull origin main` 后的本地 `main` 会包含已合并进远程主干的改动（例如合并后的 PR #12 等）。

## 构建方式

### 本地构建

```bash
# 克隆项目
git clone <repo-url>
cd wechat-edit-app

# 构建 Debug APK
./gradlew assembleDebug

# APK 路径：app/build/outputs/apk/debug/app-debug.apk
```

#### iOS

```bash
# 构建 iOS 模拟器 App
xcodebuild \
  -project ios/WeChatEditor.xcodeproj \
  -scheme WeChatEditor \
  -configuration Debug \
  -sdk iphonesimulator \
  -derivedDataPath build/ios/DerivedData \
  CODE_SIGNING_ALLOWED=NO \
  build

# 构建 iOS Release xcarchive（未签名）
xcodebuild archive \
  -project ios/WeChatEditor.xcodeproj \
  -scheme WeChatEditor \
  -configuration Release \
  -destination "generic/platform=iOS" \
  -archivePath build/ios/archive/WeChatEditor.xcarchive \
  CODE_SIGNING_ALLOWED=NO \
  SKIP_INSTALL=NO
```

### GitHub Actions 自动构建

推送代码后，GitHub Actions 会自动：

1. **Android Lint 检查 + 单元测试** — Android 构建触发
2. **Android Debug APK 构建** — PR、普通 push 或手动 debug 构建触发，Artifact 保留 30 天
3. **iOS Debug App 构建** — PR、普通 push 或手动 debug 构建触发，上传 simulator `.app` 压缩包，Artifact 保留 30 天
4. **Android Release APK 构建 + 发布** — 推送 `v*` 格式 tag 或手动 release 构建触发
5. **iOS Release Archive 构建 + 发布** — 推送 `v*` 格式 tag 或手动 release 构建触发，上传未签名 `.xcarchive` 压缩包

手动触发工作流时可以通过 `platform` 选择 `all`、`android` 或 `ios`，通过 `build_type` 选择 `debug` 或 `release`。

#### 触发正式发布

```bash
git tag v1.0.0
git push origin v1.0.0
```

#### 配置签名（可选）

iOS 会生成 `.xcarchive.zip`，用于后续接入 Apple 证书、导出 IPA 或上传 TestFlight。当前流水线默认不做 iOS 签名。

在仓库 Settings → Secrets 中添加：

| Secret 名称 | 说明 |
|-------------|------|
| `KEYSTORE_BASE64` | Base64 编码的 keystore 文件 |
| `KEYSTORE_PASSWORD` | Keystore 密码 |
| `KEY_ALIAS` | Key 别名 |
| `KEY_PASSWORD` | Key 密码 |

生成 Base64 keystore：
```bash
base64 -i release.keystore | tr -d '\n'
```

## 项目结构

```
app/src/main/java/com/wechat/editor/
├── MainActivity.kt                  # 应用入口
├── model/
│   ├── Article.kt                   # 文章数据模型 + 排版设置
│   └── EditorState.kt               # 编辑器状态模型
├── viewmodel/
│   ├── ArticleListViewModel.kt      # 文章列表 ViewModel
│   └── EditorViewModel.kt           # 编辑器 ViewModel
├── ui/
│   ├── theme/                       # Material 主题配置
│   ├── screens/
│   │   ├── ArticleListScreen.kt     # 文章列表界面
│   │   ├── EditorScreen.kt          # 编辑器界面
│   │   ├── PreviewScreen.kt         # HTML 预览界面
│   │   └── NavGraph.kt              # 导航图
│   └── components/
│       ├── FormatToolbar.kt         # 格式工具栏
│       ├── ColorPickerDialog.kt     # 颜色选择器
│       ├── LinkDialog.kt            # 链接插入对话框
│       └── TemplateDialog.kt        # 模板选择对话框
├── utils/
│   ├── HtmlGenerator.kt             # Markdown → HTML 转换器 + CSS 生成
│   └── ClipboardUtils.kt            # 剪贴板工具
└── ios/
    ├── WeChatEditor.xcodeproj       # iOS Xcode 工程
    └── WeChatEditor/                # SwiftUI iOS 应用源码
```
