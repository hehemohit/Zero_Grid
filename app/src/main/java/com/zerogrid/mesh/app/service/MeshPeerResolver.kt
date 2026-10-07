package com.zerogrid.mesh.app.service

import android.util.Log
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Types of physical or wireless interfaces supported by the ZeroGrid mesh transport layer.
 */
enum class NetworkInterfaceType(val displayName: String, val throughputWeight: Double) {
    BLUETOOTH_LE("Bluetooth Low Energy", 1.0),
    WIFI_DIRECT("Wi-Fi Direct", 1.25),
    WIFI_AWARE("Wi-Fi Aware", 1.20),
    LOCAL_LAN("Local Subnet", 1.50)
}

/**
 * Endpoint descriptor representing an interface through which a peer device was observed.
 */
data class PeerInterfaceEndpoint(
    val uniqueDeviceId: String,
    val interfaceType: NetworkInterfaceType,
    val address: String,
    val rssi: Int, // Signal strength in dBm (e.g. -45 dBm is stronger than -85 dBm)
    val lastSeenTimestamp: Long = System.currentTimeMillis(),
    val metadata: Map<String, String> = emptyMap()
) {
    /**
     * Checks if this endpoint report is still fresh.
     */
    fun isFresh(staleThresholdMs: Long = 45_000L): Boolean {
        return (System.currentTimeMillis() - lastSeenTimestamp) <= staleThresholdMs
    }

    /**
     * Calculates an effective route quality score combining raw signal strength (RSSI),
     * interface bandwidth capability weight, and recency.
     */
    fun computeQualityScore(): Double {
        // RSSI usually ranges between -100 dBm (weakest) and -30 dBm (strongest)
        val normalizedRssi = (rssi.coerceIn(-100, -30) + 100) / 70.0 // 0.0 to 1.0
        val ageMs = (System.currentTimeMillis() - lastSeenTimestamp).coerceAtLeast(0)
        val freshnessFactor = (1.0 - (ageMs / 45_000.0)).coerceIn(0.2, 1.0)
        return (normalizedRssi * 100.0 * interfaceType.throughputWeight) * freshnessFactor
    }
}

/**
 * Resolved route representing the optimal physical interface for communicating with a peer.
 */
data class BestPeerRoute(
    val uniqueDeviceId: String,
    val selectedInterface: NetworkInterfaceType,
    val address: String,
    val rssi: Int,
    val qualityScore: Double,
    val allAvailableInterfaces: List<PeerInterfaceEndpoint>
)

/**
 * Background Interface Deduplication Engine.
 *
 * Tracks peer endpoints discovered concurrently via multiple physical interfaces (e.g., BLE vs. Wi-Fi Direct)
 * using their unique device identifier (uniqueDeviceId).
 *
 * Automatically monitors, stores, and evaluates signal strength (RSSI) across all interfaces in the background
 * without user intervention, and resolves the single best interface for reliable multi-hop communication.
 */
class MeshPeerResolver private constructor() {

