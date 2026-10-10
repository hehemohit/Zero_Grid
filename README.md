# ZeroGrid

### Autonomous Emergency Response & Off-Grid Mesh Platform

[![Bharat Builds](https://img.shields.io/badge/Bharat%20Builds-Environment%20Track%202026-orange?style=flat&logo=target&logoColor=white)](https://github.com/hehemohit/Zero_Grid)
[![Android](https://img.shields.io/badge/Android-Kotlin%20%7C%20Compose-3DDC84?style=flat&logo=android&logoColor=white)](https://github.com/hehemohit/Zero_Grid)
[![Mesh](https://img.shields.io/badge/Mesh-BLE%20%26%20Wi--Fi%20Direct-0078D7?style=flat&logo=bluetooth&logoColor=white)](https://github.com/hehemohit/Zero_Grid)
[![AWS](https://img.shields.io/badge/AWS-ECS%20Fargate-FF9900?style=flat&logo=amazon-aws&logoColor=white)](https://github.com/hehemohit/Zero_Grid)
[![Amazon Bedrock](https://img.shields.io/badge/Amazon-Bedrock-7B2CBF?style=flat&logo=amazonaws&logoColor=white)](https://github.com/hehemohit/Zero_Grid)
[![Strands Agents](https://img.shields.io/badge/Strands-Agents%20SDK-FF4F00?style=flat&logo=openai&logoColor=white)](https://github.com/hehemohit/Zero_Grid)
[![AWS Maps](https://img.shields.io/badge/AWS%20Maps-Amazon%20Location%20Service-232F3E?style=flat&logo=amazonaws&logoColor=white)](https://aws.amazon.com/location/)
[![DynamoDB](https://img.shields.io/badge/DynamoDB-33kV%20SCADA%20Topology-4053D6?style=flat&logo=amazondynamodb&logoColor=white)](https://aws.amazon.com/dynamodb/)
[![MongoDB](https://img.shields.io/badge/MongoDB-Atlas%202dsphere-47A248?style=flat&logo=mongodb&logoColor=white)](https://github.com/hehemohit/Zero_Grid)
[![ElastiCache](https://img.shields.io/badge/ElastiCache-Redis%20Redlock-DC382D?style=flat&logo=redis&logoColor=white)](https://github.com/hehemohit/Zero_Grid)
[![Next.js](https://img.shields.io/badge/Next.js-16%20Amplify-black?style=flat&logo=nextdotjs&logoColor=white)](https://github.com/hehemohit/Zero_Grid)

> Zero-infrastructure emergency coordination for catastrophic monsoon floods and power grid blackouts — saving lives when mobile networks collapse.

ZeroGrid is a **hardware-first, agentic crisis response system**: Native Android P2P Radio Mesh + Dynamic Flood Route Copilot + Autonomous Multi-Agent Engine (**AgentZero** powered by **Strands Agents SDK** and **Amazon Bedrock**) — built to **ship on AWS**.

**Architecture Blueprint**: [aws_architecture_final.jpg](file:///C:/Users/ACER/.gemini/antigravity-ide/brain/230e08d2-b887-4e64-9883-849acbcc94b7/aws_architecture_strands_1791639732883.jpg) · **Mobile Core**: [mobile application working core.md](file:///c:/Users/ACER/AndroidStudioProjects/gridzero/mobile%20application%20working%20core.md) · **Cloud Infrastructure**: [WebCloudFunction.md](file:///c:/Users/ACER/AndroidStudioProjects/gridzero/WebCloudFunction.md) · **Packet Wire Specs**: [packet_structure_diagrams.md](file:///c:/Users/ACER/AndroidStudioProjects/gridzero/gridZeroExpress/packet_structure_diagrams.md) · **RSSI Radar Tracking**: [rssiTracking.md](file:///c:/Users/ACER/AndroidStudioProjects/gridzero/rssiTracking.md)

---

## 🌊 Bharat Builds Hackathon Problem Statement & Impact

> [!IMPORTANT]
> **Bharat Builds Hackathon — Environment Track: Heat & Water (Monsoon Floods & Grid Blackout)**  
> Enables resilient life-safety communication across submerged territories when power infrastructure and cellular telecommunications are incapacitated.

During catastrophic monsoons and urban flood disasters (e.g., Mumbai, Chennai, Kerala floods):
1. **Cellular Towers Submerge & Power Grids Fail**: Mobile networks collapse within 45 minutes of heavy inundation, cutting off trapped citizens from emergency services.
2. **Submerged Electrical Transformers Cause Electrocution**: Uncoordinated rescue attempts lead to tragic drowning and high-voltage line-contact fatalities.
3. **Emergency Helplines Experience Severe Overload**: 112/NDRF call centers face thousands of panicked, duplicate, or unverified distress calls without actionable GPS coordinates.
4. **Impassable Roads Trap Rescue Boats**: First responders waste critical golden-hour time driving into submerged culverts and drowned underpasses.

**ZeroGrid solves this end-to-end**:
* **At the Edge (Mobile)**: Citizens send SOS beacons without cell towers or internet via a multi-hop BLE & Wi-Fi Direct peer mesh network, navigating safely via an offline **Safe Route Copilot** that routes around flood zones.
* **In the Cloud (AWS)**: Incoming alerts are validated by an autonomous **4-Phase Multi-Agent Engine (AgentZero)**, isolating flooded 33kV electrical plinths before rescue boats arrive, matching nearest relief personnel via **MongoDB 2dsphere**, and locking squads via **Amazon ElastiCache Redis**.

---

## 📑 Table of Contents

1. [Full Technology Stack](#1-full-technology-stack)
2. [Exhaustive Monorepo Directory Structure](#2-exhaustive-monorepo-directory-structure)
3. [PART I: Mobile Application Working Core (`app/`)](#3-part-i-mobile-application-working-core-app)
   - 3.1 [Dual-Path SOS Dispatch Orchestration](#31-dual-path-sos-dispatch-orchestration)
   - 3.2 [Safe Route Copilot, AWS Maps SDK & Dynamic Flood Hazard Radar](#32-safe-route-copilot-aws-maps-sdk--dynamic-flood-hazard-radar)
   - 3.3 [P2P Off-Grid Mesh Protocol & Hardware Drivers](#33-p2p-off-grid-mesh-protocol--hardware-drivers)
   - 3.4 [32-Bit Word-Aligned Binary RF Packet Specification](#34-32-bit-word-aligned-binary-rf-packet-specification)
   - 3.5 [TTL Hop Control & 500-Slot LRU Deduplication](#35-ttl-hop-control--500-slot-lru-deduplication)
   - 3.6 [Tactical Compass Radar vs. Google Maps Mode](#36-tactical-compass-radar-vs-google-maps-mode)
   - 3.7 [Navigation Stack & WhatsApp-Style Transitions](#37-navigation-stack--whatsapp-style-transitions)
   - 3.8 [Device Resilience & WorkManager Offline Sync](#38-device-resilience--workmanager-offline-sync)
4. [PART II: AWS Cloud Infrastructure & Multi-Agent Backend](#4-part-ii-aws-cloud-infrastructure--multi-agent-backend)
   - 4.1 [Official AWS Architecture Blueprint](#41-official-aws-architecture-blueprint)
   - 4.2 [Ingress & Security: API Gateway, Cognito & Secrets Manager](#42-ingress--security-api-gateway-cognito--secrets-manager)
   - 4.3 [Core Backend: AWS ECS Fargate (AgentZero powered by Strands Agents SDK)](#43-core-backend-aws-ecs-fargate-agentzero-powered-by-strands-agents-sdk)
   - 4.4 [Serverless Compute: Lambda A1 Voice Hook & Lambda GenAI](#44-serverless-compute-lambda-a1-voice-hook--lambda-genai)
   - 4.5 [AI Intelligence: Amazon Bedrock Two-Way Reasoning](#45-ai-intelligence-amazon-bedrock-two-way-reasoning)
   - 4.6 [Data Persistence: MongoDB Atlas (2dsphere), DynamoDB & ElastiCache](#46-data-persistence-mongodb-atlas-2dsphere-dynamodb--elasticache)
   - 4.7 [Autonomous 4-Phase Circular Multi-Agent Pipeline](#47-autonomous-4-phase-circular-multi-agent-pipeline)
   - 4.8 [Electrical Grid Safety: 33kV Topology & Hospital ICU Lifeline](#48-electrical-grid-safety-33kV-topology--hospital-icu-lifeline)
   - 4.9 [160-Admin Workforce Allocation Matrix](#49-160-admin-workforce-allocation-matrix)
   - 4.10 [AWS Maps SDK & Amazon Location Service Safe Detour Engine](#410-aws-maps-sdk--amazon-location-service-safe-detour-engine)
5. [PART III: Web Operations Command Center (Amplify / Next.js 16)](#5-part-iii-web-operations-command-center-amplify--nextjs-16)
6. [End-to-End Emergency Incident Lifecycle (Sequence Flow)](#6-end-to-end-emergency-incident-lifecycle-sequence-flow)
7. [Quickstart & Local Multi-Container Development](#7-quickstart--local-multi-container-development)
8. [Security, Governance & Resilience Matrix](#8-security-governance--resilience-matrix)

---

## 1. Full Technology Stack

### 📱 Mobile Client (Native Android)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9+-7F52FF?style=flat&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=flat&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Android](https://img.shields.io/badge/Target%20SDK-35%20(Android%208.0%2B)-3DDC84?style=flat&logo=android&logoColor=white)](https://developer.android.com/)
[![BLE](https://img.shields.io/badge/Radio-BLE%205.0%20Peripheral%20%26%20Central-0078D7?style=flat&logo=bluetooth&logoColor=white)](https://developer.android.com/guide/topics/connectivity/bluetooth/ble-overview)
[![Wi-Fi Direct](https://img.shields.io/badge/Wi--Fi-Direct%20P2P%20Sockets-1DA1F2?style=flat&logo=wi-fi&logoColor=white)](https://developer.android.com/guide/topics/connectivity/wifip2p)
[![Google Maps](https://img.shields.io/badge/Maps-Google%20Maps%20Compose%204.4-4285F4?style=flat&logo=googlemaps&logoColor=white)](https://developers.google.com/maps)
[![WorkManager](https://img.shields.io/badge/Offline-WorkManager%202.9-34A853?style=flat&logo=android&logoColor=white)](https://developer.android.com/topic/libraries/architecture/workmanager)
[![Socket.IO](https://img.shields.io/badge/Realtime-Socket.IO%20Java%202.1-010101?style=flat&logo=socketdotio&logoColor=white)](https://socket.io/)

* **Core Runtime**: Kotlin 1.9+, Coroutines & StateFlow / SharedFlow, AndroidX Lifecycle.
* **Network & Ingress**: Retrofit 2.11 + OkHttp 4.12 (HTTP/2, TLS 1.3), Socket.IO Java Client 2.1.0 (`/sos` namespace).
* **Hardware Drivers**: BLE Advertising (Peripheral Mode) & Scanning (Central GATT), Android `WifiP2pManager` Direct Groups, `SensorManager` Magnetometer Canvas Compass.

### ⚙️ Core Backend Container (AWS ECS Fargate — `gridZeroExpress`)
[![Node.js](https://img.shields.io/badge/Node.js-20%20LTS-339933?style=flat&logo=nodedotjs&logoColor=white)](https://nodejs.org/)
[![Express](https://img.shields.io/badge/Express-5.2.1-000000?style=flat&logo=express&logoColor=white)](https://expressjs.com/)
[![Strands Agents](https://img.shields.io/badge/Agents-AWS%20Strands%20SDK%201.19-FF4F00?style=flat&logo=openai&logoColor=white)](https://strandsagents.com/)
[![MCP](https://img.shields.io/badge/Protocol-MCP%20SDK%201.32-6C5CE7?style=flat)](https://modelcontextprotocol.io/)
[![MongoDB](https://img.shields.io/badge/Database-MongoDB%20Atlas%202dsphere-47A248?style=flat&logo=mongodb&logoColor=white)](https://mongodb.com/)
[![Redis](https://img.shields.io/badge/Mutex-ioredis%206.0%20Redlock-DC382D?style=flat&logo=redis&logoColor=white)](https://redis.io/)
[![Python](https://img.shields.io/badge/Voice%20Hook-Python%203.11%20FastAPI-3776AB?style=flat&logo=python&logoColor=white)](https://fastapi.tiangolo.com/)
[![Prometheus](https://img.shields.io/badge/Telemetry-Prometheus%20Client%2015.1-E6522C?style=flat&logo=prometheus&logoColor=white)](https://prometheus.io/)

* **Agentic Loop**: AWS Strands Agents SDK (`@strands-agents/sdk` 1.19) driving **AgentZero** reasoning, tool dispatch, and crisis triage.
* **Geospatial & Storage**: Mongoose 9.10 + MongoDB Node Driver 7.6 (`2dsphere` geospatial indexing), Distributed Redlock Mutex (`ioredis`).
* **Microservices**: Python 3.11 + FastAPI + Mangum Serverless Adapter for voice intent classification.

### ☁️ AWS Cloud Ingress & Serverless Infrastructure
[![AWS ECS](https://img.shields.io/badge/Compute-AWS%20ECS%20Fargate-FF9900?style=flat&logo=amazonecs&logoColor=white)](https://aws.amazon.com/ecs/)
[![API Gateway](https://img.shields.io/badge/Ingress-Amazon%20API%20Gateway-FF4F8B?style=flat&logo=amazonapigateway&logoColor=white)](https://aws.amazon.com/api-gateway/)
[![Cognito](https://img.shields.io/badge/Auth-Amazon%20Cognito%20JWT-DD344C?style=flat&logo=amazoncognito&logoColor=white)](https://aws.amazon.com/cognito/)
[![Secrets Manager](https://img.shields.io/badge/Security-AWS%20Secrets%20Manager-DD344C?style=flat&logo=amazonaws&logoColor=white)](https://aws.amazon.com/secrets-manager/)
[![AWS Lambda](https://img.shields.io/badge/Serverless-AWS%20Lambda%20A1%20%26%20GenAI-FF9900?style=flat&logo=awslambda&logoColor=white)](https://aws.amazon.com/lambda/)
[![Amazon Bedrock](https://img.shields.io/badge/AI-Amazon%20Bedrock%20Claude%203.5-7B2CBF?style=flat&logo=amazonaws&logoColor=white)](https://aws.amazon.com/bedrock/)
[![DynamoDB](https://img.shields.io/badge/Grid%20Topology-Amazon%20DynamoDB-4053D6?style=flat&logo=amazondynamodb&logoColor=white)](https://aws.amazon.com/dynamodb/)
[![ElastiCache](https://img.shields.io/badge/Cache-Amazon%20ElastiCache%20Redis-C925D1?style=flat&logo=redis&logoColor=white)](https://aws.amazon.com/elasticache/)
[![Amplify](https://img.shields.io/badge/Hosting-AWS%20Amplify%20Edge%20CDN-FF9900?style=flat&logo=awsamplify&logoColor=white)](https://aws.amazon.com/amplify/)
[![Location Service](https://img.shields.io/badge/Routing-Amazon%20Location%20Service%20v2-232F3E?style=flat&logo=amazonaws&logoColor=white)](https://aws.amazon.com/location/)

### 💻 Web Operations Command Center (AWS Amplify)
[![Next.js](https://img.shields.io/badge/Framework-Next.js%2016.3.5%20App%20Router-black?style=flat&logo=nextdotjs&logoColor=white)](https://nextjs.org/)
[![React](https://img.shields.io/badge/UI-React%2019.2-61DAFB?style=flat&logo=react&logoColor=black)](https://react.dev/)
[![Tailwind CSS](https://img.shields.io/badge/Styling-Tailwind%20CSS%20v4-38B2AC?style=flat&logo=tailwindcss&logoColor=white)](https://tailwindcss.com/)
[![MapLibre](https://img.shields.io/badge/Mapping-MapLibre%20GL%204.7-396B9E?style=flat&logo=maplibre&logoColor=white)](https://maplibre.org/)
[![Lucide](https://img.shields.io/badge/Icons-Lucide%20React-F56565?style=flat&logo=feather&logoColor=white)](https://lucide.dev/)

```
┌──────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                     ZERO-GRID TECH STACK MATRIX                                  │
├───────────────────┬──────────────────────────────────────────────────────────────────────────────┤
│ Mobile Client     │ • Kotlin 1.9+ | Jetpack Compose (Material 3) | Target SDK 35 (Android 8.0+)  │
│ (Native Android)  │ • Coroutines & Kotlin StateFlow / SharedFlow                                 │
│                   │ • Retrofit 2.11 + OkHttp 4.12 (HTTP/2, TLS 1.3, Interceptors)                │
│                   │ • Socket.IO Java Client 2.1.0 (WSS /sos Namespace)                           │
│                   │ • AndroidX WorkManager 2.9 (Exponential Backoff Offline Sync)                │
│                   │ • Google Maps Compose 4.4.1 + Play Services Location (FusedProvider)        │
│                   │ • Hardware BLE Advertising (Peripheral) & Scanning (Central GATT)            │
│                   │ • Android WifiP2pManager (Wi-Fi Direct High-Bandwidth Socket Streamer)       │
│                   │ • Hardware SensorManager (Magnetometer Compass Canvas Radar)                 │
├───────────────────┼──────────────────────────────────────────────────────────────────────────────┤
│ Cloud Backend     │ • Node.js 20 LTS | Express 5.2.1 | Socket.IO 4.8                             │
│ (Core Container)  │ • AWS Strands Agents SDK (@strands-agents/sdk 1.19) Driving AgentZero        │
│                   │ • Model Context Protocol SDK (@modelcontextprotocol/sdk 1.32)                │
│                   │ • Mongoose 9.10 + MongoDB Node Driver 7.6 (2dsphere Geospatial Indexes)      │
│                   │ • Redis Client (ioredis 6.0) for Distributed Redlock & Pub/Sub               │
│                   │ • Python 3.11 + FastAPI + Mangum Serverless Adapter (Voice Microservice)     │
│                   │ • Prometheus Client (prom-client 15.1) for SLA & Queue Telemetry             │
├───────────────────┼──────────────────────────────────────────────────────────────────────────────┤
│ Cloud Ingress &   │ • AWS ECS Fargate (Containerized Backend & AgentZero Core Runtime)           │
│ Infrastructure    │ • Amazon API Gateway (REST HTTP APIs & WebSocket Gateway)                    │
│                   │ • Amazon Cognito User Pools (JWT Signature Validation & RBAC)                │
│                   │ • AWS Secrets Manager (Dynamic Ingestion of Credentials & Secrets)           │
│                   │ • AWS Lambda ([Lambda A1] Voice Agent Hook & [AWS Lambda GenAI] Batch Triage)│
│                   │ • Amazon Bedrock (Anthropic Claude 3.5 Sonnet & Claude 3 Haiku)              │
│                   │ • Amazon DynamoDB (Single-Table Grid Topology & Finished Case Logs)          │
│                   │ • Amazon ElastiCache for Redis (Sub-millisecond Mutex Locks & Spatial Cache) │
│                   │ • MongoDB Atlas (Active GeoJSON Incidents & Rescuer 2dsphere Searches)       │
│                   │ • AWS Amplify (Next.js 16 Web Command Center Hosting & Edge CDN)             │
│                   │ • Amazon Location Service v2 (Dynamic Detour Vector Tiles & Hazards)         │
├───────────────────┼──────────────────────────────────────────────────────────────────────────────┤
│ Web Command       │ • Next.js 16.3.5 (App Router, Server Actions, React 19.2)                    │
│ Center (Amplify)  │ • Tailwind CSS v4 | Lucide React Icons | MapLibre GL 4.7                     │
│                   │ • Socket.IO Client 4.8 | Web Audio API Microphone Visualizer                 │
└───────────────────┴──────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Exhaustive Monorepo Directory Structure

```text
gridzero/ (Repository Workspace Root)
│
├── app/                                    # NATIVE ANDROID CLIENT (Kotlin / Jetpack Compose)
│   ├── src/main/
│   │   ├── AndroidManifest.xml             # Permissions: BLE, Location, Wi-Fi P2P, Foregrounds
│   │   ├── java/com/example/zerogrid/
│   │   │   ├── MainActivity.kt             # Single-Activity entrypoint & custom back-stack handler
│   │   │   ├── ZeroGridApplication.kt     # App lifecycle, notification channels & singletons
│   │   │   │
│   │   │   ├── admin/                      # Mobile Admin Command Center (Disaster Dispatcher)
│   │   │   │   ├── AdminPanelScreen.kt     # Tabbed Admin Interface (Live Radar, History, Users)
│   │   │   │   ├── data/                   # AdminRetrofitApi, AdminSosRepository, AdminSocketManager
│   │   │   │   └── ui/                     # AdminGoogleMapView, TacticalRadarCanvas, SosDetailBottomSheet
│   │   │   │
│   │   │   ├── auth/                       # Identity & Cloud Authentication
│   │   │   │   ├── LoginScreen.kt          # Email/Password & Google Sign-In
│   │   │   │   ├── RegisterScreen.kt       # Citizen registration form
│   │   │   │   └── CompleteProfileScreen.kt# Emergency contacts & medical identifiers
│   │   │   │
│   │   │   ├── contacts/                   # Emergency Contacts Subsystem
│   │   │   │   ├── EmergencyContactsScreen.kt # Ice Contact Management UI
│   │   │   │   └── ContactsViewModel.kt    # Contact sync & Room database bindings
│   │   │   │
│   │   │   ├── debug/                      # Diagnostic Consoles & Telemetry
│   │   │   │   ├── DebugConsoleScreen.kt   # Live packet transmission graphs & RF log viewer
│   │   │   │   └── LogBuffer.kt            # Thread-safe in-memory circular diagnostic buffer
│   │   │   │
│   │   │   ├── emergency/                  # Core SOS, Route Copilot & Dynamic Safety Agents
│   │   │   │   ├── SendSosScreen.kt        # 1-Tap Emergency Trigger (Category, Battery, Notes)
│   │   │   │   ├── SosCenterScreen.kt      # Real-time nearby distress radar & safety confirm
│   │   │   │   ├── TrackSosScreen.kt       # Live Incident Tracker with Rescuer ETA & Telemetry
│   │   │   │   ├── SafeRouteCopilotScreen.kt# Turn-by-turn flood evacuation HUD
│   │   │   │   ├── SafeRoutePlannerDialog.kt# Origin/destination detour planner
│   │   │   │   ├── RouteCopilotViewModel.kt# State holder for Strands Agent safe routes
│   │   │   │   ├── RouteSafetyAgent.kt     # Client-side hazard boundary & proximity evaluator
│   │   │   │   ├── HazardOverlayView.kt    # Canvas rendering of active waterlogging polygons
│   │   │   │   ├── OverlayAlertManager.kt  # System alert banners for electrical breaker trips
│   │   │   │   ├── UnifiedSosDispatcher.kt # Dual-Dispatch Engine (Parallel Mesh + Cloud Uplink)
│   │   │   │   └── SosUploadWorker.kt      # WorkManager worker for offline ticket synchronization
│   │   │   │
│   │   │   ├── family/                     # Family Geofencing & Location Monitoring
│   │   │   │   ├── FamilyLinksScreen.kt    # Dependent child/elder tracking interface
│   │   │   │   └── FamilyViewModel.kt      # Real-time encrypted GPS coordinate fetcher
│   │   │   │
│   │   │   ├── fcm/                        # Push Notification Subsystem
│   │   │   │   └── ZeroGridFcmService.kt   # High-priority alert notification builder
│   │   │   │
│   │   │   ├── hardware/                   # System Hardware Radio Observer
│   │   │   │   ├── HardwareStateManager.kt # Bluetooth, Wi-Fi P2P & Location state tracker
│   │   │   │   └── HardwareRequirementBanner.kt # Dynamic warning chip for disabled radios
│   │   │   │
│   │   │   ├── home/                       # Citizen Home Interface
│   │   │   │   ├── MeshDashboardScreen.kt  # Active peer density badge & nearby hazard summary
│   │   │   │   └── HomeViewModel.kt        # State aggregator for local mesh radio status
│   │   │   │
│   │   │   ├── location/                   # Geospatial Helpers
│   │   │   │   ├── LocationHelper.kt       # FusedLocationProviderClient GPS tagger
│   │   │   │   └── LocationSearchHelper.kt # Offline landmark resolver & geocoding helper
│   │   │   │
│   │   │   ├── mesh/                       # Autonomous Radio Mesh Engine
│   │   │   │   ├── engine/
│   │   │   │   │   ├── MeshEngine.kt       # Central mesh singleton, PeerTable & event flows
│   │   │   │   │   ├── MeshRoutingEngine.kt# Distance-vector multi-hop routing logic
│   │   │   │   │   ├── DeduplicationCache.kt# 500-slot LRU cache preventing broadcast storms
│   │   │   │   │   ├── MeshPacket.kt       # Immutable packet data class
│   │   │   │   │   └── PacketType.kt       # Enumeration of packet classifications
│   │   │   │   └── transport/
│   │   │   │       ├── BleMeshDriver.kt    # BLE peripheral advertiser & central GATT scanner
│   │   │   │       └── WifiDirectMeshDriver.kt # Android WifiP2pManager socket streamer
│   │   │   │
│   │   │   ├── messaging/                  # Mesh Chat Subsystem
│   │   │   │   ├── MessagesScreen.kt       # Conversations inbox (Public & Direct)
│   │   │   │   ├── PeerDirectChatScreen.kt # 1:1 direct end-to-end peer messaging
│   │   │   │   └── ChannelChatScreen.kt    # Regional public emergency broadcast channel
│   │   │   │
│   │   │   ├── navigation/                 # Navigation Architecture
│   │   │   │   ├── NavGraph.kt             # HorizontalPager root & animated sub-screen stack
│   │   │   │   └── Routes.kt               # Type-safe navigation destination enum
│   │   │   │
│   │   │   ├── network/                    # Cloud Networking Layer
│   │   │   │   ├── ApiConstants.kt         # Base CloudFront/API Gateway endpoint constants
│   │   │   │   ├── RetrofitInstance.kt     # OkHttp client with JWT bearer interceptors
│   │   │   │   ├── SosApiService.kt        # Retrofit interface for SOS & Detour routes
│   │   │   │   └── SocketManager.kt        # Socket.IO Java Client (/sos namespace)
│   │   │   │
│   │   │   ├── onboarding/                 # Initial Setup Flow
│   │   │   │   ├── SplashScreen.kt         # Token & identity validator
│   │   │   │   ├── PermissionsScreen.kt    # Runtime permission dialogs (BLE, Location)
│   │   │   │   └── CreateIdentityScreen.kt # Local offline nickname & crypto node ID setup
│   │   │   │
│   │   │   ├── profile/                    # User Medical Profile
│   │   │   │   └── ProfileScreen.kt        # Blood type, allergies & emergency notes editor
│   │   │   │
│   │   │   ├── service/                    # Long-Running Android Services
│   │   │   │   └── MeshForegroundService.kt# Persistent background notification service
│   │   │   │
│   │   │   ├── settings/                   # User Settings & Admin Access Gate
│   │   │   │   └── SettingsScreen.kt       # Radio toggles, cache purger & Admin Panel gate
│   │   │   │
│   │   │   ├── ui/                         # Design System & Primitives
│   │   │   │   ├── theme/                  # Material 3 colors, typography, shapes & AMOLED dark
│   │   │   │   └── components/             # NearbyHazardsRadarCard, Glassmorphic cards
│   │   │   │
│   │   │   └── util/                       # Helpers
│   │   │       └── PhoneValidator.kt       # Phone number sanitization utility
│   │   │
│   │   └── res/                            # Drawable icons, strings & color tokens
│   └── build.gradle.kts                    # Android build script (SDK 35, Jetpack Compose)
│
├── gridZeroExpress/ (webapplication core)  # WEB APPLICATION CORE & CLOUD BACKEND STACK
│   │
│   ├── backend/                            # Core Transactional Backend on AWS ECS Fargate
│   │   ├── src/
│   │   │   ├── server.js                   # Express app, Socket.IO /sos namespace, Mongo init
│   │   │   ├── controllers/
│   │   │   │   ├── sosController.js        # SOS creation, Detour routes, Rescuer matching
│   │   │   │   ├── authController.js       # Register, login, Google OAuth verification
│   │   │   │   ├── adminController.js      # Incident history, 160-Admin directory queries
│   │   │   │   ├── userController.js       # User profile, complete-profile, FCM tokens
│   │   │   │   ├── contactController.js    # Emergency contacts CRUD
│   │   │   │   └── familyController.js     # Parent-child tracking links
│   │   │   ├── models/
│   │   │   │   ├── SosEvent.js             # Incident schema with GeoJSON 2dsphere index
│   │   │   │   ├── User.js                 # 160 Admins roster & citizen profile schema
│   │   │   │   ├── Contact.js              # Emergency contact document model
│   │   │   │   └── ParentChildLink.js      # Family link associations
│   │   │   ├── routes/
│   │   │   │   ├── sosRoutes.js            # /api/sos endpoints
│   │   │   │   ├── routeRoutes.js          # /api/routes/detour (Strands Agent integration)
│   │   │   │   ├── authRoutes.js           # /api/auth
│   │   │   │   ├── adminRoutes.js          # /api/admin
│   │   │   │   ├── userRoutes.js           # /api/users
│   │   │   │   ├── contactRoutes.js        # /api/contacts
│   │   │   │   └── familyRoutes.js         # /api/family
│   │   │   ├── middleware/
│   │   │   │   ├── verifyToken.js          # JWT Bearer token validator
│   │   │   │   └── verifyAdminRole.js      # Fresh DB check for ADMIN role & approval
│   │   │   └── utils/
│   │   │       ├── agentZeroWebhook.js     # Bridge to Strands Agents SDK & Lambda
│   │   │       ├── metrics.js              # Prometheus metrics collector (/metrics)
│   │   │       ├── tideService.js          # Coastal tide forecast API fetcher
│   │   │       ├── weatherService.js       # Real-time precipitation & wind data
│   │   │       ├── fcm.js                  # Firebase Cloud Messaging push dispatcher
│   │   │       └── jwt.js                  # JWT token generator
│   │   ├── .platform/                      # AWS Elastic Beanstalk / ECS Nginx proxy config
│   │   │   └── nginx/conf.d/websocket.conf # Nginx WebSocket upgrade reverse proxy
│   │   ├── Dockerfile                      # Production Docker container definition
│   │   └── package.json                    # Backend dependencies (Express, Strands SDK, etc.)
│   │
│   ├── frontend/                           # Command Center Web Dashboard on AWS Amplify
│   │   ├── public/maplibre-gl-worker.mjs   # Static MapLibre Web Worker
│   │   ├── src/
│   │   │   ├── app/                        # Next.js 16 App Router
│   │   │   │   ├── layout.tsx              # Root HTML & theme providers
│   │   │   │   ├── (app)/dashboard/page.tsx# Disaster Command Center (KPIs & Fleet Telemetry)
│   │   │   │   ├── (app)/admin/page.tsx    # Master Incident Map & Operations Console
│   │   │   │   ├── (app)/prediction/page.tsx# 24-Hour Multi-Domain Chaos Prediction
│   │   │   │   └── (app)/flow/page.tsx     # Autonomous Multi-Agent Interactive Test Bench
│   │   │   ├── components/
│   │   │   │   ├── admin/
│   │   │   │   │   ├── SosLiveMap.tsx      # Amazon Location Service v2 / MapLibre Live Layer
│   │   │   │   │   ├── SosDrawer.tsx       # Dispatcher assignment & status update drawer
│   │   │   │   │   └── UserManagementModal.tsx # 160-Admin workforce management
│   │   │   │   └── voice/
│   │   │   │       └── VoiceAssistant.tsx  # Hands-free emergency voice dispatcher
│   │   │   └── lib/
│   │   │       └── api.ts                  # Axios client with interceptors
│   │   └── package.json                    # Frontend dependencies (Next.js, MapLibre, Tailwind)
│   │
│   ├── voice-agent/                        # [Lambda A1] Voice Microservice on AWS Lambda
│   │   ├── main.py                         # FastAPI + Mangum Serverless Adapter (Bedrock Haiku)
│   │   ├── grid_graph.py                   # Single-Table DynamoDB Adjacency Graph Engine
│   │   ├── seed_dynamodb.py                # DynamoDB Electrical Topology Seeding Utility
│   │   └── requirements.txt                # Python dependencies
│   │
│   └── postman/                            # Automated Postman test suites
│
├── mobile application working core.md      # Comprehensive Android architecture deep-dive
├── packet_structure_diagrams.md            # Byte-level RF frame and packet specifications
├── WebCloudFunction.md                     # Circular multi-agent & electrical grid reference
└── docker-compose.yml                      # Local development multi-container orchestration
```

---

## 3. PART I: Mobile Application Working Core (`app/`)

[![Android](https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026--35)-3DDC84?style=flat&logo=android&logoColor=white)](https://github.com/hehemohit/Zero_Grid)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin%201.9-7F52FF?style=flat&logo=kotlin&logoColor=white)](https://github.com/hehemohit/Zero_Grid)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20(Material%203)-4285F4?style=flat&logo=jetpackcompose&logoColor=white)](https://github.com/hehemohit/Zero_Grid)
[![BLE Mesh](https://img.shields.io/badge/Mesh-Bluetooth%205.0%20BLE-0078D7?style=flat&logo=bluetooth&logoColor=white)](https://github.com/hehemohit/Zero_Grid)
[![Wi-Fi Direct](https://img.shields.io/badge/Mesh-Wi--Fi%20Direct%20P2P-1DA1F2?style=flat&logo=wi-fi&logoColor=white)](https://github.com/hehemohit/Zero_Grid)

> 📘 **Detailed Architectural Deep-Dive**: For raw implementation code, see [mobile application working core.md](file:///c:/Users/ACER/AndroidStudioProjects/gridzero/mobile%20application%20working%20core.md).

Built entirely in **Kotlin** and **Jetpack Compose (Material 3)**, targeting Android 8.0+ (API 26 to 35).

---

### 3.1 Dual-Path SOS Dispatch Orchestration

When a citizen in a flooded area taps **"Send SOS"**, [UnifiedSosDispatcher.kt](file:///c:/Users/ACER/AndroidStudioProjects/gridzero/app/src/main/java/com/example/zerogrid/emergency/UnifiedSosDispatcher.kt) executes a parallel dispatch strategy:

```mermaid
sequenceDiagram
    autonumber
    actor Citizen as Citizen (SendSosScreen)
    participant Dispatcher as UnifiedSosDispatcher
    participant Mesh as MeshEngine
    participant BLE as BleMeshDriver
    participant Wifi as WifiDirectMeshDriver
    participant Net as ConnectivityChecker
    participant Retrofit as Retrofit (SosApiService)
    participant Worker as WorkManager (SosUploadWorker)
    participant Cloud as AWS Cloud (API Gateway -> ECS)

    Citizen->>Dispatcher: dispatchSos(category, message, lat, lng, battery)
    
    par Path A: Immediate Offline Radio Mesh (Zero Internet)
        Dispatcher->>Mesh: sendPacket(type = SOS_BEACON)
        Mesh->>BLE: Start High-Frequency BLE Advertising & GATT Burst
        Mesh->>Wifi: Broadcast to Connected Wi-Fi Direct Group
        BLE-->>Citizen: UI Feedback: "Transmitted via Local Mesh (Hop 0)"
    and Path B: Opportunistic Cloud Uplink
        Dispatcher->>Net: isInternetAvailable()
        alt Cellular / Wi-Fi Active
            Dispatcher->>Retrofit: POST /api/sos (Payload + Cognito JWT)
            Retrofit->>Cloud: Amazon API Gateway -> ECS Fargate (AgentZero)
            Cloud-->>Dispatcher: HTTP 201 Created (Incident ID assigned)
            Dispatcher-->>Citizen: "Incident Registered with AWS Cloud HQ"
        else Offline / Disconnected
            Dispatcher->>Worker: Enqueue OneTimeWorkRequest(SosUploadWorker)
            Worker-->>Citizen: "Offline: Queued for Cloud Upload upon Reconnection"
        end
    end
```

---

### 3.2 Safe Route Copilot, AWS Maps SDK & Dynamic Flood Hazard Radar

In flash-flooded corridors, standard commercial GPS routing directs civilians and rescue boats directly into drowned railway underpasses and electrified transformer plinths. ZeroGrid integrates the **AWS Maps SDK (Amazon Location Service)** and an on-device **Dynamic Flood Hazard Radar**:

```mermaid
flowchart LR
    GPS["GPS Location Provider"] --> AWSMaps["AWS Maps SDK / Amazon Location<br/>(Vector Tiles & Places API)"]
    AWSMaps --> Agent["RouteSafetyAgent.kt"]
    Agent --> Check{"Hazard Proximity Check"}
    
    Check -->|Inside Flood Polygon| Alert["OverlayAlertManager<br/>(Trigger Impassable Warning)"]
    Check -->|Approaching Submerged Zone| Detour["Call /api/routes/detour<br/>(AWS Strands Agent & Location Service)"]
    
    Detour --> HUD["SafeRouteCopilotScreen.kt<br/>(Turn-by-turn Detour HUD)"]
    Alert --> Canvas["HazardOverlayView.kt<br/>(Render Geometric Water Polygons)"]
```

* **AWS Maps SDK & Vector Tile Engine**: Powered by **Amazon Location Service Maps & Routes SDK** via MapLibre GL raster and vector tile pipelines. Renders topological terrain elevation, hydrological watercourses, and high-ground relief centers without relying on Google Play Services in low-battery or air-gapped zones.
* **Offline Vector Cache**: Pre-caches tile sets and known Mumbai/Kochi flood basin GeoJSON polygons in local encrypted SQLite. When cellular towers fail, the map continues rendering fluidly at 60 FPS offline.
* **Multi-Modal Avoidance Routing**: Unlike car-only routing engines, the Copilot calculates routes specifically weighted for **shallow-draft rescue boats**, **pedestrian high-ground evacuation**, and **heavy NDRF rescue trucks**, actively routing around active 33kV sub-surface line faults.
* [SafeRouteCopilotScreen.kt](file:///c:/Users/ACER/AndroidStudioProjects/gridzero/app/src/main/java/com/example/zerogrid/emergency/SafeRouteCopilotScreen.kt): Turn-by-turn evacuation navigation HUD.
* [RouteSafetyAgent.kt](file:///c:/Users/ACER/AndroidStudioProjects/gridzero/app/src/main/java/com/example/zerogrid/emergency/RouteSafetyAgent.kt): Monitors user coordinates against known hazard boundaries and prompts the backend for rerouting.
* [HazardOverlayView.kt](file:///c:/Users/ACER/AndroidStudioProjects/gridzero/app/src/main/java/com/example/zerogrid/emergency/HazardOverlayView.kt): Renders dynamic waterlogging polygons and downed electrical lines directly over the map.
* [OverlayAlertManager.kt](file:///c:/Users/ACER/AndroidStudioProjects/gridzero/app/src/main/java/com/example/zerogrid/emergency/OverlayAlertManager.kt): Displays non-intrusive system alert toasts when a citizen walks toward an active flood zone.

---

### 3.3 P2P Off-Grid Mesh Protocol & Hardware Drivers

When cellular towers lose power during heavy floods, Android devices self-organize into an autonomous radio relay:
* **`BleMeshDriver.kt`**:
  * Advertises custom ZeroGrid Service UUID: `0000ZG01-0000-1000-8000-00805F9B34FB`.
  * Bidirectional GATT Characteristic: `0000ZG02-0000-1000-8000-00805F9B34FB`.
  * Handles automatic MTU negotiation up to 512 bytes with packet segmentation and reassembly.
* **`WifiDirectMeshDriver.kt`**:
  * Implements Android `WifiP2pManager` to discover high-bandwidth Wi-Fi Direct groups without routers.
  * Opens TCP sockets across the P2P group for peer-to-peer data burst streaming.

---

### 3.4 32-Bit Word-Aligned Binary RF Packet Specification

```mermaid
gantt
    title P2P BLE Mesh Packet Header & Payload Bitmask (32-Bit Word Alignment)
    dateFormat X
    axisFormat %s

    section Word 0
    Magic Byte (0x5A 'ZG')        :0, 8
    Packet Type (0x01 SOS)         :8, 16
    TTL Hop Count (Max 5)          :16, 24
    Battery Level (0-100%)         :24, 32

    section Word 1
    Sender Node ID Hash (Bytes 4-7) :32, 64

    section Word 2
    Target Addr (0xFFFFFFFF Broadcast) :64, 96

    section Word 3
    Latitude (IEEE 754 Float32)     :96, 128

    section Word 4
    Longitude (IEEE 754 Float32)    :128, 160

    section Word 5
    Accuracy Meters (Float16)      :160, 176
    Category Enum (3 Bits)         :176, 184
    Payload Length (Bytes)         :184, 192
    Reserved Flags                 :192, 208

    section Payload & CRC
    UTF-8 Encoded Message Payload  :208, 272
    CRC-32 Integrity Checksum      :272, 304
```

| Byte Offset | Field Name | Data Type | Size | Description |
| :--- | :--- | :--- | :--- | :--- |
| **0x00** | `MAGIC_BYTE` | `uint8` | 1 Byte | Frame identifier: `0x5A` (ASCII `'Z'`). |
| **0x01** | `PACKET_TYPE` | `uint8` | 1 Byte | `0x01` SOS, `0x02` MSG, `0x03` ANNOUNCE, `0x04` ACK. |
| **0x02** | `TTL` | `uint8` | 1 Byte | Hop countdown (Default: `5`, max: `10`). Decremented per hop. |
| **0x03** | `BATTERY_PCT` | `uint8` | 1 Byte | Device battery percentage (`0–100%`). |
| **0x04–0x07**| `SENDER_HASH` | `uint32` | 4 Bytes | CRC32 hash of originating device public key. |
| **0x08–0x0B**| `TARGET_ADDR` | `uint32` | 4 Bytes | `0xFFFFFFFF` for broadcast; specific hash for unicast. |
| **0x0C–0x0F**| `LATITUDE` | `float32` | 4 Bytes | IEEE 754 single-precision GPS latitude. |
| **0x10–0x13**| `LONGITUDE` | `float32` | 4 Bytes | IEEE 754 single-precision GPS longitude. |
| **0x14–0x15**| `ACCURACY` | `float16` | 2 Bytes | GPS horizontal accuracy radius in meters. |
| **0x16** | `CATEGORY` | `uint8` | 1 Byte | `0x00` Medical, `0x01` Flood, `0x02` Trapped, `0x03` Fire. |
| **0x17** | `PAYLOAD_LEN` | `uint8` | 1 Byte | Byte length of dynamic UTF-8 message string. |
| **0x18–0x19**| `FLAGS` | `uint16` | 2 Bytes | Bitmask flags: Bit 0 = Critical, Bit 1 = Breaker Risk. |
| **Dynamic** | `PAYLOAD` | `string` | Variable | UTF-8 encoded text message. |
| **Tail** | `CRC32` | `uint32` | 4 Bytes | IEEE 802.3 CRC-32 integrity validation checksum. |

---

### 3.5 TTL Hop Control & 500-Slot LRU Deduplication

```
[Incoming Packet Ingress]
           │
           ▼
  DeduplicationCache.contains(packetId)?
     ├── YES ──► [DROP PACKET IMMEDIATELY] (Zero-cost loop defense)
     └── NO  ──► Commit packetId to 500-slot LRU Cache
                 │
                 ▼
  Is recipientId == localNodeId OR "*"?
     ├── YES ──► Parse payload & render on UI (SosCenter / Alert HUD)
     └── NO  ──► Skip local display
                 │
                 ▼
  Is TTL > 1?
     ├── NO  ──► [DROP PACKET] (Reached maximum hop horizon)
     └── YES ──► Clone packet:
                   ttl = ttl - 1
                   hopCount = hopCount + 1
                 Relay to all connected peers in PeerTable EXCLUDING sender
```

---

### 3.6 Tactical Compass Radar vs. Google Maps Mode

Dispatchers toggle between two specialized display modes:
1. **Google Maps View (`AdminGoogleMapView.kt`)**: Satellite/road map with color-coded pins (`#EF4444` Medical, `#F97316` Flood, `#3B82F6` Trapped) and GPS accuracy circles.
2. **Tactical Radar Canvas (`TacticalRadarCanvas.kt`)**: Zero-dependency concentric `Canvas` screen oriented in real-time by the device's physical magnetometer compass, rendering relative bearings to nearby victims without network data.

---

### 3.7 Navigation Stack & WhatsApp-Style Transitions

* **Root Navigation (`NavGraph.kt`)**: Hosts an optimized `HorizontalPager` with `beyondViewportPageCount = 1` for swipeable root navigation:
  * Page 0: `MeshDashboardScreen` (Peer density, radio badges, quick SOS).
  * Page 1: `MessagesScreen` (Mesh chat channels & direct messaging).
  * Page 2: `SosCenterScreen` (Emergency radar and broadcast center).
  * Page 3: `SettingsScreen` (Radio hardware toggles & admin portal entrance).
* **Animated Sub-Screen Stack**: Sub-screens (`PEER_DIRECT_CHAT`, `SEND_SOS`, `TRACK_SOS`, `SAFE_ROUTE_COPILOT`, `ADMIN_PANEL`) are pushed onto an internal `subScreenStack` rendered with smooth WhatsApp-style horizontal slide animations.

---

### 3.8 Device Resilience & WorkManager Offline Sync

* **`MeshForegroundService.kt`**: Operates as a persistent foreground service with low battery overhead, keeping BLE advertising and scanning active while the app is in the background.
* **`SosUploadWorker.kt`**: Executes in the background with `BackoffPolicy.EXPONENTIAL`. Retries cloud upload automatically upon network reconnection.
* **`ZeroGridFcmService.kt`**: Captures high-priority FCM emergency pushes, instantiating high-priority system alert notifications.

---

## 4. PART II: AWS Cloud Infrastructure & Multi-Agent Backend

[![AWS ECS](https://img.shields.io/badge/Compute-AWS%20ECS%20Fargate-FF9900?style=flat&logo=amazonecs&logoColor=white)](https://github.com/hehemohit/Zero_Grid)
[![Strands Agents](https://img.shields.io/badge/Agentic-Strands%20Agents%20SDK-FF4F00?style=flat&logo=openai&logoColor=white)](https://github.com/hehemohit/Zero_Grid)
[![Amazon Bedrock](https://img.shields.io/badge/AI%20Reasoning-Amazon%20Bedrock-7B2CBF?style=flat&logo=amazonaws&logoColor=white)](https://github.com/hehemohit/Zero_Grid)
[![MongoDB](https://img.shields.io/badge/Database-MongoDB%20Atlas%202dsphere-47A248?style=flat&logo=mongodb&logoColor=white)](https://github.com/hehemohit/Zero_Grid)
[![ElastiCache](https://img.shields.io/badge/Cache-ElastiCache%20Redis%20Redlock-DC382D?style=flat&logo=redis&logoColor=white)](https://github.com/hehemohit/Zero_Grid)
[![DynamoDB](https://img.shields.io/badge/Topology-Amazon%20DynamoDB-4053D6?style=flat&logo=amazondynamodb&logoColor=white)](https://github.com/hehemohit/Zero_Grid)

Once an SOS packet reaches an active internet uplink (cellular, Starlink, or Wi-Fi), it transitions from the physical mesh into the AWS Cloud Microservices platform.

---

### 4.1 Official AWS Architecture Blueprint

![ZeroGrid Official AWS Cloud Microservices Architecture](C:/Users/ACER/.gemini/antigravity-ide/brain/230e08d2-b887-4e64-9883-849acbcc94b7/aws_architecture_strands_1791639732883.jpg)

```mermaid
flowchart TB
    subgraph Tier1["1. Frontend Ingress"]
        AmplifyWeb["Web Dashboard<br/>(AWS Amplify / Next.js)"]
        KotlinMobile["Kotlin Android Mobile App<br/>(Citizen & Field Responder)"]
        DispatchTablet["Dispatch App Console<br/>(Incident Commander)"]
    end

    subgraph Tier2["2. Security & Gateway"]
        APIGW["Amazon API Gateway<br/>(HTTP REST & WebSocket /sos)"]
        Cognito["Amazon Cognito<br/>(JWT User Pool Authorizer)"]
        SecretsMgr["AWS Secrets Manager<br/>(Dynamic Key Injection)"]
    end

    subgraph Tier3["3. Serverless Compute"]
        LambdaVoice["Lambda A1: Voice Agent Hook<br/>(FastAPI + Mangum Adapter)"]
        LambdaGenAI["AWS Lambda GenAI<br/>(Async Incident Clustering & Triage)"]
    end

    subgraph Tier4["4. Core Backend Layer (AWS ECS Fargate)"]
        AgentZeroCore["AWS ECS Fargate Container<br/>(AgentZero powered by Strands Agents SDK)<br/>- Multi-step Reasoning Engine<br/>- Real-time Socket.io Ingress<br/>- Safe Detour Routing Engine"]
    end

    subgraph Tier5["5. AI Intelligence"]
        Bedrock["Amazon Bedrock<br/>(Claude 3.5 Sonnet & Claude 3 Haiku)"]
    end

    subgraph Tier6["6. Data Persistence & Caching"]
        MongoAtlas["MongoDB Atlas (2dsphere)<br/>- Active Citizen SOS Tickets<br/>- Rescuer Proximity Matching"]
        DynamoStore["Amazon DynamoDB State<br/>- Finished Case Dossiers<br/>- Agent Graph Memory & 33kV Topology"]
        RedisCluster["Amazon ElastiCache (Redis)<br/>- Distributed Redlock Mutex<br/>- Ephemeral Spatial Geohash Cache"]
    end

    %% Wiring
    AmplifyWeb -->|"HTTPS / WSS"| APIGW
    KotlinMobile -->|"HTTPS / WSS"| APIGW
    DispatchTablet -->|"HTTPS / WSS"| APIGW

    APIGW -.->|"Verify JWT"| Cognito
    SecretsMgr -.->|"Inject Secrets"| AgentZeroCore
    SecretsMgr -.->|"Inject Secrets"| LambdaVoice
    SecretsMgr -.->|"Inject Secrets"| LambdaGenAI

    APIGW -->|"Voice Audio Stream"| LambdaVoice
    APIGW -->|"Async Triage Events"| LambdaGenAI
    APIGW -->|"Core REST & WebSocket"| AgentZeroCore

    LambdaVoice -->|"Fast Voice Intent"| Bedrock
    LambdaGenAI -->|"Batch Incident Reasoning"| Bedrock
    AgentZeroCore <-->|"Two-Way Model Requests & Inferences"| Bedrock

    AgentZeroCore -->|"GeoJSON Spatial Queries (2dsphere)"| MongoAtlas
    AgentZeroCore -->|"Case Dossiers & Graph Memory"| DynamoStore
    AgentZeroCore -->|"Distributed Redlock & Ephemeral Cache"| RedisCluster

    LambdaVoice -.->|"Cache Voice Telemetry"| RedisCluster
    LambdaGenAI -->|"Persist Triaged Assessment"| DynamoStore
```

---

### 4.2 Ingress & Security: API Gateway, Cognito & Secrets Manager

* **Amazon API Gateway**: Routes incoming HTTP REST requests and maintains bidirectional WebSocket channels (`/sos`) for live coordinate streaming.
* **Amazon Cognito**: Verifies JWT tokens on every incoming distress call, enforcing role-based permissions (`CITIZEN` vs. `ADMIN`).
* **AWS Secrets Manager**: Automatically injects dynamic credentials (`MONGODB_URI`, JWT secrets) without storing static keys in application code.

---

### 4.3 Core Backend: AWS ECS Fargate (AgentZero powered by Strands Agents SDK)

* Hosted inside high-availability **AWS ECS Fargate** containers.
* **Strands Agents SDK (`@strands-agents/sdk`)**: Powers **AgentZero**'s multi-step agentic loop:
  1. Ingests raw disaster telemetry from the API Gateway.
  2. Dispatches tools for geospatial queries against MongoDB.
  3. Acquires distributed locks in ElastiCache Redis.
  4. Coordinates with **Amazon Bedrock** for multi-step reasoning.
  5. Broadcasts real-time incident state changes over Socket.io `/sos`.

---

### 4.4 Serverless Compute: Lambda A1 Voice Hook & Lambda GenAI

* **`[Lambda A1] Voice Agent Hook`**:
  * Python 3.11 + FastAPI wrapped with a **Mangum** serverless adapter.
  * Ingests voice command audio bursts from trapped citizens and queries **Amazon Bedrock (Claude 3 Haiku)**.
  * Returns concise voice guidance (under 35 words) and evacuation action tags within 800ms.
* **`[AWS Lambda GenAI]`**:
  * Triggered asynchronously by API Gateway or SQS events.
  * Groups nearby flood distress reports into disaster clusters and drafts NDMA situation reports using **Claude 3.5 Sonnet**.

---

### 4.5 AI Intelligence: Amazon Bedrock Two-Way Reasoning

* **Two-Way Bidirectional Loop**: ECS Fargate sends structured situation prompts to **Amazon Bedrock** and receives parsed action plans, evacuation waypoints, and equipment quotas.
* **Multi-Modal Capabilities**: Analyzes text descriptions, acoustic clamor, and satellite flood inundation reports.

---

### 4.6 Data Persistence: MongoDB Atlas (2dsphere), DynamoDB & ElastiCache

ZeroGrid implements a specialized three-tier data architecture to balance geospatial proximity search, sub-millisecond electrical topology state, and distributed mutex synchronization:

1. **MongoDB Atlas (`2dsphere`)**:
   * Stores active citizen distress incidents in native GeoJSON format (`Point`, `Polygon`).
   * Executes `$nearSphere` queries to calculate nearest rescue units and shelters within a 5km radius in under 20ms.
   * Runs `$geoWithin` with `$centerSphere` for autonomous 250m spatial deduplication clusters.

2. **Amazon DynamoDB (Sub-Millisecond 33kV SCADA & Graph State)**:
   * **Single-Table Design (`ZeroGrid-State`)**: Dedicated high-throughput NoSQL database optimized for single-digit millisecond (`< 4ms`) point lookups.
   * **Electrical Grid Topology Graph**: Maps substations, 33kV/11kV transformers, feeder vacuum circuit breakers, and hospital ICU busbars.
   * **Schema Architecture**:
     ```
     PK: SUBSTATION#<SubstationId>          SK: FEEDER#<FeederId>
     Attributes:
       - VoltageRating: 33kV | 11kV
       - BreakerStatus: ENERGIZED | TRIPPED | STANDBY
       - InundationDepthCm: 55.0
       - HospitalPriorityFlag: true (ICU Busbar Lock)
       - StandbyTieLineId: FEEDER_TIE_14B
     ```
   * **Automated Breaker Isolation**: When flood depths exceed 30cm near high-voltage plinths, AgentZero executes a conditional DynamoDB update (`UpdateItem` with `ConditionExpression`) to safely verify that the hospital ICU busbar has transferred to a standby tie-line before asserting the breaker trip.
   * **Finished Case Dossiers**: Archives historical incident trajectories upon resolution for post-monsoon state auditability.

3. **Amazon ElastiCache for Redis**:
   * Provides distributed Redlock synchronization (`SETNX lock:squad:<id>`) to prevent duplicate SOS dispatches across parallel agents.
   * Caches ephemeral rescuer GPS coordinates and powers multi-server Socket.io pub/sub.

---

### 4.7 Autonomous 4-Phase Circular Multi-Agent Pipeline

$$\text{Confidence Calculator } (\ge 65\%) \longrightarrow \text{Agent 0 Gatekeeper} \longrightarrow \text{4 Specialized Sub-Agents} \longrightarrow \text{Agent 0 Workforce Allocation}$$

```mermaid
flowchart TD
    In["Citizen SOS / Sensor Ingress"] --> P1["Phase 1: Confidence Calculator Agent<br/>- Audio Spectrum (25%)<br/>- Visual Satellite (35%)<br/>- IoT Submersible Probes (25%)<br/>- Coastal Tide Surge (15%)"]
    
    P1 -->|Score >= 65%| P2["Phase 2: AgentZero Gatekeeper<br/>- Spatial Deduplication on Grid Adjacency<br/>- Threat Indexing (0-100)<br/>- Autonomous Domain Routing"]
    P1 -->|Score < 65%| Drop["Flag as Low-Confidence Sensor Artifact"]

    P2 --> Sub1["1. Flood Management Sub-Agent"]
    P2 --> Sub2["2. Heatwave Management Sub-Agent"]
    P2 --> Sub3["3. Power Grid Operations Sub-Agent"]
    P2 --> Sub4["4. Rescue Management Sub-Agent"]

    Sub1 --> P4["Phase 4: AgentZero Workforce Allocation<br/>- Query 160-Admin MongoDB Roster<br/>- Atomic ElastiCache Redis Lock<br/>- Round 1: Match Primary Tactical Tags<br/>- Shortfall? Iterative Fallback Department Loop<br/>- Lock Units & Emit Socket.io Update"]
    Sub2 --> P4
    Sub3 --> P4
    Sub4 --> P4
```

* **Phase 1: Confidence Calculator Agent**: Evaluates multi-modal data before alerting operators:
  $$\text{Score} = (0.25 \times \text{Audio}) + (0.35 \times \text{Visual}) + (0.25 \times \text{IoT Depth}) + (0.15 \times \text{Tide Surge})$$
  *(Alerts $\ge 65\%$ pass through; alerts $< 65\%$ are held in an observation queue).*
* **Phase 2: AgentZero Gatekeeper**: Deduplicates alerts against existing grid incidents and indexes threat levels (0–100).
* **Phase 3: Specialized Sub-Agents**: Formulates squad demand quotas and identifies required equipment tags (`ZODIAC_BOAT`, `DEWATERING`, `MEDICAL_TRIAGE`).
* **Phase 4: Workforce Allocation Loop**: Matches requirements against available administrative personnel and acquires atomic Redis locks.

---

### 4.8 Electrical Grid Safety: 33kV Topology & Hospital ICU Lifeline

* **Hospital Lifeline ICU Protection**: Guarantees Sanjeevani Hospital ICU busbars are dynamically transferred to standby 33kV tie-lines with 0ms interruption before isolation of flooded primary substations.
* **Human-in-the-Loop (HITL) Safety Gate**: High-voltage breaker actuations require explicit digital sign-off from the Incident Commander via modal authentication before triggering lock-out/tag-out directives.

---

### 4.9 160-Admin Workforce Allocation Matrix

| Department Tag | Headcount | Primary Tactical Tags | Iterative Fallback Department |
| :--- | :--- | :--- | :--- |
| `FLOOD_MANAGEMENT` | 40 Admins | `DEWATERING`, `DEEP_WATER_RESQ`, `ZODIAC_BOAT` | `RESCUE_MANAGEMENT` |
| `HEATWAVE_MANAGEMENT` | 40 Admins | `MEDICAL_TRIAGE`, `HYDRATION_SQUAD`, `COOLING_STATION`| `RESCUE_MANAGEMENT` |
| `POWER_GRID_MANAGEMENT` | 40 Admins | `HV_LINEMAN`, `SUBSTATION_CREW`, `AIR_GAP_ISOLATION` | `RESCUE_MANAGEMENT` |
| `RESCUE_MANAGEMENT` | 40 Admins | `HEAVY_RESCUE`, `COLLAPSE_SEARCH`, `TRAUMA_PARAMEDIC` | `FLOOD_MANAGEMENT` |

* **Collision Defense**: Every squad allocation invokes an **Amazon ElastiCache Redis** atomic distributed lock (`SETNX lock:squad:TEAM_ALPHA px 300000`). If a squad is already deployed, AgentZero executes an **iterative fallback negotiation**, pulling equivalent personnel from the fallback department.

---

### 4.10 AWS Maps SDK & Amazon Location Service Safe Detour Engine

ZeroGrid integrates **Amazon Location Service (Maps, Routes, Places)** through the **AWS Maps SDK** to calculate dynamic navigation paths that guide civilian evacuation and rescue boats around flooded deathtraps:

```mermaid
flowchart TD
    Origin["Origin Coordinates (Victim / Boat)"] --> Avoid["Fetch Active Hazards & 33kV Zones<br/>(DynamoDB + MongoDB Atlas)"]
    Avoid --> ALSRoutes["Amazon Location Service Routes API<br/>POST /routes/v0/calculators/ZeroGrid-Route/calculate/route"]
    ALSRoutes --> Filter["Dynamic Avoidance Filter<br/>- Water Depth > 30cm (Non-Passable)<br/>- Energized 33kV Plinth (180m Buffer)<br/>- High Tide Sluice Gate Backflow"]
    Filter --> GeoJSON["Generate Safe Detour Polyline (GeoJSON)"]
    GeoJSON --> Render["Render on AWS Maps SDK / MapLibre GL<br/>(Web Console & Android Compose HUD)"]
```

* **Dynamic Detour API (`/api/routes/detour`)**: Calls Amazon Location Service with customized avoidance geometries.
* **AWS Maps SDK Map Canvas (`MapLibre GL` on Web & `AWS Maps SDK` on Android)**:
  * Renders dark tactical vector tiles with zero external dependencies.
  * Plots real-time victim coordinates, live emergency badges, and color-coded flood hazard polygons.
  * Draws safe navigation corridors directly to dry ground evacuation staging points.

---

## 5. PART III: Web Operations Command Center (Amplify / Next.js 16)

[![Next.js](https://img.shields.io/badge/Framework-Next.js%2016.3.5%20App%20Router-black?style=flat&logo=nextdotjs&logoColor=white)](https://github.com/hehemohit/Zero_Grid)
[![React](https://img.shields.io/badge/UI-React%2019.2-61DAFB?style=flat&logo=react&logoColor=black)](https://github.com/hehemohit/Zero_Grid)
[![Amplify](https://img.shields.io/badge/Hosting-AWS%20Amplify%20Edge%20CDN-FF9900?style=flat&logo=awsamplify&logoColor=white)](https://github.com/hehemohit/Zero_Grid)
[![MapLibre](https://img.shields.io/badge/Mapping-MapLibre%20GL%204.7-396B9E?style=flat&logo=maplibre&logoColor=white)](https://github.com/hehemohit/Zero_Grid)
[![Tailwind](https://img.shields.io/badge/CSS-Tailwind%20CSS%20v4-38B2AC?style=flat&logo=tailwindcss&logoColor=white)](https://github.com/hehemohit/Zero_Grid)

The web dashboard is hosted on **AWS Amplify** with edge SSR/SSG:
* **Live Operations Map (`SosLiveMap.tsx`)**: Powered by **Amazon Location Service v2** and **MapLibre GL**, displaying real-time incident pins and dynamic flood overlay polygons.
* **Interactive Incident Drawer (`SosDrawer.tsx`)**: Enables disaster coordinators to review AI threat assessments, assign rescue squads, and log field notes.
* **Chaos Prediction Console**: Visualizes 24-hour predictive water-level hydrodynamics based on tidal surge models and rainfall sensors.

---

## 6. End-to-End Emergency Incident Lifecycle (Sequence Flow)

```mermaid
sequenceDiagram
    autonumber
    actor Citizen as Citizen Mobile (Kotlin)
    participant ApiGw as Amazon API Gateway
    participant Cognito as Amazon Cognito
    participant ECS as AWS ECS (AgentZero Core)
    participant Redis as ElastiCache (Redis Lock)
    participant Mongo as MongoDB Atlas (2dsphere)
    participant Bedrock as Amazon Bedrock (Claude 3.5)
    participant Dynamo as DynamoDB (Grid State)
    actor Commander as Incident Commander (Amplify)
    actor Responder as Field Responder (Android)

    %% 1. Ingress
    Citizen->>ApiGw: POST /api/v1/sos (lat, lng, category, battery) + JWT
    ApiGw->>Cognito: Validate Bearer Token
    Cognito-->>ApiGw: Token Valid (Claims: CITIZEN)
    ApiGw->>ECS: Ingest Distress Ticket

    %% 2. Processing & Deduplication
    ECS->>Redis: Acquire Redlock: lock:incident:user_id
    ECS->>Mongo: Insert GeoJSON Point with 2dsphere index
    ECS->>Mongo: $nearSphere search for closest available rescuers
    ECS->>Redis: Release Incident Lock

    %% 3. Multi-Agent Reasoning
    ECS->>Dynamo: Query 33kV Adjacency Topology & Flood Risk
    ECS->>Bedrock: Model Request: Calculate Action Plan & Directives
    Bedrock-->>ECS: Inference: P1-Critical, Dispatch Boat, Trip Substation 4
    
    %% 4. Command & HITL Safety Gate
    ECS->>Commander: WebSocket emit('sos:new', incidentDossier)
    Commander->>ECS: Confirm Breaker Trip & Approve Workforce Allocation
    ECS->>Dynamo: Update Breaker State (TRIPPED, Hospital TieLine ENGAGED)
    ECS->>Redis: Acquire Squad Mutex: lock:squad:TEAM_NDRF_ALPHA
    
    %% 5. Dispatch & Resolution
    ECS->>Responder: Push Notification + Dynamic Detour Route
    Responder->>ECS: PUT /api/v1/sos/id/resolve (Field Notes)
    ECS->>Mongo: Update Status to RESOLVED
    ECS->>Dynamo: Archive Case Dossier & Agent Graph Memory
    ECS->>Redis: Release Squad Mutex Lock
    ECS->>Commander: WebSocket emit('sos:resolved', { id, timestamp })
```

---

## 7. Quickstart & Local Multi-Container Development

### 7.1 Start Local Databases (`docker-compose.yml`)

```bash
docker-compose up -d
```
*(Starts local MongoDB 7.0 with 2dsphere support and Redis 7.2 on port 6379).*

### 7.2 Run Core Backend (`gridZeroExpress/backend`)

```bash
cd gridZeroExpress/backend
npm install
npm run dev
```
* Health Check: `http://localhost:5000/health`
* WebSocket Ingress: `ws://localhost:5000/sos`

### 7.3 Run Command Center Web (`gridZeroExpress/frontend`)

```bash
cd gridZeroExpress/frontend
npm install
npm run dev
```
* Dashboard URL: `http://localhost:3000`

### 7.4 Run Native Android App (`app/`)

1. Open repository root in Android Studio (Ladybug / Meerkat).
2. Configure `local.properties`:
   ```properties
   sdk.dir=C:\\Users\\ACER\\AppData\\Local\\Android\\Sdk
   MAPS_API_KEY=AIzaSy...
   ```
3. Set target backend in `app/src/main/java/com/example/zerogrid/network/ApiConstants.kt`:
   ```kotlin
   const val BASE_URL = "https://d111111abcdef8.cloudfront.net/"
   ```
4. Build and deploy to physical Android device (Android 8.0+ / API 26–35).

---

## 8. Security, Governance & Resilience Matrix

| Security Dimension | Implementation Mechanism | Defensive Purpose |
| :--- | :--- | :--- |
| **Transport Encryption** | TLS 1.3 / HTTPS (Enforced via CloudFront & API Gateway) | Protects distress beacons and responder coordinates in transit. |
| **Authentication** | AWS Cognito User Pools + Signed JWT (HMAC-SHA256) | Stateless authorization for all REST and WebSocket connections. |
| **Privilege Escalation Defense** | Live database check in `verifyAdminRole.js` | Prevents token claims forgery; ensures revoked admins are immediately blocked. |
| **Distributed Collision Defense** | Redis Redlock (`SETNX` mutex locks) | Guarantees emergency rescue squads cannot be double-booked. |
| **Geospatial Isolation** | MongoDB `2dsphere` indexing | Restricts spatial searches to authorized radii without scanning entire collections. |
| **Credential Hardening** | AWS Secrets Manager + IAM Instance Profiles | Eliminates long-lived static AWS access keys in production environments. |
| **Offline Loop Prevention** | 500-slot LRU `DeduplicationCache` + TTL countdown | Stops infinite RF broadcast storms across peer mesh networks. |
