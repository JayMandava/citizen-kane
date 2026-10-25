package dev.jeyanth.citizenkane

import android.content.Intent
import kotlinx.coroutines.delay
import java.io.IOException

object RuntimeMode {
    fun fixture(intent: Intent) = intent.getStringExtra("fixture")
    fun transport(mode: String?): Transport = if (mode == null) HttpTransport() else Transport { url ->
        if (mode == "loading") delay(30_000)
        if (mode == "offline" || mode == "error") throw IOException("Synthetic offline")
        when {
            url.contains("rashi-phalalu") -> "<h2 class='ttl'>ఈరోజు (25-10-2026)</h2><p>ఈరోజు మీ పనుల్లో మంచి పురోగతి ఉంటుంది. కుటుంబంతో ఆనందంగా గడుపుతారు.</p><h2 class='ttl'>ఈవారం</h2><p>ఆలోచించి నిర్ణయాలు తీసుకోండి. కొత్త అవకాశాలు లభిస్తాయి.</p>"
            url.contains("eenadu.net") -> "<a href='https://example.invalid/eenadu/${url.substringAfterLast('/')}'><div class='bt-thumb-description'><h3 class='article-title-rgt'>తెలుగు వార్తలు: కొత్త అవకాశాలతో ముందుకు సాగుతున్న ప్రజలు</h3>ఇది పరీక్ష కోసం రూపొందించిన నమూనా వార్త.</div></a>"
            url.contains("andhrajyothy.com") -> "<figure><figcaption><h3><a href='https://example.invalid/andhrajyothi/${url.substringAfterLast('/')}'>తెలంగాణలో అభివృద్ధి కార్యక్రమాలపై ప్రత్యేక కథనం</a></h3></figcaption></figure>"
            url.contains("rss") || url.contains("/feed") -> "<rss><channel><item><title>తెలుగు ప్రజల కోసం నేటి ముఖ్యమైన వార్తల సమాహారం</title><link>https://example.invalid/${url.substringAfter("https://").substringBefore('/')}</link><description>పరీక్ష కోసం రూపొందించిన వార్తా వివరణ.</description><pubDate>Sun, 25 Oct 2026 07:00:00 GMT</pubDate></item></channel></rss>"
            else -> ""
        }
    }
}
