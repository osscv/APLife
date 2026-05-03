package net.dkly.aplife.sync

import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import net.dkly.aplife.data.ExamEntry
import java.time.Instant
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

/**
 * Schedules a one-time WorkManager job ~5 minutes before each exam to post a
 * personalised "Good luck for your <subject> exam at <time>" notification.
 * On every sync we cancel the previous batch and reschedule from scratch.
 */
class ExamLuckScheduler(private val context: Context) {

    fun rescheduleAll(exams: List<ExamEntry>) {
        val wm = WorkManager.getInstance(context.applicationContext)
        wm.cancelAllWorkByTag(TAG)
        val now = Instant.now()
        for (exam in exams) {
            val examInstant = runCatching { OffsetDateTime.parse(exam.since).toInstant() }
                .getOrNull() ?: continue
            val triggerInstant = examInstant.minus(LEAD_MINUTES, ChronoUnit.MINUTES)
            if (!triggerInstant.isAfter(now)) continue
            val delayMs = java.time.Duration.between(now, triggerInstant).toMillis()

            val data = workDataOf(
                ExamLuckWorker.KEY_NAME to exam.subjectDescription,
                ExamLuckWorker.KEY_MODULE to exam.module,
                ExamLuckWorker.KEY_VENUE to (exam.venue.orEmpty()),
                ExamLuckWorker.KEY_SINCE_ISO to exam.since,
            )
            val request = OneTimeWorkRequestBuilder<ExamLuckWorker>()
                .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                .setInputData(data)
                .addTag(TAG)
                .build()
            wm.enqueue(request)
        }
    }

    companion object {
        const val TAG = "aplife-exam-luck"
        /** Fire the "good luck" notification 5 minutes before exam start. */
        const val LEAD_MINUTES = 5L
    }
}
