package com.erthi.amanah

import com.erthi.amanah.util.PasswordStrength
import com.erthi.amanah.util.PasswordUtils
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for password strength classification — exercises PasswordUtils.passwordStrength()
 * which is the production function delegated to by VaultViewModel.passwordStrength().
 */
class PasswordStrengthTest {

    // ── WEAK ─────────────────────────────────────────────────────────────

    @Test
    fun empty_password_is_weak() {
        assertEquals(PasswordStrength.WEAK, PasswordUtils.passwordStrength(""))
    }

    @Test
    fun single_char_is_weak() {
        assertEquals(PasswordStrength.WEAK, PasswordUtils.passwordStrength("a"))
    }

    @Test
    fun short_lowercase_only_is_weak() {
        // length < 6 → WEAK immediately
        assertEquals(PasswordStrength.WEAK, PasswordUtils.passwordStrength("abcd"))
    }

    @Test
    fun digits_only_short_is_weak() {
        assertEquals(PasswordStrength.WEAK, PasswordUtils.passwordStrength("1234"))
    }

    @Test
    fun lowercase_and_digits_no_length_no_upper_is_weak() {
        // "abcd1234": lower(1) + digit(2) = score 2 → WEAK
        assertEquals(PasswordStrength.WEAK, PasswordUtils.passwordStrength("abcd1234"))
    }

    // ── MEDIUM ───────────────────────────────────────────────────────────

    @Test
    fun upper_lower_digit_is_medium() {
        // "Abcde123": upper(1) + lower(2) + digit(3) = score 3 → MEDIUM
        assertEquals(PasswordStrength.MEDIUM, PasswordUtils.passwordStrength("Abcde123"))
    }

    @Test
    fun lower_digit_symbol_is_medium() {
        // "abc12!@#": lower(1) + digit(2) + symbol(3) = score 3 → MEDIUM
        assertEquals(PasswordStrength.MEDIUM, PasswordUtils.passwordStrength("abc12!@#"))
    }

    @Test
    fun twelve_chars_lower_digit_is_medium() {
        // "abcdef123456": length>=12(1) + lower(2) + digit(3) = score 3 → MEDIUM
        assertEquals(PasswordStrength.MEDIUM, PasswordUtils.passwordStrength("abcdef123456"))
    }

    // ── STRONG ───────────────────────────────────────────────────────────

    @Test
    fun twelve_char_all_classes_is_strong() {
        // "Abcdef123!@#": length>=12(1) + upper(2) + lower(3) + digit(4) + symbol(5) → STRONG
        assertEquals(PasswordStrength.STRONG, PasswordUtils.passwordStrength("Abcdef123!@#"))
    }

    @Test
    fun sixteen_char_all_classes_is_strong() {
        assertEquals(PasswordStrength.STRONG, PasswordUtils.passwordStrength("MyP@ssw0rd!2024x"))
    }

    @Test
    fun upper_lower_digit_symbol_no_length_is_strong() {
        // "Abcde1!q": upper(1) + lower(2) + digit(3) + symbol(4) = score 4 → STRONG
        assertEquals(PasswordStrength.STRONG, PasswordUtils.passwordStrength("Abcde1!q"))
    }

    // ── Boundaries ───────────────────────────────────────────────────────

    @Test
    fun exactly_5_chars_is_weak_regardless_of_complexity() {
        // length < 6 → WEAK
        assertEquals(PasswordStrength.WEAK, PasswordUtils.passwordStrength("Ab1!x"))
    }

    @Test
    fun exactly_6_chars_is_scored_normally() {
        // "Ab1!xY": upper(1) + lower(2) + digit(3) + symbol(4) = score 4 → STRONG
        assertEquals(PasswordStrength.STRONG, PasswordUtils.passwordStrength("Ab1!xY"))
    }

    @Test
    fun exactly_12_chars_gets_length_bonus() {
        // "ABCDEFGHIJ12": length>=12(1) + upper(2) + digit(3) = score 3 → MEDIUM
        assertEquals(PasswordStrength.MEDIUM, PasswordUtils.passwordStrength("ABCDEFGHIJ12"))
    }

    // ── Generated password quality ────────────────────────────────────────

    @Test
    fun generated_password_all_classes_12_chars_is_strong() {
        // All char classes present, 12 chars
        assertEquals(PasswordStrength.STRONG, PasswordUtils.passwordStrength("aB3@dE6!gH9#"))
    }
}
