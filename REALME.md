# ZeroGrid 🌐

> **Decentralized, Off-Grid Peer-to-Peer Mesh Communication & Emergency Network for Android**

ZeroGrid is a decentralized off-grid mesh communication application that enables secure peer-to-peer messaging, channel broadcasts, multi-hop file sharing, and emergency SOS alerts without relying on cellular networks or internet connectivity.

---

## 📌 Project Status & Overview

```
┌────────────────────────────────────────────────────────────────────────┐
│ UI / Frontend Layer (Jetpack Compose Material 3)    [ ✔ COMPLETED ]    │
│ Navigation & Screen Routing (18 Screens)           [ ✔ COMPLETED ]    │
│ Design System & Cybernetic Dark Palette             [ ✔ COMPLETED ]    │
│ C++ JNI Scaffold (CMake / NDK)                      [ ✔ CONFIGURED ]   │
│ BLE & Wi-Fi Direct Mesh Transports                 [ ✔ COMPLETED ]    │
│ Multi-hop Store-and-Forward Relay Engine            [ ✔ COMPLETED ]    │
│ Dynamic Peer Discovery & Routing Table              [ ✔ COMPLETED ]    │
│ Foreground Service Daemon (Mesh Background Node)    [ ✔ COMPLETED ]    │
│ ────────────────────────────────────────────────────────────────────── │
│ End-to-End Cryptography (Noise / Signal Ratchet)    [ ⏳ PENDING ]     │
│ Local Database (Room / SQLCipher) & Storage         [ ⏳ PENDING ]     │
│ ViewModel & Repository State Data Binding           [ ⏳ PENDING ]     │
└────────────────────────────────────────────────────────────────────────┘
```

> [!NOTE]
> All 18 UI screens, navigation graph, BLE & Wi-Fi Direct transport drivers, multi-hop routing engine and foreground service daemon are **fully implemented and verified**.

---

## 🛠 Tech Stack & Environment

- **Language:** Kotlin (`17` JVM target) & C++17 (Native NDK)
- **UI Framework:** Jetpack Compose (Material 3 + Compose BOM)
- **Build System:** Gradle Kotlin DSL (`build.gradle.kts`) with Version Catalog (`libs.versions.toml`)
- **Native Build:** CMake `3.22.1`, Android NDK `27.0.12077973`
- **Target SDK:** Android 15 (API 35) | **Min SDK:** Android 8.0 (API 26)

---

## 🎨 Frontend & UI Modules

All user interface screens are organized by feature domain under `app/src/main/java/com/example/zerogrid/`:

### 1. 🏠 Home & Dashboard (`home/`)

- [`MeshDashboardScreen.kt`](app/src/main/java/com/example/zerogrid/home/MeshDashboardScreen.kt)
  - Active node identity header (Node ID, Signal strength, Mesh status).
  - Real-time mesh connection status card (hop count, routing mode, active peers count, tap for Network Status).
  - Quick action shortcuts (Messages, Mesh Network, Files, SOS Beacon).
  - Nearby discovered peers list with signal dBm, hop distance, and device roles.
  - Floating SOS emergency broadcast FAB.

### 2. 💬 Messaging & Channels (`messaging/`)

- [`MessagesScreen.kt`](app/src/main/java/com/example/zerogrid/messaging/MessagesScreen.kt)
  - Direct 1-on-1 conversations list with unread badges, timestamp, encryption status, and last seen metrics.
  - Quick access to channels and direct peer chat navigation.
- [`ChannelsScreen.kt`](app/src/main/java/com/example/zerogrid/messaging/ChannelsScreen.kt)
  - Public, private, and localized mesh broadcast channels list (`#general`, `#emergency`, `#camp-alpha`).
  - Active member counters and channel subscription status.
- [`ChannelChatScreen.kt`](app/src/main/java/com/example/zerogrid/messaging/ChannelChatScreen.kt)
  - Real-time chat timeline with incoming/outgoing message bubbles.
  - Message delivery status indicators (Sent, Relayed via X Hops, Delivered, Read).

### 3. 📡 Mesh Management & Discovery (`mesh/`)

- [`NearbyDevicesScreen.kt`](app/src/main/java/com/example/zerogrid/mesh/NearbyDevicesScreen.kt)
  - BLE & Wi-Fi Direct peer radar/scanner list with RSSI dBm and transport type.
  - Manual connection triggers and peer detail view navigation.
