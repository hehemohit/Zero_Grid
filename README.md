# ZeroGrid — Technical System Architecture & Specification

### 🏆 Bharat Builds Hackathon — Track 2: Heat & Water
> **High-availability, decentralized disaster telemetry & autonomous rerouting platform. Combines an offline-first Android BLE/Wi-Fi Direct mesh network with an AWS cloud brain powered by the AWS Strands Agents SDK (`@strands-agents/sdk`), OSRM dynamic routing, and AWS Amplify.**

---

## 1. System Design & End-to-End Data Flow

ZeroGrid is architected as a 4-tier hybrid distributed system designed to maintain zero-latency spatial intelligence even during total cellular and power grid failure.

```
═══════════════════════════════════════════════════════════════════════════════════════════════════════════════
 TIER 1: DECENTRALIZED EDGE MESH (OFFLINE CITIZENS & SENSORS)
═══════════════════════════════════════════════════════════════════════════════════════════════════════════════

  [Node A: Citizen / Field Scout]
     │  (Detects 45cm waterlogging / Fallen 11kV line)
     │  Encapsulates into HAZARD_BEACON packet
     │
     ├──► [BLE 5.0 Advertisements / GATT Client-Server]
     │      Range: ~30-100m | Payload: 512 bytes | Zero Router Infrastructure
     │
     ▼
  [Node B: Intermediate Peer Device]
     │  - DeduplicationCache checks packetId (LRU Bloom filter)
     │  - Decrements TTL (max 5 hops), appends hop metric
     │  - Re-broadcasts over dual BLE + Wi-Fi Direct sockets
     │
     ▼
  [Node C: Opportunistic Data Mule]
     │  - Accumulates mesh packets in encrypted local SQLite/Room storage
     │  - Moves into area with cellular coverage (LTE / 5G / Starlink uplink)
     │  - Triggers WorkManager exponential-backoff bulk sync
     │
     └────────────────────────────────────────────────────────────┐
                                                                  │ HTTPS REST / TLS 1.3
                                                                  ▼
═══════════════════════════════════════════════════════════════════════════════════════════════════════════════
 TIER 2: CLOUD INGESTION & SPATIAL DATA PLATFORM (AWS CLOUD INFRASTRUCTURE)
═══════════════════════════════════════════════════════════════════════════════════════════════════════════════

  ┌───────────────────────────────────────────────────────────────┴───────────────────────────────────────────┐
  │ AWS ECS / Fargate Cluster (Express 5 + Node.js 20 Microservices)                                          │
  │                                                                                                           │
  │   [POST /api/sos/bulk-mule] ──► Deduplication & Ingestion Pipeline                                       │
  │                                         │                                                                 │
  │   [POST /api/routes/detour] ────────────┼───────────┐                                                     │
  │                                         ▼           ▼                                                     │
  │   [Socket.IO Server /sos] ──────► Real-Time Event Dispatcher (sos:new, sos:updated)                       │
  └─────────────────────────────────────────┬─────────────────────────────────────────────────────────────────┘
                                            │
                                            ▼
  ┌───────────────────────────────────────────────────────────────────────────────────────────────────────────┐
  │ Amazon DocumentDB (MongoDB 5.0 Compatible Spatial Database)                                              │
  │  - 2dsphere Geospatial Index on `location.coordinates` [lng, lat]                                        │
  │  - Collections: `soses` (Hazard Telemetry), `users`, `emergencynotes`                                     │
  │  - $nearSphere and $geoWithin queries execute in < 4ms for 10km geo-windows                              │
  └─────────────────────────────────────────┬─────────────────────────────────────────────────────────────────┘
                                            │
                                            ▼
═══════════════════════════════════════════════════════════════════════════════════════════════════════════════
 TIER 3: AUTONOMOUS AGENTIC REASONING & DETOUR ENGINE
═══════════════════════════════════════════════════════════════════════════════════════════════════════════════

  ┌───────────────────────────────────────────────────────────────────────────────────────────────────────────┐
  │ AWS Strands Agents SDK Framework (@strands-agents/sdk)                                                    │
  │                                                                                                           │
  │   Foundation Model: Claude 3.5 Sonnet / Haiku via Amazon Bedrock                                          │
  │                                                                                                           │
  │   [Agent Tool Execution Pipeline]:                                                                        │
  │   ├── Tool: query_nearby_hazards(lat, lng, radiusKm, category)                                            │
  │   │     └── Pulls verified telemetry: waterDepthCm, passability, structural damage                        │
  │   ├── Tool: evaluate_vehicle_risk(vehicleType, waterDepthCm)                                              │
  │   │     └── Evaluates mechanical clearance (Sedan 20cm, SUV 45cm, 4x4 70cm)                             │
  │   ├── Tool: compute_evasion_corridor(origin, dest, infectedCentroids)                                     │
  │   │     └── Calculates orthogonal offset waypoints (±90°, 350m buffer) around hazard zones                │
  │   └── Tool: synthesize_municipal_situation_brief(incidentClusterGeoJson)                                  │
  │         └── Aggregates stranded citizen clusters and pump-truck deployment priorities                     │
  └─────────────────────────────────────────┬─────────────────────────────────────────────────────────────────┘
                                            │
                                            ▼
  ┌───────────────────────────────────────────────────────────────────────────────────────────────────────────┐
  │ OSRM (Open Source Routing Machine) Service Engine                                                         │
  │  - Direct Polyline Endpoint: /route/v1/driving/{lng1,lat1;lng2,lat2;...}                                 │
  │  - Computes driving polyline geometry passing strictly through verified evasion waypoints                 │
  │  - Returns GeoJSON LineString coordinates, distance (meters), and duration (seconds)                      │
  │  - Offline Piecewise Geodesic Fallback: Guarantees routing geometry on edge device when fully disconnected│
  └─────────────────────────────────────────┬─────────────────────────────────────────────────────────────────┘
                                            │
                       ┌────────────────────┴────────────────────┐
                       │                                         │
                       ▼                                         ▼
═══════════════════════════════════════════════════════════════════════════════════════════════════════════════
 TIER 4: CLIENT ACTION SURFACES
═══════════════════════════════════════════════════════════════════════════════════════════════════════════════

  [SURFACE A: ANDROID PROACTIVE DRIVER COPILOT & AI AGENT]   [SURFACE B: AWS AMPLIFY MUNICIPAL CONSOLE]
  • HazardProximityMonitor (60s tick, 0 network)             • Next.js 16 Web Dashboard on AWS Amplify
  • SYSTEM_ALERT_WINDOW (<200m danger breach)                • Real-Time Google Maps / Tactical Canvas Layer
  • Safe Route Copilot (Conversational Agent Drawer):        • Socket.IO Live Telemetry Feed
    - In-App GoogleMap Polyline + Evasion Buffers            • One-Click Strands Agent Crisis Briefing
    - Dynamic Vehicle Matrix (Walk / 2W / Car / SUV)         • Dispatcher Pump Truck & Barricade Directives
    - Live Nearby Places Autocomplete & Proximity Sorting    • Multi-Agency Audit Trail & Incident Resolution
    - Responsive IME Soft Keyboard & Animated Collapse
    - 🛡 Avoided Hazards vs. ⚠️ Unavoidable Risks
  • Waypoints-Enforced Google Maps Navigation Intent:
    - Injects intermediate evasion corridors (&waypoints=...)
    - Prevents Google Maps from recalculating through floods
```

