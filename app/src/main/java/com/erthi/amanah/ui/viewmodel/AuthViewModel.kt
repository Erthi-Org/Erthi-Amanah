package com.erthi.amanah.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erthi.amanah.data.security.AuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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

    private var lockGeneration = 0L
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated

    val authState: StateFlow<AuthState> = combine(
        authManager.isOnboardingDone,
        authManager.isPinSet,
        _isAuthenticated,
    ) { onboardingDone, pinSet, authenticated ->
        when {
            !onboardingDone -> AuthState.FirstLaunch
            !pinSet -> AuthState.PinNotSet
            authenticated -> AuthState.Unlocked
            else -> AuthState.Locked
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AuthState.FirstLaunch,
    )

    private val _pinError = MutableStateFlow<String?>(null)
    val pinError: StateFlow<String?> = _pinError

    // Lockout is persisted in DataStore — survives process death
    val remainingLockSeconds: StateFlow<Int> = authManager.lockoutUntilMs
        .map { until ->
            val remaining = ((until - System.currentTimeMillis()) / 1000).toInt()
            if (remaining > 0) remaining else 0
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun setPin(pin: String, confirmPin: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (pin.length < 4) { onError("الرمز السري يجب أن يكون 4 أرقام على الأقل"); return }
        if (pin != confirmPin) { onError("الرمزان غير متطابقان"); return }
        val generation = lockGeneration
        viewModelScope.launch {
            authManager.setPin(pin)
            if (generation != lockGeneration) return@launch
            _isAuthenticated.value = true
            onSuccess()
        }
    }

    fun verifyPin(pin: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val generation = lockGeneration
        viewModelScope.launch {
            val lockUntil = authManager.lockoutUntilMs.first()
            val now = System.currentTimeMillis()
            if (lockUntil > now) {
                val remaining = ((lockUntil - now) / 1000) + 1
                onError("محاولات خاطئة كثيرة. انتظر $remaining ثانية")
                return@launch
            }
            if (authManager.verifyPin(pin)) {
                authManager.clearLockout()
                if (generation != lockGeneration) return@launch
                _isAuthenticated.value = true
                onSuccess()
            } else {
                val attempts = authManager.recordFailedAttempt()
                when {
                    attempts >= 10 -> {
                        authManager.setLockout(300_000L)
                        onError("10 محاولات خاطئة. الرجاء الانتظار 5 دقائق")
                    }
                    attempts >= 5 -> {
                        authManager.setLockout(30_000L)
                        onError("5 محاولات خاطئة. الرجاء الانتظار 30 ثانية")
                    }
                    else -> onError("رمز سري خاطئ. تبقّت ${5 - attempts} محاولات")
                }
            }
        }
    }

    fun onBiometricSuccess() {
        _isAuthenticated.value = true
    }

    fun lock() {
        lockGeneration++
        _isAuthenticated.value = false
    }

    fun isBiometricEnabled() = authManager.isBiometricEnabled

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch { authManager.setBiometricEnabled(enabled) }
    }
}
