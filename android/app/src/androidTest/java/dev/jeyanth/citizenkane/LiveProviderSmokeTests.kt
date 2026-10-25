package dev.jeyanth.citizenkane

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Opt-in checks against public, unauthenticated news pages; never use account or personal data. */
class LiveProviderSmokeTests {
    private fun requireOptIn() = assumeTrue(InstrumentationRegistry.getArguments().getString("live") == "true")

    @Test fun currentPublicProvidersReturnHeadlines() = runBlocking {
        requireOptIn()
        val result = NewsRepository(HttpTransport()).news(providers.map { it.first }.toSet())
        val counts = result.articles.groupingBy { it.source }.eachCount()
        println("Public provider counts: $counts; failed providers: ${result.failedProviders}")
        providers.forEach { (_, name) -> assertTrue("No current headlines from $name", counts.getOrDefault(name, 0) > 0) }
    }

    @Test fun currentPublicRashiReturnsToday() = runBlocking {
        requireOptIn()
        val prediction = NewsRepository(HttpTransport()).rashi(RashiSign.makar)
        assertEquals(RashiSign.makar, prediction.sign)
        assertTrue(prediction.today.isNotBlank())
        println("Public rashi has Today; weekly present: ${prediction.weekly.isNotBlank()}")
    }
}