---

## 2. Edge Mesh Protocol Specification (`gridzero`)

### 2.1 Packet Envelope Specification

Packets flowing through the P2P mesh network are serialized as deterministic, low-overhead UTF-8 JSON structures designed to fit within BLE MTU constraints:

```json
{
  "packetId": "43659c0a-9e55-49e5-8a46-d06093bdee74",
  "senderId": "node-android-a910f",
  "recipientId": null,
  "type": "HAZARD_BEACON",
  "payload": {
    "category": "WATERLOGGING",
    "waterDepthCm": 45,
    "passability": "IMPASSABLE_SEDANS",
    "lat": 19.0825,
    "lng": 72.8411,
    "accuracy": 4.2,
    "batteryPercentage": 78,
    "message": "Milan Subway water depth 45cm and rising. 2 hatchbacks stalled."
  },
  "timestamp": 1728374120000,
  "ttl": 5,
  "hopCount": 0
}
```

#### Field Constraints
- `packetId` (UUIDv4, 36 bytes): Universally unique identifier used for deduplication.
- `type` (Enum): `HAZARD_BEACON`, `SOS_BEACON`, `PEER_DISCOVERY`, `DIRECT_MESSAGE`, `CHANNEL_BROADCAST`.
- `ttl` (Integer): Time-to-Live, initialized to `5`. Decremented at each hop. Dropped when `ttl <= 0`.
- `hopCount` (Integer): Incremented at each relay to track mesh network diameter.

