package com.erthi.amanah.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class DebtDirection { OWED_BY_ME, OWED_TO_ME }
enum class RepaymentStatus { PENDING, PARTIAL, PAID }

@Entity(tableName = "debts")
data class DebtEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val direction: DebtDirection,
    val personName: String,
    val amount: Double,
    val currency: String = "EGP",
    val dueDate: Long? = null,
    val description: String = "",
    val evidence: String = "",
    val status: RepaymentStatus = RepaymentStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
