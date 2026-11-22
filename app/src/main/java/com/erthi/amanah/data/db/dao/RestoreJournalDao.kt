package com.erthi.amanah.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.erthi.amanah.data.db.entity.RestoreJournalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RestoreJournalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJournal(journal: RestoreJournalEntity)

    @Query("SELECT * FROM restore_journal LIMIT 1")
    suspend fun getPendingJournal(): RestoreJournalEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM restore_journal)")
    fun hasPendingRestore(): Flow<Boolean>

    @Query("DELETE FROM restore_journal WHERE sessionId = :sessionId")
    suspend fun deleteJournal(sessionId: String)

    @Query("DELETE FROM restore_journal")
    suspend fun deleteAll()
}