- [`PeerDetailsScreen.kt`](app/src/main/java/com/example/zerogrid/mesh/PeerDetailsScreen.kt)
  - Detailed peer telemetry (Public Key fingerprint, MAC address, RTT latency, Relay status).
  - Actions: Direct Message, Send File, Block/Trust Peer.
- [`NetworkStatusScreen.kt`](app/src/main/java/com/example/zerogrid/mesh/NetworkStatusScreen.kt)
  - Mesh topology statistics: Active nodes, direct neighbors, relay hops, packets relayed, and throughput.

### 4. 📁 File Sharing & Transfer (`files/`)

- [`FilesScreen.kt`](app/src/main/java/com/example/zerogrid/files/FilesScreen.kt)
  - Offline file explorer showing received/shared media, documents, and APK packages.
- [`SendFileScreen.kt`](app/src/main/java/com/example/zerogrid/files/SendFileScreen.kt)
  - File picker interface, recipient selection (direct peer vs. multi-hop broadcast), and transfer compression options.
- [`FileTransferScreen.kt`](app/src/main/java/com/example/zerogrid/files/FileTransferScreen.kt)
  - Live transfer progress bar with chunk transfer rate (KB/s), estimated time remaining, SHA-256 chunk validation status, and pause/cancel controls.

### 5. 🚨 Emergency & SOS Beacon (`emergency/`)

- [`SosCenterScreen.kt`](app/src/main/java/com/example/zerogrid/emergency/SosCenterScreen.kt)
  - High-visibility emergency control room with active emergency alerts received over the mesh.
  - Quick emergency guides (Medical, Disaster, Rescue).
- [`SendSosScreen.kt`](app/src/main/java/com/example/zerogrid/emergency/SendSosScreen.kt)
  - Emergency beacon trigger with incident category selector (Medical, Disaster, Trapped, Security).
  - Custom emergency message and GPS location attachment toggle.

### 6. 🚀 Onboarding & Identity (`onboarding/`)

- [`SplashScreen.kt`](app/src/main/java/com/example/zerogrid/onboarding/SplashScreen.kt): Animated cybernetic entrance logo.
- [`OnboardingScreen.kt`](app/src/main/java/com/example/zerogrid/onboarding/OnboardingScreen.kt): Multi-hop mesh concept walkthrough.
- [`PermissionsScreen.kt`](app/src/main/java/com/example/zerogrid/onboarding/PermissionsScreen.kt): Requesting Bluetooth, Nearby Devices, Location, and Wi-Fi permissions.
- [`CreateIdentityScreen.kt`](app/src/main/java/com/example/zerogrid/onboarding/CreateIdentityScreen.kt): Cryptographic node pseudonym and key pair generation prompt.

### 7. ⚙️ Settings & Privacy (`settings/`)

- [`SettingsScreen.kt`](app/src/main/java/com/example/zerogrid/settings/SettingsScreen.kt): Network mode, mesh discovery, automatic switching, and relay mode toggles.
- [`SecurityPrivacyScreen.kt`](app/src/main/java/com/example/zerogrid/settings/SecurityPrivacyScreen.kt): End-to-End encryption management, public key fingerprint export/QR code, identity reset, and anonymous routing toggles.

### 8. 🗺 Navigation & Theme (`navigation/`, `ui/theme/`)

- [`Routes.kt`](app/src/main/java/com/example/zerogrid/navigation/Routes.kt): `Screen` enum definition for all 18 routes.
- [`NavGraph.kt`](app/src/main/java/com/example/zerogrid/navigation/NavGraph.kt): Navigation controller with back-stack support and back-press handling.
- [`BottomNavigation.kt`](app/src/main/java/com/example/zerogrid/navigation/BottomNavigation.kt): Persistent bottom navigation bar.
- [`Color.kt`](app/src/main/java/com/example/zerogrid/ui/theme/Color.kt) & [`Theme.kt`](app/src/main/java/com/example/zerogrid/ui/theme/Theme.kt): Dark cybernetic palette (Neon Cyan, Emergency Orange/Red, Tactical Slate).

---

## 📡 Mesh Networking & Core Engine Implementation

ZeroGrid features a multi-hop P2P communications layer:

### 1. Transports (`mesh/transport/`)

