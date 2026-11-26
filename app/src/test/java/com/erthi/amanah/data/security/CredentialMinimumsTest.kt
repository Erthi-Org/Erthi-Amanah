package com.erthi.amanah.data.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CredentialMinimumsTest {
    @Test fun newPinsMustHaveSixToEightDigits() {
        assertFalse(CredentialMinimums.validNewPin("1234"))
        assertFalse(CredentialMinimums.validNewPin("12345x"))
        assertFalse(CredentialMinimums.validNewPin("123456789"))
        assertTrue(CredentialMinimums.validNewPin("123456"))
        assertTrue(CredentialMinimums.validNewPin("١٢٣٤٥٦"))
    }

    @Test fun newBackupPassphrasesMustHaveAtLeastTwelveCharacters() {
        assertFalse(CredentialMinimums.validNewBackupPassphrase("abcdefghijk"))
        assertFalse(CredentialMinimums.validNewBackupPassphrase(" ".repeat(12)))
        assertTrue(CredentialMinimums.validNewBackupPassphrase("abcdefghijkl"))
    }
}
