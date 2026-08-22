package com.prsnl.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.prsnl.document.repository.NotebookRepository
import com.prsnl.ui.editor.PageEditorScreen
import com.prsnl.ui.editor.PageEditorViewModel
import com.prsnl.ui.folder.FolderDetailScreen
import com.prsnl.ui.home.HomeScreen
import com.prsnl.ui.home.HomeViewModel

@Composable
fun PrsnlAppNavHost(
    repository: NotebookRepository
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "home",
        enterTransition = { fadeIn() + slideInHorizontally(initialOffsetX = { 300 }) },
        exitTransition = { fadeOut() + slideOutHorizontally(targetOffsetX = { -300 }) },
        popEnterTransition = { fadeIn() + slideInHorizontally(initialOffsetX = { -300 }) },
        popExitTransition = { fadeOut() + slideOutHorizontally(targetOffsetX = { 300 }) }
    ) {
        // Level 1: Folders Grid
        composable("home") {
            val homeViewModel = HomeViewModel(repository)
            HomeScreen(
                viewModel = homeViewModel,
                onFolderClick = { folderName ->
                    navController.navigate("folder_detail/$folderName")
                }
            )
        }

        // Level 2: Notebooks Grid inside Folder
        composable(
            route = "folder_detail/{folderName}",
            arguments = listOf(navArgument("folderName") { type = NavType.StringType })
        ) { backStackEntry ->
            val folderName = backStackEntry.arguments?.getString("folderName") ?: "General"
            val homeViewModel = HomeViewModel(repository)
            FolderDetailScreen(
                folderName = folderName,
                viewModel = homeViewModel,
                onBackClick = { navController.popBackStack() },
                onNotebookClick = { notebookId ->
                    navController.navigate("page_editor/$notebookId")
                }
            )
        }

        // Level 3: Printable A4 Writing Editor inside Notebook
        composable(
            route = "page_editor/{pageId}",
            arguments = listOf(navArgument("pageId") { type = NavType.StringType })
        ) { backStackEntry ->
            val pageId = backStackEntry.arguments?.getString("pageId") ?: ""
            val editorViewModel = PageEditorViewModel(repository, pageId)
            PageEditorScreen(
                viewModel = editorViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
