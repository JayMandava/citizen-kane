package dev.jeyanth.citizenkane

import android.content.Intent
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.inspector.WindowInspector
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import androidx.test.filters.SdkSuppress
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Synthetic app-only physical-device checks; no device permissions or personal data. */
@SdkSuppress(minSdkVersion = 29)
class PhysicalParityTests {
    @get:Rule val compose = createEmptyComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun capture(name: String, anchor: String, scenario: ActivityScenario<MainActivity>) {
        compose.waitForIdle()
        compose.onNodeWithText(anchor).assertExists()
        // Inspect only this process's app windows. Compose root capture can use
        // the activity Window even when the selected node belongs to a Dialog.
        fun dialogWindow(view: View): Window? {
            (view as? DialogWindowProvider)?.let { return it.window }
            if (view is ViewGroup) for (index in 0 until view.childCount) {
                dialogWindow(view.getChildAt(index))?.let { return it }
            }
            return null
        }
        lateinit var bitmap: Bitmap
        val copied = CountDownLatch(1)
        var result = -1
        scenario.onActivity { activity ->
            val focused = WindowInspector.getGlobalWindowViews().lastOrNull { it.hasWindowFocus() }
            val dialog = focused?.let(::dialogWindow)
            if (anchor in listOf("Settings", "Aries", "Read Full Article")) assertNotNull("Expected an app dialog window", dialog)
            val window = dialog ?: activity.window
            bitmap = Bitmap.createBitmap(window.decorView.width, window.decorView.height, Bitmap.Config.ARGB_8888)
            PixelCopy.request(window, bitmap, { code -> result = code; copied.countDown() }, Handler(Looper.getMainLooper()))
        }
        assertTrue("App-window capture timed out", copied.await(5, TimeUnit.SECONDS))
        assertEquals(PixelCopy.SUCCESS, result)
        File(context.getExternalFilesDir("physical-parity"), "$name.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test fun coreFlowsAppearanceRashiAndLifecycle() {
        ActivityScenario.launch<MainActivity>(Intent(context, MainActivity::class.java).putExtra("fixture", "news")).use { scenario ->
            compose.waitUntil(15_000) { compose.onAllNodesWithText("Eenadu").fetchSemanticsNodes().isNotEmpty() }
            capture("oneplus-news", "సిటిజన్ కేన్", scenario)
            compose.onNodeWithContentDescription("Settings").performClick()
            providers.forEach { compose.onNodeWithContentDescription(it.second).assertExists() }
            compose.onNodeWithText("Dark").performClick()
            capture("oneplus-settings-dark", "Settings", scenario)
            compose.onNodeWithText("Light").performClick()
            compose.onNodeWithText("Your Sign").performScrollTo().performClick()
            capture("oneplus-sign-picker", "Aries", scenario)
            compose.onNodeWithText("Aries").performClick()
            compose.onNodeWithText("Done").performClick()
            compose.onNodeWithText("Rashi").performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Aries").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("TODAY").assertIsDisplayed()
            compose.onNodeWithText("THIS WEEK").assertIsDisplayed()
            capture("oneplus-rashi", "Rashi Phalalu", scenario)
            scenario.moveToState(Lifecycle.State.CREATED)
            scenario.moveToState(Lifecycle.State.RESUMED)
            compose.onNodeWithText("Aries").assertIsDisplayed()
            scenario.recreate()
            compose.onNodeWithText("Aries").assertIsDisplayed()
            compose.onNodeWithText("News").performClick()
            compose.onAllNodesWithText("తెలుగు వార్తలు: కొత్త అవకాశాలతో ముందుకు సాగుతున్న ప్రజలు").onFirst().performClick()
            compose.onNodeWithText("Read Full Article").performScrollTo().assertIsDisplayed()
            capture("oneplus-article", "Read Full Article", scenario)
            compose.onNodeWithText("Read Full Article").performClick()
            compose.onNodeWithText("Synthetic article preview. External navigation is disabled for fixture tests.").assertIsDisplayed()
            compose.onNodeWithText("OK").performClick()
        }
    }
}
