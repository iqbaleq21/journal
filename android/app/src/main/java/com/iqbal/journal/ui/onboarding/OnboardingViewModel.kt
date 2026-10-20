package com.iqbal.journal.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iqbal.journal.domain.model.JournalError
import com.iqbal.journal.domain.model.Outcome
import com.iqbal.journal.domain.repository.ProfileRepository
import com.iqbal.journal.domain.validation.ProfileRules
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val name: String = "",
    val email: String = "",
    /** Error validasi baru ditampilkan setelah pengguna menekan Mulai. */
    val showValidation: Boolean = false,
    val isSubmitting: Boolean = false,
    /** Masuk lagi karena sesi di HP ini hilang; isian diambil dari profil lama. */
    val isSessionLost: Boolean = false,
    val error: JournalError? = null
) {
    val isNameValid: Boolean get() = ProfileRules.isValidName(name)
    val isEmailValid: Boolean get() = ProfileRules.isValidEmail(email)
    val showNameError: Boolean get() = showValidation && !isNameValid
    val showEmailError: Boolean get() = showValidation && !isEmailValid
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(private val profileRepository: ProfileRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            profileRepository.myProfile.first()?.let { profile ->
                _uiState.update { it.copy(name = profile.displayName, email = profile.email, isSessionLost = true) }
            }
        }
    }

    fun onNameChange(name: String) = _uiState.update { it.copy(name = name.take(ProfileRules.MAX_NAME_LENGTH)) }

    fun onEmailChange(email: String) = _uiState.update { it.copy(email = email) }

    fun onErrorShown() = _uiState.update { it.copy(error = null) }

    /** Setelah sukses, profil muncul di repository dan aplikasi pindah layar dengan sendirinya. */
    fun onSubmit() {
        val state = _uiState.value
        if (state.isSubmitting) return
        if (!state.isNameValid || !state.isEmailValid) {
            _uiState.update { it.copy(showValidation = true) }
            return
        }
        _uiState.update { it.copy(isSubmitting = true, showValidation = true) }
        viewModelScope.launch {
            val result = profileRepository.saveProfile(state.name, state.email)
            _uiState.update {
                it.copy(isSubmitting = false, error = (result as? Outcome.Failure)?.error)
            }
        }
    }
}
