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

data class SyncResult(
    val inserted: Int,
    val skipped: Int,
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

    fun syncTimetable(
        calendarId: Long,
        entries: List<TimetableEntry>,
        reminderOffsetsMinutes: List<Int>,
    ): SyncResult {
        var inserted = 0
        var skipped = 0
        for (entry in entries) {
            val start = OffsetDateTime.parse(entry.timeFromIso).toInstant().toEpochMilli()
            val end = OffsetDateTime.parse(entry.timeToIso).toInstant().toEpochMilli()
            val title = entry.moduleName
            val description = buildString {
                entry.moduleId?.let { append("Module: ").append(it).append('\n') }
                entry.lecturerName?.let { append("Lecturer: ").append(it).append('\n') }
                entry.grouping?.let { append("Group: ").append(it).append('\n') }
                entry.classCode?.let { append("Ref: ").append(it) }
            }.trim()
            val location = buildLocation(entry.room, entry.location)

            if (eventExists(calendarId, title, start)) {
                skipped++
                continue
            }
            val eventId = insertEvent(calendarId, title, description, location, start, end, allDay = false)
            if (eventId > 0) attachReminders(eventId, reminderOffsetsMinutes)
            inserted++
        }
        return SyncResult(inserted, skipped)
    }

    fun syncExams(
        calendarId: Long,
        exams: List<ExamEntry>,
        reminderOffsetsMinutes: List<Int>,
    ): SyncResult {
        var inserted = 0
        var skipped = 0
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

            if (eventExists(calendarId, title, start)) {
                skipped++
                continue
            }
            val eventId = insertEvent(calendarId, title, description, exam.venue, start, end, allDay = false)
            if (eventId > 0) attachReminders(eventId, reminderOffsetsMinutes)
            inserted++
        }
        return SyncResult(inserted, skipped)
    }

    fun syncHolidays(calendarId: Long, holidays: List<Holiday>): SyncResult {
        var inserted = 0
        var skipped = 0
        for (h in holidays) {
            val title = "APU Holiday: ${h.name}"
            // Calendar Provider expects all-day events in UTC midnight bounds
            val startUtc = h.startEpochMs.toUtcDayStart()
            val endUtc = (h.endEpochMs.toUtcDayStart() + DAY_MS)
            if (eventExists(calendarId, title, startUtc)) {
                skipped++
                continue
            }
            insertEvent(
                calendarId = calendarId,
                title = title,
                description = h.description,
                location = null,
                startMs = startUtc,
                endMs = endUtc,
                allDay = true,
            )
            inserted++
        }
        return SyncResult(inserted, skipped)
    }

    fun syncPersonalEvents(
        calendarId: Long,
        events: List<PersonalEvent>,
        reminderOffsetsMinutes: List<Int>,
    ): SyncResult {
        var inserted = 0
        var skipped = 0
        for (e in events) {
            val title = "${e.type.label}: ${e.title}"
            if (eventExists(calendarId, title, e.startMs)) {
                skipped++
                continue
            }
            val description = buildString {
                if (e.notes.isNotBlank()) append(e.notes)
                if (!e.lecturerName.isNullOrBlank()) {
                    if (isNotEmpty()) append("\n\n")
                    append("With: ").append(e.lecturerName)
                    if (!e.lecturerCode.isNullOrBlank()) append(" (").append(e.lecturerCode).append(")")
                }
            }
            val id = insertEvent(
                calendarId = calendarId,
                title = title,
                description = description.takeIf { it.isNotBlank() },
                location = e.location,
                startMs = e.startMs,
                endMs = e.endMs,
                allDay = false,
            )
            if (id > 0) attachReminders(id, reminderOffsetsMinutes)
            inserted++
        }
        return SyncResult(inserted, skipped)
    }

    fun insertAppointment(
        calendarId: Long,
        title: String,
        description: String?,
        location: String?,
        startMs: Long,
        endMs: Long,
        reminderOffsetsMinutes: List<Int>,
    ): Long {
        if (eventExists(calendarId, title, startMs)) return -1L
        val id = insertEvent(calendarId, title, description, location, startMs, endMs, allDay = false)
        if (id > 0) attachReminders(id, reminderOffsetsMinutes)
        return id
    }

    private fun buildLocation(room: String?, campus: String?): String? {
        val parts = listOfNotNull(room?.takeIf(String::isNotBlank), campus?.takeIf(String::isNotBlank))
        return parts.joinToString(", ").ifBlank { null }
    }

    private fun insertEvent(
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

    private fun eventExists(calendarId: Long, title: String, startMs: Long): Boolean {
        val projection = arrayOf(CalendarContract.Events._ID)
        val selection =
            "${CalendarContract.Events.CALENDAR_ID} = ? AND " +
                "${CalendarContract.Events.TITLE} = ? AND " +
                "${CalendarContract.Events.DTSTART} = ? AND " +
                "${CalendarContract.Events.DELETED} = 0"
        val args = arrayOf(calendarId.toString(), title, startMs.toString())
        context.contentResolver.query(
            CalendarContract.Events.CONTENT_URI, projection, selection, args, null,
        )?.use { c ->
            return c.moveToFirst()
        }
        return false
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
