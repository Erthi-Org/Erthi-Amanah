package com.erthi.amanah.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "beneficiaries")
data class BeneficiaryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fullName: String,
    val relation: String,
    val phone: String = "",
    val email: String = "",
    val nationalId: String = "",
    val isEmergencyContact: Boolean = false,
    val emergencyPriority: Int = 0,
    val canAccessWill: Boolean = false,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
