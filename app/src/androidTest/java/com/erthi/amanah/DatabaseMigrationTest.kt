package com.erthi.amanah

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.erthi.amanah.data.db.ErthiDatabase
import com.erthi.amanah.data.di.MIGRATION_1_2
import com.erthi.amanah.data.di.MIGRATION_2_3
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/**
 * Migration tests using MigrationTestHelper against exported Room schema JSONs
 * (app/schemas/com.erthi.amanah.data.db.ErthiDatabase/{1,2,3}.json).
 * Each test creates a database at an old version using the actual historical schema,
 * populates it with synthetic data, runs the real migrations, and verifies both
 * schema validity and data preservation.
 */
@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        ErthiDatabase::class.java,
    )

    // ── V1 → V2 ──────────────────────────────────────────────────────────────

    @Test
    fun migrate1To2_allV1TablesPreservedAndVaultItemsCreated() {
        val name = "migration-1-2-${UUID.randomUUID()}"
        // Create a V1 database using the REAL historical schema JSON.
        helper.createDatabase(name, 1).use { db ->
            db.execSQL("INSERT INTO wills (title, content, isDraft, legalDisclaimer, createdAt, updatedAt) VALUES ('My Will', 'Body text', 0, 1, 1000, 2000)")
            db.execSQL("INSERT INTO documents (title, category, filePath, mimeType, notes, createdAt, updatedAt) VALUES ('Passport', 'personal', '/data/app/passport.wasi_doc', 'application/pdf', '', 1000, 2000)")
            db.execSQL("INSERT INTO beneficiaries (fullName, relation, phone, email, nationalId, isEmergencyContact, emergencyPriority, canAccessWill, notes, createdAt, updatedAt) VALUES ('Ahmad', 'son', '+966500000001', 'a@a.com', '', 0, 0, 1, '', 1000, 2000)")
        }
        // Run migration to V2 and validate against the actual V2 schema.
        helper.runMigrationsAndValidate(name, 2, true, MIGRATION_1_2).use { db ->
            // Wills preserved.
            val willsCursor = db.query("SELECT title FROM wills")
            assertEquals(1, willsCursor.count)
            willsCursor.moveToFirst()
            assertEquals("My Will", willsCursor.getString(0))
            willsCursor.close()
            // Documents preserved.
            val docsCursor = db.query("SELECT title FROM documents")
            assertEquals(1, docsCursor.count)
            docsCursor.close()
            // Beneficiaries preserved.
            val benCursor = db.query("SELECT fullName FROM beneficiaries")
            assertEquals(1, benCursor.count)
            benCursor.close()
            // vault_items table was added.
            val tblCursor = db.query(
                "SELECT name FROM sqlite_master WHERE type='table' AND name='vault_items'"
            )
            assertEquals(1, tblCursor.count)
            tblCursor.close()
        }
    }

    // ── V2 → V3 ──────────────────────────────────────────────────────────────

    @Test
    fun migrate2To3_vaultItemsPreservedAndRestoreJournalCreated() {
        val name = "migration-2-3-${UUID.randomUUID()}"
        helper.createDatabase(name, 2).use { db ->
            db.execSQL("INSERT INTO wills (title, content, isDraft, legalDisclaimer, createdAt, updatedAt) VALUES ('V2 Will', 'Content', 0, 1, 1000, 2000)")
            db.execSQL(
                "INSERT INTO vault_items (serviceName, category, password, createdAt, updatedAt) " +
                    "VALUES ('Google', 'email', 'encrypted_pass', 1000, 2000)"
            )
        }
        helper.runMigrationsAndValidate(name, 3, true, MIGRATION_2_3).use { db ->
            // Wills preserved.
            val wCursor = db.query("SELECT title FROM wills")
            assertEquals(1, wCursor.count)
            wCursor.close()
            // vault_items preserved.
            val vCursor = db.query("SELECT serviceName FROM vault_items")
            assertEquals(1, vCursor.count)
            vCursor.moveToFirst()
            assertEquals("Google", vCursor.getString(0))
            vCursor.close()
            // restore_journal was added.
            val jCursor = db.query(
                "SELECT name FROM sqlite_master WHERE type='table' AND name='restore_journal'"
            )
            assertEquals(1, jCursor.count)
            jCursor.close()
            // restore_journal is empty after a fresh migration.
            val emptyCursor = db.query("SELECT * FROM restore_journal")
            assertEquals(0, emptyCursor.count)
            emptyCursor.close()
        }
    }

    // ── V1 → V3 (both migrations in one shot) ────────────────────────────────

    @Test
    fun migrate1To3_allV1DataPreservedBothNewTablesCreated() {
        val name = "migration-1-3-${UUID.randomUUID()}"
        helper.createDatabase(name, 1).use { db ->
            db.execSQL("INSERT INTO wills (title, content, isDraft, legalDisclaimer, createdAt, updatedAt) VALUES ('Direct V1 Will', 'V1 content', 0, 1, 1000, 2000)")
            db.execSQL("INSERT INTO documents (title, category, filePath, mimeType, notes, createdAt, updatedAt) VALUES ('V1 Doc', 'official', '/data/doc.wasi_doc', 'application/pdf', 'notes', 1000, 2000)")
            db.execSQL("INSERT INTO debts (direction, personName, amount, currency, description, evidence, status, createdAt, updatedAt) VALUES ('owed_by_me', 'Khalid', 500.0, 'SAR', 'Car loan', '', 'pending', 1000, 2000)")
            db.execSQL("INSERT INTO assets (type, name, description, currency, location, accountReference, notes, createdAt, updatedAt) VALUES ('real_estate', 'House', 'Family home', 'SAR', 'Riyadh', '', '', 1000, 2000)")
        }
        helper.runMigrationsAndValidate(name, 3, true, MIGRATION_1_2, MIGRATION_2_3).use { db ->
            // All V1 tables preserved.
            db.query("SELECT title FROM wills").use { c ->
                assertEquals(1, c.count)
                c.moveToFirst()
                assertEquals("Direct V1 Will", c.getString(0))
            }
            db.query("SELECT title FROM documents").use { c -> assertEquals(1, c.count) }
            db.query("SELECT personName FROM debts").use { c ->
                assertEquals(1, c.count)
                c.moveToFirst()
                assertEquals("Khalid", c.getString(0))
            }
            db.query("SELECT name FROM assets").use { c -> assertEquals(1, c.count) }
            // Both new tables exist.
            db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='vault_items'").use {
                assertEquals("vault_items table must exist after v1→v3 migration", 1, it.count)
            }
            db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='restore_journal'").use {
                assertEquals("restore_journal table must exist after v1→v3 migration", 1, it.count)
            }
            // Final version is 3.
            assertEquals(3, db.version)
        }
    }
}
