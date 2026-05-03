package net.dkly.aplife.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.dkly.aplife.calendar.CalendarSyncManager
import net.dkly.aplife.calendar.DeviceCalendar
import net.dkly.aplife.data.ClassOverride
import net.dkly.aplife.data.ClassOverrideRepository
import net.dkly.aplife.data.Department
import net.dkly.aplife.data.DepartmentRepository
import net.dkly.aplife.data.ExamEntry
import net.dkly.aplife.data.FreeSlot
import net.dkly.aplife.data.Holiday
import net.dkly.aplife.data.HolidayRepository
import net.dkly.aplife.data.Lecturer
import net.dkly.aplife.data.LecturerScheduleEntry
import net.dkly.aplife.data.LecturerScheduleRepository
import net.dkly.aplife.data.PersonalEvent
import net.dkly.aplife.data.PersonalEventRepository
import net.dkly.aplife.data.StaffRepository
import net.dkly.aplife.data.ScheduledTrip
import net.dkly.aplife.data.TimetableEntry
import net.dkly.aplife.data.TimetableRepository
import net.dkly.aplife.data.TransportLocation
import net.dkly.aplife.data.TransportRepository
import net.dkly.aplife.data.UserPreferences
import net.dkly.aplife.notes.Note
import net.dkly.aplife.notes.NoteRepository
import net.dkly.aplife.notes.NoteType
import net.dkly.aplife.sync.AutoSyncWorker
import net.dkly.aplife.sync.ExamLuckScheduler
import net.dkly.aplife.sync.HolidayReminderScheduler
import net.dkly.aplife.sync.SyncNotifier
import net.dkly.aplife.sync.SyncStats
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId

data class ScheduleState(
    val intakeCode: String = "",
    val selectedGroups: Set<String> = emptySet(),
    val availableGroups: List<String> = emptyList(),
    val intakeEntries: List<TimetableEntry> = emptyList(),
    val filteredEntries: List<TimetableEntry> = emptyList(),
    val exams: List<ExamEntry> = emptyList(),
    val holidays: List<Holiday> = emptyList(),
    val calendars: List<DeviceCalendar> = emptyList(),
    val calendarsLoaded: Boolean = false,
    val isLoadingCalendars: Boolean = false,
    val calendarLoadError: String? = null,
    val selectedCalendarIds: Set<Long> = emptySet(),
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val message: String? = null,
    val error: String? = null,
    val onboardingComplete: Boolean = false,
    val autoSyncEnabled: Boolean = true,
)

data class LecturerState(
    val query: String = "",
    val results: List<Lecturer> = emptyList(),
    val selected: Lecturer? = null,
    val schedule: List<LecturerScheduleEntry> = emptyList(),
    val appointmentDate: LocalDate = LocalDate.now(),
    val freeSlots: List<FreeSlot> = emptyList(),
    val isLoadingSchedule: Boolean = false,
    val error: String? = null,
)

