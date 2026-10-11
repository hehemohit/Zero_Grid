# SOS Real GPS Location Broadcast & Compass Tracking Implementation Plan

## Overview:

When an SOS is triggered, the sender's real-time GPS location (latitude, longitude, accuracy) must be obtained and transmitted both:

1. **Offline via BLE/Wi-Fi Direct Mesh (`MeshPacket`)** so nearby peers can receive it off-grid.
2. **Online via REST API (`SosDispatchRequest`)** so the central backend, FCM push notifications, and future WebAdmin tracking panel receive the exact coordinates.

Additionally, when a family member, emergency contact, or peer receives this emergency alert, they can tap **"TRACK LOCATION"** to open a new **Track SOS Screen (`TrackSosScreen`)**. This screen computes the live distance and bearing to the victim, using the responder's phone sensors (magnetometer/accelerometer rotation vector) to render an active **directional compass pointer / radar needle** pointing directly toward the victim's location, alongside an offline radar grid and an option to launch Google Maps / turn-by-turn navigation.

---

## User Review Required

> [!IMPORTANT]
> **Zero-Dependency Offline GPS**:
> Coordinates are acquired using Android's native `LocationManager` (`GPS_PROVIDER` with fallback to `NETWORK_PROVIDER` / last known location). This ensures GPS coordinates can be acquired completely offline without requiring Google Play Services or active cell towers.
>
> **Sensor Fusion for Compass**:
> Direction pointing uses Android's `SensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)` (with graceful fallback to `TYPE_ACCELEROMETER` + `TYPE_MAGNETIC_FIELD`). This accurately aligns the compass arrow with the device's physical azimuth so the arrow turns smoothly in real time as the user turns their phone.

---

## Proposed Changes

### Component 1: Native Offline Location Helper

#### [NEW] [`LocationHelper.kt`](file:///c:/Users/ACER/AndroidStudioProjects/gridzero/app/src/main/java/com/example/zerogrid/location/LocationHelper.kt)

- Provides a lightweight, singleton helper using Android's `LocationManager`.
- Checks for `ACCESS_FINE_LOCATION` or `ACCESS_COARSE_LOCATION` permissions.
- First attempts to retrieve the freshest `lastKnownLocation` from `GPS_PROVIDER` or `NETWORK_PROVIDER`.
- If unavailable or stale, requests a one-time single location update with a short timeout (e.g. 3–5 seconds) so SOS dispatch is never blocked indefinitely.
- Returns `Pair<Double, Double>?` (latitude, longitude) and `Float?` (accuracy in meters).

---

### Component 2: SOS Serialization & Mesh Engine Integration

#### [MODIFY] [`MeshPacket.kt`](file:///c:/Users/ACER/AndroidStudioProjects/gridzero/app/src/main/java/com/example/zerogrid/mesh/engine/MeshPacket.kt)

- Introduce a structured payload helper or parser for `PacketType.SOS_BEACON`:
  - Structured JSON format: `{"category":"MEDICAL","message":"...","lat":12.9716,"lng":77.5946,"accuracy":15.0,"timestamp":...}`
  - Backwards-compatible parser: If the payload is the legacy string format (`"Category: ... | Lat: ... | Lon: ..."`), parse using regex.
- Provide helper methods: `getSosCoordinates(): Pair<Double, Double>?` and `getSosAccuracy(): Float?`.

#### [MODIFY] [`MeshEngine.kt`](file:///c:/Users/ACER/AndroidStudioProjects/gridzero/app/src/main/java/com/example/zerogrid/mesh/engine/MeshEngine.kt)

- Update `triggerSosBeacon(category, message, lat, lon, accuracy, preferredTransport)` to serialize coordinates properly into the beacon payload.

---

### Component 3: SOS Trigger Flow in UI

#### [MODIFY] [`SendSosScreen.kt`](file:///c:/Users/ACER/AndroidStudioProjects/gridzero/app/src/main/java/com/example/zerogrid/emergency/SendSosScreen.kt)

- Replace hardcoded `0.0` coordinates on line 179 with active GPS acquisition via `LocationHelper`.
- When `locationSharingEnabled` is true:
  - Query `LocationHelper.getCurrentLocation(context)` before calling `sosDispatcher.triggerSos(...)`.
  - Pass acquired `lat`, `lng`, and `accuracy` to `sosDispatcher.triggerSos(lat, lng, accuracy, category, message)`.
- Display live coordinates and accuracy status directly in the `Location Sharing` card in `SendSosScreen` so the user has immediate visual confirmation of their GPS fix.

