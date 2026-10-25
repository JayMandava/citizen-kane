package dev.jeyanth.citizenkane

import android.content.Context
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class SettingsStore(context: Context, name: String = "settings") {
    private val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
    fun load() = Preferences(
        enabledProviders = prefs.getStringSet("providers", providers.map { it.first }.toSet())!!.toSet(),
        sign = runCatching { RashiSign.valueOf(prefs.getString("sign", "makar")!!) }.getOrDefault(RashiSign.makar),
        appearance = runCatching { Appearance.valueOf(prefs.getString("appearance", "System")!!) }.getOrDefault(Appearance.System),
        reminder = prefs.getBoolean("reminder", false), hour = prefs.getInt("hour", 7).coerceIn(0, 23),
        minute = prefs.getInt("minute", 0).coerceIn(0, 59),
    )
    fun save(value: Preferences) {
        prefs.edit().putStringSet("providers", value.enabledProviders).putString("sign", value.sign.name)
            .putString("appearance", value.appearance.name).putBoolean("reminder", value.reminder)
            .putInt("hour", value.hour).putInt("minute", value.minute).apply()
    }
}

class ArticleCache(context: Context, filename: String = "cached-articles.json") {
    private val file = AtomicFile(File(context.filesDir, filename))
    fun load(): List<Article> = runCatching {
        val array = JSONArray(file.openRead().bufferedReader().use { it.readText() })
        (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            Article(item.getString("source"), item.getString("category"), item.getString("title"), item.getString("url"),
                item.optString("imageUrl").takeIf { it.isNotEmpty() }, item.optString("snippet").takeIf { it.isNotEmpty() },
                item.getLong("scrapedAt"))
        }
    }.getOrDefault(emptyList())

    fun save(articles: List<Article>) {
        val json = JSONArray().apply { articles.forEach { article ->
            put(JSONObject().put("source", article.source).put("category", article.category).put("title", article.title)
                .put("url", article.url).put("imageUrl", article.imageUrl.orEmpty()).put("snippet", article.snippet.orEmpty())
                .put("scrapedAt", article.scrapedAt))
        } }
        val stream = file.startWrite()
        try { stream.write(json.toString().toByteArray(Charsets.UTF_8)); file.finishWrite(stream) }
        catch (e: Exception) { file.failWrite(stream); throw e }
    }
}
