package com.example.ui.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.editor.EditorScreen
import com.example.ui.editor.EditorViewModel
import com.example.ui.home.HomeScreen
import com.example.ui.home.HomeViewModel
import com.example.ui.settings.SettingsScreen
import com.example.ui.settings.SettingsViewModel

private object Routes {
    const val HOME = "home"
    const val EDITOR = "editor?noteId={noteId}&template={template}"
    const val SETTINGS = "settings"

    fun editor(noteId: String? = null, template: String? = null): String {
        val params = buildList {
            if (noteId != null) add("noteId=$noteId")
            if (template != null) add("template=$template")
        }
        return if (params.isEmpty()) "editor" else "editor?" + params.joinToString("&")
    }
}

@Composable
fun MyNotesNavigation() {
    val navController = rememberNavController()
    val dur = 240

    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        enterTransition = {
            fadeIn(tween(dur, easing = FastOutSlowInEasing)) +
                scaleIn(tween(dur, easing = FastOutSlowInEasing), initialScale = 0.97f)
        },
        exitTransition = {
            fadeOut(tween(160, easing = FastOutSlowInEasing))
        },
        popEnterTransition = {
            fadeIn(tween(dur, easing = FastOutSlowInEasing)) +
                scaleIn(tween(dur, easing = FastOutSlowInEasing), initialScale = 0.97f)
        },
        popExitTransition = {
            fadeOut(tween(160, easing = FastOutSlowInEasing)) +
                scaleOut(tween(160, easing = FastOutSlowInEasing), targetScale = 0.97f)
        },
    ) {
        composable(Routes.HOME) {
            val viewModel: HomeViewModel = viewModel()
            HomeScreen(
                viewModel = viewModel,
                onNoteClick = { noteId -> navController.navigate(Routes.editor(noteId = noteId)) },
                onCreateNote = { template -> navController.navigate(Routes.editor(template = template)) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }

        composable(
            route = Routes.EDITOR,
            arguments = listOf(
                navArgument("noteId") {
                    type = NavType.StringType; nullable = true; defaultValue = null
                },
                navArgument("template") {
                    type = NavType.StringType; nullable = true; defaultValue = null
                },
            ),
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getString("noteId")
            val template = backStackEntry.arguments?.getString("template")
            val viewModel: EditorViewModel = viewModel()
            EditorScreen(
                viewModel = viewModel,
                noteId = noteId,
                template = template,
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(Routes.SETTINGS) {
            val viewModel: SettingsViewModel = viewModel()
            SettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
