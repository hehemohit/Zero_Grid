# ZeroGrid — Urban Flood & Heatwave Resilience Mesh

### 🏆 Built for Bharat Builds Hackathon — Track 2: Heat and Water
> **An offline-first, agent-driven disaster resilience platform that decentralizes hyper-local hazard telemetry through peer-to-peer Android mesh networks, orchestrates autonomous risk mitigation via the AWS Strands Agents SDK, and delivers life-saving rerouting to citizens and real-time crisis intelligence to municipal authorities.**

[![Track](https://img.shields.io/badge/Bharat%20Builds-Track%202%3A%20Heat%20%26%20Water-00C853?style=for-the-badge)](https://bharatbuilds.dev)
[![AWS](https://img.shields.io/badge/AWS-Amplify%20%7C%20ECS%20%7C%20Strands%20Agents-FF9900?style=for-the-badge&logo=amazon-aws&logoColor=white)](https://aws.amazon.com/)
[![Android](https://img.shields.io/badge/Android-Kotlin%20%7C%20Jetpack%20Compose%20%7C%20BLE%20Mesh-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue?style=for-the-badge)](LICENSE)

---

## 📑 Table of Contents

1. [The Crisis: Urban Climate Vulnerability in India](#1-the-crisis-urban-climate-vulnerability-in-india)
2. [The Vision: ZeroGrid](#2-the-vision-zerogrid)
3. [Core Innovation Pillars](#3-core-innovation-pillars)
   - 3.1 [The Edge Layer: Offline-First P2P Mesh & Data Mules](#31-the-edge-layer-offline-first-p2p-mesh--data-mules)
   - 3.2 [The Cloud Brain: AWS Strands Agents SDK Integration](#32-the-cloud-brain-aws-strands-agents-sdk-integration)
   - 3.3 [The Citizen Experience: Proactive Driver Copilot](#33-the-citizen-experience-proactive-driver-copilot)
   - 3.4 [The Municipal Experience: AWS Amplify Command Center](#34-the-municipal-experience-aws-amplify-command-center)
4. [End-to-End System Architecture](#4-end-to-end-system-architecture)
5. [AWS Cloud & Agentic Architecture](#5-aws-cloud--agentic-architecture)
   - 5.1 [AWS Strands Agents SDK Pipeline](#51-aws-strands-agents-sdk-pipeline)
   - 5.2 [AWS Cloud Infrastructure Blueprint](#52-aws-cloud-infrastructure-blueprint)
6. [Android Edge Engineering (`gridzero`)](#6-android-edge-engineering-gridzero)
   - 6.1 [BLE & Wi-Fi Direct Mesh Engine](#61-ble--wi-fi-direct-mesh-engine)
   - 6.2 [Proactive System Alert Window (`SYSTEM_ALERT_WINDOW`)](#62-proactive-system-alert-window-system_alert_window)
   - 6.3 [OSRM Autonomous Route Engine & Dual-Category Analysis](#63-osrm-autonomous-route-engine--dual-category-analysis)
   - 6.4 [Offline Hazard Cache Manager & Monitored Categories](#64-offline-hazard-cache-manager--monitored-categories)
7. [Municipal Command Center (`gridZeroExpress/frontend`)](#7-municipal-command-center-gridzeroexpressfrontend)
8. [Why ZeroGrid Wins Hackathon Judging](#8-why-zerogrid-wins-hackathon-judging)
9. [Project Directory Structure](#9-project-directory-structure)
10. [Setup & Deployment Guide](#10-setup--deployment-guide)
11. [Troubleshooting & Hardware Optimization Notes](#11-troubleshooting--hardware-optimization-notes)

---

## 1. The Crisis: Urban Climate Vulnerability in India

Every monsoon, Indian metropolises (Mumbai, Chennai, Bengaluru, Delhi, Kolkata) experience catastrophic flash floods, localized cloudbursts, and severe waterlogging. Simultaneously, pre-monsoon heatwaves regularly push urban heat island temperatures past 48°C.

During these extreme climate events:
1. **Cellular Infrastructure Collapses**: Cell towers flood, lose grid power, or become saturated by surge traffic, creating total communication blackouts.
2. **Traditional GPS Becomes Blind**: Mainstream navigation applications (Google Maps, Apple Maps) require persistent cloud connectivity and lack hyper-local ground telemetry—such as water depth inside subways or fallen electrical lines—leading unsuspecting drivers straight into submerged underpasses (e.g., Milan Subway, Malad Subway) and fatal electrocution hazards.
3. **Municipal Responders Operate in the Dark**: City disaster control rooms rely on delayed phone calls, leaving them unable to identify blocked arterial roads or pinpoint where emergency de-watering pump trucks are urgently needed.

---

## 2. The Vision: ZeroGrid

**ZeroGrid** is an infrastructure-independent disaster resilience platform tailored for **Track 2: Heat and Water** of the Bharat Builds Hackathon.

ZeroGrid decentralizes disaster telemetry by turning every citizen's Android smartphone into an autonomous mesh relay node. Using Bluetooth Low Energy (BLE) and Wi-Fi Direct, devices broadcast localized hazard beacons peer-to-peer. When any node encounters an active connection, it acts as a **"Data Mule"**, uplinking telemetry to an **AWS Cloud Brain** powered by the **AWS Strands Agents SDK**.

The agent cross-references live user travel vectors against real-time water depth and heat indexes, generating proactive audio/visual bypass routes for citizens and automated crisis briefings for city authorities.

---

## 3. Core Innovation Pillars

### 3.1 The Edge Layer: Offline-First P2P Mesh & Data Mules
- **Zero-Cellular Telemetry**: Devices form autonomous ad-hoc P2P networks using BLE GATT and Wi-Fi Direct sockets. Packets hop device-to-device across a 5-hop TTL with loop-prevention deduplication.
- **`HAZARD_BEACON` Protocol**: Nodes broadcast compact, byte-efficient telemetry:
  - `WATERLOGGING` (with measured `waterDepthCm` and passability status)
  - `SUBMERGED_UNDERPASS`
  - `FALLEN_POWERLINE` / `LIVE_WIRE`
  - `HEATWAVE_SHELTER` / `COOLING_CENTER`
  - `ROAD_BLOCKAGE` & `STRUCTURAL_COLLAPSE`
- **Opportunistic Data Mule Ingress**: When any citizen walks or drives near an area with residual cellular reception, their device automatically flushes the local mesh packet store to the AWS backend in bulk via WorkManager with exponential backoff.

### 3.2 The Cloud Brain: AWS Strands Agents SDK Integration
- **Framework Compliance**: Built on the official AWS open-source framework: `@strands-agents/sdk`.
- **Spatial Tool Calling**: The Strands Agent is equipped with spatial tools (`queryNearbyHazards`, `evaluatePassabilityMatrix`, `calculateBypassWaypoints`, `synthesizeSituationBrief`).
- **Dynamic Vehicle Clearance Matrix**: The agent cross-references user vehicle profiles (Hatchback $\le 20\text{ cm}$, Sedan $\le 25\text{ cm}$, SUV $\le 45\text{ cm}$, 4x4 $\le 70\text{ cm}$) against real-time water depths to calculate whether a corridor is safe, hazardous, or impassable.

### 3.3 The Citizen Experience: Proactive Driver Copilot
- **200m–500m Geofenced System Alert Window**: A background proximity monitor running in a foreground service triggers a high-priority `SYSTEM_ALERT_WINDOW` over any application or lockscreen when the user approaches an active hazard.
- **Hands-Free Audio Warning**: Speaks out danger alerts: *"Warning! 45 cm deep waterlogging detected 300 meters ahead at Milan Subway. Re-routing via elevated flyover."*
- **In-App Interactive Route Preview**: Renders the complete OSRM safe bypass polyline, danger circles, and evasion waypoints directly on an interactive Google Map before handing off to turn-by-turn navigation.
- **Dual-List Risk Classification**:
  - 🛡 **Avoided by GridZero**: Hazards successfully detoured around.
  - ⚠️ **Unavoidable Hazards to Face Anyway**: Residual conditions near destination gates, paired with tactical driving advice (e.g., maintaining 1st gear, low throttle).

### 3.4 The Municipal Experience: AWS Amplify Command Center
- **Deployed on AWS Amplify**: Globally accessible, responsive command dashboard for municipal corporations (e.g., BMC, BBMP, NDMC).
- **Live Spatial Incident Map**: Real-time visualization of flood depths, submerged underpasses, and heatwave shelter capacities.
- **Automated AI Crisis Briefings**: Municipal operators trigger one-click Strands Agent briefings summarizing blocked arteries, stranded vehicle clusters, and optimal pump deployment zones.

---

## 4. End-to-End System Architecture

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                        EDGE TIER: BHARAT DISASTER GROUND ZERO                          │
│                                                                                        │
│   [Citizen A (Offline)] ───(BLE GATT)───► [Citizen B (Relay)]                          │
│     • Spots 60cm waterlogging                • Propagates packet                       │
│     • Broadcasts HAZARD_BEACON               • TTL decremented                         │
│                                                      │ (Wi-Fi Direct P2P)              │
│                                                      ▼                                 │
│                                            [Citizen C (Data Mule)]                     │
│                                            • Moves near cellular tower                 │
│                                            • Opportunistic uplink                      │
└──────────────────────────────────────────────┬─────────────────────────────────────────┘
                                               │ HTTPS REST / Bulk Mule API
                                               ▼
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                         AWS CLOUD BRAIN (HOSTED ON AWS)                                │
│                                                                                        │
│  ┌───────────────────────┐   ┌────────────────────────┐   ┌─────────────────────────┐  │
│  │ AWS Amplify           │   │ AWS ECS / Fargate      │   │ AWS Strands Agents SDK  │  │
│  │ Municipal Web Console │◄──┤ Express 5 Backend Node │◄──┤ @strands-agents/sdk     │  │
│  │ Next.js 16 Dashboard  │   │ REST & Socket.IO       │   │ Spatial Tool Reasoner   │  │
│  └───────────────────────┘   └───────────┬────────────┘   └─────────────────────────┘  │
│                                          │                                             │
│                                          ▼                                             │
│                              ┌────────────────────────┐                                │
│                              │ Amazon DocumentDB      │                                │
│                              │ (MongoDB Compatible)   │                                │
│                              │ 2dsphere Geo-Indexes   │                                │
│                              └────────────────────────┘                                │
└──────────────────────────────────────────┬─────────────────────────────────────────────┘
                                           │
                                           ▼
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                      PROACTIVE CITIZEN & MUNICIPAL ACTION                              │
│                                                                                        │
│   [CITIZEN DRIVER COPILOT]                     [MUNICIPAL COMMAND CENTER]              │
│   • Background Geofence Alert (<200m)          • Real-time heat & water incident map   │
│   • In-App OSRM Visual Route Preview           • Strands Agent Crisis Briefing         │
│   • Avoided vs. Unavoidable Breakdown          • Pump-truck & cooling center dispatch  │
│   • Google Maps Navigation Handoff             • Multi-agency situational awareness    │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 5. AWS Cloud & Agentic Architecture

### 5.1 AWS Strands Agents SDK Pipeline

ZeroGrid leverages the **AWS Strands Agents SDK** (`@strands-agents/sdk`) to execute deterministic and LLM-augmented spatial reasoning:

```
                          ┌───────────────────────────┐
                          │   Incoming Hazard Event   │
                          │   (Telemetry / SOS Mule)  │
                          └─────────────┬─────────────┘
                                        │
                                        ▼
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                          AWS STRANDS AGENT ORCHESTRATOR                                 │
│                                                                                         │
│   Agent Model: Claude 3.5 Sonnet / Haiku via Amazon Bedrock (Strands Execution Loop)    │
│                                                                                         │
│   Registered Agent Tools:                                                               │
│   1. get_nearby_hazards(lat, lng, radiusKm, category)                                   │
│   2. assess_vehicle_risk(vehicleType, waterDepthCm, passability)                        │
│   3. calculate_evasion_corridor(origin, dest, hazardCoordinates)                       │
│   4. generate_municipal_briefing(incidentClusters, infrastructureStatus)                │
└───────────────────────────────────────┬─────────────────────────────────────────────────┘
                                        │
                   ┌────────────────────┴────────────────────┐
                   ▼                                         ▼
┌─────────────────────────────────────┐   ┌─────────────────────────────────────┐
│      Citizen Detour Strategy        │   │     Municipal Action Directive      │
│  • Evasion waypoints (+350m offset) │   │  • Blockade Malad Subway (65cm)     │
│  • Dual-Category classification     │   │  • Dispatch 2 pumps to SV Road      │
│  • Google Maps intent payload       │   │  • Open BKC Cooling Shelter #4      │
└─────────────────────────────────────┘   └─────────────────────────────────────┘
```

### 5.2 AWS Cloud Infrastructure Blueprint

| AWS Service | Role in ZeroGrid | Benefit |
|---|---|---|
| **AWS Amplify** | Hosting & CI/CD for Municipal Web Dashboard | Global low-latency CDN, instant pull-request previews, zero-config Next.js 16 deployment. |
| **AWS ECS / Fargate** | Containerized Express backend & Socket.IO server | Serverless container orchestration that auto-scales during cloudburst traffic spikes. |
| **Amazon Bedrock** | Foundation model provider for Strands Agents SDK | Enterprise-grade, low-latency generative inference within AWS data centers. |
| **Amazon DocumentDB** | Managed MongoDB-compatible spatial database | Native `2dsphere` geospatial indexing for high-speed radius queries. |
| **Amazon SNS / SQS** | Telemetry ingestion queue & push notification fanout | Decouples mobile data-mule batch submissions from real-time socket broadcasts. |

---

## 6. Android Edge Engineering (`gridzero`)

The Android application is built in **100% Kotlin with Jetpack Compose** and operates without cloud dependencies.

### 6.1 BLE & Wi-Fi Direct Mesh Engine
- **Dual-Radio Driver Architecture**:
  - `BleMeshDriver.kt`: Operates custom BLE advertising and scanning service with GATT servers for small telemetry packets.
  - `WifiDirectMeshDriver.kt`: Opens high-throughput Wi-Fi P2P groups for bulk sync when nodes remain proximate.
- **Routing Engine (`MeshRoutingEngine.kt`)**: Implements ad-hoc flooding with a 5-hop TTL and in-memory LRU deduplication (`DeduplicationCache.kt`), preventing packet storms across dense clusters.
- **Background Persistence**: Kept alive via `MeshForegroundService.kt` with explicit `FOREGROUND_SERVICE_TYPE_LOCATION` compliance for Android 14+.

### 6.2 Proactive System Alert Window (`SYSTEM_ALERT_WINDOW`)
- **Zero-Power Geofence Monitor (`HazardProximityMonitor.kt`)**: Ticks once every 60 seconds reading the user's last-known GPS coordinates and evaluating distance against `HazardCacheManager.kt`. Routine checks require **0 network calls**.
- **System Overlay (`OverlayAlertManager.kt` & `HazardOverlayView.kt`)**: When a user breaches the 200m hazard threshold, a full-screen alert overlay inflates over any app or lockscreen with an alarm ringtone and distinct vibration cadence.
- **Interactive Triage**: Drivers select their vehicle type, view clearance risk assessments, search destinations, and inspect the safe bypass route before dismissing the overlay.

### 6.3 OSRM Autonomous Route Engine & Dual-Category Analysis
- **Routing Engine (`OsrmRoutingService.kt`)**: Integrates Open Source Routing Machine driving algorithms (`/route/v1/driving`) with custom intermediate evasion waypoints, parsing GeoJSON polylines into map coordinates.
- **Offline Interpolation Fallback**: Automatically generates piecewise geodesic coordinates if internet is severed.
- **Autonomous Safety Agent (`RouteSafetyAgent.kt`)**:
  - Evaluates direct corridors against local database hazard clusters.
  - Shifts evasive waypoints orthogonally ($\pm 90^\circ$, 350m offset) around hazard centers.
  - Outputs a **Dual-Category Impact Assessment**:
    - 🛡 **Hazards Avoided**: Submerged underpasses and live power cables detoured around.
    - ⚠️ **Unavoidable Hazards**: Residual puddling near destination gates with tactical survival advice.
- **In-App Visual Preview (`SafeRoutePreviewMap.kt`)**: Renders an interactive map with the safe route polyline, origin/destination pins, hazard danger circles, and evasion waypoints before triggering Google Maps navigation.

### 6.4 Offline Hazard Cache Manager & Monitored Categories
- **Persistence (`HazardCacheManager.kt`)**: Stores hazard SOS events within 10 km in an atomic in-memory reference and persists to AndroidX DataStore JSON.
- **Automatic Refresh Triggers**: Re-fetches only when internet is restored, after a 5-minute TTL, or when the user moves $> 1\text{ km}$ from the last fetch center.
- **Supported Hazards**: `WATERLOGGING`, `SUBMERGED_UNDERPASS`, `DRAINAGE_OVERFLOW`, `FALLEN_POWERLINE`, `LIVE_WIRE`, `POWER_OUTAGE`, `FALLEN_TREE`, `STRUCTURAL_COLLAPSE`, `ROAD_BLOCKAGE`.

---

## 7. Municipal Command Center (`gridZeroExpress/frontend`)

Hosted on **AWS Amplify**, the municipal dashboard provides disaster management cells (NDRF, BMC, Traffic Police) with real-time situational control:

- **Google Maps Web Layer (`SosLiveMap.tsx`)**: Renders live water depth markers, heat risk contours, and emergency contact clusters.
- **Incident Dispatch Drawer (`SosDrawer.tsx`)**: Allows dispatchers to assign pump units, update incident status (`ACTIVE` $\rightarrow$ `ACKNOWLEDGED` $\rightarrow$ `RESOLVED`), and log chronological field notes.
- **Tactical Fallback Radar (`MapCanvas.tsx`)**: A pure HTML5 Canvas radar for low-bandwidth workstations.
- **AI Crisis Briefing Generator**: Invokes the AWS Strands Agent to generate structured operational summaries for field command teams.

---

## 8. Why ZeroGrid Wins Hackathon Judging

| Judging Dimension | Why ZeroGrid Excels |
|---|---|
| **Bharat Builds Theme Fit (Track 2: Heat & Water)** | Directly solves urban Indian monsoon flooding (subways, flooded roads) and severe heatwaves (cooling center discovery, vulnerable citizen monitoring). |
| **True Infrastructure Independence** | Operates off-grid via P2P Bluetooth/Wi-Fi mesh; zero reliance on cell towers during disaster blackouts. |
| **AWS Technical Depth** | Built with AWS Amplify (hosting), AWS ECS/Fargate (backend), and the official AWS Strands Agents SDK (`@strands-agents/sdk`). |
| **Agentic Innovation** | Uses autonomous spatial tool-calling agents for live corridor hazard avoidance rather than basic static routing. |
| **Production-Grade Engineering** | 100% Kotlin Compose, Android 14+ foreground service compliance, robust offline DataStore caching, and Google Maps deep-link navigation handoff. |

---

## 9. Project Directory Structure

```
AndroidStudioProjects/
├── gridzero/                               # Android Native Client (Kotlin / Compose)
│   ├── app/src/main/
│   │   ├── java/com/example/zerogrid/
│   │   │   ├── admin/                      # Mobile Admin Panel & Tactical Radar
│   │   │   ├── auth/                       # Local & Google OAuth Authentication
│   │   │   ├── emergency/                  # Hazard Overlay, Safety Agent & Safe Route Planner
│   │   │   │   ├── HazardOverlayView.kt    # Full-screen system alert window
│   │   │   │   ├── OverlayAlertManager.kt  # SYSTEM_ALERT_WINDOW manager
│   │   │   │   ├── RouteSafetyAgent.kt     # Autonomous route safety agent
│   │   │   │   ├── SafeRoutePlannerDialog.kt # Safe route destination modal
│   │   │   │   ├── SosCenterScreen.kt      # Emergency feed & beacon trigger
│   │   │   │   └── UnifiedSosDispatcher.kt # Mesh + Cloud parallel dispatcher
│   │   │   ├── home/                       # Dashboard with NearbyHazardsRadarCard
│   │   │   ├── location/                   # Location & Proximity Geofencing
│   │   │   │   ├── HazardCacheManager.kt   # 10km offline DataStore hazard cache
│   │   │   │   ├── HazardProximityMonitor.kt # 60s background geofence loop
│   │   │   │   ├── LocationHelper.kt       # Multi-provider GPS with Samsung fixes
│   │   │   │   ├── LocationSearchHelper.kt # Unified geocoding & OSM search
│   │   │   │   └── VehicleRiskCalculator.kt # Vehicle clearance vs water depth matrix
│   │   │   ├── mesh/                       # MeshEngine, BLE & Wi-Fi Direct Drivers
│   │   │   ├── network/                    # Retrofit & OsrmRoutingService
│   │   │   ├── service/                    # MeshForegroundService (Type: Location)
│   │   │   └── ui/components/              # NearbyHazardsRadarCard, SafeRoutePreviewMap
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

## 10. Setup & Deployment Guide

### 10.1 Backend Setup (AWS ECS / Local)
```bash
cd gridZeroExpress/backend
npm install
```
Configure `.env`:
```env
PORT=5000
NODE_ENV=development
MONGODB_URI=mongodb+srv://<user>:<pwd>@cluster.mongodb.net/zerogrid?retryWrites=true&w=majority
JWT_SECRET=your_super_secret_jwt_key_at_least_32_chars_long
ALLOWED_ORIGINS=http://localhost:3000
AWS_REGION=ap-south-1
```
Start server:
```bash
npm run dev
# Health check: http://localhost:5000/health
# WebSocket: ws://localhost:5000/sos
```

### 10.2 Municipal Dashboard (AWS Amplify / Local)
```bash
cd gridZeroExpress/frontend
npm install
```
Configure `.env.local`:
```env
NEXT_PUBLIC_API_URL=http://localhost:5000
NEXT_PUBLIC_GOOGLE_MAPS_API_KEY=AIzaSy...
```
Start dashboard:
```bash
npm run dev
# Dashboard live at: http://localhost:3000
```

### 10.3 Android Client Setup (`gridzero`)
1. Open `gridzero` in Android Studio.
2. In root directory, update `local.properties`:
   ```properties
   sdk.dir=C:\\Users\\<USER>\\AppData\\Local\\Android\\Sdk
   MAPS_API_KEY=AIzaSy...
   ```
3. Ensure `google-services.json` is located in `app/`.
4. Run on a physical Android device (API 26+ / Android 8.0+) to support BLE peripheral advertising.

---

## 11. Troubleshooting & Hardware Optimization Notes

| Observed Behavior | Root Cause | Engineering Solution |
|---|---|---|
| **Samsung FLP listener rejection (`10416_FINE_fg_svc_false_foreground`)** | Android 14+ requires foreground services accessing location to specify `foregroundServiceType="location"`. | Declared `FOREGROUND_SERVICE_LOCATION` in manifest and passed `FOREGROUND_SERVICE_TYPE_LOCATION` in `MeshForegroundService.startForeground()`. |
| **System overlay keyboard blocked** | `WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE` prevented IME keyboard input in the overlay. | Removed `FLAG_NOT_FOCUSABLE` and configured `softInputMode = SOFT_INPUT_ADJUST_RESIZE` in `OverlayAlertManager.kt`. |
| **Background location freeze on Samsung devices** | Aggressive vendor battery management suspends fused location listeners. | Implemented multi-provider polling (`PASSIVE_PROVIDER`, `FUSED_PROVIDER`, `getCurrentLocation()`) in `LocationHelper.kt`. |
| **BLE advertising failure (`ADVERTISE_FAILED_FEATURE_UNSUPPORTED`)** | Android Emulator lacks hardware BLE peripheral mode. | Run on physical hardware for full P2P mesh testing. |
| **High Logcat frame invalidation (`gralloc4`)** | Infinite layout animations in top bars. | Replaced infinite transitions with static indicator chips. |

---

<p align="center">
  <b>Built with ❤️ for Bharat Builds Hackathon 2026</b><br>
  <i>Empowering Indian Cities Against Climate Disasters Through Decentralized Technology</i>
</p>
