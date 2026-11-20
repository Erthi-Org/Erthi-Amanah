package com.erthi.amanah.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erthi.amanah.data.security.AuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthState {
    data object FirstLaunch : AuthState()
    data object PinNotSet : AuthState()
    data object Locked : AuthState()
    data object Unlocked : AuthState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authManager: AuthManager,
) : ViewModel() {

    val authState: StateFlow<AuthState> = combine(
        authManager.isOnboardingDone,
        authManager.isPinSet,
    ) { onboardingDone, pinSet ->
        when {
            !onboardingDone -> AuthState.FirstLaunch
            !pinSet -> AuthState.PinNotSet
            else -> AuthState.Locked
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AuthState.FirstLaunch,
    )

    private val _pinError = MutableStateFlow<String?>(null)
    val pinError: StateFlow<String?> = _pinError

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated

    fun setPin(pin: String, confirmPin: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (pin.length < 4) {
            onError("الرمز السري يجب أن يكون 4 أرقام على الأقل")
            return
        }
        if (pin != confirmPin) {
            onError("الرمزان غير متطابقان")
            return
        }
        viewModelScope.launch {
            authManager.setPin(pin)
            _isAuthenticated.value = true
            onSuccess()
        }
    }

    fun verifyPin(pin: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val valid = authManager.verifyPin(pin)
            if (valid) {
                _isAuthenticated.value = true
                onSuccess()
            } else {
                onError("الرمز السري غير صحيح")
            }
        }
    }

    fun onBiometricSuccess() {
        _isAuthenticated.value = true
    }

    fun lock() {
        _isAuthenticated.value = false
    }
}
