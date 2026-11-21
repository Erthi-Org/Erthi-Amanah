package com.erthi.amanah.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.erthi.amanah.data.db.dao.*
import com.erthi.amanah.data.db.entity.*
import com.erthi.amanah.data.db.entity.RestoreJournalEntity

@Database(
    entities = [
        WillEntity::class,
        DebtEntity::class,
        AssetEntity::class,
        EntrustedEntity::class,
        DigitalAccountEntity::class,
        DocumentEntity::class,
        BeneficiaryEntity::class,
        VaultItemEntity::class,
        RestoreJournalEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class ErthiDatabase : RoomDatabase() {
    abstract fun willDao(): WillDao
    abstract fun debtDao(): DebtDao
    abstract fun assetDao(): AssetDao
    abstract fun entrustedDao(): EntrustedDao
    abstract fun digitalAccountDao(): DigitalAccountDao
    abstract fun documentDao(): DocumentDao
    abstract fun beneficiaryDao(): BeneficiaryDao
    abstract fun vaultItemDao(): VaultItemDao
    abstract fun restoreJournalDao(): RestoreJournalDao
}