    companion object {
        private const val TAG = "MeshPeerResolver"
        private const val DEFAULT_STALE_THRESHOLD_MS = 45_000L
        private const val BACKGROUND_EVALUATION_INTERVAL_MS = 15_000L

        @Volatile
        private var INSTANCE: MeshPeerResolver? = null

        fun getInstance(): MeshPeerResolver {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MeshPeerResolver().also { INSTANCE = it }
            }
        }
    }

    // Map: uniqueDeviceId -> Map of interfaceType -> PeerInterfaceEndpoint
    private val peerEndpoints = ConcurrentHashMap<String, ConcurrentHashMap<NetworkInterfaceType, PeerInterfaceEndpoint>>()

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var evaluationJob: Job? = null

    init {
        startBackgroundEvaluationLoop()
    }

    /**
     * Starts autonomous periodic signal evaluation and stale route pruning.
     */
    private fun startBackgroundEvaluationLoop() {
        evaluationJob?.cancel()
        evaluationJob = scope.launch {
            while (isActive) {
                delay(BACKGROUND_EVALUATION_INTERVAL_MS)
                try {
                    evaluateAndPruneStaleEndpoints()
                } catch (e: Exception) {
                    Log.e(TAG, "Error during background interface evaluation", e)
                }
            }
        }
    }

    @Volatile
    private var localNodeId: String? = null

    fun setLocalNodeId(id: String) {
        localNodeId = id
    }

    /**
     * Records or updates a peer endpoint discovered over a specific wireless/network interface.
     * Called automatically by BLE and Wi-Fi drivers whenever advertisements or packets are observed.
     */
    fun recordEndpoint(
        uniqueDeviceId: String,
        interfaceType: NetworkInterfaceType,
        address: String,
        rssi: Int,
        metadata: Map<String, String> = emptyMap()
    ) {
        if (uniqueDeviceId.isBlank()) return

        val myId = localNodeId
        if (myId != null) {
            val mySuffix = myId.removePrefix("NODE-")
            if (uniqueDeviceId.equals(myId, ignoreCase = true) ||
                uniqueDeviceId.removePrefix("NODE-").equals(mySuffix, ignoreCase = true)
            ) {
                return // Drop self endpoint
            }
        }

        val deviceEndpoints = peerEndpoints.computeIfAbsent(uniqueDeviceId) {
            ConcurrentHashMap()
        }

        val endpoint = PeerInterfaceEndpoint(
            uniqueDeviceId = uniqueDeviceId,
            interfaceType = interfaceType,
            address = address,
            rssi = rssi,
            lastSeenTimestamp = System.currentTimeMillis(),
            metadata = metadata
        )

        deviceEndpoints[interfaceType] = endpoint
        scope.launch {
            Log.d(TAG, "Updated endpoint for $uniqueDeviceId via ${interfaceType.displayName} ($address, RSSI=$rssi dBm)")
        }
    }

    /**
     * Evaluates signal strength (RSSI) across all recorded interfaces for a given device,
     * and returns the single best interface with the strongest signal for communication.
     *
     * If multiple interfaces are available (e.g. device is visible on both BLE and Wi-Fi Direct),
     * this deduplicates the interfaces and selects the one with the highest quality score.
     */
    fun getBestRouteForDevice(
        uniqueDeviceId: String,
        staleThresholdMs: Long = DEFAULT_STALE_THRESHOLD_MS
    ): BestPeerRoute? {
        val deviceMap = peerEndpoints[uniqueDeviceId] ?: return null
        val now = System.currentTimeMillis()

        // Filter for fresh endpoints
        val activeEndpoints = deviceMap.values.filter { (now - it.lastSeenTimestamp) <= staleThresholdMs }
        if (activeEndpoints.isEmpty()) {
            return null
        }

        // Evaluate and select the endpoint with the highest quality score (strongest RSSI + interface weight)
        val best = activeEndpoints.maxByOrNull { it.computeQualityScore() } ?: return null

        return BestPeerRoute(
            uniqueDeviceId = uniqueDeviceId,
            selectedInterface = best.interfaceType,
            address = best.address,
            rssi = best.rssi,
            qualityScore = best.computeQualityScore(),
            allAvailableInterfaces = activeEndpoints
        )
    }

    /**
     * Returns all discovered interfaces for a given device.
     */
    fun getEndpointsForDevice(uniqueDeviceId: String): List<PeerInterfaceEndpoint> {
        return peerEndpoints[uniqueDeviceId]?.values?.toList() ?: emptyList()
    }

    /**
     * Returns all unique device IDs currently tracked across any interface.
     */
    fun getAllTrackedDevices(): Set<String> {
        return peerEndpoints.keys.toSet()
    }

    /**
     * Prunes endpoints that have timed out without fresh radio signals.
     */
    fun evaluateAndPruneStaleEndpoints(staleThresholdMs: Long = DEFAULT_STALE_THRESHOLD_MS) {
        val now = System.currentTimeMillis()
        var prunedCount = 0

        peerEndpoints.forEach { (deviceId, interfaceMap) ->
            interfaceMap.entries.removeIf { (_, endpoint) ->
                val isStale = (now - endpoint.lastSeenTimestamp) > staleThresholdMs
                if (isStale) prunedCount++
                isStale
            }
            if (interfaceMap.isEmpty()) {
                peerEndpoints.remove(deviceId)
            }
        }

        if (prunedCount > 0) {
            Log.d(TAG, "Background deduplication engine pruned $prunedCount stale interface endpoint(s)")
        }
    }

    /**
     * Explicitly removes a peer from the resolver (e.g., upon graceful disconnect).
     */
    fun removeDevice(uniqueDeviceId: String) {
        peerEndpoints.remove(uniqueDeviceId)
    }

    /**
     * Clears all recorded interface routes.
     */
    fun clear() {
        peerEndpoints.clear()
    }

    /**
     * Stops the background evaluation loop.
     */
    fun destroy() {
        evaluationJob?.cancel()
        scope.cancel()
    }
}
