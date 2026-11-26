package com.erthi.amanah

import android.content.Context
import android.util.Base64
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.erthi.amanah.data.db.ErthiDatabase
import com.erthi.amanah.data.db.entity.VaultItemEntity
import com.erthi.amanah.data.security.CryptoManager
import com.erthi.amanah.data.security.VaultCrypto
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VaultKeyMigrationTest {
    @Test fun pinEnvelopeSurvivesRestartAndMigrationIsIdempotent() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, ErthiDatabase::class.java).build()
        try {
            val dao = db.vaultItemDao()
            val legacy = CryptoManager()
            val initial = VaultCrypto(legacy, db, dao)
            val ciphertext = legacy.encrypt("legacy password".toByteArray()).toByteArray()
            val old = "[enc]" + Base64.encodeToString(ciphertext, Base64.NO_WRAP)
            val id = dao.insert(VaultItemEntity(serviceName = "legacy service", password = old))
            val corrupt = VaultItemEntity(serviceName = "damaged", password = "[enc]invalid")
            val corruptId = dao.insert(corrupt)
            initial.unlockLegacy()
            assertEquals("legacy password", initial.decrypt(old))
            assertTrue(initial.encrypt("transitional").startsWith("[enc]"))
            initial.lock()
            val key = initial.newKey()
            val envelope = initial.sealForPin(key, "4321")
            initial.migrate(key)
            val migrated = checkNotNull(dao.getById(id))
            assertTrue(initial.isProtected(migrated.password))
            assertTrue(initial.isProtected(migrated.serviceName))
            assertEquals(corrupt.copy(id = corruptId), dao.getById(corruptId))
            try {
                initial.newKey()
                error("The vault should not generate a replacement key for migrated data")
            } catch (e: IllegalStateException) {
                assertEquals("Existing vault is missing its key envelope", e.message)
            }
            assertThrows(IllegalStateException::class.java) { initial.decrypt(migrated.password) }

            val restarted = VaultCrypto(legacy, db, dao)
            val recovered = restarted.openForPin(envelope, "4321")
            restarted.migrate(recovered)
            assertEquals(migrated, dao.getById(id))
            restarted.unlock(recovered)
            assertEquals("legacy password", restarted.decrypt(migrated.password))
            assertEquals("legacy service", restarted.decrypt(migrated.serviceName))
            restarted.lock()
            assertThrows(IllegalStateException::class.java) { restarted.decrypt(migrated.password) }
            Unit
        } finally {
            db.close()
        }
    }
}
