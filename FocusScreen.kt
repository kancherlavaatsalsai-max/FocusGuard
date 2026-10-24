package com.focusguard.app.ui.focus

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focusguard.app.domain.model.SessionType
import com.focusguard.app.ui.theme.FocusPrimary
import com.focusguard.app.ui.theme.FocusSecondary
import com.focusguard.app.ui.theme.WarnAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusScreen(
    onBack: () -> Unit,
    viewModel: FocusViewModel = hiltViewModel(LocalContext.current as ComponentActivity)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isBreak) "☕ Break Time" else "🎯 Focus Session", fontWeight = FontWeight.Bold) },
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
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            if (state.completedCoins != null) {
                CompletedContent(coins = state.completedCoins!!, onDone = viewModel::dismissComplete)
            } else if (!state.isRunning && !state.isPaused) {
                // ── Setup UI ─────────────────────────────────────────────
                SessionSetupContent(
                    goal        = state.sessionGoal,
                    onGoalChange = viewModel::setGoal,
                    selectedType = state.selectedType,
                    onTypeChange = viewModel::setType,
                    customMin   = state.customMinutes,
                    onCustomMin = viewModel::setCustomMinutes,
                    onStart     = viewModel::startSession
                )
            } else {
                // ── Timer UI ─────────────────────────────────────────────
                TimerContent(
                    state    = state,
                    onPause  = viewModel::pauseSession,
                    onResume = viewModel::resumeSession,
                    onStop   = viewModel::stopSession,
                    onSkip   = viewModel::skipBreak
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionSetupContent(
    goal: String,
    onGoalChange: (String) -> Unit,
    selectedType: SessionType,
    onTypeChange: (SessionType) -> Unit,
    customMin: Int,
    onCustomMin: (Int) -> Unit,
    onStart: () -> Unit
) {
    // Goal input
    OutlinedTextField(
        value = goal,
        onValueChange = onGoalChange,
        label = { Text("What are you working on?") },
        placeholder = { Text("e.g., Study math chapter 3") },
        modifier = Modifier.fillMaxWidth(),
        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
        shape = RoundedCornerShape(12.dp)
    )

    // Session type
    Text("Session Type", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SessionType.values().forEach { type ->
            FilterChip(
                selected = selectedType == type,
                onClick  = { onTypeChange(type) },
                label    = { Text(type.label()) },
                leadingIcon = if (selectedType == type) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null
            )
        }
    }

    // Custom duration slider
    if (selectedType == SessionType.CUSTOM) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Duration", style = MaterialTheme.typography.bodyMedium)
                Text("$customMin minutes", color = FocusPrimary, fontWeight = FontWeight.SemiBold)
            }
            Slider(
                value         = customMin.toFloat(),
                onValueChange = { onCustomMin(it.toInt()) },
                valueRange    = 5f..180f,
                steps         = 34,
                colors        = SliderDefaults.colors(thumbColor = FocusPrimary, activeTrackColor = FocusPrimary)
            )
        }
    }

    // Info cards
    SessionInfoCard(selectedType, customMin)

    // Start button
    Button(
        onClick  = onStart,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape    = RoundedCornerShape(16.dp),
        colors   = ButtonDefaults.buttonColors(containerColor = FocusPrimary)
    ) {
        Icon(Icons.Default.PlayArrow, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Start Session", fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SessionInfoCard(type: SessionType, customMin: Int) {
    val (duration, breakTime, desc) = when (type) {
        SessionType.POMODORO  -> Triple(25, 5,  "Classic 25/5 Pomodoro technique")
        SessionType.DEEP_WORK -> Triple(90, 15, "Deep work for maximum productivity")
        SessionType.CUSTOM    -> Triple(customMin, 10, "Your custom focus duration")
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape    = RoundedCornerShape(16.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("⏱ $duration min focus", fontWeight = FontWeight.SemiBold, color = FocusPrimary)
                Text("☕ $breakTime min break", fontWeight = FontWeight.SemiBold, color = FocusSecondary)
                Text(desc, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun TimerContent(
    state: FocusUiState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onSkip: () -> Unit
) {
    val totalSeconds = if (state.isBreak)
        state.session.breakMinutes * 60
    else
        state.session.durationMinutes * 60

    val progress = 1f - (state.remainingSeconds.toFloat() / totalSeconds.toFloat())
    val color = if (state.isBreak) FocusSecondary else FocusPrimary

    // Pomodoro dots
    if (state.session.type == SessionType.POMODORO) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (1..state.totalPomodoros).forEach { i ->
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            if (i < state.currentPomodoro) FocusPrimary
                            else if (i == state.currentPomodoro) WarnAmber
                            else MaterialTheme.colorScheme.surfaceVariant,
                            CircleShape
                        )
                )
            }
        }
    }

    // Goal label
    if (state.sessionGoal.isNotBlank()) {
        Text(
            "📚 ${state.sessionGoal}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }

    // Circular timer
    Box(contentAlignment = Alignment.Center) {
        CircularProgressTimer(progress = progress, color = color, size = 260)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                state.remainingSeconds.formatAsTime(),
                fontSize    = 52.sp,
                fontWeight  = FontWeight.Bold,
                color       = color
            )
            Text(
                if (state.isBreak) "Break" else "Focus",
                fontSize = 16.sp,
                color    = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    // Controls
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        // Stop
        OutlinedIconButton(
            onClick = onStop,
            modifier = Modifier.size(64.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
        ) {
            Icon(Icons.Default.Stop, contentDescription = "Stop", tint = MaterialTheme.colorScheme.error)
        }

        // Pause/Resume
        FloatingActionButton(
            onClick            = if (state.isRunning) onPause else onResume,
            modifier           = Modifier.size(80.dp),
            containerColor     = color,
            contentColor       = Color.White,
            shape              = CircleShape
        ) {
            Icon(
                if (state.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (state.isRunning) "Pause" else "Resume",
                modifier           = Modifier.size(36.dp)
            )
        }

        // Skip break (only active during a break)
        OutlinedIconButton(
            onClick = onSkip,
            enabled = state.isBreak,
            modifier = Modifier.size(64.dp)
        ) {
            Icon(Icons.Default.SkipNext, contentDescription = "Skip break")
        }
    }

    if (state.isPaused) {
        Text("Paused", color = WarnAmber, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun CompletedContent(coins: Int, onDone: () -> Unit) {
    Spacer(Modifier.height(40.dp))
    Text("🎉", fontSize = 72.sp)
    Text("Session complete!", fontSize = 26.sp, fontWeight = FontWeight.Bold)
    Text("You earned 🪙 $coins focus coins", fontSize = 16.sp, color = FocusSecondary)
    Text(
        "Great work. Take a proper break before the next one.",
        color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center
    )
    Button(
        onClick = onDone,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = FocusPrimary)
    ) { Text("Done", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
}

@Composable
private fun CircularProgressTimer(progress: Float, color: Color, size: Int) {
    val animatedProgress by animateFloatAsState(
        targetValue    = progress,
        animationSpec  = tween(500),
        label          = "timer_progress"
    )
    androidx.compose.foundation.Canvas(
        modifier = Modifier.size(size.dp)
    ) {
        val stroke = 12.dp.toPx()
        // Track
        drawArc(
            color      = color.copy(alpha = 0.15f),
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter  = false,
            style      = Stroke(width = stroke, cap = StrokeCap.Round)
        )
        // Progress
        drawArc(
            color      = color,
            startAngle = -90f,
            sweepAngle = 360f * animatedProgress,
            useCenter  = false,
            style      = Stroke(width = stroke, cap = StrokeCap.Round)
        )
    }
}

private fun Int.formatAsTime(): String {
    val m = this / 60
    val s = this % 60
    return "%02d:%02d".format(m, s)
}

private fun SessionType.label() = when (this) {
    SessionType.POMODORO  -> "Pomodoro"
    SessionType.DEEP_WORK -> "Deep Work"
    SessionType.CUSTOM    -> "Custom"
}
