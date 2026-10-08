# ZeroGrid — Comprehensive System Architecture & Developer Guide

> **Zero-Infrastructure Emergency Response & Mesh Communication Platform**  
> Enables resilient, peer-to-peer off-grid alerting and messaging across Android devices using Bluetooth Low Energy (BLE) and Wi-Fi Direct, seamlessly bridging to a centralized Node.js/Express + MongoDB cloud backend and Next.js Web Admin Console whenever cellular/satellite connectivity is available.

---

## Table of Contents

1. [Ecosystem Overview](#1-ecosystem-overview)
2. [Monorepo / Directory Structure](#2-monorepo--directory-structure)
3. [End-to-End System Architecture](#3-end-to-end-system-architecture)
4. [Mesh Protocol & Packet Architecture](#4-mesh-protocol--packet-architecture)
   - 4.1 [Packet Envelope Specification](#41-packet-envelope-specification)
   - 4.2 [Packet Types & Semantics](#42-packet-types--semantics)
   - 4.3 [Multi-Hop Routing, TTL & Loop Prevention](#43-multi-hop-routing-ttl--loop-prevention)
   - 4.4 [Transport Drivers (BLE & Wi-Fi Direct)](#44-transport-drivers-ble--wi-fi-direct)
   - 4.5 [Deduplication Engine](#45-deduplication-engine)
5. [Backend Architecture (`gridZeroExpress/backend`)](#5-backend-architecture-gridzeroexpressbackend)
   - 5.1 [Runtime & Technology Stack](#51-runtime--technology-stack)
   - 5.2 [Directory Organization](#52-directory-organization)
   - 5.3 [Data Schemas (Mongoose)](#53-data-schemas-mongoose)
   - 5.4 [REST API Contract](#54-rest-api-contract)
   - 5.5 [Real-Time WebSocket Architecture (`/sos` namespace)](#55-real-time-websocket-architecture-sos-namespace)
   - 5.6 [Middleware & Security Guards](#56-middleware--security-guards)
6. [Web Dashboard (`gridZeroExpress/frontend`)](#6-web-dashboard-gridzeroexpressfrontend)
   - 6.1 [Technology Stack & Next.js Architecture](#61-technology-stack--nextjs-architecture)
   - 6.2 [Directory Structure](#62-directory-structure)
   - 6.3 [Component Breakdown & Admin Maps](#63-component-breakdown--admin-maps)
   - 6.4 [Authentication & Context Management](#64-authentication--context-management)
7. [Android Application (`gridzero`)](#7-android-application-gridzero)
   - 7.1 [Technology Stack & Build Configuration](#71-technology-stack--build-configuration)
   - 7.2 [Module Architecture & Package Structure](#72-module-architecture--package-structure)
   - 7.3 [Navigation System & Custom Pager Stack](#73-navigation-system--custom-pager-stack)
   - 7.4 [Module Deep-Dives](#74-module-deep-dives)
     - Core Mesh Engine & Hardware Managers
     - Emergency Dispatcher & WorkManager Offline Sync
     - Mobile Admin Panel & Tactical Radar / Google Maps
     - Identity, Auth, Contacts & Family Linking
     - Background Services (BLE GATT + Firebase Cloud Messaging)
8. [End-to-End SOS Lifecycle Workflow](#8-end-to-end-sos-lifecycle-workflow)
9. [Security, Roles & Permission Model](#9-security-roles--permission-model)
10. [Setup, Execution & Configuration Guide](#10-setup-execution--configuration-guide)
11. [Troubleshooting & Known Architecture Notes](#11-troubleshooting--known-architecture-notes)
12. [Proactive Hazard Intelligence & Autonomous Safe Route Engine](#12-proactive-hazard-intelligence--autonomous-safe-route-engine)
   - 12.1 [System Alert Overlay & Proximity Geofencing (`SYSTEM_ALERT_WINDOW`)](#121-system-alert-overlay--proximity-geofencing-system_alert_window)
   - 12.2 [Offline Hazard Cache Manager & Monitored Hazard Categories](#122-offline-hazard-cache-manager--monitored-hazard-categories)
   - 12.3 [Home Screen Nearby Hazard Radar Card](#123-home-screen-nearby-hazard-radar-card)
   - 12.4 [Autonomous Route Safety Agent (Rule-Based & Pluggable AI Contract)](#124-autonomous-route-safety-agent-rule-based--pluggable-ai-contract)
   - 12.5 [OSRM Driving Route Engine & Offline Interpolation Fallback](#125-osrm-driving-route-engine--offline-interpolation-fallback)
   - 12.6 [Dual-Category Risk Classification: Avoided vs. Unavoidable Hazards](#126-dual-category-risk-classification-avoided-vs-unavoidable-hazards)
   - 12.7 [In-App Visual Route Preview & Google Maps Navigation Handoff](#127-in-app-visual-route-preview--google-maps-navigation-handoff)

---

## 1. Ecosystem Overview

The ZeroGrid ecosystem provides a hybrid topology designed specifically for disaster zones, network blackouts, and critical field operations:

```
┌────────────────────────────────────────────────────────────────────────┐
│                          LOCAL DISASTER AREA                           │
│                                                                        │
│   [Citizen A (Sender)] ──(BLE GATT / WiFi-P2P)──► [Citizen B (Relay)]   │
│            │                                              │            │
│       (No Internet)                                 (BLE / WiFi-P2P)   │
│            │                                              ▼            │
│            └────────────────────────────────────► [Citizen C (Gateway)] │
└───────────────────────────────────────────────────────────┬────────────┘
                                                            │ (Cellular / Starlink)
                                                            ▼
                                           ┌─────────────────────────────┐
                                           │   gridZeroExpress BACKEND   │
                                           │  Node.js / Express 5 / Mongo│
                                           └──────┬───────────────┬──────┘
                                                  │               │
                               (WebSocket / REST) │               │ (Socket.IO / REST)
                                                  ▼               ▼
                                       ┌────────────────┐   ┌────────────┐
                                       │ Web Admin      │   │ Mobile     │
                                       │ Dashboard      │   │ Admin      │
                                       │ (Next.js 16)   │   │ (Android)  │
                                       └────────────────┘   └────────────┘
```

1. **Local Mesh Sub-tier (Android Devices)**: Form autonomous ad-hoc P2P networks using Bluetooth LE advertisements/GATT connections and Wi-Fi Direct. Packets travel hop-by-hop without routers or cellular towers.
2. **Cloud Uplink (Opportunistic Ingress)**: When any node in the mesh connects to cellular or Wi-Fi, it acts as a gateway uploading queued mesh SOS packets to the cloud backend.
3. **Command & Control Tier (Web + Android Admin)**: Incident responders and disaster dispatch teams visualize all live incidents, assign responders, update incident status, and log chronological field notes.

---

## 2. Monorepo / Directory Structure

The system is maintained across two companion project directories:

```
AndroidStudioProjects/
│
├── gridzero/                               # Android Application (Kotlin / Compose)
│   ├── app/
│   │   ├── src/main/
│   │   │   ├── cpp/                        # Native C++ JNI bridge for BLE optimizations
│   │   │   ├── java/com/example/zerogrid/  # Primary Kotlin codebase
│   │   │   │   ├── admin/                  # Mobile Admin Console (Maps, Radar, History, Users)
│   │   │   │   ├── auth/                   # Authentication (Local & Google OAuth)
│   │   │   │   ├── contacts/               # Emergency contact management
│   │   │   │   ├── debug/                  # Diagnostic consoles & in-memory log buffer
│   │   │   │   ├── emergency/              # SOS creation, dispatcher & WorkManager upload
│   │   │   │   ├── family/                 # Child/Parent location tracking
│   │   │   │   ├── hardware/               # BLE & Wi-Fi state observers & top banner
│   │   │   │   ├── home/                   # Mesh dashboard & active peers summary
│   │   │   │   ├── location/               # Fused GPS provider helper
│   │   │   │   ├── mesh/                   # Routing engine, packet models & drivers
│   │   │   │   ├── messaging/              # Channels & 1:1 direct peer chat
│   │   │   │   ├── navigation/             # Navigation graph & pager controller
│   │   │   │   ├── network/                # Retrofit HTTP services & Repositories
│   │   │   │   ├── onboarding/             # Splash, Identity setup & permission checks
│   │   │   │   ├── profile/                # Profile management
│   │   │   │   ├── service/                # Foreground BLE service & FCM listener
│   │   │   │   ├── settings/               # Settings, keys & security preferences
│   │   │   │   ├── ui/                     # Design tokens, themes & shared components
│   │   │   │   └── util/                   # Input validation utilities
│   │   │   └── res/                        # Layouts, drawables, strings, colors
│   │   └── build.gradle.kts                # Android build script (SDK 35, Compose, Maps)
│   ├── local.properties                    # Secrets (MAPS_API_KEY, Keystores) - Git Ignored
│   └── README.md                           # This document
│
└── gridZeroExpress/                        # Unified Cloud Server & Web Console
    ├── backend/                            # Node.js + Express + MongoDB Server
    │   ├── src/
    │   │   ├── server.js                   # Server initialization, Socket.IO & Mongo hooks
    │   │   ├── controllers/                # Business logic (SOS, Auth, Admin, Family)
    │   │   ├── routes/                     # Express API endpoint declarations
    │   │   ├── models/                     # Mongoose Schemas (User, SosEvent, Contact, etc.)
    │   │   ├── middleware/                 # JWT authenticator & Role-based access guards
    │   │   └── utils/                      # FCM dispatcher, JWT generator & validators
    │   └── package.json                    # Backend dependencies
    │
    ├── frontend/                           # Next.js 16 Web Admin Dashboard
    │   ├── src/
    │   │   ├── app/                        # App Router (pages: admin, dashboard, auth)
    │   │   ├── components/                 # UI components (SosLiveMap, Drawer, Sidebar)
    │   │   ├── context/                    # AuthContext & ThemeContext providers
    │   │   └── lib/                        # Axios HTTP API client & token injector
    │   └── package.json                    # Frontend dependencies
    └── postman/                            # Postman collection for API test automation
```

---

## 3. End-to-End System Architecture

```
                                  ┌─────────────────────────────┐
                                  │      GPS Satellites         │
                                  └──────────────┬──────────────┘
                                                 │
                                                 ▼
┌────────────────────────────────────────────────────────────────────────────────────────┐
│ ANDROID MOBILE CLIENT (gridzero)                                                       │
│                                                                                        │
│  [ LocationHelper ] ──► [ UnifiedSosDispatcher ]                                      │
│                                │                                                       │
│                ┌───────────────┴───────────────┐                                       │
│                ▼                               ▼                                       │
│       [ MeshEngine ]                 [ RetrofitInstance / SosApiService ]              │
│        - PeerTable                   (if online) ──► POST /api/sos                     │
│        - DeduplicationCache                            │ (if offline)                  │
│        - BleMeshDriver                                 ▼                               │
│        - WifiDirectMeshDriver               [ SosUploadWorker ]                        │
│                │                             (WorkManager Exponential Backoff)         │
│                ▼                                       │                               │
│        Over-the-Air Packet                             ▼                               │
└────────────────┼───────────────────────────────────────┼───────────────────────────────┘
                 │                                       │ HTTPS REST
                 ▼                                       ▼
┌─────────────────────────────────┐   ┌──────────────────────────────────────────────────┐
│ AD-HOC MESH NETWORK             │   │ EXPRESS BACKEND SERVER (gridZeroExpress/backend) │
│ - BLE Advertisements / GATT     │   │                                                  │
│ - Wi-Fi Direct Sockets          │   │  - POST /api/sos                                 │
│ - Forwarded up to 5 Hops (TTL)  │   │  - PUT  /api/sos/:id/acknowledge                 │
└─────────────────────────────────┘   │  - PUT  /api/sos/:id/resolve                     │
                                      │  - GET  /api/admin/sos                           │
                                      │  - Socket.IO Server (namespace: /sos)            │
                                      │  - Firebase Cloud Messaging (FCM Push)           │
                                      └──────────────┬──────────────────┬────────────────┘
                                                     │                  │
                                     WebSocket Event │                  │ MongoDB Connection
                                     'sos:new'       │                  │
                                                     ▼                  ▼
                                      ┌──────────────────────┐  ┌────────────────────────┐
                                      │ Web & Mobile Admin   │  │ MongoDB Database       │
                                      │ - Live Google Maps   │  │ - Users (2dsphere idx) │
                                      │ - Tactical Canvas    │  │ - SosEvents            │
                                      │ - Incident Feed      │  │ - Contacts             │
                                      └──────────────────────┘  └────────────────────────┘
```

---

## 4. Mesh Protocol & Packet Architecture

### 4.1 Packet Envelope Specification

Every packet traveling through the ZeroGrid mesh is encapsulated in a compact, deterministic UTF-8 JSON envelope:

```json
{
  "packetId": "d9b2e048-c89b-4b13-a7cf-e48f1082aa91",
  "senderId": "node-8f2a1b",
  "recipientId": "*",
  "ttl": 5,
  "hopCount": 0,
  "type": "SOS_BEACON",
  "payload": "{\"category\":\"MEDICAL\",\"message\":\"Injured leg\",\"lat\":19.0760,\"lng\":72.8777,\"accuracy\":8.5,\"senderName\":\"John\",\"ts\":1727337600000}",
  "timestamp": 1727337600000,
  "signature": ""
}
```

| Field | Type | Description |
|---|---|---|
| `packetId` | `String` (UUIDv4) | Globally unique identifier used by every node for deduplication. |
| `senderId` | `String` | Originating Node ID (derived from device public key or unique hardware hash). |
| `recipientId` | `String` | Target Node ID, or wildcard `"*"` for broadcast packets. |
| `ttl` | `Int` | Time-to-Live hop countdown (default: `5`). Decremented at each hop. |
| `hopCount` | `Int` | Diagnostic counter (starts at `0`, incremented at each hop). |
| `type` | `PacketType` | Classification determining internal routing and unpacking logic. |
| `payload` | `String` | Serialized JSON string containing category, message, lat/lng coordinates. |
| `timestamp` | `Long` | Epoch millisecond timestamp at generation. |
| `signature` | `String` | Cryptographic signature slot (reserved for Ed25519 signing). |

### 4.2 Packet Types & Semantics

Defined in [PacketType.kt](file:///c:/Users/ACER/AndroidStudioProjects/gridzero/app/src/main/java/com/example/zerogrid/mesh/engine/PacketType.kt):

| PacketType | Delivery Scope | Payload Contents | Handled By |
|---|---|---|---|
| `SOS_BEACON` | **Broadcast (`*`)** | Coordinates (`lat`, `lng`), `accuracy`, `category`, `message`, `senderName` | `SosCenterScreen`, `UnifiedSosDispatcher` |
| `DIRECT_MESSAGE` | **Unicast (Node ID)** | Encrypted or plain text private message, message ID, timestamp | `PeerDirectChatScreen`, `MessageStore` |
| `CHANNEL_MESSAGE`| **Broadcast (`*`)** | Public channel name, message text, sender alias | `ChannelChatScreen`, `MessageStore` |
| `PEER_ANNOUNCE` | **Broadcast (`*`)** | Node capabilities, battery %, channel mode, display name | `PeerTable`, `NearbyDevicesScreen` |
| `PEER_GOODBYE`  | **Broadcast (`*`)** | Origin node ID signaling graceful disconnect | `PeerTable` (evicts peer) |
| `ROUTE_REQUEST` | **Broadcast (`*`)** | Target node ID being searched | `MeshRoutingEngine` |
| `ROUTE_REPLY`   | **Unicast** | Routing table cost metric and next-hop address | `MeshRoutingEngine` |
| `ACK`           | **Unicast** | Confirmed `packetId` | Direct transport acknowledgment |

### 4.3 Multi-Hop Routing, TTL & Loop Prevention

1. **Reception**: A packet is received via `BleMeshDriver` or `WifiDirectMeshDriver`.
2. **Deduplication Gate**: `DeduplicationCache.contains(packetId)` is evaluated:
   - If true: Packet is immediately dropped.
   - If false: `packetId` is committed to the cache.
3. **Local Delivery**: If `recipientId == localNodeId` or `recipientId == "*"`, the packet is parsed and dispatched to relevant UI listeners (`sosAlerts`, `conversations`).
4. **Relay Decision**:
   - Check `ttl > 1`. If `ttl <= 1`, the packet has reached its horizon and is dropped.
   - Copy packet with `ttl = ttl - 1` and `hopCount = hopCount + 1`.
   - Transmit copied packet to all active peers in `PeerTable` **excluding** the peer who delivered it.

### 4.4 Transport Drivers (BLE & Wi-Fi Direct)

- **`BleMeshDriver`**:
  - Implements Bluetooth Low Energy peripheral advertising and central GATT scanning.
  - Advertises ZeroGrid Custom Service UUID `0000ZG01-0000-1000-8000-00805F9B34FB`.
  - GATT Characteristic `0000ZG02-...` handles bidirectional streaming with notifications.
  - Automatic MTU negotiation up to 512 bytes with packet segment reassembly for oversized payloads.
- **`WifiDirectMeshDriver`**:
  - Implements Android `WifiP2pManager` to discover and connect high-bandwidth Wi-Fi Direct peers.
  - Opens TCP client/server sockets across the established P2P group for peer-to-peer burst streaming.

### 4.5 Deduplication Engine

`DeduplicationCache.kt` utilizes a synchronized `LinkedHashMap` configured as an LRU (Least Recently Used) cache with a maximum capacity of **500 packet IDs**. Any attempt to re-process an existing packet ID within this window returns `false`, preventing circular broadcast storms.

---

## 5. Backend Architecture (`gridZeroExpress/backend`)

### 5.1 Runtime & Technology Stack

- **Runtime**: Node.js v18+ / v20+ LTS
- **Framework**: Express 5.2.1
- **Real-Time Engine**: Socket.IO 4.8.3
- **Database**: MongoDB 7.6 / 8.0 via Mongoose 9.10.1
- **Security**: JWT (`jsonwebtoken` 9.0), `bcrypt` 6.0, `express-rate-limit` 8.7
- **Push Notification Service**: Firebase Admin SDK (`firebase-admin` 13.10.0)

### 5.2 Directory Organization

```
backend/src/
├── server.js                # Express app, HTTP server, Socket.IO /sos namespace, Mongo connection
├── controllers/
│   ├── authController.js    # Register, login, Google OAuth verification
│   ├── userController.js    # Profile retrieval, complete-profile, FCM token registration
│   ├── sosController.js     # SOS creation, retrieval, acknowledgment, resolution, notes
│   ├── adminController.js   # Incident history, directory queries, admin promotion/revocation
│   ├── contactController.js # Emergency contact CRUD
│   └── familyController.js  # Parent-child linking and GPS coordinates querying
├── models/
│   ├── User.js              # User account, roles, approval flags, location
│   ├── SosEvent.js          # SOS incident schema with GeoJSON 2dsphere index
│   ├── Contact.js           # Emergency contact references
│   └── ParentChildLink.js   # Family link state (PENDING / ACCEPTED / REVOKED)
├── routes/
│   ├── authRoutes.js        # /api/auth
│   ├── userRoutes.js        # /api/users
│   ├── sosRoutes.js         # /api/sos
│   ├── adminRoutes.js       # /api/admin
│   ├── contactRoutes.js     # /api/contacts
│   └── familyRoutes.js      # /api/family
├── middleware/
│   ├── verifyToken.js       # JWT validation header extractor
│   └── verifyAdminRole.js   # Strict database check: role === 'ADMIN' && adminApproved === true
└── utils/
    ├── fcm.js               # Firebase messaging dispatcher
    ├── jwt.js               # Token generation and claims encoder
    └── validation.js        # Request sanitizers
```

### 5.3 Data Schemas (Mongoose)

#### SosEvent Schema
```javascript
{
  triggeredBy: { type: ObjectId, ref: 'User', required: true, index: true },
  location: {
    type: { type: String, enum: ['Point'], required: true },
    coordinates: { type: [Number], required: true } // [Longitude, Latitude]
  },
  accuracyMeters: { type: Number, default: null },
  category: { 
    type: String, 
    enum: ['MEDICAL', 'DISASTER', 'TRAPPED', 'SECURITY', 'OTHER'], 
    default: 'OTHER' 
  },
  message: { type: String, default: '' },
  transport: { type: String, enum: ['ONLINE', 'MESH', 'BOTH'], default: 'ONLINE' },
  batteryPercentage: { type: Number, min: 0, max: 100, default: null },
  status: { 
    type: String, 
    enum: ['ACTIVE', 'ACKNOWLEDGED', 'RESOLVED'], 
    default: 'ACTIVE', 
    index: true 
  },
  acknowledgedBy: { type: ObjectId, ref: 'User', default: null },
  acknowledgedByUsers: [{
    userId: { type: ObjectId, ref: 'User', required: true },
    displayName: String,
    confirmedSafe: Boolean,
    acknowledgedAt: { type: Date, default: Date.now }
  }],
  resolvedBy: { type: ObjectId, ref: 'User', default: null },
  assignedAdmin: { type: ObjectId, ref: 'User', default: null },
  notes: [{
    authorId: { type: ObjectId, ref: 'User', required: true },
    text: { type: String, required: true },
    timestamp: { type: Date, default: Date.now }
  }]
}
// Index: location -> 2dsphere for proximity search
```

#### User Schema
```javascript
{
  email: { type: String, required: true, unique: true, lowercase: true },
  passwordHash: { type: String, default: null },
  displayName: { type: String, required: true },
  authProvider: { type: String, enum: ['LOCAL', 'GOOGLE'], default: 'LOCAL' },
  googleId: { type: String, default: null },
  role: { type: String, enum: ['CITIZEN', 'ADMIN'], default: 'CITIZEN' },
  adminApproved: { type: Boolean, default: null }, // false: pending, true: approved
  phoneNumber: { type: String, default: null },
  dateOfBirth: { type: Date, default: null },
  photoUrl: { type: String, default: null },
  profileComplete: { type: Boolean, default: false },
  fcmToken: { type: String, default: null },
  lastKnownLocation: {
    type: { type: String, enum: ['Point'], default: 'Point' },
    coordinates: { type: [Number], default: null }
  },
  lastLocationAt: { type: Date, default: null }
}
```

### 5.4 REST API Contract

#### Authentication (`/api/auth`)
- `POST /register`: Create local citizen account `{ email, password, displayName }`.
- `POST /login`: Authenticate and receive signed JWT `{ email, password }`.
- `POST /google`: Verify Google Auth token and return JWT `{ idToken }`.

#### Users (`/api/users`)
- `GET /me`: Returns profile of authenticated user.
- `PUT /me`: Whitelist update of user profile fields.
- `PUT /me/complete-profile`: Sets `phoneNumber`, `dateOfBirth`, updates `profileComplete: true`.
- `PUT /me/fcm-token`: Attaches device FCM push token.

#### Emergency Services (`/api/sos`)
- `POST /`: Trigger SOS incident. *Rate limit: 2 requests per 30 seconds.*
- `GET /active`: Return list of active (`ACTIVE`, `ACKNOWLEDGED`) alerts.
- `GET /acknowledged`: Return SOS events personally confirmed safe by current user.
- `GET /:id`: Return detailed single incident.
- `PUT /:id/acknowledge`: Acknowledge incident and update safety status.
- `PUT /:id/resolve`: **Admin Only.** Mark incident as `RESOLVED`.
- `PUT /:id/assign`: **Admin Only.** Assign or reassign admin owner.
- `POST /:id/notes`: **Admin Only.** Append investigation note.

#### Admin Center (`/api/admin`)
- `GET /sos`: Query live active incidents.
- `GET /sos/history`: Query resolved incidents with pagination.
- `GET /users?q=`: Search user directory.
- `POST /admins`: Elevate citizen to admin.
- `DELETE /admins/:userId`: Revoke admin status.

#### Family Management (`/api/family`)
- `GET /links`: List all parent-child associations.
- `POST /link-request`: Parent initiates request to child's email.
- `PUT /link/:id/accept`: Child accepts link.
- `PUT /link/:id/revoke`: Either party severs relationship.
- `GET /child/:childId/location`: Security-checked query for child's latest coordinates.

### 5.5 Real-Time WebSocket Architecture (`/sos` namespace)

The backend creates a dedicated Socket.IO namespace at `/sos`. Clients subscribe on connection:
```javascript
const sosNamespace = io.of('/sos');
sosNamespace.on('connection', (socket) => {
  // Real-time listener registration
});
```

Whenever an incident state mutates, the controllers trigger real-time updates:
- **`sos:new`**: Broadcast when a new incident is created via `triggerSos`.
- **`sos:updated`**: Broadcast when an incident is acknowledged, resolved, assigned, or updated with notes.

### 5.6 Middleware & Security Guards

1. **`verifyToken.js`**: Decodes `Bearer <JWT>`, verifies HMAC signature, checks expiry, and injects payload into `req.user`.
2. **`verifyAdminRole.js`**: Re-queries MongoDB to guarantee `user.role === 'ADMIN'` and `user.adminApproved === true`. Stale JWT claims cannot bypass this check.
3. **`express-rate-limit`**: Throttles `/api/auth` (10 per 15 min) and `/api/sos` (2 per 30 sec).

---

## 6. Web Dashboard (`gridZeroExpress/frontend`)

### 6.1 Technology Stack & Next.js Architecture

- **Framework**: Next.js 16.3.5 (App Router with Server & Client components)
- **UI & State**: React 19.2.8, Tailwind CSS v4, Lucide React
- **Maps**: `@vis.gl/react-google-maps` 1.10.1
- **Real-Time Client**: `socket.io-client` 4.8.3

### 6.2 Directory Structure

```
frontend/src/
├── app/
│   ├── layout.tsx                     # Global Root Layout (Theme & Auth Providers)
│   ├── globals.css                    # Tailwind v4 theme variables
│   ├── (app)/                         # Authenticated App Route Group
│   │   ├── layout.tsx                 # App Shell (AppSidebar + Navbar)
│   │   ├── admin/page.tsx             # Master Admin Incident & Map Dashboard
│   │   ├── dashboard/page.tsx         # Citizen Overview Dashboard
│   │   ├── contacts/page.tsx          # Emergency Contact Management
│   │   ├── family/page.tsx            # Family Location & Linking Interface
│   │   ├── profile/page.tsx           # Profile Details Editor
│   │   └── sos/[id]/page.tsx          # Incident Detail Deep Link View
│   └── auth/
│       ├── login/page.tsx             # User Sign-In
│       └── register/page.tsx          # User Registration
├── components/
│   ├── AppSidebar.tsx                 # Adaptive navigation sidebar
│   ├── Navbar.tsx                     # Top bar, status indicators, theme switch
│   ├── admin/
│   │   ├── SosLiveMap.tsx             # Google Maps JavaScript SDK live incident layer
│   │   ├── MapCanvas.tsx              # Fallback Tactical Radar Canvas
│   │   ├── SosDrawer.tsx              # Incident management side-drawer
│   │   └── UserManagementModal.tsx    # Admin promotion & directory search modal
│   └── ui.tsx                         # Core primitives (Button, Card, Badge, Alert)
├── context/
│   ├── AuthContext.tsx                # Token persistence & current user state
│   └── ThemeContext.tsx               # Light/Dark mode state management
└── lib/
    └── api.ts                         # Axios client with interceptors
```

### 6.3 Component Breakdown & Admin Maps

- **`SosLiveMap.tsx`**: Renders full-screen Google Maps interface with real-time marker management:
  - Color-coded pins matching incident categories (`#EF4444` Medical, `#F97316` Disaster, etc.).
  - Accuracy circle overlays showing GPS accuracy radius.
  - Marker click listener opens `SosDrawer.tsx`.
- **`SosDrawer.tsx`**: Provides dispatchers with full incident context:
  - Dispatcher assignment dropdown.
  - Incident status transition controls (Acknowledge, Resolve).
  - Chronological note stream with interactive append tool.
- **`UserManagementModal.tsx`**: Search directory and toggle administrative roles.

### 6.4 Authentication & Context Management

- **`AuthContext.tsx`**: Stores JWT token in `localStorage`. Automatically attaches token to outbound requests via `lib/api.ts` interceptor. Redirects unauthenticated visits to `/auth/login`.
- **`ThemeContext.tsx`**: Coordinates light/dark theme preference across DOM classes.

---

## 7. Android Application (`gridzero`)

### 7.1 Technology Stack & Build Configuration

- **Language / UI**: 100% Kotlin + Jetpack Compose (Material 3)
- **Compile SDK**: 35 | **Min SDK**: 26 (Android 8.0) | **Target SDK**: 35
- **Networking**: Retrofit 2.11.0 + OkHttp 4.12.0
- **Real-Time Client**: Socket.IO Java Client 2.1.0
- **Background Sync**: AndroidX WorkManager 2.9.1
- **Maps**: Google Maps Compose 4.4.1 + Play Services Maps 18.2.0
- **Secrets Management**: Dynamic `resValue` injection via `local.properties` (never committed to VCS).

### 7.2 Module Architecture & Package Structure

```
com.example.zerogrid/
├── admin/                         # Admin module (Map, History, User Management)
├── auth/                          # Login, Register, Profile Completion screens
├── contacts/                      # Emergency contacts screen & ViewModel
├── debug/                         # In-memory logging ring buffer & console viewer
├── emergency/                     # SOS beacons, System Alert Overlay, Safety Agent & Safe Route Planner
│   ├── HazardOverlayView.kt       # Full-screen system alert window with vehicle risk & detour
│   ├── OverlayAlertManager.kt     # SYSTEM_ALERT_WINDOW manager & ringtone/vibrator controller
│   ├── RouteSafetyAgent.kt        # Rule-based safety agent & corridor risk evaluator
│   ├── SafeRoutePlannerDialog.kt  # Destination search & safe route execution modal
│   ├── SendSosScreen.kt           # SOS trigger UI
│   ├── SosCenterScreen.kt         # Live emergency incident feed
│   ├── SosUploadWorker.kt         # WorkManager offline queue worker
│   └── UnifiedSosDispatcher.kt    # Dual mesh/cloud broadcast dispatcher
├── family/                        # Family links screen & ViewModel
├── hardware/                      # BLE / Wi-Fi hardware state monitor & banner
├── home/                          # MeshDashboardScreen with Nearby Hazards Radar Card
├── location/                      # Geolocation & Proximity services
│   ├── HazardCacheManager.kt      # In-memory & DataStore 10km hazard cache manager
│   ├── HazardProximityMonitor.kt  # 60s background proximity geofence monitor
│   ├── LocationHelper.kt          # Multi-provider GPS provider with Samsung FLP optimizations
│   ├── LocationSearchHelper.kt    # Unified geocoder & OpenStreetMap destination search
│   └── VehicleRiskCalculator.kt   # Vehicle clearance vs flood depth matrix
├── mesh/                          # Mesh routing engine, packets, BLE & Wi-Fi Direct drivers
├── messaging/                     # 1:1 direct chat, channel broadcast chat & MessageStore
├── navigation/                    # NavGraph, Routes, Bottom Navigation bar
├── network/                       # Retrofit client, SosApiService, OsrmRoutingService
├── onboarding/                    # Splash, Permissions, Create Identity screens
├── profile/                       # Profile management screen
├── service/                       # MeshForegroundService (Type: Location) & FCM Listener
├── settings/                      # Settings & security preferences
├── ui/                            # Design system, themes & shared components
│   └── components/                # ProximityWarningBanner, NearbyHazardsRadarCard, SafeRoutePreviewMap
└── util/                          # MapsIntentBuilder & validation helpers
```

### 7.3 Navigation System & Custom Pager Stack

The application employs a custom high-performance navigation architecture:
- **Root Screen (`NavGraph.kt`)**: Hosts an optimized `HorizontalPager` with `beyondViewportPageCount = 1` for instantaneous tab switching between:
  1. `MeshDashboardScreen` (Page 0)
  2. `MessagesScreen` (Page 1)
  3. `SosCenterScreen` (Page 2)
  4. `SettingsScreen` (Page 3)
- **Sub-Screen Stack**: Sub-screens (`PEER_DIRECT_CHAT`, `SEND_SOS`, `TRACK_SOS`, `ADMIN_PANEL`, etc.) are pushed onto an internal `subScreenStack` overlay rendered with an `AnimatedContent` horizontal slide transition (WhatsApp-style).
- **Back-Press Arbitration**:
  1. If `subScreenStack` is non-empty, pops top screen.
  2. If on root tab index > 0, smoothly animates pager back to Home (Tab 0).
  3. If on Home, allows Android system back to minimize the app.

### 7.4 Module Deep-Dives

#### Core Mesh Engine & Hardware Managers
- **`MeshEngine.kt`**: Central singleton orchestrator. Holds `StateFlow` streams for `connectedPeers`, `conversations`, `sosAlerts`, and `acknowledgedAlertIds`.
- **`HardwareStateManager.kt`**: Continuously monitors hardware radios (Bluetooth, Wi-Fi, Location) and drives `HardwareRequirementBanner.kt`.

#### Emergency Dispatcher & WorkManager Offline Sync
- **`UnifiedSosDispatcher.kt`**: Receives incident requests from `SendSosScreen`. Performs parallel dispatch:
  1. Transmits `MeshPacket(type = PacketType.SOS_BEACON)` over local radios via `MeshEngine`.
  2. Evaluates Internet availability via `ConnectivityChecker`. If online, immediately invokes `SosApiService.triggerSos()`. If offline, enqueues `SosUploadWorker` in WorkManager.
- **`SosUploadWorker.kt`**: Executes in the background with `BackoffPolicy.EXPONENTIAL`. Retries cloud upload automatically upon network reconnection.

#### Proactive Hazard Intelligence & Autonomous Safety Agent
- **`HazardProximityMonitor.kt` & `HazardCacheManager.kt`**: Operates a zero-network 60-second background geofence evaluation against active local hazards (waterlogging, power lines, structural failures) within 10 km.
- **`OverlayAlertManager.kt` & `HazardOverlayView.kt`**: Spawns a high-priority `SYSTEM_ALERT_WINDOW` over any application or lockscreen whenever a user enters the 200m danger buffer of a verified hazard.
- **`RouteSafetyAgent.kt` & `OsrmRoutingService.kt`**: Evaluates travel corridors against infected hazard zones in the local database, generates orthogonal evasion waypoints, queries OSRM driving geometry, and classifies route impact into **Avoided** vs. **Unavoidable** hazards.
- **`SafeRoutePreviewMap.kt`**: Renders an in-app interactive Google Map preview with polylines, danger circles, and evasion waypoints before triggering turn-by-turn navigation in Google Maps via `MapsIntentBuilder.kt`.

#### Mobile Admin Panel & Tactical Radar / Google Maps
- **Access Gate**: Admin Panel button in `SettingsScreen` is conditionally displayed only when `AuthRepository.userRole == "ADMIN"` and `adminApproved == true`.
- **`AdminGoogleMapView.kt`**: Seamlessly switches between:
  - **Google Maps Mode**: Renders `GoogleMap` composable with custom markers and camera animation.
  - **Tactical Radar Mode (`TacticalRadarCanvas.kt`)**: Zero-dependency `Canvas` radar screen drawing concentric range rings and blips relative to device orientation and GPS coordinates.
- **`SosDetailBottomSheet.kt`**: Admin modal allowing dispatchers to claim ownership, log investigation notes, and resolve incidents.

#### Identity, Auth, Contacts & Family Linking
- **`CreateIdentityScreen.kt`**: Allows users to configure an offline mesh handle and avatar before internet registration.
- **`EmergencyContactsScreen.kt` & `FamilyLinksScreen.kt`**: Integrates with backend endpoints to manage emergency contacts and track child locations.

#### Background Services
- **`MeshForegroundService.kt`**: Operates as a persistent foreground service with `FOREGROUND_SERVICE_TYPE_LOCATION`, maintaining BLE discovery and location monitoring while the device is locked or minimized.
- **`ZeroGridFirebaseMessagingService.kt`**: Captures high-priority FCM emergency pushes, instantiating high-priority system alert notifications.

---

## 8. End-to-End SOS Lifecycle Workflow

```
[CITIZEN SENDS SOS]
       │
       ▼
SendSosScreen.kt
  │ - Captures category, message, coordinates, battery %
  │ - Invokes UnifiedSosDispatcher.dispatchSos(...)
  │
  ├──► [PATH A: OFFLINE MESH]
  │      │
  │      ▼
  │    MeshEngine.sendPacket(MeshPacket(type = SOS_BEACON))
  │      │
  │      ├─► BleMeshDriver transmits over BLE advertisements & GATT
  │      └─► WifiDirectMeshDriver streams over P2P sockets
  │            │
  │            ▼
  │    Nearby Peer Android Device
  │      │ - DeduplicationCache confirms packet is new
  │      │ - Decrements TTL, increments hop count, relays packet to other peers
  │      └─► SosCenterScreen alerts nearby citizen on-screen
  │
  └──► [PATH B: CLOUD UPLINK]
         │
         ├── Online? ──► Direct POST /api/sos ────────────────────────────┐
         └── Offline? ──► Enqueue SosUploadWorker (WorkManager)            │
                            │                                              │
                            ▼ (Upon connection)                            │
                          POST /api/sos ◄──────────────────────────────────┘
                            │
                            ▼
                  [EXPRESS BACKEND]
                    │ - Validates JWT & rate limits
                    │ - Saves SosEvent to MongoDB with 2dsphere coordinates
                    │ - Emits WebSocket event 'sos:new' on namespace /sos
                    │ - Triggers FCM Push to family contacts
                    │
                    ├──► [WEB ADMIN CONSOLE]
                    │      │ - Socket.IO captures 'sos:new'
                    │      └─► SosLiveMap drops alert marker & sounds chime
                    │
                    └──► [ANDROID ADMIN PANEL]
                           │ - AdminSocketManager captures 'sos:new'
                           └─► AdminSosRepository appends to StateFlow; IncidentCard appears

[ADMIN RESOLUTION]
  Dispatcher reviews incident in SosDetailBottomSheet.kt
  │
  ▼
  PUT /api/sos/:id/resolve
    │ - Updates status to 'RESOLVED' in MongoDB
    │ - Backend broadcasts 'sos:updated' over WebSocket
    │ - Marker moves to History view across Web and Mobile consoles
```

---

## 9. Security, Roles & Permission Model

| Security Dimension | Implementation Mechanism | Purpose |
|---|---|---|
| **API Transport** | TLS 1.3 / HTTPS (Enforced on Render) | Protects credentials and incident coordinates in transit. |
| **Authentication** | JWT (HMAC-SHA256), 7-day expiration | Stateless session management for REST and WebSockets. |
| **Password Storage**| `bcrypt` with salt rounds = 12 | Secure irreversible credential hashing. |
| **Role Verification**| `verifyAdminRole.js` (fresh DB check) | Eliminates token-forgery and privilege escalation risks. |
| **Rate Limiting** | `express-rate-limit` (2 req / 30s for SOS) | Prevents denial-of-service and accidental duplicate submissions. |
| **Geospatial Queries** | MongoDB `2dsphere` indexes | High-performance spatial querying without exposing raw table scans. |
| **Maps API Secret** | Android `local.properties` + dynamic `resValue` | Prevents credential leaks in source code repositories. |
| **Device Permissions** | `BLUETOOTH_SCAN`, `BLUETOOTH_ADVERTISE`, `ACCESS_FINE_LOCATION` | Required by Android OS for BLE mesh discovery and GPS tagging. |

---

## 10. Setup, Execution & Configuration Guide

### 10.1 Backend Setup (`gridZeroExpress/backend`)

1. Navigate to directory:
   ```bash
   cd gridZeroExpress/backend
   ```
2. Create environment configuration file `.env`:
   ```env
   PORT=5000
   NODE_ENV=development
   MONGODB_URI=mongodb+srv://<username>:<password>@cluster.mongodb.net/zerogrid?retryWrites=true&w=majority
   JWT_SECRET=your_super_secret_jwt_key_at_least_32_chars_long
   ALLOWED_ORIGINS=http://localhost:3000
   GOOGLE_CLIENT_ID=your-google-oauth-client-id.apps.googleusercontent.com
   ```
3. Install dependencies and start server:
   ```bash
   npm install
   npm run dev
   ```
4. Verify readiness:
   - Health check: `http://localhost:5000/health`
   - WebSocket endpoint: `ws://localhost:5000/sos`

### 10.2 Web Frontend Setup (`gridZeroExpress/frontend`)

1. Navigate to directory:
   ```bash
   cd gridZeroExpress/frontend
   ```
2. Create `.env.local`:
   ```env
   NEXT_PUBLIC_API_URL=http://localhost:5000
   NEXT_PUBLIC_GOOGLE_MAPS_API_KEY=AIzaSy...
   ```
3. Install dependencies and start Next.js:
   ```bash
   npm install
   npm run dev
   ```
4. Access interface at `http://localhost:3000`.

### 10.3 Android Client Setup (`gridzero`)

1. Open project `c:\Users\ACER\AndroidStudioProjects\gridzero` in Android Studio.
2. In the root directory, create/update `local.properties`:
   ```properties
   sdk.dir=C:\\Users\\ACER\\AppData\\Local\\Android\\Sdk
   MAPS_API_KEY=AIzaSy...
   ```
3. Ensure `google-services.json` is located in `app/`.
4. Configure target backend in `app/build.gradle.kts`:
   - For Android Emulator connecting to local server: `buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:5000/\"")`
   - For physical device on local Wi-Fi: `buildConfigField("String", "BASE_URL", "\"http://192.168.x.x:5000/\"")`
   - For deployed cloud server: `buildConfigField("String", "BASE_URL", "\"https://zerogridweb.onrender.com/\"")`
5. Sync Gradle and run on device or emulator (Android 8.0+ / API 26+).

---

## 11. Troubleshooting & Known Architecture Notes

| Observed Behavior | Root Cause | Solution / Architecture Note |
|---|---|---|
| **High Logcat frame invalidation (`gralloc4`)** | Infinite transition animations recalculating layout every frame. | Replaced infinite transitions in `AdminTopBar.kt` with static indicator chips. |
| **Logcat flooded with large payloads** | `HttpLoggingInterceptor` set to `Level.BODY`. | Lowered to `Level.BASIC` in `RetrofitInstance.kt`. |
| **Offline SOS double-upload** | Both `UnifiedSosDispatcher` and `SosUploadWorker` executing simultaneously upon rapid network re-association. | Guarded with state check and idempotency token. |
| **Google Maps blank on Android** | Missing or unauthorized Maps API Key in Google Cloud Console. | Verify `MAPS_API_KEY` in `local.properties` has `Maps SDK for Android` enabled. |
| **Android BLE Advertising fails (`ADVERTISE_FAILED_FEATURE_UNSUPPORTED`)** | Android Emulator lacks hardware BLE peripheral mode. | Run on physical Android device for mesh advertising and scanning testing. |
| **Samsung FLP listener failure (`10416_FINE_fg_svc_false_foreground`)** | Android 14+ requires foreground services accessing location to declare `android:foregroundServiceType="location"`. | Declared `FOREGROUND_SERVICE_LOCATION` in manifest and passed `FOREGROUND_SERVICE_TYPE_LOCATION` in `MeshForegroundService.startForeground()`. |
| **Overlay text keyboard inaccessible** | `WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE` prevented soft keyboard input in system alert overlay. | Removed `FLAG_NOT_FOCUSABLE` and set `softInputMode = SOFT_INPUT_ADJUST_RESIZE` in `OverlayAlertManager.kt`. |
| **Background location stall on Samsung devices** | Samsung power management suspends standard network/fused listeners when app is minimized. | Polled `LocationManager.PASSIVE_PROVIDER` alongside `FUSED_PROVIDER` and `getCurrentLocation()` for API 30+ in `LocationHelper.kt`. |

---

## 12. Proactive Hazard Intelligence & Autonomous Safe Route Engine

ZeroGrid integrates a proactive disaster intelligence pipeline that warns users of impending localized hazards (waterlogging, live electrical wire drops, fallen trees, structural collapses) before they enter danger zones, and orchestrates safety-verified evacuation routes using Open Source Routing Machine (OSRM) and an autonomous safety agent.

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                        TWO COMPANION ENTRY POINTS (SAME PIPELINE)                      │
│                                                                                        │
│   [A] Home Dashboard: "Hazard Radar"          [B] Proactive Geofence System Alert      │
│       - Active hazards within 10 km               - 60s background monitor loop        │
│       - "Plan Safe Route with Agent" button       - Triggers within 200m of hazard     │
└───────────────────────────────────────────┬────────────────────────────────────────────┘
                                            │
                                            ▼
┌────────────────────────────────────────────────────────────────────────────────────────┐
│ 1. REAL-TIME COORDINATE ACQUISITION                                                    │
│    • Origin: Verified GPS coordinates via LocationHelper.kt                            │
│    • Destination: LocationSearchHelper.kt (Geocoder + OSM Nominatim) OR Map Pin Tap    │
└───────────────────────────────────────────┬────────────────────────────────────────────┘
                                            │
                                            ▼
┌────────────────────────────────────────────────────────────────────────────────────────┐
│ 2. AUTONOMOUS ROUTE SAFETY AGENT (RouteSafetyAgent.kt + OsrmRoutingService.kt)         │
│    • Ingests active hazards from HazardCacheManager.kt (Local DB & DataStore)          │
│    • Performs spatial corridor collision check (cross-track distance < 160m)           │
│    • Computes orthogonal evasion waypoints (+350m offset around hazard centers)        │
│    • Queries OSRM for driving polyline; falls back to piecewise geodesic interpolation │
│    • Dual-Category Risk Classification:                                                │
│      ├─ 🛡 HAZARDS AVOIDED BY GRIDZERO (successfully bypassed by detour)               │
│      └─ ⚠️ UNAVOIDABLE CONDITIONS TO FACE ANYWAY (residual hazards + survival tips)    │
└───────────────────────────────────────────┬────────────────────────────────────────────┘
                                            │
                                            ▼
┌────────────────────────────────────────────────────────────────────────────────────────┐
│ 3. IN-APP INTERACTIVE ROUTE PREVIEW (SafeRoutePreviewMap.kt)                           │
│    • Embedded GoogleMap showing Origin (🟢), Destination (🏁), Route Polyline (🔵)    │
│    • Danger Circles (🔴/🟢 100-150m) around hazard centers + Detour Waypoints (🟡)     │
│    • Metrics Bar: Distance (km) and Travel Time (mins)                                 │
│    • Dual breakdown cards (Avoided vs. Unavoidable Hazards)                            │
└───────────────────────────────────────────┬────────────────────────────────────────────┘
                                            │
                                            ▼
┌────────────────────────────────────────────────────────────────────────────────────────┐
│ 4. GOOGLE MAPS NAVIGATION HANDOFF (MapsIntentBuilder.kt)                               │
│    • [ 🗺 Open in Google Maps Navigation ] button deep-links to Google Maps with       │
│      evasion waypoints pre-encoded, enforcing safe navigation along the verified path  │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

### 12.1 System Alert Overlay & Proximity Geofencing (`SYSTEM_ALERT_WINDOW`)

- **`HazardProximityMonitor.kt`**: Operates a lightweight coroutine loop tied to `MeshForegroundService.kt` that wakes every 60 seconds. Reads the user's last-known location and queries `HazardCacheManager.getCachedHazards()`. Generates **zero network calls** and **zero battery wake-locks** during routine ticks.
- **`OverlayAlertManager.kt`**: When distance $\le 200\text{ m}$ to any active hazard, it inflates `HazardOverlayView.kt` via `WindowManager.addView()` as `TYPE_APPLICATION_OVERLAY`. Plays an emergency alert ringtone with a distinct vibration pattern (`0, 400, 200, 400, 800`).
- **Interactive Multi-Step Overlay Flow**:
  1. **`WARNING`**: Displays hazard type, water depth, and distance with dismiss `✕` button.
  2. **`VEHICLE_SELECT`**: Prompts user for vehicle type (Sedan, Hatchback, SUV, 4x4, Motorcycle, Walking).
  3. **`RISK_RESULT`**: Evaluates vehicle clearance against water depth via `VehicleRiskCalculator.kt` (SAFE, CAUTION, HIGH RISK, IMPASSABLE).
  4. **`DEST_INPUT`**: Interactive destination search with autocomplete and tap-to-pin Google Map.
  5. **`FETCHING_ROUTE`**: Safety Agent runs corridor analysis and OSRM routing.
  6. **`ROUTE_READY`**: Renders full in-app route map preview with avoided vs. unavoidable hazard breakdown.

### 12.2 Offline Hazard Cache Manager & Monitored Hazard Categories

- **`HazardCacheManager.kt`**: Implements a hybrid memory + AndroidX DataStore cache of verified hazard SOS events within 10 km.
- **Monitored Hazard Categories**:
  - `WATERLOGGING` / `DRAINAGE_OVERFLOW` (with recorded water depth in cm)
  - `SUBMERGED_UNDERPASS`
  - `FALLEN_POWERLINE` / `LIVE_WIRE`
  - `POWER_OUTAGE` (Blackout zones)
  - `FALLEN_TREE`
  - `STRUCTURAL_COLLAPSE` (Building and debris collapse)
  - `ROAD_BLOCKAGE`
- **Cache Refresh Triggers**:
  1. Network connectivity restored (`ConnectivityChecker` callback).
  2. Every 5 minutes while online.
  3. When user drifts $> 1\text{ km}$ from the last fetch origin (re-centers the 10 km window).

### 12.3 Home Screen Nearby Hazard Radar Card

- **`NearbyHazardsRadarCard.kt`**: Embedded prominently on `MeshDashboardScreen.kt`.
- **Features**:
  - Displays total active hazard count in the 10 km radius.
  - Lists the 3 closest hazards sorted by proximity with category icons (`⚡`, `🌊`, `🌲`, `🛑`), distance badges (`180m away`, `1.2km away`), and risk levels (`CRITICAL`, `HIGH`, `CAUTION`).
  - Empty state when clear: `🛡 Area Safe — 0 Active Hazards in 10 km`.
  - Prompts users with a direct CTA: `🧭 Plan Safe Route with Agent →`.

### 12.4 Autonomous Route Safety Agent (Rule-Based & Pluggable AI Contract)

- **`RouteSafetyAgent.kt`**: Implements a clean, decoupled interface designed for immediate deterministic execution and seamless future migration to conversational LLM / AI agents:
  ```kotlin
  interface RouteSafetyAgent {
      suspend fun executeSafeRoute(
          origin: LatLng,
          destination: LatLng,
          cachedHazards: List<CachedHazard>
      ): RouteSafetyReport
  }
  ```
- **`RuleBasedRouteSafetyAgent` Implementation**:
  1. **Corridor Intersection**: Computes minimum distance from every cached hazard to the direct segment connecting Origin and Destination. Hazards within 160 meters are flagged as conflicting.
  2. **Evasion Waypoint Generation**: For top-priority conflicting hazards, computes detour coordinates shifted orthogonally ($\pm 90^\circ$ relative to the travel bearing) by 350 meters around the hazard perimeter.
  3. **OSRM Route Execution**: Queries OSRM with `[Origin, Waypoint_1, ..., Destination]`.
  4. **Post-Route Classification**: Categorizes every local hazard into Avoided or Unavoidable lists based on the final polyline distance.

### 12.5 OSRM Driving Route Engine & Offline Interpolation Fallback

- **`OsrmRoutingService.kt`**: Executes driving routes against OSRM (`/route/v1/driving/{coords}?overview=full&geometries=geojson`).
- Parses GeoJSON LineString coordinates into `List<LatLng>`, extracting total distance (meters) and duration (seconds).
- **Offline Interpolation Fallback**: If offline or OSRM is unreachable, generates smooth piecewise geodesic interpolation points between checkpoints, ensuring navigation preview never fails.

### 12.6 Dual-Category Risk Classification: Avoided vs. Unavoidable Hazards

The Safety Agent produces two distinct, actionable lists presented to the user:
1. 🛡 **Hazards Avoided Using GridZero:**
   - Hazards that were in the original direct travel corridor but were successfully circumvented by the calculated detour.
   - Example: *`⚡ Live Power Cable Drop — Circumvented by +420m via detour corridor`*
   - Example: *`🌊 Waterlogging (45 cm) — Bypassed via flyover route`*
2. ⚠️ **Unavoidable Conditions to Face Anyway:**
   - Residual hazards that remain near the safe route (e.g., at user's starting point, near the destination gate, or narrow choke points).
   - Generates tactical survival guidance based on hazard type and severity.
   - Example: *`🌊 Waterlogging (15 cm) — Moderate pooling within 80m. Keep speed under 20 km/h in low gear.`*
   - Example: *`⚡ Live Power Cable Drop — High voltage risk within 90m. Do NOT step out of vehicle.`*

### 12.7 In-App Visual Route Preview & Google Maps Navigation Handoff

- **`SafeRoutePreviewMap.kt`**: Shared Compose component utilized by both the Emergency Overlay and Home Safe Route Planner.
- Renders an interactive `GoogleMap` directly on-screen before launching external apps:
  - 🟢 **Origin Pin** (You)
  - 🏁 **Destination Pin**
  - 🔵 **Safe Route Polyline** (OSRM path)
  - 🔴/🟢 **Danger Circles** (100m–120m circular hazard buffers)
  - 🟡 **Evasion Waypoint Pins**
- Below the map: Metrics bar (distance in km, duration in mins), Agent reasoning summary, and cards for Avoided and Unavoidable hazards.
- **`MapsIntentBuilder.kt`**: Launches the Google Maps application with intermediate waypoints (`&waypoints=lat1,lng1|lat2,lng2`), forcing Google Maps turn-by-turn navigation to follow GridZero's safe detour corridor.

