package net.dkly.aplife.data

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

class TransportRepository {

    private val mutex = Mutex()
    private var locationsCache: List<TransportLocation>? = null
    private var scheduleCache: TransportSchedule? = null
    private var fetchedAt: Long = 0L

    suspend fun load(forceRefresh: Boolean = false): TransportData {
        val locs = locationsCache
        val sched = scheduleCache
        if (!forceRefresh && locs != null && sched != null &&
            (System.currentTimeMillis() - fetchedAt) < CACHE_TTL_MS) {
            return TransportData(locs, sched)
        }
        return mutex.withLock {
            val locs2 = locationsCache
            val sched2 = scheduleCache
            if (!forceRefresh && locs2 != null && sched2 != null &&
                (System.currentTimeMillis() - fetchedAt) < CACHE_TTL_MS) {
                return@withLock TransportData(locs2, sched2)
            }
            val locations = ApiClient.fetchTransportLocations()
            val schedule = ApiClient.fetchActiveTransportSchedule()
            locationsCache = locations
            scheduleCache = schedule
            fetchedAt = System.currentTimeMillis()
            TransportData(locations, schedule)
        }
    }

    /** Returns trips that apply to [date], ordered by absolute time, projected onto [date]. */
    fun tripsForDate(schedule: TransportSchedule, date: LocalDate): List<ScheduledTrip> {
        val zone = ZoneId.systemDefault()
        return schedule.trips.asSequence()
            .filter { matchesDay(it.day, date.dayOfWeek) }
            .mapNotNull { trip ->
                val parsed = runCatching { OffsetDateTime.parse(trip.time) }.getOrNull() ?: return@mapNotNull null
                val time = parsed.atZoneSameInstant(zone).toLocalTime()
                ScheduledTrip(
                    trip = trip,
                    departure = ZonedDateTime.of(date, time, zone),
                )
            }
            .sortedBy { it.departure }
            .toList()
    }

    fun nextTrips(schedule: TransportSchedule, now: ZonedDateTime, count: Int = 6): List<ScheduledTrip> {
        val today = tripsForDate(schedule, now.toLocalDate()).filter { it.departure.isAfter(now) }
        if (today.size >= count) return today.take(count)
        val tomorrow = tripsForDate(schedule, now.toLocalDate().plusDays(1))
        return (today + tomorrow).take(count)
    }

    private fun matchesDay(day: String, weekday: DayOfWeek): Boolean {
        val d = day.lowercase()
        return when {
            d.contains("mon") && d.contains("fri") -> weekday in DayOfWeek.MONDAY..DayOfWeek.FRIDAY
            d.contains("friday") -> weekday == DayOfWeek.FRIDAY
            d.contains("saturday") || d == "sat" -> weekday == DayOfWeek.SATURDAY
            d.contains("sunday") || d == "sun" -> weekday == DayOfWeek.SUNDAY
            d.contains("daily") || d.contains("everyday") -> true
            else -> false
        }
    }

    private companion object {
        const val CACHE_TTL_MS = 30L * 60L * 1000L // 30 minutes
    }
}

data class TransportData(
    val locations: List<TransportLocation>,
    val schedule: TransportSchedule,
)

data class ScheduledTrip(
    val trip: TransportTrip,
    val departure: ZonedDateTime,
)
