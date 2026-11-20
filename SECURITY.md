# Security — Erthi Amanah

## Threat Model

Erthi Amanah is designed to protect sensitive personal information (wills, financial data, beneficiary contacts) from:

1. **Unauthorized access** when the device is lost or stolen
2. **Accidental data exposure** through screenshots, logs, or sharing
3. **Insecure cloud uploads** that could expose plaintext data
4. **Malicious apps** reading the database

It does NOT protect against:
- A compromised Android OS (rooted device with active attacker)
- Exploitation of Android platform vulnerabilities
- Physical access with sophisticated forensics against a fully patched device

## Encryption

### Vault Key

- Generated in **Android Keystore** (hardware-backed where available)
- Algorithm: **AES-256-GCM** (authenticated encryption)
- Key alias: `erthi_vault_key`
- `setUserAuthenticationRequired(false)` — key accessible after device unlock; biometric gates the app UI layer

### Database

- Room database stored in app-private storage (`/data/data/com.erthi.amanah/databases/`)
- Sensitive string fields encrypted at application layer with `CryptoManager` before writing to Room

### Preferences

- PIN hash stored in `EncryptedSharedPreferences` (Jetpack Security Crypto)
- PIN hashed with SHA-256 + 32-byte random salt
- Plaintext PIN never persisted

### Backup Files

- Backup archives are encrypted before upload
- Encryption key derived from the vault key
- Recovery key generated from backup and exported to user; not stored by Erthi
- Encrypted backup file is useless without the recovery key

## Authentication

1. User sets a minimum 4-digit PIN on first launch
2. Biometric (fingerprint, face) supported as a convenience unlock
3. App locks on background, process termination, and restart
4. PIN brute-force is a risk; a longer alphanumeric PIN is recommended

## Permissions

The application requests only permissions necessary for declared features:

| Permission | Purpose |
|---|---|
| `CALL_PHONE` | Emergency dialling |
| `RECORD_AUDIO` | Voice will recording |
| `CAMERA` | Video will recording |
| `USE_BIOMETRIC` | Biometric unlock |
| `ACCESS_FINE_LOCATION` | Optional emergency SOS location |
| `INTERNET` | Cloud backup (optional) |

## Known Limitations

- PIN hashed with SHA-256, not a memory-hard KDF (Argon2/bcrypt). A longer PIN reduces brute-force risk.
- Database-level encryption (SQLCipher) is not implemented in the initial release. App-layer field encryption mitigates partial exposure.
- Backup encryption key management requires the user to securely store the recovery key. Loss of the recovery key means loss of backup data.
- Emergency features do not bypass Android platform restrictions on background SMS.

## Reporting Vulnerabilities

Please report security issues via GitHub Issues with the label `security` or by contacting the organization through the GitHub page at https://github.com/Erthi-Org.
