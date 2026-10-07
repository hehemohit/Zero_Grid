# Multi-Hop BLE Relay & Transitive Peer Discovery Implementation Plan

## 1. Executive Summary

This plan details how to enable **true multi-hop relaying over Bluetooth Low Energy (BLE)** in ZeroGrid, so that:
1. **SOS Alerts & Channel Messages** travel across intermediate devices (`A <---> B <---> C`) over BLE without needing Wi-Fi Direct or Internet.
2. **Direct Messages** can be sent to peers who are out of physical Bluetooth range via common intermediate relay nodes.
3. **Peer Discovery** shows 2-hop and multi-hop peers (`NearbyDevicesScreen` filter for "2 Hops" and "Relay") discovered through common neighbors.

> [!NOTE]
> This plan leaves the existing RSSI tracking implementation plan in `rssiTracking` completely untouched.

---

## 2. Why Multi-Hop BLE Fails in the Current Code (Root Causes)

### Root Cause A: Same-Transport Anti-Relay Filter in [`MeshRoutingEngine.kt`](file:///c:/Users/ACER/AndroidStudioProjects/gridzero/app/src/main/java/com/example/zerogrid/mesh/engine/MeshRoutingEngine.kt#L106-L119)
```kotlin
// In MeshRoutingEngine.processInboundPacket()
activeTransports.forEach { transport ->
    if (transport != sourceTransport && transport.isRunning) { // ❌ BUG
        transport.sendPacket(relayedPacket)
    }
}
```
* **What happens**: When Device B receives a BLE packet from Device A, `sourceTransport` is `BleMeshDriver`.
* The check `transport != sourceTransport` **skips `BleMeshDriver`**.
* **Effect**: B will *never* relay a packet to another BLE peer C. It only relays if C happens to be connected via Wi-Fi Direct.

### Root Cause B: Premature Reachability Check in [`BleMeshDriver.kt`](file:///c:/Users/ACER/AndroidStudioProjects/gridzero/app/src/main/java/com/example/zerogrid/mesh/transport/BleMeshDriver.kt#L498-L504)
```kotlin
// In BleMeshDriver.sendPacket()
val directTarget = targetPeerId ?: if (packet.recipientId != BROADCAST && packet.recipientId != "*") packet.recipientId else null
if (directTarget != null && resolveDeviceForPeerId(directTarget) == null) {
    return false // ❌ BUG: Aborts outbound message on Device A!
}
```
* **What happens**: When A sends a direct message to C (`recipientId = "NODE-C"`), A checks if C is directly connected to A's Bluetooth radio.
* Because C is 2 hops away, `resolveDeviceForPeerId("NODE-C")` returns `null`.
* **Effect**: Device A drops the message before it even leaves the phone, instead of transmitting it to neighbor B to forward.

### Root Cause C: Incomplete Transitive Peer Advertising
* `PEER_DISCOVERY` packets announce `localNodeId` and display name, but when B receives it, B doesn't relay it to C.
* Hence, C never learns that A exists, and A never learns that C exists.

---

## 3. What Changes Need to be Made

### Change 1: Enable Same-Transport Multi-Hop Relaying in [`MeshRoutingEngine.kt`](file:///c:/Users/ACER/AndroidStudioProjects/gridzero/app/src/main/java/com/example/zerogrid/mesh/engine/MeshRoutingEngine.kt)
* **Replace** `if (transport != sourceTransport)` with controlled wireless relaying:
  ```kotlin
  val shouldForward = (isBroadcast || !isTargetedToMe) && (packet.ttl > 1)
  if (shouldForward) {
      val relayedPacket = packet.copy(
          ttl = packet.ttl - 1,
          hopCount = packet.hopCount + 1
      )
      activeTransports.forEach { transport ->
          if (transport.isRunning) {
              // Pass immediate sender ID to avoid echoing directly back to the transmitter
              transport.sendPacket(relayedPacket)
          }
      }
  }
  ```
* **Loop Prevention**: ZeroGrid already has `DeduplicationCache` checking `packet.packetId`. Once a packet is seen by any node, it will never be processed or forwarded a second time.

### Change 2: Next-Hop Routing & Relay Delivery in [`BleMeshDriver.kt`](file:///c:/Users/ACER/AndroidStudioProjects/gridzero/app/src/main/java/com/example/zerogrid/mesh/transport/BleMeshDriver.kt)
* When transmitting an outbound packet where `targetPeerId` is not in direct BLE range:
  1. Check if the target is known in `PeerTable` as a multi-hop peer (`hopDistance > 1`).
  2. If yes (or if relaying an inbound packet intended for another node), do **not** drop the packet!
  3. Instead, resolve the **next-hop direct neighbor** (e.g. Device B) or broadcast to all active connected BLE neighbors so the mesh carries it to the recipient.
