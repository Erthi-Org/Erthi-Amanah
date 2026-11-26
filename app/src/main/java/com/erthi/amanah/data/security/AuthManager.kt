package com.erthi.amanah.data.security

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.MutablePreferences
import android.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>,
) {

    companion object {
        private val KEY_PIN_VERSION = intPreferencesKey("pin_hash_version")
        private val KEY_PIN_HASH = stringPreferencesKey("pin_hash")
        private val KEY_PIN_SALT = stringPreferencesKey("pin_salt")
        private val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        private val KEY_BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        private val KEY_LOCKOUT_UNTIL_MS = androidx.datastore.preferences.core.longPreferencesKey("lockout_until_ms")
        private val KEY_FAILED_ATTEMPTS = androidx.datastore.preferences.core.intPreferencesKey("failed_attempts")
    }

    val lockoutUntilMs: Flow<Long> = dataStore.data.map { prefs -> prefs[KEY_LOCKOUT_UNTIL_MS] ?: 0L }
    val failedAttempts: Flow<Int> = dataStore.data.map { prefs -> prefs[KEY_FAILED_ATTEMPTS] ?: 0 }

    suspend fun recordFailedAttempt(): Int {
        var newCount = 0
        dataStore.edit { prefs ->
            newCount = (prefs[KEY_FAILED_ATTEMPTS] ?: 0) + 1
            prefs[KEY_FAILED_ATTEMPTS] = newCount
        }
        return newCount
    }

    suspend fun setLockout(durationMs: Long) {
        dataStore.edit { prefs ->
            prefs[KEY_LOCKOUT_UNTIL_MS] = System.currentTimeMillis() + durationMs
            prefs[KEY_FAILED_ATTEMPTS] = 0
        }
    }

    suspend fun clearLockout() {
        dataStore.edit { prefs ->
            prefs[KEY_LOCKOUT_UNTIL_MS] = 0L
            prefs[KEY_FAILED_ATTEMPTS] = 0
        }
    }

    val isPinSet: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_PIN_HASH] != null
    }

    val isOnboardingDone: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_ONBOARDING_DONE] == true
    }

    val isBiometricEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_BIOMETRIC_ENABLED] == true
    }

    suspend fun setPin(pin: String) {
        val salt = PinHash.salt()
        val hash = withContext(Dispatchers.Default) { PinHash.derive(pin, salt, PinHash.PBKDF2) }
        dataStore.edit { prefs ->
            prefs.writePin(salt, hash)
            prefs[KEY_ONBOARDING_DONE] = true
        }
    }

    suspend fun verifyPin(pin: String): Boolean = withContext(Dispatchers.Default) {
        var verified = false
        dataStore.edit { prefs ->
            val salt = prefs[KEY_PIN_SALT]?.let(::decode) ?: return@edit
            val hash = prefs[KEY_PIN_HASH]?.let(::decode) ?: return@edit
            val version = prefs[KEY_PIN_VERSION] ?: PinHash.LEGACY
            verified = PinHash.verify(pin, salt, hash, version)
            if (verified && version == PinHash.LEGACY) {
                val newSalt = PinHash.salt()
                prefs.writePin(newSalt, PinHash.derive(pin, newSalt, PinHash.PBKDF2))
            }
        }
        verified
    }

    private fun decode(value: String): ByteArray? =
        try {
            Base64.decode(value, Base64.NO_WRAP)
        } catch (_: IllegalArgumentException) {
            null
        }

    private fun MutablePreferences.writePin(salt: ByteArray, hash: ByteArray) {
        this[KEY_PIN_SALT] = Base64.encodeToString(salt, Base64.NO_WRAP)
        this[KEY_PIN_HASH] = Base64.encodeToString(hash, Base64.NO_WRAP)
        this[KEY_PIN_VERSION] = PinHash.PBKDF2
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_BIOMETRIC_ENABLED] = enabled }
    }

    suspend fun markOnboardingDone() {
        dataStore.edit { prefs -> prefs[KEY_ONBOARDING_DONE] = true }
    }

}
