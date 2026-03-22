# Design Document: unity-android-location-vibration-bridge

## Overview

This feature embeds a Unity 3D scene inside the NshutiPlanner Android app. The scene displays a 3D location marker at a partner's GPS coordinates and triggers a native Android haptic pulse — all driven by a bidirectional Kotlin↔Unity bridge.

The bridge is built on two pillars:

- **Kotlin side** (`UnityBridge`): a `@JvmStatic`-annotated class that delegates to the existing `LocationRepository` and `NshutiFirebaseMessagingService`, then calls back into Unity via `UnityPlayer.UnitySendMessage`.
- **C# side** (`AndroidBridge`): a Unity MonoBehaviour that wraps all `AndroidJavaObject` calls, guards against non-Android platforms with `#if` preprocessor directives, and surfaces all errors as `BridgeResponse` objects.

The Unity scene (`UnityLocationScene`) hosts a uGUI canvas with a `NavbarButton`, a 3D `LocationMarker` GameObject, and a TextMeshPro label. The scene is launched from `MainActivity` via an explicit `Intent` targeting `UnityBridgeActivity`, which extends `UnityPlayerActivity`.

---

## Architecture

```mermaid
graph TD
    subgraph Android App
        MA[MainActivity] -->|Intent| UBA[UnityBridgeActivity\nextends UnityPlayerActivity]
        UBA --> UB[UnityBridge.kt\n@JvmStatic methods]
        UB -->|delegates| LR[LocationRepository]
        UB -->|delegates| FCM[NshutiFirebaseMessagingService\n.triggerHapticPulse]
        LR --> FS[(Firestore)]
    end

    subgraph Unity Scene
        NB[NavbarButton\nuGUI Button] --> FV[FindAndVibrateHandler.cs]
        FV --> AB[AndroidBridge.cs\nAndroidJavaObject wrapper]
        AB -->|AndroidJavaObject call| UB
        UB -->|UnitySendMessage| FV
        FV --> LM[LocationMarker\nGameObject]
        FV --> TL[TextMeshPro Label]
    end
```

### Key Design Decisions

1. **Delegate, don't duplicate**: `UnityBridge` calls `LocationRepository.fetchReceiverLocation` and `NshutiFirebaseMessagingService.triggerHapticPulse` directly. No location or vibration logic is re-implemented.
2. **Callback via `UnitySendMessage`**: Because `AndroidJavaObject` calls block the calling thread, `fetchLocationByEmail` runs on a coroutine and calls back into Unity asynchronously. This avoids blocking the Unity main thread.
3. **Single bridge entry point**: `AndroidBridge.cs` is the only C# class that instantiates `AndroidJavaObject`. All other scripts call `AndroidBridge` methods.
4. **Platform guards**: `#if UNITY_ANDROID && !UNITY_EDITOR` isolates all native calls; the Editor path returns configurable stubs so the scene can be developed without a device.
5. **Activity launch strategy**: `UnityBridgeActivity` is launched via an explicit `Intent` from `MainActivity`, keeping the Unity runtime lifecycle separate from the Compose navigation stack.

---

## Components and Interfaces

### Android / Kotlin Components

#### `UnityBridge.kt`
Path: `app/src/main/java/com/example/nshutiplanner/unity/UnityBridge.kt`

```kotlin
class UnityBridge(private val context: Context) {
    companion object {
        @JvmStatic
        fun fetchLocationByEmail(
            email: String,
            callbackObjectName: String,
            callbackMethodName: String
        )

        @JvmStatic
        fun triggerVibration()
    }
}
```

- `fetchLocationByEmail` launches a coroutine, queries Firestore for the user by email, reads their `LocationRecord`, serializes a `BridgeResponse` to JSON, and calls `UnityPlayer.UnitySendMessage(callbackObjectName, callbackMethodName, json)`.
- `triggerVibration` calls `NshutiFirebaseMessagingService`'s haptic logic directly (extracted to a static helper or duplicated minimally for the non-service context), handling API 26+ and legacy paths.

#### `UnityBridgeActivity.kt`
Path: `app/src/main/java/com/example/nshutiplanner/unity/UnityBridgeActivity.kt`

Extends `UnityPlayerActivity`. No additional logic required beyond the superclass; serves as the declared `<activity>` entry in the manifest so the Unity runtime can be hosted.

