package dev.jeyanth.citizenkane

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File

class DeviceParityTests {
    @get:Rule val compose = createEmptyComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before fun resetSyntheticFixturesOnly() {
        context.getSharedPreferences("fixture-settings", Context.MODE_PRIVATE).edit().clear().commit()
        File(context.filesDir, "fixture-articles.json").delete()
    }
    @After fun finish() { scenario?.close() }
    private fun launch(mode: String = "news") {
        scenario = ActivityScenario.launch(Intent(context, MainActivity::class.java).putExtra("fixture", mode))
        compose.waitForIdle()
    }
    private fun waitNews() {
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Eenadu").fetchSemanticsNodes().isNotEmpty() }
    }
    private fun capture(name: String) {
        compose.waitForIdle()
        // Capture all app windows, including sheets/dialogs; a Compose root capture
        // can select the underlying activity instead of the modal window.
        val screen = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()!!
        var top = 0; var bottom = 0
        scenario!!.onActivity { activity ->
            val bars = androidx.core.view.ViewCompat.getRootWindowInsets(activity.window.decorView)
                ?.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            top = bars?.top ?: 0; bottom = bars?.bottom ?: 0
        }
        val image = Bitmap.createBitmap(screen, 0, top, screen.width, screen.height - top - bottom)
        val file = File(context.getExternalFilesDir("parity"), "$name.png")
        file.outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
    private fun settings() { compose.onNodeWithContentDescription("Settings").performClick() }

    @Test fun newsArticleAndBrowserFlow() {
        launch(); waitNews(); capture("news-light")
        compose.onAllNodesWithText("తెలుగు వార్తలు: కొత్త అవకాశాలతో ముందుకు సాగుతున్న ప్రజలు").onFirst().performClick()
        compose.onNodeWithText("Read Full Article").assertIsDisplayed()
        compose.onNodeWithText("ఇది పరీక్ష కోసం రూపొందించిన నమూనా వార్త.").assertIsDisplayed()
        capture("article-detail")
        compose.onNodeWithText("Read Full Article").performClick()
        compose.onNodeWithText("Synthetic article preview. External navigation is disabled for fixture tests.").assertIsDisplayed()
        compose.onNodeWithText("OK").performClick()
    }
    @Test fun allSourcesDisabledShowsEmptyState() {
        launch(); waitNews(); settings()
        providers.forEach { compose.onNodeWithContentDescription(it.second).performClick() }
        compose.onNodeWithText("Done").performClick()
        compose.onNodeWithText("No sources enabled").assertIsDisplayed()
        capture("news-empty")
        assertTrue(SettingsStore(context, "fixture-settings").load().enabledProviders.isEmpty())
    }
    @Test fun appearancePersistsThroughActivityRecreation() {
        launch(); waitNews(); settings()
        compose.onNodeWithText("Dark").performClick()
        capture("settings-dark")
        compose.onNodeWithText("Done").performClick()
        scenario!!.recreate(); compose.waitForIdle()
        assertEquals(Appearance.Dark, SettingsStore(context, "fixture-settings").load().appearance)
        capture("news-dark")
    }
    @Test fun signSelectionAndRashiContent() {
        launch(); waitNews(); settings()
        compose.onNodeWithText("Your Sign").performScrollTo().performClick()
        capture("sign-picker")
        compose.onNodeWithText("Aries").performClick()
        compose.onNodeWithText("Done").performClick()
        compose.onNodeWithText("Rashi").performClick()
        compose.onNodeWithText("Aries").assertIsDisplayed()
        compose.onNodeWithText("TODAY").assertIsDisplayed()
        compose.onNodeWithText("THIS WEEK").assertIsDisplayed()
        capture("rashi")
        assertEquals(RashiSign.mesh, SettingsStore(context, "fixture-settings").load().sign)
    }
    @Test fun offlineRestartRetainsLastSuccessfulFeed() {
        launch(); waitNews(); scenario!!.close()
        launch("offline"); waitNews()
        compose.onNodeWithText("Couldn't load news").assertDoesNotExist()
        capture("news-offline-cache")
    }
    @Test fun offlineWithoutCacheShowsErrorAndRetry() {
        launch("error")
        compose.onNodeWithText("Couldn't load news").assertIsDisplayed()
        compose.onNodeWithText("Try Again").assertIsDisplayed()
        capture("news-error")
        compose.onNodeWithText("Rashi").performClick()
        compose.onNodeWithText("Couldn't load rashi phalalu").assertIsDisplayed()
        capture("rashi-error")
    }
    @Test fun loadingStatesAreVisibleAndFullWidth() {
        launch("loading")
        compose.onNodeWithContentDescription("Loading news").assertIsDisplayed()
        capture("news-loading")
        compose.onNodeWithText("Rashi").performClick()
        compose.onNodeWithContentDescription("Loading rashi phalalu").assertIsDisplayed()
        capture("rashi-loading")
    }
    @Test fun fixtureReminderDoesNotRequestPermissionOrSchedule() {
        launch(); waitNews(); settings()
        compose.onNodeWithContentDescription("Remind me every morning").performScrollTo().performClick()
        compose.onNodeWithText("Notifications Disabled").assertIsDisplayed()
        compose.onNodeWithText("OK").performClick()
        assertFalse(SettingsStore(context, "fixture-settings").load().reminder)
    }
    @Test fun cacheRoundtripCorruptionAndPreferences() {
        val cache = ArticleCache(context, "test-cache.json")
        val articles = listOf(Article("Test", "Latest", "Synthetic", "https://example.invalid/article", snippet = "Synthetic body", scrapedAt = 42))
        cache.save(articles); assertEquals(articles, cache.load())
        File(context.filesDir, "test-cache.json").writeText("corrupt")
        assertTrue(cache.load().isEmpty())
        File(context.filesDir, "test-cache.json").delete()
        val store = SettingsStore(context, "test-settings")
        val value = Preferences(emptySet(), RashiSign.meen, Appearance.Dark, true, 22, 13)
        store.save(value); assertEquals(value, store.load())
        context.getSharedPreferences("test-settings", Context.MODE_PRIVATE).edit().clear().commit()
    }
    @Test fun androidRSSParserPreservesCDATAAndBlocksExternalEntities() {
        val xml = "<rss xmlns:content='urn:content'><channel><item><title>సింథటిక్ వార్త</title><link>https://example.invalid/test</link><description><![CDATA[సమాచారం]]></description><content:encoded><![CDATA[<p>Body</p><img src='https://example.invalid/image.jpg'>]]></content:encoded></item></channel></rss>"
        val articles = Parsing.rss(xml, "namasthe-telangana")
        assertEquals(1, articles.size)
        assertEquals("సమాచారం", articles.single().snippet)
        assertEquals("https://example.invalid/image.jpg", articles.single().imageUrl)
        assertTrue(Parsing.rss("<!DOCTYPE rss SYSTEM 'https://example.invalid/dtd'><rss/>", "sakshi").isEmpty())
    }
    @Test fun tabSwitchPreservesNewsScroll() {
        launch(); waitNews()
        compose.onNodeWithTag("news-list").performScrollToIndex(12)
        val before = compose.onNodeWithTag("news-list").fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.VerticalScrollAxisRange].value()
        assertTrue(before > 0)
        compose.onNodeWithText("Rashi").performClick()
        compose.onNodeWithText("News").performClick()
        val after = compose.onNodeWithTag("news-list").fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.VerticalScrollAxisRange].value()
        assertEquals(before, after, 1f)
    }
    @Test fun eachTabKeepsItsOwnHeaderDuringRapidNavigation() {
        launch(); waitNews()
        // A programmatic list jump bypasses the nested scroll that collapses the bar.
        compose.onNodeWithTag("news-list").performTouchInput { swipeUp() }
        val inlineTop = compose.onNodeWithText("సిటిజన్ కేన్").fetchSemanticsNode().boundsInRoot.top
        compose.onNodeWithText("Rashi").performClick()
        compose.waitForIdle()
        val expandedTop = compose.onNodeWithText("Rashi Phalalu").fetchSemanticsNode().boundsInRoot.top
        assertTrue("Rashi should retain its expanded header independently", expandedTop > inlineTop + 20f)
        repeat(5) {
            compose.onNodeWithText("News").performClick()
            compose.onNodeWithText("Rashi").performClick()
        }
        compose.onNodeWithText("News").performClick()
        assertEquals(inlineTop, compose.onNodeWithText("సిటిజన్ కేన్").fetchSemanticsNode().boundsInRoot.top, 1f)
    }
    @Test fun completeLicensesAreReadableOffline() {
        launch(); waitNews(); settings()
        compose.onNodeWithText("Open-source licenses").performScrollTo().performClick()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("Citizen Kane Android — Third-party notices", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Copyright (c) 2012 Silicon Andhra", substring = true).assertExists()
        compose.onNodeWithText("Copyright (c) 2009-2024 Jonathan Hedley", substring = true).assertExists()
        compose.onNodeWithText("Apache License", substring = true).assertExists()
        capture("licenses")
    }
}