### 2.2 Deduplication Engine & Loop Prevention

To prevent packet storms in dense mesh clusters:
1. **LRU In-Memory Ring Buffer**: `DeduplicationCache.kt` tracks the last 10,000 processed `packetId` entries with a 30-minute expiration window.
2. **Reverse Path Check**: Packets received with `senderId == localNodeId` or `packetId` existing in cache are silently dropped.
3. **Hop Budget**: Packets reaching `ttl == 0` are discarded before reaching the radio driver queue.

### 2.3 Dual Hardware Transport Drivers

```
                ┌──────────────────────────────────────┐
                │          MeshRoutingEngine           │
                └──────────────────┬───────────────────┘
                                   │
                  ┌────────────────┴────────────────┐
                  ▼                                 ▼
   ┌───────────────────────────────┐ ┌───────────────────────────────┐
   │        BleMeshDriver          │ │     WifiDirectMeshDriver      │
   ├───────────────────────────────┤ ├───────────────────────────────┤
   │ • BLE 5.0 Advertisements      │ │ • Wi-Fi P2P Group Formation   │
   │ • 128-bit Custom Service UUID │ │ • High-Throughput TCP Sockets │
   │ • GATT Client & Server Roles  │ │ • Batch Data Mule Sync        │
   │ • Payload: Small Beacons      │ │ • Payload: Media & Chat Logs  │
   └───────────────────────────────┘ └───────────────────────────────┘
```

- **`BleMeshDriver.kt`**:
  - Implements simultaneous Bluetooth LE peripheral advertising and central scanning.
  - Custom 128-bit Service UUID: `0000FFF0-0000-1000-8000-00805F9B34FB`.
  - Transmits compact 24-byte identification beacons via manufacturer data; larger envelopes are transmitted over transient GATT connections.
- **`WifiDirectMeshDriver.kt`**:
  - Automatically negotiates Wi-Fi P2P group ownership (`WifiP2pManager`).
  - Opens TCP server sockets on port `8888` for high-throughput batch synchronization between proximate devices.

---

## 3. Autonomous Safety Agent & OSRM Detour Engine

### 3.1 Autonomous Agent Architecture (`RouteSafetyAgent.kt`)

The agent is decoupled through a future-ready, pluggable Kotlin contract:

```kotlin
interface RouteSafetyAgent {
    suspend fun executeSafeRoute(
        origin: LatLng,
        destination: LatLng,
        cachedHazards: List<CachedHazard>
    ): RouteSafetyReport
}
```

#### Deterministic Spatial Algorithm (`RuleBasedRouteSafetyAgent`)
1. **Geodesic Corridor Intersection**:
   Calculates the cross-track perpendicular distance from every hazard point $P(lat, lng)$ to the direct geodesic segment connecting Origin $A$ and Destination $B$:
   $$d_{cross} = \arcsin\left(\sin\left(\frac{dist(A, P)}{R}\right) \cdot \sin(\theta_{AP} - \theta_{AB})\right) \cdot R$$
   Hazards within $d_{cross} \le 160\text{ meters}$ are flagged as corridor collisions.

2. **Orthogonal Evasion Waypoint Derivation**:
   For the top 3 highest-severity colliding hazards, the agent computes evasion waypoints shifted orthogonally ($\pm 90^\circ$ relative to bearing $\theta_{AB}$) by 350 meters:
   $$\phi_2 = \arcsin\left(\sin(\phi_1)\cos(d/R) + \cos(\phi_1)\sin(d/R)\cos(\theta \pm 90^\circ)\right)$$
   $$\lambda_2 = \lambda_1 + \text{atan2}\left(\sin(\theta \pm 90^\circ)\sin(d/R)\cos(\phi_1), \cos(d/R) - \sin(\phi_1)\sin(\phi_2)\right)$$

