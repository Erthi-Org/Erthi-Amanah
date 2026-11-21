# Google Drive Backup — Setup Guide

The Google Drive backup feature uses the **appDataFolder** scope, which keeps all backup
files private to this app and invisible in the user's regular Drive folder.

## Prerequisites

- A Google account with an active Google Play Developer account
- Access to the [Google Cloud Console](https://console.cloud.google.com/)
- The debug keystore SHA-1 fingerprint (see below)

---

## Step 1 — Get the keystore SHA-1

> **SHA-1 vs SHA-256**: Google Cloud Console OAuth client IDs for Android require the
> **SHA-1** certificate fingerprint, NOT SHA-256. These are two different hashes computed
> from the same certificate data — do not use the SHA-256 as a substitute.

The stable debug keystore (committed as `DEBUG_KEYSTORE_BASE64`) has these fingerprints:

| Algorithm | Fingerprint |
|-----------|-------------|
| **SHA-1** (required for OAuth client) | `A2:F9:4E:1B:E7:96:09:FF:33:78:70:04:27:39:3B:00:8E:10:26:66` |
| SHA-256 (used by CI verification) | `36:00:E8:FF:78:AA:D2:0C:39:A9:1F:50:A6:19:CF:4B:5B:1D:77:26:5D:15:32:8E:C1:C3:17:BE:91:EB:AB:EA` |

To re-derive these from the secret (e.g. after key rotation):

```bash
# Decode the base64 keystore (Linux/macOS)
echo "$DEBUG_KEYSTORE_BASE64" | base64 -d > /tmp/debug.keystore

# Print both SHA-1 and SHA-256 fingerprints
keytool -list -v \
  -keystore /tmp/debug.keystore \
  -alias "$DEBUG_KEY_ALIAS" \
  -storepass "$DEBUG_STORE_PASSWORD" \
  | grep -A3 "Certificate fingerprints"
```

---

## Step 2 — Create a Google Cloud project

1. Go to <https://console.cloud.google.com/> and create a new project (e.g. `erthi-amanah`).
2. Enable the **Google Drive API** under *APIs & Services → Library*.

---

## Step 3 — Configure the OAuth consent screen

1. Go to *APIs & Services → OAuth consent screen*.
2. Choose **External** (or Internal if you have a Workspace org).
3. Fill in app name (`Erthi Amanah`), support email, and developer contact.
4. Under *Scopes*, add:
   - `https://www.googleapis.com/auth/drive.appdata`
5. Add test users (your own Google account) while in *Testing* status.
6. Save and continue.

---

## Step 4 — Create an Android OAuth client ID

1. Go to *APIs & Services → Credentials → Create Credentials → OAuth client ID*.
2. Application type: **Android**.
3. Package name: `com.erthi.amanah` (debug variant: `com.erthi.amanah.debug`).
4. SHA-1 certificate fingerprint: paste the SHA-1 from Step 1.
5. Click **Create**. No `google-services.json` download is needed — the client ID
   is embedded in the GMS framework on the device.

> **Important**: The package name must match exactly what is used at runtime.
> Debug builds use the `applicationIdSuffix ".debug"` variant, so they need a
> *separate* client ID with package name `com.erthi.amanah.debug`.

---

## Step 5 — Verify the setup

1. Install a debug build on a physical Android device (emulators work for the sign-in
   flow but Drive uploads require real network access).
2. Open *Settings → النسخ الاحتياطي (Backup)*.
3. Tap **تسجيل الدخول بـ Google** — the Google account picker should appear.
4. Select an account that is listed as a test user in the consent screen.
5. Grant the Drive permission when prompted.
6. After returning to the screen you should see the account email and the
   *رفع نسخة احتياطية* button enabled.

---

## Troubleshooting

| Symptom | Likely cause |
|---|---|
| Sign-in succeeds but scope is not granted | Package name or SHA-1 mismatch in Cloud Console |
| `10:` error code from Google Sign-In | OAuth client ID not created / wrong package name |
| `DriveApiException: فشل الرفع: 403` | Drive API not enabled, or consent screen in testing with unverified user |
| Upload succeeds but list is empty | `spaces=appDataFolder` requires a file to exist first; list again after first backup |

---

## Production release

Before publishing to Google Play:

1. Create a **release** OAuth client ID using the release keystore SHA-1.
2. Submit the OAuth consent screen for Google verification (required for apps
   with more than 100 test users or for public production).
3. The consent screen review typically takes 3–5 business days.
