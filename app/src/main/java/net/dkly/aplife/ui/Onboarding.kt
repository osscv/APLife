package net.dkly.aplife.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.dkly.aplife.calendar.DeviceCalendar

// =====================================================================
// Onboarding host — drives the multi-step wizard.
// =====================================================================

private enum class OnboardStep { Welcome, Permissions, Intake, Groups, Calendar, Reminder }

private object OnboardingPermissions {
    fun calendarGranted(context: android.content.Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED
}

@Composable
fun OnboardingFlow(
    viewModel: AppViewModel,
    onFinished: () -> Unit,
) {
    val schedule by viewModel.schedule.collectAsStateWithLifecycle()
    val allIntakes by viewModel.allIntakes.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    var step by remember { mutableStateOf(OnboardStep.Welcome) }
    var search by remember { mutableStateOf("") }
    val checkedGroups = remember { androidx.compose.runtime.mutableStateMapOf<String, Boolean>() }
    var simpleReminder by remember { mutableIntStateOf(15) }

    val context = LocalContext.current
    var calendarPermissionGranted by remember {
        mutableStateOf(OnboardingPermissions.calendarGranted(context))
    }

    // Initialise checkedGroups when groups arrive
    LaunchedEffect(schedule.availableGroups) {
        if (checkedGroups.isEmpty() && schedule.availableGroups.isNotEmpty()) {
            schedule.selectedGroups.forEach { checkedGroups[it] = true }
        }
    }

    // Refresh calendar list when entering the Calendar step (only if permission granted)
    LaunchedEffect(step, calendarPermissionGranted) {
        if (step == OnboardStep.Calendar && calendarPermissionGranted && !schedule.calendarsLoaded) {
            viewModel.loadCalendars()
        }
    }

    // Safety net: if calendar permission gets revoked (e.g. via system Settings) while
    // the user is past the Permissions step, snap them back so they re-grant before continuing.
    LaunchedEffect(step, calendarPermissionGranted) {
        if (!calendarPermissionGranted && step.ordinal > OnboardStep.Permissions.ordinal) {
            step = OnboardStep.Permissions
        }
    }

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(WindowInsets.systemBars.asPaddingValues()),
        ) {
            Box(modifier = Modifier.weight(1f)) {
                when (step) {
                    OnboardStep.Welcome -> WelcomeScreen()
                    OnboardStep.Permissions -> PermissionsScreen(
                        onCalendarGrantedChange = { calendarPermissionGranted = it },
                    )
                    OnboardStep.Intake -> IntakeSearchScreen(
                        query = search,
                        onQueryChange = { search = it.uppercase() },
                        suggestions = filterIntakes(allIntakes, search),
                        onPick = { code ->
                            viewModel.setIntakeAndLoad(code)
                            step = OnboardStep.Groups
                        },
                    )
                    OnboardStep.Groups -> GroupsScreen(
                        intakeCode = schedule.intakeCode,
                        availableGroups = schedule.availableGroups,
                        checked = checkedGroups,
                        isLoading = schedule.isLoading,
                    )
                    OnboardStep.Calendar -> CalendarPickerScreen(
                        calendars = schedule.calendars,
                        selectedIds = schedule.selectedCalendarIds,
                        loaded = schedule.calendarsLoaded,
                        isLoading = schedule.isLoadingCalendars,
                        permissionGranted = calendarPermissionGranted,
                        loadError = schedule.calendarLoadError,
                        onRefresh = { viewModel.loadCalendars() },
                        onToggle = viewModel::toggleCalendar,
                    )
                    OnboardStep.Reminder -> ReminderScreen(
                        selected = simpleReminder,
                        onSelect = { simpleReminder = it },
                    )
                }
            }

            BottomBar(
                step = step,
                schedule = schedule,
                checkedGroups = checkedGroups,
                calendarPermissionGranted = calendarPermissionGranted,
                onBack = {
                    step = when (step) {
                        OnboardStep.Welcome -> OnboardStep.Welcome
                        OnboardStep.Permissions -> OnboardStep.Welcome
                        OnboardStep.Intake -> OnboardStep.Permissions
                        OnboardStep.Groups -> OnboardStep.Intake
                        OnboardStep.Calendar -> OnboardStep.Groups
                        OnboardStep.Reminder -> OnboardStep.Calendar
                    }
                },
                onForward = {
                    when (step) {
                        OnboardStep.Welcome -> step = OnboardStep.Permissions
                        OnboardStep.Permissions -> step = OnboardStep.Intake
                        OnboardStep.Intake -> step = OnboardStep.Groups
                        OnboardStep.Groups -> {
                            val groups = checkedGroups.filterValues { it }.keys
                            viewModel.setGroups(groups)
                            step = OnboardStep.Calendar
                        }
                        OnboardStep.Calendar -> step = OnboardStep.Reminder
                        OnboardStep.Reminder -> {
                            val mins = if (simpleReminder <= 0) emptyList() else listOf(simpleReminder)
                            viewModel.setClassReminders(mins)
                            viewModel.completeOnboarding()
                            viewModel.syncToCalendar(includeExams = true)
                            onFinished()
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun BottomBar(
    step: OnboardStep,
    schedule: ScheduleState,
    checkedGroups: SnapshotStateMap<String, Boolean>,
    calendarPermissionGranted: Boolean,
    onBack: () -> Unit,
    onForward: () -> Unit,
) {
    val canForward by remember(step, schedule, checkedGroups.toMap(), calendarPermissionGranted) {
        derivedStateOf {
            when (step) {
                OnboardStep.Welcome, OnboardStep.Reminder -> true
                OnboardStep.Permissions -> calendarPermissionGranted
                OnboardStep.Intake -> schedule.intakeCode.isNotBlank()
                OnboardStep.Groups -> checkedGroups.any { it.value } || schedule.availableGroups.isEmpty()
                OnboardStep.Calendar -> schedule.selectedCalendarIds.isNotEmpty()
            }
        }
    }
    val label = when (step) {
        OnboardStep.Welcome -> "Get started"
        OnboardStep.Reminder -> "Finish & sync"
        else -> "Continue"
    }

    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        if (step != OnboardStep.Welcome) {
            TextButton(onClick = onBack) { Text("Back") }
            Spacer(Modifier.height(4.dp))
        }
        Button(
            onClick = onForward,
            enabled = canForward,
            shape = RoundedCornerShape(16.dp),
            colors = brandButtonColors(),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
        ) {
            if (step == OnboardStep.Reminder) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ---- Step 1: Welcome ----------------------------------------------------

@Composable
private fun WelcomeScreen() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(48.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp)),
        ) {
            Icon(
                Icons.Default.DateRange,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp),
            )
        }
        Spacer(Modifier.height(20.dp))
        Text("APLife", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            "Your APU timetable, in your phone calendar.\nAlways up to date.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        FeatureRow(Icons.Default.Refresh, "Live from APU", "Reads your timetable straight from APU, no login.")
        FeatureRow(Icons.Default.CheckCircle, "Auto-sync daily", "Room changes, exams and holidays update by themselves.")
        FeatureRow(Icons.Default.Notifications, "Change alerts", "Get notified the moment your timetable changes.")
        FeatureRow(Icons.Default.Star, "Notes per class", "Class tests, presentations, appointments — kept with the date.")
    }
}

@Composable
private fun FeatureRow(icon: ImageVector, title: String, body: String) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ---- Step 2: Permissions ------------------------------------------------

@Composable
private fun PermissionsScreen(
    onCalendarGrantedChange: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    var calendarGranted by remember {
        mutableStateOf(OnboardingPermissions.calendarGranted(context))
    }
    LaunchedEffect(calendarGranted) { onCalendarGrantedChange(calendarGranted) }
    var notifGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= 33) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
            } else true,
        )
    }
    var audioGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }

    val calendarLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        calendarGranted = result[Manifest.permission.WRITE_CALENDAR] == true &&
            result[Manifest.permission.READ_CALENDAR] == true
    }
    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> notifGranted = granted }
    val audioLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> audioGranted = granted }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
    ) {
        item {
            Spacer(Modifier.height(16.dp))
            Text("Permissions", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(8.dp))
            Text(
                "APLife needs a couple of things to work.",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                "We ask once. You can change them anytime in Android Settings.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))
        }
        item {
            PermissionCard(
                icon = Icons.Default.DateRange,
                title = "Calendar",
                requiredChip = true,
                description = "Required. Lets APLife write your classes into your phone calendar.",
                granted = calendarGranted,
                onRequest = {
                    calendarLauncher.launch(
                        arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR),
                    )
                },
            )
            Spacer(Modifier.height(12.dp))
        }
        item {
            PermissionCard(
                icon = Icons.Default.Notifications,
                title = "Notifications",
                requiredChip = false,
                description = "Optional. Tells you when classes get rescheduled or cancelled.",
                granted = notifGranted,
                onRequest = {
                    if (Build.VERSION.SDK_INT >= 33) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    else notifGranted = true
                },
            )
            Spacer(Modifier.height(12.dp))
        }
        item {
            PermissionCard(
                icon = Icons.Default.Edit,
                title = "Audio",
                requiredChip = false,
                description = "Optional. Lets you attach a voice memo to a note.",
                granted = audioGranted,
                onRequest = { audioLauncher.launch(Manifest.permission.RECORD_AUDIO) },
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PermissionCard(
    icon: ImageVector,
    title: String,
    requiredChip: Boolean,
    description: String,
    granted: Boolean,
    onRequest: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !granted) { onRequest() },
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.padding(16.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (requiredChip) {
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(999.dp),
                        ) {
                            Text(
                                "REQUIRED",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (granted) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = net.dkly.aplife.ui.theme.SuccessGreen,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Allowed",
                            style = MaterialTheme.typography.labelMedium,
                            color = net.dkly.aplife.ui.theme.SuccessGreen,
                            fontWeight = FontWeight.SemiBold,
                        )
                    } else {
                        Text(
                            "Tap to allow",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

// ---- Step 3: Intake search ---------------------------------------------

@Composable
private fun IntakeSearchScreen(
    query: String,
    onQueryChange: (String) -> Unit,
    suggestions: List<String>,
    onPick: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        Text("Select your intake", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            placeholder = { Text("Search intake (e.g. APD2F)") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
        if (suggestions.isEmpty() && query.isNotBlank()) {
            Text(
                "No matches",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(suggestions, key = { it }) { code ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPick(code) }
                        .padding(vertical = 12.dp),
                ) {
                    Text(code, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

// ---- Step 4: Group selection (multi) ------------------------------------

@Composable
private fun GroupsScreen(
    intakeCode: String,
    availableGroups: List<String>,
    checked: SnapshotStateMap<String, Boolean>,
    isLoading: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        Text("Select your groups", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(2.dp))
        Text(intakeCode, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(12.dp))
        Text(
            "Tick every group you attend.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        if (isLoading && availableGroups.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (availableGroups.isEmpty()) {
            Spacer(Modifier.height(20.dp))
            Text(
                "No groups detected for this intake — your timetable doesn't split into groups, so just continue.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyColumn {
                items(availableGroups, key = { it }) { group ->
                    val isChecked = checked[group] ?: false
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { checked[group] = !isChecked }
                            .padding(vertical = 14.dp),
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(group, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                groupHint(group),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { checked[group] = it },
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                }
                item {
                    Spacer(Modifier.height(16.dp))
                    Text("Not sure?", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "Select your specific Tutorial/Lab group. If in doubt, check your APSpace timetable first.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

private fun groupHint(g: String): String = when (g.uppercase()) {
    "G1" -> "Tutorial / Lab Group 1"
    "G2" -> "Tutorial / Lab Group 2"
    "G3" -> "Tutorial / Lab Group 3"
    "G4" -> "Tutorial / Lab Group 4"
    "L" -> "Lecture (whole intake)"
    else -> "Group $g"
}

// ---- Step 5: Calendar picker -------------------------------------------

@Composable
private fun CalendarPickerScreen(
    calendars: List<DeviceCalendar>,
    selectedIds: Set<Long>,
    loaded: Boolean,
    isLoading: Boolean,
    permissionGranted: Boolean,
    loadError: String?,
    onRefresh: () -> Unit,
    onToggle: (Long) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
    ) {
        item {
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Choose a calendar", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.weight(1f))
                TextButton(onClick = onRefresh, enabled = !isLoading && permissionGranted) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (isLoading) "Loading…" else "Refresh")
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Classes will be written to every calendar you tick. Pick more than one if you want APLife to write to your phone calendar AND your Google/Outlook calendar at the same time.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "To add a Google or Outlook account, open Android Settings → Accounts → Add account.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }

        when {
            !permissionGranted || (isLoading && !loaded) -> {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
            loaded && calendars.isEmpty() -> {
                item {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "No writable calendars found on this device.",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Add a Google or Outlook account in Android Settings → Accounts, or open the Calendar app once and create a local calendar, then come back and tap Refresh.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (!loadError.isNullOrBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            loadError,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
            else -> {
                calendars.groupBy { it.accountName }.forEach { (account, list) ->
                    item {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            account.ifBlank { "Local" },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                    items(list, key = { it.id }) { cal ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggle(cal.id) }
                                .padding(vertical = 12.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                cal.displayName.ifBlank { account.ifBlank { "Calendar #${cal.id}" } },
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f),
                            )
                            Checkbox(
                                checked = cal.id in selectedIds,
                                onCheckedChange = { onToggle(cal.id) },
                            )
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

// ---- Step 6: Reminder choice -------------------------------------------

private val ReminderOptions: List<Pair<Int, String>> = listOf(
    0 to "Off",
    5 to "5 minutes before",
    10 to "10 minutes before",
    15 to "15 minutes before",
    30 to "30 minutes before",
    60 to "1 hour before",
    120 to "2 hours before",
)

@Composable
private fun ReminderScreen(
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
    ) {
        item {
            Spacer(Modifier.height(16.dp))
            Text("Class reminders", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(12.dp))
            Text("How early should your calendar remind you?", style = MaterialTheme.typography.titleMedium)
            Text(
                "Your phone will send an alert before each class. You can change this anytime in Settings.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))
        }
        items(ReminderOptions) { (value, label) ->
            val isSelected = selected == value
            val recommended = value == 15
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .let { if (isSelected) it.background(MaterialTheme.colorScheme.primaryContainer) else it }
                    .clickable { onSelect(value) }
                    .padding(horizontal = 12.dp, vertical = 12.dp),
            ) {
                RadioButton(selected = isSelected, onClick = { onSelect(value) })
                Spacer(Modifier.width(8.dp))
                Text(
                    label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                if (recommended) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(999.dp),
                    ) {
                        Text(
                            "Recommended",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

// =====================================================================
// Helpers
// =====================================================================

private fun filterIntakes(all: List<String>, query: String): List<String> {
    if (all.isEmpty()) return emptyList()
    val q = query.trim().uppercase()
    if (q.isBlank()) return all.take(200)
    val starts = all.filter { it.startsWith(q) }
    val contains = all.filter { it.contains(q) && !it.startsWith(q) }
    return (starts + contains).take(80)
}
