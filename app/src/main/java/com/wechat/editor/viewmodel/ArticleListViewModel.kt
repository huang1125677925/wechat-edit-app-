package com.wechat.editor.viewmodel

import androidx.lifecycle.ViewModel
import com.wechat.editor.model.Article
import com.wechat.editor.model.ArticleTemplate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ArticleListViewModel : ViewModel() {

    private val _articles = MutableStateFlow<List<Article>>(emptyList())
    val articles: StateFlow<List<Article>> = _articles.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _showDeleteDialog = MutableStateFlow<Article?>(null)
    val showDeleteDialog: StateFlow<Article?> = _showDeleteDialog.asStateFlow()

    init {
        loadSampleArticles()
    }

    private fun loadSampleArticles() {
        _articles.value = listOf(
            Article(
                title = "微信公众号排版技巧分享",
                content = "好的排版可以让文章更易读，提高读者体验...",
                author = "编辑小助手",
                template = ArticleTemplate.ELEGANT,
                updatedAt = System.currentTimeMillis() - 3600000
            ),
            Article(
                title = "2024年公众号运营必知的10件事",
                content = "公众号运营是一项需要持续学习和实践的工作...",
                author = "运营达人",
                template = ArticleTemplate.BUSINESS,
                updatedAt = System.currentTimeMillis() - 86400000
            )
        )
    }

    fun saveArticle(article: Article) {
        _articles.update { list ->
            val index = list.indexOfFirst { it.id == article.id }
            if (index >= 0) {
                list.toMutableList().also { it[index] = article.copy(updatedAt = System.currentTimeMillis()) }
            } else {
                listOf(article.copy(updatedAt = System.currentTimeMillis())) + list
            }
        }
    }

    fun deleteArticle(article: Article) {
        _articles.update { list -> list.filter { it.id != article.id } }
        _showDeleteDialog.value = null
    }

    fun showDeleteConfirm(article: Article) {
        _showDeleteDialog.value = article
    }

    fun dismissDeleteDialog() {
        _showDeleteDialog.value = null
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun getFilteredArticles(): List<Article> {
        val query = _searchQuery.value
        return if (query.isBlank()) {
            _articles.value
        } else {
            _articles.value.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.content.contains(query, ignoreCase = true)
            }
        }
    }

    fun getArticleById(id: String): Article? {
        return _articles.value.find { it.id == id }
    }
}
