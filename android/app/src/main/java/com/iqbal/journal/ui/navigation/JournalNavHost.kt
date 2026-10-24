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
import com.iqbal.journal.ui.groupdetail.GroupDetailRoute
import com.iqbal.journal.ui.home.HomeRoute
import com.iqbal.journal.ui.settings.SettingsRoute
import com.iqbal.journal.ui.start.StartRoute
import kotlinx.serialization.Serializable

@Serializable
data class StartDestination(val canGoBack: Boolean)

@Serializable
data object HomeDestination

/** [noteId] null = catatan baru. Nama properti dipakai sebagai kunci argumen di EditorViewModel. */
@Serializable
data class EditorDestination(val groupId: String, val noteId: String? = null)

/** Nama properti dipakai sebagai kunci argumen di GroupDetailViewModel. */
@Serializable
data class GroupDetailDestination(val groupId: String)

@Serializable
data object SettingsDestination

/** Navigasi aplikasi utama (setelah profil diisi). */
@Composable
fun JournalNavHost(
    hasGroups: Boolean,
    noteLink: NoteLink?,
    onNoteLinkHandled: () -> Unit,
    navController: NavHostController = rememberNavController()
) {
    // Tujuan awal hanya ditentukan sekali; perubahan selanjutnya lewat navigasi.
    val startDestination: Any = remember { if (hasGroups) HomeDestination else StartDestination(canGoBack = false) }

    LaunchedEffect(hasGroups) {
        val onStart = navController.currentBackStackEntry?.destination?.hasRoute<StartDestination>() == true
        val onInitialStart = onStart && navController.previousBackStackEntry == null
        when {
            // Grup bisa muncul belakangan lewat sinkron (mis. sesi lama dipulihkan);
            // layar Mulai awal jadi tidak relevan.
            hasGroups && onInitialStart -> navController.navigateToHome()

            // Grup terakhir ditinggalkan atau anggota dikeluarkan: tidak ada lagi yang bisa ditampilkan Beranda.
            !hasGroups && !onStart -> navController.navigate(StartDestination(canGoBack = false)) {
                popUpTo(navController.graph.id) { inclusive = true }
            }
        }
    }

    LaunchedEffect(noteLink) {
        if (noteLink != null) {
            navController.navigate(EditorDestination(noteLink.groupId, noteLink.noteId)) { launchSingleTop = true }
            onNoteLinkHandled()
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable<StartDestination> { entry ->
            val canGoBack = entry.toRoute<StartDestination>().canGoBack
            StartRoute(
                onGroupReady = { navController.navigateToHome() },
                onBack = if (canGoBack) ({ navController.navigateBack() }) else null
            )
        }
        composable<HomeDestination> {
            HomeRoute(
                onAddGroup = { navController.navigate(StartDestination(canGoBack = true)) },
                onOpenEditor = { groupId, noteId -> navController.navigate(EditorDestination(groupId, noteId)) },
                onOpenGroup = { groupId -> navController.navigate(GroupDetailDestination(groupId)) },
                onOpenSettings = { navController.navigate(SettingsDestination) }
            )
        }
        composable<GroupDetailDestination> {
            GroupDetailRoute(onBack = { navController.navigateBack() }, onGone = { navController.navigateBack() })
        }
        composable<SettingsDestination> {
            SettingsRoute(onBack = { navController.navigateBack() })
        }
        composable<EditorDestination> {
            EditorRoute(onDone = { navController.navigateBack() })
        }
    }
}

/**
 * Kembali satu layar, tapi tidak pernah membuang layar awal: back yang terpanggil dua kali
 * (mis. ketukan ganda) jika tidak dijaga akan mengosongkan NavHost dan menampilkan layar kosong.
 */
private fun NavHostController.navigateBack() {
    if (previousBackStackEntry != null) popBackStack()
}

private fun NavHostController.navigateToHome() = navigate(HomeDestination) {
    popUpTo(graph.id) { inclusive = true }
    launchSingleTop = true
}
