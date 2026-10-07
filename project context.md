# ZeroGrid: Complete Project Context & Architecture Master Document 🌐

> **Decentralized, Off-Grid Peer-to-Peer Mesh Communication & Disaster Emergency Network for Android**  
> **Repository:** `gridzero` / `zerogrid`  
> **Target Platform:** Android 8.0 (API 26) to Android 15 (API 35)  
> **Document Purpose:** Single source of truth documenting current progress, full technical architecture, code breakdown, transport protocols, and active engineering roadmap.

---

## 📋 Table of Contents

1. [Executive Summary & Core Philosophy](#1-executive-summary--core-philosophy)
2. [Current Progress & Implementation Status Matrix](#2-current-progress--implementation-status-matrix)
3. [System Architecture & Data Flow](#3-system-architecture--data-flow)
4. [File & Directory Map](#4-file--directory-map)
5. [Core Engine Deep Dive](#5-core-engine-deep-dive)
6. [Transport Layer Protocols (BLE & Wi-Fi)](#6-transport-layer-protocols-ble--wi-fi)
7. [Interface Resolution & Dynamic Routing](#7-interface-resolution--dynamic-routing)
8. [UI/UX Architecture & Screen Inventory](#8-uiux-architecture--screen-inventory)
9. [Message Persistence & Store-and-Forward](#9-message-persistence--store-and-forward)
10. [Emergency SOS Beacon Subsystem](#10-emergency-sos-beacon-subsystem)
11. [Wire Protocol & Packet Specifications](#11-wire-protocol--packet-specifications)
12. [Native C++ Layer (NDK / JNI)](#12-native-c-layer-ndk--jni)
13. [Permissions & Android OS Integration](#13-permissions--android-os-integration)
14. [Testing Suite & Verification Status](#14-testing-suite--verification-status)
15. [Identified Gaps, Technical Debt & Next Steps](#15-identified-gaps-technical-debt--next-steps)

---

## 1. Executive Summary & Core Philosophy

**ZeroGrid** is an Android communication system engineered to operate without cell towers, internet service providers, satellite relays, or centralized servers. It creates an ad-hoc, multi-hop mesh network directly between mobile devices using:

- **Bluetooth Low Energy (BLE)** for continuous, low-power neighbor discovery and lightweight messaging.
- **Wi-Fi Direct (P2P) & Local WLAN/Hotspot (UDP/TCP)** for high-bandwidth messaging, file sharing, and faster routing.

### Primary Use Cases
1. **Natural Disasters:** Grid collapse, earthquakes, floods, wildfires, hurricanes where cellular towers lose power or backhaul.
2. **Remote Operations:** Wilderness expeditions, hiking, maritime trips, subterranean or remote rural areas without signal.
3. **Crisis & Emergency Relief:** Real-time SOS telemetry, emergency broadcast channels, and triage coordination between authorities and affected populations.
4. **Privacy & Censorship Resistance:** Localized, peer-governed direct messaging and public broadcast channels.

### Core Architecture Highlights
- **Zero Cloud Reliance:** All packet routing, peer resolution, deduplication, and persistence happen on-device.
- **Store-and-Forward Flooding:** Packets travel across intermediate relay nodes using bounded TTL and hop tracking. If a destination node is temporarily out of range, the sender queues and retries delivery upon reconnection.
- **Dual Gateway Entry Experience:** Provides both tailored command interfaces (Citizen Panel vs. Authority Command Panel) and a full cybernetic multi-screen mesh operations hub.

---

## 2. Current Progress & Implementation Status Matrix

| Component / Subsystem | Status | Verified Technical Details |
| :--- | :---: | :--- |
| **Android Shell & Gradle** | ✅ **COMPLETED** | Gradle Kotlin DSL, Version Catalog (`libs.versions.toml`), Target SDK 35, Min SDK 26, JVM 17. |
| **Foreground Service Daemon** | ✅ **COMPLETED** | `MeshForegroundService` (`connectedDevice` type), persistent notification, survives app backgrounding. |
| **Dual Role Gateway UI** | ✅ **COMPLETED** | `RoleSelectionScreen`, `NameEntryScreen`, `UserDashboardScreen` (Citizen), `AuthorityDashboardScreen`. |
| **Cybernetic Design System** | ✅ **COMPLETED** | Dark cybernetic theme (`#0D1117`, Neon Cyan `#00E5FF`, Danger Red `#FF3B30`, Amber `#FF9500`). |
| **20 Full Mesh UI Screens** | ✅ **COMPLETED** | Complete Compose Material 3 implementation with reactive `StateFlow` binding and back-stack handling. |
| **BLE Mesh Driver** | ✅ **COMPLETED** | Adv/Scan, GATT Server, GATT Client with MTU 512, frame chunking/reassembly, RPA mapping, scan-pause. |
| **Wi-Fi Mesh Driver** | ✅ **COMPLETED** | Dual mode: Wi-Fi Direct P2P DNS-SD + Local WLAN/Hotspot UDP broadcast (8889) and TCP stream (8888). |
| **Dynamic Interface Resolver** | ✅ **COMPLETED** | `MeshPeerResolver`: autonomous RSSI weighting, throughput scoring, multi-interface fallback. |
| **Routing Engine** | ✅ **COMPLETED** | `MeshRoutingEngine`: TTL decrement, hop count increment, source transport exclusion, loop prevention. |
| **Deduplication Cache** | ✅ **COMPLETED** | `DeduplicationCache`: Thread-safe time-bounded (5 min) LRU packet cache preventing duplicate loops. |
| **Peer Table Management** | ✅ **COMPLETED** | `PeerTable`: Thread-safe registry tracking RSSI, hop count, available interfaces, 90s stale pruning. |
| **Direct Messaging (DM)** | ✅ **COMPLETED** | 1-on-1 chats, ACK delivery confirmations, peer status tracking (`SENT`, `DELIVERED`, `PAUSED`). |
| **Broadcast Channels** | ✅ **COMPLETED** | Public broadcast channels (`#general`, `#emergency`, `#camp-alpha`), multi-hop flood propagation. |
| **Local Message Store** | ✅ **COMPLETED** | `MessageStore`: SharedPreferences JSON storage surviving restarts and reconnection cycles. |
| **Store-and-Forward Retries** | ✅ **COMPLETED** | Background 15s retry loop in `MeshEngine` delivering `PAUSED` messages when peers reappear. |
| **Emergency SOS Subsystem** | ✅ **COMPLETED** | High-priority TTL=10 broadcast, heads-up system notification, full-screen global modal alert dialog. |
| **Debug Console & Logging** | ✅ **COMPLETED** | In-app live rolling log buffer (`DebugLogger`), terminal console screen with severity filtering. |
| **Unit Test Suite** | ✅ **COMPLETED** | Unit tests for UUIDs, packet framing/serialization, and transport byte-level reassembly. |
| **End-to-End Encryption (E2EE)**| ⏳ **PLANNED** | Cryptographic key exchange (Noise Protocol / Signal Double Ratchet); placeholder fingerprint today. |
| **SQLCipher / Room DB** | ⏳ **PLANNED** | Current storage uses JSON SharedPreferences; Room + SQLCipher migration planned for large data. |
| **File Chunk Transfer Protocol**| ⏳ **PARTIAL** | UI screens ready (`SendFileScreen`, `FileTransferScreen`); chunked binary transport protocol pending. |
| **C++ Native Crypto Core** | ⏳ **SCAFFOLD** | CMakeLists.txt and `native-lib.cpp` configured; ready for libsodium or native cryptography hooks. |

---

## 3. System Architecture & Data Flow

### High-Level Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                       USER INTERFACE LAYER                                      │
│  ┌────────────────────────────────────────┐     ┌─────────────────────────────────────────────┐  │
│  │         Role-Based Gateway UI          │     │          Full Mesh Application (20 Screens) │  │
│  │   RoleSelection  ->  NameEntry         │     │   Dashboard • Messages • Direct Chat        │  │
│  │   UserDashboard  /   AuthorityDashboard│     │   Channels • SOS Center • Files • Settings  │  │
│  └───────────────────┬────────────────────┘     └──────────────────────┬──────────────────────┘  │
└──────────────────────┼─────────────────────────────────────────────────┼────────────────────────┘
                       │                                                 │
                       ▼                                                 ▼
┌─────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                    APPLICATION ENGINE FACADE                                    │
│                                           MeshEngine                                            │
│   • Local Identity & Prefs       • Active StateFlow Streams       • 10s Peer Announce Loop      │
│   • Conversation Store Bridge    • High-Priority SOS Dispatch     • 15s Store-and-Forward Loop  │
└──────────────────────┬─────────────────────────────────────────────────┬────────────────────────┘
                       │                                                 │
        ┌──────────────┴──────────────┐                   ┌──────────────┴──────────────┐
        ▼                             ▼                   ▼                             ▼
┌────────────────────────┐  ┌───────────────────┐  ┌─────────────────────┐  ┌───────────────────┐
│   MeshRoutingEngine    │  │ MeshPeerResolver  │  │    MessageStore     │  │     PeerTable     │
│ • TTL & Hop Decrement  │  │ • Multi-Interface │  │ • Persistent JSON   │  │ • Thread-Safe     │
│ • Source Exclusion     │  │   Signal Quality  │  │   SharedPreferences │  │   Active Registry │
│ • Local Deliver First  │  │ • Auto Interface  │  │ • Status Lifecycle  │  │ • 90s Stale Pruning│
│ • DeduplicationCache   │  │   Failover Logic  │  │   (PAUSED/SENT/ACK) │  │ • RSSI & Hops Track│
└───────────┬────────────┘  └─────────┬─────────┘  └─────────────────────┘  └───────────────────┘
            │                         │
            └───────────┬─────────────┘
                        ▼
┌─────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                     TRANSPORT DRIVER LAYER                                      │
│                                      << MeshTransport >>                                        │
│               ┌─────────────────────────────────┴─────────────────────────────────┐             │
│               ▼                                                                   ▼             │
│      BleMeshDriver (BLE)                                         WifiDirectMeshDriver (Wi-Fi)   │
│ • BLE Advertiser & Scanner                                      • Wi-Fi Direct P2P DNS-SD       │
│ • GATT Server (Notify Characteristic)                           • UDP Broadcast Discovery (8889)│
│ • GATT Client (Write Characteristic)                            • TCP Stream Framing (8888)     │
│ • 512 MTU Negotiation + Frame Chunking                          • Concurrent Socket Pool        │
│ • NodeID-to-MAC RPA Dynamic Mapping                             • Local WLAN/Hotspot Support    │
└─────────────────────────────────────────────────────────────────────────────────────────────────┘
```

### Packet Traversal Flowchart (Inbound & Outbound)

```
[OUTBOUND PACKET]
       │
       ▼
 MeshEngine.sendDirectMessage() / triggerSosBeacon() / broadcastChannelMessage()
       │
       ▼
 MeshRoutingEngine.sendOutboundPacket()
       │
       ├─► DeduplicationCache.record(packetId) [Mark as seen locally]
       │
       ▼ Is Destination Unicast?
      / \
    YES  NO (Broadcast '*')
    /     \
   │       └─► Transmit to ALL running transports (BLE + Wi-Fi)
   ▼
 MeshPeerResolver.getBestRouteForDevice(recipientId)
   │
   ├─► Evaluates RSSI, freshness, and interface throughput weight
   │
   ├─► Try Preferred Transport (e.g. Wi-Fi Direct) ──(Success)──► [RADIO TRANSMIT]
   │                      │
   │                   (Fails)
   │                      ▼
   └─► Automatic Fallback to Secondary Transport (BLE) ────────► [RADIO TRANSMIT]

-----------------------------------------------------------------------------------------

[INBOUND PACKET]
       │
       ▼
 Physical Radio (BLE GATT Event or Wi-Fi TCP Stream)
       │
       ▼
 MeshTransport.packetFlow.emit(packet)
       │
       ▼
 MeshRoutingEngine.processInboundPacket()
       │
       ├─► [Check 0] Is packet.senderId == localNodeId? ──────► DROP (Echo prevention)
       ├─► [Check 1] DeduplicationCache.isDuplicate()? ───────► DROP (Loop prevention)
       │
       ├─► [Check 2] Is packet for ME or BROADCAST?
       │        │
       │       YES ──► Emit to MeshEngine.incomingPackets Flow
       │               ├─► If SOS: Trigger Heads-up Notification & Dialog
       │               ├─► If DM: Persist to MessageStore & Send ACK Packet
       │               ├─► If ACK: Update Message status to DELIVERED
       │               └─► If Channel: Append to received stream
       │
       └─► [Check 3] Should packet be relayed?
                │
         (isBroadcast || !forMe) && (packet.ttl > 1)
                │
               YES ──► RelayedPacket = packet.copy(ttl = ttl - 1, hopCount = hopCount + 1)
                       Relay to ALL active transports EXCEPT sourceTransport!
```

---

## 4. File & Directory Map

```
c:\Users\ACER\AndroidStudioProjects\gridzero\
├── README.md                              # Main repository overview & quickstart
├── BACKEND_INTEGRATION_GUIDE.md           # Backend technical reference & specifications
├── project context.md                     # THIS FILE — Master all-in-one project context
├── build.gradle.kts                       # Root project build configuration
├── settings.gradle.kts                    # Module declarations & repository definitions
├── gradle.properties                      # JVM args, AndroidX toggles
├── gradle/libs.versions.toml              # Version catalog (Compose BOM, Kotlin, AndroidX)
│
└── app/
    ├── build.gradle.kts                   # Application module build config (API 35, NDK, C++)
    ├── proguard-rules.pro                 # R8 / Proguard rules
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml        # System permissions, Activity, Foreground Service
        │   ├── cpp/                       # Native C++ layer
        │   │   ├── CMakeLists.txt         # CMake build instructions
        │   │   └── native-lib.cpp         # JNI bridge entrypoint
        │   │
        │   ├── res/                       # Resource icons, colors, themes, XML configurations
        │   │
        │   └── java/
        │       ├── com/zerogrid/mesh/app/ # Role-based Gateway subsystem
        │       │   ├── service/
        │       │   │   └── MeshPeerResolver.kt      # Autonomous multi-interface signal resolver
        │       │   └── ui/
        │       │       ├── AppNavigation.kt         # UserSessionManager & gateway session state
        │       │       ├── RoleSelectionScreen.kt   # Role switcher (Citizen vs Authority)
        │       │       ├── NameEntryScreen.kt       # Callsign / Node display name entry
        │       │       ├── UserDashboardScreen.kt   # Regular citizen command dashboard
        │       │       ├── AuthorityDashboardScreen.kt # Official rescue / incident command panel
        │       │       └── navigation/
        │       │           └── AppScreen.kt         # Sealed class for gateway navigation states
        │       │
        │       └── com/example/zerogrid/  # Primary ZeroGrid application package
        │           ├── MainActivity.kt              # App entrypoint, permission launcher, gateway
        │           │
        │           ├── home/
        │           │   └── MeshDashboardScreen.kt   # Primary cybernetic home dashboard
        │           │
        │           ├── messaging/
        │           │   ├── MessagesScreen.kt        # Conversation thread list & quick shortcuts
        │           │   ├── PeerDirectChatScreen.kt  # 1-on-1 peer direct message timeline
        │           │   ├── ChannelsScreen.kt        # Broadcast channels list (#general, #sos)
        │           │   ├── ChannelChatScreen.kt     # Broadcast channel chat room
        │           │   └── MessageStore.kt          # JSON SharedPreferences persistent DM store
        │           │
        │           ├── mesh/
        │           │   ├── NearbyDevicesScreen.kt   # BLE/Wi-Fi peer scanner radar
        │           │   ├── PeerDetailsScreen.kt     # In-depth peer telemetry & diagnostic view
        │           │   ├── NetworkStatusScreen.kt   # Mesh topology statistics & traffic metrics
        │           │   │
        │           │   ├── engine/                  # Core routing & protocol engine
        │           │   │   ├── MeshEngine.kt        # Central orchestrator singleton facade
        │           │   │   ├── MeshRoutingEngine.kt # Multi-hop store-and-forward relay coordinator
        │           │   │   ├── MeshPacket.kt        # Packet data model & JSON serializer
        │           │   │   ├── PacketType.kt        # Enum of packet operation types
        │           │   │   ├── MeshNode.kt          # Peer representation & telemetry model
        │           │   │   ├── PeerTable.kt         # Thread-safe peer cache with stale pruning
        │           │   │   └── DeduplicationCache.kt# Time-bounded LRU packet deduplicator
        │           │   │
        │           │   └── transport/               # Radio hardware abstraction drivers
        │           │       ├── MeshTransport.kt     # Common interface for mesh radio drivers
        │           │       ├── BleMeshDriver.kt     # BLE GATT Advertiser/Scanner/Server/Client
        │           │       └── WifiDirectMeshDriver.kt # Wi-Fi P2P & Hotspot UDP/TCP driver
        │           │
        │           ├── emergency/
        │           │   ├── SosCenterScreen.kt       # High-visibility emergency incident center
        │           │   └── SendSosScreen.kt         # Emergency beacon broadcaster with GPS toggle
        │           │
        │           ├── files/
        │           │   ├── FilesScreen.kt           # Offline shared file manager
        │           │   ├── SendFileScreen.kt        # Recipient & file payload selector
        │           │   └── FileTransferScreen.kt    # Real-time chunked transfer progress monitor
        │           │
        │           ├── onboarding/
        │           │   ├── SplashScreen.kt          # Animated brand intro
        │           │   ├── OnboardingScreen.kt      # Concept carousel walkthrough
        │           │   ├── PermissionsScreen.kt     # Guided runtime permission requester
        │           │   └── CreateIdentityScreen.kt  # Cryptographic identity creation prompt
        │           │
        │           ├── settings/
        │           │   ├── SettingsScreen.kt        # Network parameters, discovery, relay toggles
        │           │   └── SecurityPrivacyScreen.kt # Cryptographic keys, QR export, privacy toggles
        │           │
        │           ├── service/
        │           │   └── MeshForegroundService.kt # Android Background Service daemon
        │           │
        │           ├── debug/
        │           │   ├── DebugLogger.kt           # Memory-buffered log streamer
        │           │   └── DebugConsoleScreen.kt    # In-app terminal log viewer
        │           │
        │           ├── navigation/
        │           │   ├── Routes.kt                # Screen enum (20 routes)
        │           │   ├── NavGraph.kt              # App router, back-stack, global SOS dialog
        │           │   └── BottomNavigation.kt      # Cybernetic bottom navigation bar
        │           │
        │           └── ui/theme/
        │               ├── Color.kt                 # Neon Cyan, Tactical Dark, Warning colors
        │               └── Theme.kt                 # Material 3 dark cybernetic theme definition
        │
        └── test/java/com/example/zerogrid/
            ├── BleUuidTest.kt                       # Service & Characteristic UUID validation
            ├── MeshProtocolTest.kt                  # JSON packet serialization & parsing tests
            └── TransportFramingTest.kt              # Byte-level chunking and reassembly tests
```

---

## 5. Core Engine Deep Dive

### `MeshEngine.kt` (The Central Facade)
- **Singleton Pattern:** Accessed via `MeshEngine.getInstance(context)`. Lifecycle spans the lifetime of the application or `MeshForegroundService`.
- **Node Identity:** Auto-generates a persistent 8-character hex identity (e.g., `NODE-d48923d7`) stored in `zerogrid_identity_prefs`.
- **Display Name Synchronization:** Detects device Bluetooth/model name or lets users configure a callsign. Advertises this name via BLE scan response data and periodic discovery packets.
- **Reactive StateFlow Properties:**
  - `connectedPeers: StateFlow<List<MeshNode>>`: Real-time reactive list of active neighbors.
  - `conversations: StateFlow<Map<String, List<StoredMessage>>>`: Active DM threads mapped by peer ID.
  - `sosAlerts: StateFlow<List<MeshPacket>>`: Active emergency alerts received over the mesh.
  - `isMeshActive: StateFlow<Boolean>`: Radio driver operational status.
  - `packetsRelayedCount: StateFlow<Int>`: Running count of multi-hop packets forwarded for others.
- **Background Autonomous Loops:**
  - **10s Peer Announce Loop:** Broadcasts identity packets so neighbors keep local routing tables fresh.
  - **15s Store-and-Forward Retry Loop:** Periodically checks for queued messages with `MessageStatus.PAUSED` and automatically re-transmits them if the destination peer has re-entered radio range.

### `MeshRoutingEngine.kt` (Store-and-Forward Routing)
- **Controlled Flooding Algorithm:**
  1. Validates packet freshness using `DeduplicationCache`.
  2. If the packet is addressed to this node (`localNodeId`) or is a broadcast (`*`), it is consumed locally.
  3. If the packet is not for this node (or is broadcast) and `ttl > 1`, it decrements TTL (`ttl - 1`), increments hop count (`hopCount + 1`), and forwards it to all active transports **except the transport it was received on**.
- **Loopback & Self-Packet Suppression:** Implements prefix/suffix normalization (e.g. `d48923d7` vs `NODE-d48923d7`) to guarantee a node never relays its own packets back to the mesh.

### `DeduplicationCache.kt` (Loop Prevention)
- Backed by a `ConcurrentHashMap` with a 5-minute sliding expiration window.
- If a packet with ID `X` arrives via BLE and 200ms later arrives via Wi-Fi Direct or a second-hop neighbor, the second copy is discarded immediately without triggering UI updates or radio re-transmissions.

### `PeerTable.kt` (Dynamic Neighbor Registry)
- Stores active `MeshNode` instances thread-safely.
- Tracks RSSI (dBm), physical transports available (BLE, Wi-Fi Direct), and hop distance.
- `pruneStalePeers(staleThresholdMs = 90_000)`: Removes unreachable peers from the active peer list if no beacons are heard within 90 seconds.

---

## 6. Transport Layer Protocols (BLE & Wi-Fi)

### BLE Mesh Driver (`BleMeshDriver.kt`)
BLE is the backbone for low-energy proximity discovery and small-packet store-and-forward routing.

- **Standardized UUIDs:**
  - **Service UUID:** `0000a701-0000-1000-8000-00805f9b34fb`
  - **Write Characteristic (Client -> Server):** `0000a702-0000-1000-8000-00805f9b34fb` (Properties: `WRITE | WRITE_NO_RESPONSE`)
  - **Notify Characteristic (Server -> Client):** `0000a703-0000-1000-8000-00805f9b34fb`
  - **Name Advertising UUID (Scan Response):** `0000a704-0000-1000-8000-00805f9b34fb`
  - **CCCD UUID:** `00002902-0000-1000-8000-00805f9b34fb`
- **Logical Node-ID to BLE MAC Mapping:** Modern Android OS versions rotate BLE Resolvable Private Addresses (RPAs) every 15 minutes. `BleMeshDriver` extracts the logical `NODE-xxxxxxxx` from BLE Service Data on every scan result, continuously updating its internal hardware address mapping.
- **Scan-Pause on Connection (Radio Contention Fix):** Calling `connectGatt()` while active scanning is running causes Android Bluetooth chipsets to error with `GATT Status 133` or `147`. The driver pauses scanning during GATT connection handshakes and resumes upon connection success or timeout.
- **MTU Negotiation & Framing:**
  - Negotiates MTU up to **512 bytes** on connection.
  - Slices larger payloads into chunked `BleFrame` structures with sequential sequence IDs and start/end control flags.
  - Reassembles incoming fragments into full `MeshPacket` JSON instances with a 30-second timeout.

### Wi-Fi Mesh Driver (`WifiDirectMeshDriver.kt`)
High-speed data transport designed for high throughput, media sharing, and immediate multi-node communication.

- **Dual-Mode Operation:**
  1. **Wi-Fi Direct (P2P):** Uses Android `WifiP2pManager` DNS-SD service registration (`_presence._tcp` / `_zerogrid`) to discover peers without access points.
  2. **Local WLAN / Hotspot Mode:** Broadcasts UDP presence discovery beacons on port **8889** over local subnet or tethered Wi-Fi hotspot.
- **TCP Streaming (Port 8888):**
  - Uses length-prefixed binary framing: `[4 bytes integer length][UTF-8 payload bytes]`.
  - Maintains persistent connected sockets in a `ConcurrentHashMap` for zero-handshake message dispatch.

---

## 7. Interface Resolution & Dynamic Routing

### `MeshPeerResolver.kt`
When communicating with a neighbor that has **both** BLE and Wi-Fi Direct connections active, transmitting over both channels simultaneously wastes battery and radio spectrum. `MeshPeerResolver` solves this autonomously.

### Scoring Formula
Each physical interface endpoint is evaluated according to:

$$\text{Normalized RSSI} = \frac{\text{clamp}(\text{RSSI}, -100, -30) + 100}{70}$$

$$\text{Freshness Factor} = \text{clamp}\left(1.0 - \frac{\text{AgeMs}}{45000}, 0.2, 1.0\right)$$

$$\text{Score} = (\text{Normalized RSSI} \times 100 \times \text{Weight}) \times \text{Freshness Factor}$$

| Interface Type | Weight | Notes |
| :--- | :---: | :--- |
| **Local Subnet (LAN / Hotspot)** | `1.50` | Highest bandwidth, lowest packet drop |
| **Wi-Fi Direct (P2P)** | `1.25` | High bandwidth, direct peer radio |
| **Wi-Fi Aware (NAN)** | `1.20` | Future hardware expansion |
| **Bluetooth Low Energy (BLE)** | `1.00` | Universal baseline, lower bandwidth |

### Dynamic Route Selection & Automatic Fallback
When sending a direct message:
1. `MeshRoutingEngine` queries `MeshPeerResolver.getBestRouteForDevice(peerId)`.
2. It attempts transmission over the highest-scoring interface (e.g. Wi-Fi Direct).
3. If transmission fails (e.g., socket drop), it **instantly falls back to BLE** without dropping the user's message.

---

## 8. UI/UX Architecture & Screen Inventory

The application has a dual-gateway architecture:
- **Role Gateway (`com.zerogrid.mesh.app.ui`):** Fast triage role-oriented flow.
- **Full Mesh Application (`com.example.zerogrid`):** Full-featured, 20-screen cybernetic mesh terminal.

### Complete Screen Inventory

```
[Gateway Root]
  ├── RoleSelectionScreen        (Select: Standard Citizen vs Official Authority)
  ├── NameEntryScreen            (Enter Callsign / Node Display Name)
  ├── UserDashboardScreen        (Citizen Quick Panel: status, nearby, SOS trigger)
  └── AuthorityDashboardScreen   (Command Panel: incident feeds, broadcast alert composer)
        │
        └── [Launch Full Mesh Terminal] -> ZeroGridApp() (20 Screens)
```

| Screen Route | File | Key Capabilities & Features |
| :--- | :--- | :--- |
| `HOME` | [`MeshDashboardScreen.kt`](file:///c:/Users/ACER/AndroidStudioProjects/gridzero/app/src/main/java/com/example/zerogrid/home/MeshDashboardScreen.kt) | Active Node ID, signal dBm, hop count, active peers preview, quick navigation cards, emergency SOS FAB. |
| `MESSAGES` | [`MessagesScreen.kt`](file:///c:/Users/ACER/AndroidStudioProjects/gridzero/app/src/main/java/com/example/zerogrid/messaging/MessagesScreen.kt) | 1-on-1 conversations list, unread badges, timestamp, peer online/offline indicators, channel navigation. |
| `PEER_DIRECT_CHAT`| [`PeerDirectChatScreen.kt`](file:///c:/Users/ACER/AndroidStudioProjects/gridzero/app/src/main/java/com/example/zerogrid/messaging/PeerDirectChatScreen.kt) | Direct chat timeline, delivery status (`PAUSED`, `SENT`, `DELIVERED`), manual retry button, live presence. |
| `CHANNELS` | [`ChannelsScreen.kt`](file:///c:/Users/ACER/AndroidStudioProjects/gridzero/app/src/main/java/com/example/zerogrid/messaging/ChannelsScreen.kt) | Public mesh broadcast channels (`#general`, `#emergency`, `#camp-alpha`), active subscriber counters. |
| `CHAT_DETAIL` | [`ChannelChatScreen.kt`](file:///c:/Users/ACER/AndroidStudioProjects/gridzero/app/src/main/java/com/example/zerogrid/messaging/ChannelChatScreen.kt) | Live public channel chat feed, multi-hop relay message badges showing hop distance. |
| `MESH` | [`NearbyDevicesScreen.kt`](file:///c:/Users/ACER/AndroidStudioProjects/gridzero/app/src/main/java/com/example/zerogrid/mesh/NearbyDevicesScreen.kt) | Radar scanner for BLE & Wi-Fi peers, real-time RSSI signal bars, direct connect triggers. |
| `PEER_DETAILS` | [`PeerDetailsScreen.kt`](file:///c:/Users/ACER/AndroidStudioProjects/gridzero/app/src/main/java/com/example/zerogrid/mesh/PeerDetailsScreen.kt) | Cryptographic fingerprint, hardware MAC, hop distance, RTT latency, direct chat shortcut. |
| `NETWORK_STATUS`| [`NetworkStatusScreen.kt`](file:///c:/Users/ACER/AndroidStudioProjects/gridzero/app/src/main/java/com/example/zerogrid/mesh/NetworkStatusScreen.kt) | Topology telemetry: active nodes, direct neighbors, total packets relayed, throughput graphs. |
| `SOS_CENTER` | [`SosCenterScreen.kt`](file:///c:/Users/ACER/AndroidStudioProjects/gridzero/app/src/main/java/com/example/zerogrid/emergency/SosCenterScreen.kt) | High-visibility emergency operations room, live incident feed, triage instructions, acknowledgment. |
| `SEND_SOS` | [`SendSosScreen.kt`](file:///c:/Users/ACER/AndroidStudioProjects/gridzero/app/src/main/java/com/example/zerogrid/emergency/SendSosScreen.kt) | Category selector (Medical, Disaster, Trapped, Security), GPS coordinate toggle, high-priority broadcast. |
| `FILES` | [`FilesScreen.kt`](file:///c:/Users/ACER/AndroidStudioProjects/gridzero/app/src/main/java/com/example/zerogrid/files/FilesScreen.kt) | Offline shared file manager, categorized files (documents, APKs, images), storage stats. |
| `SEND_FILE` | [`SendFileScreen.kt`](file:///c:/Users/ACER/AndroidStudioProjects/gridzero/app/src/main/java/com/example/zerogrid/files/SendFileScreen.kt) | File picker, target peer selection, compression toggles, transmission preview. |
| `FILE_TRANSFER` | [`FileTransferScreen.kt`](file:///c:/Users/ACER/AndroidStudioProjects/gridzero/app/src/main/java/com/example/zerogrid/files/FileTransferScreen.kt) | Chunk transfer rate (KB/s), SHA-256 chunk validation, pause/resume/cancel controls. |
| `SETTINGS` | [`SettingsScreen.kt`](file:///c:/Users/ACER/AndroidStudioProjects/gridzero/app/src/main/java/com/example/zerogrid/settings/SettingsScreen.kt) | Mesh transport toggles (BLE on/off, Wi-Fi on/off), auto-relay permissions, callsign editor. |
| `SECURITY_PRIVACY`|[`SecurityPrivacyScreen.kt`](file:///c:/Users/ACER/AndroidStudioProjects/gridzero/app/src/main/java/com/example/zerogrid/settings/SecurityPrivacyScreen.kt)| Fingerprint QR code, identity regeneration, anonymous relay mode, security diagnostics. |
| `DEBUG_CONSOLE`| [`DebugConsoleScreen.kt`](file:///c:/Users/ACER/AndroidStudioProjects/gridzero/app/src/main/java/com/example/zerogrid/debug/DebugConsoleScreen.kt) | Live scrolling terminal output with color-coded severity (`INFO`, `DEBUG`, `WARN`, `ERROR`), log copy. |
| `SPLASH` | [`SplashScreen.kt`](file:///c:/Users/ACER/AndroidStudioProjects/gridzero/app/src/main/java/com/example/zerogrid/onboarding/SplashScreen.kt) | Animated cybernetic pulse logo and system initialization check. |
| `ONBOARDING` | [`OnboardingScreen.kt`](file:///c:/Users/ACER/AndroidStudioProjects/gridzero/app/src/main/java/com/example/zerogrid/onboarding/OnboardingScreen.kt) | Multi-step interactive tutorial explaining off-grid store-and-forward mesh concepts. |
| `PERMISSIONS` | [`PermissionsScreen.kt`](file:///c:/Users/ACER/AndroidStudioProjects/gridzero/app/src/main/java/com/example/zerogrid/onboarding/PermissionsScreen.kt) | Guided rationale for Bluetooth, Nearby Wi-Fi, and Location runtime permissions. |
| `CREATE_IDENTITY`|[`CreateIdentityScreen.kt`](file:///c:/Users/ACER/AndroidStudioProjects/gridzero/app/src/main/java/com/example/zerogrid/onboarding/CreateIdentityScreen.kt)| First-run callsign selection and cryptographic node pseudonym generation. |

---

## 9. Message Persistence & Store-and-Forward

### Message Persistence Architecture (`MessageStore.kt`)
- **Keyed by Peer Node ID:** Each conversation is stored as a JSON array under key `conv_<peerId>` in `zerogrid_messages.xml`.
- **Bounded Retention:** Stores up to 200 messages per conversation thread to prevent memory overflow on constrained devices.
- **Survives App Restarts:** Messages, statuses, and peer aliases persist across process termination and reboots.

### Message Lifecycle States (`MessageStatus`)
```
  [User Taps Send]
         │
         ▼
     [PAUSED] ◄─── Peer offline / out of range (Queued in Store-and-Forward)
         │               │
         │ (Peer online) │ (15s Retry loop detects peer online)
         ▼               ▼
      [SENT]  ────► Transmitted over BLE or Wi-Fi radio to neighbor
         │
         ▼ (Remote node receives packet and transmits ACK back)
    [DELIVERED]
         │
         ▼ (Recipient opens conversation)
      [READ]
```

---

## 10. Emergency SOS Beacon Subsystem

The SOS subsystem is designed to cut through all standard network traffic with highest priority:

1. **Protocol Propagation:**
   - Packet type: `PacketType.SOS_BEACON`.
   - Time-To-Live: **TTL = 10** (Double the standard message TTL of 5) to maximize geographic reach.
   - Broadcast address: `*`.
   - Transmitted concurrently over **all** active physical radios.
2. **System-Level Heads-Up Notification:**
   - Triggered via `MeshForegroundService.showSosNotification()`.
   - High-importance channel (`CHANNEL_SOS`) with audio and vibration alert.
3. **Global In-App Pop-Up Alert Dialog:**
   - Hosted directly in `NavGraph.kt`.
   - Appears regardless of which screen the user is viewing.
   - Displays sender callsign, coordinates, and emergency message.
   - Requires explicit acknowledgment to dismiss.

---

## 11. Wire Protocol & Packet Specifications

All packets exchanged over radio interfaces are serialized as UTF-8 JSON payloads adhering to the following schema:

```json
{
  "packetId": "d98b1c42-2b65-4d0f-8c31-9f9353912a7a",
  "senderId": "NODE-a1b2c3d4",
  "recipientId": "NODE-e5f60718",
  "ttl": 5,
  "hopCount": 0,
  "type": "DIRECT_MESSAGE",
  "payload": "Hello from off-grid node!",
  "timestamp": 1773860000000,
  "signature": ""
}
```

### Packet Field Definitions
- `packetId` *(String, UUID)*: Unique identifier used by `DeduplicationCache` to filter duplicate transmissions.
- `senderId` *(String)*: Originating Node ID (e.g., `NODE-a1b2c3d4`).
- `recipientId` *(String)*: Target Node ID or `*` for broadcast.
- `ttl` *(Int)*: Hop limit (default 5 for messages, 10 for SOS). Decremented by 1 at each forwarder.
- `hopCount` *(Int)*: Starts at 0, incremented by 1 at each intermediate relay.
- `type` *(String, PacketType)*: Operational intent of the packet.
- `payload` *(String)*: Text message, JSON manifest, or chunk data.
- `timestamp` *(Long)*: Epoch milliseconds when packet was generated.
- `signature` *(String)*: Cryptographic signature placeholder (to be verified when E2EE is enabled).

### Packet Types Reference (`PacketType.kt`)
| Type | Value | Routing Rule |
| :--- | :---: | :--- |
| `PEER_DISCOVERY` | `PEER_DISCOVERY` | Broadcast (`*`), TTL=1 or 2, updates neighbor tables with display names. |
| `DIRECT_MESSAGE` | `DIRECT_MESSAGE` | Unicast, routed to specific `recipientId`, triggers ACK upon local delivery. |
| `ACK` | `ACK` | Unicast, payload contains original `packetId`, updates sender's message to `DELIVERED`. |
| `CHANNEL_BROADCAST` | `CHANNEL_BROADCAST` | Broadcast (`*`), delivered to all nodes subscribed to `#channel`. |
| `SOS_BEACON` | `SOS_BEACON` | Broadcast (`*`), TTL=10, high-priority heads-up alert and dialog. |
| `FILE_CHUNK` | `FILE_CHUNK` | Unicast or Broadcast, contains sequential chunk payload for file reassembly. |
| `ROUTE_UPDATE` | `ROUTE_UPDATE` | Broadcast or Unicast, topology metrics for multi-hop path optimization. |

---

## 12. Native C++ Layer (NDK / JNI)

- **Configuration:** CMake `3.22.1`, Android NDK `27.0.12077973`, C++17 standard (`-std=c++17`).
- **Files:**
  - `app/src/main/cpp/CMakeLists.txt`
  - `app/src/main/cpp/native-lib.cpp`
- **Current Status:** Scaffold compiled and linked into APK. Ready for performance-critical native crypto algorithms (e.g. Ed25519 signing, ChaCha20-Poly1305, Noise Protocol handshakes).

---

## 13. Permissions & Android OS Integration

ZeroGrid requires permissions tailored for Android 8 through Android 15:

```xml
<!-- Bluetooth & BLE -->
<uses-permission android:name="android.permission.BLUETOOTH" android:maxSdkVersion="30" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" android:maxSdkVersion="30" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" tools:targetApi="31" />
<uses-permission android:name="android.permission.BLUETOOTH_ADVERTISE" tools:targetApi="31" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" tools:targetApi="31" />

<!-- Location (Required by Android OS for BLE and Wi-Fi scanning) -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />

<!-- Wi-Fi Direct & Local Subnet -->
<uses-permission android:name="android.permission.NEARBY_WIFI_DEVICES" android:usesPermissionFlags="neverForLocation" tools:targetApi="33" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
<uses-permission android:name="android.permission.CHANGE_WIFI_MULTICAST_STATE" />
<uses-permission android:name="android.permission.INTERNET" />

<!-- System Notifications & Background Daemon -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE" tools:targetApi="34" />
```

---

## 14. Testing Suite & Verification Status

Unit tests located in `app/src/test/java/com/example/zerogrid/`:

1. **`BleUuidTest.kt`:**
   - Validates that `SERVICE_UUID`, `WRITE_CHAR_UUID`, and `NOTIFY_CHAR_UUID` match protocol specifications.
2. **`MeshProtocolTest.kt`:**
   - Validates `MeshPacket` JSON serialization and deserialization.
   - Verifies TTL decrement, hop count increment, and broadcast matching logic.
3. **`TransportFramingTest.kt`:**
   - Verifies byte chunking, sequential framing, and reassembly logic for BLE payloads exceeding MTU limits.

### Build Verification Commands
```bash
# Verify compilation of all Kotlin and C++ code
./gradlew assembleDebug

# Run unit tests
./gradlew testDebugUnitTest
```

---

## 15. Identified Gaps, Technical Debt & Next Steps

### Planned Engineering Tasks (Roadmap)

1. **End-to-End Encryption (E2EE) Implementation:**
   - Integrate cryptographic key generation (Curve25519 / Ed25519) in `CreateIdentityScreen`.
   - Implement Noise Protocol framework or Signal Double Ratchet for private DMs.
   - Sign all packets with private keys to prevent malicious node impersonation or spoofed SOS beacons.

2. **Database Migration (Room + SQLCipher):**
   - Migrate `MessageStore` from SharedPreferences JSON arrays to an encrypted Room database.
   - Enable indexed searches, full-text message queries, and media attachment storage.

3. **File Chunking & Assembly Pipeline:**
   - Build `FileTransferManager` to slice arbitrary files (images, offline maps, voice memos) into `FILE_CHUNK` packets with SHA-256 integrity verification.
   - Wire `FileTransferScreen` to live file transfer events.

4. **Battery & Radio Optimization:**
   - Implement dynamic BLE scan duty-cycling (e.g. 5s scan / 15s sleep) when no active transfers are in flight to preserve battery during extended emergency outages.
