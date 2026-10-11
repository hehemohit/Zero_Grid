# Fix Compass Direction & Needle Tracking in TrackSosScreen

Fix the orientation tracking, sensor coordinate remapping, jitter filtering, 360° wrap-around glitch, and target alignment logic in `TrackSosScreen.kt`.

## Why the Compass is Currently Inaccurate and Buggy

1. **Walking Backwards Marked as "On Target" (Fatal Math Bug)**:
   In line 88: `val isOnTarget = abs(needleAngle - 180f) < 10f || needleAngle < 10f...`.
   `180°` is pointing **directly behind you (opposite direction)**. This made the radar turn green and claim you were "On Target" when walking completely away from the victim!
2. **Missing Sensor Coordinate Remapping (Tilt & Pitch Distortion)**:
   `SensorManager.getRotationMatrixFromVector` assumes the phone is lying flat on a table. When a user holds their phone upright while walking, the coordinate system must be remapped (`SensorManager.remapCoordinateSystem(rotMatrix, AXIS_X, AXIS_Z, ...)`) to match device display pitch. Without this, tilting the phone even 20 degrees introduces a 45°–90° false heading shift.
3. **`Location.distanceBetween` Returns `-180°` to `+180°`**:
   Android's `Location.distanceBetween` outputs initial bearing in the `[-180.0, +180.0]` range. It was not normalized to `[0, 360]`, leading to incorrect relative needle math.
4. **360° Wrap-Around Needle Spinning**:
   When the angle crosses North (`359°` to `1°`), `animateFloatAsState` animates from 359 down through 180 to 1, causing the needle to spin wildly around the dial in the wrong direction.
5. **Magnetic North vs True Geographic North**:
   GPS bearings use **True North**, while the phone's magnetometer measures **Magnetic North**. Without applying `GeomagneticField.declination`, there is a permanent offset error.
6. **Raw Sensor Jitter**:
   The magnetometer is noisy and vulnerable to micro-movements. Without an Exponential Moving Average (EMA) low-pass filter, the needle vibrates and twitches erratically.

---

## Proposed Changes

### Android App (`gridzero`)

#### [MODIFY] [TrackSosScreen.kt](file:///c:/Users/ACER/AndroidStudioProjects/gridzero/app/src/main/java/com/example/zerogrid/emergency/TrackSosScreen.kt)

1. **Remap Coordinate System for Upright Handheld Orientation**:
   - Detect device pitch/tilt or remap rotation matrix to `AXIS_X` and `AXIS_Z` when phone is held upright, keeping heading stable in hand.
2. **Exponential Moving Average (EMA) Low-Pass Filter**:
   - Smooth device azimuth and relative needle angle using a low-pass filter ($\alpha \approx 0.15$) so the needle glides smoothly without jitter.
3. **Shortest Angular Distance Interpolation (Fix 360° Wrap Glitch)**:
   - Calculate delta angle as `((target - current + 540) % 360) - 180` to prevent wild 360° spins when crossing North.
4. **True North Compensation (GeomagneticField)**:
   - Use `GeomagneticField(lat, lon, alt, time).declination` to adjust magnetic heading to True North matching GPS bearing.
5. **Fix "ON TARGET" Alignment**:
   - Mark as "On Target" only when the relative angle to the target is within $\pm 15^\circ$ of straight ahead ($0^\circ$).
6. **Bearing Normalization**:
   - Normalize GPS bearing: `(results[1] + 360f) % 360f`.

---

## Verification Plan

### Automated Tests

- Run `.\gradlew compileDebugKotlin` to verify compilation.

### Manual Verification

1. Open Track SOS screen.
2. Verify needle points steadily towards the victim's location.
3. Rotate phone 360°: confirm needle stays locked on target direction and does not do a full spin at 0°/360° boundary.
4. Tilt phone upright: confirm heading does not jump or distort.
5. Point phone directly towards victim: confirm green "ON TARGET" triggers only when facing them (not when facing away).
