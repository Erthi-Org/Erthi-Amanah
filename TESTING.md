# Testing — Erthi Amanah

## Running Tests

```bash
# Unit tests only
./gradlew testDebugUnitTest

# Generate test reports
./gradlew testDebugUnitTest --continue
# Reports: app/build/reports/tests/testDebugUnitTest/index.html
```

## Test Coverage

### Unit Tests (automated, run in CI)

| Test file | What it covers |
|---|---|
| `CryptoManagerTest` | EncryptedData serialization, PIN validation rules |
| `DebtEntityTest` | Entity defaults, direction enum, edge cases |
| `NavigationScreenTest` | Route uniqueness, specific route values |

### Integration Tests (require connected device or emulator)

```bash
./gradlew connectedAndroidTest
```

| Area | Status |
|---|---|
| Room database CRUD | Requires emulator |
| Biometric authentication | Requires physical device |
| Cloud backup (Google Drive) | Requires device + Google account |
| Emergency dialler intent | Requires emulator with phone capability |
| File provider export | Requires emulator |

### Physical Device Tests (not automated)

The following features require manual testing on a real Android device:

- Biometric unlock (fingerprint / face)
- Emergency call initiation
- Location sharing in emergency SOS
- Cloud backup to Google Drive / OneDrive / Dropbox
- Storage Access Framework file export
- WorkManager background periodic backup
- Boot receiver re-registration

### RTL Layout Verification

Test RTL layout on an Arabic-locale emulator:
```
Settings → System → Language → Add Arabic → Set as default
```

Verify all screens display correctly in RTL direction.

## CI Verification

GitHub Actions runs the following on every push and PR:
1. `./gradlew testDebugUnitTest` — unit tests
2. `./gradlew assembleDebug` — debug APK build
3. `./gradlew lintDebug` — static analysis (non-blocking)

A passing CI run confirms the project compiles and unit tests pass. It does not confirm UI correctness or device-specific behaviour.