3. **OSRM Route Execution**:
   Queries OSRM driving service passing `[Origin, Waypoint_1, ..., Waypoint_n, Destination]` to generate the driving polyline geometry.

4. **Dual-Category Impact Classification**:
   - 🛡 **Hazards Avoided**: Hazards where direct distance $\le 160\text{m}$ but safe polyline distance $> 200\text{m}$.
   - ⚠️ **Unavoidable Risks**: Hazards remaining within $180\text{m}$ of the final path (e.g., at user origin or destination gate), appended with dynamic vehicle clearance survival tips.

### 3.2 OSRM Routing Service (`OsrmRoutingService.kt`)

- **Primary Path**: Calls OSRM API endpoint:
  ```
  GET https://router.project-osrm.org/route/v1/driving/{lng1},{lat1};{lng2},{lat2};...;{destLng},{destLat}?overview=full&geometries=geojson&steps=false
  ```
- **Parsing**: Deserializes GeoJSON `LineString` coordinates into `List<LatLng>`, extracting `distance` (meters) and `duration` (seconds).
- **Offline Interpolation Fallback**: When offline or if OSRM is unreachable, computes smooth piecewise geodesic interpolation points spaced every 150m between origin, waypoints, and destination, guaranteeing route visual rendering never fails.

### 3.3 Offline Hazard Cache Manager (`HazardCacheManager.kt`)

- **Dual-Layer Storage**: Thread-safe in-memory `AtomicReference<List<CachedHazard>>` backed by persistent AndroidX DataStore JSON serialization (`hazard_cache.preferences_pb`).
- **Geo-Window**: Maintains all active hazard SOS events within a 10 km radius.
- **Refresh Policy**:
  - Condition 1: Network connectivity restored (`ConnectivityChecker`).
  - Condition 2: Cache age exceeds TTL of 5 minutes (`CACHE_TTL_MS = 300,000`).
  - Condition 3: User location drifts $> 1\text{ km}$ from the last fetch origin (`LOCATION_DRIFT_M = 1,000`).

---

## 4. Proactive Driver Copilot & Overlay Architecture

### 4.1 System Alert Window Lifecycle (`SYSTEM_ALERT_WINDOW`)

```
                        [MeshForegroundService]
                                   │
                   (Every 60s non-blocking tick)
                                   ▼
                      [HazardProximityMonitor]
                                   │
           Reads: LocationHelper.getLastKnownLocation()
           Reads: HazardCacheManager.getCachedHazards()
           (ZERO network calls | ZERO wake-lock leaks)
                                   │
                 Haversine Distance <= 200 meters?
                                   │
                        YES ───────┴─────── NO ──► (Sleep 60s)
                         │
                         ▼
               [OverlayAlertManager]
                         │
                         ├── WindowManager.addView()
                         │   • Layout: TYPE_APPLICATION_OVERLAY
                         │   • Window Flags: NOT_TOUCH_MODAL | KEEP_SCREEN_ON
                         │   • Soft Input: SOFT_INPUT_ADJUST_RESIZE
                         │
                         ├── Ringtone: TYPE_ALARM (Looping emergency alert)
                         ├── Vibrator: [0, 400, 200, 400, 800] ms cadence
                         │
                         ▼
                [HazardOverlayView] (Compose over any app / lockscreen)
                         │
       ┌─────────────────┼─────────────────┬─────────────────┐
       ▼                 ▼                 ▼                 ▼
  Step 1: WARNING   Step 2: VEHICLE   Step 3: RISK      Step 4: DEST
  - Hazard Type     - Sedan/Hatch     - Clearance       - Geocoder Search
  - Water Depth     - SUV / 4x4       - Safe / Impass   - Map Pin Tap
  - Close '✕' btn   - Walking         - Evade Advice    - Agent Analysis
                                                             │
                                                             ▼
                                                    Step 5: ROUTE_READY
                                                    - SafeRoutePreviewMap
                                                    - Polyline & Danger Circles
                                                    - Avoided vs Unavoidable
                                                    - Google Maps Handoff
```

