package com.example.nshutiplanner.ui.screens.tasks

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.example.nshutiplanner.data.model.Task
import com.example.nshutiplanner.data.model.TaskCategory
import com.example.nshutiplanner.ui.components.*
import com.example.nshutiplanner.ui.screens.planner.EmptyState
import com.example.nshutiplanner.ui.theme.*
import com.example.nshutiplanner.viewmodel.TasksViewModel

private val categoryColors = mapOf(
    TaskCategory.HEALTH.name to TealDark,
    TaskCategory.CAREER.name to LavenderDark,
    TaskCategory.RELATIONSHIP.name to PeachDark,
    TaskCategory.FUN.name to Color(0xFFFFB300)
)

private val categoryEmojis = mapOf(
    TaskCategory.HEALTH.name to "💪",
    TaskCategory.CAREER.name to "💼",
    TaskCategory.RELATIONSHIP.name to "💜",
    TaskCategory.FUN.name to "🎉"
)

@Composable
fun TasksScreen(vm: TasksViewModel, currentUid: String) {
    val tasks by vm.tasks.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    val filtered = if (selectedCategory == null) tasks else tasks.filter { it.category == selectedCategory }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = TealDark,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) { Icon(Icons.Rounded.Add, "Add Task") }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            SectionHeader("🎯 Tasks & Goals")

            // Category Filter
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CategoryChip("All", selectedCategory == null, LavenderDark) { selectedCategory = null }
                TaskCategory.values().forEach { cat ->
                    CategoryChip(
                        "${categoryEmojis[cat.name]} ${cat.name.lowercase().replaceFirstChar { it.uppercase() }}",
                        selectedCategory == cat.name,
                        categoryColors[cat.name] ?: LavenderDark
                    ) { selectedCategory = cat.name }
                }
            }

            Spacer(Modifier.height(16.dp))

            if (filtered.isEmpty()) {
                EmptyState("No tasks yet.\nTap + to add your first goal! ⭐")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(filtered, key = { it.id }) { task ->
                        TaskItem(task, currentUid,
                            onProgressChange = { vm.updateProgress(task.id, it) },
                            onDelete = { vm.deleteTask(task.id) }
                        )
                    }
                }
            }
        }
    }

    if (showDialog) {
        AddTaskDialog(
            currentUid = currentUid,
            onSave = { vm.saveTask(it); showDialog = false },
            onDismiss = { showDialog = false }
        )
    }
}

@Composable
private fun TaskItem(task: Task, currentUid: String, onProgressChange: (Float) -> Unit, onDelete: () -> Unit) {
    val color = categoryColors[task.category] ?: LavenderDark
    val emoji = categoryEmojis[task.category] ?: "📌"
    var sliderValue by remember(task.progress) { mutableFloatStateOf(task.progress) }

    NshutiCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 24.sp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(task.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (task.isShared) {
                    Text("Shared task", style = MaterialTheme.typography.labelSmall, color = color)
                }
            }
            if (task.isCompleted) Text("✅", fontSize = 18.sp)
            if (task.createdBy == currentUid) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Rounded.DeleteOutline, null, tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        NshutiProgressBar(sliderValue, "Progress", color)
        Spacer(Modifier.height(4.dp))
        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            onValueChangeFinished = { onProgressChange(sliderValue) },
            colors = SliderDefaults.colors(thumbColor = color, activeTrackColor = color),
            modifier = Modifier.fillMaxWidth()
        )

        if (task.isCompleted && task.milestone.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Surface(shape = RoundedCornerShape(8.dp), color = Mint.copy(alpha = 0.4f)) {
                Text("🎉 ${task.milestone}", modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun AddTaskDialog(currentUid: String, onSave: (Task) -> Unit, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(TaskCategory.RELATIONSHIP.name) }
    var isShared by remember { mutableStateOf(false) }
    var milestone by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Task / Goal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it },
                    label = { Text("Task Title") }, shape = RoundedCornerShape(12.dp),
                    singleLine = true, modifier = Modifier.fillMaxWidth())

                Text("Category", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TaskCategory.values().forEach { cat ->
                        FilterChip(
                            selected = category == cat.name,
                            onClick = { category = cat.name },
                            label = { Text("${categoryEmojis[cat.name]} ${cat.name.take(3)}", fontSize = 11.sp) },
                            shape = RoundedCornerShape(50.dp)
                        )
                    }
                }

                OutlinedTextField(value = milestone, onValueChange = { milestone = it },
                    label = { Text("Milestone message (optional)") }, shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth())

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = isShared, onCheckedChange = { isShared = it })
                    Spacer(Modifier.width(8.dp))
                    Text("Shared with partner", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (title.isNotBlank()) onSave(Task(title = title, category = category,
                    isShared = isShared, createdBy = currentUid, assignedTo = currentUid, milestone = milestone))
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
