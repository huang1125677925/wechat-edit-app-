package com.wechat.editor.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.wechat.editor.viewmodel.AiNewsChatViewModel
import com.wechat.editor.viewmodel.AiNewsDigestViewModel
import com.wechat.editor.viewmodel.ArticleListViewModel
import com.wechat.editor.viewmodel.EditorViewModel
import com.wechat.editor.viewmodel.SettingsViewModel

sealed class Screen(val route: String) {
    object ArticleList : Screen("article_list")
    object AiNewsDigest : Screen("ai_news_digest")
    object AiNewsChat : Screen("ai_news_chat")
    object Settings : Screen("settings")
    object Editor : Screen("editor/{articleId}") {
        fun createRoute(articleId: String = "new") = "editor/$articleId"
    }
}

@Composable
fun WeChatEditorNavGraph(
    navController: NavHostController,
    articleListViewModel: ArticleListViewModel = viewModel(),
    aiNewsDigestViewModel: AiNewsDigestViewModel = viewModel(),
    aiNewsChatViewModel: AiNewsChatViewModel = viewModel(),
    editorViewModel: EditorViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel()
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
                },
                onOpenSettings = { navController.navigate(Screen.Settings.route) },
                onOpenAiNewsDigest = { navController.navigate(Screen.AiNewsDigest.route) },
                onOpenAiNewsChat = { navController.navigate(Screen.AiNewsChat.route) }
            )
        }

        composable(Screen.AiNewsDigest.route) {
            AiNewsDigestScreen(
                viewModel = aiNewsDigestViewModel,
                onBack = { navController.popBackStack() },
                onDigestReady = { article ->
                    editorViewModel.loadArticle(article)
                    navController.navigate(Screen.Editor.createRoute("new")) {
                        popUpTo(Screen.ArticleList.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }

        composable(Screen.AiNewsChat.route) {
            AiNewsChatScreen(
                viewModel = aiNewsChatViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = settingsViewModel,
                onBack = { navController.popBackStack() }
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
