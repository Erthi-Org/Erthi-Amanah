package com.erthi.amanah

import com.erthi.amanah.util.PasswordUtils
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for password generation — exercises PasswordUtils.generatePassword()
 * which is the production function delegated to by VaultViewModel.generatePassword().
 */
class PasswordGeneratorTest {

    // ── Length ───────────────────────────────────────────────────────────

    @Test
    fun generated_password_has_correct_length_8() {
        val pwd = PasswordUtils.generatePassword(8, upper = true, lower = true, digits = true, symbols = true)
        assertEquals(8, pwd.length)
    }

    @Test
    fun generated_password_has_correct_length_12() {
        val pwd = PasswordUtils.generatePassword(12, upper = true, lower = true, digits = true, symbols = true)
        assertEquals(12, pwd.length)
    }

    @Test
    fun generated_password_has_correct_length_16() {
        val pwd = PasswordUtils.generatePassword(16, upper = true, lower = true, digits = true, symbols = false)
        assertEquals(16, pwd.length)
    }

    @Test
    fun generated_password_has_correct_length_32() {
        val pwd = PasswordUtils.generatePassword(32, upper = true, lower = true, digits = true, symbols = true)
        assertEquals(32, pwd.length)
    }

    // ── Non-null, non-empty ───────────────────────────────────────────────

    @Test
    fun generated_password_is_not_empty() {
        val pwd = PasswordUtils.generatePassword(12, upper = true, lower = true, digits = true, symbols = true)
        assertTrue(pwd.isNotEmpty())
    }

    // ── Charset composition ───────────────────────────────────────────────

    @Test
    fun uppercase_only_generates_uppercase_chars() {
        repeat(10) {
            val pwd = PasswordUtils.generatePassword(20, upper = true, lower = false, digits = false, symbols = false)
            assertTrue("Expected all uppercase: $pwd", pwd.all { it.isUpperCase() })
        }
    }

    @Test
    fun lowercase_only_generates_lowercase_chars() {
        repeat(10) {
            val pwd = PasswordUtils.generatePassword(20, upper = false, lower = true, digits = false, symbols = false)
            assertTrue("Expected all lowercase: $pwd", pwd.all { it.isLowerCase() })
        }
    }

    @Test
    fun digits_only_generates_digit_chars() {
        repeat(10) {
            val pwd = PasswordUtils.generatePassword(20, upper = false, lower = false, digits = true, symbols = false)
            assertTrue("Expected all digits: $pwd", pwd.all { it.isDigit() })
        }
    }

    @Test
    fun symbols_only_uses_production_symbol_pool() {
        // Production symbol pool: @#$%^&*!_-+=?
        val symbolPool = "@#\$%^&*!_-+=?"
        repeat(20) {
            val pwd = PasswordUtils.generatePassword(20, upper = false, lower = false, digits = false, symbols = true)
            assertTrue("All chars must be in production symbol pool: $pwd", pwd.all { it in symbolPool })
        }
    }

    @Test
    fun full_charset_password_uses_production_pool() {
        val pool = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@#\$%^&*!_-+=?"
        repeat(20) {
            val pwd = PasswordUtils.generatePassword(20, upper = true, lower = true, digits = true, symbols = true)
            assertTrue("All chars must be in production pool: $pwd", pwd.all { it in pool })
        }
    }

    // ── Randomness (statistical) ──────────────────────────────────────────

    @Test
    fun two_generated_passwords_are_different() {
        val a = PasswordUtils.generatePassword(16, upper = true, lower = true, digits = true, symbols = true)
        val b = PasswordUtils.generatePassword(16, upper = true, lower = true, digits = true, symbols = true)
        assertNotEquals(a, b)
    }

    @Test
    fun generated_passwords_have_entropy() {
        repeat(5) {
            val pwd = PasswordUtils.generatePassword(16, upper = true, lower = true, digits = true, symbols = true)
            val distinctChars = pwd.toSet().size
            assertTrue("Expected at least 4 distinct chars in 16-char password: $pwd", distinctChars >= 4)
        }
    }

    // ── Fallback when all options disabled ────────────────────────────────

    @Test
    fun no_options_selected_still_returns_non_empty_password() {
        val pwd = PasswordUtils.generatePassword(12, upper = false, lower = false, digits = false, symbols = false)
        assertTrue(pwd.isNotEmpty())
        assertEquals(12, pwd.length)
        // Falls back to lowercase alphabet
        assertTrue("Fallback must be lowercase: $pwd", pwd.all { it.isLowerCase() })
    }
}
