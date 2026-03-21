# Requirements Document

## Introduction

The `location-vibration-connect` feature adds a navbar button to NshutiPlanner that lets the current user (Sender) fetch their connected partner's (Receiver) real-time location and display it on a 3D/AR map, while simultaneously sending a haptic vibration signal to the Receiver's device. The feature uses Firebase Firestore for location data exchange and Firebase Cloud Messaging (FCM) for push-triggered vibration delivery. Both users must be linked via the existing `coupleId` / `partnerId` relationship.

---

## Glossary

- **Sender**: The currently authenticated user who initiates the location fetch and vibration signal.
- **Receiver**: The partner user (identified by email) whose location is fetched and who receives the vibration signal.
- **LocationRecord**: A Firestore document storing a user's last known latitude, longitude, accuracy, and timestamp.
- **VibrationSignal**: An FCM data message sent to the Receiver's device that triggers a haptic vibration pattern.
- **LocationScreen**: The Jetpack Compose screen that displays the Receiver's location on a 3D/AR map.
- **LocationViewModel**: The ViewModel that manages location fetching, sharing, and vibration signal state.
- **LocationRepository**: The data-layer class responsible for reading/writing LocationRecords and dispatching VibrationSignals via FCM.
- **MapRenderer**: The component (Sceneform AR or Google Maps SDK 3D) responsible for rendering the Receiver's position on-screen.
- **FCM_Token**: The Firebase Cloud Messaging registration token stored per user in Firestore, used to address push messages.

---

## Requirements

### Requirement 1: Navbar Entry Point

**User Story:** As a Sender, I want a dedicated navbar button for the location-vibration feature, so that I can access it quickly from anywhere in the app.

#### Acceptance Criteria

1. THE Navigation SHALL include a `LocationVibrate` route in the bottom navigation bar alongside the existing routes (Dashboard, Planner, Tasks, Profile).
2. WHEN the Sender taps the navbar button, THE Navigation SHALL navigate to the LocationScreen.
3. THE Navigation SHALL display the navbar button with the `LocationOn` icon and the label "Find & Vibrate".

---

### Requirement 2: Location Permission Handling

**User Story:** As a Sender, I want the app to request location permissions before accessing my location, so that the app respects Android permission guidelines.

#### Acceptance Criteria

1. WHEN the Sender opens the LocationScreen for the first time, THE LocationScreen SHALL request `ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION` permissions.
2. IF the Sender denies location permission, THEN THE LocationScreen SHALL display an explanatory message and a button to open app settings.
3. WHILE location permission is granted, THE LocationRepository SHALL be able to read the Sender's current GPS coordinates.
4. IF the Sender's device has no GPS fix within 10 seconds, THEN THE LocationRepository SHALL return the last known location if available, or surface an error state.

---

### Requirement 3: Sender Location Publishing

**User Story:** As a Sender, I want my current location to be stored in Firebase so my partner can retrieve it, so that the feature works without a direct peer-to-peer connection.

#### Acceptance Criteria

1. WHEN the Sender taps "Find & Vibrate", THE LocationRepository SHALL write a LocationRecord containing the Sender's latitude, longitude, accuracy (in metres), and a server timestamp to the Firestore path `locations/{senderUid}`.
2. THE LocationRecord SHALL include the fields: `uid`, `latitude`, `longitude`, `accuracyMetres`, `updatedAt` (Firestore Timestamp).
3. IF writing the LocationRecord fails, THEN THE LocationViewModel SHALL expose an error state with a human-readable message.
4. THE LocationRepository SHALL overwrite (not append) the existing LocationRecord on each publish, so only the latest position is stored.

---

### Requirement 4: Receiver Location Fetching

**User Story:** As a Sender, I want to fetch my partner's last known location from Firebase by their email, so that I can see where they are on the map.

#### Acceptance Criteria

1. WHEN the Sender taps "Find & Vibrate", THE LocationRepository SHALL look up the Receiver's `uid` by querying the `users` collection where `email` equals the provided partner email.
2. WHEN the Receiver's `uid` is resolved, THE LocationRepository SHALL read the LocationRecord at `locations/{receiverUid}`.
3. IF no LocationRecord exists for the Receiver, THEN THE LocationViewModel SHALL expose a `ReceiverLocationUnavailable` error state.
4. IF the Receiver's LocationRecord `updatedAt` timestamp is older than 30 minutes, THEN THE LocationScreen SHALL display a staleness warning alongside the map.
5. WHEN the LocationRecord is successfully fetched, THE LocationViewModel SHALL expose the Receiver's latitude and longitude as UI state.

---

### Requirement 5: 3D / AR Map Display

