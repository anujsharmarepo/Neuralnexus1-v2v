package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.GuardiansRepositoryImpl
import com.example.data.repository.UserRepositoryImpl
import com.example.domain.model.Guardian
import com.example.domain.model.User
import com.example.domain.repository.GuardiansRepository
import com.example.domain.repository.UserRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface AuthUiState {
    object Idle : AuthUiState
    object Loading : AuthUiState
    data class Success(val message: String) : AuthUiState
    data class Error(val error: String) : AuthUiState
}

class AbhayaViewModel(application: android.app.Application) : androidx.lifecycle.AndroidViewModel(application) {
    private val userRepository: UserRepository = UserRepositoryImpl(application)
    private val guardiansRepository: GuardiansRepository = GuardiansRepositoryImpl()

    // Auth flows
    val currentUser: StateFlow<User?> = userRepository.currentUser

    private val _authUiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val authUiState: StateFlow<AuthUiState> = _authUiState.asStateFlow()

    // Guardians list state
    val guardians: StateFlow<List<Guardian>> = guardiansRepository.guardians
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI Input States
    val loginEmail = MutableStateFlow("")
    val loginPassword = MutableStateFlow("")

    val signupName = MutableStateFlow("")
    val signupEmail = MutableStateFlow("")
    val signupPassword = MutableStateFlow("")

    val forgotEmail = MutableStateFlow("")

    // Bottom Nav state
    private val _currentTab = MutableStateFlow("Home")
    val currentTab: StateFlow<String> = _currentTab.asStateFlow()

    // SOS active progress
    private val _sosHoldProgress = MutableStateFlow(0f) // 0.0 to 1.0
    val sosHoldProgress: StateFlow<Float> = _sosHoldProgress.asStateFlow()

    private val _sosTriggered = MutableStateFlow(false)
    val sosTriggered: StateFlow<Boolean> = _sosTriggered.asStateFlow()

    private var sosJob: Job? = null

    // For demonstration/quick actions alert dialogs or snackbars
    private val _alertMessage = MutableStateFlow<String?>(null)
    val alertMessage: StateFlow<String?> = _alertMessage.asStateFlow()

    fun selectTab(tab: String) {
        _currentTab.value = tab
    }

    fun clearAlert() {
        _alertMessage.value = null
    }

    fun triggerQuickAction(actionName: String) {
        _alertMessage.value = "Quick Action triggered: $actionName. Activating secure protocols."
    }

    fun login() {
        viewModelScope.launch {
            _authUiState.value = AuthUiState.Loading
            userRepository.login(loginEmail.value, loginPassword.value)
                .onSuccess {
                    _authUiState.value = AuthUiState.Success("Welcome back, ${it.name}!")
                    // Clear fields
                    loginPassword.value = ""
                }
                .onFailure {
                    _authUiState.value = AuthUiState.Error(it.message ?: "Authentication failed")
                }
        }
    }

    fun signUp() {
        viewModelScope.launch {
            _authUiState.value = AuthUiState.Loading
            userRepository.signUp(signupName.value, signupEmail.value, signupPassword.value)
                .onSuccess {
                    _authUiState.value = AuthUiState.Success("Account created successfully, ${it.name}!")
                    // Clear fields
                    signupPassword.value = ""
                }
                .onFailure {
                    _authUiState.value = AuthUiState.Error(it.message ?: "Registration failed")
                }
        }
    }

    fun sendPasswordReset() {
        viewModelScope.launch {
            _authUiState.value = AuthUiState.Loading
            userRepository.resetPassword(forgotEmail.value)
                .onSuccess {
                    _authUiState.value = AuthUiState.Success("Password reset instructions sent to ${forgotEmail.value}")
                }
                .onFailure {
                    _authUiState.value = AuthUiState.Error(it.message ?: "Unable to reset password")
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            userRepository.logout()
            _authUiState.value = AuthUiState.Idle
            _sosTriggered.value = false
            _sosHoldProgress.value = 0f
            // Pre-populate with default credentials for user convenience in debug environment
            loginEmail.value = "anujsharma1555r@gmail.com"
        }
    }

    fun resetAuthState() {
        _authUiState.value = AuthUiState.Idle
    }

    // SOS Gesture handlers
    fun startSosCountdown() {
        if (_sosTriggered.value) return
        sosJob?.cancel()
        sosJob = viewModelScope.launch {
            val totalSteps = 60 // 3 seconds, 50ms intervals
            for (step in 1..totalSteps) {
                delay(50)
                _sosHoldProgress.value = step / totalSteps.toFloat()
            }
            // If we completed the loop without cancellation
            _sosTriggered.value = true
            _alertMessage.value = "🚨 SOS DISTRESS TRIGGERED! Sending GPS coordinates & live recording to all 5 active guardians."
        }
    }

    fun cancelSosCountdown() {
        if (_sosTriggered.value) return
        sosJob?.cancel()
        sosJob = null
        _sosHoldProgress.value = 0f
    }

    fun dismissSosAlert() {
        _sosTriggered.value = false
        _sosHoldProgress.value = 0f
        _alertMessage.value = "Emergency alert deactivated safely."
    }

    fun addGuardian(name: String, relation: String, phone: String) {
        viewModelScope.launch {
            guardiansRepository.addGuardian(Guardian(
                id = System.currentTimeMillis().toString(),
                name = name,
                relation = relation,
                phone = phone,
                isActive = true
            ))
        }
    }

    fun removeGuardian(id: String) {
        viewModelScope.launch {
            guardiansRepository.removeGuardian(id)
        }
    }

    fun toggleGuardian(id: String) {
        viewModelScope.launch {
            guardiansRepository.toggleGuardianActive(id)
        }
    }
}
