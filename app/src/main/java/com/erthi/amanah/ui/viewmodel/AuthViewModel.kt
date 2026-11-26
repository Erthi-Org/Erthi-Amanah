package com.erthi.amanah.ui.viewmodel

import com.erthi.amanah.data.security.CredentialMinimums
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

sealed class AuthState {
    data object Loading : AuthState()
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
            !pinSet && !onboardingDone -> AuthState.FirstLaunch
            !pinSet -> AuthState.PinNotSet
            authenticated -> AuthState.Unlocked
            else -> AuthState.Locked
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AuthState.Loading,
    )

    private val _pinError = MutableStateFlow<String?>(null)
    val pinError: StateFlow<String?> = _pinError

    private val pinAttempts = Mutex()
    val remainingLockSeconds: StateFlow<Int> = authManager.remainingLockSeconds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private val _suggestPinUpgrade = MutableStateFlow(false)
    val suggestPinUpgrade: StateFlow<Boolean> = _suggestPinUpgrade

    fun dismissPinUpgrade() { _suggestPinUpgrade.value = false }

    fun setPin(pin: String, confirmPin: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (!CredentialMinimums.validNewPin(pin)) { onError("أدخل رمزاً من 6 إلى 8 أرقام"); return }
        if (pin != confirmPin) { onError("الرمزان غير متطابقان"); return }
        val generation = lockGeneration
        viewModelScope.launch {
            pinAttempts.withLock {
                if (generation != lockGeneration) return@launch
                authManager.setPin(pin)
            }
            if (generation != lockGeneration) return@launch
            dismissPinUpgrade()
            _isAuthenticated.value = true
            onSuccess()
        }
    }

    fun verifyPin(pin: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val generation = lockGeneration
        viewModelScope.launch {
            pinAttempts.withLock {
                if (generation != lockGeneration || _isAuthenticated.value) return@withLock
                val remainingMs = authManager.remainingLockMs()
                if (remainingMs > 0) {
                    val seconds = (remainingMs + 999) / 1000
                    onError("محاولات خاطئة كثيرة. انتظر ${seconds} ثانية")
                    return@withLock
                }
                if (authManager.verifyPin(pin)) {
                    authManager.clearLockout()
                    if (generation != lockGeneration) return@withLock
                    _suggestPinUpgrade.value = !CredentialMinimums.validNewPin(pin)
                    _isAuthenticated.value = true
                    onSuccess()
                } else {
                    val attempts = authManager.recordFailedAttempt()
                    val message = when {
                        attempts >= 10 -> "10 محاولات خاطئة. الرجاء الانتظار 5 دقائق"
                        attempts >= 5 -> "محاولات خاطئة كثيرة. الرجاء الانتظار 30 ثانية"
                        else -> "رمز سري خاطئ. تبقّت ${5 - attempts} محاولات"
                    }
                    onError(message)
                }
            }
        }
    }

    fun onBiometricSuccess() {
        _isAuthenticated.value = true
    }

    fun lock() {
        lockGeneration++
        dismissPinUpgrade()
        _isAuthenticated.value = false
    }

    fun finishOnboarding() {
        viewModelScope.launch { authManager.markOnboardingDone() }
    }

    fun isBiometricEnabled() = authManager.isBiometricEnabled

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch { authManager.setBiometricEnabled(enabled) }
    }
}
