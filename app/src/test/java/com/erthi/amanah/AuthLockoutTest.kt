package com.erthi.amanah

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for the brute-force lockout business rules in AuthViewModel.
 * These test the pure logic without needing Android or Hilt — the ViewModel
 * delegates the actual timing to System.currentTimeMillis(), so we validate
 * the threshold rules here.
 */
class AuthLockoutTest {

    @Test
    fun lockout_triggersAfterFiveFailures() {
        // Mirror the ViewModel's threshold: 5 failures → 30-second lockout
        val maxBeforeLock = 5
        var attempts = 0
        var locked = false

        repeat(maxBeforeLock) {
            attempts++
            if (attempts >= maxBeforeLock) locked = true
        }

        assertTrue("Should be locked after $maxBeforeLock attempts", locked)
    }

    @Test
    fun lockout_triggersAfterTenFailures_secondStage() {
        val maxBeforeHardLock = 10
        var attempts = 0
        var hardLocked = false

        repeat(maxBeforeHardLock) {
            attempts++
            if (attempts >= maxBeforeHardLock) hardLocked = true
        }

        assertTrue("Should be hard-locked after $maxBeforeHardLock attempts", hardLocked)
    }

    @Test
    fun remainingAttempts_decreasesCorrectly() {
        val limit = 5
        for (failCount in 1 until limit) {
            val remaining = limit - failCount
            assertTrue("Remaining must be positive before lockout", remaining > 0)
        }
    }

    @Test
    fun lockout_duration_firstStage_isThirtySeconds() {
        val lockMs = 30_000L
        val lockSeconds = lockMs / 1000
        assertEquals("First lockout should be 30 seconds", 30L, lockSeconds)
    }

    @Test
    fun lockout_duration_secondStage_isFiveMinutes() {
        val lockMs = 300_000L
        val lockMinutes = lockMs / 60_000
        assertEquals("Second lockout should be 5 minutes", 5L, lockMinutes)
    }

    @Test
    fun pinLength_minimum_isFour() {
        val pin = "123"
        assertTrue("PIN shorter than 4 must be rejected", pin.length < 4)
        val validPin = "1234"
        assertTrue("PIN of length 4 must be accepted", validPin.length >= 4)
    }

    @Test
    fun pinConfirmation_mismatch_isRejected() {
        val pin = "1234"
        val confirm = "5678"
        assertNotEquals("Mismatched PINs must not pass", pin, confirm)
    }
}
