package com.example.nshutiplanner.ui.screens.care

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.example.nshutiplanner.data.model.Message
import com.example.nshutiplanner.ui.components.*
import com.example.nshutiplanner.ui.theme.*
import com.example.nshutiplanner.viewmodel.CareViewModel

private val encouragementEmojis = listOf("💜", "🌟", "🔥", "🤗", "💪", "🌈", "✨", "🥰", "👏", "🎉")

private val dailyPrompts = listOf(
    "How are you feeling today? 💭",
    "What made you smile today? 😊",
    "What are you grateful for? 🙏",
    "What's one thing you need right now? 💜",
    "Share a win from today! 🏆"
)

@Composable
fun NshutiCareScreen(vm: CareViewModel, currentUid: String) {
    val messages by vm.messages.collectAsState()
    val moods by vm.moods.collectAsState()
    var text by remember { mutableStateOf("") }
    var showMoodDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Column(Modifier.fillMaxSize()) {
        // Tab Header
        TabRow(selectedTabIndex = selectedTab, containerColor = MaterialTheme.colorScheme.surface) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 },
                text = { Text("💬 Chat") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 },
                text = { Text("😊 Mood") })
        }

        when (selectedTab) {
            0 -> ChatTab(messages, currentUid, text, listState,
                onTextChange = { text = it },
                onSend = { if (text.isNotBlank()) { vm.sendMessage(text); text = "" } },
                onEmoji = { vm.sendMessage("", emoji = it) },
                onMoodCheck = { showMoodDialog = true }
            )
            1 -> MoodTab(moods, currentUid)
        }
    }

    if (showMoodDialog) {
        MoodDialog(
            onSave = { mood, note -> vm.saveMood(mood, note); showMoodDialog = false },
            onDismiss = { showMoodDialog = false }
        )
    }
}

@Composable
private fun ChatTab(
    messages: List<Message>,
    currentUid: String,
    text: String,
    listState: LazyListState,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onEmoji: (String) -> Unit,
    onMoodCheck: () -> Unit
) {
    val todayPrompt = dailyPrompts[(System.currentTimeMillis() / 86400000 % dailyPrompts.size).toInt()]

    Column(Modifier.fillMaxSize()) {
        // Daily Prompt Banner
        Surface(color = adaptiveLavenderLight(), modifier = Modifier.fillMaxWidth()) {
            Row(
                Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("💭", fontSize = 18.sp)
                Spacer(Modifier.width(8.dp))
                Text(todayPrompt, style = MaterialTheme.typography.bodyMedium,
                    color = LavenderDark, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                TextButton(onClick = onMoodCheck) { Text("Check In", style = MaterialTheme.typography.labelLarge) }
            }
        }

        // Messages
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                MessageBubble(msg, isOwn = msg.senderId == currentUid)
            }
        }

        // Emoji Row
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            encouragementEmojis.forEach { emoji ->
                Text(
                    emoji,
                    fontSize = 22.sp,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { onEmoji(emoji) }
                        .padding(4.dp)
                )
            }
        }

        // Input Row
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                placeholder = { Text("Send a message or note...") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                maxLines = 3
            )
            IconButton(
                onClick = onSend,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(LavenderDark)
            ) {
                Icon(Icons.Rounded.Send, "Send", tint = Color.White)
            }
        }
    }
}

@Composable
private fun MessageBubble(message: Message, isOwn: Boolean) {
    val bubbleColor = if (isOwn) LavenderDark else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isOwn) Color.White else MaterialTheme.colorScheme.onSurface
    val alignment = if (isOwn) Alignment.End else Alignment.Start
    val shape = if (isOwn) RoundedCornerShape(18.dp, 4.dp, 18.dp, 18.dp)
    else RoundedCornerShape(4.dp, 18.dp, 18.dp, 18.dp)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        if (message.emoji.isNotEmpty()) {
            Text(message.emoji, fontSize = 28.sp,
                modifier = Modifier.padding(horizontal = 4.dp))
        } else {
            Surface(shape = shape, color = bubbleColor) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                    if (message.isNote) {
                        Text("📝 Note", style = MaterialTheme.typography.labelSmall,
                            color = textColor.copy(alpha = 0.7f))
                    }
                    Text(message.text, color = textColor, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun MoodTab(moods: List<com.example.nshutiplanner.data.model.MoodEntry>, currentUid: String) {
    val moodLabels = mapOf(1 to "😢 Sad", 2 to "😕 Low", 3 to "😐 Okay", 4 to "🙂 Good", 5 to "😄 Great")

    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        SectionHeader("Mood History 😊")
        Text("Last 14 check-ins", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))

        if (moods.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("No mood check-ins yet.\nTap 'Check In' in the chat tab! 💜",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        } else {
            moods.forEach { entry ->
                val isOwn = entry.userId == currentUid
                NshutiCard(
                    color = if (isOwn) adaptiveLavenderLight().copy(alpha = 0.5f) else adaptivePeachLight().copy(alpha = 0.5f),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(moodLabels[entry.mood] ?: "😐", style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.weight(1f))
                        Text(if (isOwn) "You" else "Partner",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isOwn) LavenderDark else PeachDark)
                    }
                    if (entry.note.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text(entry.note, style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun MoodDialog(onSave: (Int, String) -> Unit, onDismiss: () -> Unit) {
    var mood by remember { mutableIntStateOf(3) }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("How are you feeling? 💜") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MoodSelector(selected = mood, onSelect = { mood = it })
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Add a note (optional)") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(mood, note) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
