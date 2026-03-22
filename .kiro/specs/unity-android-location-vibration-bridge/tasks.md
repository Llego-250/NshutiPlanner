# Implementation Plan: unity-android-location-vibration-bridge

## Overview

Implement the bidirectional Kotlin↔Unity bridge by building the Android-side Kotlin classes first, then the Unity C# components, wiring them together through the manifest and build configuration.

## Tasks

- [x] 1. Configure Android build and manifest for Unity integration
  - Add Unity `.aar` as a `flatDir` repository and `implementation` dependency in `app/build.gradle.kts`
  - Add `UnityBridgeActivity` entry to `AndroidManifest.xml` with `android:configChanges` covering `orientation|keyboardHidden|keyboard|screenSize|smallestScreenSize|locale|layoutDirection|fontScale|screenLayout|density|uiMode`
  - Confirm `VIBRATE`, `ACCESS_FINE_LOCATION`, and `ACCESS_COARSE_LOCATION` permissions are present (already declared; verify they are retained)
  - _Requirements: 5.1, 5.2, 5.3, 5.4_

- [x] 2. Implement Kotlin `BridgeResponse` data class and JSON serialization
  - [x] 2.1 Create `app/src/main/java/com/example/nshutiplanner/unity/BridgeResponse.kt`
    - Define `data class BridgeResponse(val success: Boolean, val latitude: Double, val longitude: Double, val displayName: String, val error: String)` with defaults
    - Add `fun toJson(): String` using `org.json.JSONObject` to serialize all five fields
    - Add `companion object { fun fromJson(json: String): BridgeResponse }` for symmetry
    - _Requirements: 7.3, 7.4_

  - [ ]* 2.2 Write property test for `BridgeResponse` JSON round-trip (Property 11)
    - **Property 11: BridgeResponse JSON round-trip preserves all field values**
    - **Validates: Requirements 7.3, 7.4**
    - Use kotest property testing; generate random `BridgeResponse` instances; serialize with `toJson()`, deserialize with `fromJson()`; assert all fields equal
    - File: `app/src/test/java/com/example/nshutiplanner/unity/UnityBridgeTest.kt`

- [x] 3. Implement `UnityBridge.kt` — vibration method
  - [x] 3.1 Create `app/src/main/java/com/example/nshutiplanner/unity/UnityBridge.kt` with `triggerVibration()`
    - Implement `companion object` with `@JvmStatic fun triggerVibration(context: Context)`
    - Mirror the three-branch vibration logic from `NshutiFirebaseMessagingService.triggerHapticPulse()`: API 31+ (`VibratorManager`), API 26–30 (`VibrationEffect`), legacy (`vibrate(500)`)
    - Guard: if vibrator service is null or `VIBRATE` permission absent, log with `Log.w` and return without throwing
    - _Requirements: 3.1, 3.5, 3.6, 6.4_

  - [ ]* 3.2 Write property test for `triggerVibration` never throws (Property 7)
    - **Property 7: triggerVibration never throws**
    - **Validates: Requirements 3.5, 3.6, 6.4**
    - Use kotest; generate random API-level scenarios with mocked `Vibrator`/`VibratorManager`; assert no exception is thrown across ≥100 iterations
    - File: `app/src/test/java/com/example/nshutiplanner/unity/UnityBridgeTest.kt`

- [x] 4. Implement `UnityBridge.kt` — location fetch method
  - [x] 4.1 Add `@JvmStatic fun fetchLocationByEmail(email: String, callbackObjectName: String, callbackMethodName: String)` to `UnityBridge`
    - Launch a `CoroutineScope(Dispatchers.IO)` coroutine
    - Query Firestore `users` collection with `whereEqualTo("email", email).limit(1)`; if empty, call `UnitySendMessage` with `BridgeResponse(success=false, error="User not found for email: $email").toJson()`
    - If user found, query `locations/{uid}`; if missing, call `UnitySendMessage` with `BridgeResponse(success=false, error="Location not available for user").toJson()`
    - On success, call `UnitySendMessage(callbackObjectName, callbackMethodName, BridgeResponse(success=true, latitude=..., longitude=..., displayName=...).toJson())`
    - Wrap entire body in `try/catch`; on exception log with `Log.e` and call `UnitySendMessage` with `BridgeResponse(success=false, error=e.message ?: "Unknown error").toJson()`
    - _Requirements: 3.2, 3.3, 3.4, 6.1, 6.2, 6.3_

  - [ ]* 4.2 Write property test for `fetchLocationByEmail` success path (Property 5)
    - **Property 5: fetchLocationByEmail success path produces correct BridgeResponse**
    - **Validates: Requirements 3.2, 3.3**
    - Use kotest; generate random `(email, latitude, longitude, displayName)` tuples with mocked Firestore returning matching documents; assert `UnitySendMessage` is called with JSON where `success=true` and fields match
    - File: `app/src/test/java/com/example/nshutiplanner/unity/UnityBridgeTest.kt`

  - [ ]* 4.3 Write property test for `fetchLocationByEmail` failure paths (Property 6)
    - **Property 6: fetchLocationByEmail failure path produces success=false BridgeResponse**
    - **Validates: Requirements 3.4, 6.1, 6.2, 6.3**
    - Use kotest; generate random unknown emails, missing-location scenarios, and random exception messages; assert `success=false` and non-empty `error` in all cases across ≥100 iterations
    - File: `app/src/test/java/com/example/nshutiplanner/unity/UnityBridgeTest.kt`

