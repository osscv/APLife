package net.dkly.aplife.data

import android.content.Context

class UserPreferences(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(
        "aplife_prefs", Context.MODE_PRIVATE,
    )

    var intakeCode: String?
        get() = prefs.getString(KEY_INTAKE, null)
        set(value) = prefs.edit().putString(KEY_INTAKE, value).apply()

    /** Legacy single-group field. Prefer [selectedGroups]. */
    var grouping: String?
        get() = prefs.getString(KEY_GROUP, null)
        set(value) = prefs.edit().putString(KEY_GROUP, value).apply()

    /** Multi-group selection. Persists as comma-separated list. */
    var selectedGroups: Set<String>
        get() = prefs.getString(KEY_GROUPS_MULTI, null)
            ?.split(',')
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?.toSet()
            ?: grouping?.let { setOf(it) }
            ?: emptySet()
        set(value) = prefs.edit()
            .putString(KEY_GROUPS_MULTI, value.joinToString(","))
            .apply()

    /** All calendars the user wants events written to. */
    var calendarIds: Set<Long>
        get() {
            val csv = prefs.getString(KEY_CALENDAR_IDS, null)
            if (csv != null) {
                return csv.split(',')
                    .mapNotNull { it.trim().toLongOrNull() }
                    .filter { it > 0 }
                    .toSet()
            }
            // Legacy single-calendar migration
            val single = prefs.getLong(KEY_CALENDAR_ID, -1L)
            return if (single > 0) setOf(single) else emptySet()
        }
        set(value) = prefs.edit()
            .putString(KEY_CALENDAR_IDS, value.joinToString(","))
            .apply()

    /** Reminder offsets in minutes-before-event for class events. */
    var classReminderMinutes: List<Int>
        get() = prefs.getString(KEY_CLASS_REMINDERS, null)
            ?.split(',')
            ?.mapNotNull { it.trim().toIntOrNull() }
            ?: DEFAULT_CLASS_REMINDERS
        set(value) = prefs.edit()
            .putString(KEY_CLASS_REMINDERS, value.joinToString(","))
            .apply()

    /** Reminder offsets in minutes-before-event for exams. */
    var examReminderMinutes: List<Int>
        get() = prefs.getString(KEY_EXAM_REMINDERS, null)
            ?.split(',')
            ?.mapNotNull { it.trim().toIntOrNull() }
            ?: DEFAULT_EXAM_REMINDERS
        set(value) = prefs.edit()
            .putString(KEY_EXAM_REMINDERS, value.joinToString(","))
            .apply()

    var syncHolidays: Boolean
        get() = prefs.getBoolean(KEY_SYNC_HOLIDAYS, true)
        set(value) = prefs.edit().putBoolean(KEY_SYNC_HOLIDAYS, value).apply()

    var onboardingComplete: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDED, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDED, value).apply()

    var autoSyncEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_SYNC, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_SYNC, value).apply()

    var lastSyncMs: Long
        get() = prefs.getLong(KEY_LAST_SYNC, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_SYNC, value).apply()

    private companion object {
        const val KEY_INTAKE = "intake_code"
        const val KEY_GROUP = "grouping"
        const val KEY_GROUPS_MULTI = "grouping_multi"
        const val KEY_CALENDAR_ID = "calendar_id"
        const val KEY_CALENDAR_IDS = "calendar_ids"
        const val KEY_CLASS_REMINDERS = "class_reminders_minutes"
        const val KEY_EXAM_REMINDERS = "exam_reminders_minutes"
        const val KEY_SYNC_HOLIDAYS = "sync_holidays"
        const val KEY_ONBOARDED = "onboarding_complete"
        const val KEY_AUTO_SYNC = "auto_sync_enabled"
        const val KEY_LAST_SYNC = "last_sync_ms"

        val DEFAULT_CLASS_REMINDERS = listOf(15) // 15 min — recommended default
        val DEFAULT_EXAM_REMINDERS = listOf(
            21 * 24 * 60, // 3 weeks
            14 * 24 * 60, // 2 weeks
            7 * 24 * 60,  // 1 week
            3 * 24 * 60,  // 3 days
            2 * 24 * 60,  // 2 days
            1 * 24 * 60,  // 1 day
            3 * 60,       // 3 hours
            2 * 60,       // 2 hours
            60,           // 1 hour
            30,           // 30 minutes
        )
    }
}
