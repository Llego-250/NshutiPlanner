# Implementation Plan: location-vibration-connect

## Overview

Implement the "Find & Vibrate" feature incrementally: data models → repository → ViewModel → UI (screen + map renderer) → FCM token management → navigation wiring. Each step builds on the previous and ends with everything integrated.

## Tasks

- [x] 1. Add data models and update AndroidManifest permissions
  - Add `LocationRecord` data class to `Models.kt` with fields: `uid`, `latitude`, `longitude`, `accuracyMetres`, `updatedAt`
  - Add `Role` enum and `Actor` data class to `Models.kt`
  - Add `ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION` permissions to `AndroidManifest.xml`
  - Add `VIBRATE` permission to `AndroidManifest.xml`
  - _Requirements: 2.1, 3.2, 8.1_

- [-] 2. Implement `LocationRepository`
  - [x] 2.1 Create `data/repository/LocationRepository.kt` with the four key methods: `resolveActors`, `publishSenderLocation`, `fetchReceiverLocation`, `dispatchVibrationSignal`
    - `publishSenderLocation` must guard `role == Role.SENDER` and throw `IllegalArgumentException` otherwise
    - `fetchReceiverLocation` must guard `role == Role.RECEIVER` and throw `IllegalArgumentException` otherwise
    - `dispatchVibrationSignal` must guard receiver `role == Role.RECEIVER` and throw `IllegalArgumentException` otherwise
    - `dispatchVibrationSignal` must guard against empty/absent `fcmToken` and return without calling FCM
    - FCM dispatch uses FCM HTTP v1 API via OkHttp with the Firebase Auth ID token
    - _Requirements: 3.1, 3.4, 4.1, 4.2, 6.1, 6.2, 6.3, 8.2, 8.3, 8.4_

  - [ ]* 2.2 Write property test for role-based write guard (Property 6)
    - **Property 6: Role-based write guard**
    - **Validates: Requirements 8.2**
    - Use Kotest `forAll` with generated `RECEIVER` actors; assert `IllegalArgumentException` is thrown and no Firestore write occurs

  - [ ] 2.3 Write property test for role-based read guard (Property 7)
    - **Property 7: Role-based read guard**
    - **Validates: Requirements 8.3**
    - Use Kotest `forAll` with generated `SENDER` actors; assert `IllegalArgumentException` is thrown and no Firestore read occurs

  - [ ]* 2.4 Write property test for role-based dispatch guard (Property 8)
    - **Property 8: Role-based dispatch guard**
    - **Validates: Requirements 8.4**
    - Use Kotest `forAll` with `SENDER` actors as receiver arg; assert `IllegalArgumentException` is thrown and no FCM call occurs

  - [ ]* 2.5 Write property test for VibrationSignal payload correctness (Property 5)
    - **Property 5: VibrationSignal payload correctness**
    - **Validates: Requirements 6.2**
    - Use Kotest `forAll` with generated sender/receiver Actor pairs; assert dispatched FCM payload contains `type="vibration"`, `senderUid`, `senderName`

- [x] 3. Checkpoint — Ensure all repository tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 4. Implement `LocationViewModel`
  - [x] 4.1 Create `viewmodel/LocationViewModel.kt` with `LocationUiState` sealed class, `ErrorKind` enum, and `findAndVibrate()` action
    - `findAndVibrate()` transitions: `Idle → Loading → Success | Error`
    - Compute `isStale` when `(now - updatedAt) > 30 * 60 * 1000 ms`
    - Enforce self-target guard: if `sender.uid == receiver.uid` emit `Error(kind = SELF_TARGET)`
    - Expose `uiState: StateFlow<LocationUiState>`
    - _Requirements: 3.3, 4.3, 4.4, 4.5, 6.3, 6.6, 8.1, 8.5, 9.1, 9.2, 9.3, 9.4_

  - [ ]* 4.2 Write property test for staleness detection (Property 4)
    - **Property 4: Staleness detection**
    - **Validates: Requirements 4.4**
    - Use Kotest `forAll` with generated timestamps; assert `isStale = true` when `> 30 min` ago, `false` otherwise

  - [ ]* 4.3 Write unit tests for `LocationViewModel` state transitions
    - Test: `Idle → Loading → Success` happy path
    - Test: each `ErrorKind` transition (PERMISSION_DENIED, RECEIVER_LOCATION_UNAVAILABLE, VIBRATION_UNAVAILABLE, SELF_TARGET, WRITE_FAILED, FCM_FAILED)
    - _Requirements: 9.1, 9.2, 9.3_

