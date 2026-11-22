package com.erthi.amanah

import com.erthi.amanah.data.backup.RestoreFiles
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException

class RestoreFilesTest {
    @get:Rule val folder = TemporaryFolder()

    @Test
    fun replacesSameSizeWrongDestinationAndCanReplayAfterRename() {
        val staged = folder.newFile().apply { writeText("good") }
        val destination = folder.newFile().apply { writeText("evil") }
        val entry = RestoreFiles.Entry(staged, destination, 4, RestoreFiles.sha256(staged))
        val persisted = RestoreFiles.entries(JSONArray().put(entry.toJson()).toString()).single()

        RestoreFiles.publish(persisted)
        assertEquals("good", destination.readText())
        assertTrue(staged.isFile)
        assertTrue(staged.delete())
        RestoreFiles.publish(persisted)
        destination.writeText("evil")
        assertThrows(IOException::class.java) { RestoreFiles.publish(persisted) }
        assertEquals("evil", destination.readText())
    }

    @Test
    fun corruptedStagingDoesNotOverwriteExistingFile() {
        val staged = folder.newFile().apply { writeText("good") }
        val destination = folder.newFile().apply { writeText("safe") }
        val entry = RestoreFiles.Entry(staged, destination, 4, RestoreFiles.sha256(staged))
        staged.writeText("evil")
        assertThrows(IOException::class.java) { RestoreFiles.publish(entry) }
        assertEquals("safe", destination.readText())
        assertTrue(staged.exists())
    }

    @Test
    fun legacyJournalRequiresStagingToVerifyAnExistingDestination() {
        val staged = folder.newFile().apply { writeText("good") }
        val destination = folder.newFile()
        val entry = RestoreFiles.Entry(staged, destination, 0, null)
        RestoreFiles.publish(entry)
        assertEquals("good", destination.readText())
        assertTrue(staged.delete())
        assertThrows(IOException::class.java) { RestoreFiles.publish(entry) }
    }
}
