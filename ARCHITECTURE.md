# Architecture — Erthi Amanah

## Overview

Erthi Amanah follows **Clean Architecture** with **MVVM** presentation layer, implemented with modern Android libraries.

```
app/src/main/java/com/erthi/amanah/
├── ErthiAmanaApplication.kt       # Hilt application class
├── MainActivity.kt                # Single-activity entry point
├── data/
│   ├── db/                        # Room database, entities, DAOs
│   ├── security/                  # CryptoManager, AuthManager
│   ├── worker/                    # WorkManager background tasks
│   └── di/                        # Hilt modules
├── ui/
│   ├── theme/                     # Color, Typography, Shape, Theme
│   ├── navigation/                # NavGraph, Screen sealed class
│   ├── components/                # Reusable Compose components
│   ├── screens/                   # One package per screen
│   └── viewmodel/                 # Shared ViewModels
└── util/                          # Extensions and constants
```

## Data Flow

```
UI (Compose) → ViewModel → Repository → DAO → Room DB (encrypted)
                        ↓
                    CryptoManager (Android Keystore AES-256-GCM)
```

## Security Architecture

### Local Storage

- Room database in app-private directory (`/data/data/com.erthi.amanah/databases/`)
- Sensitive string fields encrypted at the application level before writing
- `CryptoManager` uses Android Keystore with AES-256-GCM
- No cloud uploads by default

### Authentication

- `AuthManager` persists a salted SHA-256 PIN hash in DataStore (encrypted preferences)
- Biometric authentication via `BiometricPrompt`
- PIN fallback
- App locks on background → `AuthState.Locked`

### Backup

- Backup archive encrypted with the vault key before upload
- User provides their own cloud storage (Google Drive, OneDrive, Dropbox, SAF)
- Erthi operates no custody servers
- Recovery key exported by user and stored separately

## Key Libraries

| Library | Purpose |
|---|---|
| Jetpack Compose | Declarative UI |
| Material 3 | Design system |
| Room | Local SQLite ORM |
| Hilt | Dependency injection |
| DataStore | Encrypted preferences |
| BiometricPrompt | Biometric auth |
| WorkManager | Background tasks |
| Navigation Compose | Screen navigation |
| Kotlin Coroutines + Flow | Async data streams |
