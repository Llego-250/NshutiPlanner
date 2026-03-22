using System;
using System.Collections;
using UnityEngine;

// AndroidBridge is the single point of contact between Unity C# and the Kotlin UnityBridge.
// No other C# script should instantiate AndroidJavaObject directly for bridge calls.
// See: Requirements 7.1, 8.1
public class AndroidBridge : MonoBehaviour
{
    // Editor stub configuration — tweak in the Inspector without recompiling.
    // See: Requirements 8.2, 8.4
    [SerializeField] private bool stubSuccess = true;
    [SerializeField] private double stubLatitude = -1.9441;
    [SerializeField] private double stubLongitude = 30.0619;
    [SerializeField] private string stubDisplayName = "Test Partner";

    // Stores the pending callback supplied by FetchLocation so that
    // OnLocationReceived (called via UnitySendMessage) can invoke it.
    private Action<BridgeResponse> _pendingCallback;

    /// <summary>
    /// Fetches the location for the given email by calling the Kotlin UnityBridge.
    /// The callback is invoked exactly once with a BridgeResponse on completion.
    /// See: Requirements 2.2, 2.4, 2.5, 8.1, 8.2
    /// </summary>
    public void FetchLocation(string email, Action<BridgeResponse> callback)
    {
        _pendingCallback = callback;

#if UNITY_ANDROID && !UNITY_EDITOR
        try
        {
            AndroidJavaObject unityBridge = new AndroidJavaObject("com.example.nshutiplanner.unity.UnityBridge");
            AndroidJavaClass unityPlayer = new AndroidJavaClass("com.unity3d.player.UnityPlayer");
            AndroidJavaObject activity = unityPlayer.GetStatic<AndroidJavaObject>("currentActivity");
            unityBridge.CallStatic("fetchLocationByEmail", activity, email, gameObject.name, "OnLocationReceived");
        }
        catch (AndroidJavaException e)
        {
            Debug.LogError("[AndroidBridge] FetchLocation failed: " + e.Message);
            _pendingCallback = null;
            callback?.Invoke(new BridgeResponse { success = false, error = e.Message });
        }
        catch (Exception e)
        {
            Debug.LogError("[AndroidBridge] FetchLocation failed: " + e.Message);
            _pendingCallback = null;
            callback?.Invoke(new BridgeResponse { success = false, error = e.Message });
        }
#else
        // Editor stub: simulate async response after 1 second.
        // See: Requirements 8.2, 8.4
        StartCoroutine(StubFetchCoroutine(callback));
#endif
    }

    /// <summary>
    /// Called by Unity's UnitySendMessage from the Kotlin side when location data is ready.
    /// Deserializes the JSON payload and invokes the pending callback.
    /// See: Requirements 2.2, 3.3, 3.4
    /// </summary>
    public void OnLocationReceived(string json)
    {
        try
        {
            // Note: JsonUtility has limited double precision but works for GPS coordinate values.
            BridgeResponse response = JsonUtility.FromJson<BridgeResponse>(json);
            if (_pendingCallback != null)
            {
                Action<BridgeResponse> cb = _pendingCallback;
                _pendingCallback = null;
                cb.Invoke(response);
            }
        }
        catch (Exception e)
        {
            Debug.LogError("[AndroidBridge] OnLocationReceived parse error: " + e.Message);
        }
    }

    /// <summary>
    /// Triggers a native Android vibration via the Kotlin UnityBridge.
    /// In the Editor, logs a stub message instead.
    /// See: Requirements 2.3, 8.3
    /// </summary>
    public void TriggerVibration()
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        try
        {
            AndroidJavaObject unityBridge = new AndroidJavaObject("com.example.nshutiplanner.unity.UnityBridge");
            AndroidJavaClass unityPlayer = new AndroidJavaClass("com.unity3d.player.UnityPlayer");
            AndroidJavaObject activity = unityPlayer.GetStatic<AndroidJavaObject>("currentActivity");
            unityBridge.CallStatic("triggerVibration", activity);
        }
        catch (Exception e)
        {
            Debug.LogError("[AndroidBridge] TriggerVibration failed: " + e.Message);
        }
#else
        // Editor stub — no native vibration available.
        // See: Requirement 8.3
        Debug.Log("[AndroidBridge] Vibration stub called (Editor)");
#endif
    }

    /// <summary>
    /// Coroutine used in the Unity Editor to simulate the async Kotlin callback.
    /// Waits 1 second then invokes the callback with configured stub values.
    /// See: Requirements 8.2, 8.4
    /// </summary>
    private IEnumerator StubFetchCoroutine(Action<BridgeResponse> callback)
    {
        yield return new WaitForSeconds(1f);
        callback?.Invoke(new BridgeResponse
        {
            success = stubSuccess,
            // Cast double stub fields to float for BridgeResponse (JsonUtility compatibility).
            latitude = (float)stubLatitude,
            longitude = (float)stubLongitude,
            displayName = stubDisplayName,
            error = stubSuccess ? "" : "Android platform required"
        });
    }
}