### 4.2 In-App Interactive Route Preview (`SafeRoutePreviewMap.kt`)

Renders directly inside Compose before launching third-party navigation apps:
- **`GoogleMap` Composable**:
  - `Polyline`: Color `#29B6F6`, width `12f` representing verified safe geometry.
  - `Marker(Origin)`: Green circle icon for user GPS fix.
  - `Marker(Destination)`: Finish flag pin.
  - `Circle(DangerZones)`: Red (`0x33FF5252`) and Green (`0x334CAF50`) translucent circular buffers (100–120m radius) around hazard coordinates.
  - `CameraPositionState`: Automatically computes `LatLngBounds` enclosing origin, destination, and all waypoints with 80dp padding.
- **`MapsIntentBuilder.kt`**: Deep-links into the Google Maps app with mandatory intermediate waypoint enforcement:
  ```
  https://www.google.com/maps/dir/?api=1&origin={lat},{lng}&destination={lat},{lng}&travelmode=driving&waypoints={wp1_lat},{wp1_lng}|{wp2_lat},{wp2_lng}
  ```
  Forcing Google Maps turn-by-turn navigation to strictly follow the hazard-evasion corridor without recalculating back through submerged streets.

### 4.3 Permanent Conversational Safe Route Copilot (`SafeRouteCopilotScreen.kt`, `RouteCopilotViewModel.kt`)

ZeroGrid incorporates a permanent, first-class conversational route copilot screen accessible across the application:
1. **Interactive Dual-Panel Canvas**:
   - **Top Google Map (Weight Animated)**: Renders live hazard circles, safe emerald polyline (`#10B981`), origin pin, destination pin, and floating stats HUD (Avoided hazards count, distance, ETA).
   - **Bottom Agent Copilot Drawer**: Conversational chat interface displaying AI advice, why-this-detour explanations, and vehicle mode toggles.
2. **Dynamic Vehicle Clearance Matrix**:
   - 🚶 **Walking**: 15 cm max water depth
   - 🛵 **2-Wheeler**: 20 cm max water depth
   - 🚗 **Car / Auto**: 30 cm max water depth
   - 🚙 **SUV / 4x4**: 50 cm max water depth
   - Selecting any vehicle dynamically re-evaluates the corridor against cached hazards and updates the route polyline.
3. **Live Proximity-Biased Autocomplete (`LocationSearchHelper.kt`)**:
   - As the user types into the copilot prompt, debounced background queries (`260ms`) search native Geocoder (`±0.3°` bounding box) and OSM Nominatim.
   - Computes Haversine distance in meters to the user's GPS fix and sorts closest places first with distance badges (e.g. `450 m`, `1.8 km`).
   - Single-tap destination selection from the live recommendations card immediately plots safe routing.
4. **Soft Keyboard Inset Management**:
   - Uses `WindowInsets.ime` detection to animate the map weight from `1.05f` down to `0.35f` when the keyboard opens.
   - Employs `Modifier.navigationBarsPadding().imePadding()` so the conversational drawer and text input box float cleanly above the keyboard without being obscured.

---

## 5. AWS Cloud & Strands Agent Orchestration

### 5.1 AWS Strands Agents Framework Integration (`@strands-agents/sdk`)

The cloud orchestrator runs on **AWS ECS Fargate** using `@strands-agents/sdk` to evaluate spatial telemetry batches uploaded by Data Mules:

