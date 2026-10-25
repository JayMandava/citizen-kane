package dev.jeyanth.citizenkane

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler
import java.io.StringReader
import java.net.URI
import java.text.SimpleDateFormat
import java.util.Locale
import javax.xml.parsers.SAXParserFactory
import org.xml.sax.InputSource

object Parsing {
    fun normalize(text: String) = text.trim().replace(Regex("[\\s\\u00a0]+"), " ")
    fun url(value: String?, base: String = ""): String? = runCatching {
        val raw = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val uri = if (base.isEmpty()) URI(raw) else URI(base).resolve(raw)
        uri.takeIf { it.scheme in listOf("https", "http") && !it.host.isNullOrBlank() }?.toString()
    }.getOrNull()

    fun eenadu(html: String, category: String): List<Article> {
        val base = "https://www.eenadu.net"
        return Jsoup.parse(html).select(".bt-thumb-description").take(20).mapNotNull { card ->
            val title = card.selectFirst("h3.article-title-rgt") ?: card.selectFirst("h2,h3,h4")
            val text = title?.text()?.let(::normalize)?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            val anchor = generateSequence(card) { it.parent() }.take(4).firstOrNull { it.tagName() == "a" }
                ?: return@mapNotNull null
            val link = url(anchor.attr("href"), base) ?: return@mapNotNull null
            Article("Eenadu", category, text, link, url(anchor.selectFirst("img")?.attr("src"), base),
                normalize(card.ownText()).takeIf { it.isNotEmpty() })
        }
    }

    fun andhrajyothi(html: String, category: String): List<Article> {
        val base = "https://www.andhrajyothy.com"
        return Jsoup.parse(html).select("h3").take(20).mapNotNull { heading ->
            val anchor = heading.selectFirst("a") ?: return@mapNotNull null
            val title = normalize(anchor.text()).takeIf { it.codePointCount(0, it.length) >= 15 }
                ?: return@mapNotNull null
            val link = url(anchor.attr("href"), base) ?: return@mapNotNull null
            Article("Andhrajyothi", category, title, link,
                url(heading.parent()?.parent()?.selectFirst("img")?.attr("src"), base))
        }
    }

    fun openGraph(html: String): String? = url(Jsoup.parse(html).selectFirst("meta[property=og:image]")?.attr("content"))

    fun rss(xml: String, provider: String): List<Article> {
        // Expat on Android does not support Xerces' disallow-doctype-decl feature.
        // Feeds need no DTD. Reject declarations before parsing on every platform.
        if (Regex("<!\\s*(DOCTYPE|ENTITY)\\b", RegexOption.IGNORE_CASE).containsMatchIn(xml)) return emptyList()
        val items = mutableListOf<Map<String, String>>()
        var item: MutableMap<String, String>? = null
        var field = ""
        val buffer = StringBuilder()
        val handler = object : DefaultHandler() {
            override fun startElement(uri: String?, localName: String?, qName: String, attributes: Attributes) {
                if (qName == "item") item = mutableMapOf()
                field = qName
                buffer.setLength(0)
                if (qName == "media:thumbnail") item?.set("image", attributes.getValue("url") ?: "")
            }
            override fun characters(ch: CharArray, start: Int, length: Int) { buffer.append(ch, start, length) }
            override fun endElement(uri: String?, localName: String?, qName: String) {
                if (qName == field) item?.set(qName, buffer.toString().trim())
                if (qName == "item") { item?.let { items.add(it.toMap()) }; item = null }
                buffer.setLength(0)
            }
        }
        runCatching {
            val factory = SAXParserFactory.newInstance()
            runCatching { factory.setFeature("http://xml.org/sax/features/external-general-entities", false) }
            runCatching { factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
            val reader = factory.newSAXParser().xmlReader
            reader.entityResolver = org.xml.sax.EntityResolver { _, _ -> InputSource(StringReader("")) }
            reader.contentHandler = handler
            reader.errorHandler = handler
            reader.parse(InputSource(StringReader(xml)))
        }.getOrElse { return emptyList() }
        val name = providers.first { it.first == provider }.second
        return items.mapNotNull { data ->
            val title = data["title"]?.let(::normalize)?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            val link = url(data["link"]) ?: return@mapNotNull null
            val image = when (provider) {
                "bbc-telugu" -> url(data["image"])
                "namasthe-telangana" -> url(Jsoup.parse(data["content:encoded"].orEmpty()).selectFirst("img")?.attr("src"))
                else -> null
            }
            Article(name, "Latest", title, link, image,
                if (provider == "sakshi") null else data["description"]?.let(::normalize),
                parseDate(data["pubDate"]) ?: System.currentTimeMillis())
        }
    }

    fun parseDate(value: String?): Long? {
        if (value == null) return null
        for (format in listOf("EEE, d MMM yyyy HH:mm:ss Z", "EEE, d MMM yyyy HH:mm:ss zzz")) {
            runCatching { SimpleDateFormat(format, Locale.US).parse(value)?.time }.getOrNull()?.let { return it }
        }
        return null
    }

    fun rashi(html: String, sign: RashiSign): Prediction? {
        var today = ""; var weekly = ""; var date = ""
        Jsoup.parse(html).select("h2.ttl,h3.ttl").forEach { heading ->
            val label = heading.text()
            val text = generateSequence(heading.nextElementSibling()) { it.nextElementSibling() }
                .takeWhile { it.tagName() !in listOf("h2", "h3") }.filter { it.tagName() == "p" }
                .joinToString(" ") { it.text() }.let(::normalize)
            when {
                label.contains("ఈరోజు") || label.contains("today", true) -> {
                    today = text; date = Regex("\\(([^)]+)\\)").find(label)?.groupValues?.get(1).orEmpty()
                }
                label.contains("ఈవారం") || label.contains("week", true) -> weekly = text
            }
        }
        return today.takeIf { it.isNotEmpty() }?.let { Prediction(sign, date, it, weekly) }
    }
}
