package com.erthi.amanah.data.db.dao

import androidx.room.*
import com.erthi.amanah.data.db.entity.DebtDirection
import com.erthi.amanah.data.db.entity.DebtEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DebtDao {
    @Query("SELECT * FROM debts ORDER BY createdAt DESC")
    fun getAllDebts(): Flow<List<DebtEntity>>

    @Query("SELECT * FROM debts WHERE direction = :direction ORDER BY createdAt DESC")
    fun getDebtsByDirection(direction: DebtDirection): Flow<List<DebtEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebt(debt: DebtEntity): Long

    @Update
    suspend fun updateDebt(debt: DebtEntity)

    @Delete
    suspend fun deleteDebt(debt: DebtEntity)

    @Query("DELETE FROM debts")
    suspend fun deleteAll()
}
