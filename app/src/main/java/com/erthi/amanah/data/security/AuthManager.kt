package com.erthi.amanah.data.security

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
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
        private val KEY_LEGACY_LOCKOUT = longPreferencesKey("lockout_until_ms")
        private val KEY_LOCKOUT_START = longPreferencesKey("lockout_elapsed_start")
        private val KEY_LOCKOUT_DURATION = longPreferencesKey("lockout_duration")
        private val KEY_LOCKOUT_BOOT = intPreferencesKey("lockout_boot_count")
        private val KEY_FAILED_ATTEMPTS = intPreferencesKey("failed_attempts")
    }

    private fun bootCount(): Int =
        Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, -1)

    val remainingLockSeconds: Flow<Int> = flow {
        while (true) {
            emit(((remainingLockMs() + 999) / 1000).toInt())
            delay(1_000)
        }
    }

    suspend fun remainingLockMs(): Long {
        var result = 0L
        dataStore.edit { prefs ->
            val boot = bootCount()
            val now = SystemClock.elapsedRealtime()
            val duration = prefs[KEY_LOCKOUT_DURATION] ?: 0L
            val lockout = Lockout(prefs[KEY_LOCKOUT_BOOT] ?: -1, prefs[KEY_LOCKOUT_START] ?: 0L, duration)
            val active = when {
                (prefs.remove(KEY_LEGACY_LOCKOUT) ?: 0L) > 0L ->
                    Lockout(boot, now, Lockout.MAX_DURATION_MS).also { prefs.saveLockout(it) }
                duration > 0 && lockout.needsRestart(boot, now) ->
                    lockout.copy(boot = boot, startedMs = now).also { prefs.saveLockout(it) }
                else -> lockout
            }
            result = active.remainingMs(now)
            if (result == 0L) {
                prefs.remove(KEY_LOCKOUT_DURATION)
                prefs.remove(KEY_LOCKOUT_START)
                prefs.remove(KEY_LOCKOUT_BOOT)
            }
        }
        return result
    }

    suspend fun recordFailedAttempt(): Int {
        var count = 0
        dataStore.edit { prefs ->
            count = ((prefs[KEY_FAILED_ATTEMPTS] ?: 0) + 1).coerceAtMost(10)
            prefs[KEY_FAILED_ATTEMPTS] = count
            val duration = Lockout.durationFor(count)
            if (duration > 0) {
                prefs.saveLockout(Lockout(bootCount(), SystemClock.elapsedRealtime(), duration))
            }
        }
        return count
    }

    suspend fun clearLockout() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_LOCKOUT_DURATION)
            prefs.remove(KEY_LOCKOUT_START)
            prefs.remove(KEY_LOCKOUT_BOOT)
            prefs.remove(KEY_LEGACY_LOCKOUT)
            prefs.remove(KEY_FAILED_ATTEMPTS)
        }
    }

    private fun MutablePreferences.saveLockout(lockout: Lockout) {
        this[KEY_LOCKOUT_START] = lockout.startedMs
        this[KEY_LOCKOUT_DURATION] = lockout.durationMs
        this[KEY_LOCKOUT_BOOT] = lockout.boot
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
