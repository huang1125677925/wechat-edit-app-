package com.wechat.editor.model

/**
 * Short example Markdown per template so the template picker can render a faithful HTML preview
 * (headings, quote, code, lists) without loading the user's draft.
 */
object TemplatePreviewSamples {

    fun sampleMarkdown(template: ArticleTemplate): String = when (template) {
        ArticleTemplate.CLASSIC_WECHAT -> """
# 示例标题：春日随笔

这是一段**公众号正文**示例，蓝边标题与清晰段落层次适合日常推送。

## 小节标题

- 要点一：结构清晰
- 要点二：阅读友好

> 引用：简洁排版让内容更有说服力。

文末一句总结，便于对照整体样式。
        """.trimIndent()

        ArticleTemplate.HS_MAGAZINE -> """
# 封面故事 · 城市与光

**编者按**：衬线大字与暖色纸张感，适合杂志风长文。

## 第一章

正文采用舒展行距与两端对齐，标题居中强调氛围感。

> 「排版是一种沉默的语气。」

---

##### 小结

留白与字体搭配决定沉浸感。
        """.trimIndent()

        ArticleTemplate.HS_TECH -> """
# 技术笔记 · Kotlin 协程入门

蓝绿点缀与强对比代码块，适合教程与工程文档。

## 异步示例

```kotlin
suspend fun load(): Data =
    withContext(Dispatchers.IO) { api.fetch() }
```

> **Tip**：注意线程切换与异常传播。

- `launch`：Fire-and-forget
- `async`：返回 `Deferred`
        """.trimIndent()

        ArticleTemplate.HS_AI_CODER -> """
# 长文与代码：模型上下文策略

适合 AI / 开发类文章：**强调色**与等宽代码统一视觉。

## 片段

```python
def clamp(x: float, lo: float, hi: float) -> float:
    return max(lo, min(hi, x))
```

> 引用块用于摘录论文观点或工具说明。

文末可附延伸阅读链接：`README` 与变更日志。
        """.trimIndent()

        ArticleTemplate.HS_DEEP_READ -> """
# 深度阅读版式示例

高对比、舒适留白，适合长文精读与严肃评论。

## 论证结构

第一段展开背景与问题意识；第二段提出核心论点，并用**加粗**标出关键词。

> 引用用于呈现对立观点或一手材料，分隔正文节奏。

第三段收束并指向后续讨论——整体层次依赖标题与间距而非花哨装饰。
        """.trimIndent()

        ArticleTemplate.HS_CLAUDE -> """
# 温和编辑 · 陶土色点缀

暖灰底与强调色块，适合温和叙述与产品思考。

## 章节

正文中的 **重点** 会以浅色底纹提示读者暂停思考。

> 引用块带有渐变与左侧色条，营造对话感。

---

一段收尾，观察标题与链接如何在同一调色板内协调。
        """.trimIndent()

        ArticleTemplate.HS_NYT -> """
# The Briefing — 新闻纸质感

Georgia 大标题与经典栏式正文，适合资讯与深度报道。

## Analysis

This sample mixes English and 中文 to show serif rhythm and hierarchy.

> “Good design recedes; the story comes forward.”

- Bullet one
- Bullet two

**Dateline** — 结尾可放署名或责任编辑说明。
        """.trimIndent()

        ArticleTemplate.HS_MEDIUM -> """
# Medium 风格长文示例

Georgia 标题与克制分隔线，接近平台上常见的长阅读体验。

## 第一节

段落开头避免臃肿装饰，依靠字距与行高建立节奏。

> 引用常常是全文的金句或摘要。

---

第二节可以继续展开，链接与加粗保持低调对比。
        """.trimIndent()

        ArticleTemplate.WECHAT_CYBER_ZEN -> """
# 赛博长文 · 暖灰与陶土红

暖灰底、**陶土红标题**，链接与代码样式贴近微信长文审美。

## 标注说明

正文混排 **强调** 与 `inline code`，下面是摘录式引用：

> 适合知识付费与专栏连载的节奏控制。

- 列表项保持紧凑
- 第二项可含 `术语`
        """.trimIndent()

        ArticleTemplate.DEFAULT -> """
# 默认样式预览

简洁清晰的默认排版：标题、正文与列表一目了然。

## 示例小节

这是普通段落，包含 **加粗** 与 `代码` 片段。

> 引用块展示左侧样式。

- 无序列表项 A
- 无序列表项 B
        """.trimIndent()

        ArticleTemplate.ELEGANT -> """
# 月色与回信

优雅文艺向：**情感类**与随笔散文可参考此行距与配色。

## 第二节

夜色落在窗台上，纸张摩擦的声音很轻。

> 引用适合诗句或书信摘录。

段落舒展，适合较长段落不换场。
        """.trimIndent()

        ArticleTemplate.TECH -> """
# 科技资讯短讯

科技简约：冷静配色与 GitHub 风格代码。

```bash
curl -s https://api.example.com/v1/status | jq .
```

> 一句话点评：适合速览与工具推荐。
        """.trimIndent()

        ArticleTemplate.BUSINESS -> """
# 季度复盘 · 战略与执行

商务专业：稳重标题与清晰层级，适合职场与管理类内容。

## 关键指标

1. 营收与毛利
2. 现金流与客户留存

> 执行摘要可放在引用块中。

**结论**：下一步聚焦核心产品线与客户成功。
        """.trimIndent()

        ArticleTemplate.LIFE -> """
# 周末厨房 · 一锅温热

生活休闲：暖色点缀与轻松语气。

## 食材准备

- 番茄两颗
- 鸡蛋三枚

> 小贴士：小火慢煮更出味。

盛盘前撒一点香草——这是属于周末的节奏。
        """.trimIndent()

        ArticleTemplate.EDUCATION -> """
# 课程导论 · 如何高效复习

教育学习：背景色块引用适合定义与要点。

## 本课目标

理解 **刻意练习** 与间隔复习的关系。

> **定义**：在舒适区边缘重复并即时反馈。

课后作业：整理三张知识卡片并自测。
        """.trimIndent()
    }
}
