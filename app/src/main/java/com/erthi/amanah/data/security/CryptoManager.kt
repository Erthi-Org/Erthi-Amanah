package com.erthi.amanah.data.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CryptoManager @Inject constructor() {

    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "erthi_vault_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
        private const val IV_LENGTH = 12
    }

    private val keyStore: KeyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply {
        load(null)
    }

    @Synchronized
    fun getOrCreateKey(): SecretKey {
        return if (keyStore.containsAlias(KEY_ALIAS)) {
            (keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
        } else {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                KEYSTORE_PROVIDER,
            )
            keyGenerator.init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .setUserAuthenticationRequired(false)
                    .build(),
            )
            keyGenerator.generateKey()
        }
    }

    fun encrypt(plaintext: ByteArray): EncryptedData {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext)
        return EncryptedData(iv = iv, ciphertext = ciphertext)
    }

    internal fun encryptDocument(input: java.io.InputStream, output: java.io.OutputStream, checkActive: () -> Unit) {
        DocumentEncryption.write(input, output, getOrCreateKey(), checkActive)
    }

    fun decrypt(encryptedData: EncryptedData): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, encryptedData.iv)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), spec)
        return cipher.doFinal(encryptedData.ciphertext)
    }

    fun deleteKey() {
        if (keyStore.containsAlias(KEY_ALIAS)) {
            keyStore.deleteEntry(KEY_ALIAS)
        }
    }

    data class EncryptedData(val iv: ByteArray, val ciphertext: ByteArray) {
        fun toByteArray(): ByteArray = iv + ciphertext

        companion object {
            fun fromByteArray(bytes: ByteArray): EncryptedData {
                require(bytes.size > IV_LENGTH) { "Invalid encrypted data length" }
                return EncryptedData(
                    iv = bytes.copyOfRange(0, IV_LENGTH),
                    ciphertext = bytes.copyOfRange(IV_LENGTH, bytes.size),
                )
            }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as EncryptedData
            return iv.contentEquals(other.iv) && ciphertext.contentEquals(other.ciphertext)
        }

        override fun hashCode(): Int = 31 * iv.contentHashCode() + ciphertext.contentHashCode()
    }
}
