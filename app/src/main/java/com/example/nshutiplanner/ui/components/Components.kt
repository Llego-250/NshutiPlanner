package com.example.nshutiplanner.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.example.nshutiplanner.data.model.Badge
import com.example.nshutiplanner.ui.theme.*

@Composable
fun adaptiveColor(lightColor: Color, darkColor: Color): Color {
    return if (isSystemInDarkTheme() || MaterialTheme.colorScheme.background == BackgroundDark)
        darkColor else lightColor
}

// Adaptive pastel helpers
@Composable fun adaptiveLavenderLight() = if (MaterialTheme.colorScheme.surface == SurfaceDark) LavenderLightDark else LavenderLight
@Composable fun adaptiveTealLight() = if (MaterialTheme.colorScheme.surface == SurfaceDark) TealLightDark else TealLight
@Composable fun adaptivePeachLight() = if (MaterialTheme.colorScheme.surface == SurfaceDark) PeachLightDark else PeachLight
@Composable fun adaptiveSoftPink() = if (MaterialTheme.colorScheme.surface == SurfaceDark) SoftPinkDark else SoftPink
@Composable fun adaptiveSoftYellow() = if (MaterialTheme.colorScheme.surface == SurfaceDark) SoftYellowDark else SoftYellow
@Composable fun adaptiveMint() = if (MaterialTheme.colorScheme.surface == SurfaceDark) MintDark else Mint

@Composable
fun NshutiCard(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surface,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        content = { Column(modifier = Modifier.padding(16.dp), content = content) }
    )
}

@Composable
fun PastelIconButton(
    icon: ImageVector,
    label: String,
    tint: Color,
    background: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(background),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = tint)
    }
}

@Composable
fun NshutiProgressBar(
    progress: Float,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(800, easing = EaseOutCubic),
        label = "progress"
    )
    Column(modifier = modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text("${(animatedProgress * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, color = color)
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.2f)
        )
    }
}

@Composable
fun BadgeChip(badge: Badge) {
    Surface(
        shape = RoundedCornerShape(50.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.padding(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(badge.icon, fontSize = 16.sp)
            Spacer(Modifier.width(6.dp))
            Text(badge.title, style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
fun MoodSelector(selected: Int, onSelect: (Int) -> Unit) {
    val moods = listOf("😢" to 1, "😕" to 2, "😐" to 3, "🙂" to 4, "😄" to 5)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        moods.forEach { (emoji, value) ->
            val isSelected = selected == value
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onSelect(value) },
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = 22.sp)
            }
        }
    }
}

@Composable
fun CategoryChip(label: String, selected: Boolean, color: Color, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelLarge) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = color,
            selectedLabelColor = Color.White
        ),
        shape = RoundedCornerShape(50.dp)
    )
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = modifier.padding(vertical = 8.dp)
    )
}
