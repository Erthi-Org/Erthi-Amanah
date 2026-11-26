package com.erthi.amanah.data.security

import android.util.Base64
import com.erthi.amanah.data.db.entity.AssetEntity
import com.erthi.amanah.data.db.entity.BeneficiaryEntity
import com.erthi.amanah.data.db.entity.DebtEntity
import com.erthi.amanah.data.db.entity.DigitalAccountEntity
import com.erthi.amanah.data.db.entity.EntrustedEntity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Shared field-level cipher for the five tables whose string columns were previously
 * stored as plaintext.  Uses the same [enc] prefix convention as VaultViewModel /
 * BackupViewModel so that legacy plaintext rows decode gracefully (dec() returns them
 * unchanged), and already-encrypted values are not double-encrypted (enc() is idempotent).
 */
@Singleton
class FieldCipher @Inject constructor(private val crypto: CryptoManager) {

    internal val ENC = "[enc]"

    fun enc(value: String): String {
        if (value.startsWith(ENC)) return value
        val encrypted = crypto.encrypt(value.toByteArray(Charsets.UTF_8))
        return ENC + Base64.encodeToString(encrypted.toByteArray(), Base64.NO_WRAP)
    }

    fun dec(value: String): String {
        if (!value.startsWith(ENC)) return value
        return try {
            val bytes = Base64.decode(value.removePrefix(ENC), Base64.NO_WRAP)
            String(crypto.decrypt(CryptoManager.EncryptedData.fromByteArray(bytes)), Charsets.UTF_8)
        } catch (_: Exception) { value }
    }

    // ── Per-entity overloads ───────────────────────────────────────────────

    fun encrypt(e: BeneficiaryEntity) = e.copy(
        fullName = enc(e.fullName),
        phone    = enc(e.phone),
        email    = enc(e.email),
        nationalId = enc(e.nationalId),
        notes    = enc(e.notes),
    )

    fun decrypt(e: BeneficiaryEntity) = e.copy(
        fullName   = dec(e.fullName),
        phone      = dec(e.phone),
        email      = dec(e.email),
        nationalId = dec(e.nationalId),
        notes      = dec(e.notes),
    )

    fun encrypt(e: DebtEntity) = e.copy(
        personName  = enc(e.personName),
        description = enc(e.description),
        evidence    = enc(e.evidence),
    )

    fun decrypt(e: DebtEntity) = e.copy(
        personName  = dec(e.personName),
        description = dec(e.description),
        evidence    = dec(e.evidence),
    )

    fun encrypt(e: AssetEntity) = e.copy(
        name             = enc(e.name),
        description      = enc(e.description),
        location         = enc(e.location),
        accountReference = enc(e.accountReference),
        notes            = enc(e.notes),
    )

    fun decrypt(e: AssetEntity) = e.copy(
        name             = dec(e.name),
        description      = dec(e.description),
        location         = dec(e.location),
        accountReference = dec(e.accountReference),
        notes            = dec(e.notes),
    )

    fun encrypt(e: EntrustedEntity) = e.copy(
        ownerName           = enc(e.ownerName),
        ownerPhone          = enc(e.ownerPhone),
        itemDescription     = enc(e.itemDescription),
        returnInstructions  = enc(e.returnInstructions),
        evidence            = enc(e.evidence),
    )

    fun decrypt(e: EntrustedEntity) = e.copy(
        ownerName          = dec(e.ownerName),
        ownerPhone         = dec(e.ownerPhone),
        itemDescription    = dec(e.itemDescription),
        returnInstructions = dec(e.returnInstructions),
        evidence           = dec(e.evidence),
    )

    fun encrypt(e: DigitalAccountEntity) = e.copy(
        platform          = enc(e.platform),
        accountIdentifier = enc(e.accountIdentifier),
        instructions      = enc(e.instructions),
        notes             = enc(e.notes),
    )

    fun decrypt(e: DigitalAccountEntity) = e.copy(
        platform          = dec(e.platform),
        accountIdentifier = dec(e.accountIdentifier),
        instructions      = dec(e.instructions),
        notes             = dec(e.notes),
    )
}