#### `BridgeResponse.kt` (Kotlin data class)
```kotlin
data class BridgeResponse(
    val success: Boolean = false,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val displayName: String = "",
    val error: String = ""
)
```
Serialized to JSON using `org.json.JSONObject` (already a transitive dependency via Firebase).

---

### Unity / C# Components

#### `AndroidBridge.cs`
Single MonoBehaviour that wraps all `AndroidJavaObject` interactions.

```csharp
public class AndroidBridge : MonoBehaviour
{
    // Editor stub configuration
    [SerializeField] private bool stubSuccess = true;
    [SerializeField] private double stubLatitude = -1.9441;
    [SerializeField] private double stubLongitude = 30.0619;
    [SerializeField] private string stubDisplayName = "Test Partner";

    public void FetchLocation(string email, Action<BridgeResponse> callback);
    public void TriggerVibration();
}
```

- On Android: uses `AndroidJavaObject("com.example.nshutiplanner.unity.UnityBridge")` to call `fetchLocationByEmail` and `triggerVibration`.
- In Editor: `FetchLocation` waits 1 second (coroutine) then invokes callback with stub data; `TriggerVibration` logs a message.
- All `AndroidJavaException` and general exceptions are caught and surfaced as `BridgeResponse { success = false, error = message }`.

#### `BridgeResponse.cs`
```csharp
[Serializable]
public class BridgeResponse
{
    public bool success;
    public double latitude;
    public double longitude;
    public string displayName;
    public string error;
}
```
Deserialized from JSON using `JsonUtility.FromJson<BridgeResponse>`.

#### `FindAndVibrateHandler.cs`
MonoBehaviour attached to the `NavbarButton`'s parent canvas object. Orchestrates the full flow:
1. Disables `NavbarButton` interactability.
2. Calls `AndroidBridge.FetchLocation(email, OnLocationReceived)`.
3. In `OnLocationReceived`: calls `AndroidBridge.TriggerVibration()`, repositions `LocationMarker`, updates the TextMeshPro label.
4. Re-enables `NavbarButton` and shows success/error label for 2 seconds.

#### Unity Scene: `UnityLocationScene`
- Canvas (Screen Space – Overlay)
  - `NavbarButton` (uGUI Button)
  - Status label (TextMeshPro)
- Map plane (flat quad, world space)
- `LocationMarker` prefab (sphere, cyan material)
  - Child: TextMeshPro label (floating name tag)

---

## Data Models

### `BridgeResponse` JSON Schema
```json
{
  "success": true,
  "latitude": -1.9441,
  "longitude": 30.0619,
  "displayName": "Alice",
  "error": ""
}
```

Field rules:
- `success = false` → `error` is non-empty; `latitude`/`longitude`/`displayName` are zero/empty.
- `success = true` → `error` is empty; `latitude`/`longitude` are valid doubles; `displayName` is non-empty.

### GPS → Unity World-Space Mapping
```
worldX = (longitude - originLongitude) * MetresPerDegree * MetresPerUnit
worldZ = (latitude  - originLatitude)  * MetresPerDegree * MetresPerUnit
worldY = 0  (marker sits on the map plane)
```
Where `MetresPerDegree ≈ 111_320` (latitude) and `MetresPerUnit` defaults to 1.0 (configurable `[SerializeField]`).

### Firestore Query Path (inside `UnityBridge`)
```
users collection → where("email", "==", email) → limit(1)
  → get uid
  → locations/{uid} → get LocationRecord
```

### Existing Models Reused
- `LocationRecord` (from `Models.kt`) — read directly from Firestore.
- `Actor` / `Role` — not used by `UnityBridge`; location is fetched by email lookup, bypassing the partner-resolution flow.
- `User` — queried by email to obtain `uid` and `displayName`.

---

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system — essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: Button disabled during in-progress state

*For any* invocation of `FindAndVibrate` while a fetch or dispatch is already in progress, the `NavbarButton` interactability flag SHALL be `false` until the flow reaches a terminal state (success or error).

**Validates: Requirements 1.3**

---

### Property 2: Error response surfaces error message in UI

*For any* `BridgeResponse` with `success = false` and a non-empty `error` string, the UI status label text SHALL equal the `error` field of that response after the flow completes.

