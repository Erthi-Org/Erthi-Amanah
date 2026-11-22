package com.erthi.amanah.data.backup

import android.accounts.Account
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.Scope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

// Drive API uses the appDataFolder — private to the app, not visible in
// the user's regular Drive folder list. Files persist across reinstalls
// as long as the same Google account is used.
private const val DRIVE_API = "https://www.googleapis.com/drive/v3"
private const val DRIVE_UPLOAD = "https://www.googleapis.com/upload/drive/v3"
private const val DRIVE_SCOPE = "https://www.googleapis.com/auth/drive.appdata"
private const val MIME_OCTET = "application/octet-stream"
private const val TIMEOUT_MS = 30_000

// Requires Google Cloud Console setup — see docs/GOOGLE_DRIVE_SETUP.md
// Without OAuth client ID registration, sign-in will succeed for the
// account picker but Drive scope authorization will fail at runtime.
// The app falls back gracefully to showing a "setup required" state.

@Singleton
class DriveBackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val scope = Scope(DRIVE_SCOPE)

    fun buildSignInOptions(): GoogleSignInOptions =
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(scope)
            .build()

    fun buildSignInIntent(): Intent =
        GoogleSignIn.getClient(context, buildSignInOptions()).signInIntent

    fun getSignedInAccount(): GoogleSignInAccount? =
        GoogleSignIn.getLastSignedInAccount(context)

    fun hasRequiredScopes(): Boolean {
        val account = getSignedInAccount() ?: return false
        return GoogleSignIn.hasPermissions(account, scope)
    }

    fun handleSignInResult(data: Intent?): GoogleSignInAccount? =
        try {
            GoogleSignIn.getSignedInAccountFromIntent(data).result
        } catch (_: Exception) { null }

    suspend fun signOut() = withContext(Dispatchers.IO) {
        try {
            GoogleSignIn.getClient(context, buildSignInOptions()).signOut()
        } catch (_: Exception) {}
    }

    // Fetches or refreshes the OAuth token for Drive.
    // Must be called from a background thread.
    private suspend fun token(account: GoogleSignInAccount): String =
        withContext(Dispatchers.IO) {
            GoogleAuthUtil.getToken(
                context,
                account.account ?: throw IllegalStateException("No Android account"),
                "oauth2:$DRIVE_SCOPE",
            )
        }

    // ── Upload ───────────────────────────────────────────────────────────────

    suspend fun uploadBackup(
        encryptedBytes: ByteArray,
        filename: String,
    ): String = withContext(Dispatchers.IO) {
        val account = getSignedInAccount()
            ?: throw IllegalStateException("لم يتم تسجيل الدخول إلى Google")
        val tok = token(account)

        val boundary = "erthi_backup_${System.currentTimeMillis()}"
        val metadataPart = buildString {
            append("--$boundary\r\n")
            append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
            append("""{"name":"$filename","parents":["appDataFolder"]}""")
            append("\r\n")
        }
        val dataPart = "--$boundary\r\nContent-Type: $MIME_OCTET\r\n\r\n"
        val closing = "\r\n--$boundary--\r\n"

        val body = metadataPart.toByteArray(Charsets.UTF_8) +
                dataPart.toByteArray(Charsets.UTF_8) +
                encryptedBytes +
                closing.toByteArray(Charsets.UTF_8)

        val conn = openConn("$DRIVE_UPLOAD/files?uploadType=multipart", "POST", tok)
        conn.setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")
        conn.setFixedLengthStreamingMode(body.size)
        conn.getOutputStream().use { it.write(body) }

        val response = conn.readResponse()
        if (conn.responseCode !in 200..299) {
            conn.disconnect()
            throw DriveApiException("فشل الرفع: ${conn.responseCode} — $response")
        }
        conn.disconnect()

        JSONObject(response).getString("id")
    }

    // Streaming upload — avoids loading the entire archive into memory.
    suspend fun uploadBackupFile(
        file: java.io.File,
        filename: String,
    ): String = withContext(Dispatchers.IO) {
        val account = getSignedInAccount()
            ?: throw IllegalStateException("لم يتم تسجيل الدخول إلى Google")
        val tok = token(account)

        val boundary = "erthi_backup_${System.currentTimeMillis()}"
        val metadataBytes = buildString {
            append("--$boundary\r\n")
            append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
            append("""{"name":"$filename","parents":["appDataFolder"]}""")
            append("\r\n")
        }.toByteArray(Charsets.UTF_8)
        val dataPartBytes = "--$boundary\r\nContent-Type: $MIME_OCTET\r\n\r\n".toByteArray(Charsets.UTF_8)
        val closingBytes = "\r\n--$boundary--\r\n".toByteArray(Charsets.UTF_8)
        val totalLength = metadataBytes.size.toLong() + dataPartBytes.size.toLong() +
                file.length() + closingBytes.size.toLong()

        val conn = openConn("$DRIVE_UPLOAD/files?uploadType=multipart", "POST", tok)
        conn.setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")
        conn.setFixedLengthStreamingMode(totalLength)
        conn.getOutputStream().use { out ->
            out.write(metadataBytes)
            out.write(dataPartBytes)
            file.inputStream().use { it.copyTo(out) }
            out.write(closingBytes)
        }

        val response = conn.readResponse()
        if (conn.responseCode !in 200..299) {
            conn.disconnect()
            throw DriveApiException("فشل الرفع: ${conn.responseCode} — $response")
        }
        conn.disconnect()

        JSONObject(response).getString("id")
    }

    // ── List backups ─────────────────────────────────────────────────────────

    suspend fun listBackups(): List<DriveBackupFile> = withContext(Dispatchers.IO) {
        val account = getSignedInAccount()
            ?: throw IllegalStateException("لم يتم تسجيل الدخول إلى Google")
        val tok = token(account)

        val url = "$DRIVE_API/files?" +
                "spaces=appDataFolder&" +
                "orderBy=modifiedTime+desc&" +
                "pageSize=20&" +
                "fields=files(id,name,size,createdTime,modifiedTime)"
        val conn = openConn(url, "GET", tok)
        val response = conn.readResponse()
        if (conn.responseCode !in 200..299) {
            conn.disconnect()
            throw DriveApiException("فشل جلب النسخ: ${conn.responseCode}")
        }
        conn.disconnect()

        val filesArray = JSONObject(response).optJSONArray("files") ?: return@withContext emptyList()
        val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        List(filesArray.length()) { i ->
            val f = filesArray.getJSONObject(i)
            DriveBackupFile(
                id = f.getString("id"),
                name = f.optString("name", "backup"),
                sizeBytes = f.optLong("size", 0L),
                createdTimeMs = fmt.parse(f.optString("createdTime", "1970-01-01T00:00:00.000Z"))?.time ?: 0L,
                modifiedTimeMs = fmt.parse(f.optString("modifiedTime", "1970-01-01T00:00:00.000Z"))?.time ?: 0L,
            )
        }
    }

    // ── Download ─────────────────────────────────────────────────────────────

    suspend fun downloadBackup(fileId: String): ByteArray = withContext(Dispatchers.IO) {
        val account = getSignedInAccount()
            ?: throw IllegalStateException("لم يتم تسجيل الدخول إلى Google")
        val tok = token(account)

        val conn = openConn("$DRIVE_API/files/$fileId?alt=media", "GET", tok)
        if (conn.responseCode !in 200..299) {
            conn.disconnect()
            throw DriveApiException("فشل التنزيل: ${conn.responseCode}")
        }
        val bytes = conn.inputStream.readBytes()
        conn.disconnect()
        bytes
    }

    // Streaming download — writes to a temp file so the entire archive is never in memory.
    // Throws if more than maxBytes are received before the connection closes.
    suspend fun downloadBackupToFile(
        fileId: String,
        destination: java.io.File,
        maxBytes: Long,
    ) = withContext(Dispatchers.IO) {
        val account = getSignedInAccount()
            ?: throw IllegalStateException("لم يتم تسجيل الدخول إلى Google")
        val tok = token(account)

        val conn = openConn("$DRIVE_API/files/$fileId?alt=media", "GET", tok)
        if (conn.responseCode !in 200..299) {
            conn.disconnect()
            throw DriveApiException("فشل التنزيل: ${conn.responseCode}")
        }
        try {
            var received = 0L
            val buffer = ByteArray(64 * 1024)
            conn.inputStream.use { input ->
                destination.outputStream().use { output ->
                    while (true) {
                        val count = input.read(buffer)
                        if (count == -1) break
                        received += count
                        if (received > maxBytes) throw IllegalStateException(
                            "حجم ملف النسخة يتجاوز الحد المسموح به"
                        )
                        output.write(buffer, 0, count)
                    }
                }
            }
        } finally {
            conn.disconnect()
        }
    }

    // ── Delete ───────────────────────────────────────────────────────────────

    suspend fun deleteBackup(fileId: String) = withContext(Dispatchers.IO) {
        val account = getSignedInAccount()
            ?: throw IllegalStateException("لم يتم تسجيل الدخول إلى Google")
        val tok = token(account)

        val conn = openConn("$DRIVE_API/files/$fileId", "DELETE", tok)
        val code = conn.responseCode
        conn.disconnect()
        if (code !in 200..299 && code != 204) {
            throw DriveApiException("فشل الحذف: $code")
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun openConn(url: String, method: String, token: String): HttpURLConnection {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.requestMethod = method
        conn.setRequestProperty("Authorization", "Bearer $token")
        conn.setRequestProperty("Accept", "application/json")
        conn.connectTimeout = TIMEOUT_MS
        conn.readTimeout = TIMEOUT_MS
        if (method in listOf("POST", "PUT", "PATCH")) {
            conn.doOutput = true
        }
        return conn
    }

    private fun HttpURLConnection.readResponse(): String {
        val stream = try { inputStream } catch (_: Exception) { errorStream ?: return "" }
        return BufferedReader(InputStreamReader(stream, Charsets.UTF_8))
            .use { it.readText() }
    }
}

class DriveApiException(message: String) : Exception(message)
