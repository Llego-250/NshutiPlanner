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
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        content = { Column(modifier = Modifier.padding(20.dp), content = content) }
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
                .size(64.dp)
                .clip(CircleShape)
                .background(background),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = tint, fontWeight = FontWeight.Bold)
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
            Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("${(animatedProgress * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, color = color)
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.15f)
        )
    }
}

@Composable
fun BadgeChip(badge: Badge) {
    Surface(
        shape = RoundedCornerShape(50.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.padding(4.dp),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(badge.icon, fontSize = 18.sp)
            Spacer(Modifier.width(8.dp))
            Text(badge.title, style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
fun MoodSelector(selected: Int, onSelect: (Int) -> Unit) {
    val moods = listOf("😢" to 1, "😕" to 2, "😐" to 3, "🙂" to 4, "😄" to 5)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        moods.forEach { (emoji, value) ->
            val isSelected = selected == value
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onSelect(value) },
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = 26.sp)
            }
        }
    }
}

@Composable
fun CategoryChip(label: String, selected: Boolean, color: Color, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(vertical = 4.dp)) },
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
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.ExtraBold,
        modifier = modifier.padding(vertical = 12.dp)
    )
}
