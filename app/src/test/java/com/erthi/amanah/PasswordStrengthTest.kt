package com.erthi.amanah

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for password strength classification logic used in VaultViewModel.
 * Mirrors the evaluateStrength() logic: score-based with WEAK/MEDIUM/STRONG levels.
 */
class PasswordStrengthTest {

    private enum class PasswordStrength { WEAK, MEDIUM, STRONG }

    private fun evaluateStrength(password: String): PasswordStrength {
        var score = 0
        if (password.length >= 8) score++
        if (password.length >= 12) score++
        if (password.any { it.isUpperCase() }) score++
        if (password.any { it.isLowerCase() }) score++
        if (password.any { it.isDigit() }) score++
        if (password.any { !it.isLetterOrDigit() }) score++
        return when {
            score >= 5 -> PasswordStrength.STRONG
            score >= 3 -> PasswordStrength.MEDIUM
            else -> PasswordStrength.WEAK
        }
    }

    // ── WEAK ─────────────────────────────────────────────────────────────

    @Test
    fun empty_password_is_weak() {
        assertEquals(PasswordStrength.WEAK, evaluateStrength(""))
    }

    @Test
    fun single_char_is_weak() {
        assertEquals(PasswordStrength.WEAK, evaluateStrength("a"))
    }

    @Test
    fun short_lowercase_only_is_weak() {
        assertEquals(PasswordStrength.WEAK, evaluateStrength("abcd"))
    }

    @Test
    fun digits_only_short_is_weak() {
        assertEquals(PasswordStrength.WEAK, evaluateStrength("1234"))
    }

    // ── MEDIUM ───────────────────────────────────────────────────────────

    @Test
    fun eight_char_mixed_case_is_medium() {
        // length>=8(1) + upper(1) + lower(1) + digit(1) = 4 → MEDIUM (>=3)
        assertEquals(PasswordStrength.MEDIUM, evaluateStrength("Abcde123"))
    }

    @Test
    fun eight_char_letters_and_digits_is_medium() {
        assertEquals(PasswordStrength.MEDIUM, evaluateStrength("abcd1234"))
    }

    // ── STRONG ───────────────────────────────────────────────────────────

    @Test
    fun twelve_char_mixed_with_symbol_is_strong() {
        // length>=8(1) + length>=12(1) + upper(1) + lower(1) + digit(1) + special(1) = 6 → STRONG
        assertEquals(PasswordStrength.STRONG, evaluateStrength("Abcdef123!@#"))
    }

    @Test
    fun sixteen_char_all_char_classes_is_strong() {
        assertEquals(PasswordStrength.STRONG, evaluateStrength("MyP@ssw0rd!2024x"))
    }

    @Test
    fun symbol_only_twelve_chars_hits_at_least_medium() {
        // length>=8(1) + length>=12(1) + special(1) = 3 → MEDIUM (not WEAK)
        val result = evaluateStrength("!@#\$%^&*()!@")
        assertNotEquals(PasswordStrength.WEAK, result)
    }

    // ── Boundaries ───────────────────────────────────────────────────────

    @Test
    fun exactly_8_chars_counts_length_bonus() {
        // "abcABC1!" → length>=8(1) + upper(1) + lower(1) + digit(1) + special(1) = 5 → STRONG
        assertEquals(PasswordStrength.STRONG, evaluateStrength("abcABC1!"))
    }

    @Test
    fun exactly_12_chars_counts_both_length_bonuses() {
        // "abcABC123!@#" → length>=8(1) + length>=12(1) + upper(1) + lower(1) + digit(1) + special(1) = 6 → STRONG
        assertEquals(PasswordStrength.STRONG, evaluateStrength("abcABC123!@#"))
    }

    @Test
    fun seven_chars_no_length_bonus() {
        // "Abcde1!" → upper(1) + lower(1) + digit(1) + special(1) = 4 → MEDIUM
        assertEquals(PasswordStrength.MEDIUM, evaluateStrength("Abcde1!"))
    }

    // ── Generated password quality ────────────────────────────────────────

    @Test
    fun generated_12_char_alphanumeric_symbol_is_strong() {
        // Simulated generated password with all classes
        assertEquals(PasswordStrength.STRONG, evaluateStrength("aB3!dE6@gH9#"))
    }
}
