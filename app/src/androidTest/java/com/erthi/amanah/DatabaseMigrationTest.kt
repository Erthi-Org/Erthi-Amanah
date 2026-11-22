package com.erthi.amanah

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.erthi.amanah.data.db.ErthiDatabase
import com.erthi.amanah.data.db.entity.DocumentEntity
import com.erthi.amanah.data.di.MIGRATION_1_2
import com.erthi.amanah.data.di.MIGRATION_2_3
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {
    @Test
    fun fromV1ToV3UsesBothMigrationsAndPreservesDocuments() = verifyUpgrade(1)

    @Test
    fun fromV2ToV3PreservesDocuments() = verifyUpgrade(2)

    private fun verifyUpgrade(oldVersion: Int) = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "migration-fixture-${UUID.randomUUID()}.db"
        val legacyDocument = DocumentEntity(title = "Synthetic archived document", filePath = "legacy-path")

        fun open() = Room.databaseBuilder(context, ErthiDatabase::class.java, name)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3).build()
        try {
            open().use { it.documentDao().insertDocument(legacyDocument) }
            SQLiteDatabase.openDatabase(context.getDatabasePath(name).absolutePath, null, SQLiteDatabase.OPEN_READWRITE).use {
                // V2 added only vault_items. V3 added only restore_journal.
                it.execSQL("DROP TABLE restore_journal")
                if (oldVersion == 1) it.execSQL("DROP TABLE vault_items")
                it.execSQL("DROP TABLE room_master_table")
                it.version = oldVersion
            }
            open().use { migrated ->
                val document = migrated.documentDao().getAllDocuments().first().single()
                assertEquals(legacyDocument.title, document.title)
                assertEquals(legacyDocument.filePath, document.filePath)
                assertEquals(3, migrated.openHelper.readableDatabase.version)
                assertNull(migrated.restoreJournalDao().getPendingJournal())
            }
        } finally {
            context.deleteDatabase(name)
        }
    }
}
