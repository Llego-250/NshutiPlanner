package com.example.nshutiplanner.unity

import android.content.Context
import android.content.Intent

// UnityPlayerActivity is only available at runtime when the Unity .aar is linked.
// At compile time this class will not resolve unless the Unity Android SDK .aar is
// added as a dependency in build.gradle (see Requirement 5.4).
class UnityBridgeActivity : com.unity3d.player.UnityPlayerActivity() {

    companion object {
        fun newIntent(context: Context): Intent =
            Intent(context, UnityBridgeActivity::class.java)
    }
}
