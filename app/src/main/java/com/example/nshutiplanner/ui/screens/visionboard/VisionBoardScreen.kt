package com.example.nshutiplanner.ui.screens.visionboard

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
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
import com.example.nshutiplanner.data.model.VisionItem
import com.example.nshutiplanner.ui.components.*
import com.example.nshutiplanner.ui.screens.planner.EmptyState
import com.example.nshutiplanner.ui.theme.*
import com.example.nshutiplanner.viewmodel.VisionViewModel

@Composable
fun VisionBoardScreen(vm: VisionViewModel, currentUid: String, onBack: () -> Unit = {}) {
    val items by vm.items.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var dialogType by remember { mutableStateOf("quote") }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { vm.uploadImage(it) { url ->
            vm.saveItem(VisionItem(type = "image", content = url, addedBy = currentUid))
        }}
    }

    Scaffold(
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallFloatingActionButton(
                    onClick = { imagePicker.launch("image/*") },
                    containerColor = adaptiveTealLight(),
                    contentColor = TealDark
                ) { Icon(Icons.Rounded.Image, "Add Image") }
                SmallFloatingActionButton(
                    onClick = { dialogType = "goal"; showDialog = true },
                    containerColor = adaptivePeachLight(),
                    contentColor = PeachDark
                ) { Icon(Icons.Rounded.Flag, "Add Goal") }
                FloatingActionButton(
                    onClick = { dialogType = "quote"; showDialog = true },
                    containerColor = LavenderDark,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                ) { Icon(Icons.Rounded.FormatQuote, "Add Quote") }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Rounded.ArrowBackIosNew, "Back", tint = MaterialTheme.colorScheme.onSurface)
                }
                SectionHeader("🌟 Vision Board", modifier = Modifier.weight(1f))
            }
            Text("Your shared dreams & inspirations",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))

            if (items.isEmpty()) {
                EmptyState("Your vision board is empty.\nAdd quotes, goals, or images! ✨")
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(items, key = { it.id }) { item ->
                        VisionItemCard(item, currentUid, onDelete = { vm.deleteItem(item.id) })
                    }
                }
            }
        }
    }

    if (showDialog) {
        AddVisionDialog(
            type = dialogType,
            onSave = { content ->
                vm.saveItem(VisionItem(type = dialogType, content = content, addedBy = currentUid))
                showDialog = false
            },
            onDismiss = { showDialog = false }
        )
    }
}

@Composable
private fun VisionItemCard(item: VisionItem, currentUid: String, onDelete: () -> Unit) {
    val bgColor = when (item.type) {
        "quote" -> adaptiveLavenderLight()
        "goal" -> adaptivePeachLight()
        else -> adaptiveTealLight()
    }
    val isOwn = item.addedBy == currentUid

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(if (item.type == "image") 1f else 0.85f)
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
    ) {
        when (item.type) {
            "image" -> AsyncImage(
                model = item.content,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            "quote" -> Column(
                Modifier.fillMaxSize().padding(12.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("❝", fontSize = 24.sp, color = LavenderDark)
                Text(item.content, style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
            }
            "goal" -> Column(
                Modifier.fillMaxSize().padding(12.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🎯", fontSize = 28.sp)
                Spacer(Modifier.height(6.dp))
                Text(item.content, style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
            }
        }

        if (isOwn) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier.align(Alignment.TopEnd).size(32.dp)
            ) {
                Icon(Icons.Rounded.Close, null, tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(16.dp))
            }
        }

        // Partner badge
        if (!isOwn) {
            Surface(
                modifier = Modifier.align(Alignment.BottomStart).padding(6.dp),
                shape = RoundedCornerShape(50),
                color = SoftPink.copy(alpha = 0.8f)
            ) {
                Text("💜 Partner", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall, color = PeachDark)
            }
        }
    }
}

@Composable
private fun AddVisionDialog(type: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var content by remember { mutableStateOf("") }
    val (title, hint) = when (type) {
        "goal" -> "Add a Goal 🎯" to "What do you want to achieve?"
        else -> "Add a Quote ❝" to "An inspiring quote or thought..."
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text(hint) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
        },
        confirmButton = {
            Button(onClick = { if (content.isNotBlank()) onSave(content) }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
