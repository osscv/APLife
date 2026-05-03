package net.dkly.aplife.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import net.dkly.aplife.calendar.CalendarSyncManager
import net.dkly.aplife.data.HolidayRepository
import net.dkly.aplife.data.TimetableRepository
import net.dkly.aplife.data.UserPreferences
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * Periodic background worker. Runs once a day; only does real work on Saturday
 * and Sunday so the upcoming week's timetable is in the user's calendars before
 * Monday morning.
 */
class AutoSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val prefs = UserPreferences(applicationContext)
        if (!prefs.autoSyncEnabled) return Result.success()
        val today = LocalDate.now().dayOfWeek
        if (today != DayOfWeek.SATURDAY && today != DayOfWeek.SUNDAY) {
            // Outside the weekend window — wait for the next weekend.
            return Result.success()
        }
        val intake = prefs.intakeCode?.takeIf { it.isNotBlank() } ?: return Result.success()
        val calendarIds = prefs.calendarIds
        if (calendarIds.isEmpty()) return Result.success()

        val notifier = SyncNotifier(applicationContext)
        return try {
            val timetableRepo = TimetableRepository()
            val holidayRepo = HolidayRepository(applicationContext)
            val calendar = CalendarSyncManager(applicationContext)

            val classes = timetableRepo.loadForIntake(intake, forceRefresh = true)
            val groups = prefs.selectedGroups
            val filtered = if (groups.isEmpty()) classes else classes.filter {
                it.grouping.isNullOrBlank() || it.grouping.lowercase() in groups.map { g -> g.lowercase() }
            }
            val exams = runCatching { timetableRepo.fetchExams(intake) }.getOrDefault(emptyList())
            val holidays = if (prefs.syncHolidays) {
                val year = LocalDate.now().year
                runCatching { holidayRepo.forStudents(year) }.getOrDefault(emptyList())
            } else emptyList()

            for (calendarId in calendarIds) {
                calendar.syncTimetable(calendarId, filtered, prefs.classReminderMinutes)
                calendar.syncExams(calendarId, exams, prefs.examReminderMinutes)
                if (holidays.isNotEmpty()) calendar.syncHolidays(calendarId, holidays)
            }

            prefs.lastSyncMs = System.currentTimeMillis()

            // Build summary stats for the notification.
            val zone = ZoneId.systemDefault()
            val classDates = filtered.mapNotNull { entry ->
                runCatching {
                    OffsetDateTime.parse(entry.timeFromIso).atZoneSameInstant(zone).toLocalDate()
                }.getOrNull()
            }
            notifier.notifySuccess(
                SyncStats(
                    intakeCode = intake,
                    startDate = classDates.minOrNull(),
                    endDate = classDates.maxOrNull(),
                    weeks = classDates.map { it.with(DayOfWeek.MONDAY) }.distinct().size,
                    classCount = filtered.size,
                    examCount = exams.size,
                    holidayCount = holidays.size,
                )
            )
            // (Re)schedule the per-exam "Good luck" notifications.
            ExamLuckScheduler(applicationContext).rescheduleAll(exams)
            // (Re)schedule the day-before-holiday reminders.
            HolidayReminderScheduler(applicationContext).rescheduleAll(holidays)
            Result.success()
        } catch (t: Throwable) {
            notifier.notifyFailure(intake, t.message)
            Result.retry()
        }
    }

    companion object {
        const val UNIQUE_NAME = "aplife-auto-sync"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            // Daily — the worker itself filters to Sat/Sun before doing real work.
            val request = PeriodicWorkRequestBuilder<AutoSyncWorker>(24, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context.applicationContext)
                .enqueueUniquePeriodicWork(UNIQUE_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context.applicationContext).cancelUniqueWork(UNIQUE_NAME)
        }
    }
}
