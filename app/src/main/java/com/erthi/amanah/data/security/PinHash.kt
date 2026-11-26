package com.erthi.amanah.data.security

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

internal object PinHash {
    const val LEGACY = 1
    const val PBKDF2 = 2

    fun salt(): ByteArray = ByteArray(32).also(SecureRandom()::nextBytes)

    fun derive(pin: String, salt: ByteArray, version: Int): ByteArray = when (version) {
        LEGACY -> MessageDigest.getInstance("SHA-256").run {
            update(salt)
            digest(pin.toByteArray(Charsets.UTF_8))
        }
        PBKDF2 -> {
            val spec = PBEKeySpec(pin.toCharArray(), salt, 310_000, 256)
            try {
                SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
            } finally {
                spec.clearPassword()
            }
        }
        else -> error("Unsupported PIN hash version")
    }

    fun verify(pin: String, salt: ByteArray, hash: ByteArray, version: Int): Boolean =
        version in LEGACY..PBKDF2 && MessageDigest.isEqual(hash, derive(pin, salt, version))
}
