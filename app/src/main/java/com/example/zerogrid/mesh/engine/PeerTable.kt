package com.example.zerogrid.mesh.engine

import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe dynamic peer discovery registry and dynamic routing table.
 * Implements multi-interface deduplication (BLE vs Wi-Fi Direct), automatic RSSI signal
 * evaluation, and canonical identity merging.
 */
class PeerTable(var localNodeId: String? = null) {

    private val lock = Any()
    private val peers = ConcurrentHashMap<String, MeshNode>()

    /**
     * Updates or inserts a peer node. If the device was previously observed on another
     * physical interface (e.g. BLE vs Wi-Fi), merges the profiles into a single logical
     * peer and automatically selects whichever transport currently offers the stronger RSSI signal.
     */
    fun updateOrAddPeer(node: MeshNode) = synchronized(lock) {
        val myId = localNodeId
        if (myId != null) {
            val mySuffix = myId.removePrefix("NODE-")
            if (node.nodeId.equals(myId, ignoreCase = true) ||
                node.nodeId.removePrefix("NODE-").equals(mySuffix, ignoreCase = true) ||
                node.alias.equals(android.os.Build.MODEL, ignoreCase = true)) {
                return // Guard: Never register self as a peer
            }
        }

        val existing = peers[node.nodeId]
        if (existing == null) {
            if (node.transportType == MeshNode.TRANSPORT_BLE) {
                node.bleRssi = node.rssi
                node.availableTransports.add(MeshNode.TRANSPORT_BLE)
            } else if (node.transportType == MeshNode.TRANSPORT_WIFI_DIRECT) {
                node.wifiRssi = node.rssi
                node.availableTransports.add(MeshNode.TRANSPORT_WIFI_DIRECT)
            }
            node.availableTransports.addAll(node.availableTransports)
            peers[node.nodeId] = node
        } else {
            existing.lastSeenTimestamp = System.currentTimeMillis()

            // Merge interfaces and update per-interface signal strengths
            if (node.transportType == MeshNode.TRANSPORT_BLE) {
                existing.bleRssi = node.rssi
                existing.availableTransports.add(MeshNode.TRANSPORT_BLE)
            } else if (node.transportType == MeshNode.TRANSPORT_WIFI_DIRECT) {
                existing.wifiRssi = node.rssi
                existing.availableTransports.add(MeshNode.TRANSPORT_WIFI_DIRECT)
            }
            existing.availableTransports.addAll(node.availableTransports)

            // Update transport type and RSSI to the physical interface actively receiving this update
            existing.rssi = node.rssi
            existing.transportType = node.transportType

            // Update alias if incoming is a real custom display name
            if (isRealDisplayName(node.alias)) {
                existing.alias = node.alias
            } else if (!isRealDisplayName(existing.alias) && node.alias.isNotBlank()) {
                existing.alias = node.alias
            }

            if (node.hopDistance <= existing.hopDistance) {
                existing.hopDistance = node.hopDistance
                existing.isDirectNeighbor = node.isDirectNeighbor
                if (node.nextHopNodeId != null) {
                    existing.nextHopNodeId = node.nextHopNodeId
                }
            } else if (existing.nextHopNodeId == null && node.nextHopNodeId != null) {
                existing.nextHopNodeId = node.nextHopNodeId
            }
        }
    }

    /**
     * Resolves the immediate 1-hop physical neighbor node ID used to route packets to a target peer.
     * Returns the target itself if directly reachable, or the next-hop relay node ID.
     */
    fun getNextHop(nodeId: String): String? = synchronized(lock) {
        val directMatch = peers[nodeId]
        if (directMatch != null) {
            if (directMatch.isDirectNeighbor) return nodeId
            return directMatch.nextHopNodeId ?: nodeId
        }
        val suffixMatch = peers.entries.firstOrNull { 
            it.key.removePrefix("NODE-").equals(nodeId.removePrefix("NODE-"), ignoreCase = true) 
        }?.value
        if (suffixMatch != null) {
            if (suffixMatch.isDirectNeighbor) return suffixMatch.nodeId
            return suffixMatch.nextHopNodeId ?: suffixMatch.nodeId
        }
        return null
    }

