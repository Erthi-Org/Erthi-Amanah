# Release Guide — Erthi Amanah

## Debug Build

```bash
./gradlew assembleDebug
```
Output: `app/build/outputs/apk/debug/app-debug.apk`

## Release Build (Signed)

1. Create a keystore:
```bash
keytool -genkey -v -keystore erthi-release.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias erthi-key
```

2. Create `keystore.properties` (never commit this file):
```properties
storeFile=path/to/erthi-release.jks
storePassword=YOUR_STORE_PASSWORD
keyAlias=erthi-key
keyPassword=YOUR_KEY_PASSWORD
```

3. Update `app/build.gradle.kts` to read `keystore.properties` in the `signingConfigs` block.

4. Build:
```bash
./gradlew assembleRelease
```

## GitHub Actions APK Artifact

Every push to `feature/**` and `main` automatically builds the debug APK and uploads it as a workflow artifact named `erthi-amanah-debug`.

Download from: **Actions → android-ci.yml → latest run → Artifacts**

## F-Droid Distribution

For F-Droid submission:
- Application must build reproducibly without non-free SDKs
- Remove any Google services references if present
- Submit to F-Droid Data repository with appropriate metadata

## Versioning

Format: `MAJOR.MINOR.PATCH` (Semantic Versioning)

Update `versionCode` and `versionName` in `app/build.gradle.kts` before each release.
