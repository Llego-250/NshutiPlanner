package com.example.nshutiplanner.ui.screens.location

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.nshutiplanner.data.model.LocationRecord
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.*
import java.text.SimpleDateFormat
import java.util.*

enum class RenderMode { AR, MAP_3D, MAP_2D_FALLBACK }

fun selectRenderMode(isArCoreSupported: Boolean, isMapInitOk: Boolean): RenderMode = when {
    isArCoreSupported -> RenderMode.AR
    isMapInitOk -> RenderMode.MAP_3D
    else -> RenderMode.MAP_2D_FALLBACK
}

// Dark map style JSON — matches the screenshot's dark navy theme
private val DARK_MAP_STYLE = """
[
  {"elementType":"geometry","stylers":[{"color":"#0d1b2a"}]},
  {"elementType":"labels.text.fill","stylers":[{"color":"#4a6fa5"}]},
  {"elementType":"labels.text.stroke","stylers":[{"color":"#0d1b2a"}]},
  {"featureType":"road","elementType":"geometry","stylers":[{"color":"#1a2f45"}]},
  {"featureType":"road","elementType":"geometry.stroke","stylers":[{"color":"#0d1b2a"}]},
  {"featureType":"road.highway","elementType":"geometry","stylers":[{"color":"#1e3a5f"}]},
  {"featureType":"water","elementType":"geometry","stylers":[{"color":"#0a1628"}]},
  {"featureType":"poi","elementType":"geometry","stylers":[{"color":"#0f2035"}]},
  {"featureType":"transit","elementType":"geometry","stylers":[{"color":"#0f2035"}]},
  {"featureType":"administrative","elementType":"geometry.stroke","stylers":[{"color":"#1a3a5c"}]}
]
""".trimIndent()

@Composable
fun MapRenderer(
    receiverLocation: LocationRecord,
    receiverDisplayName: String,
    receiverPhotoUrl: String = "",
    renderMode: RenderMode,
    onMapInitFailed: () -> Unit
) {
    when (renderMode) {
        RenderMode.AR -> {
            // AR placeholder — falls back to map style
            DarkMapWithRadar(
                receiverLocation = receiverLocation,
                receiverDisplayName = receiverDisplayName,
                receiverPhotoUrl = receiverPhotoUrl,
                tilt = 0f
            )
        }
        RenderMode.MAP_3D -> {
            DarkMapWithRadar(
                receiverLocation = receiverLocation,
                receiverDisplayName = receiverDisplayName,
                receiverPhotoUrl = receiverPhotoUrl,
                tilt = 45f
            )
        }
        RenderMode.MAP_2D_FALLBACK -> {
            DarkMapWithRadar(
                receiverLocation = receiverLocation,
                receiverDisplayName = receiverDisplayName,
                receiverPhotoUrl = receiverPhotoUrl,
                tilt = 0f
            )
        }
    }
}

@Composable
private fun DarkMapWithRadar(
    receiverLocation: LocationRecord,
    receiverDisplayName: String,
    receiverPhotoUrl: String,
    tilt: Float
) {
    val receiverLatLng = LatLng(receiverLocation.latitude, receiverLocation.longitude)

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.Builder()
            .target(receiverLatLng)
            .zoom(15f)
            .tilt(tilt)
            .bearing(0f)
            .build()
    }

    val mapProperties = MapProperties(
        mapStyleOptions = MapStyleOptions(DARK_MAP_STYLE)
    )

    val mapUiSettings = MapUiSettings(
        zoomControlsEnabled = false,
        myLocationButtonEnabled = false,
        compassEnabled = false,
        mapToolbarEnabled = false
    )

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = mapProperties,
            uiSettings = mapUiSettings
        ) {
            // Radar pulse rings drawn as map overlays
            RadarCircleOverlay(center = receiverLatLng)

            // Partner avatar marker
            MarkerComposable(
                state = MarkerState(position = receiverLatLng),
                title = receiverDisplayName
            ) {
                AvatarPin(
                    photoUrl = receiverPhotoUrl,
                    displayName = receiverDisplayName,
                    dotColor = Color(0xFF4FC3F7) // light blue dot
                )
            }
        }

        // Re-center FAB
        FloatingActionButton(
            onClick = {
                cameraPositionState.position = CameraPosition.Builder()
                    .target(receiverLatLng)
                    .zoom(15f)
                    .tilt(tilt)
                    .build()
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 120.dp)
                .size(44.dp),
            containerColor = Color(0xFF1E2D3D),
            contentColor = Color.White,
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(4.dp)
        ) {
            Icon(Icons.Default.MyLocation, contentDescription = "Re-center", modifier = Modifier.size(20.dp))
        }

        // Bottom info sheet
        BottomInfoSheet(
            receiverDisplayName = receiverDisplayName,
            receiverLocation = receiverLocation,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun RadarCircleOverlay(center: LatLng) {
    // Animated pulse scale
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseOutCubic),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseOutCubic),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha"
    )

    // Static background circles (150m, 300m radius)
    Circle(
        center = center,
        radius = 300.0,
        fillColor = Color(0x1A1E6FA5),
        strokeColor = Color(0x331E6FA5),
        strokeWidth = 1f
    )
    Circle(
        center = center,
        radius = 150.0,
        fillColor = Color(0x261E6FA5),
        strokeColor = Color(0x4D1E6FA5),
        strokeWidth = 1f
    )

    // Animated pulse circle
    Circle(
        center = center,
        radius = (300.0 * pulseScale),
        fillColor = Color(0x001E6FA5),
        strokeColor = Color(0x661E6FA5).copy(alpha = pulseAlpha),
        strokeWidth = 2f
    )
}

@Composable
private fun AvatarPin(
    photoUrl: String,
    displayName: String,
    dotColor: Color
) {
    Box(contentAlignment = Alignment.TopEnd) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(Color(0xFF1E2D3D))
                .border(2.dp, Color(0xFF2A4A6B), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (photoUrl.isNotBlank()) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = displayName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            } else {
                // Initials fallback
                Text(
                    text = displayName.take(1).uppercase(),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Online dot
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(dotColor)
                .border(2.dp, Color(0xFF0D1B2A), CircleShape)
        )
    }
}

@Composable
private fun BottomInfoSheet(
    receiverDisplayName: String,
    receiverLocation: LocationRecord,
    modifier: Modifier = Modifier
) {
    val updatedTime = remember(receiverLocation.updatedAt) {
        val sdf = SimpleDateFormat("MM/dd/yyyy  hh:mm a", Locale.getDefault())
        sdf.format(receiverLocation.updatedAt.toDate())
    }
    val (dateStr, timeStr) = updatedTime.split("  ").let {
        if (it.size == 2) it[0] to it[1] else updatedTime to ""
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = Color(0xFF111D2B),
        tonalElevation = 8.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            // Drag handle
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF2A4A6B))
                    .align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(16.dp))

            InfoRow(
                icon = "📍",
                text = receiverDisplayName.ifBlank { "Partner" } + "'s last known location"
            )
            Spacer(Modifier.height(12.dp))
            InfoRow(icon = "📅", text = dateStr)
            Spacer(Modifier.height(12.dp))
            InfoRow(icon = "🕐", text = timeStr)
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun InfoRow(icon: String, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text = icon, fontSize = 18.sp)
        Spacer(Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFFB0C4DE),
            modifier = Modifier.weight(1f)
        )
    }
}
