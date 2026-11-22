package com.erthi.amanah.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class VaultCategory {
    BANK, SOCIAL_MEDIA, EMAIL, WEBSITE, APP, OTHER
}

@Entity(tableName = "vault_items")
data class VaultItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val serviceName: String,          // encrypted
    val category: VaultCategory = VaultCategory.OTHER,
    val websiteUrl: String? = null,   // encrypted
    val username: String? = null,     // encrypted
    val email: String? = null,        // encrypted
    val password: String,             // encrypted
    val accountIdentifier: String? = null, // encrypted
    val recoveryEmail: String? = null, // encrypted
    val recoveryPhone: String? = null, // encrypted
    val recoveryCodes: String? = null, // encrypted
    val notes: String? = null,        // encrypted
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
