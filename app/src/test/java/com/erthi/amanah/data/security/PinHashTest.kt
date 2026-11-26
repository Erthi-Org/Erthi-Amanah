package com.erthi.amanah.data.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PinHashTest {
    @Test fun legacyPinCanBeVerifiedAndRehashedWithNewSalt() {
        val salt = PinHash.salt()
        val legacy = PinHash.derive("1234", salt, PinHash.LEGACY)
        assertFalse(PinHash.verify("1235", salt, legacy, PinHash.LEGACY))
        assertTrue(PinHash.verify("1234", salt, legacy, PinHash.LEGACY))
        val newSalt = PinHash.salt()
        val migrated = PinHash.derive("1234", newSalt, PinHash.PBKDF2)
        assertFalse(salt.contentEquals(newSalt))
        assertTrue(PinHash.verify("1234", newSalt, migrated, PinHash.PBKDF2))
        assertFalse(PinHash.verify("1234", salt, migrated, PinHash.PBKDF2))
        assertFalse(PinHash.verify("1234", newSalt, migrated, 99))
    }

    @Test fun pbkdf2MatchesIndependentVector() {
        val hash = PinHash.derive("123456", ByteArray(32), PinHash.PBKDF2)
        assertEquals(
            "5be5771936221fef6c02fa26fe8503c16c3c601aaea4cf96a4b76cfcc81bafa8",
            hash.joinToString("") { "%02x".format(it) },
        )
    }
}
