package com.iqbal.journal.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.iqbal.journal.ui.editor.EditorRoute
import com.iqbal.journal.ui.home.HomeRoute
import com.iqbal.journal.ui.start.StartRoute
import kotlinx.serialization.Serializable

@Serializable
data class StartDestination(val canGoBack: Boolean)

@Serializable
data object HomeDestination

/** [noteId] null = catatan baru. Nama properti dipakai sebagai kunci argumen di EditorViewModel. */
@Serializable
data class EditorDestination(val groupId: String, val noteId: String? = null)

/** Navigasi aplikasi utama (setelah profil diisi). */
@Composable
fun JournalNavHost(hasGroups: Boolean, navController: NavHostController = rememberNavController()) {
    // Tujuan awal hanya ditentukan sekali; perubahan selanjutnya lewat navigasi.
    val startDestination: Any = remember { if (hasGroups) HomeDestination else StartDestination(canGoBack = false) }

    // Grup bisa muncul belakangan lewat sinkron (mis. sesi lama dipulihkan); layar Mulai awal lalu tidak relevan.
    LaunchedEffect(hasGroups) {
        val onInitialStart = navController.currentBackStackEntry?.destination?.hasRoute<StartDestination>() == true &&
            navController.previousBackStackEntry == null
        if (hasGroups && onInitialStart) navController.navigateToHome()
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable<StartDestination> { entry ->
            val canGoBack = entry.toRoute<StartDestination>().canGoBack
            StartRoute(
                onGroupReady = { navController.navigateToHome() },
                onBack = if (canGoBack) ({ navController.popBackStack() }) else null
            )
        }
        composable<HomeDestination> {
            HomeRoute(
                onAddGroup = { navController.navigate(StartDestination(canGoBack = true)) },
                onOpenEditor = { groupId, noteId -> navController.navigate(EditorDestination(groupId, noteId)) }
            )
        }
        composable<EditorDestination> {
            EditorRoute(onDone = { navController.popBackStack() }, onBack = { navController.popBackStack() })
        }
    }
}

private fun NavHostController.navigateToHome() = navigate(HomeDestination) {
    popUpTo(graph.id) { inclusive = true }
    launchSingleTop = true
}
