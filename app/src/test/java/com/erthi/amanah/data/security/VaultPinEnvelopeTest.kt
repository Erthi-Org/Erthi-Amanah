package com.erthi.amanah.data.security

import javax.crypto.AEADBadTagException
import javax.crypto.spec.SecretKeySpec
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test

class VaultPinEnvelopeTest {
    private val key = SecretKeySpec(ByteArray(32) { it.toByte() }, "AES")

    @Test fun legacyPinCanRecoverTheSameKeyAfterAPinChange() {
        val legacy = VaultPinEnvelope.seal(key, "4321")
        val opened = VaultPinEnvelope.open(legacy, "4321")
        val changed = VaultPinEnvelope.seal(opened, "123456")
        assertArrayEquals(key.encoded, VaultPinEnvelope.open(changed, "123456").encoded)
        assertThrows(AEADBadTagException::class.java) { VaultPinEnvelope.open(changed, "4321") }
    }

    @Test fun repeatedWrappingUsesFreshSaltAndNonce() {
        val first = VaultPinEnvelope.seal(key, "123456")
        val second = VaultPinEnvelope.seal(key, "123456")
        assertFalse(first.copyOfRange(0, 32).contentEquals(second.copyOfRange(0, 32)))
        assertFalse(first.copyOfRange(32, 44).contentEquals(second.copyOfRange(32, 44)))
    }

    @Test fun alteredOrTruncatedEnvelopesAreRejected() {
        val envelope = VaultPinEnvelope.seal(key, "123456")
        assertThrows(IllegalArgumentException::class.java) {
            VaultPinEnvelope.open(envelope.copyOf(envelope.size - 1), "123456")
        }
        envelope[envelope.lastIndex] = (envelope.last().toInt() xor 1).toByte()
        assertThrows(AEADBadTagException::class.java) { VaultPinEnvelope.open(envelope, "123456") }
    }
}
