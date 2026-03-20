package com.example.nshutiplanner.ui.screens.planner

import androidx.compose.foundation.*
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.*
import com.example.nshutiplanner.data.model.Plan
import com.example.nshutiplanner.ui.components.*
import com.example.nshutiplanner.ui.theme.*
import com.example.nshutiplanner.viewmodel.PlannerViewModel
import java.time.LocalDate

@Composable
fun PlannerScreen(vm: PlannerViewModel, currentUid: String) {
    val plans by vm.plans.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var isWeeklyTab by remember { mutableStateOf(false) }

    val filtered = plans.filter { it.isWeekly == isWeeklyTab }
    val today = LocalDate.now().toString()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = LavenderDark,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) { Icon(Icons.Rounded.Add, "Add Plan") }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            SectionHeader("📅 Planner")

            // Tab Row
            Row(
                Modifier.fillMaxWidth().background(LavenderLight, RoundedCornerShape(12.dp)).padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("Daily" to false, "Weekly" to true).forEach { (label, weekly) ->
                    val selected = isWeeklyTab == weekly
                    Box(
                        modifier = Modifier.weight(1f)
                            .background(if (selected) LavenderDark else Color.Transparent, RoundedCornerShape(10.dp))
                            .clickable { isWeeklyTab = weekly }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, color = if (selected) Color.White else LavenderDark,
                            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            if (filtered.isEmpty()) {
                EmptyState("No ${if (isWeeklyTab) "weekly" else "daily"} plans yet.\nTap + to add one! 🌱")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(filtered, key = { it.id }) { plan ->
                        PlanItem(plan, currentUid,
                            onToggle = { vm.togglePlan(plan.id, !plan.isCompleted) },
                            onDelete = { vm.deletePlan(plan.id) }
                        )
                    }
                }
            }
        }
    }

    if (showDialog) {
        AddPlanDialog(
            currentUid = currentUid,
            isWeekly = isWeeklyTab,
            today = today,
            onSave = { vm.savePlan(it); showDialog = false },
            onDismiss = { showDialog = false }
        )
    }
}

@Composable
private fun PlanItem(plan: Plan, currentUid: String, onToggle: () -> Unit, onDelete: () -> Unit) {
    val isOwn = plan.createdBy == currentUid
    NshutiCard(color = if (plan.isCompleted) Mint.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = plan.isCompleted,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(checkedColor = TealDark)
            )
            Column(Modifier.weight(1f)) {
                Text(
                    plan.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (plan.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                )
                if (plan.description.isNotEmpty()) {
                    Text(plan.description, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(plan.date, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (!isOwn) {
                        Spacer(Modifier.width(8.dp))
                        Surface(shape = RoundedCornerShape(50), color = SoftPink.copy(alpha = 0.5f)) {
                            Text("Partner's", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall, color = PeachDark)
                        }
                    }
                }
            }
            if (plan.streakCount > 0) {
                Text("🔥${plan.streakCount}", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.width(4.dp))
            }
            if (isOwn) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Rounded.DeleteOutline, null, tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                }
            }
        }
    }
}

@Composable
private fun AddPlanDialog(currentUid: String, isWeekly: Boolean, today: String,
                          onSave: (Plan) -> Unit, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(today) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New ${if (isWeekly) "Weekly" else "Daily"} Plan") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it },
                    label = { Text("Title") }, shape = RoundedCornerShape(12.dp), singleLine = true,
                    modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = description, onValueChange = { description = it },
                    label = { Text("Description (optional)") }, shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = date, onValueChange = { date = it },
                    label = { Text("Date (yyyy-MM-dd)") }, shape = RoundedCornerShape(12.dp),
                    singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = {
                if (title.isNotBlank()) onSave(Plan(title = title, description = description,
                    date = date, isWeekly = isWeekly, createdBy = currentUid, assignedTo = currentUid))
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun EmptyState(message: String) {
    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(message, style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}
