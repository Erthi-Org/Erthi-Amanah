package com.erthi.amanah.data.db.dao

import androidx.room.*
import com.erthi.amanah.data.db.entity.EntrustedEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EntrustedDao {
    @Query("SELECT * FROM entrusted_items ORDER BY createdAt DESC")
    fun getAllEntrusted(): Flow<List<EntrustedEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntrusted(item: EntrustedEntity): Long

    @Update
    suspend fun updateEntrusted(item: EntrustedEntity)

    @Delete
    suspend fun deleteEntrusted(item: EntrustedEntity)

    @Query("DELETE FROM entrusted_items")
    suspend fun deleteAll()
}