**User Story:** As a Sender, I want to see my partner's location rendered on a 3D or AR map, so that the experience feels immersive and spatially meaningful.

#### Acceptance Criteria

1. WHEN the Receiver's LocationRecord is available, THE MapRenderer SHALL render the Receiver's position as a pin or AR anchor on the LocationScreen.
2. THE MapRenderer SHALL support two rendering modes: `AR` (Sceneform-based, requires ARCore) and `MAP_3D` (Google Maps SDK with tilt/bearing enabled).
3. WHERE the device supports ARCore, THE LocationScreen SHALL default to `AR` rendering mode.
4. WHERE the device does not support ARCore, THE LocationScreen SHALL fall back to `MAP_3D` rendering mode automatically.
5. THE MapRenderer SHALL display the Receiver's `displayName` and profile photo as a label above the location pin/anchor.
6. IF the MapRenderer fails to initialise, THEN THE LocationScreen SHALL display a plain 2D fallback map using Google Maps SDK.

---

### Requirement 6: Vibration Signal Dispatch

**User Story:** As a Sender, I want to send a vibration signal to my partner's device, so that they know I am thinking of them without requiring them to open the app.

#### Acceptance Criteria

1. WHEN the Sender taps "Find & Vibrate", THE LocationRepository SHALL retrieve the Receiver's `FCM_Token` from the Firestore `users/{receiverUid}` document.
2. WHEN the FCM_Token is retrieved, THE LocationRepository SHALL send a VibrationSignal as an FCM data message with the payload `{ "type": "vibration", "senderUid": "<senderUid>", "senderName": "<displayName>" }` to the Receiver's device.
3. IF the Receiver's `FCM_Token` is absent or empty, THEN THE LocationViewModel SHALL expose a `VibrationUnavailable` error state and SHALL NOT attempt to send the FCM message.
4. WHEN the VibrationSignal FCM message is delivered, THE NshutiFirebaseMessagingService on the Receiver's device SHALL trigger the device vibrator with a 500 ms pulse.
5. WHILE the Receiver's device is in Do Not Disturb mode, THE NshutiFirebaseMessagingService SHALL still execute the vibration because FCM data messages bypass notification channels.
6. IF the FCM dispatch call fails, THEN THE LocationViewModel SHALL expose an error state with a human-readable message and SHALL NOT retry automatically.

---

### Requirement 7: FCM Token Management

**User Story:** As a user, I want my FCM token to be kept up to date in Firebase so that vibration signals can always reach my device.

#### Acceptance Criteria

1. WHEN the app starts and a user is authenticated, THE NshutiFirebaseMessagingService SHALL write the current FCM_Token to `users/{uid}` under the field `fcmToken`.
2. WHEN the FCM_Token is refreshed by the Firebase SDK, THE NshutiFirebaseMessagingService SHALL update `users/{uid}.fcmToken` in Firestore within 5 seconds of receiving the new token.
3. IF writing the FCM_Token to Firestore fails, THEN THE NshutiFirebaseMessagingService SHALL log the error and retry on the next app launch.

---

### Requirement 8: Actor Model Enforcement

**User Story:** As a developer, I want the feature to enforce the Sender/Receiver actor model, so that location and vibration actions are always correctly directed.

#### Acceptance Criteria

1. THE LocationViewModel SHALL represent the current user as an `Actor` with `role = Role.SENDER` and the partner as an `Actor` with `role = Role.RECEIVER`.
2. THE LocationRepository SHALL only write LocationRecords for `Role.SENDER` actors.
3. THE LocationRepository SHALL only read LocationRecords for `Role.RECEIVER` actors.
4. THE LocationRepository SHALL only dispatch VibrationSignals to `Role.RECEIVER` actors.
5. IF the Sender and Receiver resolve to the same `uid`, THEN THE LocationViewModel SHALL expose a `SelfTargetError` state and SHALL NOT proceed with location fetch or vibration dispatch.

---

### Requirement 9: UI Loading and Error States

**User Story:** As a Sender, I want clear feedback during loading and on errors, so that I always know what the app is doing.

#### Acceptance Criteria

1. WHILE the LocationViewModel is fetching the Receiver's location or dispatching a VibrationSignal, THE LocationScreen SHALL display a loading indicator.
2. WHEN both the location fetch and vibration dispatch complete successfully, THE LocationScreen SHALL display a success confirmation message for at least 2 seconds.
3. IF any operation fails, THE LocationScreen SHALL display the error message from the LocationViewModel state and a "Retry" button.
4. THE LocationScreen SHALL disable the "Find & Vibrate" button while a request is in progress to prevent duplicate submissions.
