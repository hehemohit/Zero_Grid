package com.example.zerogrid.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zerogrid.network.AuthRepository
import com.example.zerogrid.network.AuthResult
import com.example.zerogrid.network.ProfileResult
import com.example.zerogrid.network.UserDto
import com.zerogrid.mesh.app.ui.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ── UI States ──────────────────────────────────────────────────────────────

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val role: UserRole, val profileComplete: Boolean) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
    data class AdminPending(val message: String) : AuthUiState()
}

sealed class ProfileUiState {
    object Idle : ProfileUiState()
    object Loading : ProfileUiState()
    data class Success(val user: UserDto) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}

// ── ViewModel ──────────────────────────────────────────────────────────────

class AuthViewModel(
    private val repository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _profileState = MutableStateFlow<ProfileUiState>(ProfileUiState.Idle)
    val profileState: StateFlow<ProfileUiState> = _profileState.asStateFlow()

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }

    fun resetProfileState() {
        _profileState.value = ProfileUiState.Idle
    }

    fun login(email: String, password: String) {
        if (_uiState.value is AuthUiState.Loading) return
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = repository.login(email.trim(), password)
            if (result is AuthResult.Success) {
                syncFcmTokenOnSuccess()
            }
            _uiState.value = when (result) {
                is AuthResult.Success      -> AuthUiState.Success(result.role, result.user.profileComplete)
                is AuthResult.Error        -> AuthUiState.Error(result.message)
                is AuthResult.AdminPending -> AuthUiState.AdminPending(result.message)
            }
        }
    }

    fun register(email: String, password: String, displayName: String, role: UserRole) {
        if (_uiState.value is AuthUiState.Loading) return
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = repository.register(email.trim(), password, displayName.trim(), role)
            if (result is AuthResult.Success) {
                syncFcmTokenOnSuccess()
            }
            _uiState.value = when (result) {
                is AuthResult.Success      -> AuthUiState.Success(result.role, result.user.profileComplete)
                is AuthResult.Error        -> AuthUiState.Error(result.message)
                is AuthResult.AdminPending -> AuthUiState.AdminPending(result.message)
            }
        }
    }

    private fun syncFcmTokenOnSuccess() {
        try {
            com.google.firebase.messaging.FirebaseMessaging.getInstance().token
                .addOnCompleteListener { task ->
                    if (task.isSuccessful && !task.result.isNullOrBlank()) {
                        val token = task.result
                        viewModelScope.launch {
                            try {
                                repository.updateFcmToken(token)
                                android.util.Log.d("FCM", "FCM token synced after auth: ${token.take(15)}...")
                            } catch (e: Exception) {
                                android.util.Log.w("FCM", "Failed to sync token after auth", e)
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            android.util.Log.w("FCM", "Firebase messaging unavailable", e)
        }
    }

    fun completeProfile(phoneNumber: String, dateOfBirth: String) {
        if (_profileState.value is ProfileUiState.Loading) return
        viewModelScope.launch {
            _profileState.value = ProfileUiState.Loading
            _profileState.value = when (val result = repository.completeProfile(phoneNumber.trim(), dateOfBirth.trim())) {
                is ProfileResult.Success -> ProfileUiState.Success(result.user)
                is ProfileResult.Error   -> ProfileUiState.Error(result.message)
            }
        }
    }

    fun loadProfile() {
        if (_profileState.value is ProfileUiState.Loading) return
        viewModelScope.launch {
            _profileState.value = ProfileUiState.Loading
            _profileState.value = when (val result = repository.getProfile()) {
                is ProfileResult.Success -> ProfileUiState.Success(result.user)
                is ProfileResult.Error   -> ProfileUiState.Error(result.message)
            }
        }
    }

    fun updateProfile(displayName: String? = null, phoneNumber: String? = null, dateOfBirth: String? = null) {
        if (_profileState.value is ProfileUiState.Loading) return
        viewModelScope.launch {
            _profileState.value = ProfileUiState.Loading
            _profileState.value = when (val result = repository.updateProfile(displayName, phoneNumber, dateOfBirth)) {
                is ProfileResult.Success -> ProfileUiState.Success(result.user)
                is ProfileResult.Error   -> ProfileUiState.Error(result.message)
            }
        }
    }
}
