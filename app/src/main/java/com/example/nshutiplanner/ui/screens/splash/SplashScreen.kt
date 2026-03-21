package com.example.nshutiplanner.ui.screens.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    // Fade + scale in animation
    val animSpec = tween<Float>(durationMillis = 800, easing = EaseOutCubic)
    var started by remember { mutableStateOf(false) }

    val alpha by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = animSpec,
        label = "alpha"
    )
    val scale by animateFloatAsState(
        targetValue = if (started) 1f else 0.75f,
        animationSpec = animSpec,
        label = "scale"
    )

    LaunchedEffect(Unit) {
        started = true
        delay(2200)
        onFinished()
    }

    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1A1030), Color(0xFF2D1B69), Color(0xFF1A1030))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = rememberAsyncImagePainter(
                model = ImageRequest.Builder(context)
                    .data("file:///android_asset/logo.png")
                    .build()
            ),
            contentDescription = "NshutiPlanner Logo",
            modifier = Modifier
                .size(220.dp)
                .scale(scale)
                .alpha(alpha)
        )
    }
}
