# 微信公众号文章编辑器

一款专为微信公众号内容创作者设计的安卓应用，支持富文本编辑、多种排版模板和 HTML 导出。

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

### GitHub Actions 自动构建

推送代码后，GitHub Actions 会自动：

1. **Lint 检查 + 单元测试** — 所有分支 / PR 触发
2. **Debug APK 构建** — 推送到任意分支时触发，Artifact 保留 30 天
3. **Release APK 构建 + 发布** — 推送 `v*` 格式 tag 时触发，自动创建 GitHub Release

#### 触发正式发布

```bash
git tag v1.0.0
git push origin v1.0.0
```

#### 配置签名（可选）

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
└── utils/
    ├── HtmlGenerator.kt             # Markdown → HTML 转换器 + CSS 生成
    └── ClipboardUtils.kt            # 剪贴板工具
```