**Validates: Requirements 1.5**

---

### Property 3: FetchLocation callback always invoked

*For any* call to `AndroidBridge.FetchLocation(email, callback)`, the `callback` SHALL be invoked exactly once with a `BridgeResponse` object — regardless of whether the call succeeds, fails, or throws an exception.

**Validates: Requirements 2.2, 2.5, 6.5**

---

### Property 4: AndroidJavaObject exceptions caught and surfaced

*For any* exception thrown during an `AndroidJavaObject` call inside `AndroidBridge`, the resulting `BridgeResponse` passed to the callback SHALL have `success = false` and `error` equal to the exception's message string.

**Validates: Requirements 2.5, 6.5**

---

### Property 5: fetchLocationByEmail success path produces correct BridgeResponse

*For any* email that matches a Firestore `users` document that has a corresponding `LocationRecord`, calling `fetchLocationByEmail` SHALL result in `UnitySendMessage` being called with a JSON string that deserializes to a `BridgeResponse` where `success = true`, `latitude` and `longitude` match the stored `LocationRecord`, and `displayName` matches the user's `displayName`.

**Validates: Requirements 3.2, 3.3**

---

### Property 6: fetchLocationByEmail failure path produces success=false BridgeResponse

*For any* email that does not match a Firestore user, or any user with no `LocationRecord`, or any Firestore exception during the query, calling `fetchLocationByEmail` SHALL result in `UnitySendMessage` being called with a JSON string that deserializes to a `BridgeResponse` where `success = false` and `error` is a non-empty human-readable string.

**Validates: Requirements 3.4, 6.1, 6.2, 6.3**

---

### Property 7: triggerVibration never throws

*For any* Android API level (including API < 26) and any vibrator service availability state, calling `UnityBridge.triggerVibration()` SHALL complete without throwing an exception.

**Validates: Requirements 3.5, 3.6, 6.4**

---

### Property 8: LocationMarker world position matches GPS-to-world mapping

*For any* `BridgeResponse` with `success = true` containing latitude `lat` and longitude `lng`, the `LocationMarker` GameObject's world-space position SHALL equal `((lng - originLng) * MetresPerDegree * MetresPerUnit, 0, (lat - originLat) * MetresPerDegree * MetresPerUnit)` using the configured origin and scale.

**Validates: Requirements 4.1, 4.5**

---

### Property 9: displayName label matches BridgeResponse

*For any* `BridgeResponse` with `success = true`, the TextMeshPro label floating above the `LocationMarker` SHALL display exactly the `displayName` field of that response.

**Validates: Requirements 4.3**

---

### Property 10: Exactly one LocationMarker after any number of successful responses

*For any* sequence of one or more `BridgeResponse` objects with `success = true` processed by the scene, the total count of `LocationMarker` GameObjects in the scene SHALL be exactly 1 after each response is handled.

**Validates: Requirements 4.4**

---

### Property 11: BridgeResponse JSON round-trip preserves all field values

