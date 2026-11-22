package com.erthi.amanah.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Tracks a restore in progress so a crash after DB commit can be recovered idempotently. */
@Entity(tableName = "restore_journal")
data class RestoreJournalEntity(
    @PrimaryKey val sessionId: String,
    /** JSON array of {staged, final} path pairs still needing file moves. */
    val stagedJson: String,
    val stagingDirPath: String,
    val createdAt: Long = System.currentTimeMillis(),
)
