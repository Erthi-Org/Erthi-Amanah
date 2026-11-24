package com.erthi.amanah.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.erthi.amanah.data.db.dao.AssetDao
import com.erthi.amanah.data.db.dao.BeneficiaryDao
import com.erthi.amanah.data.db.dao.DebtDao
import com.erthi.amanah.data.db.dao.DigitalAccountDao
import com.erthi.amanah.data.db.dao.EntrustedDao
import com.erthi.amanah.data.security.FieldCipher
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * One-time migration that encrypts any plaintext rows left by the pre-encryption app
 * version.  Runs on first launch after update; subsequent launches skip instantly via
 * a SharedPreferences flag.  enc() is idempotent — already-encrypted rows are unaffected.
 */
@HiltWorker
class FieldEncryptionMigrationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val cipher: FieldCipher,
    private val beneficiaryDao: BeneficiaryDao,
    private val debtDao: DebtDao,
    private val assetDao: AssetDao,
    private val entrustedDao: EntrustedDao,
    private val digitalAccountDao: DigitalAccountDao,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_DONE, false)) return@withContext Result.success()

        beneficiaryDao.getAllBeneficiaries().first()
            .filter { !it.fullName.startsWith(cipher.ENC) }
            .forEach { beneficiaryDao.updateBeneficiary(cipher.encrypt(it)) }

        debtDao.getAllDebts().first()
            .filter { !it.personName.startsWith(cipher.ENC) }
            .forEach { debtDao.updateDebt(cipher.encrypt(it)) }

        assetDao.getAllAssets().first()
            .filter { !it.name.startsWith(cipher.ENC) }
            .forEach { assetDao.updateAsset(cipher.encrypt(it)) }

        entrustedDao.getAllEntrusted().first()
            .filter { !it.ownerName.startsWith(cipher.ENC) }
            .forEach { entrustedDao.updateEntrusted(cipher.encrypt(it)) }

        digitalAccountDao.getAllAccounts().first()
            .filter { !it.platform.startsWith(cipher.ENC) }
            .forEach { digitalAccountDao.updateAccount(cipher.encrypt(it)) }

        prefs.edit().putBoolean(KEY_DONE, true).apply()
        Result.success()
    }

    companion object {
        const val PREFS_NAME = "erthi_field_migration"
        const val KEY_DONE   = "field_encryption_migrated_v1"
    }
}
