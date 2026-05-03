package net.dkly.aplife.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import net.dkly.aplife.calendar.DeviceCalendar
import net.dkly.aplife.data.ClassOverride
import net.dkly.aplife.data.ClosedTimeRange
import net.dkly.aplife.data.Department
import net.dkly.aplife.data.ExamEntry
import net.dkly.aplife.data.Holiday
import net.dkly.aplife.data.Lecturer
import net.dkly.aplife.data.PersonalEvent
import net.dkly.aplife.data.PersonalEventType
import net.dkly.aplife.data.ScheduledTrip
import net.dkly.aplife.data.TimetableEntry
import net.dkly.aplife.data.TransportLocation
import net.dkly.aplife.notes.Note
import net.dkly.aplife.notes.NoteType
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.TextStyle
import java.util.Locale

// ===== SCHEDULE TAB =====================================================

@Composable
fun ScheduleTab(
    state: ScheduleState,
    settings: SettingsState,
    notes: List<Note>,
    personalEvents: List<PersonalEvent>,
    onResync: () -> Unit,
    onRefreshFromApu: () -> Unit,
    onChangeIntakeRequested: () -> Unit,
    onAddNoteForClass: (TimetableEntry) -> Unit,
    onAddNoteForDate: (LocalDate) -> Unit,
    onAddEventForDate: (LocalDate) -> Unit,
    onEditPersonalEvent: (PersonalEvent) -> Unit,
    onDeletePersonalEvent: (PersonalEvent) -> Unit,
    onLecturerCardTap: (TimetableEntry) -> Unit,
    onEditClass: (TimetableEntry) -> Unit,
    onDeleteNote: (Note) -> Unit,
    contentPadding: PaddingValues,
) {
    val today = remember { LocalDate.now() }
    var weekStart by remember { mutableStateOf(today.with(java.time.DayOfWeek.MONDAY)) }
    var selectedDate by remember { mutableStateOf(today) }
    var viewMode by remember { mutableStateOf(ScheduleViewMode.Day) }
    var monthAnchor by remember { mutableStateOf(today.withDayOfMonth(1)) }

    val zone = ZoneId.systemDefault()
    val weekEnd = weekStart.plusDays(7)
    val classesForDay = remember(selectedDate, state.filteredEntries) {
        state.filteredEntries.filter {
            runCatching {
                OffsetDateTime.parse(it.timeFromIso).atZoneSameInstant(zone).toLocalDate()
            }.getOrNull() == selectedDate
        }.sortedBy { it.timeFromIso }
    }
    val classesForWeek = remember(weekStart, state.filteredEntries) {
        state.filteredEntries.mapNotNull { entry ->
            val date = runCatching {
                OffsetDateTime.parse(entry.timeFromIso).atZoneSameInstant(zone).toLocalDate()
            }.getOrNull()
            if (date != null && !date.isBefore(weekStart) && date.isBefore(weekEnd)) date to entry
            else null
        }.sortedBy { it.second.timeFromIso }
    }
    val examsForDay = remember(selectedDate, state.exams) {
        state.exams.filter {
            runCatching {
                OffsetDateTime.parse(it.since).atZoneSameInstant(zone).toLocalDate()
            }.getOrNull() == selectedDate
        }
    }
    val examsForWeek = remember(weekStart, state.exams) {
        state.exams.mapNotNull { exam ->
            val date = runCatching {
                OffsetDateTime.parse(exam.since).atZoneSameInstant(zone).toLocalDate()
            }.getOrNull()
            if (date != null && !date.isBefore(weekStart) && date.isBefore(weekEnd)) date to exam
            else null
        }.sortedBy { it.second.since }
    }
    val holidaysForDay = remember(selectedDate, state.holidays) {
        state.holidays.filter {
            java.time.Instant.ofEpochMilli(it.startEpochMs).atZone(zone).toLocalDate() == selectedDate
        }
    }
    val holidaysForWeek = remember(weekStart, state.holidays) {
        state.holidays.mapNotNull { h ->
            val date = java.time.Instant.ofEpochMilli(h.startEpochMs).atZone(zone).toLocalDate()
            if (!date.isBefore(weekStart) && date.isBefore(weekEnd)) date to h else null
        }
    }
    val notesForDay = remember(selectedDate, notes) {
        val dayStart = selectedDate.atStartOfDay(zone).toInstant().toEpochMilli()
        val dayEnd = selectedDate.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        notes.filter { it.dueAtMs != null && it.dueAtMs in dayStart until dayEnd }
    }
    val personalForDay = remember(selectedDate, personalEvents) {
        val dayStart = selectedDate.atStartOfDay(zone).toInstant().toEpochMilli()
        val dayEnd = selectedDate.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        personalEvents.filter { it.startMs in dayStart until dayEnd }.sortedBy { it.startMs }
    }
    val personalForWeek = remember(weekStart, personalEvents) {
        val ws = weekStart.atStartOfDay(zone).toInstant().toEpochMilli()
        val we = weekEnd.atStartOfDay(zone).toInstant().toEpochMilli()
        personalEvents.mapNotNull { e ->
            if (e.startMs in ws until we) {
                java.time.Instant.ofEpochMilli(e.startMs).atZone(zone).toLocalDate() to e
            } else null
        }.sortedBy { it.second.startMs }
    }
    val weeksCovered = remember(state.filteredEntries) {
        state.filteredEntries.mapNotNull {
            runCatching {
                OffsetDateTime.parse(it.timeFromIso).atZoneSameInstant(zone)
                    .toLocalDate().with(java.time.DayOfWeek.MONDAY)
            }.getOrNull()
        }.distinct().size
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = ScreenHorizontalPadding),
        verticalArrangement = Arrangement.spacedBy(SectionGap),
    ) {
        item { Spacer(Modifier.height(4.dp)) }

        item {
            ScheduleHeaderCard(
                state = state,
                weeksCovered = weeksCovered,
                isSyncing = state.isSyncing,
                onResync = onResync,
                onRefreshFromApu = onRefreshFromApu,
                onChangeIntakeRequested = onChangeIntakeRequested,
            )
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ViewModeToggle(viewMode = viewMode, onChange = { viewMode = it })
                Spacer(Modifier.weight(1f))
                val showToday = when (viewMode) {
                    ScheduleViewMode.Day -> selectedDate != today
                    ScheduleViewMode.Week -> weekStart != today.with(java.time.DayOfWeek.MONDAY)
                    ScheduleViewMode.Month -> monthAnchor != today.withDayOfMonth(1)
                }
                if (showToday) {
                    AssistChip(
                        onClick = {
                            selectedDate = today
                            weekStart = today.with(java.time.DayOfWeek.MONDAY)
                            monthAnchor = today.withDayOfMonth(1)
                        },
                        label = { Text("Today") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.DateRange,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                        },
                        shape = ChipCorner,
                    )
                }
            }
        }

        when (viewMode) {
            ScheduleViewMode.Day -> {
                item {
                    WeekStrip(
                        weekStart = weekStart,
                        selected = selectedDate,
                        today = today,
                        viewMode = viewMode,
                        classes = state.filteredEntries,
                        exams = state.exams,
                        holidays = state.holidays,
                        personalEvents = personalEvents,
                        onDateSelected = { selectedDate = it },
                        onWeekShift = { offset ->
                            weekStart = weekStart.plusWeeks(offset.toLong())
                            if (selectedDate.isBefore(weekStart) || !selectedDate.isBefore(weekStart.plusDays(7))) {
                                selectedDate = weekStart
                            }
                        },
                    )
                }
                item {
                    SectionHeader(
                        title = dayHeadline(selectedDate, today),
                        subtitle = daySubtitle(selectedDate, today),
                        leadingIcon = Icons.Default.DateRange,
                        trailing = {
                            DayActionsMenu(
                                onAddNote = { onAddNoteForDate(selectedDate) },
                                onAddEvent = { onAddEventForDate(selectedDate) },
                            )
                        },
                    )
                }

                if (holidaysForDay.isNotEmpty()) {
                    items(holidaysForDay, key = { it.name + it.startEpochMs }) { HolidayCard(it) }
                }
                if (classesForDay.isNotEmpty()) {
                    items(
                        classesForDay,
                        key = { it.classCode ?: ((it.moduleId ?: "?") + it.timeFromIso) },
                    ) { entry ->
                        ClassCard(
                            entry = entry,
                            onAddNote = { onAddNoteForClass(entry) },
                            onTapLecturer = { onLecturerCardTap(entry) },
                            onEditClass = { onEditClass(entry) },
                        )
                    }
                }
                if (personalForDay.isNotEmpty()) {
                    items(personalForDay, key = { "p-${it.id}" }) { event ->
                        PersonalEventCard(
                            event = event,
                            onEdit = { onEditPersonalEvent(event) },
                            onDelete = { onDeletePersonalEvent(event) },
                        )
                    }
                }
                if (examsForDay.isNotEmpty()) {
                    items(examsForDay, key = { it.module + it.since }) { exam -> ExamCard(exam) }
                }
                if (classesForDay.isEmpty() && examsForDay.isEmpty() && holidaysForDay.isEmpty() && personalForDay.isEmpty()) {
                    val unpublished = !isWithinPublishedRange(selectedDate, state.intakeEntries, zone)
                    if (unpublished) {
                        item { UnpublishedHint(selectedDate) }
                    } else {
                        item {
                            EmptyState(
                                icon = Icons.Default.CheckCircle,
                                title = "Nothing scheduled",
                                subtitle = "A free day! Tap + Event or + Note above to plan something.",
                            )
                        }
                    }
                }
            }

            ScheduleViewMode.Week -> {
                item {
                    WeekRangeHeader(
                        weekStart = weekStart,
                        onShift = { weekStart = weekStart.plusWeeks(it.toLong()) },
                    )
                }
                item {
                    SectionHeader(
                        title = "${weekStart.format(DAY_DATE_FMT)} – ${weekStart.plusDays(6).format(DAY_DATE_FMT)}",
                        subtitle = whenWeekSubtitle(weekStart, today),
                        leadingIcon = Icons.Default.DateRange,
                    )
                }
                val isEmptyWeek = classesForWeek.isEmpty() && examsForWeek.isEmpty() &&
                    holidaysForWeek.isEmpty() && personalForWeek.isEmpty()
                if (isEmptyWeek) {
                    val unpublished = !isWithinPublishedRange(weekStart.plusDays(6), state.intakeEntries, zone)
                    if (unpublished) {
                        item { UnpublishedHint(weekStart) }
                    } else {
                        item {
                            EmptyState(
                                icon = Icons.Default.CheckCircle,
                                title = "Nothing this week",
                                subtitle = "No classes, exams, holidays or personal events in this week.",
                            )
                        }
                    }
                } else {
                    (0..6).forEach { i ->
                        val date = weekStart.plusDays(i.toLong())
                        val cs = classesForWeek.filter { it.first == date }
                        val es = examsForWeek.filter { it.first == date }
                        val hs = holidaysForWeek.filter { it.first == date }
                        val ps = personalForWeek.filter { it.first == date }
                        if (cs.isEmpty() && es.isEmpty() && hs.isEmpty() && ps.isEmpty()) return@forEach
                        item(key = "weekday-$date") {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(10.dp),
                            ) {
                                Text(
                                    "${date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} · ${date.format(DAY_DATE_FMT)}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                )
                            }
                        }
                        items(hs, key = { "h-${it.second.name}-${it.second.startEpochMs}" }) { HolidayCard(it.second) }
                        items(cs, key = { "c-" + (it.second.classCode ?: ((it.second.moduleId ?: "?") + it.second.timeFromIso)) }) { (_, entry) ->
                            ClassCard(
                                entry = entry,
                                onAddNote = { onAddNoteForClass(entry) },
                                onTapLecturer = { onLecturerCardTap(entry) },
                                onEditClass = { onEditClass(entry) },
                            )
                        }
                        items(ps, key = { "p-${it.second.id}" }) { (_, event) ->
                            PersonalEventCard(
                                event = event,
                                onEdit = { onEditPersonalEvent(event) },
                                onDelete = { onDeletePersonalEvent(event) },
                            )
                        }
                        items(es, key = { "e-${it.second.module}-${it.second.since}" }) { (_, exam) -> ExamCard(exam) }
                    }
                }
            }
            ScheduleViewMode.Month -> {
                item {
                    MonthHeader(
                        anchorMonth = monthAnchor,
                        onShiftMonths = { delta ->
                            monthAnchor = monthAnchor.plusMonths(delta.toLong())
                        },
                    )
                }
                item {
                    MonthGrid(
                        anchorMonth = monthAnchor,
                        today = today,
                        selected = selectedDate,
                        classes = state.filteredEntries,
                        exams = state.exams,
                        holidays = state.holidays,
                        personalEvents = personalEvents,
                        onPickDate = {
                            selectedDate = it
                            weekStart = it.with(java.time.DayOfWeek.MONDAY)
                            monthAnchor = it.withDayOfMonth(1)
                            viewMode = ScheduleViewMode.Day
                        },
                    )
                }
                item {
                    SectionHeader(
                        title = formatDate(selectedDate),
                        subtitle = "Tap a date above to switch to Day view",
                        leadingIcon = Icons.Default.DateRange,
                    )
                }
            }
        }

        if (notesForDay.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Your notes for this day",
                    subtitle = "${notesForDay.size} saved",
                    leadingIcon = Icons.Default.Edit,
                )
            }
            items(notesForDay, key = { it.id }) { note ->
                ScheduleNoteCard(note = note, onDelete = { onDeleteNote(note) })
            }
        }

        state.error?.let {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
        }

        item { Spacer(Modifier.height(32.dp)) }
    }
}

