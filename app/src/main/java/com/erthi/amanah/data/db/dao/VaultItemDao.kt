package com.erthi.amanah.data.db.dao

import androidx.room.*
import com.erthi.amanah.data.db.entity.VaultCategory
import com.erthi.amanah.data.db.entity.VaultItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultItemDao {

    @Query("SELECT * FROM vault_items ORDER BY updatedAt DESC")
    fun getAllItems(): Flow<List<VaultItemEntity>>

    @Query("SELECT * FROM vault_items ORDER BY id")
    suspend fun getItemsForMigration(): List<VaultItemEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM vault_items WHERE serviceName LIKE '[vault2]%')")
    suspend fun hasProtectedItems(): Boolean

    @Query("SELECT * FROM vault_items WHERE category = :category ORDER BY updatedAt DESC")
    fun getByCategory(category: VaultCategory): Flow<List<VaultItemEntity>>

    @Query("SELECT * FROM vault_items WHERE id = :id")
    suspend fun getById(id: Long): VaultItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: VaultItemEntity): Long

    @Update
    suspend fun update(item: VaultItemEntity)

    @Delete
    suspend fun delete(item: VaultItemEntity)

    @Query("DELETE FROM vault_items")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM vault_items")
    suspend fun count(): Int
}