```javascript
import { Agent, BedrockModel, tool } from '@strands-agents/sdk';

// 1. Tool Definition: Spatial Radius Telemetry Query
const queryNearbyHazards = tool({
  name: 'queryNearbyHazards',
  description: 'Searches DocumentDB for active waterlogging and powerline hazards within radius',
  parameters: {
    lat: { type: 'number', required: true },
    lng: { type: 'number', required: true },
    radiusKm: { type: 'number', default: 10.0 }
  },
  execute: async ({ lat, lng, radiusKm }) => {
    return await SosEvent.find({
      status: { $in: ['ACTIVE', 'ACKNOWLEDGED'] },
      location: {
        $nearSphere: {
          $geometry: { type: 'Point', coordinates: [lng, lat] },
          $maxDistance: radiusKm * 1000
        }
      }
    }).lean();
  }
});

// 2. Tool Definition: Passability & Vehicle Clearance Assessment
const assessVehicleRisk = tool({
  name: 'assessVehicleRisk',
  description: 'Evaluates vehicle mechanical passability against water depth',
  parameters: {
    vehicleType: { type: 'string', required: true },
    waterDepthCm: { type: 'number', required: true }
  },
  execute: async ({ vehicleType, waterDepthCm }) => {
    const limits = { HATCHBACK: 20, SEDAN: 25, SUV: 45, FOUR_BY_FOUR: 70 };
    const maxSafe = limits[vehicleType.toUpperCase()] || 20;
    return {
      isPassable: waterDepthCm < maxSafe,
      riskLevel: waterDepthCm >= maxSafe ? 'CRITICAL_IMPASSABLE' : 'CAUTION_PROCEED',
      maxSafeDepthCm: maxSafe
    };
  }
});

// 3. Agent Instantiation
export const disasterResilienceAgent = new Agent({
  model: new BedrockModel({
    modelId: 'anthropic.claude-3-5-sonnet-20241022-v2:0',
    region: process.env.AWS_REGION || 'ap-south-1'
  }),
  systemPrompt: `You are the ZeroGrid Urban Climate Resilience Agent.
Your role is to analyze flood and heatwave telemetry from mesh networks,
compute safe evasion corridors avoiding flooded underpasses (depth >= 30cm)
and live electrical wires, and formulate operational briefings for municipal command.`,
  tools: [queryNearbyHazards, assessVehicleRisk]
});
```

### 5.2 AWS Cloud Infrastructure Topology

| Component | AWS Implementation | Configuration / Role |
|---|---|---|
| **Municipal Dashboard** | **AWS Amplify Hosting** | Next.js 16 SSR + Static Pages. Deployed via Git-based CI/CD pipeline on Amplify. |
| **API & Socket Backend** | **AWS ECS on AWS Fargate** | Express 5 Node.js container. Auto-scaling policy based on CPU/Memory and incoming connection surges. |
| **Agentic AI Model** | **Amazon Bedrock** | Provides Claude 3.5 Sonnet / Haiku execution for the `@strands-agents/sdk` runtime. |
| **Spatial Database** | **Amazon DocumentDB** | MongoDB 5.0 compatible. Multi-AZ cluster with `2dsphere` indexes on geospatial collections. |
| **Notification Broker** | **Amazon SNS + FCM** | High-priority push notifications dispatched to registered family contacts and field rescue units. |

---

## 6. REST API & WebSocket Interface

### 6.1 Telemetry & Detour REST Endpoints

```
POST /api/sos/bulk-mule
Content-Type: application/json
Authorization: Bearer <JWT>

{
  "packets": [
    {
      "packetId": "e1f2a3b4-...",
      "lat": 19.0760,
      "lng": 72.8777,
      "category": "WATERLOGGING",
      "waterDepthCm": 50,
      "passability": "IMPASSABLE",
      "transport": "MESH",
      "ts": 1728374100000
    }
  ]
}
```
**Response (200 OK):**
```json
{
  "accepted": 1,
  "duplicates": 0
}
```

```
POST /api/routes/detour
Content-Type: application/json

{
  "originLat": 19.0760,
  "originLng": 72.8777,
  "destLat": 19.1136,
  "destLng": 72.8697
}
```
**Response (200 OK):**
```json
{
  "safeRouteGeoJson": "{\"type\":\"LineString\",\"coordinates\":[[72.8777,19.0760],[72.8710,19.0920],[72.8697,19.1136]]}",
  "warningMessage": "Detoured around 2 critical flood hazards at Milan Subway.",
  "avoidedHazardsCount": 2,
  "agentReasoning": "Corridor passed within 60m of 50cm waterlogging. Shifted path 350m west onto SV Road elevated corridor."
}
```

