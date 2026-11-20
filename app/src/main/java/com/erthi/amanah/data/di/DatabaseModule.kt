package com.erthi.amanah.data.di

import android.content.Context
import androidx.room.Room
import com.erthi.amanah.data.db.ErthiDatabase
import com.erthi.amanah.data.db.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ErthiDatabase {
        // Database file is stored in app-private storage.
        // Additional encryption layer via CryptoManager is applied to sensitive fields.
        return Room.databaseBuilder(
            context,
            ErthiDatabase::class.java,
            "erthi_vault.db",
        )
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
}
