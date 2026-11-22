package com.erthi.amanah

internal class PickerSession {
    private var requestCode: Int? = null
    private var startedAt = 0L

    fun begin(requestCode: Int, now: Long) {
        this.requestCode = requestCode
        startedAt = now
    }

    fun isActive(now: Long): Boolean = requestCode != null && now - startedAt in 0 until TIMEOUT_MS

    fun isExpectedResult(requestCode: Int): Boolean = this.requestCode == requestCode

    fun clear() {
        requestCode = null
        startedAt = 0
    }

    companion object {
        const val TIMEOUT_MS = 120_000L
    }
}
