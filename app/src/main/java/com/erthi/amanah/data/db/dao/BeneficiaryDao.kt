package com.erthi.amanah.data.db.dao

import androidx.room.*
import com.erthi.amanah.data.db.entity.BeneficiaryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BeneficiaryDao {
    @Query("SELECT * FROM beneficiaries ORDER BY emergencyPriority ASC, fullName ASC")
    fun getAllBeneficiaries(): Flow<List<BeneficiaryEntity>>

    @Query("SELECT * FROM beneficiaries WHERE isEmergencyContact = 1 ORDER BY emergencyPriority ASC")
    fun getEmergencyContacts(): Flow<List<BeneficiaryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBeneficiary(beneficiary: BeneficiaryEntity): Long

    @Update
    suspend fun updateBeneficiary(beneficiary: BeneficiaryEntity)

    @Delete
    suspend fun deleteBeneficiary(beneficiary: BeneficiaryEntity)

    @Query("DELETE FROM beneficiaries")
    suspend fun deleteAll()
}
