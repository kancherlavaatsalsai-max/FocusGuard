package com.focusguard.app.ui.battles

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focusguard.app.data.db.entity.ScrollBattleEntity
import com.focusguard.app.ui.theme.BlockRed
import com.focusguard.app.ui.theme.FocusAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BattlesScreen(onBack: () -> Unit, viewModel: BattlesViewModel = hiltViewModel()) {
    val battles by viewModel.battles.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Scroll Battles", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("New battle")
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            Text(
                "Challenge a friend: whoever scrolls FEWER Reels/Shorts today wins. " +
                    "Your count updates automatically. Type your friend's count yourself.",
                fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            if (battles.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No active battles.\nTap “New battle” to start one.",
                        textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 90.dp)) {
                    items(battles, key = { it.id }) { b ->
                        BattleCard(
                            b,
                            onOpponent = { viewModel.setOpponent(b.id, it) },
                            onEnd = { viewModel.end(b.id) }
                        )
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("New battle") },
            text = {
                OutlinedTextField(value = name, onValueChange = { name = it },
                    label = { Text("Friend's name") }, singleLine = true)
            },
            confirmButton = {
                TextButton(onClick = { viewModel.create(name); name = ""; showDialog = false }) { Text("Start") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun BattleCard(b: ScrollBattleEntity, onOpponent: (Int) -> Unit, onEnd: () -> Unit) {
    val winning = b.myScrollCount <= b.opponentScrollCount
    val accent = if (winning) FocusAccent else BlockRed
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(if (winning) "🏆 You're winning" else "😬 ${b.opponentName} is winning",
                color = accent, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Score("You", b.myScrollCount)
                Text("vs", Modifier.align(Alignment.CenterVertically), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Score(b.opponentName, b.opponentScrollCount)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("${b.opponentName}'s count", Modifier.weight(1f), fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                IconButton(onClick = { onOpponent(b.opponentScrollCount - 1) }) { Icon(Icons.Default.Remove, "Minus") }
                IconButton(onClick = { onOpponent(b.opponentScrollCount + 1) }) { Icon(Icons.Default.Add, "Plus") }
            }
            TextButton(onClick = onEnd) { Text("End battle") }
        }
    }
}

@Composable
private fun Score(name: String, n: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$n", fontSize = 34.sp, fontWeight = FontWeight.Bold)
        Text(name, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
