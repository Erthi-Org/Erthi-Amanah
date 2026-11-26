package com.erthi.amanah

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.erthi.amanah.data.db.entity.AssetEntity
import com.erthi.amanah.data.db.entity.AssetType
import com.erthi.amanah.data.db.entity.BeneficiaryEntity
import com.erthi.amanah.data.db.entity.DebtDirection
import com.erthi.amanah.data.db.entity.DebtEntity
import com.erthi.amanah.data.db.entity.DigitalAccountEntity
import com.erthi.amanah.data.db.entity.EntrustedEntity
import com.erthi.amanah.data.security.CryptoManager
import com.erthi.amanah.data.security.FieldCipher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for [FieldCipher] — requires Android Keystore (real device / emulator).
 *
 * Covers:
 *  1. enc → dec round-trip for a plain string
 *  2. dec() on legacy plaintext (no [enc] prefix) returns unchanged value — migration fallback
 *  3. enc() is idempotent — double-encrypting leaves the ciphertext unchanged
 *  4. Per-entity round-trips (encrypt then decrypt recovers the original entity)
 */
@RunWith(AndroidJUnit4::class)
class FieldCipherTest {

    private lateinit var cipher: FieldCipher

    @Before
    fun setUp() {
        cipher = FieldCipher(CryptoManager())
    }

    // ── String-level ──────────────────────────────────────────────────────

    @Test
    fun encDec_roundTrip() {
        val plain = "محمد علي — test@example.com — 01012345678"
        val ciphertext = cipher.enc(plain)
        assertTrue("Encrypted value must carry [enc] prefix", ciphertext.startsWith("[enc]"))
        assertEquals("dec(enc(x)) must equal x", plain, cipher.dec(ciphertext))
    }

    @Test
    fun dec_legacyPlaintext_returnsUnchanged() {
        val legacy = "Ahmad Khalil"
        assertEquals("dec() on plaintext without prefix must return value as-is", legacy, cipher.dec(legacy))
    }

    @Test
    fun enc_isIdempotent() {
        val plain = "idempotence test"
        val once  = cipher.enc(plain)
        val twice = cipher.enc(once)
        assertEquals("enc(enc(x)) must equal enc(x)", once, twice)
    }

    @Test
    fun dec_emptyString_returnsEmpty() {
        assertEquals("", cipher.dec(""))
    }

    // ── Entity round-trips ────────────────────────────────────────────────

    @Test
    fun beneficiaryEntity_roundTrip() {
        val original = BeneficiaryEntity(
            id = 1L, fullName = "فاطمة محمود", relation = "زوجة",
            phone = "01011111111", email = "f@example.com",
            nationalId = "29901010100000", isEmergencyContact = true,
            emergencyPriority = 1, canAccessWill = true, notes = "ملاحظة",
        )
        val recovered = cipher.decrypt(cipher.encrypt(original))
        assertEquals(original, recovered)
    }

    @Test
    fun debtEntity_roundTrip() {
        val original = DebtEntity(
            id = 2L, direction = DebtDirection.OWED_BY_ME,
            personName = "خالد إبراهيم", amount = 5000.0, currency = "EGP",
            description = "قرض شخصي", evidence = "مستند PDF",
        )
        val recovered = cipher.decrypt(cipher.encrypt(original))
        assertEquals(original, recovered)
    }

    @Test
    fun assetEntity_roundTrip() {
        val original = AssetEntity(
            id = 3L, type = AssetType.BANK_ACCOUNT,
            name = "حساب البنك الأهلي", description = "حساب توفير",
            estimatedValue = 100_000.0, location = "القاهرة",
            accountReference = "123456789", notes = "ملاحظة الأصل",
        )
        val recovered = cipher.decrypt(cipher.encrypt(original))
        assertEquals(original, recovered)
    }

    @Test
    fun entrustedEntity_roundTrip() {
        val original = EntrustedEntity(
            id = 4L, ownerName = "محمود سعيد", ownerPhone = "01099999999",
            itemDescription = "ساعة ذهبية", returnInstructions = "يُعاد عند الوفاة",
            evidence = "صورة الساعة",
        )
        val recovered = cipher.decrypt(cipher.encrypt(original))
        assertEquals(original, recovered)
    }

    @Test
    fun digitalAccountEntity_roundTrip() {
        val original = DigitalAccountEntity(
            id = 5L, platform = "Gmail", accountIdentifier = "user@gmail.com",
            instructions = "احذف الحساب بعد 90 يوماً", notes = "ملاحظة رقمية",
        )
        val recovered = cipher.decrypt(cipher.encrypt(original))
        assertEquals(original, recovered)
    }

    // ── Migration idempotence ─────────────────────────────────────────────

    @Test
    fun migrationIdempotence_plaintext_encryptsThenStaysEncrypted() {
        val plain = "بيانات قديمة غير مشفرة"
        // Simulate migration: first run encrypts the plaintext row
        val firstPass = cipher.enc(plain)
        assertTrue(firstPass.startsWith("[enc]"))
        // Simulate migration running again on the same (now-encrypted) row
        val secondPass = cipher.enc(firstPass)
        assertEquals("Second migration pass must leave already-encrypted value unchanged", firstPass, secondPass)
        // Value is still correctly recoverable
        assertEquals(plain, cipher.dec(secondPass))
    }

    @Test
    fun decrypt_encryptedEntityFromLegacyPlaintext_roundTrips() {
        // Model a row read from DB before migration: all fields are plaintext.
        val legacyRow = BeneficiaryEntity(
            fullName = "يوسف عبدالله", relation = "ابن",
            phone = "01022222222", email = "",
            nationalId = "", notes = "",
        )
        // After migration, encrypt is called on the row.
        val migratedRow = cipher.encrypt(legacyRow)
        // The ViewModel maps the migrated row through decrypt before showing it.
        val displayed = cipher.decrypt(migratedRow)
        assertEquals("Displayed fullName must match original", legacyRow.fullName, displayed.fullName)
        assertFalse("Stored fullName must carry [enc] prefix", migratedRow.fullName.startsWith(legacyRow.fullName))
    }
}
