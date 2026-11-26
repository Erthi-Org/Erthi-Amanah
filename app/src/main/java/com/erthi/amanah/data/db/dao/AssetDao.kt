package com.erthi.amanah.data.db.dao

import androidx.room.*
import com.erthi.amanah.data.db.entity.AssetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AssetDao {
    @Query("SELECT * FROM assets ORDER BY createdAt DESC")
    fun getAllAssets(): Flow<List<AssetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAsset(asset: AssetEntity): Long

    @Update
    suspend fun updateAsset(asset: AssetEntity)

    @Delete
    suspend fun deleteAsset(asset: AssetEntity)

    @Query("DELETE FROM assets")
    suspend fun deleteAll()
}