### 6.2 Real-Time WebSocket Interface (`/sos` namespace)

- **`connection`**: Clients establish bidirectional WebSocket stream over TLS.
- **`sos:new`**: Broadcast when a fresh hazard or citizen SOS is registered (via mesh mule or direct uplink).
- **`sos:updated`**: Broadcast when status transitions (`ACTIVE` $\rightarrow$ `ACKNOWLEDGED` $\rightarrow$ `RESOLVED`).

---

## 7. Codebase Directory Organization

```
AndroidStudioProjects/
├── gridzero/                               # Android Application (Kotlin / Compose)
│   ├── app/src/main/
│   │   ├── java/com/example/zerogrid/
│   │   │   ├── admin/                      # Mobile Admin Panel & Tactical Radar Canvas
│   │   │   ├── auth/                       # Local & Google OAuth2 Authentication
│   │   │   ├── emergency/                  # Hazard Overlay, Safety Agent & Detour Planning
│   │   │   │   ├── HazardOverlayView.kt    # Full-screen system alert window
│   │   │   │   ├── OverlayAlertManager.kt  # SYSTEM_ALERT_WINDOW manager
│   │   │   │   ├── RouteSafetyAgent.kt     # Rule-based safety agent & corridor evaluator
│   │   │   │   ├── RouteCopilotViewModel.kt # Conversational Copilot & live search state
│   │   │   │   ├── SafeRouteCopilotScreen.kt # Split-screen interactive map + chat copilot
│   │   │   │   ├── SafeRoutePlannerDialog.kt # Safe route destination modal
│   │   │   │   ├── SosCenterScreen.kt      # Emergency feed & beacon trigger
│   │   │   │   └── UnifiedSosDispatcher.kt # Mesh + Cloud parallel dispatcher
│   │   │   ├── home/                       # Dashboard with NearbyHazardsRadarCard.kt
│   │   │   ├── location/                   # Location & Proximity Geofencing
│   │   │   │   ├── HazardCacheManager.kt   # 10km offline DataStore hazard cache
│   │   │   │   ├── HazardProximityMonitor.kt # 60s background geofence loop
│   │   │   │   ├── LocationHelper.kt       # Multi-provider GPS (Samsung FLP compliant)
│   │   │   │   ├── LocationSearchHelper.kt # Unified proximity-biased geocoding & OSM search
│   │   │   │   └── VehicleRiskCalculator.kt # Vehicle clearance vs water depth matrix
│   │   │   ├── mesh/                       # MeshEngine, BLE & Wi-Fi Direct Drivers
│   │   │   │   ├── engine/                 # RoutingEngine, Packet models, DeduplicationCache
│   │   │   │   └── transport/              # BleMeshDriver, WifiDirectMeshDriver
│   │   │   ├── network/                    # Retrofit APIs & OsrmRoutingService.kt
│   │   │   ├── service/                    # MeshForegroundService (Type: Location)
│   │   │   ├── ui/components/              # NearbyHazardsRadarCard (Dynamic Theme), SafeRoutePreviewMap
│   │   │   └── util/                       # MapsIntentBuilder.kt (Waypoint enforcement)
│   │   └── AndroidManifest.xml             # Foreground service & overlay declarations
│   └── build.gradle.kts                    # Android build script (SDK 35, Maps Compose)
│
└── gridZeroExpress/                        # AWS Cloud Backend & Web Console
    ├── backend/                            # Express 5 + MongoDB + AWS Strands Agents
    │   ├── src/
    │   │   ├── server.js                   # REST & Socket.IO server initialization
    │   │   ├── controllers/                # SOS, Data Mule & Detour endpoints
    │   │   ├── models/                     # Mongoose Schemas (2dsphere indexed)
    │   │   └── routes/                     # REST API routes (/api/sos, /api/routes)
    │   └── package.json                    # Backend dependencies (@strands-agents/sdk)
    │
    └── frontend/                           # AWS Amplify Next.js 16 Web Dashboard
        ├── src/
        │   ├── app/                        # App Router (admin, dashboard, auth)
        │   └── components/admin/           # SosLiveMap, SosDrawer, MapCanvas
        └── package.json                    # Frontend dependencies
```

