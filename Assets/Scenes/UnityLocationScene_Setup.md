# UnityLocationScene — Setup Guide

> **Requirements covered:** 1.1, 4.2, 4.3  
> This guide walks you through building `UnityLocationScene` entirely inside the Unity Editor.  
> All C# scripts (`AndroidBridge`, `FindAndVibrateHandler`, `BridgeResponse`) are already in `Assets/Scripts/`.

---

## Prerequisites

- Unity 2021.3 LTS or newer (URP or Built-in RP both work)
- TextMeshPro package installed (`Window → Package Manager → TextMeshPro`)
- The Android Build Support module installed for your Unity version

---

## Step 1 — Create the Scene File

1. In the **Project** window, navigate to `Assets/Scenes/` (create the folder if it doesn't exist).
2. Right-click → **Create → Scene**.
3. Rename the new scene to **`UnityLocationScene`**.
4. Double-click it to open it.

---

## Step 2 — Add the Canvas (UI Overlay)

### 2a. Create the Canvas

1. In the **Hierarchy**, right-click → **UI → Canvas**.
2. With the Canvas selected, in the **Inspector**:
   - **Render Mode** → `Screen Space – Overlay`
   - Add a **Canvas Scaler** component (it is added automatically):
     - **UI Scale Mode** → `Scale With Screen Size`
     - **Reference Resolution** → `1080 × 1920`

### 2b. Add the NavbarButton

1. Right-click the Canvas in the Hierarchy → **UI → Button - TextMeshPro**.
2. Rename the Button GameObject to **`NavbarButton`**.
3. In the **Rect Transform**:
   - **Anchor** → top-center
   - **Pos Y** → `-80` (so it sits near the top of the screen)
   - **Width** → `400`, **Height** → `100`
4. Expand `NavbarButton` → select the child **Text (TMP)** object.
   - Set the text to **`Find & Vibrate`**.
   - Font size: `36`, alignment: `Center Middle`.

### 2c. Add the Status Label

1. Right-click the Canvas → **UI → Text - TextMeshPro**.
2. Rename it to **`StatusLabel`**.
3. In the **Rect Transform**:
   - **Anchor** → top-center
   - **Pos Y** → `-200`
   - **Width** → `800`, **Height** → `80`
4. Set the default text to an empty string `""`.
5. Font size: `28`, alignment: `Center Middle`, color: white.

---

## Step 3 — Add the Map Plane

1. In the Hierarchy, right-click → **3D Object → Quad**.
2. Rename it to **`MapPlane`**.
3. In the **Transform**:
   - **Position** → `(0, 0, 0)`
   - **Rotation** → `(90, 0, 0)` (so the quad lies flat, facing up)
   - **Scale** → `(100, 100, 1)`
4. Create a simple material for the map plane:
   - In the Project window, right-click → **Create → Material**, name it `MapPlaneMaterial`.
   - Set its **Albedo** color to a dark grey or any map-like color.
   - Drag the material onto `MapPlane` in the scene.

---

## Step 4 — Create the LocationMarker Prefab

### 4a. Build the Prefab in the Scene

1. In the Hierarchy, right-click → **3D Object → Sphere**.
2. Rename it to **`LocationMarker`**.
3. In the **Transform**:
   - **Scale** → `(0.5, 0.5, 0.5)`
4. Create a cyan material:
   - Project window → right-click → **Create → Material**, name it `LocationMarkerMaterial`.
   - Set **Albedo** color to **Cyan** (`R:0, G:255, B:255`).
   - Drag the material onto the `LocationMarker` sphere.

### 4b. Add the Floating Name-Tag Label

1. Right-click `LocationMarker` in the Hierarchy → **3D Object → Text - TextMeshPro** (this creates a world-space TMP object as a child).
2. Rename the child to **`NameLabel`**.
3. In the **Transform**:
   - **Position** → `(0, 1, 0)` (floats 1 unit above the sphere)
   - **Scale** → `(0.1, 0.1, 0.1)` (world-space TMP text is large by default; scale it down)
4. In the **TextMeshPro** component:
   - Default text: `""` (will be set at runtime)
   - Font size: `36`
   - Alignment: `Center Middle`
   - Enable **Face → Color** → white so it's visible against the map.

### 4c. Save as a Prefab

1. In the Project window, navigate to `Assets/Prefabs/` (create the folder if it doesn't exist).
2. Drag `LocationMarker` from the Hierarchy into `Assets/Prefabs/`.
3. Unity will ask **"Original Prefab or Prefab Variant"** — choose **Original Prefab**.
4. The prefab is now saved at **`Assets/Prefabs/LocationMarker.prefab`**.
5. Delete the `LocationMarker` instance from the Hierarchy (it will be instantiated at runtime).

---

## Step 5 — Add the BridgeController GameObject

1. In the Hierarchy, right-click → **Create Empty**.
2. Rename it to **`BridgeController`**.
3. In the **Inspector**, click **Add Component**:
   - Search for and add **`FindAndVibrateHandler`**.
   - Search for and add **`AndroidBridge`**.

### 5a. Wire SerializeField References on FindAndVibrateHandler

With `BridgeController` selected, find the **FindAndVibrateHandler** component and fill in each field:

| Field | Value |
|---|---|
| **Bridge** | Drag `BridgeController` itself (it has the `AndroidBridge` component) |
| **Navbar Button** | Drag `NavbarButton` from the Hierarchy |
| **Status Label** | Drag `StatusLabel` from the Hierarchy |
| **Location Marker Prefab** | Drag `Assets/Prefabs/LocationMarker.prefab` from the Project window |
| **Map Plane** | Drag `MapPlane` from the Hierarchy |
| **Target Email** | Type the partner's email address (e.g., `partner@example.com`) |
| **Origin Latitude** | `0` (or your map's reference latitude) |
| **Origin Longitude** | `0` (or your map's reference longitude) |
| **Metres Per Unit** | `1` |

### 5b. Configure AndroidBridge Stub Values (Editor Testing)

With `BridgeController` selected, find the **AndroidBridge** component:

| Field | Value |
|---|---|
| **Stub Success** | ✅ checked |
| **Stub Latitude** | `-1.9441` (Kigali, Rwanda — default test location) |
| **Stub Longitude** | `30.0619` |
| **Stub Display Name** | `Test Partner` |

These values are used only in the Unity Editor; on Android the real Kotlin bridge is called.

---

## Step 6 — Connect NavbarButton.onClick

1. Select `NavbarButton` in the Hierarchy.
2. In the **Inspector**, scroll to the **Button** component → **On Click ()** section.
3. Click the **`+`** button to add a new listener.
4. Drag **`BridgeController`** into the object slot.
5. In the function dropdown, select **`FindAndVibrateHandler → FindAndVibrate ()`**.

The button will now call `FindAndVibrateHandler.FindAndVibrate()` when tapped.

---

## Step 7 — Build Settings

### 7a. Add the Scene to Build Settings

1. Open **File → Build Settings**.
2. Click **Add Open Scenes** — `Assets/Scenes/UnityLocationScene` should appear in the list.
3. Make sure it is checked (enabled).

### 7b. Set Android as the Target Platform

1. In **Build Settings**, select **Android** in the platform list.
2. Click **Switch Platform** (Unity will reimport assets for Android).
3. Click **Player Settings** and verify:
   - **Company Name** / **Product Name** match your project.
   - **Minimum API Level** → `API 21` (Android 5.0) or higher.
   - **Target API Level** → `API 33` or **Automatic (highest installed)**.
   - **Scripting Backend** → `IL2CPP` (recommended for release) or `Mono` (faster iteration).
   - **Target Architectures** → check `ARM64` (required for modern Android devices).

---

## Step 8 — Verify the Scene in the Editor

1. Press **Play** in the Unity Editor.
2. Click the **Find & Vibrate** button in the Game view.
3. Expected behaviour (Editor stubs active):
   - The button becomes non-interactable immediately.
   - After ~1 second, a cyan sphere appears on the map plane at the world-space position derived from `(-1.9441, 30.0619)` with the configured origin/scale.
   - The floating label above the sphere reads `Test Partner`.
   - The status label reads `✓ Vibration sent to Test Partner!`.
   - After 2 seconds the button becomes interactable again.
   - The Unity Console shows `[AndroidBridge] Vibration stub called (Editor)`.

---

## Folder Structure Summary

```
Assets/
├── Prefabs/
│   └── LocationMarker.prefab       ← cyan sphere + floating TMP label
├── Scenes/
│   ├── UnityLocationScene.unity    ← the scene created in Step 1
│   └── UnityLocationScene_Setup.md ← this guide
└── Scripts/
    ├── AndroidBridge.cs
    ├── BridgeResponse.cs
    └── FindAndVibrateHandler.cs
```

---

## Troubleshooting

| Symptom | Fix |
|---|---|
| `TextMeshPro` components missing | Install TMP via `Window → Package Manager → TextMeshPro` |
| Button click does nothing | Verify `On Click ()` is wired to `FindAndVibrateHandler.FindAndVibrate()` on `BridgeController` |
| Marker appears at wrong position | Check **Origin Latitude/Longitude** and **Metres Per Unit** on `FindAndVibrateHandler` |
| `NullReferenceException` on Play | Ensure all `[SerializeField]` fields on `FindAndVibrateHandler` are assigned in the Inspector |
| Android build fails | Confirm Android Build Support module is installed and **Switch Platform** was clicked |
