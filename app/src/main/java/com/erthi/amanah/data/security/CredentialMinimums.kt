package com.erthi.amanah.data.security

internal object CredentialMinimums {
    const val PIN_LENGTH = 6
    const val BACKUP_PASSPHRASE_LENGTH = 12

    fun validNewPin(pin: String): Boolean =
        pin.length in PIN_LENGTH..8 && pin.all(Char::isDigit)

    fun validNewBackupPassphrase(passphrase: String): Boolean =
        passphrase.length >= BACKUP_PASSPHRASE_LENGTH && passphrase.isNotBlank()
}
