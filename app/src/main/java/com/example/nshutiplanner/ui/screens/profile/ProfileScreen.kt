package com.example.nshutiplanner.ui.screens.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import com.example.nshutiplanner.data.model.*
import com.example.nshutiplanner.data.repository.FirebaseRepository
import com.example.nshutiplanner.ui.components.*
import com.example.nshutiplanner.ui.theme.*
import com.example.nshutiplanner.viewmodel.CareViewModel
import com.example.nshutiplanner.viewmodel.DashboardViewModel
import com.example.nshutiplanner.viewmodel.VmFactory
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

private val moodEmojis = mapOf(1 to "😢", 2 to "😕", 3 to "😐", 4 to "🙂", 5 to "😄")
private val themeOptions = listOf(
    "lavender" to LavenderDark,
    "teal" to TealDark,
    "peach" to PeachDark
)

@Composable
fun ProfileScreen(
    user: User?,
    repo: FirebaseRepository,
    darkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    onLogout: () -> Unit,
    onCareClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val dashVm: DashboardViewModel = viewModel(factory = VmFactory(repo))
    val careVm: CareViewModel = viewModel(factory = VmFactory(repo))

    LaunchedEffect(user?.coupleId) {
        user?.coupleId?.let {
            dashVm.init(it)
            careVm.init(it)
        }
    }

    val plans by dashVm.plans.collectAsState()
    val tasks by dashVm.tasks.collectAsState()
    val badges by dashVm.badges.collectAsState()
    val moods by careVm.moods.collectAsState()

    var editingMotto by remember { mutableStateOf(false) }
    var mottoText by remember(user?.motto) { mutableStateOf(user?.motto ?: "") }
    var selectedMood by remember(user?.currentMood) { mutableIntStateOf(user?.currentMood ?: 3) }
    var showThemePicker by remember { mutableStateOf(false) }

    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val url = repo.uploadProfilePhoto(it, repo.currentUid)
                repo.updateProfile(repo.currentUid, mapOf("photoUrl" to url))
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Avatar & Name ──────────────────────────────────────────────────
        NshutiCard(color = LavenderLight.copy(alpha = 0.5f)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    if (user?.photoUrl?.isNotEmpty() == true) {
                        AsyncImage(
                            model = user.photoUrl,
                            contentDescription = "Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(90.dp).clip(CircleShape).border(3.dp, LavenderDark, CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier.size(90.dp).clip(CircleShape).background(LavenderDark),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                user?.displayName?.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                fontSize = 36.sp, color = Color.White, fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    IconButton(
                        onClick = { photoLauncher.launch("image/*") },
                        modifier = Modifier.size(28.dp).clip(CircleShape).background(LavenderDark)
                    ) {
                        Icon(Icons.Rounded.CameraAlt, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(user?.displayName ?: "", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(user?.email ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Spacer(Modifier.height(10.dp))

                // Mood indicator
                Text("Current Mood", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                MoodSelector(selected = selectedMood, onSelect = { mood ->
                    selectedMood = mood
                    scope.launch { repo.updateProfile(repo.currentUid, mapOf("currentMood" to mood)) }
                })

                Spacer(Modifier.height(10.dp))

                // Motto
                if (editingMotto) {
                    OutlinedTextField(
                        value = mottoText,
                        onValueChange = { mottoText = it },
                        label = { Text("Your motto") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = {
                                editingMotto = false
                                scope.launch { repo.updateProfile(repo.currentUid, mapOf("motto" to mottoText)) }
                            }) { Icon(Icons.Rounded.Check, null, tint = TealDark) }
                        }
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { editingMotto = true }
                    ) {
                        Text(
                            if (mottoText.isNotEmpty()) "\"$mottoText\"" else "Add a personal motto...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (mottoText.isNotEmpty()) LavenderDark else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Rounded.Edit, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        // ── Theme Toggle ───────────────────────────────────────────────────
        NshutiCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (darkTheme) Icons.Rounded.DarkMode else Icons.Rounded.LightMode,
                    null, tint = LavenderDark, modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    if (darkTheme) "Dark Mode" else "Light Mode",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = darkTheme,
                    onCheckedChange = { onToggleTheme() },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = LavenderDark)
                )
            }
        }

        // ── Theme Colors ───────────────────────────────────────────────────
        NshutiCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Theme Color", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                IconButton(onClick = { showThemePicker = !showThemePicker }) {
                    Icon(Icons.Rounded.Palette, null, tint = LavenderDark)
                }
            }
            if (showThemePicker) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 8.dp)) {
                    themeOptions.forEach { (name, color) ->
                        val isSelected = user?.themeColor == name
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(if (isSelected) 3.dp else 0.dp, Color.White, CircleShape)
                                .clickable {
                                    scope.launch { repo.updateProfile(repo.currentUid, mapOf("themeColor" to name)) }
                                }
                        )
                    }
                }
            }
        }

        // ── Personal Progress ──────────────────────────────────────────────
        NshutiCard {
            SectionHeader("My Progress 📊")
            val myTasks = tasks.filter { it.assignedTo == repo.currentUid }
            val myCompleted = myTasks.count { it.isCompleted }
            val myProgress = if (myTasks.isEmpty()) 0f else myCompleted.toFloat() / myTasks.size
            NshutiProgressBar(myProgress, "My Tasks ($myCompleted/${myTasks.size})", LavenderDark)
            Spacer(Modifier.height(8.dp))
            val myPlans = plans.filter { it.createdBy == repo.currentUid }
            val myPlansCompleted = myPlans.count { it.isCompleted }
            val planProgress = if (myPlans.isEmpty()) 0f else myPlansCompleted.toFloat() / myPlans.size
            NshutiProgressBar(planProgress, "My Plans ($myPlansCompleted/${myPlans.size})", TealDark)
        }

        // ── Couple Dashboard ───────────────────────────────────────────────
        NshutiCard(color = TealLight.copy(alpha = 0.4f)) {
            SectionHeader("💑 Couple Progress")
            val totalTasks = tasks.size
            val doneTasks = tasks.count { it.isCompleted }
            val coupleProgress = if (totalTasks == 0) 0f else doneTasks.toFloat() / totalTasks
            NshutiProgressBar(coupleProgress, "Together ($doneTasks/$totalTasks tasks)", TealDark)
            Spacer(Modifier.height(8.dp))
            Text(
                "Together you've completed $doneTasks shared tasks! 🎉",
                style = MaterialTheme.typography.bodyMedium,
                color = TealDark,
                fontWeight = FontWeight.Medium
            )
        }

        // ── Badges ─────────────────────────────────────────────────────────
        if (badges.isNotEmpty()) {
            NshutiCard {
                SectionHeader("Badges 🏆")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(badges) { BadgeChip(it) }
                }
            }
        }

        // ── Mood History ───────────────────────────────────────────────────
        if (moods.isNotEmpty()) {
            NshutiCard {
                SectionHeader("Mood History 💜")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    moods.takeLast(10).forEach { entry ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(moodEmojis[entry.mood] ?: "😐", fontSize = 18.sp)
                            Box(
                                modifier = Modifier
                                    .width(20.dp)
                                    .height((entry.mood * 8).dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(LavenderDark.copy(alpha = 0.6f + entry.mood * 0.08f))
                            )
                        }
                    }
                }
            }
        }

        // ── Quick Care Note ────────────────────────────────────────────────
        NshutiCard(color = SoftPink.copy(alpha = 0.3f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Send a Care Note 💌", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Reach out to your partner", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(
                    onClick = onCareClick,
                    colors = ButtonDefaults.buttonColors(containerColor = LavenderDark),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.Favorite, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Care")
                }
            }
        }

        // ── Logout ─────────────────────────────────────────────────────────
        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Icon(Icons.Rounded.Logout, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Logout")
        }

        Spacer(Modifier.height(8.dp))
    }
}
