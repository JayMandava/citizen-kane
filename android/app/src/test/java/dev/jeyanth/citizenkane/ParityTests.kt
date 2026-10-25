package dev.jeyanth.citizenkane

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.time.ZoneId
import java.time.ZonedDateTime

class ParityTests {
    private fun fixture(name: String) = javaClass.getResource("/fixtures/$name")!!.readText()

    @Test fun eenaduMatchesIOSFixture() {
        val articles = Parsing.eenadu(fixture("eenadu_latest_news.html"), "Latest")
        assertEquals(3, articles.size)
        val first = articles.first()
        assertEquals("సింథటిక్ వార్త ఒకటి: పరీక్ష కోసం రూపొందించిన శీర్షిక", first.title)
        assertTrue(first.url.contains("synthetic-eenadu-one"))
        assertTrue(first.imageUrl!!.startsWith("https://example.invalid/"))
        assertTrue(first.snippet!!.contains("నమూనా"))
        assertFalse(first.snippet.contains(first.title))
        assertEquals(first.url, first.id)
    }
    @Test fun eenaduRejectsMissingTitleAndUnsafeLinks() {
        assertTrue(Parsing.eenadu("<a href='javascript:alert(1)'><div class='bt-thumb-description'><h3>Title</h3></div></a>", "Latest").isEmpty())
        assertTrue(Parsing.eenadu("<p>Nothing</p>", "Latest").isEmpty())
    }
    @Test fun andhrajyothiMatchesIOSFixture() {
        val articles = Parsing.andhrajyothi(fixture("andhrajyothi_telangana.html"), "Telangana")
        assertEquals(4, articles.size)
        assertTrue(articles.first().title.contains("సింథటిక్"))
        assertTrue(articles.first().url.contains("synthetic-telangana-one"))
        assertNotNull(articles.first().imageUrl)
    }
    @Test fun andhrajyothiSkipsShortHeadings() {
        assertTrue(Parsing.andhrajyothi("<h3><a href='/x'>too short</a></h3>", "Latest").isEmpty())
    }
    @Test fun bbcMatchesIOSFixture() {
        val articles = Parsing.rss(fixture("bbc_telugu_feed.xml"), "bbc-telugu")
        assertEquals(2, articles.size)
        assertTrue(articles.first().title.contains("సింథటిక్"))
        assertTrue(articles.first().url.contains("bbc-first"))
        assertTrue(articles.first().imageUrl!!.startsWith("https://example.invalid/"))
        assertTrue(articles.first().snippet!!.contains("నమూనా"))
    }
    @Test fun wordpressPreservesCDATAAndEmbeddedImage() {
        val articles = Parsing.rss(fixture("namasthe_telangana_feed.xml"), "namasthe-telangana")
        assertEquals(2, articles.size)
        assertTrue(articles.first().title.startsWith("సింథటిక్"))
        assertTrue(articles.first().url.contains("namasthe-first"))
        assertTrue(articles.first().imageUrl!!.startsWith("https://example.invalid/"))
    }
    @Test fun sakshiHasNoBoilerplateSnippetOrFeedImage() {
        val articles = Parsing.rss(fixture("sakshi_feed.xml"), "sakshi")
        assertEquals(2, articles.size)
        assertTrue(articles.first().title.contains("సింథటిక్"))
        assertNull(articles.first().snippet)
        assertNull(articles.first().imageUrl)
    }
    @Test fun openGraphMatchesIOSFixture() {
        assertEquals("https://example.invalid/sakshi-image.jpg", Parsing.openGraph(fixture("sakshi_article_head.html")))
        assertNull(Parsing.openGraph("<p>Nothing</p>"))
    }
    @Test fun rashiMatchesIOSFixture() {
        val result = Parsing.rashi(fixture("eenadu_makara_rashi.html"), RashiSign.makar)!!
        assertEquals("26-09-2026", result.date)
        assertTrue(result.today.startsWith("సింథటిక్ ఈరోజు"))
        assertTrue(result.weekly.startsWith("సింథటిక్ ఈవారం"))
        assertEquals(RashiSign.makar, result.sign)
    }
    @Test fun rashiRejectsMissingTodayAndStopsAtNextHeading() {
        assertNull(Parsing.rashi("<h2 class='ttl'>This Week</h2><p>Week</p>", RashiSign.mesh))
        val result = Parsing.rashi("<h3 class='ttl'>Today (date)</h3><p>One</p><p>Two</p><h2>Other</h2><p>Excluded</p>", RashiSign.mesh)!!
        assertEquals("One Two", result.today)
        assertEquals("", result.weekly)
    }
    @Test fun urlResolutionAndWhitespace() {
        assertEquals("https://example.com/a", Parsing.url("/a", "https://example.com"))
        assertEquals("https://cdn.example.com/a", Parsing.url("//cdn.example.com/a", "https://example.com"))
        assertNull(Parsing.url("file:///etc/passwd"))
        assertNull(Parsing.url(""))
        assertEquals("one two", Parsing.normalize(" one\n\t two "))
    }
    @Test fun datesHandleBothRSSZoneFormats() {
        assertEquals(Parsing.parseDate("Sat, 26 Sep 2026 07:00:00 GMT"), Parsing.parseDate("Sat, 26 Sep 2026 12:30:00 +0530"))
        assertNotNull(Parsing.parseDate("Sat, 26 Sep 2026 07:00:00 GMT"))
        assertNull(Parsing.parseDate("invalid"))
    }
    @Test fun malformedAndExternalEntityRSSIsRejected() {
        assertTrue(Parsing.rss("<rss><broken>", "sakshi").isEmpty())
        assertTrue(Parsing.rss("<!DOCTYPE rss [<!ENTITY secret SYSTEM 'file:///etc/passwd'>]><rss><channel><item><title>&secret;</title><link>https://example.com</link></item></channel></rss>", "sakshi").isEmpty())
    }
    @Test fun feedIsFairAndSortedOnlyWithinEachSource() {
        fun article(id: String, source: String, time: Long) = Article(source, "Latest", id, "https://example.com/$id", scrapedAt = time)
        val result = interleave(listOf(listOf(article("a1", "A", 1), article("a2", "A", 2), article("a3", "A", 3)),
            listOf(article("b1", "B", 0)), listOf(article("c1", "C", -1))))
        assertEquals(listOf("a3", "b1", "c1", "a2", "a1"), result.map { it.title })
        assertTrue(interleave(emptyList()).isEmpty())
    }
    @Test fun defaultPreferencesAndAllSignsMatchIOS() {
        val value = Preferences()
        assertEquals(5, value.enabledProviders.size)
        assertEquals(RashiSign.makar, value.sign)
        assertEquals(Appearance.System, value.appearance)
        assertFalse(value.reminder)
        assertEquals(7, value.hour)
        assertEquals(0, value.minute)
        assertEquals(12, RashiSign.entries.size)
        assertEquals("makar-rashi-today", RashiSign.makar.slug)
    }
    @Test fun partialProviderFailuresStillReturnUsableNews() = runTest {
        val repository = NewsRepository(Transport { url ->
            if (url.contains("bbci")) fixture("bbc_telugu_feed.xml") else throw IOException("Synthetic failure")
        })
        val result = repository.news(setOf("bbc-telugu", "eenadu"))
        assertEquals(2, result.articles.size)
        assertEquals(setOf("eenadu"), result.failedProviders)
    }
    @Test fun disabledProvidersNeverMakeRequests() = runTest {
        var requests = 0
        val repository = NewsRepository(Transport { requests++; error("Unexpected request") })
        assertTrue(repository.news(emptySet()).articles.isEmpty())
        assertEquals(0, requests)
    }
    @Test fun allProviderFailuresAreReported() = runTest {
        val repository = NewsRepository(Transport { throw IOException("Synthetic") })
        val result = repository.news(setOf("bbc-telugu", "sakshi"))
        assertTrue(result.articles.isEmpty())
        assertEquals(setOf("bbc-telugu", "sakshi"), result.failedProviders)
    }
    @Test fun scrapedSectionsDeduplicateURLs() = runTest {
        val repository = NewsRepository(Transport { fixture("eenadu_latest_news.html") })
        assertEquals(3, repository.news(setOf("eenadu")).articles.size)
    }
    @Test fun sakshiImageFailureDoesNotLoseHeadlines() = runTest {
        val repository = NewsRepository(Transport { url ->
            if (url.endsWith("rss.xml")) fixture("sakshi_feed.xml") else throw IOException("Image unavailable")
        })
        val articles = repository.news(setOf("sakshi")).articles
        assertEquals(2, articles.size)
        assertTrue(articles.all { it.imageUrl == null })
    }
    @Test fun cancellationIsNeverConvertedToProviderFailure() = runTest {
        try {
            NewsRepository(Transport { throw CancellationException("Canceled") }).news(setOf("bbc-telugu"))
            fail("Cancellation must propagate")
        } catch (_: CancellationException) { }
    }
    @Test fun reminderMovesToNextDayAfterSelectedTime() {
        val now = ZonedDateTime.of(2026, 10, 25, 8, 0, 0, 0, ZoneId.of("Asia/Kolkata"))
        assertEquals(26, nextReminder(now, 7, 0).dayOfMonth)
        assertEquals(25, nextReminder(now, 9, 0).dayOfMonth)
        assertEquals(26, nextReminder(now.withHour(7), 7, 0).dayOfMonth)
    }
    @Test fun reminderPreservesWallClockAcrossDST() {
        val now = ZonedDateTime.of(2026, 10, 31, 8, 0, 0, 0, ZoneId.of("America/New_York"))
        val next = nextReminder(now, 7, 30)
        assertEquals(7, next.hour); assertEquals(30, next.minute); assertEquals(1, next.dayOfMonth)
    }
    @Test fun reminderDoesNotCarrySpringGapAdjustmentIntoTomorrow() {
        val now = ZonedDateTime.of(2026, 3, 8, 4, 0, 0, 0, ZoneId.of("America/New_York"))
        val next = nextReminder(now, 2, 30)
        assertEquals(9, next.dayOfMonth); assertEquals(2, next.hour); assertEquals(30, next.minute)
    }
}
