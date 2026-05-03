package net.dkly.aplife

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import net.dkly.aplife.data.TimetableEntry
import net.dkly.aplife.notes.Note
import net.dkly.aplife.data.PersonalEvent
import net.dkly.aplife.ui.AddNoteDialog
import net.dkly.aplife.ui.AppViewModel
import net.dkly.aplife.ui.ChangeIntakeDialog
import net.dkly.aplife.ui.DepartmentsTab
import net.dkly.aplife.ui.EditClassDialog
import net.dkly.aplife.ui.PersonalEventDialog
import net.dkly.aplife.ui.LecturerInfoDialog
import net.dkly.aplife.ui.LecturersTab
import net.dkly.aplife.ui.NotesTab
import net.dkly.aplife.ui.OnboardingFlow
import net.dkly.aplife.ui.ScheduleTab
import net.dkly.aplife.ui.SettingsTab
import net.dkly.aplife.ui.TransportTab
import net.dkly.aplife.ui.theme.APLifeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            APLifeTheme {
                AppRoot()
            }
        }
    }
}

private enum class Tab(val label: String, val title: String, val icon: ImageVector) {
    Schedule("Schedule", "Schedule", Icons.Default.DateRange),
    Notes("Notes", "Notes", Icons.Default.Edit),
    Lecturers("Staff", "Lecturer Directory", Icons.Default.AccountCircle),
    Departments("Depts", "Departments", Icons.Default.Home),
    Transport("Bus", "Shuttle", Icons.Default.LocationOn),
    Settings("Settings", "Settings", Icons.Default.Settings),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppRoot() {
    val viewModel: AppViewModel = viewModel()
    val schedule by viewModel.schedule.collectAsStateWithLifecycle()
    val intakeSuggestions by viewModel.intakeSuggestions.collectAsStateWithLifecycle()
    val lecturer by viewModel.lecturer.collectAsStateWithLifecycle()
    val departments by viewModel.departments.collectAsStateWithLifecycle()
    val transport by viewModel.transport.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val personalEvents by viewModel.personalEvents.collectAsStateWithLifecycle()

    if (!schedule.onboardingComplete) {
        OnboardingFlow(viewModel = viewModel, onFinished = { /* recompose: schedule.onboardingComplete = true */ })
        return
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarState = remember { SnackbarHostState() }

    var selectedTab by remember { mutableStateOf(Tab.Schedule) }
    var pendingNoteForClass by remember { mutableStateOf<TimetableEntry?>(null) }
    var pendingNoteFromButton by remember { mutableStateOf(false) }
    var pendingLecturerLookup by remember { mutableStateOf<TimetableEntry?>(null) }
    var showChangeIntake by remember { mutableStateOf(false) }
    var pendingEditClass by remember { mutableStateOf<TimetableEntry?>(null) }
    var pendingEventEdit by remember { mutableStateOf<Pair<PersonalEvent?, java.time.LocalDate>?>(null) }

    var hasCalendarPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) ==
                PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        val granted = result[Manifest.permission.WRITE_CALENDAR] == true &&
            result[Manifest.permission.READ_CALENDAR] == true
        hasCalendarPermission = granted
        if (granted) viewModel.loadCalendars()
    }

