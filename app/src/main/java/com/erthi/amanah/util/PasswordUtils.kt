package com.erthi.amanah.util

import java.security.SecureRandom

enum class PasswordStrength { WEAK, MEDIUM, STRONG }

object PasswordUtils {

    fun passwordStrength(password: String): PasswordStrength {
        if (password.length < 6) return PasswordStrength.WEAK
        var score = 0
        if (password.length >= 12) score++
        if (password.any { it.isUpperCase() }) score++
        if (password.any { it.isLowerCase() }) score++
        if (password.any { it.isDigit() }) score++
        if (password.any { !it.isLetterOrDigit() }) score++
        return when {
            score <= 2 -> PasswordStrength.WEAK
            score == 3 -> PasswordStrength.MEDIUM
            else -> PasswordStrength.STRONG
        }
    }

    fun generatePassword(
        length: Int = 16,
        upper: Boolean = true,
        lower: Boolean = true,
        digits: Boolean = true,
        symbols: Boolean = true,
    ): String {
        val pool = buildString {
            if (upper) append("ABCDEFGHIJKLMNOPQRSTUVWXYZ")
            if (lower) append("abcdefghijklmnopqrstuvwxyz")
            if (digits) append("0123456789")
            if (symbols) append("@#\$%^&*!_-+=?")
        }.ifEmpty { "abcdefghijklmnopqrstuvwxyz" }
        val rng = SecureRandom()
        return (1..length).map { pool[rng.nextInt(pool.length)] }.joinToString("")
    }
}
