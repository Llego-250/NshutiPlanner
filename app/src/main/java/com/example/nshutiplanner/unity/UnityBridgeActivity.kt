package com.example.nshutiplanner.unity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout

/**
 * A wrapper Activity that handles the Unity runtime using reflection.
 * This allows the project to compile without a direct dependency on the Unity Android SDK .aar,
 * which is only available after exporting the Unity project to Android.
 */
class UnityBridgeActivity : Activity() {

    private var unityPlayer: Any? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            // Attempt to instantiate com.unity3d.player.UnityPlayer via reflection
            val unityPlayerClass = Class.forName("com.unity3d.player.UnityPlayer")
            val constructor = unityPlayerClass.getConstructor(Context::class.java)
            unityPlayer = constructor.newInstance(this)

            // UnityPlayer is a View (FrameLayout), so we can add it to our layout
            val layout = FrameLayout(this)
            layout.addView(unityPlayer as android.view.View, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setContentView(layout)

            // Request focus for the Unity view
            (unityPlayer as android.view.View).requestFocus()

        } catch (e: Exception) {
            Log.e("UnityBridgeActivity", "Failed to initialize UnityPlayer. Ensure unity-classes.aar is in app/libs. Error: ${e.message}")
            // Fallback: show an error or finish
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        callUnityMethod("resume")
    }

    override fun onPause() {
        super.onPause()
        callUnityMethod("pause")
    }

    override fun onDestroy() {
        callUnityMethod("quit")
        super.onDestroy()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        try {
            val method = unityPlayer?.javaClass?.getMethod("windowFocusChanged", Boolean::class.javaPrimitiveType)
            method?.invoke(unityPlayer, hasFocus)
        } catch (_: Exception) {}
    }

    private fun callUnityMethod(name: String) {
        try {
            val method = unityPlayer?.javaClass?.getMethod(name)
            method?.invoke(unityPlayer)
        } catch (_: Exception) {}
    }

    companion object {
        fun newIntent(context: Context): Intent =
            Intent(context, UnityBridgeActivity::class.java)
    }
}
