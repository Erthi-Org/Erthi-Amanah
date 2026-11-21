package com.erthi.amanah.data.backup

data class DriveBackupFile(
    val id: String,
    val name: String,
    val sizeBytes: Long,
    val createdTimeMs: Long,
    val modifiedTimeMs: Long,
)
