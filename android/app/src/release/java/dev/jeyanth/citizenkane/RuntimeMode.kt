package dev.jeyanth.citizenkane
import android.content.Intent
object RuntimeMode {
    fun fixture(intent: Intent): String? = null
    fun transport(mode: String?): Transport = HttpTransport()
}
