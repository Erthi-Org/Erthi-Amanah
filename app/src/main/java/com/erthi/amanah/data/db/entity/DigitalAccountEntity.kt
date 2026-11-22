package com.erthi.amanah.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "digital_accounts")
data class DigitalAccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val platform: String,
    val accountIdentifier: String,
    val instructions: String = "",
    val subscriptionCost: Double? = null,
    val currency: String = "EGP",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
