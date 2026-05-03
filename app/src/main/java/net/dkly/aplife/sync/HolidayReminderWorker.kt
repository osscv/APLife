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
import java.time.LocalDate

class HolidayReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        SyncNotifier(applicationContext) // ensure channel
        val name = inputData.getString(KEY_NAME).orEmpty().ifBlank { return Result.success() }
        val dateIso = inputData.getString(KEY_DATE_ISO).orEmpty()
        val date = runCatching { LocalDate.parse(dateIso) }.getOrNull() ?: return Result.success()

        val (title, body) = holidayEveMessage(name, date)
        post(title, body, dateIso)
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
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
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

    private fun notificationId(key: String): Int = 3_000_000 + (key.hashCode() and 0x0FFFFFFF)

    companion object {
        const val KEY_NAME = "holiday_name"
        const val KEY_DATE_ISO = "holiday_date_iso"
    }
}
