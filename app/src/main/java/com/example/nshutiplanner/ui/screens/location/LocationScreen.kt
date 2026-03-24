package com.example.nshutiplanner.ui.screens.location

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.nshutiplanner.ui.theme.*
import com.example.nshutiplanner.unity.UnityBridgeActivity
import com.example.nshutiplanner.viewmodel.ErrorKind
import com.example.nshutiplanner.viewmodel.LocationUiState
import com.example.nshutiplanner.viewmodel.LocationViewModel
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.ar.core.ArCoreApk
import kotlinx.coroutines.delay

private const val TAG = "LocationScreen"

@Composable
fun LocationScreen(viewModel: LocationViewModel) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // Log every state change for debugging
    LaunchedEffect(uiState) {
        Log.d(TAG, "uiState changed → $uiState")
    }

    val hasPermission = remember(Unit) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    var permissionGranted by remember { mutableStateOf(hasPermission) }
    var permissionDenied by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        permissionGranted = granted
        permissionDenied = !granted
        Log.d(TAG, "Permission result: granted=$granted")
    }

    LaunchedEffect(Unit) {
        if (!permissionGranted) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    val isArCoreSupported = remember {
        try {
            ArCoreApk.getInstance().checkAvailability(context).isSupported
        } catch (_: Exception) { false }
    }

    var isMapInitOk by remember { mutableStateOf(true) }
    val renderMode = selectRenderMode(isArCoreSupported, isMapInitOk)

    var showSuccessBanner by remember { mutableStateOf(false) }
    LaunchedEffect(uiState) {
        if (uiState is LocationUiState.Success) {
            showSuccessBanner = true
            delay(2000)
            showSuccessBanner = false
        }
    }

    val onFindAndVibrate: () -> Unit = {
        Log.d(TAG, "Button tapped, permissionGranted=$permissionGranted")
        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        try {
            fusedClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    Log.d(TAG, "Got last location: ${location.latitude}, ${location.longitude}")
                    viewModel.findAndVibrate(true, location.latitude, location.longitude, location.accuracy)
                } else {
                    Log.d(TAG, "lastLocation null, requesting fresh location")
                    val request = CurrentLocationRequest.Builder()
                        .setDurationMillis(10_000)
                        .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                        .build()
                    fusedClient.getCurrentLocation(request, null)
                        .addOnSuccessListener { fresh ->
                            if (fresh != null) {
                                viewModel.findAndVibrate(true, fresh.latitude, fresh.longitude, fresh.accuracy)
                            } else {
                                Log.w(TAG, "Fresh location also null, using 0,0")
                                viewModel.findAndVibrate(true, 0.0, 0.0, 0f)
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "getCurrentLocation failed", e)
                            viewModel.findAndVibrate(true, 0.0, 0.0, 0f)
                        }
                }
            }.addOnFailureListener { e ->
                Log.e(TAG, "lastLocation failed", e)
                viewModel.findAndVibrate(true, 0.0, 0.0, 0f)
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException getting location", e)
            viewModel.findAndVibrate(false, 0.0, 0.0, 0f)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Find & Vibrate",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedButton(
            onClick = { context.startActivity(UnityBridgeActivity.newIntent(context)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Unity 3D View")
        }

        when {
            permissionDenied -> PermissionDeniedCard(context = context)

            permissionGranted -> {
                // ── Action button (always visible unless Success) ──────────────
                if (uiState !is LocationUiState.Success) {
                    Button(
                        onClick = onFindAndVibrate,
                        enabled = uiState !is LocationUiState.Loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Indigo500)
                    ) {
                        if (uiState is LocationUiState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            text = if (uiState is LocationUiState.Loading) "Locating..." else "Find & Vibrate",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // ── State-specific content ─────────────────────────────────────
                when (uiState) {
                    is LocationUiState.Idle -> {
                        // Nothing extra — button above is enough
                    }

                    is LocationUiState.Loading -> {
                        Text(
                            text = "Locating your partner...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    is LocationUiState.Error -> {
                        val err = uiState as LocationUiState.Error
                        Log.d(TAG, "Rendering error card: ${err.message}")
                        if (err.kind == ErrorKind.WRITE_FAILED && err.message.contains("partner", ignoreCase = true)) {
                            NoPartnerCard()
                        } else {
                            ErrorCard(uiState = err, onRetry = { viewModel.resetToIdle() })
                        }
                    }

                    is LocationUiState.Success -> {
                        val success = uiState as LocationUiState.Success

                        if (success.isStale) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFFFF176), RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = "Location may be outdated (>30 min)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF5D4037),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                        }

                        if (showSuccessBanner) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF00897B).copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = "✓ Vibration sent to ${success.receiverName}!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF00897B),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            MapRenderer(
                                receiverLocation = success.receiverLocation,
                                receiverDisplayName = success.receiverName.ifBlank { "Partner" },
                                receiverPhotoUrl = success.receiverPhotoUrl,
                                renderMode = renderMode,
                                onMapInitFailed = { isMapInitOk = false }
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = { viewModel.resetToIdle() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Find Again")
                        }
                    }
                }
            }

            else -> {
                CircularProgressIndicator(modifier = Modifier.padding(32.dp))
                Text(
                    "Waiting for location permission...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun NoPartnerCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "No Partner Linked",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Go to the Home screen and tap \"Link Partner\" to connect with your partner before using Find & Vibrate.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun PermissionDeniedCard(context: Context) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Location Permission Required",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "This feature needs access to your location to share it with your partner and send a vibration signal. Please grant location permission in app settings.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Open Settings")
            }
        }
    }
}

@Composable
private fun ErrorCard(uiState: LocationUiState.Error, onRetry: () -> Unit) {
    val showRetry = uiState.kind != ErrorKind.SELF_TARGET &&
            uiState.kind != ErrorKind.VIBRATION_UNAVAILABLE

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Something went wrong",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = uiState.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            if (showRetry) {
                Spacer(Modifier.height(16.dp))
                OutlinedButton(
                    onClick = onRetry,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Retry")
                }
            }
        }
    }
}
