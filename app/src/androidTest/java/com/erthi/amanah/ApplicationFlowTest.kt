package com.erthi.amanah

import android.graphics.Bitmap
import android.os.SystemClock
import android.view.KeyEvent
import android.view.WindowManager
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.os.LocaleListCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ApplicationFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun pinDashboardDocumentsSettingsBackupLocaleAndBackground() {
        setLanguage("ar")
        capture("01-onboarding-ar")
        clickText(R.string.btn_start)
        waitForTag("pin-entry")
        capture("02-create-pin-ar")
        compose.onNodeWithTag("pin-entry").performScrollTo().performTextInput("672495")
        compose.onNodeWithTag("pin-confirm").performScrollTo().performTextInput("672495")
        compose.onNodeWithTag("pin-acknowledge").performScrollTo().performClick()
        compose.onNodeWithTag("pin-save").performScrollTo().performClick()

        waitForText(R.string.nav_home)
        capture("03-dashboard-ar")
        clickText(R.string.nav_documents)
        waitForText(R.string.documents_empty)
        capture("04-documents-ar")
        compose.onNodeWithContentDescription(text(R.string.documents_add_record)).performClick()
        capture("05-add-document-ar")
        clickText(R.string.documents_attach)
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val deadline = SystemClock.elapsedRealtime() + 10_000
        while (automation.rootInActiveWindow?.packageName?.toString()?.contains("documentsui") != true
            && SystemClock.elapsedRealtime() < deadline) {
            SystemClock.sleep(100)
        }
        check(automation.rootInActiveWindow?.packageName?.toString()?.contains("documentsui") == true)
        shell("input keyevent ${KeyEvent.KEYCODE_BACK}")
        waitForText(R.string.documents_empty)
        clickBack()

        clickText(R.string.nav_backup)
        capture("06-backup-ar")
        clickBack()
        compose.onNodeWithContentDescription(text(R.string.nav_more)).performClick()
        clickText(R.string.settings_title)
        capture("07-settings-ar")
        clickText(R.string.settings_language)
        waitForText(R.string.language_picker_title)
        capture("08-language-picker-ar")

        setLanguage("fa")
        waitForText(R.string.language_picker_title)
        compose.activityRule.scenario.onActivity {
            assertEquals(android.view.View.LAYOUT_DIRECTION_RTL, it.resources.configuration.layoutDirection)
        }
        capture("09-language-picker-fa")
        setLanguage("en")
        waitForText(R.string.language_picker_title)
        compose.activityRule.scenario.onActivity {
            assertEquals(android.view.View.LAYOUT_DIRECTION_LTR, it.resources.configuration.layoutDirection)
        }
        capture("10-language-picker-en")

        shell("input keyevent ${KeyEvent.KEYCODE_HOME}")
        SystemClock.sleep(400)
        val packageName = InstrumentationRegistry.getInstrumentation().targetContext.packageName
        shell("am start -n $packageName/com.erthi.amanah.MainActivity")
        waitForTag("unlock-entry")
        capture("11-unlock-en")
    }

    private fun setLanguage(tag: String) {
        compose.activityRule.scenario.onActivity {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
        }
        compose.waitUntil(12_000) {
            compose.activity.resources.configuration.locales[0].language == tag
        }
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        compose.activityRule.scenario.onActivity {
            it.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        try {
            SystemClock.sleep(550)
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val screenshots = File(instrumentation.targetContext.getExternalFilesDir(null), "screenshots")
            screenshots.mkdirs()
            val screenshot = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
            File(screenshots, "$name.png").outputStream().use { screenshot.compress(Bitmap.CompressFormat.PNG, 100, it) }
        } finally {
            compose.activityRule.scenario.onActivity {
                it.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }

    private fun text(id: Int): String = compose.activity.getString(id)

    private fun clickBack() = compose.onNodeWithContentDescription(text(R.string.btn_back)).performClick()

    private fun clickText(id: Int) {
        waitForText(id)
        compose.onAllNodesWithText(text(id)).filter(hasClickAction()).onFirst().performClick()
    }

    private fun waitForText(id: Int) {
        val value = text(id)
        compose.waitUntil(12_000) { compose.onAllNodesWithText(value).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun waitForTag(tag: String) {
        compose.waitUntil(12_000) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun shell(command: String) {
        android.os.ParcelFileDescriptor.AutoCloseInputStream(
            InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
        ).use { it.readBytes() }
    }
}