    LaunchedEffect(hasCalendarPermission) {
        if (hasCalendarPermission && schedule.calendars.isEmpty()) viewModel.loadCalendars()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarState) },
        topBar = {
            val showBack = selectedTab == Tab.Lecturers && lecturer.selected != null
            if (showBack) {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            lecturer.selected?.fullName ?: selectedTab.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.clearSelectedLecturer() }) {
                            androidx.compose.material3.Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                )
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
            ) {
                Tab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            when (selectedTab) {
                Tab.Schedule -> {
                    var pendingDateForNote by remember { mutableStateOf<java.time.LocalDate?>(null) }
                    ScheduleTab(
                        state = schedule,
                        settings = settings,
                        notes = notes,
                        personalEvents = personalEvents,
                        onResync = { viewModel.syncToCalendar(includeExams = true) },
                        onRefreshFromApu = { viewModel.loadIntake(forceRefresh = true) },
                        onChangeIntakeRequested = { showChangeIntake = true },
                        onAddNoteForClass = { entry -> pendingNoteForClass = entry },
                        onAddNoteForDate = { date -> pendingDateForNote = date },
                        onAddEventForDate = { date -> pendingEventEdit = null to date },
                        onEditPersonalEvent = { event ->
                            pendingEventEdit = event to java.time.Instant.ofEpochMilli(event.startMs)
                                .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                        },
                        onDeletePersonalEvent = viewModel::deletePersonalEvent,
                        onLecturerCardTap = { entry -> pendingLecturerLookup = entry },
                        onEditClass = { entry -> pendingEditClass = entry },
                        onDeleteNote = viewModel::deleteNote,
                        contentPadding = padding,
                    )
                    pendingDateForNote?.let { date ->
                        net.dkly.aplife.ui.AddNoteDialog(
                            onDismiss = { pendingDateForNote = null },
                            onSave = { note ->
                                val zone = java.time.ZoneId.systemDefault()
                                viewModel.saveNote(
                                    note.copy(
                                        dueAtMs = note.dueAtMs ?: date.atStartOfDay(zone).toInstant().toEpochMilli(),
                                    )
                                )
                                pendingDateForNote = null
                                scope.launch { snackbarState.showSnackbar("Note saved.") }
                            },
                        )
                    }
                }
                Tab.Notes -> NotesTab(
                    notes = notes,
                    onAdd = { pendingNoteFromButton = true },
                    onDelete = viewModel::deleteNote,
                    contentPadding = padding,
                )
                Tab.Lecturers -> {
                    LaunchedEffect(Unit) {
                        if (lecturer.results.isEmpty() && lecturer.query.isBlank()) {
                            viewModel.searchLecturers("")
                        }
                    }
                    LecturersTab(
                        state = lecturer,
                        onSearch = viewModel::searchLecturers,
                        onPick = viewModel::selectLecturer,
                        onClearSelected = viewModel::clearSelectedLecturer,
                        contentPadding = padding,
                    )
                }
                Tab.Departments -> {
                    LaunchedEffect(Unit) {
                        if (departments.items.isEmpty() && !departments.isLoading) {
                            viewModel.loadDepartments(forceRefresh = false)
                        }
                    }
                    val repo = remember { viewModel.departmentRepository() }
                    DepartmentsTab(
                        state = departments,
                        onRefresh = { viewModel.loadDepartments(forceRefresh = true) },
                        isOpenNow = { repo.isOpen(it) },
                        todayShifts = { repo.shiftsForDay(it, java.time.LocalDate.now().dayOfWeek) },
                        contentPadding = padding,
                    )
                }
                Tab.Transport -> {
                    LaunchedEffect(Unit) {
                        if (transport.nextTrips.isEmpty() && !transport.isLoading) {
                            viewModel.loadTransport(forceRefresh = false)
                        }
                    }
                    TransportTab(
                        state = transport,
                        onRefresh = { viewModel.loadTransport(forceRefresh = true) },
                        contentPadding = padding,
                    )
                }
                Tab.Settings -> SettingsTab(
                    state = settings,
                    onClassReminders = viewModel::setClassReminders,
                    onExamReminders = viewModel::setExamReminders,
                    onSyncHolidaysChange = viewModel::setSyncHolidays,
                    contentPadding = padding,
                )
            }
        }
    }

    pendingNoteForClass?.let { entry ->
        AddNoteDialog(
            initialEventKey = entry.classCode,
            initialModuleId = entry.moduleId,
            onDismiss = { pendingNoteForClass = null },
            onSave = { note ->
                viewModel.saveNote(
                    note.copy(
                        lecturerCode = entry.lecturerId,
                        lecturerSam = entry.lecturerAccount,
                        dueAtMs = note.dueAtMs ?: runCatching {
                            java.time.OffsetDateTime.parse(entry.timeFromIso).toInstant().toEpochMilli()
                        }.getOrNull(),
                    )
                )
                pendingNoteForClass = null
                scope.launch { snackbarState.showSnackbar("Note saved.") }
            },
        )
    }

    if (pendingNoteFromButton) {
        AddNoteDialog(
            onDismiss = { pendingNoteFromButton = false },
            onSave = { note ->
                viewModel.saveNote(note)
                pendingNoteFromButton = false
                scope.launch { snackbarState.showSnackbar("Note saved.") }
            },
        )
    }

    pendingEventEdit?.let { (existing, date) ->
        PersonalEventDialog(
            initial = existing,
            initialDate = date,
            lecturerSearch = lecturer.query,
            lecturerResults = lecturer.results,
            onLecturerSearch = viewModel::searchLecturers,
            onSave = { event ->
                viewModel.savePersonalEvent(event)
                pendingEventEdit = null
                scope.launch { snackbarState.showSnackbar(if (existing == null) "Event added." else "Event updated.") }
            },
            onDismiss = { pendingEventEdit = null },
        )
    }

    pendingEditClass?.let { entry ->
        // Reuse the directory's lecturer search state for the picker
        val classCode = entry.classCode
        val existing = if (classCode != null) {
            // Look up if there's already an override for this class
            // (we don't expose the override map directly — passing null is fine; dialog uses entry as base)
            null
        } else null
        EditClassDialog(
            entry = entry,
            existingOverride = existing,
            lecturerSearch = lecturer.query,
            lecturerResults = lecturer.results,
            onLecturerSearch = viewModel::searchLecturers,
            onSave = { override ->
                viewModel.setClassOverride(override)
                pendingEditClass = null
                scope.launch { snackbarState.showSnackbar("Class updated.") }
            },
            onClear = {
                if (classCode != null) viewModel.clearClassOverride(classCode)
                pendingEditClass = null
                scope.launch { snackbarState.showSnackbar("Reverted to APU's version.") }
            },
            onDismiss = { pendingEditClass = null },
        )
    }

    if (showChangeIntake) {
        ChangeIntakeDialog(
            currentIntake = schedule.intakeCode,
            currentGroups = schedule.selectedGroups,
            suggestions = intakeSuggestions,
            availableGroups = schedule.availableGroups,
            onIntakeChange = viewModel::onIntakeChange,
            onPickSuggestion = viewModel::pickIntakeSuggestion,
            onToggleGroup = viewModel::toggleGroup,
            onApply = {
                showChangeIntake = false
                viewModel.loadIntake(forceRefresh = true)
            },
            onDismiss = { showChangeIntake = false },
        )
    }

    pendingLecturerLookup?.let { entry ->
        LecturerInfoDialog(
            code = entry.lecturerId,
            sam = entry.lecturerAccount,
            lookupByCode = viewModel::lookupLecturerByCode,
            lookupBySam = viewModel::lookupLecturerBySam,
            onOpenInDirectory = { lect ->
                pendingLecturerLookup = null
                viewModel.selectLecturer(lect)
                selectedTab = Tab.Lecturers
            },
            onDismiss = { pendingLecturerLookup = null },
        )
    }

}
