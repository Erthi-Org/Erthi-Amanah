package com.erthi.amanah.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "entrusted_items")
data class EntrustedEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerName: String,
    val ownerPhone: String = "",
    val itemDescription: String,
    val returnInstructions: String = "",
    val evidence: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
