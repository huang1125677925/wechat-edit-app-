package com.wechat.editor.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.wechat.editor.viewmodel.ArticleListViewModel
import com.wechat.editor.viewmodel.EditorViewModel

sealed class Screen(val route: String) {
    object ArticleList : Screen("article_list")
    object Editor : Screen("editor/{articleId}") {
        fun createRoute(articleId: String = "new") = "editor/$articleId"
    }
}

@Composable
fun WeChatEditorNavGraph(
    navController: NavHostController,
    articleListViewModel: ArticleListViewModel = viewModel(),
    editorViewModel: EditorViewModel = viewModel()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.ArticleList.route
    ) {
        composable(Screen.ArticleList.route) {
            ArticleListScreen(
                viewModel = articleListViewModel,
                onNewArticle = {
                    editorViewModel.loadNewArticle()
                    navController.navigate(Screen.Editor.createRoute("new"))
                },
                onOpenArticle = { article ->
                    editorViewModel.loadArticle(article)
                    navController.navigate(Screen.Editor.createRoute(article.id))
                }
            )
        }

        composable(
            route = Screen.Editor.route,
            arguments = listOf(
                navArgument("articleId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val articleId = backStackEntry.arguments?.getString("articleId") ?: "new"
            LaunchedEffect(articleId) {
                editorViewModel.onEditorSessionStarted(articleId)
            }
            EditorScreen(
                viewModel = editorViewModel,
                onSave = { article ->
                    articleListViewModel.saveArticle(article)
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