- [-] 5. Implement `UnityBridgeActivity.kt`
  - Create `app/src/main/java/com/example/nshutiplanner/unity/UnityBridgeActivity.kt`
  - Extend `UnityPlayerActivity` (from the Unity `.aar`); no additional logic required
  - Add an `Intent`-launching helper `companion object { fun newIntent(context: Context): Intent }` for use from `MainActivity`
  - _Requirements: 5.5_

- [ ] 6. Checkpoint — Android side complete
  - Ensure all Kotlin unit tests pass, ask the user if questions arise.

- [~] 7. Implement Unity C# `BridgeResponse.cs`
  - Create `Assets/Scripts/BridgeResponse.cs`
  - Define `[Serializable] public class BridgeResponse` with fields: `public bool success`, `public double latitude`, `public double longitude`, `public string displayName`, `public string error`
  - No methods needed; deserialization is via `JsonUtility.FromJson<BridgeResponse>`
  - _Requirements: 7.3_

  - [ ]* 7.1 Write property test for C# `BridgeResponse` JSON round-trip (Property 11 — C# side)
    - **Property 11: BridgeResponse JSON round-trip preserves all field values (C#)**
    - **Validates: Requirements 7.3, 7.4**
    - File: `Assets/Tests/EditMode/AndroidBridgeTests.cs`; use a loop of ≥100 random instances; serialize with `JsonUtility.ToJson`, deserialize with `JsonUtility.FromJson`; assert all fields equal

- [~] 8. Implement Unity C# `AndroidBridge.cs`
  - [~] 8.1 Create `Assets/Scripts/AndroidBridge.cs` as a `MonoBehaviour`
    - Add `[SerializeField]` fields: `stubSuccess`, `stubLatitude` (default `-1.9441`), `stubLongitude` (default `30.0619`), `stubDisplayName` (default `"Test Partner"`)
    - Implement `public void FetchLocation(string email, Action<BridgeResponse> callback)`:
      - `#if UNITY_ANDROID && !UNITY_EDITOR`: instantiate `AndroidJavaObject("com.example.nshutiplanner.unity.UnityBridge")`, call `fetchLocationByEmail(email, gameObject.name, "OnLocationReceived")`; store callback for `OnLocationReceived`
      - `#else` (Editor): start coroutine that waits 1 second then invokes callback with stub `BridgeResponse`
      - Wrap `AndroidJavaObject` calls in `try/catch`; on exception call `Debug.LogError("[AndroidBridge] ...")` and invoke callback with `success=false`
    - Implement `public void OnLocationReceived(string json)`: deserialize JSON, invoke stored callback on main thread
    - Implement `public void TriggerVibration()`:
      - `#if UNITY_ANDROID && !UNITY_EDITOR`: call `triggerVibration()` via `AndroidJavaObject`; catch exceptions, log with `[AndroidBridge]` prefix
      - `#else`: log `"[AndroidBridge] Vibration stub called (Editor)"`
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 6.5, 6.6, 7.1, 8.1, 8.2, 8.3, 8.4_

  - [ ]* 8.2 Write property test: `FetchLocation` callback always invoked exactly once (Property 3)
    - **Property 3: FetchLocation callback always invoked**
    - **Validates: Requirements 2.2, 2.5, 6.5**
    - File: `Assets/Tests/EditMode/AndroidBridgeTests.cs`; generate ≥100 random email strings in Editor mode; assert callback invoked exactly once per call

  - [ ]* 8.3 Write property test: exceptions surfaced as `success=false` BridgeResponse (Property 4)
    - **Property 4: AndroidJavaObject exceptions caught and surfaced**
    - **Validates: Requirements 2.5, 6.5**
    - Simulate `AndroidJavaException` with random messages; assert callback receives `success=false` and `error` equals exception message

  - [ ]* 8.4 Write property test: all error logs contain `[AndroidBridge]` prefix (Property 12)
    - **Property 12: Bridge error logs contain [AndroidBridge] prefix**
    - **Validates: Requirements 6.6**
    - Intercept `Debug.LogError` calls during simulated error conditions; assert all messages start with `"[AndroidBridge]"`

- [~] 9. Implement Unity C# `FindAndVibrateHandler.cs`
  - [~] 9.1 Create `Assets/Scripts/FindAndVibrateHandler.cs` as a `MonoBehaviour`
    - Add `[SerializeField]` references: `AndroidBridge bridge`, `Button navbarButton`, `TextMeshProUGUI statusLabel`, `GameObject locationMarkerPrefab`, `Transform mapPlane`
    - Add `[SerializeField]` GPS origin and scale fields: `double originLatitude`, `double originLongitude`, `float metresPerUnit` (default `1.0`)
    - Add `[SerializeField] string targetEmail` for the email to look up
    - Implement `public void FindAndVibrate()`:
      1. Set `navbarButton.interactable = false`
      2. Call `bridge.FetchLocation(targetEmail, OnLocationReceived)`
    - Implement `private void OnLocationReceived(BridgeResponse response)`:
      - If `response.success`: call `bridge.TriggerVibration()`, reposition/instantiate `LocationMarker` using GPS→world formula, update TextMeshPro label with `response.displayName`, show success label, start coroutine to re-enable button after 2 seconds
      - If `!response.success`: display `response.error` in status label, re-enable button immediately
    - GPS→world formula: `worldX = (lng - originLongitude) * 111320 * metresPerUnit`, `worldZ = (lat - originLatitude) * 111320 * metresPerUnit`, `worldY = 0`
    - Guard marker repositioning with null check
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 4.1, 4.2, 4.3, 4.4, 4.5_

  - [ ]* 9.2 Write property test: `NavbarButton` disabled during in-progress state (Property 1)
    - **Property 1: Button disabled during in-progress state**
    - **Validates: Requirements 1.3**
    - File: `Assets/Tests/EditMode/AndroidBridgeTests.cs`; verify `navbarButton.interactable == false` immediately after `FindAndVibrate()` is called and before callback fires

  - [ ]* 9.3 Write property test: error response surfaces error message in UI (Property 2)
    - **Property 2: Error response surfaces error message in UI**
    - **Validates: Requirements 1.5**
    - Generate random error strings in `BridgeResponse(success=false, error=...)`; assert `statusLabel.text == response.error` after handler processes the response

  - [ ]* 9.4 Write property test: `LocationMarker` world position matches GPS mapping (Property 8)
    - **Property 8: LocationMarker world position matches GPS-to-world mapping**
    - **Validates: Requirements 4.1, 4.5**
    - Generate random `(lat, lng, originLat, originLng, metresPerUnit)` tuples; assert marker position equals formula result across ≥100 iterations

  - [ ]* 9.5 Write property test: `displayName` label matches BridgeResponse (Property 9)
    - **Property 9: displayName label matches BridgeResponse**
    - **Validates: Requirements 4.3**
    - Generate random `displayName` strings; assert `statusLabel` or marker label text equals `displayName`

  - [ ]* 9.6 Write property test: exactly one `LocationMarker` after any number of successful responses (Property 10)
    - **Property 10: Exactly one LocationMarker after any number of successful responses**
    - **Validates: Requirements 4.4**
    - Generate sequences of 1–20 successful `BridgeResponse` objects; assert marker count in scene is always 1 after each response

- [~] 10. Set up Unity scene `UnityLocationScene`
  - Create `Assets/Scenes/UnityLocationScene.unity`
  - Add a Canvas (Screen Space – Overlay) with a `NavbarButton` (uGUI `Button`) and a `TextMeshProUGUI` status label
  - Add a flat quad as the map plane in world space
  - Create a `LocationMarker` prefab: a scaled sphere with a cyan `Material` and a child `TextMeshPro` label for the floating name tag
  - Add an empty GameObject with `FindAndVibrateHandler` and `AndroidBridge` components attached; wire all `[SerializeField]` references in the Inspector
  - Connect `NavbarButton.onClick` to `FindAndVibrateHandler.FindAndVibrate()`
  - _Requirements: 1.1, 4.2, 4.3_

- [ ] 11. Checkpoint — Unity side complete
  - Ensure all C# EditMode tests pass, ask the user if questions arise.

- [~] 12. Wire Android launch from `MainActivity`
  - In `MainActivity.kt`, add a navigation entry or button that launches `UnityBridgeActivity` via `UnityBridgeActivity.newIntent(context)`
  - Confirm the explicit `Intent` correctly starts the Unity runtime
  - _Requirements: 5.5_

- [ ] 13. Final checkpoint — full integration
  - Ensure all Kotlin and C# tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for a faster MVP
- Each task references specific requirements for traceability
- Property tests require ≥100 iterations per the design's testing strategy
- The Unity `.aar` must be placed in `app/libs/` before task 1 can be completed; obtain it from the Unity Android Build Support export
- `UnityPlayer.UnitySendMessage` is only available at runtime when the Unity `.aar` is linked; Kotlin unit tests should mock this call
