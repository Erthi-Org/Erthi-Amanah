package com.erthi.amanah.data.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.erthi.amanah.data.db.ErthiDatabase
import com.erthi.amanah.data.db.dao.*
import com.erthi.amanah.data.db.dao.RestoreJournalDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """CREATE TABLE IF NOT EXISTS `restore_journal` (
                `sessionId` TEXT NOT NULL PRIMARY KEY,
                `stagedJson` TEXT NOT NULL,
                `stagingDirPath` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL
            )""".trimIndent()
        )
    }
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """CREATE TABLE IF NOT EXISTS `vault_items` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `serviceName` TEXT NOT NULL,
                `category` TEXT NOT NULL,
                `websiteUrl` TEXT,
                `username` TEXT,
                `email` TEXT,
                `password` TEXT NOT NULL,
                `accountIdentifier` TEXT,
                `recoveryEmail` TEXT,
                `recoveryPhone` TEXT,
                `recoveryCodes` TEXT,
                `notes` TEXT,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL
            )""".trimIndent()
        )
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ErthiDatabase {
        return Room.databaseBuilder(
            context,
            ErthiDatabase::class.java,
            "erthi_vault.db",
        )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides fun provideWillDao(db: ErthiDatabase): WillDao = db.willDao()
    @Provides fun provideDebtDao(db: ErthiDatabase): DebtDao = db.debtDao()
    @Provides fun provideAssetDao(db: ErthiDatabase): AssetDao = db.assetDao()
    @Provides fun provideEntrustedDao(db: ErthiDatabase): EntrustedDao = db.entrustedDao()
    @Provides fun provideDigitalAccountDao(db: ErthiDatabase): DigitalAccountDao = db.digitalAccountDao()
    @Provides fun provideDocumentDao(db: ErthiDatabase): DocumentDao = db.documentDao()
    @Provides fun provideBeneficiaryDao(db: ErthiDatabase): BeneficiaryDao = db.beneficiaryDao()
    @Provides fun provideVaultItemDao(db: ErthiDatabase): VaultItemDao = db.vaultItemDao()
    @Provides fun provideRestoreJournalDao(db: ErthiDatabase): RestoreJournalDao = db.restoreJournalDao()
}
