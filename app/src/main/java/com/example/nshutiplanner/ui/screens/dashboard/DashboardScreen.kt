package com.example.nshutiplanner.ui.screens.dashboard

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.example.nshutiplanner.data.model.User
import com.example.nshutiplanner.ui.components.*
import com.example.nshutiplanner.ui.theme.*
import com.example.nshutiplanner.viewmodel.DashboardViewModel
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(vm: DashboardViewModel, user: User?, repo: com.example.nshutiplanner.data.repository.FirebaseRepository, onCareClick: () -> Unit = {}) {
    val plans by vm.plans.collectAsState()
    val tasks by vm.tasks.collectAsState()
    val badges by vm.badges.collectAsState()
    var showLinkDialog by remember { mutableStateOf(false) }
    var partnerEmail by remember { mutableStateOf("") }
    var linkMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    val planProgress = if (plans.isEmpty()) 0f else plans.count { it.isCompleted }.toFloat() / plans.size
    val taskProgress = if (tasks.isEmpty()) 0f else tasks.count { it.isCompleted }.toFloat() / tasks.size
    val overallProgress = (planProgress + taskProgress) / 2f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(adaptiveLavenderLight(), adaptiveTealLight())), RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Column {
                Text("Hello, ${user?.displayName ?: "Friend"} 💜",
                    style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Together you're ${(overallProgress * 100).toInt()}% there!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (user?.partnerId.isNullOrEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { showLinkDialog = true },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.PersonAdd, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Link Partner")
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Progress Section
        NshutiCard {
            SectionHeader("Shared Progress 📊")
            Spacer(Modifier.height(8.dp))
            NshutiProgressBar(planProgress, "Plans", LavenderDark)
            Spacer(Modifier.height(12.dp))
            NshutiProgressBar(taskProgress, "Tasks", TealDark)
            Spacer(Modifier.height(12.dp))
            NshutiProgressBar(overallProgress, "Overall", PeachDark)
        }

        Spacer(Modifier.height(16.dp))

        // Quick Stats
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Plans", plans.size, plans.count { it.isCompleted }, adaptiveLavenderLight(), LavenderDark, Modifier.weight(1f))
            StatCard("Tasks", tasks.size, tasks.count { it.isCompleted }, adaptiveTealLight(), TealDark, Modifier.weight(1f))
        }

        Spacer(Modifier.height(16.dp))

        // Badges
        if (badges.isNotEmpty()) {
            SectionHeader("Badges Earned 🏆")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(badges) { BadgeChip(it) }
            }
        } else {
            NshutiCard(color = adaptiveSoftYellow().copy(alpha = 0.4f)) {
                Text("🎯 Complete tasks and plans to earn badges!", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }

    if (showLinkDialog) {
        AlertDialog(
            onDismissRequest = { showLinkDialog = false },
            title = { Text("Link Your Partner") },
            text = {
                Column {
                    Text("Enter your partner's email address:", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = partnerEmail,
                        onValueChange = { partnerEmail = it },
                        label = { Text("Partner Email") },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    if (linkMessage.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(linkMessage, color = if (linkMessage.startsWith("✓"))
                            TealDark else MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        repo.linkPartner(partnerEmail)
                            .onSuccess { linkMessage = "✓ Partner linked!"; showLinkDialog = false }
                            .onFailure { linkMessage = it.message ?: "Failed" }
                    }
                }) { Text("Link") }
            },
            dismissButton = { TextButton(onClick = { showLinkDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun StatCard(label: String, total: Int, done: Int, bg: Color, accent: Color, modifier: Modifier) {
    NshutiCard(modifier = modifier, color = bg.copy(alpha = 0.5f)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = accent)
        Text("$done/$total", style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold, color = accent)
        Text("completed", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
