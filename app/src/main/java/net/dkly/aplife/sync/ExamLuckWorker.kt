package net.dkly.aplife.sync

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import net.dkly.aplife.MainActivity
import net.dkly.aplife.R
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class ExamLuckWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        // Re-use the SyncNotifier's channel so the notification surface is consistent.
        SyncNotifier(applicationContext) // ensures the channel exists

        val name = inputData.getString(KEY_NAME).orEmpty().ifBlank { "your exam" }
        val module = inputData.getString(KEY_MODULE).orEmpty()
        val venue = inputData.getString(KEY_VENUE).orEmpty()
        val sinceIso = inputData.getString(KEY_SINCE_ISO).orEmpty()

        val timeStr = runCatching {
            OffsetDateTime.parse(sinceIso)
                .atZoneSameInstant(ZoneId.systemDefault())
                .format(TIME_FMT)
        }.getOrDefault("")

        val title = "Good luck — $name"
        val body = buildString {
            append("Good luck for your ").append(name).append(" exam")
            if (timeStr.isNotBlank()) append(" at ").append(timeStr)
            if (module.isNotBlank()) append(" · ").append(module)
            if (venue.isNotBlank()) append('\n').append("Venue: ").append(venue)
            append("\nYou've got this!")
        }
        post(title, body, sinceIso)
        return Result.success()
    }

    private fun post(title: String, body: String, key: String) {
        if (!hasNotificationPermission()) return
        val tap = PendingIntent.getActivity(
            applicationContext,
            0,
            Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(applicationContext, SyncNotifier.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body.lineSequence().firstOrNull().orEmpty())
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(tap)
            .build()
        runCatching {
            NotificationManagerCompat.from(applicationContext).notify(notificationId(key), notification)
        }
    }

    private fun hasNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < 33) return true
        return ContextCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun notificationId(key: String): Int {
        // Stable but unique-per-exam id derived from the ISO start string.
        return 2_000_000 + (key.hashCode() and 0x0FFFFFFF)
    }

    companion object {
        const val KEY_NAME = "exam_name"
        const val KEY_MODULE = "exam_module"
        const val KEY_VENUE = "exam_venue"
        const val KEY_SINCE_ISO = "exam_since_iso"
        private val TIME_FMT: DateTimeFormatter =
            DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)
    }
}
