# Requirements Document

## Introduction

The `unity-android-location-vibration-bridge` feature adds a Unity C# scene embedded inside the NshutiPlanner Android app that displays a 3D location marker on a map pinpointing a user's location (resolved by email), and triggers a native Android vibration by calling a Kotlin function through Unity's `AndroidJavaObject` bridge. The feature builds on the existing `LocationRepository`, `LocationViewModel`, and `NshutiFirebaseMessagingService` infrastructure already present in the app, extending it with a Unity-side navbar button, a 3D marker scene, and a bidirectional Kotlin↔Unity bridge.

---

## Glossary

- **UnityBridge**: The Android Kotlin class (`UnityBridgeActivity` or `UnityPlayerActivity` subclass) that exposes native Android methods callable from Unity C# via `AndroidJavaObject`.
- **UnityLocationScene**: The Unity C# scene containing the 3D map view and the navbar button that initiates the location + vibration flow.
- **LocationMarker**: A 3D GameObject (e.g., a pin or sphere) placed in the Unity scene at the world-space position corresponding to the resolved user's GPS coordinates.
- **AndroidJavaObject**: The Unity C# class used to instantiate and call methods on Android Java/Kotlin objects at runtime.
- **VibrationBridge**: The Kotlin method exposed on `UnityBridge` that triggers a 500 ms haptic pulse on the Android device.
- **LocationBridge**: The Kotlin method exposed on `UnityBridge` that resolves a user's GPS coordinates from Firestore by email and returns them to Unity C# as a JSON string.
- **BridgeResponse**: A JSON string with the schema `{ "success": bool, "latitude": double, "longitude": double, "displayName": string, "error": string }` returned from `LocationBridge` to Unity C#.
- **NavbarButton**: The Unity UI button (uGUI `Button` component) in `UnityLocationScene` that triggers the location fetch and vibration flow.
- **UnityPlayerActivity**: The Android `Activity` subclass provided by the Unity Android SDK that hosts the Unity runtime.
- **LocationViewModel**: The existing Kotlin ViewModel that manages location fetching and vibration dispatch state (already implemented).
- **LocationRepository**: The existing Kotlin data-layer class that reads/writes Firestore `LocationRecord` documents and dispatches FCM vibration signals (already implemented).

---

## Requirements

### Requirement 1: Unity Scene Navbar Button

**User Story:** As a user, I want a button inside the Unity scene that I can tap to trigger the location fetch and vibration flow, so that the 3D experience is self-contained and accessible.

#### Acceptance Criteria

1. THE UnityLocationScene SHALL contain a `NavbarButton` implemented as a Unity uGUI `Button` component visible in the scene's canvas overlay.
2. WHEN the user taps the `NavbarButton`, THE UnityLocationScene SHALL invoke the `FindAndVibrate` C# method on the button's handler script.
3. THE NavbarButton SHALL be disabled (non-interactable) while a location fetch or vibration dispatch is in progress, to prevent duplicate submissions.
4. WHEN the `FindAndVibrate` flow completes successfully, THE NavbarButton SHALL be re-enabled and THE UnityLocationScene SHALL display a success label for at least 2 seconds.
5. IF the `FindAndVibrate` flow returns an error, THE NavbarButton SHALL be re-enabled and THE UnityLocationScene SHALL display the error message string received from the `BridgeResponse`.

---

### Requirement 2: Unity C# AndroidJavaObject Bridge Setup

**User Story:** As a developer, I want a well-defined C# bridge class that wraps `AndroidJavaObject` calls to the Kotlin `UnityBridge`, so that Unity can invoke native Android functionality in a modular, reusable way.

#### Acceptance Criteria

1. THE UnityLocationScene SHALL include a C# class named `AndroidBridge` that encapsulates all `AndroidJavaObject` interactions with the Kotlin `UnityBridge` class.
2. THE `AndroidBridge` class SHALL expose a method `FetchLocation(string email, Action<BridgeResponse> callback)` that calls the Kotlin `LocationBridge` method asynchronously and invokes the callback on the Unity main thread.
3. THE `AndroidBridge` class SHALL expose a method `TriggerVibration()` that calls the Kotlin `VibrationBridge` method synchronously on the Android UI thread.
4. WHEN running on a non-Android platform (e.g., Unity Editor), THE `AndroidBridge` class SHALL return a stub `BridgeResponse` with `success = false` and `error = "Android platform required"` instead of throwing an exception.
5. THE `AndroidBridge` class SHALL catch all exceptions thrown by `AndroidJavaObject` calls and surface them as `BridgeResponse` objects with `success = false` and the exception message in the `error` field.

