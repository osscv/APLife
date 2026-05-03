package net.dkly.aplife.sync

import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import net.dkly.aplife.data.Holiday
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * Schedules a one-time notification at 7 PM on the day BEFORE each upcoming
 * holiday with a friendly customised message.
 */
class HolidayReminderScheduler(private val context: Context) {

    fun rescheduleAll(holidays: List<Holiday>) {
        val wm = WorkManager.getInstance(context.applicationContext)
        wm.cancelAllWorkByTag(TAG)
        val now = Instant.now()
        val zone = ZoneId.systemDefault()
        for (h in holidays) {
            val holidayDate = Instant.ofEpochMilli(h.startEpochMs).atZone(zone).toLocalDate()
            // Eve reminder at 7 PM the day before
            val eveAt = holidayDate.minusDays(1).atTime(LocalTime.of(19, 0))
                .atZone(zone).toInstant()
            schedule(wm, now, eveAt, h.name, holidayDate, HolidayReminderWorker.KIND_EVE)
            // Festive greeting at 8 AM on the day
            val dayAt = holidayDate.atTime(LocalTime.of(8, 0)).atZone(zone).toInstant()
            schedule(wm, now, dayAt, h.name, holidayDate, HolidayReminderWorker.KIND_DAY_OF)
        }
    }

    private fun schedule(
        wm: WorkManager,
        now: Instant,
        triggerAt: Instant,
        name: String,
        date: java.time.LocalDate,
        kind: String,
    ) {
        if (!triggerAt.isAfter(now)) return
        val delayMs = java.time.Duration.between(now, triggerAt).toMillis()
        val data = workDataOf(
            HolidayReminderWorker.KEY_NAME to name,
            HolidayReminderWorker.KEY_DATE_ISO to date.toString(),
            HolidayReminderWorker.KEY_KIND to kind,
        )
        val request = OneTimeWorkRequestBuilder<HolidayReminderWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag(TAG)
            .build()
        wm.enqueue(request)
    }

    companion object {
        const val TAG = "aplife-holiday-eve"
    }
}

/**
 * Short, casual eve reminder — fires the night before the holiday.
 */
internal fun holidayEveReminder(holidayName: String, @Suppress("UNUSED_PARAMETER") date: LocalDate): Pair<String, String> {
    return "No class tomorrow!" to
        "Hahah, tomorrow is $holidayName — enjoy your day off!"
}

/**
 * Festive day-of greeting — fires on the morning of the holiday.
 */
internal fun holidayDayOfGreeting(holidayName: String, date: LocalDate): Pair<String, String> {
    val n = holidayName.lowercase()
    val isAug31 = date.monthValue == 8 && date.dayOfMonth == 31
    val isSep16 = date.monthValue == 9 && date.dayOfMonth == 16

    return when {
        "chinese new year" in n || "lunar" in n || n.contains("cny") ->
            "Gōngxǐ fācái!" to
                "Today is $holidayName. Time to see family, eat tangerines and enjoy the day off. Have a prosperous one!"
        "hari raya aidilfitri" in n || "aidilfitri" in n || ("hari raya" in n && "haji" !in n) ->
            "Selamat Hari Raya!" to
                "Today is $holidayName. Maaf zahir dan batin — enjoy the open houses, ketupat and rendang!"
        "aidiladha" in n || "haji" in n ->
            "Selamat Hari Raya Aidiladha!" to
                "Today is $holidayName. Wishing you a blessed celebration with family and friends."
        "christmas" in n || "xmas" in n ->
            "Merry Christmas!" to
                "Today is $holidayName. Wishing you a warm, joyful day with the people you love."
        "deepavali" in n || "diwali" in n ->
            "Happy Deepavali!" to
                "Today is $holidayName — the festival of lights. May it brighten your year ahead."
        "wesak" in n || "vesak" in n ->
            "Happy Wesak Day!" to
                "Today is $holidayName. Wishing you a day of peace and reflection."
        "thaipusam" in n ->
            "Happy Thaipusam!" to
                "Today is $holidayName. Have a blessed and meaningful day."
        "national day" in n || "kebangsaan" in n || "merdeka" in n || isAug31 ->
            "Selamat Hari Kebangsaan!" to
                "Today is Malaysia's National Day. Sambutlah hari kemerdekaan — wear your colours proud!"
        "malaysia day" in n || "hari malaysia" in n || isSep16 ->
            "Selamat Hari Malaysia!" to
                "Today we celebrate the formation of Malaysia. One nation, many cultures."
        "labour" in n || "labor" in n || "may day" in n ->
            "Happy Labour Day!" to
                "Today is $holidayName. Take the day off, you've earned it."
        "good friday" in n ->
            "Good Friday" to
                "Wishing you a peaceful, reflective day off."
        "new year" in n && "chinese" !in n && "lunar" !in n && "hijri" !in n && "muharram" !in n ->
            "Happy New Year!" to
                "Today is $holidayName — here's to a fresh start!"
        "awal muharram" in n || "maal hijrah" in n || "hijri" in n ->
            "Selamat Tahun Baru Hijrah!" to
                "Today is $holidayName. Wishing you a blessed new year."
        "prophet" in n || "mawlid" in n || "maulidur rasul" in n ->
            "Maulidur Rasul" to
                "Today is $holidayName. May it bring blessings and peace."
        "agong" in n || "yang di-pertuan agong" in n ->
            "Agong's birthday" to
                "$holidayName today — daulat tuanku and enjoy the public holiday!"
        "sultan" in n ->
            "Sultan's birthday" to
                "$holidayName — daulat tuanku and have a wonderful day off."
        else ->
            "Happy holiday!" to
                "Today is $holidayName. Enjoy your day off!"
    }
}
