package com.erthi.amanah

import org.junit.Assert.*
import org.junit.Test
import java.security.SecureRandom

/**
 * Tests for the SecureRandom-based password generator used in VaultViewModel.
 * Mirrors the generatePassword() logic exactly.
 */
class PasswordGeneratorTest {

    private fun generatePassword(
        length: Int,
        useUppercase: Boolean,
        useLowercase: Boolean,
        useDigits: Boolean,
        useSymbols: Boolean,
    ): String {
        val upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        val lower = "abcdefghijklmnopqrstuvwxyz"
        val digits = "0123456789"
        val symbols = "!@#\$%^&*()-_=+[]{}|;:,.<>?"
        val pool = buildString {
            if (useUppercase) append(upper)
            if (useLowercase) append(lower)
            if (useDigits) append(digits)
            if (useSymbols) append(symbols)
        }.ifEmpty { lower } // fallback: at least lowercase
        val rng = SecureRandom()
        return (1..length).map { pool[rng.nextInt(pool.length)] }.joinToString("")
    }

    // ── Length ───────────────────────────────────────────────────────────

    @Test
    fun generated_password_has_correct_length_8() {
        val pwd = generatePassword(8, true, true, true, true)
        assertEquals(8, pwd.length)
    }

    @Test
    fun generated_password_has_correct_length_12() {
        val pwd = generatePassword(12, true, true, true, true)
        assertEquals(12, pwd.length)
    }

    @Test
    fun generated_password_has_correct_length_16() {
        val pwd = generatePassword(16, true, true, true, false)
        assertEquals(16, pwd.length)
    }

    @Test
    fun generated_password_has_correct_length_32() {
        val pwd = generatePassword(32, true, true, true, true)
        assertEquals(32, pwd.length)
    }

    // ── Non-null, non-empty ───────────────────────────────────────────────

    @Test
    fun generated_password_is_not_null() {
        val pwd = generatePassword(12, true, true, true, true)
        assertNotNull(pwd)
    }

    @Test
    fun generated_password_is_not_empty() {
        val pwd = generatePassword(12, true, true, true, true)
        assertTrue(pwd.isNotEmpty())
    }

    // ── Charset composition ───────────────────────────────────────────────

    @Test
    fun uppercase_only_generates_uppercase_chars() {
        repeat(10) {
            val pwd = generatePassword(20, true, false, false, false)
            assertTrue("Expected all uppercase: $pwd", pwd.all { it.isUpperCase() })
        }
    }

    @Test
    fun lowercase_only_generates_lowercase_chars() {
        repeat(10) {
            val pwd = generatePassword(20, false, true, false, false)
            assertTrue("Expected all lowercase: $pwd", pwd.all { it.isLowerCase() })
        }
    }

    @Test
    fun digits_only_generates_digit_chars() {
        repeat(10) {
            val pwd = generatePassword(20, false, false, true, false)
            assertTrue("Expected all digits: $pwd", pwd.all { it.isDigit() })
        }
    }

    @Test
    fun full_charset_password_contains_chars_from_pool() {
        val pool = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#\$%^&*()-_=+[]{}|;:,.<>?"
        repeat(20) {
            val pwd = generatePassword(20, true, true, true, true)
            assertTrue("All chars must be in pool: $pwd", pwd.all { it in pool })
        }
    }

    // ── Randomness (statistical) ──────────────────────────────────────────

    @Test
    fun two_generated_passwords_are_different() {
        val a = generatePassword(16, true, true, true, true)
        val b = generatePassword(16, true, true, true, true)
        // Statistically near-impossible to collide; if this fails SecureRandom is broken
        assertNotEquals(a, b)
    }

    @Test
    fun generated_passwords_have_entropy_no_all_same_char() {
        repeat(5) {
            val pwd = generatePassword(16, true, true, true, true)
            val distinctChars = pwd.toSet().size
            assertTrue("Expected at least 4 distinct chars in 16-char password: $pwd", distinctChars >= 4)
        }
    }

    // ── Fallback when all options disabled ────────────────────────────────

    @Test
    fun no_options_selected_still_returns_non_empty_password() {
        val pwd = generatePassword(12, false, false, false, false)
        assertTrue(pwd.isNotEmpty())
        assertEquals(12, pwd.length)
    }
}
