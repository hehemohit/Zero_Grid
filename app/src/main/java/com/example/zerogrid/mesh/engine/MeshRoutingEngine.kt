package com.example.zerogrid.mesh.engine

import android.util.Log
import com.example.zerogrid.debug.DebugLevel
import com.example.zerogrid.debug.DebugLogger
import com.example.zerogrid.mesh.transport.MeshTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * Multi-hop Store-and-Forward Routing Coordinator for ZeroGrid.
 * Implements deduplication, local delivery first, and controlled packet relay across transports.
 */
class MeshRoutingEngine(
    private val localNodeId: String,
    private val deduplicationCache: DeduplicationCache = DeduplicationCache(),
) {
    companion object {
        private const val TAG = "MeshRoutingEngine"
    }

    private val activeTransports = java.util.concurrent.CopyOnWriteArrayList<MeshTransport>()
    private val transportJobs = java.util.concurrent.ConcurrentHashMap<MeshTransport, kotlinx.coroutines.Job>()

    private val _incomingPackets = MutableSharedFlow<MeshPacket>(extraBufferCapacity = 128)
    val incomingPackets: SharedFlow<MeshPacket> = _incomingPackets.asSharedFlow()

    private val scope = CoroutineScope(Dispatchers.IO)

    @Synchronized
    fun registerTransport(transport: MeshTransport) {
        if (!activeTransports.contains(transport)) {
            activeTransports.add(transport)
            val job = scope.launch {
                transport.packetFlow.collect { packet ->
                    processInboundPacket(packet, transport)
                }
            }
            transportJobs[transport] = job
            Log.d(TAG, "Registered transport: ${transport.transportName}")
        }
    }

    @Synchronized
    fun unregisterTransport(transport: MeshTransport) {
        activeTransports.remove(transport)
        transportJobs.remove(transport)?.cancel()
        Log.d(TAG, "Unregistered transport: ${transport.transportName}")
    }

    private val lastPeerDiscoveryReceived = java.util.concurrent.ConcurrentHashMap<String, Long>()

    fun processInboundPacket(packet: MeshPacket, sourceTransport: MeshTransport? = null) {
        // 0. Drop locally-originated packets (loopback/echo prevention)
        val localSuffix = localNodeId.removePrefix("NODE-")
        if (packet.senderId.equals(localNodeId, ignoreCase = true) ||
            packet.senderId.removePrefix("NODE-").equals(localSuffix, ignoreCase = true)
        ) {
            Log.d(TAG, "Dropped self packet from sender ${packet.senderId}")
            return
        }

        // 1. Deduplication Check
        if (deduplicationCache.isDuplicateAndRecord(packet.packetId)) {
            Log.d(TAG, "Dropped duplicate or locally-originated packet: ${packet.packetId}")
            return
        }

        // 1b. PEER_DISCOVERY Throttling Guard:
        // Neighbor nodes send discovery announcements periodically.
        // If we processed an announcement from this sender in the last 15s, quietly drop it
        // to prevent routing engine churn, excessive logging, and UI state re-rendering.
        if (packet.type == PacketType.PEER_DISCOVERY) {
            val now = System.currentTimeMillis()
            val lastSeen = lastPeerDiscoveryReceived[packet.senderId] ?: 0L
            if (now - lastSeen < 15_000L) {
                return
            }
            lastPeerDiscoveryReceived[packet.senderId] = now
        }

        // 1c. HAZARD_BEACON 3-Hour TTL Guard:
        // Environmental and flood hazards change rapidly. Drop hazard beacons older than 3 hours
        // so we do not relay stale flood data across the mesh or trigger false warnings.
        if (packet.type == PacketType.HAZARD_BEACON) {
            val age = System.currentTimeMillis() - packet.timestamp
            if (age > 10_800_000L || age < -300_000L) {
                Log.d(TAG, "Dropped stale HAZARD_BEACON ${packet.packetId} (age: ${age / 1000}s)")
                return
            }
        }

        val sourceName = sourceTransport?.transportName ?: "Local"
        Log.d(
            TAG,
            "Processing packet from ${packet.senderId} (Target: ${packet.recipientId}) via $sourceName (TTL=${packet.ttl}, Hops=${packet.hopCount})"
        )
        DebugLogger.log(
            TAG,
            "Processing packet from ${packet.senderId} (Target: ${packet.recipientId}) via $sourceName",
            DebugLevel.DEBUG
        )

        // 2. Check local delivery first
        val isBroadcast = packet.recipientId == MeshPacket.BROADCAST_ADDRESS || packet.recipientId == "*"
        val isTargetedToMe = isRecipientMatch(packet.recipientId, localNodeId)

        if (isTargetedToMe || isBroadcast) {
            Log.i(TAG, "Target reached! Consuming locally: ${packet.packetId}")
            DebugLogger.log(
                TAG,
                "🎯 Target reached! Consuming locally: ${packet.packetId} (Type: ${packet.type}, From: ${packet.senderId})",
                DebugLevel.INFO
            )
            scope.launch {
                _incomingPackets.emit(packet)
            }
        }

        // 3. Multi-Hop Forwarding & Relay
        // Broadcasts and non-local packets are forwarded if TTL > 1.
        // PEER_DISCOVERY announcements are capped to max 2 hops to prevent infinite discovery broadcast loops.
        val maxHopsForType = if (packet.type == PacketType.PEER_DISCOVERY) 2 else Int.MAX_VALUE
        val shouldForward = (isBroadcast || !isTargetedToMe) && (packet.ttl > 1) && (packet.hopCount < maxHopsForType)
        if (shouldForward) {
            val relayedPacket = packet.copy(
                ttl = packet.ttl - 1,
                hopCount = packet.hopCount + 1
            )

            activeTransports.forEach { transport ->
                if (transport.isRunning) {
                    Log.d(
                        TAG,
                        "Relaying packet ${relayedPacket.packetId} to neighbor via ${transport.transportName} (Remaining TTL=${relayedPacket.ttl}, Hops=${relayedPacket.hopCount})"
                    )
                    DebugLogger.log(
                        TAG,
                        "🔄 Relaying packet ${relayedPacket.packetId} via ${transport.transportName} (TTL=${relayedPacket.ttl}, Hops=${relayedPacket.hopCount})",
                        DebugLevel.DEBUG
                    )
                    transport.sendPacket(relayedPacket)
                }
            }
        }
    }

    /**
     * Checks if the recipientId matches our local node identity, handling:
     * - Exact logical NodeID ("NODE-d48923d7")
     * - Case-insensitive match
     * - Node suffix / prefix ("d48923d7" vs "NODE-d48923d7")
     * - Direct single-hop MAC-addressed delivery over BLE
     */
    private fun isRecipientMatch(recipientId: String, localNodeId: String): Boolean {
        if (recipientId.equals(localNodeId, ignoreCase = true)) return true
        if (recipientId.equals(localNodeId.removePrefix("NODE-"), ignoreCase = true)) return true
        if (localNodeId.equals("NODE-$recipientId", ignoreCase = true)) return true
        // If recipientId is a raw hardware address (e.g. "XX:XX:XX:..."), direct delivery is accepted
        if (recipientId.contains(":") && recipientId.length >= 17) return true
        return false
    }

    fun sendOutboundPacket(packet: MeshPacket, preferredTransport: String? = null): Boolean {
        // Record local packet in deduplication cache to prevent re-processing if it returns
        deduplicationCache.isDuplicateAndRecord(packet.packetId)

        Log.d(TAG, "Sending outbound packet ${packet.packetId} (Type: ${packet.type}, Preferred: $preferredTransport) to ${packet.recipientId}")
        DebugLogger.log(TAG, "📤 Outbound ${packet.type} to ${packet.recipientId} (Pref: $preferredTransport)", DebugLevel.DEBUG)

        val isBroadcast = packet.recipientId == MeshPacket.BROADCAST_ADDRESS || packet.recipientId == "*"

        // If an explicit transport was chosen (BLE or Wi-Fi Direct), target that specific transport driver
        if (!preferredTransport.isNullOrBlank()) {
            val targetedTransport = activeTransports.firstOrNull { it.transportName.equals(preferredTransport, ignoreCase = true) && it.isRunning }
            if (targetedTransport != null) {
                val sent = if (isBroadcast) targetedTransport.sendPacket(packet) else targetedTransport.sendPacket(packet, packet.recipientId)
                if (sent) {
                    Log.d(TAG, "Successfully sent packet ${packet.packetId} via explicit transport $preferredTransport")
                    return true
                } else {
                    Log.w(TAG, "Failed sending packet ${packet.packetId} via explicit transport $preferredTransport")
                    return false
                }
            } else {
                Log.w(TAG, "Requested explicit transport $preferredTransport is not running/available")
                return false
            }
        }

        // Dynamic multi-interface evaluation for direct unicast packets (Auto mode)
        if (!isBroadcast) {
            val bestRoute = com.zerogrid.mesh.app.service.MeshPeerResolver.getInstance()
                .getBestRouteForDevice(packet.recipientId)

            if (bestRoute != null) {
                val preferredTransportName = when (bestRoute.selectedInterface) {
                    com.zerogrid.mesh.app.service.NetworkInterfaceType.WIFI_DIRECT -> MeshNode.TRANSPORT_WIFI_DIRECT
                    else -> MeshNode.TRANSPORT_BLE
                }

                Log.d(
                    TAG,
                    "Dynamic route selected for ${packet.recipientId}: $preferredTransportName " +
                    "(RSSI=${bestRoute.rssi} dBm, score=${bestRoute.qualityScore})"
                )

                // 1. Send via preferred transport with strongest signal
                val transport = activeTransports.firstOrNull { it.transportName == preferredTransportName && it.isRunning }
                if (transport != null && transport.sendPacket(packet, packet.recipientId)) {
                    Log.d(TAG, "Successfully transmitted packet ${packet.packetId} via preferred $preferredTransportName")
                    return true
                }

                // 2. Seamlessly fallback to alternative transport if preferred fails
                val fallbackTransports = activeTransports.filter { it.transportName != preferredTransportName && it.isRunning }
                for (fallback in fallbackTransports) {
                    Log.d(TAG, "Falling back to ${fallback.transportName} for packet ${packet.packetId}")
                    if (fallback.sendPacket(packet, packet.recipientId)) {
                        return true
                    }
                }
            }
        }

        // Broadcast or fallback to all active transports
        var sentCount = 0
        activeTransports.forEach { transport ->
            if (transport.isRunning) {
                Log.d(TAG, "Attempting transmission via ${transport.transportName}")
                if (transport.sendPacket(packet)) {
                    sentCount++
                }
            }
        }
        return sentCount > 0
    }
}
