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
import java.lang.reflect.Method

class UnityBridge {
    companion object {
        private const val TAG = "UnityBridge"

        /**
         * Safely sends a message to Unity using reflection to avoid compile-time dependency on UnityPlayer.
         */
        private fun unitySendMessage(obj: String, method: String, msg: String) {
            try {
                val cls = Class.forName("com.unity3d.player.UnityPlayer")
                val methodObj: Method = cls.getMethod("UnitySendMessage", String::class.java, String::class.java, String::class.java)
                methodObj.invoke(null, obj, method, msg)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send message to Unity: ${e.message}")
            }
        }

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
                        unitySendMessage(callbackObjectName, callbackMethodName, json)
                        return@launch
                    }

                    val userDoc = userQuery.documents[0]
                    val uid = userDoc.id
                    val displayName = userDoc.getString("displayName") ?: ""

                    val locationDoc = db.collection("locations")
                        .document(uid)
                        .get()
                        .await()

                    if (!locationDoc.exists()) {
                        val json = BridgeResponse(
                            success = false,
                            error = "Location not available for user"
                        ).toJson()
                        unitySendMessage(callbackObjectName, callbackMethodName, json)
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
                    unitySendMessage(callbackObjectName, callbackMethodName, json)

                } catch (e: Exception) {
                    Log.e(TAG, "fetchLocationByEmail failed", e)
                    val json = BridgeResponse(
                        success = false,
                        error = e.message ?: "Unknown error"
                    ).toJson()
                    unitySendMessage(callbackObjectName, callbackMethodName, json)
                }
            }
        }

        @JvmStatic
        @Suppress("DEPRECATION")
        fun triggerVibration(context: Context) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.VIBRATE)
                != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                Log.w(TAG, "VIBRATE permission not granted")
                return
            }

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    if (vibratorManager != null) {
                        val effect = VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE)
                        val combinedVibration = android.os.CombinedVibration.createParallel(effect)
                        vibratorManager.vibrate(combinedVibration)
                    }
                } else {
                    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                    if (vibrator != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
                        } else {
                            vibrator.vibrate(500)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Vibration failed", e)
            }
        }
    }
}