---

### Requirement 3: Kotlin UnityBridge Class

**User Story:** As a developer, I want a Kotlin class that exposes location and vibration methods callable from Unity, so that the native Android capabilities are accessible through a clean, documented interface.

#### Acceptance Criteria

1. THE UnityBridge SHALL be implemented as a Kotlin class (or object) with a companion object annotated with `@JvmStatic` so its methods are callable from Unity's `AndroidJavaObject`.
2. THE UnityBridge SHALL expose a method `fetchLocationByEmail(email: String, callbackObjectName: String, callbackMethodName: String)` that resolves the user's GPS coordinates from Firestore by email and calls back into Unity using `UnityPlayer.UnitySendMessage`.
3. WHEN `fetchLocationByEmail` resolves the location successfully, THE UnityBridge SHALL call `UnityPlayer.UnitySendMessage(callbackObjectName, callbackMethodName, json)` where `json` is a `BridgeResponse` JSON string with `success = true`, `latitude`, `longitude`, and `displayName` populated.
4. IF `fetchLocationByEmail` fails (user not found, Firestore error, no location record), THEN THE UnityBridge SHALL call `UnityPlayer.UnitySendMessage` with a `BridgeResponse` JSON string where `success = false` and `error` contains a human-readable message.
5. THE UnityBridge SHALL expose a method `triggerVibration()` annotated with `@JvmStatic` that triggers a 500 ms haptic pulse using `VibrationEffect` (API 26+) with a legacy `Vibrator.vibrate(long)` fallback for API < 26.
6. IF the `VIBRATE` Android permission is not granted at runtime, THEN THE UnityBridge `triggerVibration()` method SHALL log the error and return without crashing.

---

### Requirement 4: 3D Location Marker Display

**User Story:** As a user, I want to see a 3D location marker placed on the Unity map scene at my partner's GPS coordinates, so that I get a spatially meaningful view of their location.

#### Acceptance Criteria

1. WHEN a `BridgeResponse` with `success = true` is received by the Unity C# handler, THE UnityLocationScene SHALL instantiate or reposition a `LocationMarker` GameObject at the world-space position derived from the response's `latitude` and `longitude`.
2. THE `LocationMarker` SHALL be a 3D GameObject (e.g., a scaled sphere or custom pin mesh) with a distinct material color (e.g., cyan or red) to make it visually identifiable on the map plane.
3. THE UnityLocationScene SHALL display the `BridgeResponse.displayName` as a `TextMeshPro` label floating above the `LocationMarker` at a fixed world-space offset.
4. WHEN a new `BridgeResponse` is received while a `LocationMarker` already exists in the scene, THE UnityLocationScene SHALL move the existing marker to the new position rather than instantiating a duplicate.
5. THE coordinate mapping from GPS (latitude, longitude) to Unity world-space SHALL use a configurable `MetresPerUnit` scale factor (default: 1 Unity unit = 1 metre) applied relative to a configurable origin coordinate (default origin: `(0.0, 0.0)` lat/lng).

---

### Requirement 5: Android Permissions and Manifest Setup

**User Story:** As a developer, I want all required Android permissions and manifest entries declared correctly, so that the Unity-Android bridge works without runtime permission crashes.

#### Acceptance Criteria

1. THE `AndroidManifest.xml` SHALL declare the `android.permission.VIBRATE` permission (already present from `location-vibration-connect`; this requirement confirms it is retained).
2. THE `AndroidManifest.xml` SHALL declare `android.permission.ACCESS_FINE_LOCATION` and `android.permission.ACCESS_COARSE_LOCATION` permissions (already present; retained for the Unity bridge path).
3. THE `AndroidManifest.xml` SHALL declare the `UnityPlayerActivity` (or its subclass `UnityBridgeActivity`) as an `<activity>` entry with `android:configChanges` set to handle orientation and keyboard changes without restarting the activity.
4. WHERE the Unity module is integrated as an Android library (`.aar`), THE `build.gradle` SHALL include the Unity `.aar` as a `implementation files(...)` or `flatDir` dependency.
5. THE `UnityBridgeActivity` SHALL extend `UnityPlayerActivity` and be declared as the launch activity for the Unity scene, or be launched via an explicit `Intent` from the NshutiPlanner `MainActivity`.