- **BLE Driver ([`BleMeshDriver.kt`](app/src/main/java/com/example/zerogrid/mesh/transport/BleMeshDriver.kt)):**
  - Manages BLE Advertising, Scanning, GATT Server, and GATT Client transmission for neighbor discovery and lightweight packet exchange.
- **Wi-Fi Direct Driver ([`WifiDirectMeshDriver.kt`](app/src/main/java/com/example/zerogrid/mesh/transport/WifiDirectMeshDriver.kt)):**
  - Handles `WifiP2pManager` discovery and TCP socket channels for high-bandwidth file/data transfers.

### 2. Mesh Engine & Routing (`mesh/engine/`)

- **Mesh Engine ([`MeshEngine.kt`](app/src/main/java/com/example/zerogrid/mesh/engine/MeshEngine.kt)):**
  - Central facade orchestrating transports, inbound/outbound packet flows, reactive `StateFlow` streams, and background tasks.
- **Routing Engine ([`MeshRoutingEngine.kt`](app/src/main/java/com/example/zerogrid/mesh/engine/MeshRoutingEngine.kt)):**
  - Controlled Store-and-Forward flooding with TTL decrementing, hop count tracking, and local delivery matching.
- **Deduplication Cache ([`DeduplicationCache.kt`](app/src/main/java/com/example/zerogrid/mesh/engine/DeduplicationCache.kt)):**
  - Synchronized LRU packet hash filter preventing routing loops and duplicate packet processing.
- **Peer Table ([`PeerTable.kt`](app/src/main/java/com/example/zerogrid/mesh/engine/PeerTable.kt)):**
  - Dynamic, thread-safe peer registry tracking RSSI metrics, hop distance, and last-seen timestamps.

### 3. Foreground Service (`service/`)

- **Mesh Foreground Service ([`MeshForegroundService.kt`](app/src/main/java/com/example/zerogrid/service/MeshForegroundService.kt)):**
  - Keeps radio listeners and relay nodes alive in the background with an ongoing Android system notification.

---

## 📂 Project Directory Structure

```
zerogrid/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml
│   │   │   ├── cpp/
│   │   │   │   ├── CMakeLists.txt         # Native C++ build configuration
│   │   │   │   └── native-lib.cpp         # JNI bridge
│   │   │   ├── java/com/example/zerogrid/
│   │   │   │   ├── MainActivity.kt        # App entry point
│   │   │   │   ├── emergency/             # SOS Beacon & Emergency UI
│   │   │   │   ├── files/                 # File sharing & transfer UI
│   │   │   │   ├── home/                  # Mesh Dashboard UI
│   │   │   │   ├── mesh/                  # Discovery & Network Status UI
│   │   │   │   │   ├── engine/            # MeshEngine, Routing, PeerTable, Packets
│   │   │   │   │   └── transport/         # BLE & Wi-Fi Direct drivers
│   │   │   │   ├── messaging/             # Direct & Broadcast Chat UI
│   │   │   │   ├── navigation/            # NavGraph, Routes, BottomBar
│   │   │   │   ├── onboarding/            # Splash, Intro, Permissions & Identity UI
│   │   │   │   ├── service/               # Mesh Foreground Service Daemon
│   │   │   │   ├── settings/              # Settings & Security UI
│   │   │   │   └── ui/theme/              # Material3 Cybernetic Theme
│   │   │   └── res/                       # App icons, strings, resources
│   ├── build.gradle.kts                   # Module build configuration
│   └── proguard-rules.pro
├── gradle/
│   └── libs.versions.toml                 # Version Catalog
├── build.gradle.kts                       # Root build configuration
├── settings.gradle.kts
└── README.md                              # Project documentation
```

---

## 🚀 Getting Started for Developers

### Prerequisites

1. **Android Studio** (Koala / Ladybug or newer).
2. **Android SDK** API level 35.
3. **Android NDK** `27.0.12077973` and **CMake** `3.22.1`.
4. **JDK 17** configured as Gradle JDK.

### Building & Running

```bash
# Clone the repository
git clone https://github.com/hehemohit/zerogrid.git

# Navigate to project directory
cd zerogrid

# Build debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew testDebugUnitTest
```

---

## 👥 Contributors & Maintainers

- **ZeroGrid Core Team** — Mesh Networking & UI/UX Architecture
  SHVET UI