- [x] 5. Implement `MapRenderer`
  - [x] 5.1 Create `ui/screens/location/MapRenderer.kt` with `RenderMode` enum and `selectRenderMode(isArCoreSupported, isMapInitOk)` pure function
    - `isArCoreSupported = true` → `AR`
    - `isArCoreSupported = false` → `MAP_3D`
    - `isMapInitOk = false` → `MAP_2D_FALLBACK`
    - Implement `MapRenderer` composable that renders Google Maps with tilt/bearing for `MAP_3D`, and a plain 2D map for `MAP_2D_FALLBACK`; AR mode renders an ARCore placeholder (full AR integration is a follow-up)
    - Display Receiver's `displayName` as a label above the map marker
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6_

  - [ ]* 5.2 Write unit tests for `selectRenderMode`
    - Test all three outcomes with example inputs
    - _Requirements: 5.2, 5.3, 5.4_

- [x] 6. Implement `LocationScreen`
  - [x] 6.1 Create `ui/screens/location/LocationScreen.kt`
    - Request `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` via `rememberPermissionState` on first open
    - Show explanatory message + "Open Settings" button when permission is denied
    - Observe `LocationViewModel.uiState` and render: loading spinner, `MapRenderer` on `Success`, staleness warning banner when `isStale = true`, error card with Retry button on `Error`
    - "Find & Vibrate" button disabled while `uiState is Loading`
    - Show success confirmation message for 2 seconds after `Success`
    - _Requirements: 2.1, 2.2, 4.4, 9.1, 9.2, 9.3, 9.4_

  - [ ]* 6.2 Write property test for loading state disables action (Property 9)
    - **Property 9: Loading state disables action**
    - **Validates: Requirements 9.1, 9.4**
    - For any `LocationUiState.Loading` state, assert button `enabled = false` and loading indicator is visible

- [x] 7. Extend `NshutiFirebaseMessagingService` for vibration and token management
  - [x] 7.1 Update `onMessageReceived` to handle `data["type"] == "vibration"`: call `triggerHapticPulse()` which vibrates 500 ms using `VibrationEffect` (API 26+) with legacy fallback
    - _Requirements: 6.4, 6.5_

  - [x] 7.2 Implement `onNewToken` to write the FCM token to `users/{uid}.fcmToken` in Firestore within 5 seconds; log and skip retry on failure (retry on next launch)
    - Also write the current token on app start when a user is authenticated (call from `AppViewModel.init` or `loadUser`)
    - _Requirements: 7.1, 7.2, 7.3_

  - [ ]* 7.3 Write unit tests for `NshutiFirebaseMessagingService`
    - Test: `onMessageReceived` with `type = "vibration"` calls vibrator
    - Test: notification message does NOT call vibrator
    - Test: `onNewToken` writes to correct Firestore path
    - _Requirements: 6.4, 7.1, 7.2_

- [x] 8. Wire navigation and integrate into `MainActivity`
  - [x] 8.1 Add `LocationVibrate` route to `Navigation.kt` and add `BottomNavItem` with `Icons.Filled.LocationOn` and label "Find & Vibrate" to `bottomNavItems`
    - _Requirements: 1.1, 1.3_

  - [x] 8.2 Add `LocationViewModel` to `VmFactory` and add the `composable(Route.LocationVibrate.route)` block in `MainActivity.kt`, passing the `LocationViewModel` to `LocationScreen`
    - _Requirements: 1.2_

- [x] 9. Add string resources for all user-facing error and status messages
  - Add entries to `strings.xml` for: permission denied explanation, GPS timeout error, Firestore write error, receiver location unavailable, vibration unavailable, FCM failed, self-target error, staleness warning, success confirmation
  - _Requirements: 9.3_

- [x] 10. Final checkpoint — Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for a faster MVP
- Each task references specific requirements for traceability
- Property tests use Kotest `forAll` with a minimum of 100 iterations
- FCM dispatch is client-side via HTTP v1 API using the Firebase Auth ID token (no Cloud Function needed for MVP)
- Full ARCore/Sceneform integration for `AR` render mode is deferred; `MAP_3D` and `MAP_2D_FALLBACK` are the primary targets
