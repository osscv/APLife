package net.dkly.aplife.calendar

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import net.dkly.aplife.data.ExamEntry
import net.dkly.aplife.data.Holiday
import net.dkly.aplife.data.PersonalEvent
import net.dkly.aplife.data.TimetableEntry
import java.time.OffsetDateTime
import java.util.TimeZone

data class DeviceCalendar(
    val id: Long,
    val displayName: String,
    val accountName: String,
)

/**
 * @param inserted  number of new events created
 * @param updated   existing APLife-managed events whose title / time / venue / etc. were refreshed
 */
data class SyncResult(
    val inserted: Int,
    val updated: Int,
)

class CalendarSyncManager(private val context: Context) {

    fun listWritableCalendars(): List<DeviceCalendar> {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
        )
        val selection = "${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ?"
        val args = arrayOf(CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR.toString())
        val result = mutableListOf<DeviceCalendar>()
        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection, selection, args, null,
        )?.use { c ->
            val idIdx = c.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
            val nameIdx = c.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
            val acctIdx = c.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_NAME)
            while (c.moveToNext()) {
                result += DeviceCalendar(
                    id = c.getLong(idIdx),
                    displayName = c.getString(nameIdx).orEmpty(),
                    accountName = c.getString(acctIdx).orEmpty(),
                )
            }
        }
        return result
    }

    // ---- Sync entry points -------------------------------------------------

    fun syncTimetable(
        calendarId: Long,
        entries: List<TimetableEntry>,
        reminderOffsetsMinutes: List<Int>,
    ): SyncResult {
        var inserted = 0
        var updated = 0
        for (entry in entries) {
            val start = OffsetDateTime.parse(entry.timeFromIso).toInstant().toEpochMilli()
            val end = OffsetDateTime.parse(entry.timeToIso).toInstant().toEpochMilli()
            val title = entry.moduleName
            val description = buildString {
                entry.moduleId?.let { append("Module: ").append(it).append('\n') }
                entry.lecturerName?.let { append("Lecturer: ").append(it).append('\n') }
                entry.grouping?.let { append("Group: ").append(it) }
            }.trim()
            val location = buildLocation(entry.room, entry.location)
            val key = entry.classCode ?: "${entry.moduleId.orEmpty()}|${entry.timeFromIso}"
            val (_, wasInserted) = upsert(
                calendarId = calendarId,
                title = title,
                description = description,
                location = location,
                startMs = start,
                endMs = end,
                allDay = false,
                markerType = "CLASS",
                markerKey = key,
                reminderOffsetsMinutes = reminderOffsetsMinutes,
            )
            if (wasInserted) inserted++ else updated++
        }
        return SyncResult(inserted, updated)
    }

    fun syncExams(
        calendarId: Long,
        exams: List<ExamEntry>,
        reminderOffsetsMinutes: List<Int>,
    ): SyncResult {
        var inserted = 0
        var updated = 0
        for (exam in exams) {
            val start = OffsetDateTime.parse(exam.since).toInstant().toEpochMilli()
            val end = OffsetDateTime.parse(exam.until).toInstant().toEpochMilli()
            val title = "Exam: ${exam.subjectDescription}"
            val description = buildString {
                append("Module: ").append(exam.module).append('\n')
                exam.assessmentType?.let { append("Assessment: ").append(it).append('\n') }
                exam.examType?.let { append("Type: ").append(it).append('\n') }
                exam.resultDate?.let { append("Result Date: ").append(it) }
            }.trim()
            val key = "${exam.intake}|${exam.module}|${exam.since}"
            val (_, wasInserted) = upsert(
                calendarId = calendarId,
                title = title,
                description = description,
                location = exam.venue,
                startMs = start,
                endMs = end,
                allDay = false,
                markerType = "EXAM",
                markerKey = key,
                reminderOffsetsMinutes = reminderOffsetsMinutes,
            )
            if (wasInserted) inserted++ else updated++
        }
        return SyncResult(inserted, updated)
    }

    fun syncHolidays(calendarId: Long, holidays: List<Holiday>): SyncResult {
        var inserted = 0
        var updated = 0
        for (h in holidays) {
            val title = "APU Holiday: ${h.name}"
            val startUtc = h.startEpochMs.toUtcDayStart()
            val endUtc = (h.endEpochMs.toUtcDayStart() + DAY_MS)
            val key = "${h.name}|${h.year}"
            val (_, wasInserted) = upsert(
                calendarId = calendarId,
                title = title,
                description = h.description,
                location = null,
                startMs = startUtc,
                endMs = endUtc,
                allDay = true,
                markerType = "HOLIDAY",
                markerKey = key,
                reminderOffsetsMinutes = emptyList(),
            )
            if (wasInserted) inserted++ else updated++
        }
        return SyncResult(inserted, updated)
    }

    fun syncPersonalEvents(
        calendarId: Long,
        events: List<PersonalEvent>,
        reminderOffsetsMinutes: List<Int>,
    ): SyncResult {
        var inserted = 0
        var updated = 0
        for (e in events) {
            val title = "${e.type.label}: ${e.title}"
            val description = buildString {
                if (e.notes.isNotBlank()) append(e.notes)
                if (!e.lecturerName.isNullOrBlank()) {
                    if (isNotEmpty()) append("\n\n")
                    append("With: ").append(e.lecturerName)
                    if (!e.lecturerCode.isNullOrBlank()) append(" (").append(e.lecturerCode).append(")")
                }
            }
            val key = e.id.toString()
            val (_, wasInserted) = upsert(
                calendarId = calendarId,
                title = title,
                description = description.takeIf { it.isNotBlank() },
                location = e.location,
                startMs = e.startMs,
                endMs = e.endMs,
                allDay = false,
                markerType = "PEVENT",
                markerKey = key,
                reminderOffsetsMinutes = reminderOffsetsMinutes,
            )
            if (wasInserted) inserted++ else updated++
        }
        return SyncResult(inserted, updated)
    }

    /**
     * Deletes every event APLife has written to [calendarIds] (events with an "[APLife:" marker
     * in their description). Returns the total number of rows deleted.
     */
    fun deleteAllAplifeEvents(calendarIds: Set<Long>): Int {
        if (calendarIds.isEmpty()) return 0
        var total = 0
        for (calendarId in calendarIds) {
            val n = context.contentResolver.delete(
                CalendarContract.Events.CONTENT_URI,
                "${CalendarContract.Events.CALENDAR_ID} = ? AND " +
                    "${CalendarContract.Events.DESCRIPTION} LIKE ?",
                arrayOf(calendarId.toString(), "%[APLife:%"),
            )
            total += n
        }
        return total
    }

    /** Used by the Lecturer appointment flow — single-shot, no marker since these are user-initiated. */
    fun insertAppointment(
        calendarId: Long,
        title: String,
        description: String?,
        location: String?,
        startMs: Long,
        endMs: Long,
        reminderOffsetsMinutes: List<Int>,
    ): Long {
        val id = insertEventRaw(calendarId, title, description, location, startMs, endMs, allDay = false)
        if (id > 0) attachReminders(id, reminderOffsetsMinutes)
        return id
    }

    // ---- Internals ---------------------------------------------------------

    /** Find existing APLife-managed event by marker, or insert new. Always re-applies reminders. */
    private fun upsert(
        calendarId: Long,
        title: String,
        description: String?,
        location: String?,
        startMs: Long,
        endMs: Long,
        allDay: Boolean,
        markerType: String,
        markerKey: String,
        reminderOffsetsMinutes: List<Int>,
    ): Pair<Long, Boolean> {
        val markerLine = buildMarkerLine(markerType, markerKey)
        val finalDescription = composeDescription(description, markerLine)

        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, title)
            put(CalendarContract.Events.DTSTART, startMs)
            put(CalendarContract.Events.DTEND, endMs)
            if (allDay) {
                put(CalendarContract.Events.ALL_DAY, 1)
                put(CalendarContract.Events.EVENT_TIMEZONE, "UTC")
            } else {
                put(CalendarContract.Events.ALL_DAY, 0)
                put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            }
            put(CalendarContract.Events.DESCRIPTION, finalDescription)
            if (!location.isNullOrBlank()) put(CalendarContract.Events.EVENT_LOCATION, location)
            else putNull(CalendarContract.Events.EVENT_LOCATION)
            put(CalendarContract.Events.HAS_ALARM, if (reminderOffsetsMinutes.isNotEmpty()) 1 else 0)
        }

        val existingId = findEventIdByMarker(calendarId, markerLine)
        if (existingId > 0) {
            val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, existingId)
            context.contentResolver.update(uri, values, null, null)
            // Reset reminders so the latest preference applies
            deleteRemindersFor(existingId)
            if (reminderOffsetsMinutes.isNotEmpty()) attachReminders(existingId, reminderOffsetsMinutes)
            return existingId to false
        }

        val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
        val newId = uri?.let(ContentUris::parseId) ?: -1L
        if (newId > 0 && reminderOffsetsMinutes.isNotEmpty()) attachReminders(newId, reminderOffsetsMinutes)
        return newId to true
    }

    /**
     * Find any existing APLife-tagged event for [calendarId] whose description contains [markerLine].
     * Falls back to title+start match if no marker is present (legacy events from older app builds).
     */
    private fun findEventIdByMarker(calendarId: Long, markerLine: String): Long {
        val projection = arrayOf(CalendarContract.Events._ID)
        val selection =
            "${CalendarContract.Events.CALENDAR_ID} = ? AND " +
                "${CalendarContract.Events.DESCRIPTION} LIKE ? AND " +
                "${CalendarContract.Events.DELETED} = 0"
        val likePattern = "%${markerLine.trim()}%"
        val args = arrayOf(calendarId.toString(), likePattern)
        context.contentResolver.query(
            CalendarContract.Events.CONTENT_URI, projection, selection, args, null,
        )?.use { c ->
            if (c.moveToFirst()) return c.getLong(0)
        }
        return -1L
    }

    private fun insertEventRaw(
        calendarId: Long,
        title: String,
        description: String?,
        location: String?,
        startMs: Long,
        endMs: Long,
        allDay: Boolean,
    ): Long {
        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, title)
            put(CalendarContract.Events.DTSTART, startMs)
            put(CalendarContract.Events.DTEND, endMs)
            if (allDay) {
                put(CalendarContract.Events.ALL_DAY, 1)
                put(CalendarContract.Events.EVENT_TIMEZONE, "UTC")
            } else {
                put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            }
            description?.takeIf(String::isNotBlank)?.let { put(CalendarContract.Events.DESCRIPTION, it) }
            location?.takeIf(String::isNotBlank)?.let { put(CalendarContract.Events.EVENT_LOCATION, it) }
            put(CalendarContract.Events.HAS_ALARM, 1)
        }
        val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
        return uri?.let(ContentUris::parseId) ?: -1L
    }

    private fun deleteRemindersFor(eventId: Long) {
        context.contentResolver.delete(
            CalendarContract.Reminders.CONTENT_URI,
            "${CalendarContract.Reminders.EVENT_ID} = ?",
            arrayOf(eventId.toString()),
        )
    }

    private fun attachReminders(eventId: Long, offsetsMinutes: List<Int>) {
        for (minutes in offsetsMinutes.distinct().filter { it > 0 }) {
            val values = ContentValues().apply {
                put(CalendarContract.Reminders.EVENT_ID, eventId)
                put(CalendarContract.Reminders.MINUTES, minutes)
                put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
            }
            context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, values)
        }
    }

    private fun buildLocation(room: String?, campus: String?): String? {
        val parts = listOfNotNull(room?.takeIf(String::isNotBlank), campus?.takeIf(String::isNotBlank))
        return parts.joinToString(", ").ifBlank { null }
    }

    private fun buildMarkerLine(type: String, key: String): String =
        "[APLife:$type:${key.replace('|', ':')}]"

    private fun composeDescription(userDescription: String?, markerLine: String): String {
        val body = userDescription?.trim().orEmpty()
        return if (body.isBlank()) markerLine else "$body\n\n$markerLine"
    }

    private fun Long.toUtcDayStart(): Long {
        val instant = java.time.Instant.ofEpochMilli(this)
        val date = instant.atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        return date.atStartOfDay(java.time.ZoneId.of("UTC")).toInstant().toEpochMilli()
    }

    private companion object {
        const val DAY_MS = 24L * 60 * 60 * 1000
    }
}
