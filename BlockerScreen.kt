package com.focusguard.app.ui.blocker

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focusguard.app.ui.theme.BlockRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockerScreen(
    onBack: () -> Unit,
    viewModel: BlockerViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("App Blocker", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {

            // When to block
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Block only during focus sessions", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(
                            if (state.blockOnlyDuringFocus) "Apps open normally outside a session"
                            else "Selected apps are blocked all day",
                            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = state.blockOnlyDuringFocus, onCheckedChange = viewModel::setBlockOnlyDuringFocus)
                }
            }

            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                placeholder = { Text("Search apps…") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (state.searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${state.blockedPackages.size} apps blocked",
                    color = if (state.blockedPackages.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else BlockRed,
                    fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f)
                )
                FilledTonalButton(onClick = viewModel::blockDistractions) {
                    Text("Block common distractions", fontSize = 12.sp)
                }
            }

            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.installedApps, key = { it.packageName }) { app ->
                        AppBlockItem(app, app.packageName in state.blockedPackages) { viewModel.toggleBlock(app) }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppBlockItem(app: InstalledApp, isBlocked: Boolean, onToggle: () -> Unit) {
    val context = LocalContext.current
    val icon: Bitmap? = remember(app.packageName) {
        try { context.packageManager.getApplicationIcon(app.packageName).toBitmap(96, 96) }
        catch (_: Exception) { null }
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isBlocked) BlockRed.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (icon != null) {
                Image(
                    bitmap = icon.asImageBitmap(), contentDescription = app.appName,
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp))
                )
            } else {
                Box(Modifier.size(44.dp))
            }
            Text(
                app.appName, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f)
            )
            Switch(
                checked = isBlocked,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White, checkedTrackColor = BlockRed,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    }
}
