# ZeroGrid Backend Implementation Guide

## 1. Purpose
This document serves as the canonical technical reference for the ZeroGrid backend. It provides an honest audit of the current implementation, defines the target architecture, and establishes a roadmap for transitioning from a loopback prototype to a production-ready off-grid mesh network.

## 2. Source of Truth
*   **Implementation Truth:** `app/src/main/java/com/example/zerogrid/mesh/`
*   **Target Specification:** [README.md](file:///C:/Users/user/StudioProjects/zerogrid/README.md)
*   **Native Core:** [native-lib.cpp](file:///C:/Users/user/StudioProjects/zerogrid/app/src/main/cpp/native-lib.cpp) (Current: C++ scaffold)

## 3. Current Implementation Status

| Feature | Status | Verified Implementation Detail |
| :--- | :--- | :--- |
| **Mesh Foundation** | ✅ Implemented | `MeshEngine` (Facade), `MeshForegroundService`. |
| **Routing Logic** | ✅ Implemented | `MeshRoutingEngine` (Flooding + TTL + Hops + Source Exclusion). |
| **Deduplication** | ✅ Implemented | `DeduplicationCache` (5-min time-bounded cache). |
| **Peer Discovery** | ✅ Implemented | `PeerTable` (RSSI, Hops, Transport tracking). |
| **BLE Driver** | ✅ Implemented | Adv/Scan, GATT Server, **GATT Client with Framing/MTU**. |
| **Wi-Fi Driver** | ✅ Implemented | Discovery, TCP Server, **TCP Client with Length-Prefixed Framing**. |
| **Messaging** | 🟡 Partial | UI and Packet generation. **Persistence Missing**. |
| **SOS Subsystem** | 🟡 Partial | High-priority TTL routing logic. **No Abuse Protection**. |
| **Persistence** | ⏳ Planned | No local storage implementation found in source. |
| **Security** | ⏳ Planned | No cryptographic identity or E2EE implementation found. |

---

## 4. Current Architecture: Real Multi-Device Mesh
The system has transitioned from a loopback prototype to a real device-to-device transport layer.

```text
       Application Trigger
               ↓
          MeshEngine
               ↓
    MeshRoutingEngine.send()
               ↓
    DeduplicationCache.record()  <-- [ID RECORDED]
               ↓
      MeshTransport.send()
               ↓
      [ RADIO TRANSMISSION ]     <-- [REAL PATH]
               ↓
    [ REMOTE DEVICE RADIO ]
               ↓
    MeshRoutingEngine.collect()
               ↓
    DeduplicationCache.check()   <-- [NEW PACKET]
               ↓
         [DELIVER/RELAY]
```

## 5. Target Architecture: Production Mesh
The target architecture introduces an **Outbound Queue**, a **Packet Scheduler**, and a **Transport Selection** layer to break the loopback and enable multi-device communication.

```text
       Application Layer
               ↓
        Repository Layer (Planned)
               ↓
          MeshEngine
               ↓
       MeshRoutingEngine
               ↓
        Routing Decision
               ↓
      Outbound Queue (Planned)
               ↓
         Scheduler (Planned)
               ↓
    Transport Selection (Planned)
               ↓
         MeshTransport
         ↙           ↘
       BLE       Wi-Fi Direct
         ↓           ↓
    [ REMOTE DEVICE RADIO ]
```

### Architectural Component Responsibilities
*   **Routing:** Determines whether and how a packet should propagate through the mesh.
*   **Queue:** Holds pending outbound packets waiting for transmission resources.
*   **Scheduler:** Determines *when* packets should be transmitted based on priority and network state.
*   **Transport Selection:** Chooses the most appropriate available transport (e.g., BLE for small messages, Wi-Fi for files).

---

## 6. Mesh Engine & Routing Model

### Current: TTL-Limited Controlled Flooding
ZeroGrid currently implements immediate relaying. Packets are forwarded to neighbors as soon as they are received and validated.
*   **Relay Logic:** Packets are sent to all available transports *except* the transport that received the packet.

### Target: Persistent Store-and-Forward
The target architecture will introduce persistent storage for packets. This allows a node to "store" a packet when no suitable neighbors are available and "forward" it later when connectivity is established.

### Packet Protocol (`MeshPacket.kt`)
*   **Default TTL:** 5 (Hardcoded in `DEFAULT_TTL`).
*   **SOS TTL:** 10 (Hardcoded in `triggerSosBeacon`).
*   **hopCount:** Represents the number of forwarding operations performed after packet creation. `hopCount` increments by 1 on each relay.
*   **Broadcast Address:** `*`.

### Deduplication Cache (`DeduplicationCache.kt`)
*   **Mechanism:** `ConcurrentHashMap` based time-bounded cache.
*   **Expiration:** 300,000ms (5 minutes).
*   **Scalability:** Target architecture requires bounded cache size to prevent memory exhaustion.

### Peer Management (`PeerTable.kt`)
*   **Expiration:** 60,000ms (1 minute) stale threshold.
*   **Tracking:** Records `rssi`, `hopDistance` (1 = direct neighbor), and `transportType`.

---

## 7. Transport Architecture

### BLE Transport (`BleMeshDriver.kt`)
*   **Service UUID:** `0000a701-0000-1000-8000-00805f9b34fb`
*   **Characteristic UUID:** `0000a702-0000-1000-8000-00805f9b34fb`
*   **Verified:** Advertising and Scanning functional. GATT Server accepts incoming writes.
*   **Target:** Implement GATT Client to perform writes to discovered remote peers.

### Wi-Fi Direct Transport (`WifiDirectMeshDriver.kt`)
*   **Server Port:** 8888.
*   **Verified:** `WifiP2pManager` discovery and `ServerSocket` listener functional.
*   **Target:** Implement TCP Client to transmit packets to peer IP addresses.

---

## 8. Target Outbound Pipeline: Scheduler & Scalability

### Outbound Scheduler (Planned)
Manages outbound traffic priority to prevent mesh saturation.
*   **Priority Levels:**
    *   **P0 (SOS):** Highest priority; bypasses standard message queues.
    *   **P1 (Mesh Control):** Heartbeats, ACKs, and routing updates.
    *   **P2 (Messages):** Standard text traffic.
    *   **P3 (File Chunks):** Bulk data; lowest priority.
*   **Resource Protection:**
    *   **Bounded Queues:** Prevent memory growth during high congestion.
    *   **Rate Limiting:** Protect against flooding and duplicate amplification.
    *   **Battery Awareness:** Optimize transmission frequency based on power state.

### SOS Subsystem
*   **Priority:** SOS traffic receives the highest scheduling priority while remaining subject to protocol-level rate limits and resource protections to prevent malicious flooding.
*   **Relay:** High-visibility propagation across all available transports with increased TTL.

---

## 9. Security & Identity (Planned)

### Required Security Properties
1.  **Identity:** Persistent cryptographic identity per node.
2.  **Authentication:** Proof of sender identity for every packet.
3.  **Confidentiality:** End-to-end encryption (E2EE) for private payloads.
4.  **Integrity:** Protection against packet modification during relay.
5.  **Replay Protection:** Prevention of duplicate packet injection attacks.

### Protocol Status
*   **Cryptographic Protocol:** **TBD**. Selection of specific primitives (e.g., signing schemes, key exchange) requires dedicated security design and review.
*   **Note:** The current `signature` field in `MeshPacket` is an unverified placeholder string.

---

## 10. File Transfer (Planned)
*   **Mechanism:** Chunk-based transmission via `FILE_CHUNK` packets.
*   **Manifest:** Contains File ID, Name, Total Size, and SHA-256 integrity hash.
*   **Chunking:** Application-level chunk size is **TBD** and must be selected based on transport framing, memory usage, and retransmission cost.
*   **Reliability:** Chunks must be independently routable; reassembly occurs at the destination after manifest verification.

---

## 11. Concurrency & Lifecycle
*   **Service:** `MeshForegroundService` manages the singleton `MeshEngine`.
*   **Concurrency:** Recommended target is a centralized `MeshScope` tied to the Service lifecycle, ensuring structured cancellation of radio drivers and background tasks.

---

## 12. Definition of Done

### Mesh Foundation
- [ ] Device A discovers Device B over BLE/Wi-Fi.
- [ ] Device A establishes a usable transport connection to B.
- [ ] Device A successfully sends a packet to B over the radio.
- [ ] Device B receives and validates the packet.
- [ ] Device B delivers the packet locally if intended for it.
- [ ] Device B relays a packet to Device C.
- [ ] Device C receives the packet with `hopCount` = 2 (if A sent it to B).
- [ ] TTL behaves according to protocol (packet dropped when TTL = 0).
- [ ] Deduplication suppresses duplicate processing.
- [ ] Connection loss and reconnection behavior is handled gracefully.

### Persistence (Planned)
- [ ] Persistence layer (e.g., Room) is implemented.
- [ ] Messages and peer history are stored locally.
- [ ] Offline messages are retained for later forwarding.
- [ ] Stored data survives application restart.
- [ ] Repository layer abstracts persistence from mesh transport.

---

## 13. Implementation Roadmap

### Phase 1: Break the Loopback
*   ✅ Implement BLE GATT Client writes with MTU negotiation and framing.
*   ✅ Implement Wi-Fi Direct TCP Client writes with length-prefixed framing.
*   ✅ Verify real device-to-device packet exchange via unit tests and routing simulation.

### Phase 2: Mesh Reliability
*   Implement Outbound Scheduler and Priority Queues.
*   Introduce Rate Limiting and Congestion Control.

### Phase 3: Persistence
*   Integrate local storage for message and peer history.
*   Implement the Repository layer for StateFlow data binding.

### Phase 4: Identity & Security
*   Finalize cryptographic protocol design.
*   Implement node identity generation and packet signing.

---

## 14. README Discrepancy Audit

| README Claim | Actual Implementation | Recommended Status |
| :--- | :--- | :--- |
| **BLE/Wi-Fi Transports COMPLETED** | Discovery only; Transmission is loopback. | 🟡 Partial |
| **Multi-hop Relay Engine COMPLETED** | Logic is implemented; Radio path is loopback. | 🟡 Partial |
| **Identity Generation UI/UX** | UI exists; No cryptographic keys are generated. | 🟡 UI-Only |
| **SOS Beacon COMPLETED** | UI and Packet generation exist; No real broadcast. | 🟡 UI-Only |

---

## 15. Briar/Bramble Migration Note
The Briar/Bramble architecture proposed in earlier drafts has been **discarded**. ZeroGrid is a native implementation built around its own mesh engine and transport abstractions. No external P2P frameworks are currently used.
