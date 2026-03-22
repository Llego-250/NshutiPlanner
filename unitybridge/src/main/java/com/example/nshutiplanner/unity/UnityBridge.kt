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

/**
 * Exposes native Android functionality (location fetch + vibration) to Unity via AndroidJavaObject.
 * All public methods are @JvmStatic so Unity C# can call them as static methods.
 *
 * UnitySendMessage is called via reflection so this library compiles without a hard
 * dependency on the Unity .aar — it only needs to be present at runtime.
 */
class UnityBridge {
    companion object {
        private const val TAG = "UnityBridge"

        /** Calls UnityPlayer.UnitySendMessage via reflection (no compile-time Unity dep). */
        private fun unitySendMessage(gameObject: String, method: String, message: String) {
            try {
                val cls = Class.forName("com.unity3d.player.UnityPlayer")
                val m: Method = cls.getMethod(
                    "UnitySendMessage",
                    String::class.java,
                    String::class.java,
                    String::class.java
                )
                m.invoke(null, gameObject, method, message)
            } catch (e: Exception) {
                Log.e(TAG, "UnitySendMessage failed (Unity .aar not linked?): ${e.message}")
            }
        }

        /**
         * Fetches the GPS location for [email] from Firestore and delivers the result
         * back to Unity via UnitySendMessage([callbackObjectName], [callbackMethodName], json).
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
                        unitySendMessage(
                            callbackObjectName, callbackMethodName,
                            BridgeResponse(success = false, error = "User not found for email: $email").toJson()
                        )
                        return@launch
                    }

                    val userDoc = userQuery.documents[0]
                    val uid = userDoc.id
                    val displayName = userDoc.getString("displayName") ?: ""

                    // Step 2: fetch location record
                    val locationDoc = db.collection("locations").document(uid).get().await()

                    if (!locationDoc.exists()) {
                        unitySendMessage(
                            callbackObjectName, callbackMethodName,
                            BridgeResponse(success = false, error = "Location not available for user").toJson()
                        )
                        return@launch
                    }

                    val lat = locationDoc.getDouble("latitude") ?: 0.0
                    val lng = locationDoc.getDouble("longitude") ?: 0.0

                    unitySendMessage(
                        callbackObjectName, callbackMethodName,
                        BridgeResponse(success = true, latitude = lat, longitude = lng, displayName = displayName).toJson()
                    )

                } catch (e: Exception) {
                    Log.e(TAG, "fetchLocationByEmail failed", e)
                    unitySendMessage(
                        callbackObjectName, callbackMethodName,
                        BridgeResponse(success = false, error = e.message ?: "Unknown error").toJson()
                    )
                }
            }
        }

        /** Triggers a 500 ms haptic pulse. Handles API 31+, 26–30, and legacy paths. */
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
                    val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                        ?: run { Log.w(TAG, "VibratorManager unavailable"); return }
                    vm.vibrate(
                        android.os.CombinedVibration.createParallel(
                            VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE)
                        )
                    )
                } else {
                    val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                        ?: run { Log.w(TAG, "Vibrator unavailable"); return }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        v.vibrate(500)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "triggerVibration failed", e)
            }
        }
    }
}
