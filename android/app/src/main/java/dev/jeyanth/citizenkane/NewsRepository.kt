package dev.jeyanth.citizenkane

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

fun interface Transport { suspend fun get(url: String): String }

class HttpTransport : Transport {
    override suspend fun get(url: String): String = withContext(Dispatchers.IO) {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 15_000
            connection.readTimeout = 20_000
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            if (connection.responseCode !in 200..299) throw IOException("The provider is unavailable (${connection.responseCode}).")
            connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally { connection.disconnect() }
    }
}

data class FeedResult(val articles: List<Article>, val failedProviders: Set<String>)

class NewsRepository(private val transport: Transport) {
    private val sectionPaths = listOf("latest-news" to "Latest", "telangana" to "Telangana",
        "andhra-pradesh" to "Andhra Pradesh", "sports" to "Sports", "movies" to "Cinema", "business" to "Business")

    suspend fun news(enabled: Set<String>): FeedResult = withContext(Dispatchers.IO) { coroutineScope {
        val results = providers.filter { it.first in enabled }.map { (id, _) ->
            async {
                try { id to Result.success(fetchProvider(id)) }
                catch (e: CancellationException) { throw e }
                catch (e: Exception) { id to Result.failure<List<Article>>(e) }
            }
        }.awaitAll()
        FeedResult(interleave(results.map { it.second.getOrDefault(emptyList()) }),
            results.filter { it.second.isFailure }.map { it.first }.toSet())
    } }

    private suspend fun fetchProvider(id: String): List<Article> = coroutineScope {
        when (id) {
            "eenadu", "andhrajyothi" -> {
                val sections = if (id == "eenadu") sectionPaths else listOf(sectionPaths[1], sectionPaths[2], sectionPaths[0], sectionPaths[3], sectionPaths[5])
                sections.map { (path, category) -> async {
                    val html = transport.get(if (id == "eenadu") "https://www.eenadu.net/$path" else "https://www.andhrajyothy.com/$path")
                    if (id == "eenadu") Parsing.eenadu(html, category) else Parsing.andhrajyothi(html, category)
                } }.awaitAll().flatten().distinctBy { it.id }
            }
            else -> {
                val endpoint = when (id) {
                    "sakshi" -> "https://www.sakshi.com/rss.xml"
                    "namasthe-telangana" -> "https://www.ntnews.com/feed"
                    else -> "https://feeds.bbci.co.uk/telugu/rss.xml"
                }
                val articles = Parsing.rss(transport.get(endpoint), id).distinctBy { it.id }
                if (id != "sakshi") articles else {
                    val permits = Semaphore(4)
                    articles.map { article -> async { permits.withPermit {
                        val image = try { Parsing.openGraph(transport.get(article.url)) }
                        catch (e: CancellationException) { throw e }
                        catch (_: Exception) { null }
                        article.copy(imageUrl = image)
                    } } }.awaitAll()
                }
            }
        }
    }

    suspend fun rashi(sign: RashiSign): Prediction = withContext(Dispatchers.IO) { Parsing.rashi(
        transport.get("https://www.eenadu.net/rashi-phalalu/${sign.slug}"), sign
    ) ?: throw IOException("No prediction was found. Pull to refresh and try again.") }
}
