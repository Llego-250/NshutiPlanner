package com.example.nshutiplanner.unity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Hosts the Unity runtime when the Unity .aar is present.
 * Falls back to a placeholder screen when the .aar is not yet linked,
 * so the button always navigates somewhere instead of crashing silently.
 */
class UnityBridgeActivity : Activity() {

    private var unityPlayer: Any? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val unityLoaded = tryLoadUnity()
        if (!unityLoaded) {
            showPlaceholder()
        }
    }

    /** Returns true if UnityPlayer was instantiated successfully. */
    private fun tryLoadUnity(): Boolean {
        return try {
            val unityPlayerClass = Class.forName("com.unity3d.player.UnityPlayer")
            val constructor = unityPlayerClass.getConstructor(Context::class.java)
            unityPlayer = constructor.newInstance(this)

            val layout = FrameLayout(this)
            layout.addView(
                unityPlayer as android.view.View,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setContentView(layout)
            (unityPlayer as android.view.View).requestFocus()
            true
        } catch (e: Exception) {
            Log.w("UnityBridgeActivity", "UnityPlayer not available (aar not linked): ${e.message}")
            false
        }
    }

    /** Shown when the Unity .aar is not yet integrated. */
    private fun showPlaceholder() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#0D0D1A"))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val title = TextView(this).apply {
            text = "Unity 3D View"
            textSize = 24f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(32, 0, 32, 16)
        }

        val subtitle = TextView(this).apply {
            text = "The Unity scene will appear here once the Unity Android SDK (.aar) is added to app/libs/ and the project is rebuilt."
            textSize = 14f
            setTextColor(Color.parseColor("#B0A8CC"))
            gravity = Gravity.CENTER
            setPadding(48, 0, 48, 48)
        }

        val backBtn = Button(this).apply {
            text = "Go Back"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#6C3FC5"))
            setPadding(64, 24, 64, 24)
            setOnClickListener { finish() }
        }

        root.addView(title)
        root.addView(subtitle)
        root.addView(backBtn)
        setContentView(root)
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
            val method = unityPlayer?.javaClass?.getMethod(
                "windowFocusChanged", Boolean::class.javaPrimitiveType
            )
            method?.invoke(unityPlayer, hasFocus)
        } catch (_: Exception) {}
    }

    private fun callUnityMethod(name: String) {
        try {
            unityPlayer?.javaClass?.getMethod(name)?.invoke(unityPlayer)
        } catch (_: Exception) {}
    }

    companion object {
        fun newIntent(context: Context): Intent =
            Intent(context, UnityBridgeActivity::class.java)
    }
}