* Exclude the node that just handed us the packet from the transmission list (echo suppression).

### Change 3: Transitive Peer Discovery & Peer Table Hop Tracking in [`PeerTable.kt`](file:///c:/Users/ACER/AndroidStudioProjects/gridzero/app/src/main/java/com/example/zerogrid/mesh/engine/PeerTable.kt)
* Add `nextHopNodeId: String?` to `MeshNode`.
* When Device B relays A's `PEER_DISCOVERY` packet to Device C:
  - `packet.senderId` = Node A
  - `packet.hopCount` = 1 (relayed)
* Device C records:
  - `nodeId` = Node A
  - `hopDistance` = 2
  - `isDirectNeighbor` = false
  - `nextHopNodeId` = Node B (the peer whose link brought us this packet)
* `NearbyDevicesScreen.kt` now immediately displays Node A under **"2 Hops"** and **"Relay"** filters!

### Change 4: Message Status & Store-and-Forward
* When A sends a message to C via B:
  - Message status moves to `SENT` once handed off to neighbor B.
  - When C receives it, C generates an `ACK` packet addressed back to A (`recipientId = A`).
  - B relays the `ACK` back to A.
  - When A receives the `ACK`, message status transitions to `DELIVERED`!

---

## 4. How Will This Affect the Mesh Working of the Application?

### A. Emergency SOS Relaying (Major Improvement)
* **Before**: If a victim was in a canyon, basement, or across a field out of direct 40m Bluetooth range of the responder, their SOS would be lost if only BLE was on.
* **With Changes**: Any bystander device between them automatically acts as a silent repeater. The SOS beacon hops across up to 5–10 devices (`TTL = 5–10`), carrying the victim's GPS coordinates across hundreds of meters off-grid.

### B. Peer Discovery & Network Topology Visibility
* **Before**: The "Nearby Devices" list only showed people within direct 10–30m radio visibility.
* **With Changes**:
  - The UI filter **"Direct"** shows immediate neighbors (hop 1).
  - The UI filter **"2 Hops"** shows friends-of-friends connected through your immediate peers.
  - The UI filter **"Relay"** shows all reachable mesh nodes.
  - In `PeerDetailsScreen`, the route card will accurately display: `You ──> [Node B] ──> [Node C] (2 Hops)`.

### C. Bandwidth & Radio Congestion Control
* To prevent flooding the 2.4 GHz spectrum with redundant BLE GATT connections:
  1. **Strict TTL decrement**: Every hop decrements `packet.ttl`. When `ttl <= 1`, forwarding stops.
  2. **Deduplication Cache**: Devices store recent packet UUIDs in a sliding window (LRU cache with a 5-minute expiry). If Device B receives the same packet again from another path, it is discarded in < 1 millisecond.
  3. **Peer Discovery Throttling**: Full peer discovery broadcasts run every 15–30 seconds rather than constantly, preserving battery.

### D. Offline Mesh Resilience
* Even in total blackouts, natural disasters, or remote wilderness, a chain of 4 phones can maintain full text messaging, file sharing, and SOS tracking over a 150–300 meter perimeter purely on BLE.

---

## 5. File Change Summary (Diff Overview)

| File | Change | Purpose |
| :--- | :--- | :--- |
| [`MeshRoutingEngine.kt`](file:///c:/Users/ACER/AndroidStudioProjects/gridzero/app/src/main/java/com/example/zerogrid/mesh/engine/MeshRoutingEngine.kt) | Modify relay loop | Allow relaying packets across the same transport (BLE $\to$ BLE) while preventing immediate transmitter echo. |
| [`BleMeshDriver.kt`](file:///c:/Users/ACER/AndroidStudioProjects/gridzero/app/src/main/java/com/example/zerogrid/mesh/transport/BleMeshDriver.kt) | Remove single-hop abort gate; add relay target resolution | Forward multi-hop packets to connected neighbor nodes instead of discarding when target is not directly paired. |
| [`MeshNode.kt`](file:///c:/Users/ACER/AndroidStudioProjects/gridzero/app/src/main/java/com/example/zerogrid/mesh/engine/MeshNode.kt) | Add `nextHopNodeId: String? = null` | Track which direct neighbor routes to this remote peer. |
| [`PeerTable.kt`](file:///c:/Users/ACER/AndroidStudioProjects/gridzero/app/src/main/java/com/example/zerogrid/mesh/engine/PeerTable.kt) | Track next-hop and update hop distances | Preserve multi-hop peer metadata so UI filters and routing know how to reach 2-hop peers. |
| [`MeshEngine.kt`](file:///c:/Users/ACER/AndroidStudioProjects/gridzero/app/src/main/java/com/example/zerogrid/mesh/engine/MeshEngine.kt) | Propagate multi-hop peer discovery | Parse incoming relayed discovery packets and populate peer table with `hopCount > 1`. |
