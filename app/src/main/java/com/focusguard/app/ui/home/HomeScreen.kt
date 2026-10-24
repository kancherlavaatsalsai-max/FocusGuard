package com.focusguard.app.ui.home

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focusguard.app.domain.model.toBrainState
import com.focusguard.app.ui.theme.BlockRed
import com.focusguard.app.ui.theme.FocusAccent
import com.focusguard.app.ui.theme.FocusPrimary
import com.focusguard.app.ui.theme.WarnAmber
import com.focusguard.app.util.PermissionUtils
import com.focusguard.app.util.WidgetStore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToFocus: () -> Unit,
    onNavigateToStats: () -> Unit,
    onNavigateToBlocker: () -> Unit,
    onNavigateToBattles: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val ui by viewModel.uiState.collectAsStateWithLifecycle()

    // Re-check permissions every time the user comes back to the app
    var tick by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) {
                tick++
                viewModel.refreshUsage()
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }
    val accessibilityOn = remember(tick) { PermissionUtils.isAccessibilityEnabled(context) }
    val usageOn = remember(tick) { PermissionUtils.hasUsageAccess(context) }
    val daysLeft = remember(tick) { WidgetStore.daysLeft(context) }
    val prepProgress = remember(tick) { WidgetStore.prepProgress(context) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("FocusGuard", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
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
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── CLAT countdown hero ──────────────────────────────────
            CountdownCard(daysLeft = daysLeft, progress = prepProgress)

            // ── Setup cards ──────────────────────────────────────────
            if (!accessibilityOn) {
                PermissionCard(
                    Icons.Default.Lock, "Turn on Accessibility",
                    "Needed for app blocking and the Reels counter. Find FocusGuard in the list and switch it on.",
                    BlockRed, "Enable"
                ) { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
            }
            if (!usageOn) {
                PermissionCard(
                    Icons.Default.BarChart, "Allow Usage Access",
                    "Needed to show your screen time per app.",
                    WarnAmber, "Allow"
                ) { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
            }

            // ── Brain state (BrainPal-style) ─────────────────────────
            BrainCard(reels = ui.reelsWatchedToday, limit = ui.reelLimit)

            // ── Stats ────────────────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard(Modifier.weight(1f), ui.todayFocusMinutes.fmtMin(), "Focus today", FocusPrimary, Icons.Default.Timer)
                StatCard(Modifier.weight(1f), (ui.screenTimeMs / 60000).fmtMin(), "Screen time", Color(0xFF26C6DA), Icons.Default.PhoneAndroid)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard(Modifier.weight(1f), "🔥 ${ui.streak}", "Day streak", WarnAmber, Icons.Default.LocalFireDepartment)
                StatCard(Modifier.weight(1f), "🪙 ${ui.coins}", "Focus coins", Color(0xFFFFD700), Icons.Default.Star)
            }

            // ── Quick actions ────────────────────────────────────────
            Text("Quick Actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                QuickAction(Modifier.weight(1f), Icons.Default.PlayArrow, "Start Focus",
                    listOf(FocusPrimary, Color(0xFF3949AB)), onNavigateToFocus)
                QuickAction(Modifier.weight(1f), Icons.Default.Block, "Block Apps",
                    listOf(BlockRed, Color(0xFFC62828)), onNavigateToBlocker)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                QuickAction(Modifier.weight(1f), Icons.Default.BarChart, "Statistics",
                    listOf(Color(0xFF26C6DA), Color(0xFF00897B)), onNavigateToStats)
                QuickAction(Modifier.weight(1f), Icons.Default.EmojiEvents, "Scroll Battles",
                    listOf(WarnAmber, Color(0xFFF57F17)), onNavigateToBattles)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun CountdownCard(daysLeft: Int, progress: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF3949AB), Color(0xFF5E35B1), Color(0xFF8E24AA))))
            .padding(22.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("CLAT COUNTDOWN", color = Color(0xCCFFFFFF), fontSize = 12.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("$daysLeft", color = Color.White, fontSize = 64.sp, fontWeight = FontWeight.Bold)
                Text(if (daysLeft == 1) "day left" else "days left", color = Color(0xE6FFFFFF),
                    fontSize = 18.sp, modifier = Modifier.padding(bottom = 12.dp))
            }
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = Color.White, trackColor = Color(0x33FFFFFF)
            )
            Text("Add the CLAT widget to your home screen to see this every time you unlock your phone.",
                color = Color(0xB3FFFFFF), fontSize = 11.sp)
        }
    }
}

@Composable
private fun BrainCard(reels: Int, limit: Int) {
    val state = reels.toBrainState()
    val color = Color(state.color)
    val progress = (reels.toFloat() / limit.coerceAtLeast(1)).coerceIn(0f, 1f)
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(Modifier.size(56.dp).background(color.copy(alpha = 0.15f), CircleShape), contentAlignment = Alignment.Center) {
                    Text(state.emoji, fontSize = 26.sp)
                }
                Column(Modifier.weight(1f)) {
                    Text(state.label, fontWeight = FontWeight.Bold, color = color, fontSize = 17.sp)
                    Text("$reels Reels/Shorts today · limit $limit", fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = color, trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
private fun PermissionCard(
    icon: ImageVector, title: String, desc: String, color: Color, action: String, onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, null, tint = color, modifier = Modifier.size(26.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, color = color, fontSize = 14.sp)
                Text(desc, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
            FilledTonalButton(onClick = onClick) { Text(action, fontSize = 12.sp) }
        }
    }
}

@Composable
private fun StatCard(modifier: Modifier, value: String, label: String, color: Color, icon: ImageVector) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = color)
            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun QuickAction(modifier: Modifier, icon: ImageVector, label: String, colors: List<Color>, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(80.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.horizontalGradient(colors))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, label, tint = Color.White, modifier = Modifier.size(26.dp))
            Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun Long.fmtMin(): String {
    val h = this / 60; val m = this % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}
