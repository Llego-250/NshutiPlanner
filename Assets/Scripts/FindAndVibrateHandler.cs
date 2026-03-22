using System.Collections;
using UnityEngine;

// FindAndVibrateHandler orchestrates the full Find & Vibrate flow.
// Attach this MonoBehaviour to the canvas object that owns the NavbarButton.
// See: Requirements 1.1, 1.2, 1.3, 1.4, 1.5, 4.1, 4.2, 4.3, 4.4, 4.5
public class FindAndVibrateHandler : MonoBehaviour
{
    // ── Scene references ──────────────────────────────────────────────────────
    [SerializeField] private AndroidBridge bridge;
    [SerializeField] private UnityEngine.UI.Button navbarButton;
    [SerializeField] private TMPro.TextMeshProUGUI statusLabel;
    [SerializeField] private GameObject locationMarkerPrefab;
    [SerializeField] private Transform mapPlane;

    // ── GPS origin and scale ──────────────────────────────────────────────────
    // Configurable origin coordinate (default: 0,0 lat/lng).
    // See: Requirement 4.5
    [SerializeField] private double originLatitude = 0.0;
    [SerializeField] private double originLongitude = 0.0;

    // 1 Unity unit = 1 metre by default.
    // See: Requirement 4.5
    [SerializeField] private float metresPerUnit = 1.0f;

    // ── Target email ──────────────────────────────────────────────────────────
    [SerializeField] private string targetEmail = "";

    // ── Runtime state ─────────────────────────────────────────────────────────
    // Tracks the single active marker so we reposition rather than duplicate.
    // See: Requirement 4.4
    private GameObject _activeMarker;

    /// <summary>
    /// Entry point wired to the NavbarButton's OnClick event.
    /// Disables the button, shows a status message, and kicks off the bridge call.
    /// See: Requirements 1.2, 1.3
    /// </summary>
    public void FindAndVibrate()
    {
        navbarButton.interactable = false;
        statusLabel.text = "Locating...";
        bridge.FetchLocation(targetEmail, OnLocationReceived);
    }

    /// <summary>
    /// Callback invoked by AndroidBridge when the location fetch completes.
    /// Handles both success and error paths.
    /// See: Requirements 1.4, 1.5, 4.1, 4.2, 4.3, 4.4
    /// </summary>
    private void OnLocationReceived(BridgeResponse response)
    {
        if (response.success)
        {
            // Trigger haptic feedback on the Android device.
            // See: Requirement 1.4
            bridge.TriggerVibration();

            // Convert GPS coordinates to Unity world-space position.
            // MetresPerDegree ≈ 111 320 m (latitude approximation).
            // See: Requirement 4.5
            float worldX = (float)((response.longitude - originLongitude) * 111320.0 * metresPerUnit);
            float worldZ = (float)((response.latitude  - originLatitude)  * 111320.0 * metresPerUnit);
            Vector3 worldPos = new Vector3(worldX, 0f, worldZ);

            // Reposition existing marker or instantiate a new one.
            // See: Requirement 4.4
            if (_activeMarker != null)
            {
                _activeMarker.transform.position = worldPos;
            }
            else
            {
                _activeMarker = Instantiate(locationMarkerPrefab, worldPos, Quaternion.identity);
            }

            // Update the floating name-tag label on the marker.
            // See: Requirement 4.3
            TMPro.TextMeshPro markerLabel = _activeMarker.GetComponentInChildren<TMPro.TextMeshPro>();
            if (markerLabel != null)
            {
                markerLabel.text = response.displayName;
            }

            // Show success status and re-enable the button after a short delay.
            // See: Requirements 1.4
            statusLabel.text = "✓ Vibration sent to " + response.displayName + "!";
            StartCoroutine(ReEnableButtonAfterDelay(2f));
        }
        else
        {
            // Surface the error message and immediately re-enable the button.
            // See: Requirement 1.5
            statusLabel.text = response.error;
            navbarButton.interactable = true;
        }
    }

    /// <summary>
    /// Waits for the specified number of seconds, then re-enables the NavbarButton.
    /// See: Requirement 1.4
    /// </summary>
    private IEnumerator ReEnableButtonAfterDelay(float seconds)
    {
        yield return new WaitForSeconds(seconds);
        navbarButton.interactable = true;
    }
}
