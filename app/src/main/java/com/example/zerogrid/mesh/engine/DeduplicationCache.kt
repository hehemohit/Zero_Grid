package com.example.zerogrid.mesh.engine

import java.util.concurrent.ConcurrentHashMap

/**
 * Sliding time-window cache tracking recently seen packet IDs.
 * Used for store-and-forward loop prevention in multi-hop packet flooding.
 */
class DeduplicationCache(private val expirationWindowMs: Long = 300_000) {

    private val seenPackets = ConcurrentHashMap<String, Long>()

    /**
     * Checks if packet was seen previously. If not seen, records it and returns false.
     * If already seen, returns true.
     */
    fun isDuplicateAndRecord(packetId: String): Boolean {
        pruneExpired()
        val existingTime = seenPackets.putIfAbsent(packetId, System.currentTimeMillis())
        return existingTime != null
    }

    private fun pruneExpired() {
        val now = System.currentTimeMillis()
        seenPackets.entries.removeIf { (_, timestamp) -> (now - timestamp) > expirationWindowMs }
    }

    fun clear() {
        seenPackets.clear()
    }
}