enum class ScheduleViewMode { Day, Week, Month }

@Composable
private fun ScheduleHeaderCard(
    state: ScheduleState,
    weeksCovered: Int,
    isSyncing: Boolean,
    onResync: () -> Unit,
    onRefreshFromApu: () -> Unit,
    onChangeIntakeRequested: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(22.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box {
            Column(modifier = Modifier.padding(20.dp)) {
                // Top row: label + group pill + overflow
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "INTAKE",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.weight(1f))
                    if (state.selectedGroups.isNotEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(999.dp),
                        ) {
                            Text(
                                state.selectedGroups.sorted().joinToString(", "),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                    }
                    Box {
                        androidx.compose.material3.IconButton(
                            onClick = { menuOpen = true },
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "More",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        androidx.compose.material3.DropdownMenu(
                            expanded = menuOpen,
                            onDismissRequest = { menuOpen = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (isSyncing) "Syncing…" else "Sync to calendar") },
                                leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null) },
                                enabled = !isSyncing,
                                onClick = { menuOpen = false; onResync() },
                            )
                            DropdownMenuItem(
                                text = { Text("Refresh from APU") },
                                leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                                enabled = !state.isLoading,
                                onClick = { menuOpen = false; onRefreshFromApu() },
                            )
                            DropdownMenuItem(
                                text = { Text("Change intake / groups") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = { menuOpen = false; onChangeIntakeRequested() },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                // Big bold intake code as the headline
                Text(
                    state.intakeCode.ifBlank { "No intake" },
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(4.dp))
                // Subtitle: weeks/classes coverage
                val subtitle = if (weeksCovered > 0) {
                    "Showing $weeksCovered upcoming week${if (weeksCovered == 1) "" else "s"} · " +
                        "${state.filteredEntries.size} class${if (state.filteredEntries.size == 1) "" else "es"}"
                } else "Tap menu → Refresh from APU to load classes."
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ViewModeToggle(
    viewMode: ScheduleViewMode,
    onChange: (ScheduleViewMode) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ScheduleViewMode.entries.forEach { mode ->
            FilterChip(
                selected = viewMode == mode,
                onClick = { onChange(mode) },
                label = { Text(mode.name) },
                shape = ChipCorner,
            )
        }
    }
}

@Composable
private fun WeekRangeHeader(weekStart: LocalDate, onShift: (Int) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        androidx.compose.material3.IconButton(onClick = { onShift(-1) }) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Previous week",
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        Text(
            "${weekStart.format(DAY_DATE_FMT)} – ${weekStart.plusDays(6).format(DAY_DATE_FMT)}",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        androidx.compose.material3.IconButton(onClick = { onShift(1) }) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Next week",
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun MonthHeader(anchorMonth: LocalDate, onShiftMonths: (Int) -> Unit) {
    val fmt = java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        TextButton(onClick = { onShiftMonths(-1) }) { Text("‹") }
        Spacer(Modifier.weight(1f))
        Text(
            anchorMonth.format(fmt),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.weight(1f))
        TextButton(onClick = { onShiftMonths(1) }) { Text("›") }
    }
}

@Composable
private fun MonthGrid(
    anchorMonth: LocalDate,
    today: LocalDate,
    selected: LocalDate,
    classes: List<TimetableEntry>,
    exams: List<ExamEntry>,
    holidays: List<Holiday>,
    personalEvents: List<PersonalEvent>,
    onPickDate: (LocalDate) -> Unit,
) {
    val zone = ZoneId.systemDefault()
    val firstOfMonth = anchorMonth.withDayOfMonth(1)
    val lastOfMonth = firstOfMonth.plusMonths(1).minusDays(1)
    val gridStart = firstOfMonth.with(java.time.DayOfWeek.MONDAY)
    val gridEnd = lastOfMonth.with(java.time.DayOfWeek.SUNDAY)
    val totalDays = (java.time.temporal.ChronoUnit.DAYS.between(gridStart, gridEnd).toInt() + 1)

    // Build a set of dates that have any event
    val classDates = remember(classes) {
        classes.mapNotNull {
            runCatching { OffsetDateTime.parse(it.timeFromIso).atZoneSameInstant(zone).toLocalDate() }.getOrNull()
        }.toSet()
    }
    val examDates = remember(exams) {
        exams.mapNotNull {
            runCatching { OffsetDateTime.parse(it.since).atZoneSameInstant(zone).toLocalDate() }.getOrNull()
        }.toSet()
    }
    val holidayDates = remember(holidays) {
        holidays.map { java.time.Instant.ofEpochMilli(it.startEpochMs).atZone(zone).toLocalDate() }.toSet()
    }
    val personalDates = remember(personalEvents) {
        personalEvents.map { java.time.Instant.ofEpochMilli(it.startMs).atZone(zone).toLocalDate() }.toSet()
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach { d ->
                Text(
                    d,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
        val rows = (totalDays + 6) / 7
        for (r in 0 until rows) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                for (c in 0..6) {
                    val date = gridStart.plusDays((r * 7 + c).toLong())
                    val inMonth = date.month == anchorMonth.month
                    val isSelected = date == selected
                    val isToday = date == today
                    val hasClass = date in classDates
                    val hasExam = date in examDates
                    val hasHoliday = date in holidayDates
                    val hasPersonal = date in personalDates
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                when {
                                    isSelected -> MaterialTheme.colorScheme.primary
                                    isToday -> MaterialTheme.colorScheme.primaryContainer
                                    inMonth -> MaterialTheme.colorScheme.surfaceVariant
                                    else -> Color.Transparent
                                },
                            )
                            .clickable(enabled = inMonth) { onPickDate(date) }
                            .padding(4.dp),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()) {
                            Text(
                                date.dayOfMonth.toString(),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = when {
                                    isSelected -> MaterialTheme.colorScheme.onPrimary
                                    isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                                    inMonth -> MaterialTheme.colorScheme.onSurface
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                },
                            )
                            Spacer(Modifier.weight(1f))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                modifier = Modifier.padding(bottom = 2.dp),
                            ) {
                                if (hasClass) Dot(MaterialTheme.colorScheme.primary, isSelected)
                                if (hasExam) Dot(MaterialTheme.colorScheme.tertiary, isSelected)
                                if (hasHoliday) Dot(net.dkly.aplife.ui.theme.SuccessGreen, isSelected)
                                if (hasPersonal) Dot(MaterialTheme.colorScheme.secondary, isSelected)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Dot(color: Color, selected: Boolean) {
    Box(
        modifier = Modifier
            .size(5.dp)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(if (selected) MaterialTheme.colorScheme.onPrimary else color),
    )
}

@Composable
private fun UnpublishedHint(date: LocalDate) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.padding(12.dp),
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    "Timetable not published yet",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "APU hasn't released the timetable for ${date.format(LONG_DATE_FMT)} yet. " +
                        "You can add your own events here, but check back later — your published " +
                        "classes might overlap with what you've added.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        }
    }
}

private fun isWithinPublishedRange(
    date: LocalDate,
    entries: List<TimetableEntry>,
    zone: ZoneId,
): Boolean {
    if (entries.isEmpty()) return true // unknown — don't show the hint
    val maxDate = entries.mapNotNull {
        runCatching { OffsetDateTime.parse(it.timeFromIso).atZoneSameInstant(zone).toLocalDate() }.getOrNull()
    }.maxOrNull() ?: return true
    return !date.isAfter(maxDate)
}

private fun whenWeekSubtitle(weekStart: LocalDate, today: LocalDate): String {
    val thisWeek = today.with(java.time.DayOfWeek.MONDAY)
    val diff = java.time.temporal.ChronoUnit.WEEKS.between(thisWeek, weekStart).toInt()
    return when (diff) {
        0 -> "This week"
        1 -> "Next week"
        -1 -> "Last week"
        in 2..52 -> "In $diff weeks"
        in -52..-2 -> "${-diff} weeks ago"
        else -> ""
    }
}

@Composable
private fun WeekStrip(
    weekStart: LocalDate,
    selected: LocalDate,
    today: LocalDate,
    viewMode: ScheduleViewMode,
    classes: List<TimetableEntry>,
    exams: List<ExamEntry>,
    holidays: List<Holiday>,
    personalEvents: List<PersonalEvent>,
    onDateSelected: (LocalDate) -> Unit,
    onWeekShift: (Int) -> Unit,
) {
    val zone = ZoneId.systemDefault()
    val classDates = remember(classes) {
        classes.mapNotNull {
            runCatching { OffsetDateTime.parse(it.timeFromIso).atZoneSameInstant(zone).toLocalDate() }.getOrNull()
        }.toSet()
    }
    val examDates = remember(exams) {
        exams.mapNotNull {
            runCatching { OffsetDateTime.parse(it.since).atZoneSameInstant(zone).toLocalDate() }.getOrNull()
        }.toSet()
    }
    val holidayDates = remember(holidays) {
        holidays.map { java.time.Instant.ofEpochMilli(it.startEpochMs).atZone(zone).toLocalDate() }.toSet()
    }
    val personalDates = remember(personalEvents) {
        personalEvents.map { java.time.Instant.ofEpochMilli(it.startMs).atZone(zone).toLocalDate() }.toSet()
    }
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { onWeekShift(-1) }) { Text("<") }
            Spacer(Modifier.weight(1f))
            Text(
                "${weekStart.format(DAY_DATE_FMT)} – ${weekStart.plusDays(6).format(DAY_DATE_FMT)}",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { onWeekShift(1) }) { Text(">") }
        }
        Spacer(Modifier.height(4.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            for (i in 0..6) {
                val date = weekStart.plusDays(i.toLong())
                val isSelected = viewMode == ScheduleViewMode.Day && date == selected
                val isToday = date == today
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            when {
                                isSelected -> MaterialTheme.colorScheme.primary
                                isToday -> MaterialTheme.colorScheme.primaryContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            },
                        )
                        .clickable { onDateSelected(date) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                            style = MaterialTheme.typography.labelSmall,
                            color = when {
                                isSelected -> MaterialTheme.colorScheme.onPrimary
                                isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                        Text(
                            date.dayOfMonth.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isSelected -> MaterialTheme.colorScheme.onPrimary
                                isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                                else -> MaterialTheme.colorScheme.onSurface
                            },
                        )
                        // Indicator dots: class / exam / holiday / personal event
                        val hasClass = date in classDates
                        val hasExam = date in examDates
                        val hasHoliday = date in holidayDates
                        val hasPersonal = date in personalDates
                        if (hasClass || hasExam || hasHoliday || hasPersonal) {
                            Spacer(Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                if (hasClass) Dot(MaterialTheme.colorScheme.primary, isSelected)
                                if (hasExam) Dot(MaterialTheme.colorScheme.tertiary, isSelected)
                                if (hasHoliday) Dot(net.dkly.aplife.ui.theme.SuccessGreen, isSelected)
                                if (hasPersonal) Dot(MaterialTheme.colorScheme.secondary, isSelected)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduleNoteCard(note: Note, onDelete: () -> Unit) {
    BrandCard {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = ChipCorner) {
                    Text(
                        note.type.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDelete) { Text("Delete") }
            }
            Text(note.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (note.body.isNotBlank()) {
                Text(note.body, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/** Prominent label for the day section — "Today" / "Tomorrow" / weekday name. */
private fun dayHeadline(date: LocalDate, today: LocalDate): String {
    val diff = java.time.temporal.ChronoUnit.DAYS.between(today, date).toInt()
    return when (diff) {
        0 -> "Today"
        1 -> "Tomorrow"
        -1 -> "Yesterday"
        else -> date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
    }
}

/** Supplemental subtitle below the headline — full date + relative phrase if useful. */
private fun daySubtitle(date: LocalDate, today: LocalDate): String {
    val pretty = date.format(LONG_DATE_FMT)
    val diff = java.time.temporal.ChronoUnit.DAYS.between(today, date).toInt()
    val relative = when (diff) {
        0, 1, -1 -> null
        in 2..6 -> "in $diff days"
        in -6..-2 -> "${-diff} days ago"
        in 7..30 -> "in ${diff / 7} week${if (diff / 7 == 1) "" else "s"}"
        in -30..-7 -> "${-diff / 7} week${if (-diff / 7 == 1) "" else "s"} ago"
        else -> null
    }
    return if (relative == null) pretty else "$pretty · $relative"
}

@Composable
private fun HeroCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primary),
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                )
            }
        }
    }
}

@Composable
private fun ClassCard(
    entry: TimetableEntry,
    onAddNote: () -> Unit,
    onTapLecturer: () -> Unit,
    onEditClass: () -> Unit,
) {
    BrandCard(onClick = onEditClass) {
        Row(verticalAlignment = Alignment.Top) {
            // Time column — large + bold
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(72.dp),
            ) {
                Text(
                    runCatching {
                        OffsetDateTime.parse(entry.timeFromIso)
                            .atZoneSameInstant(ZoneId.systemDefault()).format(TIME_FMT)
                    }.getOrDefault(""),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    runCatching {
                        val from = OffsetDateTime.parse(entry.timeFromIso).toInstant()
                        val to = OffsetDateTime.parse(entry.timeToIso).toInstant()
                        val mins = java.time.Duration.between(from, to).toMinutes()
                        if (mins >= 60 && mins % 60 == 0L) "${mins / 60}h"
                        else if (mins >= 60) "${mins / 60}h ${mins % 60}m"
                        else "${mins}m"
                    }.getOrDefault(""),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // Accent stripe
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .width(3.dp)
                    .height(56.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.primary),
            )
            Spacer(Modifier.width(8.dp))
            // Body
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        entry.moduleName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    if (!entry.grouping.isNullOrBlank()) {
                        Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = ChipCorner) {
                            Text(
                                entry.grouping,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    "Until " + runCatching {
                        OffsetDateTime.parse(entry.timeToIso)
                            .atZoneSameInstant(ZoneId.systemDefault()).format(TIME_FMT)
                    }.getOrDefault(""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!entry.room.isNullOrBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Place, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(entry.room, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (!entry.lecturerName.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccountCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            entry.lecturerName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onTapLecturer() }
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExamCard(exam: ExamEntry) {
    BrandCard {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.tertiaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(exam.subjectDescription, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "${formatDayDate(exam.since)} · ${formatTimeRange(exam.since, exam.until)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            val sub = listOfNotNull(exam.module, exam.venue, exam.assessmentType).joinToString(" · ")
            if (sub.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun HolidayCard(h: Holiday) {
    BrandCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(h.name, fontWeight = FontWeight.SemiBold)
                val date = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(h.startEpochMs), ZoneId.systemDefault())
                    .toLocalDate().format(LONG_DATE_FMT)
                Text(date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarPicker(
    calendars: List<DeviceCalendar>,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = calendars.firstOrNull { it.id == selectedId } ?: calendars.first()
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        TextField(
            value = "${selected.displayName} (${selected.accountName})",
            onValueChange = {},
            readOnly = true,
            label = { Text("Calendar") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            calendars.forEach { cal ->
                DropdownMenuItem(
                    text = { Text("${cal.displayName} (${cal.accountName})") },
                    onClick = {
                        onSelect(cal.id)
                        expanded = false
                    },
                )
            }
        }
    }
}

internal fun formatReminders(minutes: List<Int>): String {
    if (minutes.isEmpty()) return "off"
    return minutes.sortedDescending().joinToString(", ") { m ->
        when {
            m % (24 * 60) == 0 -> "${m / (24 * 60)}d"
            m % 60 == 0 -> "${m / 60}h"
            else -> "${m}min"
        }
    }
}

// ===== NOTES TAB ========================================================

@Composable
fun NotesTab(
    notes: List<Note>,
    onAdd: () -> Unit,
    onDelete: (Note) -> Unit,
    contentPadding: PaddingValues,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = ScreenHorizontalPadding),
    ) {
        Spacer(Modifier.height(4.dp))
        SectionHeader(
            title = "Your notes",
            subtitle = if (notes.isEmpty()) "Nothing yet" else "${notes.size} saved",
            leadingIcon = Icons.Default.Edit,
            trailing = {
                FilledTonalButton(onClick = onAdd, shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("New note")
                }
            },
        )
        Spacer(Modifier.height(8.dp))
        if (notes.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Edit,
                title = "No notes yet",
                subtitle = "Add a note from a class card, a lecturer's profile, or with the New note button.",
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(SectionGap)) {
                items(notes, key = { it.id }) { note ->
                    NoteCard(note = note, onDelete = { onDelete(note) })
                }
                item { Spacer(Modifier.height(32.dp)) }
            }
        }
    }
}

@Composable
private fun NoteCard(note: Note, onDelete: () -> Unit) {
    BrandCard {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                NoteTypeBadge(note.type)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDelete) { Text("Delete") }
            }
            Spacer(Modifier.height(4.dp))
            Text(note.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (note.body.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(note.body, style = MaterialTheme.typography.bodyMedium)
            }
            note.dueAtMs?.let {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(formatEpoch(it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            note.eventKey?.let {
                Spacer(Modifier.height(2.dp))
                Text("Class: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun NoteTypeBadge(type: NoteType) {
    val color = when (type) {
        NoteType.CLASS_TEST -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        NoteType.PRESENTATION -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        NoteType.ASSIGNMENT_DISCUSSION -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        NoteType.LECTURE -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        NoteType.APPOINTMENT -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
    }
    Surface(color = color.first, shape = ChipCorner) {
        Text(
            type.label,
            style = MaterialTheme.typography.labelSmall,
            color = color.second,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

// ===== LECTURER TAB =====================================================

@Composable
fun LecturersTab(
    state: LecturerState,
    onSearch: (String) -> Unit,
    onPick: (Lecturer) -> Unit,
    onClearSelected: () -> Unit,
    contentPadding: PaddingValues,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = ScreenHorizontalPadding),
    ) {
        Spacer(Modifier.height(4.dp))
        if (state.selected == null) {
            SectionHeader(
                title = "Lecturer Directory",
                subtitle = "Search staff to view contact info",
                leadingIcon = Icons.Default.AccountCircle,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = state.query,
                onValueChange = onSearch,
                label = { Text("Search by name, code, or department") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            if (state.results.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.Search,
                    title = "No staff to show",
                    subtitle = "Try a different search term, or clear the field to browse.",
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(state.results, key = { it.refNo ?: (it.fullName + it.code) }) { lect ->
                        LecturerRow(lect, onPick)
                    }
                    item { Spacer(Modifier.height(32.dp)) }
                }
            }
        } else {
            LecturerDetail(
                state = state,
                onClose = onClearSelected,
            )
        }
    }
}

@Composable
private fun LecturerRow(lect: Lecturer, onPick: (Lecturer) -> Unit) {
    BrandCard(onClick = { onPick(lect) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AvatarOrPlaceholder(photo = lect.photo, fallback = lect.fullName)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(lect.fullName, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                Text(
                    listOfNotNull(lect.title, lect.code).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                lect.department?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun AvatarOrPlaceholder(
    photo: String?,
    fallback: String,
    onClick: (() -> Unit)? = null,
) {
    val base = Modifier
        .size(44.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.primaryContainer)
    val mod = if (onClick != null) base.clickable(onClick = onClick) else base
    Box(
        contentAlignment = Alignment.Center,
        modifier = mod,
    ) {
        if (!photo.isNullOrBlank()) {
            AsyncImage(
                model = photo,
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                error = androidx.compose.ui.res.painterResource(net.dkly.aplife.R.drawable.no_avatar),
                fallback = androidx.compose.ui.res.painterResource(net.dkly.aplife.R.drawable.no_avatar),
                modifier = Modifier.fillMaxSize().clip(CircleShape),
            )
        } else {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(net.dkly.aplife.R.drawable.no_avatar),
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(CircleShape),
            )
        }
    }
}

@Composable
private fun LecturerDetail(
    state: LecturerState,
    onClose: () -> Unit,
) {
    val lect = state.selected ?: return
    @Suppress("UNUSED_VARIABLE") val _backHandler = onClose // back is now handled by the top app bar
    var showFullPhoto by remember { mutableStateOf(false) }
    if (showFullPhoto && !lect.photo.isNullOrBlank()) {
        LecturerPhotoDialog(
            photo = lect.photo,
            name = lect.fullName,
            subtitle = listOfNotNull(lect.title, lect.department).joinToString(" · "),
            onDismiss = { showFullPhoto = false },
        )
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(SectionGap)) {
        item {
            BrandCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AvatarOrPlaceholder(
                        photo = lect.photo,
                        fallback = lect.fullName,
                        onClick = if (!lect.photo.isNullOrBlank()) {
                            { showFullPhoto = true }
                        } else null,
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(lect.fullName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text(
                            listOfNotNull(lect.title, lect.department).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        item {
            BrandCard {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    lect.email?.let { MetaRow("Email", it) }
                    lect.staffEmail?.let { MetaRow("Staff", it) }
                    lect.location?.let { MetaRow("Office", it) }
                    lect.code?.let { MetaRow("Code", it) }
                    lect.id?.let { MetaRow("Account", it) }
                }
            }
        }
        item { LecturerContactActions(lect) }
        item {
            SectionHeader(
                title = "Lecturer Timetable",
                subtitle = if (state.isLoadingSchedule) "Loading…" else "${state.schedule.size} session(s)",
                leadingIcon = Icons.Default.DateRange,
            )
        }

        if (state.isLoadingSchedule) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else if (state.error != null) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        state.error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
        } else if (state.schedule.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Default.DateRange,
                    title = "No upcoming sessions",
                    subtitle = "This lecturer has no scheduled classes in the published timetable.",
                )
            }
        } else {
            // Group entries by local date
            val zone = ZoneId.systemDefault()
            val grouped = state.schedule
                .mapNotNull { entry ->
                    runCatching {
                        val odt = OffsetDateTime.parse(entry.time)
                        val zdt = odt.atZoneSameInstant(zone)
                        Triple(zdt.toLocalDate(), zdt, entry)
                    }.getOrNull()
                }
                .sortedWith(compareBy({ it.first }, { it.second }))
                .groupBy { it.first }

            grouped.forEach { (date, entries) ->
                item(key = "header-$date") {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text(
                            date.format(LONG_DATE_FMT),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                    }
                }
                items(entries, key = { "${it.first}-${it.third.module}-${it.third.time}" }) { triple ->
                    LecturerTimetableCard(zdt = triple.second, entry = triple.third)
                }
            }
        }
        item { Spacer(Modifier.height(32.dp)) }
    }
}

@Composable
private fun LecturerContactActions(lect: Lecturer) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val emailAddr = lect.email?.takeIf { it.isNotBlank() } ?: lect.staffEmail
    val teamsAddr = lect.staffEmail?.takeIf { it.isNotBlank() } ?: lect.email
    if (emailAddr.isNullOrBlank() && teamsAddr.isNullOrBlank()) return

    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (!emailAddr.isNullOrBlank()) {
            ContactActionButton(
                label = "Email",
                icon = Icons.Default.Email,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                onClick = { sendEmailIntent(context, emailAddr, lect.fullName) },
                modifier = Modifier.weight(1f),
            )
        }
        if (!teamsAddr.isNullOrBlank()) {
            ContactActionButton(
                label = "Teams",
                icon = Icons.AutoMirrored.Filled.Send,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                onClick = { openTeamsChat(context, teamsAddr) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun LecturerPhotoDialog(
    photo: String,
    name: String,
    subtitle: String,
    onDismiss: () -> Unit,
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column {
                AsyncImage(
                    model = photo,
                    contentDescription = name,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    error = androidx.compose.ui.res.painterResource(net.dkly.aplife.R.drawable.no_avatar),
                    fallback = androidx.compose.ui.res.painterResource(net.dkly.aplife.R.drawable.no_avatar),
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                )
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (subtitle.isNotBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onDismiss) { Text("Close") }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        contentColor = contentColor,
        modifier = modifier.height(48.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(28.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(contentColor.copy(alpha = 0.18f)),
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(10.dp))
            Text(
                label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
    }
}

private fun sendEmailIntent(context: android.content.Context, email: String, lecturerName: String) {
    val mailto = android.net.Uri.parse(
        "mailto:" + android.net.Uri.encode(email) +
            "?subject=" + android.net.Uri.encode(lecturerName)
    )
    val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO, mailto)
    val chooser = android.content.Intent.createChooser(intent, "Send email")
    runCatching { context.startActivity(chooser) }
        .onFailure { android.widget.Toast.makeText(context, "No email app installed.", android.widget.Toast.LENGTH_SHORT).show() }
}

private fun openTeamsChat(context: android.content.Context, email: String) {
    val encoded = android.net.Uri.encode(email)
    val teamsAppUri = android.net.Uri.parse("msteams:/l/chat/0/0?users=$encoded")
    val webUri = android.net.Uri.parse("https://teams.microsoft.com/l/chat/0/0?users=$encoded")
    // Prefer the Teams deep-link scheme so the app is launched directly when installed.
    val tryApp = runCatching {
        context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, teamsAppUri))
    }
    if (tryApp.isFailure) {
        val tryWeb = runCatching {
            context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, webUri))
        }
        if (tryWeb.isFailure) {
            android.widget.Toast.makeText(context, "Microsoft Teams isn't installed.", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
private fun LecturerTimetableCard(
    zdt: ZonedDateTime,
    entry: net.dkly.aplife.data.LecturerScheduleEntry,
) {
    BrandCard {
        Row(verticalAlignment = Alignment.Top) {
            // Time column
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(74.dp),
            ) {
                Text(
                    zdt.toLocalTime().format(TIME_FMT),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    "${entry.duration.toLong() / 60} min",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(48.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    entry.module ?: "Class",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(2.dp))
                val sub = listOfNotNull(
                    entry.room?.takeIf(String::isNotBlank),
                    entry.intakes.takeIf { it.isNotEmpty() }?.joinToString(", "),
                ).joinToString(" · ")
                if (sub.isNotBlank()) {
                    Text(
                        sub,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

// ===== DEPARTMENTS TAB ==================================================

@Composable
fun DepartmentsTab(
    state: DepartmentState,
    onRefresh: () -> Unit,
    isOpenNow: (Department) -> Boolean,
    todayShifts: (Department) -> List<ClosedTimeRange>,
    contentPadding: PaddingValues,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = ScreenHorizontalPadding),
    ) {
        Spacer(Modifier.height(4.dp))
        SectionHeader(
            title = "APU Departments",
            subtitle = "Operating hours · live status",
            leadingIcon = Icons.Default.Home,
            trailing = {
                TextButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (state.isLoading) "Loading…" else "Refresh")
                }
            },
        )
        state.error?.let {
            Spacer(Modifier.height(4.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(8.dp))
        if (state.items.isEmpty() && !state.isLoading) {
            EmptyState(
                icon = Icons.Default.Home,
                title = "No departments yet",
                subtitle = "Tap refresh to fetch the latest department operating hours.",
            )
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(SectionGap)) {
            items(state.items, key = { it.id }) { dept ->
                DepartmentCard(dept, isOpenNow(dept), todayShifts(dept))
            }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun DepartmentCard(
    dept: Department,
    isOpen: Boolean,
    today: List<ClosedTimeRange>,
) {
    BrandCard {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    dept.name.replaceFirstChar { it.titlecase() },
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.weight(1f))
                StatusBadge(text = if (isOpen) "OPEN" else "CLOSED", positive = isOpen)
            }
            Spacer(Modifier.height(6.dp))
            if (today.isEmpty()) {
                Text("Closed today", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text(
                    "Today · " + today.joinToString(", ") { it.pretty() },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            dept.email?.takeIf(String::isNotBlank)?.let {
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(4.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (dept.phones.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(4.dp))
                    Text(dept.phones.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// ===== TRANSPORT TAB ====================================================

@Composable
fun TransportTab(
    state: TransportState,
    onRefresh: () -> Unit,
    contentPadding: PaddingValues,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = ScreenHorizontalPadding),
    ) {
        Spacer(Modifier.height(4.dp))
        SectionHeader(
            title = "APU Shuttle",
            subtitle = "Next departures · official stops",
            leadingIcon = Icons.Default.LocationOn,
            trailing = {
                TextButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (state.isLoading) "Loading…" else "Refresh")
                }
            },
        )
        state.error?.let {
            Spacer(Modifier.height(4.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(8.dp))
        if (state.nextTrips.isEmpty() && !state.isLoading) {
            EmptyState(
                icon = Icons.Default.LocationOn,
                title = "No shuttle data",
                subtitle = "Tap refresh to load today's shuttle schedule.",
            )
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(SectionGap)) {
            if (state.nextTrips.isNotEmpty()) {
                item {
                    Text(
                        "Next departures",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                items(state.nextTrips, key = { it.trip.id.toString() + it.departure.toString() }) { st ->
                    NextTripCard(st)
                }
            }
            if (state.locations.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(4.dp))
                    HorizontalDivider()
                    Text(
                        "Stops · ${state.locations.size}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                items(state.locations, key = { it.id }) { loc -> LocationRow(loc) }
            }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun NextTripCard(st: ScheduledTrip) {
    BrandCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "${st.trip.tripFrom.name}  →  ${st.trip.tripTo.name}",
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    st.departure.toLocalDate().format(LONG_DATE_FMT) + " · " +
                        st.departure.toLocalTime().format(TIME_FMT),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!st.trip.busAssigned.isNullOrBlank()) {
                    Text(
                        "Bus · ${st.trip.busAssigned}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun LocationRow(loc: TransportLocation) {
    BrandCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val color = runCatching {
                Color(android.graphics.Color.parseColor(loc.color ?: "#888888"))
            }.getOrDefault(Color.Gray)
            ColorDot(color = color, size = 12.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(loc.name, fontWeight = FontWeight.SemiBold)
                loc.address?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            loc.type?.let {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = ChipCorner) {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
        }
    }
}

// ===== SETTINGS TAB =====================================================

@Composable
fun SettingsTab(
    state: SettingsState,
    onClassReminders: (List<Int>) -> Unit,
    onExamReminders: (List<Int>) -> Unit,
    onSyncHolidaysChange: (Boolean) -> Unit,
    contentPadding: PaddingValues,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = ScreenHorizontalPadding),
        verticalArrangement = Arrangement.spacedBy(SectionGap),
    ) {
        Spacer(Modifier.height(4.dp))
        SectionHeader(
            title = "Settings",
            subtitle = "Reminders and sync preferences",
            leadingIcon = Icons.Default.Edit,
        )
        BrandCard {
            Column {
                Text("Class reminders", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Offsets before each class start",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                ReminderChips(state.classReminderMinutes, onClassReminders)
            }
        }
        BrandCard {
            Column {
                Text("Exam reminders", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Offsets before each exam start",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                ReminderChips(state.examReminderMinutes, onExamReminders)
            }
        }
        BrandCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Sync APU holidays", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Add APU's official holiday calendar to your device calendar",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = state.syncHolidays, onCheckedChange = onSyncHolidaysChange)
            }
        }
    }
}

@Composable
private fun ReminderChips(selected: List<Int>, onChange: (List<Int>) -> Unit) {
    val options = listOf(
        21 * 24 * 60 to "3 wk",
        14 * 24 * 60 to "2 wk",
        7 * 24 * 60 to "1 wk",
        3 * 24 * 60 to "3 d",
        2 * 24 * 60 to "2 d",
        24 * 60 to "1 d",
        6 * 60 to "6 h",
        3 * 60 to "3 h",
        2 * 60 to "2 h",
        60 to "1 h",
        30 to "30 m",
        15 to "15 m",
        10 to "10 m",
    )
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        options.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { (mins, label) ->
                    val isSelected = mins in selected
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            val next = if (isSelected) selected - mins else (selected + mins).distinct().sorted()
                            onChange(next)
                        },
                        label = { Text(label) },
                        shape = ChipCorner,
                    )
                }
            }
        }
    }
}

// ===== ADD-NOTE DIALOG ==================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddNoteDialog(
    initialEventKey: String? = null,
    initialModuleId: String? = null,
    onDismiss: () -> Unit,
    onSave: (Note) -> Unit,
) {
    var type by remember { mutableStateOf(NoteType.LECTURE) }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var typeMenuOpen by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New note") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExposedDropdownMenuBox(
                    expanded = typeMenuOpen,
                    onExpandedChange = { typeMenuOpen = it },
                ) {
                    TextField(
                        value = type.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeMenuOpen) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true),
                    )
                    ExposedDropdownMenu(
                        expanded = typeMenuOpen,
                        onDismissRequest = { typeMenuOpen = false },
                    ) {
                        NoteType.entries.forEach { t ->
                            DropdownMenuItem(
                                text = { Text(t.label) },
                                onClick = { type = t; typeMenuOpen = false },
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text("Notes / what to ask") },
                    minLines = 2,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank(),
                onClick = {
                    onSave(
                        Note(
                            type = type,
                            title = title.trim(),
                            body = body.trim(),
                            eventKey = initialEventKey,
                            moduleId = initialModuleId,
                        )
                    )
                },
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

// ===== DAY ACTIONS MENU + PERSONAL-EVENT CARD ===========================

@Composable
private fun DayActionsMenu(onAddNote: () -> Unit, onAddEvent: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        AssistChip(
            onClick = onAddEvent,
            label = { Text("Event") },
            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp)) },
            shape = ChipCorner,
        )
        AssistChip(
            onClick = onAddNote,
            label = { Text("Note") },
            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp)) },
            shape = ChipCorner,
        )
    }
}

@Composable
private fun PersonalEventCard(
    event: PersonalEvent,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    BrandCard(onClick = onEdit) {
        Column {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = ChipCorner,
                        ) {
                            Text(
                                event.type.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        event.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(2.dp))
                    val zone = ZoneId.systemDefault()
                    val s = java.time.Instant.ofEpochMilli(event.startMs).atZone(zone)
                    val e = java.time.Instant.ofEpochMilli(event.endMs).atZone(zone)
                    Text(
                        "${s.toLocalDate().format(DAY_DATE_FMT)} · ${s.format(TIME_FMT)} – ${e.format(TIME_FMT)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (!event.location.isNullOrBlank() || !event.lecturerName.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!event.location.isNullOrBlank()) {
                        Icon(Icons.Default.Place, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(event.location, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(10.dp))
                    }
                    if (!event.lecturerName.isNullOrBlank()) {
                        Icon(Icons.Default.AccountCircle, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(event.lecturerName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (event.notes.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(event.notes, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = onEdit,
                    label = { Text("Edit") },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp)) },
                )
                AssistChip(
                    onClick = onDelete,
                    label = { Text("Delete") },
                )
            }
        }
    }
}

// ===== PERSONAL EVENT DIALOG ============================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalEventDialog(
    initial: PersonalEvent?,
    initialDate: LocalDate,
    lecturerSearch: String,
    lecturerResults: List<Lecturer>,
    onLecturerSearch: (String) -> Unit,
    onSave: (PersonalEvent) -> Unit,
    onDismiss: () -> Unit,
) {
    val zone = ZoneId.systemDefault()
    var type by remember { mutableStateOf(initial?.type ?: PersonalEventType.EVENT) }
    var title by remember { mutableStateOf(initial?.title.orEmpty()) }
    var location by remember { mutableStateOf(initial?.location.orEmpty()) }
    var notes by remember { mutableStateOf(initial?.notes.orEmpty()) }
    var date by remember {
        mutableStateOf(
            initial?.startMs?.let { java.time.Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
                ?: initialDate,
        )
    }
    var startTime by remember {
        mutableStateOf(
            initial?.startMs?.let { java.time.Instant.ofEpochMilli(it).atZone(zone).toLocalTime() }
                ?: java.time.LocalTime.of(10, 0),
        )
    }
    var endTime by remember {
        mutableStateOf(
            initial?.endMs?.let { java.time.Instant.ofEpochMilli(it).atZone(zone).toLocalTime() }
                ?: startTime.plusHours(1),
        )
    }
    var lecturerCode by remember { mutableStateOf(initial?.lecturerCode) }
    var lecturerSam by remember { mutableStateOf(initial?.lecturerSam) }
    var lecturerName by remember { mutableStateOf(initial?.lecturerName.orEmpty()) }

    var showDate by remember { mutableStateOf(false) }
    var showStart by remember { mutableStateOf(false) }
    var showEnd by remember { mutableStateOf(false) }
    var showTypeMenu by remember { mutableStateOf(false) }
    var showLecturer by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New event" else "Edit event") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ExposedDropdownMenuBox(
                    expanded = showTypeMenu,
                    onExpandedChange = { showTypeMenu = it },
                ) {
                    TextField(
                        value = type.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(showTypeMenu) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true),
                    )
                    ExposedDropdownMenu(
                        expanded = showTypeMenu,
                        onDismissRequest = { showTypeMenu = false },
                    ) {
                        PersonalEventType.entries.forEach { t ->
                            DropdownMenuItem(
                                text = { Text(t.label) },
                                onClick = { type = t; showTypeMenu = false },
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title (e.g. Maths replacement)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                EditFieldRow(
                    label = "Date",
                    value = date.format(LONG_DATE_FMT),
                    onClick = { showDate = true },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EditFieldRow(
                        label = "Start",
                        value = startTime.format(TIME_FMT),
                        onClick = { showStart = true },
                        modifier = Modifier.weight(1f),
                    )
                    EditFieldRow(
                        label = "End",
                        value = endTime.format(TIME_FMT),
                        onClick = { showEnd = true },
                        modifier = Modifier.weight(1f),
                    )
                }
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location / room") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (type == PersonalEventType.APPOINTMENT) {
                    EditFieldRow(
                        label = "Lecturer",
                        value = lecturerName.ifBlank { "Tap to pick" },
                        onClick = { showLecturer = true },
                    )
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    minLines = 2,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank(),
                onClick = {
                    val startMs = ZonedDateTime.of(date, startTime, zone).toInstant().toEpochMilli()
                    val endMs = ZonedDateTime.of(date, endTime, zone).toInstant().toEpochMilli()
                    onSave(
                        (initial ?: PersonalEvent(
                            type = type, title = title.trim(),
                            startMs = startMs, endMs = endMs,
                        )).copy(
                            type = type,
                            title = title.trim(),
                            location = location.takeIf { it.isNotBlank() },
                            notes = notes.trim(),
                            startMs = startMs,
                            endMs = endMs,
                            lecturerCode = lecturerCode,
                            lecturerSam = lecturerSam,
                            lecturerName = lecturerName.takeIf { it.isNotBlank() },
                        )
                    )
                },
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )

    if (showDate) {
        val datePickerState = androidx.compose.material3.rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(zone).toInstant().toEpochMilli(),
        )
        androidx.compose.material3.DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { ms ->
                        date = java.time.Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()
                    }
                    showDate = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text("Cancel") } },
        ) {
            androidx.compose.material3.DatePicker(state = datePickerState)
        }
    }
    if (showStart) {
        TimePickerDialog(
            initialHour = startTime.hour,
            initialMinute = startTime.minute,
            onConfirm = { h, m ->
                startTime = java.time.LocalTime.of(h, m)
                if (!endTime.isAfter(startTime)) endTime = startTime.plusHours(1)
                showStart = false
            },
            onDismiss = { showStart = false },
        )
    }
    if (showEnd) {
        TimePickerDialog(
            initialHour = endTime.hour,
            initialMinute = endTime.minute,
            onConfirm = { h, m ->
                endTime = java.time.LocalTime.of(h, m)
                showEnd = false
            },
            onDismiss = { showEnd = false },
        )
    }
    if (showLecturer) {
        LecturerPickerDialog(
            query = lecturerSearch,
            results = lecturerResults,
            onQuery = onLecturerSearch,
            onPick = { lect ->
                lecturerCode = lect.code
                lecturerSam = lect.id
                lecturerName = lect.fullName
                showLecturer = false
            },
            onDismiss = { showLecturer = false },
        )
    }
}

// ===== EDIT-CLASS DIALOG ================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditClassDialog(
    entry: TimetableEntry,
    existingOverride: ClassOverride?,
    lecturerSearch: String,
    lecturerResults: List<Lecturer>,
    onLecturerSearch: (String) -> Unit,
    onSave: (ClassOverride) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    val zone = ZoneId.systemDefault()
    val initialStart = remember(entry, existingOverride) {
        runCatching {
            OffsetDateTime.parse(existingOverride?.timeFromIso ?: entry.timeFromIso)
                .atZoneSameInstant(zone)
        }.getOrNull() ?: ZonedDateTime.now(zone)
    }
    val initialEnd = remember(entry, existingOverride) {
        runCatching {
            OffsetDateTime.parse(existingOverride?.timeToIso ?: entry.timeToIso)
                .atZoneSameInstant(zone)
        }.getOrNull() ?: initialStart.plusHours(1)
    }

    var date by remember { mutableStateOf(initialStart.toLocalDate()) }
    var startTime by remember { mutableStateOf(initialStart.toLocalTime()) }
    var endTime by remember { mutableStateOf(initialEnd.toLocalTime()) }
    var room by remember { mutableStateOf(existingOverride?.room ?: entry.room.orEmpty()) }
    var lecturerCode by remember { mutableStateOf(existingOverride?.lecturerCode ?: entry.lecturerId) }
    var lecturerSam by remember { mutableStateOf(existingOverride?.lecturerSam ?: entry.lecturerAccount) }
    var lecturerName by remember {
        mutableStateOf(existingOverride?.lecturerName ?: entry.lecturerName.orEmpty())
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var showStartTime by remember { mutableStateOf(false) }
    var showEndTime by remember { mutableStateOf(false) }
    var showLecturerPicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit class", maxLines = 1) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    entry.moduleName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )

                EditFieldRow(
                    label = "Date",
                    value = date.format(LONG_DATE_FMT),
                    onClick = { showDatePicker = true },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EditFieldRow(
                        label = "Start",
                        value = startTime.format(TIME_FMT),
                        onClick = { showStartTime = true },
                        modifier = Modifier.weight(1f),
                    )
                    EditFieldRow(
                        label = "End",
                        value = endTime.format(TIME_FMT),
                        onClick = { showEndTime = true },
                        modifier = Modifier.weight(1f),
                    )
                }
                OutlinedTextField(
                    value = room,
                    onValueChange = { room = it },
                    label = { Text("Room / location") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                EditFieldRow(
                    label = "Lecturer",
                    value = lecturerName.ifBlank { "Tap to pick" },
                    onClick = { showLecturerPicker = true },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val classCode = entry.classCode ?: return@TextButton
                val newStart = ZonedDateTime.of(date, startTime, zone).toOffsetDateTime().toString()
                val newEnd = ZonedDateTime.of(date, endTime, zone).toOffsetDateTime().toString()
                onSave(
                    ClassOverride(
                        classCode = classCode,
                        timeFromIso = newStart,
                        timeToIso = newEnd,
                        room = room.takeIf { it.isNotBlank() },
                        lecturerCode = lecturerCode?.takeIf { it.isNotBlank() },
                        lecturerSam = lecturerSam?.takeIf { it.isNotBlank() },
                        lecturerName = lecturerName.takeIf { it.isNotBlank() },
                    )
                )
            }) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (existingOverride != null) {
                    TextButton(onClick = onClear) { Text("Reset") }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )

    if (showDatePicker) {
        val datePickerState = androidx.compose.material3.rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(zone).toInstant().toEpochMilli(),
        )
        androidx.compose.material3.DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { ms ->
                        date = java.time.Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
        ) {
            androidx.compose.material3.DatePicker(state = datePickerState)
        }
    }

    if (showStartTime) {
        TimePickerDialog(
            initialHour = startTime.hour,
            initialMinute = startTime.minute,
            onConfirm = { h, m ->
                startTime = java.time.LocalTime.of(h, m)
                if (!endTime.isAfter(startTime)) endTime = startTime.plusHours(1)
                showStartTime = false
            },
            onDismiss = { showStartTime = false },
        )
    }
    if (showEndTime) {
        TimePickerDialog(
            initialHour = endTime.hour,
            initialMinute = endTime.minute,
            onConfirm = { h, m ->
                endTime = java.time.LocalTime.of(h, m)
                showEndTime = false
            },
            onDismiss = { showEndTime = false },
        )
    }
    if (showLecturerPicker) {
        LecturerPickerDialog(
            query = lecturerSearch,
            results = lecturerResults,
            onQuery = onLecturerSearch,
            onPick = { lect ->
                lecturerCode = lect.code
                lecturerSam = lect.id
                lecturerName = lect.fullName
                showLecturerPicker = false
            },
            onDismiss = { showLecturerPicker = false },
        )
    }
}

@Composable
private fun EditFieldRow(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = androidx.compose.material3.rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = false,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pick time") },
        text = { androidx.compose.material3.TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun LecturerPickerDialog(
    query: String,
    results: List<Lecturer>,
    onQuery: (String) -> Unit,
    onPick: (Lecturer) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pick lecturer") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQuery,
                    label = { Text("Search by name, code, department") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier.heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    items(results.take(80), key = { it.refNo ?: (it.fullName + it.code) }) { lect ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPick(lect) }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                        ) {
                            AvatarOrPlaceholder(photo = lect.photo, fallback = lect.fullName)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    lect.fullName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    listOfNotNull(lect.title, lect.code).joinToString(" · "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

// ===== CHANGE-INTAKE DIALOG =============================================

@Composable
fun ChangeIntakeDialog(
    currentIntake: String,
    currentGroups: Set<String>,
    suggestions: List<String>,
    availableGroups: List<String>,
    onIntakeChange: (String) -> Unit,
    onPickSuggestion: (String) -> Unit,
    onToggleGroup: (String) -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change intake & groups") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Pick a new intake code or adjust the groups you attend. Changes are saved when you apply.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                IntakeAutocompleteField(
                    value = currentIntake,
                    suggestions = suggestions,
                    onValueChange = onIntakeChange,
                    onSuggestionPicked = onPickSuggestion,
                )
                if (availableGroups.isNotEmpty()) {
                    Text(
                        "Groups",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        availableGroups.forEach { g ->
                            FilterChip(
                                selected = g in currentGroups,
                                onClick = { onToggleGroup(g) },
                                label = { Text(g) },
                                shape = ChipCorner,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onApply) { Text("Apply") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

// ===== LECTURER QUICK-INFO DIALOG =======================================

@Composable
fun LecturerInfoDialog(
    code: String?,
    sam: String?,
    lookupByCode: suspend (String?) -> Lecturer?,
    lookupBySam: suspend (String?) -> Lecturer?,
    onOpenInDirectory: (Lecturer) -> Unit,
    onDismiss: () -> Unit,
) {
    val lecturer by produceState<Lecturer?>(initialValue = null, code, sam) {
        value = lookupByCode(code) ?: lookupBySam(sam)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Lecturer") },
        text = {
            val l = lecturer
            if (l == null) {
                Text(if (code == null && sam == null) "No lecturer info." else "Looking up…")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AvatarOrPlaceholder(l.photo, l.fullName)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(l.fullName, fontWeight = FontWeight.Bold)
                            l.title?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    l.department?.let { MetaRow("Dept", it) }
                    l.email?.let { MetaRow("Email", it) }
                    l.location?.let { MetaRow("Office", it) }
                }
            }
        },
        confirmButton = {
            val l = lecturer
            TextButton(
                enabled = l != null,
                onClick = { l?.let(onOpenInDirectory) },
            ) { Text("Open in directory") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}
