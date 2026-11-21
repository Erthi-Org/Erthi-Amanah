package com.erthi.amanah.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AssetType {
    BANK_ACCOUNT, REAL_ESTATE, VEHICLE, GOLD, BUSINESS, INVESTMENT, OTHER
}

@Entity(tableName = "assets")
data class AssetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: AssetType,
    val name: String,
    val description: String = "",
    val estimatedValue: Double? = null,
    val currency: String = "EGP",
    val location: String = "",
    val accountReference: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
