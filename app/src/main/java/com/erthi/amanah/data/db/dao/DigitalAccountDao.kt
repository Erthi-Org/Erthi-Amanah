package com.erthi.amanah.data.db.dao

import androidx.room.*
import com.erthi.amanah.data.db.entity.DigitalAccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DigitalAccountDao {
    @Query("SELECT * FROM digital_accounts ORDER BY createdAt DESC")
    fun getAllAccounts(): Flow<List<DigitalAccountEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: DigitalAccountEntity): Long

    @Update
    suspend fun updateAccount(account: DigitalAccountEntity)

    @Delete
    suspend fun deleteAccount(account: DigitalAccountEntity)

    @Query("DELETE FROM digital_accounts")
    suspend fun deleteAll()
}
