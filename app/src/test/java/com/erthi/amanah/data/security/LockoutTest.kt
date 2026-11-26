package com.erthi.amanah.data.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LockoutTest {
    @Test fun lockoutCountsDownWithElapsedTime() {
        val lockout = Lockout(4, 5_000, 30_000)
        assertFalse(lockout.needsRestart(4, 15_000))
        assertEquals(20_000L, lockout.remainingMs(15_000))
        assertEquals(0L, lockout.remainingMs(35_001))
    }

    @Test fun rebootRequiresRestartEvenWithHigherUptime() {
        val lockout = Lockout(4, 5_000, 30_000)
        assertTrue(lockout.needsRestart(5, 60_000))
        assertTrue(lockout.needsRestart(4, 4_999))
    }

    @Test fun penaltiesEscalateAcrossAttempts() {
        assertEquals(0L, Lockout.durationFor(4))
        assertEquals(30_000L, Lockout.durationFor(5))
        assertEquals(300_000L, Lockout.durationFor(10))
    }
}
