package com.focusguard.app.ui.settings

import android.content.Intent
import android.provider.Settings
import com.focusguard.app.util.WidgetStore
import com.focusguard.app.widget.ClatWidgets
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focusguard.app.ui.theme.BlockRed
import com.focusguard.app.ui.theme.FocusPrimary
import com.focusguard.app.ui.theme.WarnAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var examDate by remember { mutableStateOf(WidgetStore.examDate(context)) }
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = examDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { ms ->
                        val picked = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()
                        WidgetStore.setExamDate(context, picked)
                        examDate = picked
                        ClatWidgets.updateAll(context)
                    }
                    showDatePicker = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = pickerState) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Permissions ──────────────────────────────────────────
            SettingsSection(title = "Permissions") {
                SettingsNavItem(
                    icon    = Icons.Default.Accessibility,
                    label   = "Accessibility Service",
                    desc    = "Required for app blocking",
                    color   = FocusPrimary,
                    action  = "Open"
                ) { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }

                SettingsNavItem(
                    icon    = Icons.Default.BarChart,
                    label   = "Usage Access",
                    desc    = "Required for screen time tracking",
                    color   = FocusPrimary,
                    action  = "Open"
                ) { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
            }

            // ── CLAT exam ─────────────────────────────────────────────
            SettingsSection(title = "CLAT Countdown") {
                SettingsNavItem(
                    icon   = Icons.Default.Event,
                    label  = "Exam date",
                    desc   = examDate.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH)),
                    color  = FocusPrimary,
                    action = "Change"
                ) { showDatePicker = true }
                Text(
                    "Long-press your home screen → Widgets → FocusGuard to add the countdown widget.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }

            // ── Appearance ────────────────────────────────────────────
            SettingsSection(title = "Appearance") {
                SettingsSwitchItem(
                    icon = Icons.Default.DarkMode,
                    label = "Dark theme",
                    desc = "Easier on the eyes at night",
                    color = FocusPrimary,
                    checked = state.darkTheme,
                    onCheckedChange = viewModel::setDarkTheme
                )
            }

            // ── Focus Settings ───────────────────────────────────────
            SettingsSection(title = "Focus") {
                SettingsSwitchItem(
                    icon    = Icons.Default.Lock,
                    label   = "Strict Mode",
                    desc    = "Disables emergency unlock during sessions",
                    color   = BlockRed,
                    checked = state.strictMode,
                    onCheckedChange = viewModel::setStrictMode
                )
            }

            // ── Digital Wellbeing ────────────────────────────────────
            SettingsSection(title = "Digital Wellbeing") {
                // Reel limit slider
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = WarnAmber, modifier = Modifier.size(22.dp))
                            Column {
                                Text("Daily Reel Limit", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                Text("Alert when limit reached", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Text("${state.reelLimit} reels", color = WarnAmber, fontWeight = FontWeight.SemiBold)
                    }
                    Slider(
                        value         = state.reelLimit.toFloat(),
                        onValueChange = { viewModel.setReelLimit(it.toInt()) },
                        valueRange    = 5f..100f,
                        steps         = 18,
                        colors        = SliderDefaults.colors(thumbColor = WarnAmber, activeTrackColor = WarnAmber)
                    )
                }

                HorizontalDivider()

                SettingsSwitchItem(
                    icon    = Icons.Default.Bedtime,
                    label   = "Bedtime Mode",
                    desc    = "Block distracting apps after ${state.bedtimeHour}:00",
                    color   = FocusPrimary,
                    checked = state.bedtimeMode,
                    onCheckedChange = viewModel::setBedtimeMode
                )

                if (state.bedtimeMode) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(start = 8.dp)) {
                        Text("Bedtime Hour: ${state.bedtimeHour}:00", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Slider(
                            value         = state.bedtimeHour.toFloat(),
                            onValueChange = { viewModel.setBedtimeHour(it.toInt()) },
                            valueRange    = 18f..24f,
                            steps         = 5,
                            colors        = SliderDefaults.colors(thumbColor = FocusPrimary, activeTrackColor = FocusPrimary)
                        )
                        Text("Morning Unlock Hour: ${state.morningLockHour}:00", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Slider(
                            value         = state.morningLockHour.toFloat(),
                            onValueChange = { viewModel.setMorningLockHour(it.toInt()) },
                            valueRange    = 5f..12f,
                            steps         = 6,
                            colors        = SliderDefaults.colors(thumbColor = FocusPrimary, activeTrackColor = FocusPrimary)
                        )
                    }
                }
            }

            // ── About ─────────────────────────────────────────────────
            SettingsSection(title = "About") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape    = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("FocusGuard v1.0", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text("All Regain Pro features — 100% free, just for you.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("No ads. No subscriptions. No paywalls.", fontSize = 12.sp, color = FocusAccent)
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape    = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(4.dp), content = content)
    }
}

@Composable
private fun SettingsNavItem(
    icon: ImageVector, label: String, desc: String,
    color: Color, action: String, onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
            Text(desc, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        FilledTonalButton(onClick = onClick, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
            Text(action, fontSize = 12.sp)
        }
    }
}

@Composable
private fun SettingsSwitchItem(
    icon: ImageVector, label: String, desc: String,
    color: Color, checked: Boolean, onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
            Text(desc, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked         = checked,
            onCheckedChange = onCheckedChange,
            colors          = SwitchDefaults.colors(checkedTrackColor = color)
        )
    }
}

// Add import for FocusAccent
private val FocusAccent = Color(0xFF66BB6A)
