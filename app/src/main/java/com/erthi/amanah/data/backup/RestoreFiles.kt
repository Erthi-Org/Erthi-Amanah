package com.erthi.amanah.data.backup

import kotlinx.coroutines.sync.Mutex
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.security.MessageDigest

internal object RestoreFiles {
    val mutex = Mutex()

    data class Entry(val staged: File, val destination: File, val size: Long, val sha256: String?) {
        fun toJson() = JSONObject().apply {
            put("staged", staged.absolutePath)
            put("final", destination.absolutePath)
            put("expectedSize", size)
            put("sha256", sha256)
        }
    }

    fun entries(json: String): List<Entry> {
        val array = JSONArray(json)
        return List(array.length()) { index ->
            val value = array.getJSONObject(index)
            Entry(
                File(value.getString("staged")),
                File(value.getString("final")),
                value.optLong("expectedSize", -1),
                value.optString("sha256").takeIf { it.matches(Regex("[0-9a-f]{64}")) },
            )
        }
    }

    fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count == -1) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }

    /** Staging stays intact until every entry has been verified and the journal is removed. */
    fun publish(entry: Entry) {
        val stagedExists = entry.staged.isFile
        val hash = entry.sha256 ?: if (stagedExists) sha256(entry.staged)
            else throw IOException("Legacy restore entry has no staged copy or integrity hash")
        val size = if (entry.sha256 == null && entry.size <= 0 && stagedExists) {
            entry.staged.length()
        } else {
            entry.size
        }
        fun matches(file: File) = file.isFile && file.length() == size && sha256(file) == hash

        if (matches(entry.destination)) return
        if (!matches(entry.staged)) throw IOException("Invalid staged restore file")

        val directory = requireNotNull(entry.destination.parentFile)
        if (!directory.isDirectory && !directory.mkdirs() && !directory.isDirectory) {
            throw IOException("Cannot create restore destination")
        }
        val temporary = File.createTempFile(".restore-", ".tmp", directory)
        try {
            entry.staged.inputStream().use { input ->
                temporary.outputStream().use { output ->
                    input.copyTo(output)
                    output.fd.sync()
                }
            }
            if (!matches(temporary)) throw IOException("Restore copy failed integrity verification")
            Files.move(temporary.toPath(), entry.destination.toPath(), ATOMIC_MOVE, REPLACE_EXISTING)
            if (!matches(entry.destination)) throw IOException("Restore destination failed integrity verification")
        } finally {
            temporary.delete()
        }
    }
}
