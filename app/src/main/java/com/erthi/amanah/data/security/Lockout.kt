package com.erthi.amanah.data.security

internal data class Lockout(val boot: Int, val startedMs: Long, val durationMs: Long) {
    fun needsRestart(currentBoot: Int, nowMs: Long): Boolean =
        boot != currentBoot || nowMs < startedMs

    fun remainingMs(nowMs: Long): Long = (durationMs - (nowMs - startedMs)).coerceAtLeast(0)

    companion object {
        const val MAX_DURATION_MS = 300_000L

        fun durationFor(attempts: Int): Long = when {
            attempts >= 10 -> MAX_DURATION_MS
            attempts >= 5 -> 30_000L
            else -> 0L
        }
    }
}
