package com.focusguard.app.ui.stats

import androidx.compose.foundation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focusguard.app.domain.model.AppUsageStat
import com.focusguard.app.ui.theme.BlockRed
import com.focusguard.app.ui.theme.FocusAccent
import com.focusguard.app.ui.theme.FocusPrimary
import com.focusguard.app.ui.theme.FocusSecondary
import com.focusguard.app.ui.theme.WarnAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onBack: () -> Unit,
    viewModel: StatsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Today", "Reels", "Focus", "All Time")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📊 Statistics", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { i, tab ->
                    Tab(
                        selected = selectedTab == i,
                        onClick  = { selectedTab = i },
                        text     = { Text(tab, fontSize = 13.sp) }
                    )
                }
            }

            when (selectedTab) {
                0 -> TodayTab(state)
                1 -> ReelsTab(state)
                2 -> FocusTab(state)
                3 -> AllTimeTab(state)
            }
        }
    }
}

@Composable
private fun TodayTab(state: StatsUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Screen Time Breakdown", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        if (state.todayUsage.isEmpty()) {
            EmptyState("No usage data yet.\nGrant Usage Access in Settings → Apps → Special Access")
        } else {
            state.todayUsage.take(10).forEach { stat ->
                AppUsageRow(stat)
            }
        }
    }
}

@Composable
private fun ReelsTab(state: StatsUiState) {
    val over = state.reelsToday > state.reelLimit
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape    = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("📱", fontSize = 44.sp)
                Text(
                    "${state.reelsToday}",
                    fontSize = 64.sp, fontWeight = FontWeight.Bold,
                    color = if (over) BlockRed else FocusPrimary
                )
                Text("Reels & Shorts watched today", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "≈ ${state.reelsToday * 20 / 60} min on short videos · limit ${state.reelLimit}",
                    fontSize = 13.sp, color = WarnAmber
                )
            }
        }

        Text("This week", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        WeekBars(state.weeklyReels, state.reelLimit)

        Text("By platform (today)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (state.reelsByPlatform.isEmpty()) {
            EmptyState("Nothing counted yet.\nTurn on the FocusGuard accessibility service, then scroll some Reels.")
        } else {
            state.reelsByPlatform.forEach { (name, count) -> PlatformReelCard(name, count, FocusPrimary) }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("ℹ️ How we count", fontWeight = FontWeight.SemiBold)
                Text(
                    "Each swipe to the next video inside Instagram Reels, YouTube Shorts, TikTok or Snapchat " +
                        "Spotlight counts as one. Counts are close but not perfect, because those apps change " +
                        "their screens from time to time.",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun WeekBars(data: List<Pair<String, Int>>, limit: Int) {
    val max = maxOf(data.maxOfOrNull { it.second } ?: 0, limit, 1)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp).height(150.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            data.forEach { (label, count) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                    Text("$count", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Box(
                        Modifier
                            .width(24.dp)
                            .height((100f * count / max).coerceAtLeast(3f).dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (count > limit) BlockRed else FocusPrimary)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun FocusTab(state: StatsUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MiniStatCard(
                modifier = Modifier.weight(1f),
                value    = state.totalFocusMinutes.formatMinutes(),
                label    = "Total Focus",
                color    = FocusPrimary
            )
            MiniStatCard(
                modifier = Modifier.weight(1f),
                value    = "${state.totalSessions}",
                label    = "Sessions Done",
                color    = FocusAccent
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MiniStatCard(
                modifier = Modifier.weight(1f),
                value    = "🔥 ${state.currentStreak}",
                label    = "Current Streak",
                color    = WarnAmber
            )
            MiniStatCard(
                modifier = Modifier.weight(1f),
                value    = "⭐ ${state.longestStreak}",
                label    = "Best Streak",
                color    = FocusSecondary
            )
        }

        Text("Pro Tip", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors   = CardDefaults.cardColors(containerColor = FocusPrimary.copy(alpha = 0.1f)),
            shape    = RoundedCornerShape(12.dp)
        ) {
            Text(
                "🎯 Try to keep a daily streak going. Studies show that even 25 minutes of focused work daily improves retention by 40%.",
                modifier = Modifier.padding(16.dp),
                fontSize = 14.sp,
                color    = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun AllTimeTab(state: StatsUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("All Time Stats", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        listOf(
            Triple("🪙 Focus Coins", "${state.totalCoins}", FocusSecondary),
            Triple("✅ Sessions Completed", "${state.totalSessions}", FocusAccent),
            Triple("⏱ Total Focus Time", state.totalFocusMinutes.formatMinutes(), FocusPrimary),
            Triple("🔥 Best Streak", "${state.longestStreak} days", WarnAmber)
        ).forEach { (label, value, color) ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape    = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(label, color = MaterialTheme.colorScheme.onSurface)
                    Text(value, fontWeight = FontWeight.Bold, color = color, fontSize = 18.sp)
                }
            }
        }
    }
}

@Composable
private fun AppUsageRow(stat: AppUsageStat) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape    = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stat.appName, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text(stat.totalTimeMs.msToReadable(), fontSize = 13.sp, color = FocusPrimary, fontWeight = FontWeight.SemiBold)
            }
            if (stat.estimatedReelCount > 0) {
                Text("📱 ~${stat.estimatedReelCount} short videos", fontSize = 12.sp, color = WarnAmber)
            }
            LinearProgressIndicator(
                progress = { (stat.totalTimeMs / (4 * 3600_000f)).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color    = FocusPrimary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
private fun PlatformReelCard(name: String, count: Int, color: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape    = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(name, color = MaterialTheme.colorScheme.onSurface)
            Text("$count", fontWeight = FontWeight.Bold, color = color, fontSize = 20.sp)
        }
    }
}

@Composable
private fun MiniStatCard(modifier: Modifier = Modifier, value: String, label: String, color: Color) {
    Card(
        modifier = modifier,
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape    = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = color)
            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

private fun Long.formatMinutes(): String {
    val h = this / 60; val m = this % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}

private fun Long.msToReadable(): String {
    val totalSec = this / 1000
    val h = totalSec / 3600; val m = (totalSec % 3600) / 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}
