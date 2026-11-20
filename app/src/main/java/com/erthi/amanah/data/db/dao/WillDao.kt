package com.erthi.amanah.data.db.dao

import androidx.room.*
import com.erthi.amanah.data.db.entity.WillEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WillDao {
    @Query("SELECT * FROM wills ORDER BY updatedAt DESC")
    fun getAllWills(): Flow<List<WillEntity>>

    @Query("SELECT * FROM wills WHERE id = :id")
    suspend fun getWillById(id: Long): WillEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWill(will: WillEntity): Long

    @Update
    suspend fun updateWill(will: WillEntity)

    @Delete
    suspend fun deleteWill(will: WillEntity)

    @Query("DELETE FROM wills")
    suspend fun deleteAllWills()
}
