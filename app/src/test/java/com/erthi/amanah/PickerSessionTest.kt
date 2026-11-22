package com.erthi.amanah

import org.junit.Assert.*
import org.junit.Test

class PickerSessionTest {
    @Test
    fun onlyTheExpectedPickerIsExemptAndExemptionExpires() {
        val picker = PickerSession()
        assertFalse(picker.isActive(1))
        picker.begin(27, 100)
        assertTrue(picker.isActive(101))
        assertTrue(picker.isExpectedResult(27))
        assertFalse(picker.isExpectedResult(28))
        assertFalse(picker.isActive(100 + PickerSession.TIMEOUT_MS))
        picker.clear()
        assertFalse(picker.isExpectedResult(27))
        assertFalse(picker.isActive(101))
    }
}
