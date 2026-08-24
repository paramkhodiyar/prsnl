package com.prsnl.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.prsnl.core.subscription.MockSubscriptionRepositoryImpl
import com.prsnl.document.repository.FolderRepository
import com.prsnl.document.repository.NotebookRepository
import com.prsnl.ui.editor.PageEditorScreen
import com.prsnl.ui.editor.PageEditorViewModel
import com.prsnl.ui.folder.FolderDetailScreen
import com.prsnl.ui.home.HomeScreen
import com.prsnl.ui.home.HomeViewModel
import kotlinx.coroutines.launch

@Composable
fun PrsnlAppNavHost(
    notebookRepository: NotebookRepository,
    folderRepository: FolderRepository
) {
    val navController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()
    val subscriptionRepository = remember { MockSubscriptionRepositoryImpl() }

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
            val homeViewModel = remember { HomeViewModel(notebookRepository, folderRepository, subscriptionRepository) }
            HomeScreen(
                viewModel = homeViewModel,
                onFolderClick = { folderName ->
                    navController.navigate("folder_detail/$folderName")
                },
                onNotebookClick = { notebookId ->
                    val cachedNb = homeViewModel.notebooks.value.find { it.id == notebookId }
                    if (cachedNb != null) {
                        val route = if (cachedNb.coverStyle == "PDF") "pdf_reader/$notebookId" else "page_editor/$notebookId"
                        navController.navigate(route)
                    } else {
                        // Not yet present in the cached notebooks list — this happens right
                        // after import, before the reactive notebooks Flow has re-emitted.
                        // Fall back to a direct one-off lookup instead of guessing.
                        coroutineScope.launch {
                            val freshNb = notebookRepository.getNotebookById(notebookId)
                            val route = if (freshNb?.coverStyle == "PDF") "pdf_reader/$notebookId" else "page_editor/$notebookId"
                            navController.navigate(route)
                        }
                    }
                },
                onNavigateToPaywall = {
                    navController.navigate("paywall")
                },
                onNavigateToAuth = {
                    navController.navigate("auth")
                }
            )
        }

        // Auth & Multi-Device Sync Screen
        composable("auth") {
            val authViewModel: com.prsnl.ui.auth.AuthViewModel = androidx.hilt.navigation.compose.hiltViewModel()
            com.prsnl.ui.auth.AuthScreen(
                viewModel = authViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }


        // Level 2: Notebooks Grid inside Folder
        composable(
            route = "folder_detail/{folderName}",
            arguments = listOf(navArgument("folderName") { type = NavType.StringType })
        ) { backStackEntry ->
            val folderName = backStackEntry.arguments?.getString("folderName") ?: "Personal"
            val homeViewModel = remember { HomeViewModel(notebookRepository, folderRepository, subscriptionRepository) }
            FolderDetailScreen(
                folderName = folderName,
                viewModel = homeViewModel,
                onBackClick = { navController.popBackStack() },
                onNotebookClick = { notebookId ->
                    val cachedNb = homeViewModel.notebooks.value.find { it.id == notebookId }
                    if (cachedNb != null) {
                        val route = if (cachedNb.coverStyle == "PDF") "pdf_reader/$notebookId" else "page_editor/$notebookId"
                        navController.navigate(route)
                    } else {
                        // Not yet present in the cached notebooks list — this happens right
                        // after import, before the reactive notebooks Flow has re-emitted.
                        // Fall back to a direct one-off lookup instead of guessing.
                        coroutineScope.launch {
                            val freshNb = notebookRepository.getNotebookById(notebookId)
                            val route = if (freshNb?.coverStyle == "PDF") "pdf_reader/$notebookId" else "page_editor/$notebookId"
                            navController.navigate(route)
                        }
                    }
                },
                onNavigateToPaywall = {
                    navController.navigate("paywall")
                }
            )
        }

        // Level 3: Printable A4 Writing Editor inside Notebook
        composable(
            route = "page_editor/{pageId}",
            arguments = listOf(navArgument("pageId") { type = NavType.StringType })
        ) { backStackEntry ->
            val pageId = backStackEntry.arguments?.getString("pageId") ?: ""
            val editorViewModel = PageEditorViewModel(notebookRepository, pageId)
            PageEditorScreen(
                viewModel = editorViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        // Level 4: PDF Reader & Stylus Markup Screen
        composable(
            route = "pdf_reader/{pageId}",
            arguments = listOf(navArgument("pageId") { type = NavType.StringType })
        ) { backStackEntry ->
            val pageId = backStackEntry.arguments?.getString("pageId") ?: ""
            val editorViewModel = PageEditorViewModel(notebookRepository, pageId)
            com.prsnl.ui.pdf.PdfReaderScreen(
                viewModel = editorViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        // Level 5: Paywall & Subscription Screen
        composable("paywall") {
            com.prsnl.ui.subscription.PaywallScreen(
                onDismiss = { navController.popBackStack() },
                onPurchasePlan = { plan ->
                    coroutineScope.launch {
                        subscriptionRepository.purchasePlan(plan)
                        navController.popBackStack()
                    }
                },
                onRestorePurchases = {
                    coroutineScope.launch {
                        subscriptionRepository.restorePurchases()
                        navController.popBackStack()
                    }
                }
            )
        }
    }
}
