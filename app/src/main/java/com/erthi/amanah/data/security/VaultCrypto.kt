package com.erthi.amanah.data.security

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.room.withTransaction
import com.erthi.amanah.data.db.ErthiDatabase
import com.erthi.amanah.data.db.dao.VaultItemDao
import com.erthi.amanah.data.db.entity.VaultItemEntity
import java.security.GeneralSecurityException
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.spec.MGF1ParameterSpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VaultCrypto @Inject constructor(
    private val legacy: CryptoManager,
    private val db: ErthiDatabase,
    private val dao: VaultItemDao,
) {
    private data class Session(val key: SecretKey?)
    @Volatile private var session: Session? = null

    private companion object {
        const val LEGACY_PREFIX = "[enc]"
        const val PREFIX = "[vault2]"
        const val ALIAS = "erthi_vault_biometric_v1"
        const val PROVIDER = "AndroidKeyStore"
        const val RSA = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding"
        val oaep = OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA1, PSource.PSpecified.DEFAULT)
    }

    internal suspend fun newKey(): SecretKey {
        check(!dao.hasProtectedItems()) { "Existing vault is missing its key envelope" }
        return KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
    }
    internal fun activeKey(): SecretKey? = session?.key
    internal fun unlock(key: SecretKey) { session = Session(key) }
    internal fun unlockLegacy() { session = Session(null) }
    internal fun lock() { session = null }

    internal fun sealForPin(key: SecretKey, pin: String): String {
        val envelope = VaultPinEnvelope.seal(key, pin)
        return encode(legacy.encrypt(envelope).toByteArray())
    }

    internal fun openForPin(value: String, pin: String): SecretKey =
        VaultPinEnvelope.open(legacy.decrypt(CryptoManager.EncryptedData.fromByteArray(decode(value))), pin)

    @Synchronized
    internal fun sealForBiometrics(key: SecretKey): String {
        val store = keyStore()
        if (!store.containsAlias(ALIAS)) {
            val builder = KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_DECRYPT)
                .setKeySize(2048)
                .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA1)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
                .setUserAuthenticationRequired(true)
                .setInvalidatedByBiometricEnrollment(true)
            if (Build.VERSION.SDK_INT >= 30) {
                builder.setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
            }
            KeyPairGenerator.getInstance("RSA", PROVIDER).apply { initialize(builder.build()) }.generateKeyPair()
        }
        val publicKey = KeyFactory.getInstance("RSA").generatePublic(
            X509EncodedKeySpec(store.getCertificate(ALIAS).publicKey.encoded),
        )
        val cipher = Cipher.getInstance(RSA)
        cipher.init(Cipher.ENCRYPT_MODE, publicKey, oaep)
        val raw = key.encoded
        return try {
            encode(cipher.doFinal(raw))
        } finally {
            raw.fill(0)
        }
    }

    internal fun biometricCipher(): Cipher = Cipher.getInstance(RSA).apply {
        init(Cipher.DECRYPT_MODE, keyStore().getKey(ALIAS, null), oaep)
    }

    internal fun openForBiometrics(cipher: Cipher, envelope: String): SecretKey {
        val raw = cipher.doFinal(decode(envelope))
        return try {
            require(raw.size == 32)
            SecretKeySpec(raw, "AES")
        } finally {
            raw.fill(0)
        }
    }

    @Synchronized
    internal fun removeBiometricKey() {
        keyStore().deleteEntry(ALIAS)
    }

    internal suspend fun migrate(key: SecretKey) = db.withTransaction {
        for (row in dao.getItemsForMigration()) {
            val migrated = try {
                row.mapVaultFields { field ->
                    if (field.startsWith(PREFIX)) field else encrypt(decrypt(field, key), key)
                }
            } catch (_: GeneralSecurityException) {
                continue  // Preserve the original row if any old ciphertext is unreadable.
            } catch (_: IllegalArgumentException) {
                continue  // Preserve malformed legacy records for possible recovery.
            }
            if (migrated != row) dao.update(migrated)
        }
    }

    internal fun isProtected(value: String): Boolean = value.startsWith(PREFIX)

    internal fun encrypt(value: String): String {
        val key = requireSession().key
        if (key != null) return encrypt(value, key)
        val encrypted = legacy.encrypt(value.toByteArray(Charsets.UTF_8))
        return LEGACY_PREFIX + encode(encrypted.toByteArray())
    }

    internal fun decrypt(value: String): String = decrypt(value, requireSession().key)

    private fun requireSession(): Session = checkNotNull(session) { "Vault is locked" }

    private fun encrypt(value: String, key: SecretKey): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        return PREFIX + encode(cipher.iv + cipher.doFinal(value.toByteArray(Charsets.UTF_8)))
    }

    private fun decrypt(value: String, key: SecretKey?): String {
        if (!value.startsWith(PREFIX) && !value.startsWith(LEGACY_PREFIX)) return value
        val bytes = decode(value.substringAfter(']'))
        val encrypted = CryptoManager.EncryptedData.fromByteArray(bytes)
        val plaintext = if (value.startsWith(PREFIX)) {
            Cipher.getInstance("AES/GCM/NoPadding").apply {
                init(Cipher.DECRYPT_MODE, checkNotNull(key) { "Unlock with PIN to read this vault item" }, GCMParameterSpec(128, encrypted.iv))
            }.doFinal(encrypted.ciphertext)
        } else {
            legacy.decrypt(encrypted)
        }
        return String(plaintext, Charsets.UTF_8)
    }

    private fun keyStore(): KeyStore = KeyStore.getInstance(PROVIDER).apply { load(null) }
    private fun encode(value: ByteArray): String = Base64.encodeToString(value, Base64.NO_WRAP)
    private fun decode(value: String): ByteArray = Base64.decode(value, Base64.NO_WRAP)
}

internal fun VaultItemEntity.mapVaultFields(transform: (String) -> String) = copy(
    serviceName = transform(serviceName),
    password = transform(password),
    websiteUrl = websiteUrl?.let(transform),
    username = username?.let(transform),
    email = email?.let(transform),
    accountIdentifier = accountIdentifier?.let(transform),
    recoveryEmail = recoveryEmail?.let(transform),
    recoveryPhone = recoveryPhone?.let(transform),
    recoveryCodes = recoveryCodes?.let(transform),
    notes = notes?.let(transform),
)
