package net.dkly.aplife.data

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class DepartmentRepository {

    private val mutex = Mutex()
    private var cached: List<Department>? = null
    private var cacheAt: Long = 0L

    suspend fun all(forceRefresh: Boolean = false): List<Department> {
        val current = cached
        if (!forceRefresh && current != null && (System.currentTimeMillis() - cacheAt) < CACHE_TTL_MS) {
            return current
        }
        return mutex.withLock {
            val again = cached
            if (!forceRefresh && again != null && (System.currentTimeMillis() - cacheAt) < CACHE_TTL_MS) {
                return@withLock again
            }
            val list = ApiClient.fetchQuixCustomers().flatMap { it.departments }
            cached = list
            cacheAt = System.currentTimeMillis()
            list
        }
    }

    /** Returns true if the department is currently open at [now]. */
    fun isOpen(department: Department, now: ZonedDateTime = ZonedDateTime.now(ZoneId.systemDefault())): Boolean {
        val time = now.toLocalTime()
        return shiftsForDay(department, now.dayOfWeek).any { time in it }
    }

    fun shiftsForDay(department: Department, day: DayOfWeek): List<ClosedTimeRange> {
        val key = when (day) {
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY -> "Mon-Thu"
            DayOfWeek.FRIDAY -> "Fri"
            DayOfWeek.SATURDAY -> "Sat"
            DayOfWeek.SUNDAY -> "Sun"
        }
        val shifts = department.shifts[key].orEmpty()
        return shifts.mapNotNull { it.toRange() }
    }

    fun shiftsForDate(department: Department, date: LocalDate): List<ClosedTimeRange> =
        shiftsForDay(department, date.dayOfWeek)

    private fun Shift.toRange(): ClosedTimeRange? = runCatching {
        ClosedTimeRange(parseTime(startTime), parseTime(endTime))
    }.getOrNull()

    private fun parseTime(value: String): LocalTime {
        val v = value.trim()
        return runCatching { LocalTime.parse(v) }
            .recoverCatching { LocalTime.parse(if (v.length == 5) "$v:00" else v) }
            .getOrThrow()
    }

    private companion object {
        const val CACHE_TTL_MS = 6L * 60L * 60L * 1000L // 6 hours
    }
}

data class ClosedTimeRange(val start: LocalTime, val end: LocalTime) {
    operator fun contains(time: LocalTime): Boolean = !time.isBefore(start) && time.isBefore(end)
    fun pretty(): String = "${start} – ${end}"
}
