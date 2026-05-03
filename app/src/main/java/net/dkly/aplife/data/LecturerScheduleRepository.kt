package net.dkly.aplife.data

import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId

class LecturerScheduleRepository {

    suspend fun fetch(samAccount: String): List<LecturerScheduleEntry> =
        ApiClient.fetchLecturerSchedule(samAccount)

    /**
     * Compute free slots for [date] within [windowStart]..[windowEnd] (local time)
     * given the lecturer's busy entries. Slots shorter than [minMinutes] are dropped.
     */
    fun freeSlotsForDay(
        entries: List<LecturerScheduleEntry>,
        date: LocalDate,
        windowStart: LocalTime = LocalTime.of(9, 0),
        windowEnd: LocalTime = LocalTime.of(18, 0),
        minMinutes: Int = 15,
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<FreeSlot> {
        val sameDay = entries
            .mapNotNull { entry ->
                val start = runCatching { OffsetDateTime.parse(entry.time) }.getOrNull() ?: return@mapNotNull null
                val end = start.plusSeconds(entry.duration.toLong())
                val startLocal = start.atZoneSameInstant(zone).toLocalDateTime()
                val endLocal = end.atZoneSameInstant(zone).toLocalDateTime()
                if (startLocal.toLocalDate() != date) null
                else BusyBlock(startLocal.toLocalTime(), endLocal.toLocalTime())
            }
            .sortedBy { it.start }

        val free = mutableListOf<FreeSlot>()
        var cursor = windowStart
        for (block in sameDay) {
            val blockStart = if (block.start.isBefore(windowStart)) windowStart else block.start
            val blockEnd = if (block.end.isAfter(windowEnd)) windowEnd else block.end
            if (blockStart.isAfter(cursor)) {
                free += FreeSlot(date, cursor, blockStart)
            }
            if (blockEnd.isAfter(cursor)) cursor = blockEnd
            if (!cursor.isBefore(windowEnd)) break
        }
        if (cursor.isBefore(windowEnd)) free += FreeSlot(date, cursor, windowEnd)
        return free.filter { it.minutes() >= minMinutes }
    }
}

data class BusyBlock(val start: LocalTime, val end: LocalTime)

data class FreeSlot(val date: LocalDate, val start: LocalTime, val end: LocalTime) {
    fun minutes(): Long = java.time.Duration.between(start, end).toMinutes()
    fun pretty(): String = "$start – $end (${minutes()} min)"
}
