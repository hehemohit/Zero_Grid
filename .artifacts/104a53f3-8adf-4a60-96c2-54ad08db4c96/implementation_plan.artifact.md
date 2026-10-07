# Implementation Plan - Phase 1: Break the Loopback

Replace local loopback transport with real device-to-device packet transmission over BLE and Wi-Fi Direct.

## User Review Required

> [!IMPORTANT]
> - BLE transmission requires establishing GATT connections to discovered peers. This can be battery-intensive and may have limits on the number of concurrent connections.
> - Wi-Fi Direct requires group formation and IP address resolution which might be more complex than the current discovery logic.
> - I will implement basic framing for TCP to ensure reliable message delivery.

## Proposed Changes

### Mesh Transport Abstraction

#### [MODIFY] [MeshTransport.kt](file:///C:/Users/user/StudioProjects/zerogrid/app/src/main/java/com/example/zerogrid/mesh/transport/MeshTransport.kt)
- Add `sourceTransport` parameter to `packetFlow` or ensure `MeshRoutingEngine` can identify the source.
- Current `packetFlow` is `SharedFlow<MeshPacket>`. I might change it to `SharedFlow<Pair<MeshPacket, MeshTransport>>` or handle it in `MeshRoutingEngine` by collecting from each transport specifically (which it already does).

### BLE Transport

#### [MODIFY] [BleMeshDriver.kt](file:///C:/Users/user/StudioProjects/zerogrid/app/src/main/java/com/example/zerogrid/mesh/transport/BleMeshDriver.kt)
- Implement GATT Client logic.
- Maintain a list of discovered `BluetoothDevice` objects.
- In `sendPacket`, if it's a broadcast, iterate through discovered devices and attempt transmission.
- Implement a simple connection manager for GATT Client.
- Remove loopback `_packetFlow.emit(packet)` from `sendPacket`.
- Handle MTU and chunking if necessary (for Phase 1, we'll keep it simple, maybe just ensuring the packet fits or using reliable write).

### Wi-Fi Direct Transport

#### [MODIFY] [WifiDirectMeshDriver.kt](file:///C:/Users/user/StudioProjects/zerogrid/app/src/main/java/com/example/zerogrid/mesh/transport/WifiDirectMeshDriver.kt)
- Hook up `WifiP2pManager.PeerListListener` to discover peer devices and their status.
- Implement `WifiP2pManager.ConnectionInfoListener` to get the group owner IP.
- Implement TCP Client to send packets to the group owner or group members.
- Implement length-prefixed framing for TCP messages.
- Remove loopback `_packetFlow.emit(packet)` from `sendPacket`.

### Mesh Engine & Routing

#### [MODIFY] [MeshRoutingEngine.kt](file:///C:/Users/user/StudioProjects/zerogrid/app/src/main/java/com/example/zerogrid/mesh/engine/MeshRoutingEngine.kt)
- Ensure `processInboundPacket` correctly identifies the `sourceTransport` to avoid echoing back.
- (Optional) Ensure deduplication logic handles the transition from loopback correctly.

## Verification Plan

### Automated Tests
- `MeshProtocolTest.kt`: Update to verify that packets are sent to transports and not just looped back.
- Add unit tests for TCP framing/unframing.
- Add unit tests for `MeshRoutingEngine` relay logic with mock transports.

### Manual Verification
- Deploy to multiple devices (simulated or real if available).
- Check logs for "Processing packet ... from ... (TTL=..., Hops=...)" on the receiving device.
- Verify that a packet sent from A reaches B via BLE.
- Verify that a packet sent from A reaches B via Wi-Fi Direct.
- Verify multi-hop: A -> B -> C.
