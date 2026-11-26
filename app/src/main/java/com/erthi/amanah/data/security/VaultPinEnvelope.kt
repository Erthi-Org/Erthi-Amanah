package com.erthi.amanah.data.security

import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

internal object VaultPinEnvelope {
    private val purpose = "erthi-vault-pin-v1".toByteArray()

    fun seal(key: SecretKey, pin: String): ByteArray {
        val salt = PinHash.salt()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, wrappingKey(pin, salt))
        cipher.updateAAD(purpose)
        val raw = key.encoded
        return try {
            salt + cipher.iv + cipher.doFinal(raw)
        } finally {
            raw.fill(0)
        }
    }

    fun open(envelope: ByteArray, pin: String): SecretKey {
        require(envelope.size == 32 + 12 + 32 + 16)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            wrappingKey(pin, envelope.copyOfRange(0, 32)),
            GCMParameterSpec(128, envelope.copyOfRange(32, 44)),
        )
        cipher.updateAAD(purpose)
        val raw = cipher.doFinal(envelope, 44, envelope.size - 44)
        return try {
            SecretKeySpec(raw, "AES")
        } finally {
            raw.fill(0)
        }
    }

    private fun wrappingKey(pin: String, salt: ByteArray): SecretKey {
        val raw = PinHash.derive(pin, salt, PinHash.PBKDF2)
        return try {
            SecretKeySpec(raw, "AES")
        } finally {
            raw.fill(0)
        }
    }
}
