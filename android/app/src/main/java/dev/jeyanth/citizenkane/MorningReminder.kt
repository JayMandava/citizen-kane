package dev.jeyanth.citizenkane

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import java.time.ZonedDateTime

fun nextReminder(now: ZonedDateTime, hour: Int, minute: Int): ZonedDateTime {
    val time = java.time.LocalTime.of(hour, minute)
    val today = now.toLocalDate().atTime(time).atZone(now.zone)
    return if (today.isAfter(now)) today else now.toLocalDate().plusDays(1).atTime(time).atZone(now.zone)
}

object MorningReminder {
    private const val channel = "morning-news"
    fun permitted(context: Context): Boolean =
        (Build.VERSION.SDK_INT < 33 || context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
            context.getSystemService(NotificationManager::class.java).areNotificationsEnabled()

    fun schedule(context: Context, preferences: Preferences) {
        val alarm = context.getSystemService(AlarmManager::class.java)
        val intent = PendingIntent.getBroadcast(context, 1, Intent(context, MorningReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        alarm.cancel(intent)
        if (!preferences.reminder || !permitted(context)) return
        // News reminders do not qualify for exact-alarm special access. Reschedule by local wall time daily.
        alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
            nextReminder(ZonedDateTime.now(), preferences.hour, preferences.minute).toInstant().toEpochMilli(), intent)
    }

    fun deliver(context: Context) {
        if (!permitted(context)) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(channel, "Morning news", NotificationManager.IMPORTANCE_DEFAULT))
        val open = PendingIntent.getActivity(context, 2, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        manager.notify(1, NotificationCompat.Builder(context, channel).setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("సిటిజన్ కేన్").setContentText("Your Telugu news and rashi phalalu are ready.")
            .setContentIntent(open).setAutoCancel(true).build())
    }
}

class MorningReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val preferences = SettingsStore(context).load()
        if (preferences.reminder) MorningReminder.deliver(context)
        MorningReminder.schedule(context, preferences)
    }
}
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action in setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_TIME_CHANGED,
                Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_MY_PACKAGE_REPLACED)) {
            MorningReminder.schedule(context, SettingsStore(context).load())
        }
    }
}