---

### Requirement 6: Error Handling

**User Story:** As a developer, I want all failure paths in the Unity-Android bridge to be handled gracefully, so that the app never crashes due to missing location data or failed vibration calls.

#### Acceptance Criteria

1. IF the email provided to `fetchLocationByEmail` does not match any Firestore `users` document, THEN THE UnityBridge SHALL return a `BridgeResponse` with `success = false` and `error = "User not found for email: <email>"`.
2. IF the resolved user has no `LocationRecord` in Firestore, THEN THE UnityBridge SHALL return a `BridgeResponse` with `success = false` and `error = "Location not available for user"`.
3. IF the Firestore query in `fetchLocationByEmail` throws an exception, THEN THE UnityBridge SHALL catch the exception, log it, and return a `BridgeResponse` with `success = false` and `error` set to the exception message.
4. IF `triggerVibration()` is called and the device vibrator service is unavailable, THEN THE UnityBridge SHALL log the condition and return without throwing an exception.
5. IF the `AndroidJavaObject` call from Unity C# throws a `AndroidJavaException`, THEN THE `AndroidBridge` C# class SHALL catch it and invoke the callback with a `BridgeResponse` where `success = false` and `error` contains the Java exception message.
6. THE `AndroidBridge` C# class SHALL log all bridge errors using `UnityEngine.Debug.LogError` with a consistent prefix `[AndroidBridge]` for easy filtering in the Unity console.

---

### Requirement 7: Modular and Extensible Code Structure

**User Story:** As a developer, I want the bridge code to be modular and well-commented, so that new bridge methods can be added without modifying existing logic.

#### Acceptance Criteria

1. THE `AndroidBridge` C# class SHALL be the single point of contact between Unity C# and the Kotlin `UnityBridge`; no other C# script SHALL instantiate `AndroidJavaObject` directly for bridge calls.
2. THE Kotlin `UnityBridge` class SHALL delegate location resolution to the existing `LocationRepository` and vibration to the existing `NshutiFirebaseMessagingService.triggerHapticPulse()` method, rather than duplicating logic.
3. THE `BridgeResponse` schema SHALL be defined as a C# struct or class in Unity and as a Kotlin data class in the Android module, with matching field names to ensure JSON round-trip correctness.
4. FOR ALL valid `BridgeResponse` objects serialized to JSON in Kotlin and deserialized in C# (or vice versa), THE field values SHALL be preserved exactly (round-trip property).
5. WHEN a new bridge method is needed, a developer SHALL be able to add it by: (a) adding a `@JvmStatic` method to `UnityBridge`, and (b) adding a corresponding wrapper method to `AndroidBridge` C# — without modifying any other class.

---

### Requirement 8: Unity Editor and Non-Android Platform Stubs

**User Story:** As a developer, I want the Unity C# bridge code to compile and run in the Unity Editor without an Android device, so that the scene can be developed and tested without a physical device.

#### Acceptance Criteria

1. THE `AndroidBridge` C# class SHALL use `#if UNITY_ANDROID && !UNITY_EDITOR` preprocessor guards to isolate all `AndroidJavaObject` instantiation.
2. WHEN running in the Unity Editor, THE `AndroidBridge.FetchLocation` method SHALL invoke the callback after a simulated 1-second delay with a configurable stub `BridgeResponse` (default: `success = true`, `latitude = -1.9441`, `longitude = 30.0619`, `displayName = "Test Partner"`).
3. WHEN running in the Unity Editor, THE `AndroidBridge.TriggerVibration` method SHALL log `"[AndroidBridge] Vibration stub called (Editor)"` to the Unity console instead of calling native code.
4. THE stub coordinates in Requirement 8.2 SHALL be configurable via Unity `[SerializeField]` fields on the `AndroidBridge` component so developers can test different locations without recompiling.