data class DepartmentState(
    val items: List<Department> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

data class SettingsState(
    val classReminderMinutes: List<Int> = emptyList(),
    val examReminderMinutes: List<Int> = emptyList(),
    val syncHolidays: Boolean = true,
)

data class TransportState(
    val locations: List<TransportLocation> = emptyList(),
    val nextTrips: List<ScheduledTrip> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val timetableRepo = TimetableRepository()
    private val staffRepo = StaffRepository(app)
    private val holidayRepo = HolidayRepository(app)
    private val lecturerScheduleRepo = LecturerScheduleRepository()
    private val departmentRepo = DepartmentRepository()
    private val transportRepo = TransportRepository()
    private val overrideRepo = ClassOverrideRepository(app)
    private val personalEventRepo = PersonalEventRepository(app)
    private val noteRepo = NoteRepository(app)
    private val prefs = UserPreferences(app)
    private val calendarSync = CalendarSyncManager(app)
    private val syncNotifier = SyncNotifier(app)
    private val examLuckScheduler = ExamLuckScheduler(app)
    private val holidayReminderScheduler = HolidayReminderScheduler(app)

    private val _schedule = MutableStateFlow(ScheduleState())
    val schedule: StateFlow<ScheduleState> = _schedule.asStateFlow()

    private val _lecturer = MutableStateFlow(LecturerState())
    val lecturer: StateFlow<LecturerState> = _lecturer.asStateFlow()

    private val _departments = MutableStateFlow(DepartmentState())
    val departments: StateFlow<DepartmentState> = _departments.asStateFlow()

    private val _transport = MutableStateFlow(TransportState())
    val transport: StateFlow<TransportState> = _transport.asStateFlow()

    private val _settings = MutableStateFlow(
        SettingsState(
            classReminderMinutes = prefs.classReminderMinutes,
            examReminderMinutes = prefs.examReminderMinutes,
            syncHolidays = prefs.syncHolidays,
        )
    )
    val settings: StateFlow<SettingsState> = _settings.asStateFlow()

    /** Cache of all distinct intake codes from the timetable for autocomplete. */
    private val _allIntakes = MutableStateFlow<List<String>>(emptyList())
    val allIntakes: StateFlow<List<String>> = _allIntakes.asStateFlow()
    val intakeSuggestions: StateFlow<List<String>> = _intakeSuggestionsState()

    private fun _intakeSuggestionsState(): StateFlow<List<String>> =
        combine(_allIntakes, _schedule) { all, sched ->
            val q = sched.intakeCode.trim().uppercase()
            if (all.isEmpty() || q.isBlank()) emptyList()
            else {
                val starts = all.filter { it.startsWith(q) }
                val contains = all.filter { it.contains(q) && !it.startsWith(q) }
                (starts + contains).take(10)
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val notes: StateFlow<List<Note>> = noteRepo.all()
    val personalEvents: StateFlow<List<PersonalEvent>> = personalEventRepo.events

    init {
        _schedule.update {
            it.copy(
                intakeCode = prefs.intakeCode.orEmpty(),
                selectedGroups = prefs.selectedGroups,
                selectedCalendarIds = prefs.calendarIds,
                onboardingComplete = prefs.onboardingComplete,
                autoSyncEnabled = prefs.autoSyncEnabled,
            )
        }
        prefetchIntakes()
        if (prefs.onboardingComplete && !prefs.intakeCode.isNullOrBlank()) {
            // Auto-load on app start so the saved timetable shows immediately.
            loadIntake(forceRefresh = false)
        }
    }

    private fun prefetchIntakes() {
        viewModelScope.launch {
            runCatching { timetableRepo.distinctIntakes() }
                .onSuccess { _allIntakes.value = it }
        }
    }

    // --- Schedule -------------------------------------------------------- //

    fun onIntakeChange(value: String) {
        _schedule.update { it.copy(intakeCode = value, error = null, message = null) }
    }

    fun pickIntakeSuggestion(code: String) {
        _schedule.update { it.copy(intakeCode = code, error = null, message = null) }
        loadIntake(forceRefresh = false)
    }

    fun toggleGroup(group: String) {
        val current = _schedule.value.selectedGroups
        val next = if (group in current) current - group else current + group
        applyGroupSelection(next)
    }

    fun setGroups(groups: Set<String>) = applyGroupSelection(groups)

    private fun applyGroupSelection(groups: Set<String>) {
        _schedule.update { current ->
            current.copy(
                selectedGroups = groups,
                filteredEntries = filterByGroups(current.intakeEntries, groups),
            )
        }
        prefs.selectedGroups = groups
    }

    private fun filterByGroups(entries: List<TimetableEntry>, groups: Set<String>): List<TimetableEntry> {
        val applied = entries.map(overrideRepo::apply)
        if (groups.isEmpty()) return applied
        val lower = groups.map { it.lowercase() }.toSet()
        return applied.filter { it.grouping.isNullOrBlank() || it.grouping.lowercase() in lower }
    }

    fun setClassOverride(override: ClassOverride) {
        overrideRepo.put(override)
        // Recompute filteredEntries with new overrides
        val current = _schedule.value
        _schedule.update {
            it.copy(filteredEntries = filterByGroups(current.intakeEntries, current.selectedGroups))
        }
    }

    fun clearClassOverride(classCode: String) {
        overrideRepo.clear(classCode)
        val current = _schedule.value
        _schedule.update {
            it.copy(filteredEntries = filterByGroups(current.intakeEntries, current.selectedGroups))
        }
    }

    fun savePersonalEvent(event: PersonalEvent) {
        val saved = event.copy(id = personalEventRepo.upsert(event))
        // Best-effort: write to selected calendars immediately so it appears outside the app too.
        viewModelScope.launch {
            val ids = _schedule.value.selectedCalendarIds
            if (ids.isNotEmpty()) {
                withContext(Dispatchers.IO) {
                    for (calId in ids) {
                        calendarSync.syncPersonalEvents(
                            calId,
                            listOf(saved),
                            _settings.value.classReminderMinutes,
                        )
                    }
                }
            }
        }
    }

    fun deletePersonalEvent(event: PersonalEvent) = personalEventRepo.delete(event)

    fun toggleCalendar(id: Long) {
        val current = _schedule.value.selectedCalendarIds
        val next = if (id in current) current - id else current + id
        applyCalendarSelection(next)
    }

    fun setCalendars(ids: Set<Long>) = applyCalendarSelection(ids)

    private fun applyCalendarSelection(ids: Set<Long>) {
        _schedule.update { it.copy(selectedCalendarIds = ids) }
        prefs.calendarIds = ids
    }

    fun loadIntake(forceRefresh: Boolean = false) {
        val code = _schedule.value.intakeCode.trim().uppercase()
        if (code.isBlank()) {
            _schedule.update { it.copy(error = "Enter an intake code first.") }
            return
        }
        prefs.intakeCode = code
        viewModelScope.launch {
            _schedule.update { it.copy(isLoading = true, error = null, message = null) }
            try {
                val entries = timetableRepo.loadForIntake(code, forceRefresh)
                if (entries.isEmpty()) {
                    _schedule.update {
                        it.copy(
                            isLoading = false,
                            intakeEntries = emptyList(),
                            filteredEntries = emptyList(),
                            availableGroups = emptyList(),
                            exams = emptyList(),
                            error = "No classes found for intake \"$code\".",
                        )
                    }
                    return@launch
                }
                val groups = timetableRepo.availableGroups(entries)
                val saved = _schedule.value.selectedGroups
                val resolved = saved.intersect(groups.toSet())
                val filtered = filterByGroups(entries, resolved)
                _schedule.update {
                    it.copy(
                        isLoading = false,
                        intakeEntries = entries,
                        availableGroups = groups,
                        selectedGroups = resolved,
                        filteredEntries = filtered,
                        message = if (resolved.isEmpty()) "Loaded ${entries.size} classes."
                            else "Loaded ${entries.size} classes (${resolved.size} group(s) selected).",
                    )
                }
                prefs.selectedGroups = resolved
                loadExams(code)
                loadHolidays()
            } catch (t: Throwable) {
                _schedule.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load timetable: ${t.message ?: t::class.simpleName}",
                    )
                }
            }
        }
    }

    private fun loadExams(intakeCode: String) {
        viewModelScope.launch {
            try {
                val exams = timetableRepo.fetchExams(intakeCode)
                _schedule.update { it.copy(exams = exams) }
            } catch (_: Throwable) {
                _schedule.update { it.copy(exams = emptyList()) }
            }
        }
    }

    private fun loadHolidays() {
        viewModelScope.launch {
            val year = LocalDate.now().year
            val list = runCatching { holidayRepo.forStudents(year) }.getOrDefault(emptyList())
            _schedule.update { it.copy(holidays = list) }
        }
    }

    fun loadCalendars() {
        if (_schedule.value.isLoadingCalendars) return
        viewModelScope.launch {
            _schedule.update { it.copy(isLoadingCalendars = true, calendarLoadError = null) }
            val result = runCatching {
                withContext(Dispatchers.IO) { calendarSync.listWritableCalendars() }
            }
            val calendars = result.getOrDefault(emptyList())
            val available = calendars.map { it.id }.toSet()
            val savedIds = _schedule.value.selectedCalendarIds
            val resolved = savedIds.intersect(available).ifEmpty {
                calendars.firstOrNull()?.id?.let { setOf(it) } ?: emptySet()
            }
            _schedule.update {
                it.copy(
                    calendars = calendars,
                    calendarsLoaded = true,
                    isLoadingCalendars = false,
                    calendarLoadError = result.exceptionOrNull()?.message,
                    selectedCalendarIds = resolved,
                )
            }
            prefs.calendarIds = resolved
        }
    }

    fun syncToCalendar(includeExams: Boolean) {
        val s = _schedule.value
        val calendarIds = s.selectedCalendarIds
        if (calendarIds.isEmpty()) {
            _schedule.update { it.copy(error = "Pick at least one calendar before syncing.") }
            return
        }
        val classes = s.filteredEntries
        val exams = if (includeExams) s.exams else emptyList()
        val holidays = if (_settings.value.syncHolidays) s.holidays else emptyList()
        if (classes.isEmpty() && exams.isEmpty() && holidays.isEmpty()) {
            _schedule.update { it.copy(error = "Nothing to sync — load an intake first.") }
            return
        }
        val classOffsets = _settings.value.classReminderMinutes
        val examOffsets = _settings.value.examReminderMinutes
        viewModelScope.launch {
            _schedule.update { it.copy(isSyncing = true, error = null, message = null) }
            try {
                var classesAdded = 0; var classesUpdated = 0
                var examsAdded = 0; var examsUpdated = 0
                var holidaysAdded = 0; var holidaysUpdated = 0
                var eventsAdded = 0; var eventsUpdated = 0
                val personal = personalEventRepo.events.value
                withContext(Dispatchers.IO) {
                    for (id in calendarIds) {
                        val c = calendarSync.syncTimetable(id, classes, classOffsets)
                        classesAdded += c.inserted; classesUpdated += c.updated
                        val e = calendarSync.syncExams(id, exams, examOffsets)
                        examsAdded += e.inserted; examsUpdated += e.updated
                        val h = calendarSync.syncHolidays(id, holidays)
                        holidaysAdded += h.inserted; holidaysUpdated += h.updated
                        val pe = calendarSync.syncPersonalEvents(id, personal, classOffsets)
                        eventsAdded += pe.inserted; eventsUpdated += pe.updated
                    }
                }
                val n = calendarIds.size
                val msg = buildString {
                    append("Classes — added $classesAdded, updated $classesUpdated")
                    if (includeExams) append('\n').append("Exams — added $examsAdded, updated $examsUpdated")
                    if (holidays.isNotEmpty()) append('\n').append("Holidays — added $holidaysAdded, updated $holidaysUpdated")
                    if (personal.isNotEmpty()) append('\n').append("Events — added $eventsAdded, updated $eventsUpdated")
                    if (n > 1) append("\nWritten to $n calendars.")
                }
                _schedule.update { it.copy(isSyncing = false, message = msg) }

                // Post notification with the summary
                val zone = ZoneId.systemDefault()
                val dates = classes.mapNotNull { entry ->
                    runCatching {
                        OffsetDateTime.parse(entry.timeFromIso).atZoneSameInstant(zone).toLocalDate()
                    }.getOrNull()
                }
                syncNotifier.notifySuccess(
                    SyncStats(
                        intakeCode = s.intakeCode,
                        startDate = dates.minOrNull(),
                        endDate = dates.maxOrNull(),
                        weeks = dates.map { it.with(DayOfWeek.MONDAY) }.distinct().size,
                        classCount = classes.size,
                        examCount = exams.size,
                        holidayCount = holidays.size,
                    )
                )
                // (Re)schedule the per-exam "Good luck" notifications.
                examLuckScheduler.rescheduleAll(s.exams)
                // (Re)schedule the day-before-holiday reminders.
                holidayReminderScheduler.rescheduleAll(s.holidays)
            } catch (t: Throwable) {
                _schedule.update {
                    it.copy(
                        isSyncing = false,
                        error = "Sync failed: ${t.message ?: t::class.simpleName}",
                    )
                }
                syncNotifier.notifyFailure(s.intakeCode, t.message)
            }
        }
    }

    // --- Lecturers ------------------------------------------------------- //

    fun searchLecturers(query: String) {
        _lecturer.update { it.copy(query = query) }
        viewModelScope.launch {
            val results = runCatching { staffRepo.search(query, limit = 80) }.getOrDefault(emptyList())
            _lecturer.update { it.copy(results = results) }
        }
    }

    fun selectLecturer(lecturer: Lecturer) {
        _lecturer.update {
            it.copy(selected = lecturer, schedule = emptyList(), freeSlots = emptyList(), error = null)
        }
        loadLecturerSchedule()
    }

    fun clearSelectedLecturer() {
        _lecturer.update {
            it.copy(selected = null, schedule = emptyList(), freeSlots = emptyList(), error = null)
        }
    }

    fun setAppointmentDate(date: LocalDate) {
        _lecturer.update { it.copy(appointmentDate = date) }
        recomputeFreeSlots()
    }

    private fun loadLecturerSchedule() {
        val sam = _lecturer.value.selected?.id ?: return
        viewModelScope.launch {
            _lecturer.update { it.copy(isLoadingSchedule = true, error = null) }
            try {
                val list = lecturerScheduleRepo.fetch(sam)
                _lecturer.update { it.copy(isLoadingSchedule = false, schedule = list) }
                recomputeFreeSlots()
            } catch (t: Throwable) {
                _lecturer.update {
                    it.copy(
                        isLoadingSchedule = false,
                        error = "Couldn't load lecturer schedule: ${t.message ?: t::class.simpleName}",
                    )
                }
            }
        }
    }

    private fun recomputeFreeSlots() {
        val state = _lecturer.value
        val slots = lecturerScheduleRepo.freeSlotsForDay(state.schedule, state.appointmentDate)
        _lecturer.update { it.copy(freeSlots = slots) }
    }

    suspend fun lookupLecturerByCode(code: String?): Lecturer? = staffRepo.byCode(code)
    suspend fun lookupLecturerBySam(sam: String?): Lecturer? = staffRepo.bySam(sam)

    fun bookAppointment(
        slot: FreeSlot,
        title: String,
        body: String,
        onResult: (success: Boolean, error: String?) -> Unit = { _, _ -> },
    ) {
        val lect = _lecturer.value.selected ?: return onResult(false, "Pick a lecturer first.")
        val calendarIds = _schedule.value.selectedCalendarIds
        if (calendarIds.isEmpty()) {
            onResult(false, "No calendar selected. Re-run onboarding from Settings to pick one.")
            return
        }
        val zone = java.time.ZoneId.systemDefault()
        val startMs = slot.date.atTime(slot.start).atZone(zone).toInstant().toEpochMilli()
        val endMs = slot.date.atTime(slot.end).atZone(zone).toInstant().toEpochMilli()
        viewModelScope.launch {
            try {
                var firstEventId = -1L
                withContext(Dispatchers.IO) {
                    for (calendarId in calendarIds) {
                        val id = calendarSync.insertAppointment(
                            calendarId = calendarId,
                            title = "Appointment: ${lect.fullName} — $title",
                            description = buildString {
                                append("With: ").append(lect.fullName).append('\n')
                                lect.code?.let { append("Code: ").append(it).append('\n') }
                                lect.email?.let { append("Email: ").append(it).append('\n') }
                                if (body.isNotBlank()) append('\n').append(body)
                            },
                            location = lect.location,
                            startMs = startMs,
                            endMs = endMs,
                            reminderOffsetsMinutes = _settings.value.classReminderMinutes,
                        )
                        if (firstEventId <= 0) firstEventId = id
                    }
                }
                noteRepo.insert(
                    Note(
                        type = NoteType.APPOINTMENT,
                        title = title.ifBlank { "Appointment with ${lect.fullName}" },
                        body = body,
                        lecturerCode = lect.code,
                        lecturerSam = lect.id,
                        dueAtMs = startMs,
                        endAtMs = endMs,
                        calendarEventId = firstEventId.takeIf { it > 0 },
                    )
                )
                onResult(true, null)
            } catch (t: Throwable) {
                onResult(false, t.message ?: t::class.simpleName)
            }
        }
    }

    // --- Departments ----------------------------------------------------- //

    fun loadDepartments(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _departments.update { it.copy(isLoading = true, error = null) }
            try {
                val list = departmentRepo.all(forceRefresh)
                _departments.update { it.copy(isLoading = false, items = list) }
            } catch (t: Throwable) {
                _departments.update {
                    it.copy(isLoading = false, error = "Failed to load departments: ${t.message}")
                }
            }
        }
    }

    fun departmentRepository(): DepartmentRepository = departmentRepo

    // --- Transport ------------------------------------------------------- //

    fun loadTransport(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _transport.update { it.copy(isLoading = true, error = null) }
            try {
                val data = transportRepo.load(forceRefresh)
                val now = java.time.ZonedDateTime.now()
                val next = transportRepo.nextTrips(data.schedule, now, count = 6)
                _transport.update {
                    it.copy(isLoading = false, locations = data.locations, nextTrips = next)
                }
            } catch (t: Throwable) {
                _transport.update {
                    it.copy(isLoading = false, error = "Failed to load transport: ${t.message}")
                }
            }
        }
    }

    // --- Notes ----------------------------------------------------------- //

    fun saveNote(note: Note) {
        viewModelScope.launch { noteRepo.insert(note) }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch { noteRepo.delete(note) }
    }

    fun notesForEvent(eventKey: String): Flow<List<Note>> = noteRepo.forEvent(eventKey)

    // --- Settings -------------------------------------------------------- //

    fun setClassReminders(values: List<Int>) {
        prefs.classReminderMinutes = values
        _settings.update { it.copy(classReminderMinutes = values) }
    }

    fun setExamReminders(values: List<Int>) {
        prefs.examReminderMinutes = values
        _settings.update { it.copy(examReminderMinutes = values) }
    }

    fun setSyncHolidays(value: Boolean) {
        prefs.syncHolidays = value
        _settings.update { it.copy(syncHolidays = value) }
    }

    fun setAutoSyncEnabled(value: Boolean) {
        prefs.autoSyncEnabled = value
        _schedule.update { it.copy(autoSyncEnabled = value) }
        if (value) AutoSyncWorker.schedule(getApplication())
        else AutoSyncWorker.cancel(getApplication())
    }

    fun completeOnboarding() {
        prefs.onboardingComplete = true
        _schedule.update { it.copy(onboardingComplete = true) }
        if (prefs.autoSyncEnabled) AutoSyncWorker.schedule(getApplication())
    }

    fun resetOnboarding() {
        prefs.onboardingComplete = false
        _schedule.update { it.copy(onboardingComplete = false) }
    }

    fun removeAllSyncedEvents(onResult: (Int) -> Unit = {}) {
        val ids = _schedule.value.selectedCalendarIds
        viewModelScope.launch {
            val deleted = if (ids.isEmpty()) 0 else withContext(Dispatchers.IO) {
                calendarSync.deleteAllAplifeEvents(ids)
            }
            // Cancel any still-pending APLife notifications too
            ExamLuckScheduler(getApplication()).rescheduleAll(emptyList())
            HolidayReminderScheduler(getApplication()).rescheduleAll(emptyList())
            onResult(deleted)
        }
    }

    fun setIntakeAndLoad(code: String) {
        prefs.intakeCode = code
        _schedule.update { it.copy(intakeCode = code) }
        loadIntake(forceRefresh = true)
    }

    fun isCalendarReady(): Boolean = _schedule.value.calendars.isNotEmpty()

    fun clearScheduleMessages() {
        _schedule.update { it.copy(error = null, message = null) }
    }
}
