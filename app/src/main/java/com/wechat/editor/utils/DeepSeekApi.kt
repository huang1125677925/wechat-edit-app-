package com.wechat.editor.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * OpenAI-compatible DeepSeek API (https://api.deepseek.com/v1/chat/completions).
 * Model: DeepSeek V4 Flash — typography-only Markdown normalization (no wording changes).
 */
object DeepSeekApi {

    private const val BASE_URL = "https://api.deepseek.com/v1/chat/completions"
    private const val MODEL = "deepseek-v4-flash"

    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    sealed class Result {
        data class Success(val markdown: String) : Result()
        data class Error(val message: String) : Result()
    }

    suspend fun polishArticleMarkdown(
        apiKey: String,
        title: String,
        author: String,
        bodyMarkdown: String
    ): Result = withContext(Dispatchers.IO) {
        val key = apiKey.trim()
        if (key.isEmpty()) {
            return@withContext Result.Error("请先在设置中填写 DeepSeek API Key")
        }

        val systemPrompt = """
            你是中文 Markdown 的「纯排版」助手。用户会提供标题、作者和正文（Markdown）。

            【最高优先级：不得改动正文语义与用词】
            - 禁止润色、改写、换同义词、增删句子、调整语序、概括或扩写。
            - 每个列表项、段落里「说了什么」必须与原文一致；只能改标点、空白、Markdown 结构与换行分段。
            - 标题与作者仅供上下文；只输出处理后的正文 Markdown 全文，不要重复输出「标题：」「作者：」等元信息块。

            【必须执行的引号与加粗规则（优先级仅次于语义保留）】
            规则 A：列表项开头的「小标题关键词」必须加粗，且统一用直角引号「…」包裹，不得使用英文双引号 "…" 或弯引号 "…"。
              - 识别方式：列表项以 `- "xxx"：` 或 `- "xxx"：` 或 `- xxx：` 形式开头，xxx 即小标题关键词。
              - 转换规则：将其统一改写为 `- **「xxx」**：` 形式（加粗 + 直角引号，冒号紧跟其后，冒号后内容原文照抄）。
              - 示例（原文 → 排版后）：
                  原：- "严禁手写文档"：他认为写文档的时代已结束……
                  后：- **「严禁手写文档」**：他认为写文档的时代已结束……
                  原：- "软件时代终结"：断言软件时代已彻底终结……
                  后：- **「软件时代终结」**：断言软件时代已彻底终结……
                  原：- 提出"AIQ"新指标：未来企业核心指标……
                  后：- **「提出「AIQ」新指标」**：未来企业核心指标……
                    （若小标题内部还有英文缩写被双引号包围，内层改用单书名号『…』或直接去掉引号，保证字面不变）
            规则 B：正文段落内的强调词（被 "…" 或 "…" 包裹的词组）若不是小标题，改用直角引号「…」，不加粗。
            规则 C：**…** 内部绝对不能再出现英文双引号 " 或弯引号 " "；若出现，按规则 A 示例转换。

            【Markdown 语法修正（必须执行）】
            以下问题属于语法错误，必须修正，字词内容照抄不变：
            M1. 标题行：`#` 后必须有且仅有一个半角空格，例如 `## 二级标题`；缺少空格或多余空格均需修正。
            M2. 列表项：`-` 或 `*` 或数字 `.` 后必须有一个半角空格；紧跟文字无空格的要补上。
            M3. 有序列表序号必须连续从 1 开始（1. 2. 3.），不能跳号或全部写成 1.。
            M4. 加粗/斜体：`**文字**` 与 `*文字*` 的星号内侧不能有多余空格；`** 文字 **` 需改为 `**文字**`。
            M5. 段落间距：两个独立段落之间必须有且仅有一个空行；连续多个空行压缩为一个。
            M6. 代码围栏：开启行（` ``` `）与关闭行（` ``` `）必须单独成行，不能与文字混写在同一行。
            M7. 链接/图片：`[文字](url)` 与 `![alt](url)` 语法不完整（缺括号、括号不匹配）的要补全；url 本身照抄不改。
            M8. 引用块：`>` 后需有一个半角空格；多级引用 `>>` 保留不变。
            M9. 水平分隔线：只允许 `---`、`***`、`___` 单独成行；`--` 或 `——` 不是合法分隔线，原样保留或视上下文保持原意。

            【其他格式规范】
            - 中英文、中文与数字之间按常见规范补半角空格（如「AI 商数」「KPI/OKR」与两侧中文之间），不改动字母与数字本身。
            - 中文叙述优先全角标点（，。：；？！）；列表项内说明性冒号用全角「：」；半角标点仅保留在 URL、代码、纯英文片段处。
            - 列表、标题层级（#）、引用（>）、空行分段：仅在明显利于渲染且不改变阅读顺序时调整。
            - 保留链接 ![](…) `代码` 行内代码与代码围栏；不要改 URL 与代码字面量。

            【输出】
            - 不要前言、不要解释；只输出处理后的正文 Markdown 字符串（与输入同一语言与信息量）。
        """.trimIndent()

        val userPayload = buildString {
            append("标题：\n")
            append(title.ifBlank { "（无）" })
            append("\n\n作者：\n")
            append(author.ifBlank { "（无）" })
            append("\n\n正文 Markdown：\n")
            append(bodyMarkdown)
        }

        val root = JSONObject().apply {
            put("model", MODEL)
            put("temperature", 0.2)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userPayload)
                })
            })
        }

        val request = Request.Builder()
            .url(BASE_URL)
            .addHeader("Authorization", "Bearer $key")
            .addHeader("Content-Type", "application/json")
            .post(root.toString().toRequestBody(jsonMedia))
            .build()

        runCatching {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val errMsg = runCatching {
                        JSONObject(responseBody).optJSONObject("error")?.optString("message")
                    }.getOrNull()
                    return@use Result.Error(
                        errMsg?.takeIf { it.isNotBlank() }
                            ?: "请求失败 HTTP ${response.code}"
                    )
                }
                val json = JSONObject(responseBody)
                val choice = json.optJSONArray("choices")?.optJSONObject(0)
                val content = choice?.optJSONObject("message")?.optString("content")
                    ?: return@use Result.Error("响应格式异常")
                val trimmed = content.trim()
                if (trimmed.isEmpty()) {
                    return@use Result.Error("模型返回为空")
                }
                Result.Success(trimmed)
            }
        }.getOrElse { e ->
            Result.Error(e.localizedMessage ?: e.message ?: "网络错误")
        }
    }

    /**
     * Writes a WeChat-style AI industry digest in Markdown from headline list + URLs.
     * The model may summarize and group topics; it must not invent specific facts not implied by the titles.
     */
    suspend fun writeAiNewsDigestMarkdown(
        apiKey: String,
        feedGeneratedAt: String,
        windowHours: Int,
        itemCount: Int,
        inputLines: String
    ): Result = withContext(Dispatchers.IO) {
        val key = apiKey.trim()
        if (key.isEmpty()) {
            return@withContext Result.Error("请先在设置中填写 DeepSeek API Key")
        }
        if (inputLines.isBlank()) {
            return@withContext Result.Error("没有可用的资讯条目")
        }

        val systemPrompt = """
            你是中文科技公众号编辑。用户会提供一批「AI / 科技」相关资讯的标题与链接（来自第三方聚合源；内容仅为标题级信息，链接只用于理解来源，不用于正文展示）。

            【任务】
            根据这些标题与链接，写一篇适合微信公众号长文排版的 **Markdown 正文**（不是 HTML），核心目标是做全面、可读的综合总结。

            【写作要求】
            - 语言：自然、专业的中文，适合一般科技读者；语气冷静，避免营销腔与标题党夸张。
            - 结构：开头用 1 段话概括本期整体动向；正文按主题分节（用 ## 二级标题），每节下用有序或无序列表组织要点；要点应以总结、归纳和对比为主，不要堆砌原始标题。
            - 覆盖面：尽量全面覆盖用户提供的条目。明显相关的条目可以合并成一个更完整的要点；孤立但重要的条目放入「其他值得关注的动态」类小节并做简短总结。
            - 列表格式：每个要点都必须以 `- ` 或 `1. ` 开头；每个要点用 1-3 句话说明「发生了什么、体现了什么趋势、为什么值得关注」。
            - 链接处理：正文不要输出 Markdown 链接、裸 URL、来源链接列表，也不要写「详情请点击链接」「点击原文」「参考链接」「相关阅读」等引导点击的内容。
            - 信息边界：你**没有**文章全文，只有标题。可以归纳「行业在关注哪些方向」、对明显相关的条目做**温和**合并，**禁止**捏造具体数据、公司行为、未在标题中出现的产品细节。不确定时写「据标题/报道方向」等弱化表述，并尽量基于标题做保守总结。

            【禁止】
            - 不要输出 YAML front matter、不要输出「```」包裹的整篇代码块包裹全文（行内 `代码` 很少需要可不用）。
            - 不要输出任何「详情请点击链接」「点击查看原文」「更多请看链接」之类的句子。
            - 不要重复输出大段完全相同的标题或来源列表。
            - 不要使用 `•`、`·` 等纯文本项目符号；统一使用合法 Markdown 列表标记。
            - 不要输出与写作无关的前言（如「好的，这是文章」）。

            【元信息（仅供参考，不要单独成行复述一整块）】
            聚合数据生成时间（UTC/上游）：$feedGeneratedAt；时间窗口：约 ${windowHours} 小时；条目约 ${itemCount} 条。
        """.trimIndent()

        val userPayload = buildString {
            append("请基于下列条目撰写正文 Markdown（从上往下尽可能覆盖重要条目；同一站点多条时可酌情合并，但不要遗漏明显 AI 相关热点；正文只写总结，不输出链接或引导点击链接的内容）：\n\n")
            append(inputLines)
        }

        val root = JSONObject().apply {
            put("model", MODEL)
            put("temperature", 0.5)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userPayload)
                })
            })
        }

        val request = Request.Builder()
            .url(BASE_URL)
            .addHeader("Authorization", "Bearer $key")
            .addHeader("Content-Type", "application/json")
            .post(root.toString().toRequestBody(jsonMedia))
            .build()

        runCatching {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val errMsg = runCatching {
                        JSONObject(responseBody).optJSONObject("error")?.optString("message")
                    }.getOrNull()
                    return@use Result.Error(
                        errMsg?.takeIf { it.isNotBlank() }
                            ?: "请求失败 HTTP ${response.code}"
                    )
                }
                val json = JSONObject(responseBody)
                val choice = json.optJSONArray("choices")?.optJSONObject(0)
                val content = choice?.optJSONObject("message")?.optString("content")
                    ?: return@use Result.Error("响应格式异常")
                val trimmed = content.trim()
                if (trimmed.isEmpty()) {
                    return@use Result.Error("模型返回为空")
                }
                Result.Success(trimmed)
            }
        }.getOrElse { e ->
            Result.Error(e.localizedMessage ?: e.message ?: "网络错误")
        }
    }

    /**
     * WeChat-style **market / finance** digest from headline list + URLs (Perps-news style feed).
     */
    suspend fun writeMarketNewsDigestMarkdown(
        apiKey: String,
        feedGeneratedAt: String,
        windowHours: Int,
        itemCount: Int,
        inputLines: String
    ): Result = withContext(Dispatchers.IO) {
        val key = apiKey.trim()
        if (key.isEmpty()) {
            return@withContext Result.Error("请先在设置中填写 DeepSeek API Key")
        }
        if (inputLines.isBlank()) {
            return@withContext Result.Error("没有可用的资讯条目")
        }

        val systemPrompt = """
            你是中文财经公众号编辑。用户会提供一批「宏观经济、资本市场、行业与公司」相关资讯的标题与链接（来自第三方聚合源；内容仅为标题级信息，可能夹杂少量非纯股市条目；链接只用于理解来源，不用于正文展示）。

            【任务】
            根据这些标题与链接，写一篇适合微信公众号长文排版的 **Markdown 正文**（不是 HTML），侧重**股市与金融市场可读性**和全面总结：宏观与政策、大类资产与流动性、行业与板块、公司层面若有明确线索可点到为止。

            【写作要求】
            - 语言：自然、专业的中文，适合一般投资者阅读；语气冷静，避免营销腔与标题党夸张；**不构成投资建议**，风险提示简短即可，不要重复堆砌。
            - 结构：开头用 1 段话概括本期市场整体关注点；正文按主题分节（用 ## 二级标题），每节下用有序或无序列表组织要点；要点应以总结、归纳和对比为主，不要堆砌原始标题。
            - 覆盖面：尽量全面覆盖用户提供的条目。明显相关的条目可以合并成一个更完整的要点；孤立但重要的条目放入「其他值得关注的动态」类小节并做简短总结。
            - 列表格式：每个要点都必须以 `- ` 或 `1. ` 开头；每个要点用 1-3 句话说明「发生了什么、影响哪类资产/板块、市场为什么关注」。
            - 链接处理：正文不要输出 Markdown 链接、裸 URL、来源链接列表，也不要写「详情请点击链接」「点击原文」「参考链接」「相关阅读」等引导点击的内容。
            - 信息边界：你**没有**文章全文，只有标题。可以归纳「市场在关注哪些方向」、对明显相关的条目做**温和**合并，**禁止**捏造具体数据、监管表态细节、未在标题中出现的价格或点位。不确定时写「据标题/报道方向」等弱化表述，并尽量基于标题做保守总结。

            【禁止】
            - 不要输出 YAML front matter、不要输出「```」包裹的整篇代码块包裹全文（行内 `代码` 很少需要可不用）。
            - 不要输出任何「详情请点击链接」「点击查看原文」「更多请看链接」之类的句子。
            - 不要重复输出大段完全相同的标题或来源列表。
            - 不要使用 `•`、`·` 等纯文本项目符号；统一使用合法 Markdown 列表标记。
            - 不要输出与写作无关的前言（如「好的，这是文章」）。

            【元信息（仅供参考，不要单独成行复述一整块）】
            数据整理时间（本机时区）：$feedGeneratedAt；时间窗口：约 ${windowHours} 小时；条目约 ${itemCount} 条。
        """.trimIndent()

        val userPayload = buildString {
            append("请基于下列条目撰写正文 Markdown（从上往下尽可能覆盖重要条目；同一来源多条时可酌情合并，但不要遗漏明显宏观或市场相关热点；正文只写总结，不输出链接或引导点击链接的内容）：\n\n")
            append(inputLines)
        }

        val root = JSONObject().apply {
            put("model", MODEL)
            put("temperature", 0.5)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userPayload)
                })
            })
        }

        val request = Request.Builder()
            .url(BASE_URL)
            .addHeader("Authorization", "Bearer $key")
            .addHeader("Content-Type", "application/json")
            .post(root.toString().toRequestBody(jsonMedia))
            .build()

        runCatching {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val errMsg = runCatching {
                        JSONObject(responseBody).optJSONObject("error")?.optString("message")
                    }.getOrNull()
                    return@use Result.Error(
                        errMsg?.takeIf { it.isNotBlank() }
                            ?: "请求失败 HTTP ${response.code}"
                    )
                }
                val json = JSONObject(responseBody)
                val choice = json.optJSONArray("choices")?.optJSONObject(0)
                val content = choice?.optJSONObject("message")?.optString("content")
                    ?: return@use Result.Error("响应格式异常")
                val trimmed = content.trim()
                if (trimmed.isEmpty()) {
                    return@use Result.Error("模型返回为空")
                }
                Result.Success(trimmed)
            }
        }.getOrElse { e ->
            Result.Error(e.localizedMessage ?: e.message ?: "网络错误")
        }
    }
}