*For any* `BridgeResponse` object (Kotlin `data class` or C# class), serializing it to a JSON string and then deserializing that string SHALL produce an object with identical values for all fields: `success`, `latitude`, `longitude`, `displayName`, and `error`.

**Validates: Requirements 7.3, 7.4**

---

### Property 12: Bridge error logs contain [AndroidBridge] prefix

*For any* error condition encountered inside `AndroidBridge.cs`, the log message emitted via `UnityEngine.Debug.LogError` SHALL begin with the string `"[AndroidBridge]"`.

**Validates: Requirements 6.6**

---

## Error Handling

### Kotlin (`UnityBridge`) Error Paths

| Condition | Behavior |
|---|---|
| Email not found in Firestore | Return `BridgeResponse(success=false, error="User not found for email: <email>")` |
| User found but no `LocationRecord` | Return `BridgeResponse(success=false, error="Location not available for user")` |
| Firestore query throws | Catch, log with `Log.e`, return `BridgeResponse(success=false, error=e.message)` |
| `VIBRATE` permission absent | Log with `Log.w`, return without calling vibrator |
| Vibrator service null | Log with `Log.w`, return without crashing |

All error responses are serialized to JSON and delivered via `UnityPlayer.UnitySendMessage` — the Unity side always receives a response, never silence.

### C# (`AndroidBridge`) Error Paths

| Condition | Behavior |
|---|---|
| `AndroidJavaException` thrown | Catch, `Debug.LogError("[AndroidBridge] ...")`, invoke callback with `success=false` |
| General `Exception` thrown | Same as above |
| Non-Android platform | Return stub `BridgeResponse(success=false, error="Android platform required")` immediately |
| JSON parse failure in `BridgeResponse` | `JsonUtility.FromJson` returns default struct; `success` will be `false` by default |

### Unity Scene Error Paths

| Condition | Behavior |
|---|---|
| `BridgeResponse.success = false` | Display `error` string in status label; re-enable `NavbarButton` |
| `BridgeResponse.displayName` empty | Label shows empty string (no crash) |
| Marker repositioning while scene is unloading | Guard with null check on `LocationMarker` reference |

---

## Testing Strategy

### Dual Testing Approach

Both unit tests and property-based tests are required. Unit tests cover specific examples and integration points; property tests verify universal correctness across generated inputs.

### Unit Tests (Kotlin — JUnit 4 + Mockito)

Focus areas:
- `UnityBridge.fetchLocationByEmail` with a mocked Firestore: verify correct JSON shape for success and each error case.
- `UnityBridge.triggerVibration` on mocked `Vibrator` / `VibratorManager`: verify no exception on API 26+ and legacy paths.
- `BridgeResponse` Kotlin serialization: verify JSON output matches expected schema.
- `UnityBridgeActivity` intent launch: verify activity starts without crash (instrumented test).

### Unit Tests (C# — Unity Test Framework, EditMode)

Focus areas:
- `AndroidBridge` Editor stub: verify `FetchLocation` invokes callback with configured stub values after ~1 second.
- `AndroidBridge` Editor stub: verify `TriggerVibration` logs the expected stub message.
- `FindAndVibrateHandler` state machine: verify button is disabled on start, re-enabled on success/error.
- `BridgeResponse` C# deserialization: verify `JsonUtility.FromJson` round-trip for all field types.

### Property-Based Tests

**Kotlin — using [kotest](https://kotest.io/) property testing module**

Each property test runs a minimum of **100 iterations**.

Tag format: `Feature: unity-android-location-vibration-bridge, Property {N}: {property_text}`

| Property | Test Description |
|---|---|
| P5 | Generate random valid `(email, LocationRecord)` pairs; verify `fetchLocationByEmail` JSON has `success=true` and matching lat/lng/displayName |
| P6 | Generate random unknown emails and random Firestore exception messages; verify `success=false` and non-empty `error` |
| P7 | Generate random API levels (< 26, 26–30, 31+); verify `triggerVibration()` completes without exception |
| P11 | Generate random `BridgeResponse` instances; serialize to JSON with `org.json.JSONObject`, deserialize back; verify all fields equal |

**C# — using [Unity Test Framework](https://docs.unity3d.com/Packages/com.unity.test-framework@1.1/manual/index.html) with a simple property harness**

Since Unity's test framework does not include a built-in PBT library, use [FsCheck for Unity](https://github.com/fscheck/FsCheck) or a lightweight custom generator loop (minimum 100 iterations).

| Property | Test Description |
|---|---|
| P3 | Generate random email strings; call `FetchLocation` in Editor mode; verify callback invoked exactly once |
| P4 | Simulate `AndroidJavaException` with random messages; verify callback receives `success=false` and matching `error` |
| P8 | Generate random `(lat, lng, originLat, originLng, MetresPerUnit)` tuples; verify world-space position formula |
| P9 | Generate random `displayName` strings in `BridgeResponse`; verify label text equals `displayName` |
| P10 | Generate sequences of 1–20 successful `BridgeResponse` objects; verify marker count is always 1 |
| P11 | Generate random `BridgeResponse` C# instances; serialize with `JsonUtility.ToJson`, deserialize with `JsonUtility.FromJson`; verify all fields equal |
| P12 | Simulate random error conditions; verify all `Debug.LogError` calls start with `"[AndroidBridge]"` |

### Manifest / Build Verification (Gradle check task)

- Assert `AndroidManifest.xml` contains `VIBRATE`, `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION` permissions.
- Assert `UnityBridgeActivity` is declared with required `configChanges`.
- These can be implemented as a simple Gradle task or a shell-based CI check.