    /**
     * Checks whether an alias is a user-chosen display name rather than a system fallback.
     */
    fun isRealDisplayName(alias: String): Boolean {
        if (alias.isBlank()) return false
        if (alias.startsWith("Peer ") || alias == "Peer") return false
        if (alias.startsWith("Android_")) return false
        if (alias.startsWith("Wi-Fi ")) return false
        if (alias.contains(":") && alias.length >= 17) return false // MAC address
        return true
    }

    /**
     * Merges a temporary MAC-addressed peer entry into its canonical logical Node ID.
     */
    fun mergePeer(fromNodeId: String, toNodeId: String) = synchronized(lock) {
        if (fromNodeId.equals(toNodeId, ignoreCase = true)) return
        val old = peers.remove(fromNodeId) ?: return
        val target = peers[toNodeId]
        if (target != null) {
            if (isRealDisplayName(old.alias) && !isRealDisplayName(target.alias)) {
                target.alias = old.alias
            }
            if (old.wifiRssi != null) target.wifiRssi = old.wifiRssi
            if (old.bleRssi != null) target.bleRssi = old.bleRssi
            target.availableTransports.addAll(old.availableTransports)
            target.rssi = target.getBestSignalRssi()
        }
    }

    fun removePeer(nodeId: String) = synchronized(lock) {
        peers.remove(nodeId)
    }

    fun getPeer(nodeId: String): MeshNode? = synchronized(lock) {
        return peers[nodeId]
    }

    fun getAllPeers(transportFilter: String? = null): List<MeshNode> = synchronized(lock) {
        val myId = localNodeId
        return peers.values
            .filter { peer ->
                val isNotSelf = if (myId != null) {
                    !peer.nodeId.equals(myId, ignoreCase = true) &&
                    !peer.nodeId.removePrefix("NODE-").equals(myId.removePrefix("NODE-"), ignoreCase = true) &&
                    !peer.alias.equals(android.os.Build.MODEL, ignoreCase = true)
                } else true
                val matchesTransport = if (transportFilter != null) {
                    peer.transportType.equals(transportFilter, ignoreCase = true)
                } else true
                isNotSelf && matchesTransport
            }
            .map { it.copy(availableTransports = java.util.concurrent.ConcurrentHashMap.newKeySet<String>().apply { addAll(it.availableTransports) }) }
            .sortedByDescending { it.lastSeenTimestamp }
    }

    fun removePeersByTransport(transportType: String) = synchronized(lock) {
        peers.entries.removeIf { entry ->
            entry.value.transportType.equals(transportType, ignoreCase = true)
        }
    }

    fun clearInactiveTransportState(activeTransport: String) = synchronized(lock) {
        peers.values.forEach { peer ->
            if (activeTransport.equals(MeshNode.TRANSPORT_BLE, ignoreCase = true)) {
                peer.wifiRssi = null
                peer.availableTransports.remove(MeshNode.TRANSPORT_WIFI_DIRECT)
            } else if (activeTransport.equals(MeshNode.TRANSPORT_WIFI_DIRECT, ignoreCase = true)) {
                peer.bleRssi = null
                peer.availableTransports.remove(MeshNode.TRANSPORT_BLE)
            }
        }
    }

    fun getDirectNeighbors(): List<MeshNode> = synchronized(lock) {
        val myId = localNodeId
        return peers.values
            .filter { peer ->
                val isNotSelf = if (myId != null) {
                    !peer.nodeId.equals(myId, ignoreCase = true) &&
                    !peer.nodeId.removePrefix("NODE-").equals(myId.removePrefix("NODE-"), ignoreCase = true) &&
                    !peer.alias.equals(android.os.Build.MODEL, ignoreCase = true)
                } else true
                peer.isDirectNeighbor && isNotSelf
            }
            .map { it.copy(availableTransports = java.util.concurrent.ConcurrentHashMap.newKeySet<String>().apply { addAll(it.availableTransports) }) }
            .sortedByDescending { it.rssi }
    }

    fun pruneStalePeers(staleThresholdMs: Long = 60_000) = synchronized(lock) {
        val now = System.currentTimeMillis()
        peers.entries.removeIf { entry -> (now - entry.value.lastSeenTimestamp) > staleThresholdMs }
    }

    fun clear() = synchronized(lock) {
        peers.clear()
    }
}
