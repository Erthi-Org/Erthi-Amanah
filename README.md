# إرثي أمانة | Erthi Amanah

<div align="center">

**وصيتك · أماناتك · شؤونك في مكان آمن**

*Your Will · Your Trusts · Your Affairs in a Safe Place*

[![Android CI](https://github.com/Erthi-Org/Erthi-Amanah/actions/workflows/android-ci.yml/badge.svg)](https://github.com/Erthi-Org/Erthi-Amanah/actions/workflows/android-ci.yml)

</div>

---

Erthi Amanah is a **free, open-source, privacy-first Android application** that helps individuals document their wills, financial obligations, debts, entrusted property, emergency contacts, and instructions for their families — all stored **locally on-device** with **AES-256 encryption**.

## Features

| Module | Description |
|---|---|
| **Digital Will** | Write, record audio/video wills; attach documents; configure access |
| **Finance** | Bank account references, assets, real estate, gold, business interests |
| **Debts** | Money you owe and money owed to you, with status tracking |
| **Entrusted Property** | Items belonging to others with return instructions |
| **Digital Accounts** | Email, subscriptions, online accounts |
| **Important Documents** | Encrypted file attachments |
| **Family & Beneficiaries** | Trusted contacts with configurable access |
| **Emergency Center** | One-tap dialling (Egypt: 123, 122, 180, 112) + SOS |
| **Encrypted Backup** | Optional encrypted backup to Google Drive / OneDrive / Dropbox / local |
| **Security** | Biometric + PIN, AES-256-GCM, Android Keystore |

## Privacy Architecture

- ✅ All personal data stored **locally on-device** only
- ✅ No registration required
- ✅ No cloud servers operated by Erthi
- ✅ No analytics SDKs, no advertising
- ✅ Works fully offline
- ✅ Encrypted cloud backup is optional — user owns the cloud storage
- ✅ Open-source, auditable code

## Security

- AES-256-GCM authenticated encryption
- Android Keystore for key management (hardware-backed where available)
- Biometric authentication + PIN fallback
- Database stored in app-private storage
- Automatic app lock
- No sensitive data in logs

## Legal Notice

The digital will stored in this application is a personal record. Its legal enforceability depends on the laws of your country and formal witnessing requirements. Consult a lawyer to confirm legal validity.

## Requirements

- Android 8.0 (API 26) or later
- Target: Android 15 (API 35)

## Build

```bash
git clone https://github.com/Erthi-Org/Erthi-Amanah.git
cd Erthi-Amanah
./gradlew assembleDebug
```

The APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

A signed release APK requires a keystore configured in `keystore.properties` (see [RELEASE.md](RELEASE.md)).

## CI / CD

GitHub Actions builds the debug APK and runs unit tests on every push and pull request. The APK artifact is downloadable from the workflow run.

Workflow: `.github/workflows/android-ci.yml`

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).

## License

This project is free and open-source. See [LICENSE](LICENSE) for details.

## Organization

GitHub: [https://github.com/Erthi-Org](https://github.com/Erthi-Org)
