package net.dkly.aplife.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class HolidayRepository(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val mutex = Mutex()
    private var cached: List<Holiday>? = null

    suspend fun all(): List<Holiday> {
        cached?.let { return it }
        return mutex.withLock {
            cached?.let { return it }
            val text = withContext(Dispatchers.IO) {
                context.assets.open("holidays.json").bufferedReader().use { it.readText() }
            }
            val sets: List<HolidaySetDto> = json.decodeFromString(text)
            val parsed = sets.asSequence()
                .filter { it.active }
                .flatMap { set ->
                    set.holidays.asSequence().map { h ->
                        Holiday(
                            name = h.name,
                            description = h.description,
                            startEpochMs = parseGmtDate(h.startDate),
                            endEpochMs = parseGmtDate(h.endDate),
                            peopleAffected = h.peopleAffected.orEmpty(),
                            year = set.year,
                        )
                    }
                }
                .sortedBy { it.startEpochMs }
                .toList()
            cached = parsed
            parsed
        }
    }

    suspend fun forYear(year: Int): List<Holiday> = all().filter { it.year == year }

    suspend fun forStudents(year: Int): List<Holiday> = forYear(year).filter {
        it.peopleAffected.equals("all", true) ||
            it.peopleAffected.equals("students", true) ||
            it.peopleAffected.isBlank()
    }

    private fun parseGmtDate(value: String): Long {
        // Format example: "Wed, 01 Jan 2026 00:00:00 GMT"
        val fmt = DateTimeFormatter.RFC_1123_DATE_TIME.withLocale(Locale.ENGLISH)
        val zdt = ZonedDateTime.parse(value, fmt)
        // Convert wall date in GMT to local-day midnight so all-day events line up
        val localDate = zdt.withZoneSameInstant(ZoneId.of("UTC")).toLocalDate()
        return localDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}
