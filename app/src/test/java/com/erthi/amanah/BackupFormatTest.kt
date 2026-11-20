package com.erthi.amanah

import org.junit.Assert.*
import org.junit.Test
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class BackupFormatTest {

    private val WASI_BAK_MAGIC = "WASI_BAK".toByteArray()
    private val WASI_BAK2_MAGIC = "WASI_BAK2".toByteArray()

    @Test
    fun wasi_bak2_magic_header_is_9_bytes() {
        assertEquals(9, WASI_BAK2_MAGIC.size)
    }

    @Test
    fun wasi_bak_magic_is_8_bytes() {
        assertEquals(8, WASI_BAK_MAGIC.size)
    }

    @Test
    fun wasi_bak2_format_salt_offset_is_9() {
        // magic(9) + salt(32) + iv(12) + ciphertext
        val saltStart = WASI_BAK2_MAGIC.size
        val saltEnd = saltStart + 32
        assertEquals(9, saltStart)
        assertEquals(41, saltEnd)
    }

    @Test
    fun wasi_bak2_format_iv_offset_is_41() {
        val ivStart = 9 + 32
        val ivEnd = ivStart + 12
        assertEquals(41, ivStart)
        assertEquals(53, ivEnd)
    }

    @Test
    fun wasi_bak2_format_ciphertext_starts_at_53() {
        val ctStart = 9 + 32 + 12
        assertEquals(53, ctStart)
    }

    @Test
    fun pbkdf2_key_derivation_produces_256_bit_key() {
        val rng = SecureRandom()
        val salt = ByteArray(32).also { rng.nextBytes(it) }
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec("test-passphrase".toCharArray(), salt, 1_000, 256)
        val key = factory.generateSecret(spec)
        spec.clearPassword()
        assertEquals(32, key.encoded.size)
    }

    @Test
    fun pbkdf2_different_salts_produce_different_keys() {
        val rng = SecureRandom()
        val salt1 = ByteArray(32).also { rng.nextBytes(it) }
        val salt2 = ByteArray(32).also { rng.nextBytes(it) }
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec1 = PBEKeySpec("passphrase".toCharArray(), salt1, 1_000, 256)
        val spec2 = PBEKeySpec("passphrase".toCharArray(), salt2, 1_000, 256)
        val key1 = factory.generateSecret(spec1).encoded
        val key2 = factory.generateSecret(spec2).encoded
        spec1.clearPassword(); spec2.clearPassword()
        assertFalse(key1.contentEquals(key2))
    }

    @Test
    fun pbkdf2_different_passphrases_produce_different_keys() {
        val salt = ByteArray(32) { it.toByte() }
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec1 = PBEKeySpec("correct-horse".toCharArray(), salt, 1_000, 256)
        val spec2 = PBEKeySpec("wrong-passphrase".toCharArray(), salt, 1_000, 256)
        val key1 = factory.generateSecret(spec1).encoded
        val key2 = factory.generateSecret(spec2).encoded
        spec1.clearPassword(); spec2.clearPassword()
        assertFalse(key1.contentEquals(key2))
    }

    @Test
    fun aes_gcm_roundtrip_with_pbkdf2_key() {
        val rng = SecureRandom()
        val salt = ByteArray(32).also { rng.nextBytes(it) }
        val iv = ByteArray(12).also { rng.nextBytes(it) }
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec("my-secret".toCharArray(), salt, 1_000, 256)
        val keyBytes = factory.generateSecret(spec).encoded
        spec.clearPassword()
        val key = SecretKeySpec(keyBytes, "AES")

        val plaintext = """{"will":"Test will content"}""".toByteArray()

        val encCipher = Cipher.getInstance("AES/GCM/NoPadding")
        encCipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        val ciphertext = encCipher.doFinal(plaintext)

        val decCipher = Cipher.getInstance("AES/GCM/NoPadding")
        decCipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        val decrypted = decCipher.doFinal(ciphertext)

        assertArrayEquals(plaintext, decrypted)
    }

    @Test
    fun wrong_passphrase_causes_gcm_authentication_failure() {
        val rng = SecureRandom()
        val salt = ByteArray(32).also { rng.nextBytes(it) }
        val iv = ByteArray(12).also { rng.nextBytes(it) }
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")

        val specEnc = PBEKeySpec("correct".toCharArray(), salt, 1_000, 256)
        val encKey = SecretKeySpec(factory.generateSecret(specEnc).encoded, "AES")
        specEnc.clearPassword()

        val specDec = PBEKeySpec("wrong".toCharArray(), salt, 1_000, 256)
        val decKey = SecretKeySpec(factory.generateSecret(specDec).encoded, "AES")
        specDec.clearPassword()

        val plaintext = "sensitive data".toByteArray()
        val encCipher = Cipher.getInstance("AES/GCM/NoPadding")
        encCipher.init(Cipher.ENCRYPT_MODE, encKey, GCMParameterSpec(128, iv))
        val ciphertext = encCipher.doFinal(plaintext)

        val decCipher = Cipher.getInstance("AES/GCM/NoPadding")
        decCipher.init(Cipher.DECRYPT_MODE, decKey, GCMParameterSpec(128, iv))
        try {
            decCipher.doFinal(ciphertext)
            fail("Expected AEADBadTagException for wrong passphrase")
        } catch (e: Exception) {
            // Expected — GCM authentication tag mismatch
        }
    }
}
