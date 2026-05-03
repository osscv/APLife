package net.dkly.aplife.sync

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import net.dkly.aplife.MainActivity
import net.dkly.aplife.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Posts a notification when an APLife sync (manual or background) finishes.
 */
class SyncNotifier(private val context: Context) {

    init { ensureChannel() }

    fun notifySuccess(stats: SyncStats) {
        val title = "Timetable synced · ${stats.intakeCode.ifBlank { "APLife" }}"
        val body = buildSuccessBody(stats)
        post(NOTIFICATION_ID_SUCCESS, title, body)
    }

    fun notifyFailure(intakeCode: String, reason: String?) {
        val title = "Timetable sync failed · ${intakeCode.ifBlank { "APLife" }}"
        val body = buildString {
            append("Couldn't update your calendar. ")
            if (!reason.isNullOrBlank()) append(reason)
            else append("Please try again from the Schedule tab.")
        }
        post(NOTIFICATION_ID_FAILURE, title, body)
    }

    private fun buildSuccessBody(stats: SyncStats): String = buildString {
        if (stats.startDate != null && stats.endDate != null) {
            append(stats.startDate.format(DAY_DATE_FMT))
            append(" – ")
            append(stats.endDate.format(DAY_DATE_FMT))
            append(" · ")
        }
        append(stats.weeks)
        append(if (stats.weeks == 1) " week · " else " weeks · ")
        append(stats.classCount)
        append(if (stats.classCount == 1) " class" else " classes")
        if (stats.examCount > 0) {
            append(" · ")
            append(stats.examCount)
            append(if (stats.examCount == 1) " exam" else " exams")
        }
        if (stats.holidayCount > 0) {
            append(" · ")
            append(stats.holidayCount)
            append(if (stats.holidayCount == 1) " holiday" else " holidays")
        }
    }

    private fun post(id: Int, title: String, body: String) {
        if (!hasPermission()) return
        val tap = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(tap)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(id, notification) }
    }

    private fun hasPermission(): Boolean {
        if (Build.VERSION.SDK_INT < 33) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Timetable sync",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Tells you when your APU timetable has been updated in your calendar."
        }
        nm.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "aplife_sync"
        const val NOTIFICATION_ID_SUCCESS = 1001
        const val NOTIFICATION_ID_FAILURE = 1002
        private val DAY_DATE_FMT: DateTimeFormatter =
            DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
    }
}

data class SyncStats(
    val intakeCode: String,
    val startDate: LocalDate?,
    val endDate: LocalDate?,
    val weeks: Int,
    val classCount: Int,
    val examCount: Int,
    val holidayCount: Int,
)
