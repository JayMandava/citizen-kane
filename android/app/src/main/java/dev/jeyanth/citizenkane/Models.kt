package dev.jeyanth.citizenkane

data class Article(
    val source: String, val category: String, val title: String, val url: String,
    val imageUrl: String? = null, val snippet: String? = null,
    val scrapedAt: Long = System.currentTimeMillis(),
) { val id: String get() = url }

enum class RashiSign(val telugu: String, val english: String) {
    mesh("మేషం", "Aries"), vrushabha("వృషభం", "Taurus"), mithun("మిథునం", "Gemini"),
    kark("కర్కాటకం", "Cancer"), singh("సింహం", "Leo"), kanya("కన్య", "Virgo"),
    tula("తుల", "Libra"), vrushchik("వృశ్చికం", "Scorpio"), dhanu("ధనుస్సు", "Sagittarius"),
    makar("మకరం", "Capricorn"), kumbh("కుంభం", "Aquarius"), meen("మీనం", "Pisces");
    val slug: String get() = "$name-rashi-today"
}
data class Prediction(val sign: RashiSign, val date: String, val today: String, val weekly: String)
enum class Appearance { System, Light, Dark }
data class Preferences(
    val enabledProviders: Set<String> = providers.map { it.first }.toSet(),
    val sign: RashiSign = RashiSign.makar, val appearance: Appearance = Appearance.System,
    val reminder: Boolean = false, val hour: Int = 7, val minute: Int = 0,
)
val providers = listOf("eenadu" to "Eenadu", "andhrajyothi" to "Andhrajyothi", "sakshi" to "Sakshi",
    "namasthe-telangana" to "Namasthe Telangana", "bbc-telugu" to "BBC Telugu")

/** Keep scrape times from overwhelming RSS publication dates. */
fun interleave(groups: List<List<Article>>): List<Article> = buildList {
    val sorted = groups.map { it.sortedByDescending(Article::scrapedAt) }
    repeat(sorted.maxOfOrNull { it.size } ?: 0) { index ->
        sorted.forEach { group -> group.getOrNull(index)?.let { add(it) } }
    }
}
