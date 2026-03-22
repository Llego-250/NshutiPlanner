# Keep UnityBridge methods callable from Unity via AndroidJavaObject
-keep class com.example.nshutiplanner.unity.UnityBridge {
    @com.unity3d.player.UnityCallbackHandler *;
    public static *;
}
-keep class com.example.nshutiplanner.unity.BridgeResponse { *; }
