package com.example.nshutiplanner.unity

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
// UnityPlayer is provided at runtime when the Unity .aar is linked as a dependency.
// It is not available at compile time in a standard Android project build.
// Add the Unity .aar to build.gradle (flatDir / implementation files(...)) to resolve this import.
import com.unity3d.player.UnityPlayer

class UnityBridge {
    companion object {
        private const val TAG = "UnityBridge"

        /**
         * Fetches the GPS location for the user identified by [email] from Firestore and
         * delivers the result back to Unity via [UnityPlayer.UnitySendMessage].
         *
         * Firestore path:
         *   users → where("email", ==, email) → limit(1) → get uid / displayName
         *   locations/{uid} → get latitude / longitude
         *
         * Requirements: 3.2, 3.3, 3.4, 6.1, 6.2, 6.3
         */
        @JvmStatic
        fun fetchLocationByEmail(
            context: Context,
            email: String,
            callbackObjectName: String,
            callbackMethodName: String
        ) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = FirebaseFirestore.getInstance()

                    // Step 1: resolve user by email
                    val userQuery = db.collection("users")
                        .whereEqualTo("email", email)
                        .limit(1)
                        .get()
                        .await()

                    if (userQuery.isEmpty) {
                        val json = BridgeResponse(
                            success = false,
                            error = "User not found for email: $email"
                        ).toJson()
                        UnityPlayer.UnitySendMessage(callbackObjectName, callbackMethodName, json)
                        return@launch
                    }

                    val userDoc = userQuery.documents[0]
                    val uid = userDoc.id
                    val displayName = userDoc.getString("displayName") ?: ""

                    // Step 2: fetch location record
                    val locationDoc = db.collection("locations")
                        .document(uid)
                        .get()
                        .await()

                    if (!locationDoc.exists()) {
                        val json = BridgeResponse(
                            success = false,
                            error = "Location not available for user"
                        ).toJson()
                        UnityPlayer.UnitySendMessage(callbackObjectName, callbackMethodName, json)
                        return@launch
                    }

                    val lat = locationDoc.getDouble("latitude") ?: 0.0
                    val lng = locationDoc.getDouble("longitude") ?: 0.0

                    val json = BridgeResponse(
                        success = true,
                        latitude = lat,
                        longitude = lng,
                        displayName = displayName
                    ).toJson()
                    UnityPlayer.UnitySendMessage(callbackObjectName, callbackMethodName, json)

                } catch (e: Exception) {
                    Log.e(TAG, "fetchLocationByEmail failed", e)
                    val json = BridgeResponse(
                        success = false,
                        error = e.message ?: "Unknown error"
                    ).toJson()
                    UnityPlayer.UnitySendMessage(callbackObjectName, callbackMethodName, json)
                }
            }
        }

        @JvmStatic
        @Suppress("DEPRECATION")
        fun triggerVibration(context: Context) {
            // Guard: check VIBRATE permission
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.VIBRATE)
                != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                Log.w(TAG, "VIBRATE permission not granted")
                return
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // API 31+: use VibratorManager
                val vibratorManager =
                    context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                if (vibratorManager == null) {
                    Log.w(TAG, "Vibrator service unavailable")
                    return
                }
                val effect = VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE)
                val combinedVibration = android.os.CombinedVibration.createParallel(effect)
                vibratorManager.vibrate(combinedVibration)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // API 26–30: use VibrationEffect
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (vibrator == null) {
                    Log.w(TAG, "Vibrator service unavailable")
                    return
                }
                vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                // Legacy: API < 26
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (vibrator == null) {
                    Log.w(TAG, "Vibrator service unavailable")
                    return
                }
                vibrator.vibrate(500)
            }
        }
    }
}