#### [MODIFY] [`UnifiedSosDispatcher.kt`](file:///c:/Users/ACER/AndroidStudioProjects/gridzero/app/src/main/java/com/example/zerogrid/emergency/UnifiedSosDispatcher.kt)

- Ensure acquired `lat`, `lng`, and `accuracy` are passed directly to both `meshEngine.triggerSosBeacon` and `SosDispatchRequest(lat, lng, accuracy, ...)`.
- If offline, `OfflineQueueManager` will persist the coordinates so when connectivity is restored, the backend & WebAdmin receive the accurate initial coordinates.

---

### Component 4: Track SOS Screen (Compass Pointer & Distance)

#### [NEW] [`TrackSosScreen.kt`](file:///c:/Users/ACER/AndroidStudioProjects/gridzero/app/src/main/java/com/example/zerogrid/emergency/TrackSosScreen.kt)

- **Parameters**: `targetLat: Double`, `targetLng: Double`, `targetName: String`, `category: String`, `timestamp: Long`, `onBack: () -> Unit`.
- **Live Responder GPS**: Continuously listens to responder's `LocationManager` updates to compute:
  - Distance (in meters or kilometers) using `Location.distanceBetween()`.
  - Bearing from responder to victim (`location.bearingTo(targetLocation)`).
- **Sensor Fusion (Azimuth / Heading)**:
  - Listens to `Sensor.TYPE_ROTATION_VECTOR` (or accelerometer/magnetometer fallback) via `SensorEventListener`.
  - Computes device azimuth (0° = North, 90° = East, etc.).
- **Dynamic Direction Pointer (Radar / Compass Needle)**:
  - Pointer Angle = `(targetBearing - deviceAzimuth + 360) % 360`.
  - Animated smoothly with Jetpack Compose `animateFloatAsState`.
  - Pointer arrow rotates in real-time as the responder moves or points their phone.
  - If the responder is facing directly towards the victim (within ±10°), display a visual "ON TARGET" locked-on glowing green indicator!
- **Tactical Grid / Radar HUD**:
  - Displays distance ring (e.g. 50m, 250m, 1km, 5km), category badge, coordinates readout, and estimated walking/travel time.
- **External Maps Intent**:
  - Includes a button: `"OPEN IN MAPS"` using `geo:targetLat,targetLng?q=targetLat,targetLng(Emergency SOS)` or `google.navigation:q=targetLat,targetLng` for Google Maps / OsmAnd turn-by-turn guidance.

---

### Component 5: Navigation & SOS Center Integration

#### [MODIFY] [`Routes.kt`](file:///c:/Users/ACER/AndroidStudioProjects/gridzero/app/src/main/java/com/example/zerogrid/navigation/Routes.kt)

- Add `Screen.TRACK_SOS`.

#### [MODIFY] [`NavGraph.kt`](file:///c:/Users/ACER/AndroidStudioProjects/gridzero/app/src/main/java/com/example/zerogrid/navigation/NavGraph.kt)

- Add state to hold `trackSosTarget: TrackSosTargetParams?`.
- Add callback `onTrackSos: (Double, Double, String, String, Long) -> Unit`.
- Render `TrackSosScreen` on `Screen.TRACK_SOS`.

#### [MODIFY] [`SosCenterScreen.kt`](file:///c:/Users/ACER/AndroidStudioProjects/gridzero/app/src/main/java/com/example/zerogrid/emergency/SosCenterScreen.kt)

- In `ActiveAlertsSection` card:
  - Parse the coordinates from the SOS packet payload.
  - If coordinates are valid (lat != 0.0 or lng != 0.0), render a prominent **"TRACK LOCATION"** button with a navigation/radar icon.
  - Tapping it navigates to `Screen.TRACK_SOS` with the victim's coordinates and metadata.

---

## Verification Plan

### Automated Build & Unit Tests

1. Run `./gradlew compileDebugKotlin` to ensure all types, sensor event listeners, Compose UI modifiers, and route params compile with 0 errors.

### Manual Verification

1. **Trigger SOS**:
   - Open `Send SOS` screen with location enabled.
   - Verify device GPS coordinates are shown on screen and passed into `sosDispatcher.triggerSos`.
2. **Mesh Alert Card**:
   - Open `SOS Center`. Inspect the incoming or broadcasted SOS card.
   - Verify the card displays "Lat: X.XXXX, Lng: Y.YYYY" and the **"TRACK LOCATION"** button is visible.
3. **Compass Pointer & Heading**:
   - Tap "TRACK LOCATION".
   - Verify distance to target displays correctly.
   - Physically rotate the device (or simulate in emulator) to verify the pointer needle rotates dynamically towards the target bearing.
   - Test "OPEN IN MAPS" button triggers the external mapping app with coordinates pre-populated.
