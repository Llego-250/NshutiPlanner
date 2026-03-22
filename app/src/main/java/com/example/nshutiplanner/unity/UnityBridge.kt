package com.example.nshutiplanner.unity

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.content.ContextCompat

class UnityBridge {
    companion object {
        private const val TAG = "UnityBridge"

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
