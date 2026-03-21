package com.example.nshutiplanner.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.nshutiplanner.data.model.Actor
import com.example.nshutiplanner.data.model.LocationRecord
import com.example.nshutiplanner.data.model.User
import com.example.nshutiplanner.data.repository.FirebaseRepository
import com.example.nshutiplanner.data.repository.LocationRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// ── UI State ──────────────────────────────────────────────────────────────────

sealed class LocationUiState {
    object Idle : LocationUiState()
    object Loading : LocationUiState()
    data class Success(val receiverLocation: LocationRecord, val isStale: Boolean, val receiverName: String = "", val receiverPhotoUrl: String = "") : LocationUiState()
    data class Error(val message: String, val kind: ErrorKind) : LocationUiState()
}

enum class ErrorKind {
    PERMISSION_DENIED,
    RECEIVER_LOCATION_UNAVAILABLE,
    VIBRATION_UNAVAILABLE,
    SELF_TARGET,
    WRITE_FAILED,
    FCM_FAILED,
    MAP_INIT_FAILED
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

class LocationViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LocationRepository()
    private val firebaseRepo = FirebaseRepository(application.applicationContext)
    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow<LocationUiState>(LocationUiState.Idle)
    val uiState: StateFlow<LocationUiState> = _uiState

    fun findAndVibrate(
        locationPermissionGranted: Boolean,
        latitude: Double,
        longitude: Double,
        accuracy: Float
    ) {
        if (!locationPermissionGranted) {
            _uiState.value = LocationUiState.Error(
                message = "Location permission is required to use Find & Vibrate.",
                kind = ErrorKind.PERMISSION_DENIED
            )
            return
        }

        _uiState.value = LocationUiState.Loading

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Step 1: Get current Firebase user
                val firebaseUser = auth.currentUser
                if (firebaseUser == null) {
                    _uiState.value = LocationUiState.Error(
                        message = "You must be signed in to use Find & Vibrate.",
                        kind = ErrorKind.WRITE_FAILED
                    )
                    return@launch
                }

                // Step 2: Load the current User document from Firestore
                val currentUser: User = firebaseRepo.getUser(firebaseUser.uid)
                    ?: run {
                        _uiState.value = LocationUiState.Error(
                            message = "Could not load your user profile.",
                            kind = ErrorKind.WRITE_FAILED
                        )
                        return@launch
                    }

                // Step 3: Resolve sender and receiver actors
                val (sender, receiver) = try {
                    repository.resolveActors(currentUser)
                } catch (e: Exception) {
                    _uiState.value = LocationUiState.Error(
                        message = "Could not resolve partner: ${e.message}",
                        kind = ErrorKind.WRITE_FAILED
                    )
                    return@launch
                }

                // Step 4: Self-target guard
                if (sender.uid == receiver.uid) {
                    _uiState.value = LocationUiState.Error(
                        message = "You cannot send a vibration to yourself.",
                        kind = ErrorKind.SELF_TARGET
                    )
                    return@launch
                }

                // Step 5: Build and publish sender's location
                val locationRecord = LocationRecord(
                    uid = sender.uid,
                    latitude = latitude,
                    longitude = longitude,
                    accuracyMetres = accuracy
                )

                try {
                    repository.publishSenderLocation(sender, locationRecord)
                } catch (e: Exception) {
                    _uiState.value = LocationUiState.Error(
                        message = "Failed to publish your location: ${e.message}",
                        kind = ErrorKind.WRITE_FAILED
                    )
                    return@launch
                }

                // Step 6: Fetch receiver's location
                val receiverLocation: LocationRecord = try {
                    repository.fetchReceiverLocation(receiver)
                } catch (e: Exception) {
                    _uiState.value = LocationUiState.Error(
                        message = "Partner's location is not available yet.",
                        kind = ErrorKind.RECEIVER_LOCATION_UNAVAILABLE
                    )
                    return@launch
                }

                // Step 7: Compute staleness (> 30 minutes)
                val isStale = (System.currentTimeMillis() -
                        receiverLocation.updatedAt.toDate().time) > 30 * 60 * 1000L

                // Step 8: Guard against missing FCM token
                if (receiver.fcmToken.isBlank()) {
                    _uiState.value = LocationUiState.Error(
                        message = "Your partner's device is not reachable. Ask them to reopen the app.",
                        kind = ErrorKind.VIBRATION_UNAVAILABLE
                    )
                    return@launch
                }

                // Step 9: Dispatch vibration signal
                try {
                    repository.dispatchVibrationSignal(sender, receiver)
                } catch (e: Exception) {
                    _uiState.value = LocationUiState.Error(
                        message = "Failed to send vibration signal: ${e.message}",
                        kind = ErrorKind.FCM_FAILED
                    )
                    return@launch
                }

                // Step 10: Emit success
                _uiState.value = LocationUiState.Success(
                    receiverLocation = receiverLocation,
                    isStale = isStale,
                    receiverName = receiver.displayName,
                    receiverPhotoUrl = receiver.photoUrl
                )

            } catch (e: Exception) {
                _uiState.value = LocationUiState.Error(
                    message = "An unexpected error occurred: ${e.message}",
                    kind = ErrorKind.WRITE_FAILED
                )
            }
        }
    }

    fun resetToIdle() {
        _uiState.value = LocationUiState.Idle
    }
}
