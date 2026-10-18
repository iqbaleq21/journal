package com.iqbal.journal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iqbal.journal.ui.navigation.JournalNavHost
import com.iqbal.journal.ui.onboarding.OnboardingRoute

/** Akar UI: Kenalan sampai profil diisi, lalu aplikasi utama. */
@Composable
fun JournalApp(viewModel: RootViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    when (val current = state) {
        RootUiState.Loading -> Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        )

        RootUiState.NeedsProfile -> OnboardingRoute()

        is RootUiState.Ready -> JournalNavHost(hasGroups = current.hasGroups)
    }
}