---

## 8. Setup & Execution Guide

### 8.1 Backend Deployment (AWS ECS / Local)
```bash
cd gridZeroExpress/backend
npm install
```
Configure `.env`:
```env
PORT=5000
NODE_ENV=development
MONGODB_URI=mongodb+srv://<user>:<pwd>@cluster.mongodb.net/zerogrid?retryWrites=true&w=majority
JWT_SECRET=super_secret_jwt_key_at_least_32_chars_long
ALLOWED_ORIGINS=http://localhost:3000
AWS_REGION=ap-south-1
```
Run development server:
```bash
npm run dev
# Health Endpoint: http://localhost:5000/health
# WebSocket URL:   ws://localhost:5000/sos
```

### 8.2 Municipal Dashboard Deployment (AWS Amplify / Local)
```bash
cd gridZeroExpress/frontend
npm install
```
Configure `.env.local`:
```env
NEXT_PUBLIC_API_URL=http://localhost:5000
NEXT_PUBLIC_GOOGLE_MAPS_API_KEY=AIzaSy...
```
Start Next.js:
```bash
npm run dev
# Dashboard accessible at: http://localhost:3000
```

### 8.3 Android Application Compilation (`gridzero`)
1. Open `gridzero` in Android Studio Ladybug (or newer).
2. Create/update `local.properties` in project root:
   ```properties
   sdk.dir=C:\\Users\\<USER>\\AppData\\Local\\Android\\Sdk
   MAPS_API_KEY=AIzaSy...
   ```
3. Verify `google-services.json` is located in `app/`.
4. Compile and assemble debug APK via CLI:
   ```bash
   ./gradlew :app:assembleDebug
   ```
5. Deploy to physical Android hardware (API 26+ / Android 8.0+) to enable BLE peripheral advertising.

---

## 9. Hardware Optimizations & Edge Troubleshooting

| Issue / Error | Root Cause | Engineering Solution |
|---|---|---|
| **Samsung FLP listener rejection (`10416_FINE_fg_svc_false_foreground`)** | Android 14+ requires foreground services accessing location to specify `foregroundServiceType="location"`. | Declared `FOREGROUND_SERVICE_LOCATION` in manifest and passed `FOREGROUND_SERVICE_TYPE_LOCATION` in `MeshForegroundService.startForeground()`. |
| **System overlay keyboard blocked** | `WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE` prevented IME keyboard input in the overlay. | Removed `FLAG_NOT_FOCUSABLE` and configured `softInputMode = SOFT_INPUT_ADJUST_RESIZE` in `OverlayAlertManager.kt`. |
| **Background location freeze on Samsung devices** | Aggressive vendor power management suspends standard fused listeners. | Implemented multi-provider polling (`PASSIVE_PROVIDER`, `FUSED_PROVIDER`, `getCurrentLocation()`) in `LocationHelper.kt`. |
| **BLE advertising failure (`ADVERTISE_FAILED_FEATURE_UNSUPPORTED`)** | Android Emulator lacks hardware BLE peripheral mode. | Run on physical Android hardware for mesh radio verification. |
| **High Logcat frame invalidation (`gralloc4`)** | Infinite layout animations recalculating every frame in top bars. | Replaced infinite transitions with static indicator chips. |
| **Soft keyboard covering chat input & suggestions** | Default Compose Scaffold consumed insets without raising the bottom drawer above virtual keyboard. | Added `navigationBarsPadding().imePadding()`, set `Scaffold(contentWindowInsets = WindowInsets.statusBars)`, and animated map weight from `1.05f` down to `0.35f`. |
| **Google Maps recalculating routes through flooded corridors** | Opening direct origin-destination intents caused Google Maps navigation to route along the shortest path (through flooded subways). | Built `MapsIntentBuilder.launchWithReport()` injecting up to 8 intermediate evasion waypoints into the intent URL (`&waypoints=...`). |

---

<p align="center">
  <b>ZeroGrid Engineering Team — Bharat Builds Hackathon 2026</b><br>
  <i>Decentralized Mesh Networking & Autonomous Agentic Climate Resilience</i>
</p>
