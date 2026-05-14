package com.wechat.editor.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wechat.editor.model.ArticleTemplate
import com.wechat.editor.ui.screens.PreviewPanel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.snapshotFlow

/**
 * Horizontal pager of live HTML previews — one page per [ArticleTemplate], using the current
 * article title, author, and Markdown body. Settling on a page applies that template to the editor.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ArticleTemplatePreviewSection(
    articleId: String,
    appliedTemplate: ArticleTemplate,
    previewHtml: (ArticleTemplate) -> String,
    onSettledTemplate: (ArticleTemplate) -> Unit,
    modifier: Modifier = Modifier
) {
    val templates = ArticleTemplate.entries

    key(articleId) {
        val initialPage = templates.indexOf(appliedTemplate).let { if (it < 0) 0 else it }
        val pagerState = rememberPagerState(
            initialPage = initialPage,
            pageCount = { templates.size }
        )

        LaunchedEffect(appliedTemplate) {
            val idx = templates.indexOf(appliedTemplate).let { if (it < 0) 0 else it }
            if (pagerState.currentPage != idx) {
                pagerState.scrollToPage(idx)
            }
        }

        LaunchedEffect(pagerState) {
            snapshotFlow { pagerState.settledPage }
                .distinctUntilChanged()
                .collect { page ->
                    onSettledTemplate(templates[page])
                }
        }

        Column(modifier = modifier.fillMaxWidth()) {
            Text(
                text = "排版模板 · 左右滑动切换，预览为当前正文实时渲染",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .height(200.dp)
                    .clip(RoundedCornerShape(10.dp))
            ) { page ->
                PreviewPanel(
                    htmlContent = previewHtml(templates[page]),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Text(
                text = templates[pagerState.settledPage].displayName,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 6.dp)
            )
        }
    }
}
