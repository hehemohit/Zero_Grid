package com.example.zerogrid.mesh.engine

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import com.example.zerogrid.messaging.MessageStatus
import com.example.zerogrid.messaging.MessageStore
import com.example.zerogrid.messaging.StoredMessage
import com.example.zerogrid.mesh.transport.BleMeshDriver
import com.example.zerogrid.mesh.transport.WifiDirectMeshDriver
import com.example.zerogrid.network.AcknowledgeSosRequest
import com.example.zerogrid.network.RetrofitInstance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.UUID
import android.location.Location
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.zerogrid.emergency.SosUploadWorker
import com.example.zerogrid.location.LocationHelper
import com.example.zerogrid.network.SosDispatchRequest
import com.example.zerogrid.service.MeshForegroundService

/**
 * Central Mesh Engine orchestrator for ZeroGrid.
 * Manages peer discovery, active transports (BLE & Wi-Fi Direct),
 * multi-hop store-and-forward routing, SOS beacons, channels, and persistent direct messaging.
 */
class MeshEngine private constructor(private val context: Context) {

    companion object {
        private const val TAG = "MeshEngine"
        private const val PREFS_NAME = "zerogrid_identity_prefs"
        private const val KEY_NODE_ID = "local_node_id"
        private const val KEY_DISPLAY_NAME = "display_name"
        private const val KEY_ACKNOWLEDGED_ALERT_IDS = "acknowledged_alert_ids"

        @Volatile
        private var INSTANCE: MeshEngine? = null

        fun getInstance(context: Context): MeshEngine {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MeshEngine(context.applicationContext).also { INSTANCE = it }
            }
        }

        private fun getOrGenerateLocalNodeId(context: Context): String {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            var id = prefs.getString(KEY_NODE_ID, null)
            if (id.isNullOrEmpty()) {
                val hex = UUID.randomUUID().toString().replace("-", "").take(8).lowercase()
                id = "NODE-$hex"
                prefs.edit().putString(KEY_NODE_ID, id).apply()
            }
            return id
        }

        private fun getOrGenerateDisplayName(context: Context, localNodeId: String): String {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            var name = prefs.getString(KEY_DISPLAY_NAME, null)
            if (name.isNullOrEmpty()) {
                val btName = try {
                    val bm = context.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager
                    bm?.adapter?.name
                } catch (_: Exception) { null }
                name = if (!btName.isNullOrBlank() && btName != "null") btName else android.os.Build.MODEL
                if (name.isNullOrBlank() || name == "null") name = "Node-${localNodeId.takeLast(4)}"
                prefs.edit().putString(KEY_DISPLAY_NAME, name).apply()
            }
            return name
        }
    }

    val localNodeId: String = getOrGenerateLocalNodeId(context)
    private val peerTable = PeerTable(localNodeId)
    private val routingEngine = MeshRoutingEngine(localNodeId)
    private val messageStore = MessageStore.getInstance(context)

    // Synchronization lock protecting live state updates across transport/callback threads
    private val stateLock = Any()

    private val _displayName = MutableStateFlow(getOrGenerateDisplayName(context, localNodeId))
    val displayName: StateFlow<String> = _displayName.asStateFlow()

    fun setCustomDisplayName(name: String) {
        _displayName.value = name.trim()
    }

    private val _activeChannelMode = MutableStateFlow(MeshChannelMode.getSavedMode(context))
    val activeChannelMode: StateFlow<MeshChannelMode> = _activeChannelMode.asStateFlow()

    private val bleDriver: BleMeshDriver = BleMeshDriver(context, localNodeId, _displayName.value)
    private val wifiDirectDriver: WifiDirectMeshDriver = WifiDirectMeshDriver(context, localNodeId, _displayName.value)

    private val transports = listOf(bleDriver, wifiDirectDriver)

    private val _connectedPeers = MutableStateFlow<List<MeshNode>>(emptyList())
    val connectedPeers: StateFlow<List<MeshNode>> = _connectedPeers.asStateFlow()

    private val _receivedMessages = MutableStateFlow<List<MeshPacket>>(emptyList())
    val receivedMessages: StateFlow<List<MeshPacket>> = _receivedMessages.asStateFlow()

    private val _sosAlerts = MutableStateFlow<List<MeshPacket>>(emptyList())
    val sosAlerts: StateFlow<List<MeshPacket>> = _sosAlerts.asStateFlow()

    private val _hazardAlerts = MutableStateFlow<List<HazardAlert>>(emptyList())
    val hazardAlerts: StateFlow<List<HazardAlert>> = _hazardAlerts.asStateFlow()

    private val _proximityWarning = MutableStateFlow<ProximityWarning?>(null)
    val proximityWarning: StateFlow<ProximityWarning?> = _proximityWarning.asStateFlow()

    private val _dataMuleQueueSize = MutableStateFlow(0)
    val dataMuleQueueSize: StateFlow<Int> = _dataMuleQueueSize.asStateFlow()

    private val _acknowledgedAlertIds = MutableStateFlow<Set<String>>(
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getStringSet(KEY_ACKNOWLEDGED_ALERT_IDS, emptySet())?.toSet() ?: emptySet()
    )
    val acknowledgedAlertIds: StateFlow<Set<String>> = _acknowledgedAlertIds.asStateFlow()

    private val _packetsRelayedCount = MutableStateFlow(0)
    val packetsRelayedCount: StateFlow<Int> = _packetsRelayedCount.asStateFlow()

    private val _isMeshActive = MutableStateFlow(value = false)
    val isMeshActive: StateFlow<Boolean> = _isMeshActive.asStateFlow()

    /**
     * In-memory conversation map: peerId -> list of StoredMessages.
     * Loaded from MessageStore at startup. Updated live as messages arrive or are sent.
     */
    private val _conversations = MutableStateFlow<Map<String, List<StoredMessage>>>(emptyMap())
    val conversations: StateFlow<Map<String, List<StoredMessage>>> = _conversations.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)

    /**
     * Publishes connected peers strictly filtered to the current active transport channel mode.
     */
    fun updateConnectedPeers() {
        val activeTransport = _activeChannelMode.value.transportName
        _connectedPeers.value = peerTable.getAllPeers(activeTransport)
    }

    private val hardwareStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    when (state) {
                        BluetoothAdapter.STATE_TURNING_OFF, BluetoothAdapter.STATE_OFF -> {
                            Log.w(TAG, "Bluetooth turned OFF: stopping BLE discovery and flushing BLE peers")
                            bleDriver.stopDiscovery()
                            if (_activeChannelMode.value == MeshChannelMode.BLE) {
                                peerTable.removePeersByTransport(MeshNode.TRANSPORT_BLE)
                                updateConnectedPeers()
                            }
                        }
                        BluetoothAdapter.STATE_ON -> {
                            Log.i(TAG, "Bluetooth turned ON")
                            if (_activeChannelMode.value == MeshChannelMode.BLE && _isMeshActive.value) {
                                Log.i(TAG, "Restarting BLE discovery after Bluetooth re-enabled")
                                bleDriver.startDiscovery()
                                broadcastPeerAnnounce()
                                updateConnectedPeers()
                            }
                        }
                    }
                }
                WifiManager.WIFI_STATE_CHANGED_ACTION -> {
                    val state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN)
                    if (state == WifiManager.WIFI_STATE_DISABLED && _activeChannelMode.value == MeshChannelMode.WIFI_DIRECT) {
                        Log.w(TAG, "Wi-Fi turned OFF: flushing Wi-Fi Direct peers")
                        peerTable.removePeersByTransport(MeshNode.TRANSPORT_WIFI_DIRECT)
                        updateConnectedPeers()
                    }
                }
            }
        }
    }

    init {
        com.zerogrid.mesh.app.service.MeshPeerResolver.getInstance().setLocalNodeId(localNodeId)

        try {
            val filter = IntentFilter().apply {
                addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
                addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(hardwareStateReceiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(hardwareStateReceiver, filter)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register hardware state receiver in MeshEngine", e)
        }

        // Zero-waste ephemeral messaging: purge persisted disk conversations so each app launch starts completely clean
        scope.launch {
            messageStore.clearAllMessages()
            synchronized(stateLock) {
                _conversations.value = emptyMap()
            }
            Log.d(TAG, "Cleared disk conversation cache for zero-waste ephemeral session")
        }

        transports.forEach { transport ->
            scope.launch {
                transport.peerDiscoveryFlow.collect { peer ->
                    val localSuffix = localNodeId.removePrefix("NODE-")
                    if (peer.nodeId.equals(localNodeId, ignoreCase = true) ||
                        peer.nodeId.removePrefix("NODE-").equals(localSuffix, ignoreCase = true) ||
                        peer.alias.equals(android.os.Build.MODEL, ignoreCase = true) ||
                        peer.alias.equals(_displayName.value, ignoreCase = true)) {
                        return@collect // Filter out own device
                    }
                    // Only register peer if it matches our active channel mode
                    if (peer.transportType.equals(_activeChannelMode.value.transportName, ignoreCase = true)) {
                        peerTable.updateOrAddPeer(peer)
                        updateConnectedPeers()
                    }
                }
            }
        }

        scope.launch {
            routingEngine.incomingPackets.collect { packet ->
                handleIncomingPacket(packet)
            }
        }

        // Periodic peer announcement broadcast with adaptive backoff to prevent flooding
        scope.launch {
            var lastPeerCount = -1
            var interval = 10_000L
            while (true) {
                delay(interval)
                try {
                    if (_isMeshActive.value) {
                        broadcastPeerAnnounce()
                        val currentPeerCount = _connectedPeers.value.size
                        if (currentPeerCount == lastPeerCount && currentPeerCount > 0) {
                            // Stable topology: incrementally back off up to 30 seconds to prevent packet storms
                            interval = (interval + 5_000L).coerceAtMost(30_000L)
                        } else {
                            // Topology changed or empty: announce at 10-second baseline
                            interval = 10_000L
                            lastPeerCount = currentPeerCount
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Exception in peer announce loop", e)
                }
            }
        }

        // 15-second background loop for paused messages, hazard cache pruning, and proximity warnings
        scope.launch {
            while (true) {
                delay(15_000L)
                try {
                    retryPausedMessages()
                    pruneExpiredHazards()
                    val myLoc = LocationHelper.getLastKnownLocation(context)
                    if (myLoc != null) {
                        computeProximityWarning(myLoc.lat, myLoc.lng)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Exception in 15s maintenance loop", e)
                }
            }
        }

        // Live reactive observer for opportunistic Data Mule queue in WorkManager
        scope.launch {
            try {
                WorkManager.getInstance(context)
                    .getWorkInfosByTagFlow("SOS_OFFLINE_UPLOAD")
                    .collect { workList ->
                        val pendingCount = workList.count {
                            it.state == WorkInfo.State.ENQUEUED ||
                            it.state == WorkInfo.State.RUNNING ||
                            it.state == WorkInfo.State.BLOCKED
                        }
                        _dataMuleQueueSize.value = pendingCount
                    }
            } catch (e: Exception) {
                Log.w(TAG, "Unable to observe WorkManager Data Mule queue", e)
            }
        }
    }

    /** Returns the conversation history for a specific peer (live StateFlow slice). */
    fun getConversation(peerId: String): List<StoredMessage> {
        return _conversations.value[peerId] ?: emptyList()
    }

    /** All peer IDs that have at least one stored message, sorted by most recent, excluding own device. */
    fun getConversationPeerIds(): List<String> {
        val localSuffix = localNodeId.removePrefix("NODE-")
        return messageStore.getAllConversationPeerIds().filter { peerId ->
            val alias = messageStore.getPeerDisplayName(peerId)
            !peerId.equals(localNodeId, ignoreCase = true) &&
            !peerId.removePrefix("NODE-").equals(localSuffix, ignoreCase = true) &&
            !alias.equals(android.os.Build.MODEL, ignoreCase = true) &&
            !alias.equals(_displayName.value, ignoreCase = true)
        }
    }

    /** Returns formatted or known custom display name for a given peer ID. */
    fun getPeerDisplayName(peerId: String): String = messageStore.getPeerDisplayName(peerId)

    /** Wipes all conversations in-memory and on disk immediately for zero-waste session clearing. */
    fun clearAllConversations() {
        messageStore.clearAllMessages()
        synchronized(stateLock) {
            _conversations.value = emptyMap()
        }
    }

    /** Removes a specific peer conversation in-memory and on disk. */
    fun deleteConversation(peerId: String) {
        messageStore.deleteConversation(peerId)
        synchronized(stateLock) {
            val updated = _conversations.value.toMutableMap()
            updated.remove(peerId)
            _conversations.value = updated
        }
    }

    /** Broadcasts our user-selected display name across all mesh transports. */
    fun broadcastPeerAnnounce() {
        val announcePacket = MeshPacket(
            senderId = localNodeId,
            recipientId = MeshPacket.BROADCAST_ADDRESS,
            type = PacketType.PEER_DISCOVERY,
            payload = _displayName.value
        )
        routingEngine.sendOutboundPacket(announcePacket)
    }

    fun setDisplayName(name: String) {
        if (name.isNotBlank()) {
            val trimmed = name.trim()
            _displayName.value = trimmed
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putString(KEY_DISPLAY_NAME, trimmed).apply()
            bleDriver.updateDisplayName(trimmed)
            wifiDirectDriver.updateDisplayName(trimmed)
            broadcastPeerAnnounce()
        }
    }

    private fun persistAcknowledgedAlertId(id: String) {
        val updated = _acknowledgedAlertIds.value + id
        _acknowledgedAlertIds.value = updated
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY_ACKNOWLEDGED_ALERT_IDS, updated)
            .apply()
    }

    fun acknowledgeSosAlert(packetId: String) {
        persistAcknowledgedAlertId(packetId)
    }

    /**
     * Acknowledge a cloud/relative SOS alert:
     * 1. Immediately marks it locally as acknowledged in [acknowledgedAlertIds] and SharedPreferences.
     * 2. Calls the backend PUT /api/sos/:id/acknowledge asynchronously.
     * confirmedSafe = true  → Relative SOS: "Are you sure he/she is safe?"
     * confirmedSafe = false → Local area SOS injected from cloud: "situation attended to"
     */
    fun acknowledgeCloudSosAlert(sosId: String, confirmedSafe: Boolean = true) {
        // Immediately update local UI state and persistence
        persistAcknowledgedAlertId(sosId)
        // Fire backend call asynchronously
        scope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitInstance.sosApi.acknowledgeSos(
                    id = sosId,
                    body = AcknowledgeSosRequest(confirmedSafe = confirmedSafe)
                )
                if (response.isSuccessful) {
                    Log.d(TAG, "Cloud SOS $sosId acknowledged on backend (confirmedSafe=$confirmedSafe)")
                } else {
                    Log.w(TAG, "Backend acknowledge returned ${response.code()} for SOS $sosId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to acknowledge cloud SOS $sosId on backend", e)
                // Local state already updated — user sees it dismissed regardless of network
            }
        }
    }

    fun getPublicKeyFingerprint(): String {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val hash = md.digest(localNodeId.toByteArray(Charsets.UTF_8))
            hash.take(16).joinToString(":") { "%02X".format(it) }
        } catch (_: Exception) {
            "ED:25:51:90:7A:42:00:FF:AA:BB:CC:DD:EE:FF:11:22"
        }
    }

    fun getBleRunning(): Boolean = bleDriver.isRunning
    fun getWifiDirectRunning(): Boolean = wifiDirectDriver.isRunning

    fun setMeshChannelMode(mode: MeshChannelMode) {
        if (_activeChannelMode.value == mode) return
        val oldMode = _activeChannelMode.value
        Log.d(TAG, "Switching mesh channel mode from $oldMode to $mode")
        _activeChannelMode.value = mode
        MeshChannelMode.saveMode(context, mode)

        if (_isMeshActive.value) {
            if (mode == MeshChannelMode.BLE) {
                wifiDirectDriver.stopDiscovery()
                routingEngine.unregisterTransport(wifiDirectDriver)
                routingEngine.registerTransport(bleDriver)
                bleDriver.startDiscovery()
            } else {
                bleDriver.stopDiscovery()
                routingEngine.unregisterTransport(bleDriver)
                routingEngine.registerTransport(wifiDirectDriver)
                wifiDirectDriver.startDiscovery()
            }
            // Purge peers and stale interface state belonging to the inactive transport
            peerTable.removePeersByTransport(oldMode.transportName)
            peerTable.clearInactiveTransportState(mode.transportName)
            updateConnectedPeers()
        }
    }

    fun startMesh() {
        val mode = _activeChannelMode.value
        Log.d(TAG, "Starting ZeroGrid Mesh Engine in $mode mode (Node ID: $localNodeId, Name: ${_displayName.value})")
        val inactiveTransport = if (mode == MeshChannelMode.BLE) MeshNode.TRANSPORT_WIFI_DIRECT else MeshNode.TRANSPORT_BLE
        peerTable.removePeersByTransport(inactiveTransport)
        peerTable.clearInactiveTransportState(mode.transportName)

        if (mode == MeshChannelMode.BLE) {
            wifiDirectDriver.stopDiscovery()
            routingEngine.unregisterTransport(wifiDirectDriver)
            routingEngine.registerTransport(bleDriver)
            bleDriver.startDiscovery()
        } else {
            bleDriver.stopDiscovery()
            routingEngine.unregisterTransport(bleDriver)
            routingEngine.registerTransport(wifiDirectDriver)
            wifiDirectDriver.startDiscovery()
        }
        _isMeshActive.value = true
        updateConnectedPeers()
    }

    fun stopMesh() {
        Log.d(TAG, "Stopping ZeroGrid Mesh Engine")
        bleDriver.stopDiscovery()
        wifiDirectDriver.stopDiscovery()
        routingEngine.unregisterTransport(bleDriver)
        routingEngine.unregisterTransport(wifiDirectDriver)
        _isMeshActive.value = false
        _connectedPeers.value = emptyList()
    }

    /** Checks if a peer is currently reachable or active on the mesh. */
    fun isPeerOnline(peerId: String): Boolean {
        val peer = peerTable.getPeer(peerId)
        val isFreshInTable = peer != null && (System.currentTimeMillis() - peer.lastSeenTimestamp <= 90_000L)
        val mode = _activeChannelMode.value
        return if (mode == MeshChannelMode.BLE) {
            isFreshInTable && (bleDriver.isPeerReachable(peerId) || peer?.transportType == MeshNode.TRANSPORT_BLE)
        } else {
            isFreshInTable && (wifiDirectDriver.isPeerReachable(peerId) || peer?.transportType == MeshNode.TRANSPORT_WIFI_DIRECT)
        }
    }

    /**
     * Send a direct message to a peer.
     * Transmits strictly over the active channel mode (BLE or Wi-Fi Direct).
     */
    fun sendDirectMessage(recipientId: String, text: String, preferredTransport: String? = null): MeshPacket {
        val packet = MeshPacket(
            senderId = localNodeId,
            recipientId = recipientId,
            type = PacketType.DIRECT_MESSAGE,
            payload = text,
        )

        val transportToUse = preferredTransport ?: _activeChannelMode.value.transportName
        val sent = routingEngine.sendOutboundPacket(packet, transportToUse)
        val isOnline = sent || isPeerOnline(recipientId)
        val status = if (sent) MessageStatus.SENT else (if (isOnline) MessageStatus.SENT else MessageStatus.PAUSED)

        // Persist the sent message to local store immediately
        val stored = StoredMessage(
            id = packet.packetId,
            senderId = localNodeId,
            recipientId = recipientId,
            text = text,
            timestamp = packet.timestamp,
            hopCount = 0,
            isMine = true,
            status = status
        )
        persistAndUpdateConversation(recipientId, stored)

        return packet
    }

    /** Manually retries sending a paused message. Returns true if sent successfully. */
    fun retryMessage(peerId: String, messageId: String): Boolean {
        val msgs = _conversations.value[peerId] ?: return false
        val msg = msgs.firstOrNull { it.id == messageId } ?: return false
        if (!isPeerOnline(peerId)) {
            Log.d(TAG, "Cannot retry message $messageId: peer $peerId is still offline")
            return false
        }
        val packet = MeshPacket(
            packetId = msg.id,
            senderId = localNodeId,
            recipientId = peerId,
            type = PacketType.DIRECT_MESSAGE,
            payload = msg.text,
            timestamp = System.currentTimeMillis()
        )
        val sent = routingEngine.sendOutboundPacket(packet, _activeChannelMode.value.transportName)
        if (sent) {
            updateMessageStatus(peerId, messageId, MessageStatus.SENT)
            return true
        }
        return false
    }

    /** Updates a message's status both on disk and in the live in-memory StateFlow safely. */
    fun updateMessageStatus(peerId: String, messageId: String, newStatus: MessageStatus) = synchronized(stateLock) {
        messageStore.updateMessageStatus(peerId, messageId, newStatus)
        val updated = _conversations.value.toMutableMap()
        val list = updated[peerId]?.toMutableList() ?: return
        val idx = list.indexOfFirst { it.id == messageId }
        if (idx != -1) {
            list[idx] = list[idx].copy(status = newStatus)
            updated[peerId] = list
            _conversations.value = updated
        }
    }

    /** Prunes stale peers and retries delivery for all paused messages destined for online peers. */
    fun retryPausedMessages() {
        peerTable.pruneStalePeers(90_000L)
        updateConnectedPeers()

        val allConvs = _conversations.value
        allConvs.forEach { (peerId, msgs) ->
            val paused = msgs.filter { it.isMine && it.status == MessageStatus.PAUSED }
            if (paused.isNotEmpty() && isPeerOnline(peerId)) {
                Log.d(TAG, "Background retry: peer $peerId online, retrying ${paused.size} paused message(s)")
                paused.forEach { msg ->
                    val packet = MeshPacket(
                        packetId = msg.id,
                        senderId = localNodeId,
                        recipientId = peerId,
                        type = PacketType.DIRECT_MESSAGE,
                        payload = msg.text,
                        timestamp = System.currentTimeMillis()
                    )
                    val sent = routingEngine.sendOutboundPacket(packet)
                    if (sent) {
                        Log.d(TAG, "Background retry succeeded for message ${msg.id} to $peerId")
                        updateMessageStatus(peerId, msg.id, MessageStatus.SENT)
                    }
                }
            }
        }
    }

    fun broadcastChannelMessage(channelName: String, text: String, preferredTransport: String? = null): MeshPacket {
        val payload = "[$channelName] $text"
        val packet = MeshPacket(
            senderId = localNodeId,
            recipientId = MeshPacket.BROADCAST_ADDRESS,
            type = PacketType.CHANNEL_BROADCAST,
            payload = payload,
        )
        routingEngine.sendOutboundPacket(packet, preferredTransport)
        return packet
    }

    fun triggerSosBeacon(
        category: String,
        message: String,
        lat: Double? = null,
        lon: Double? = null,
        accuracy: Float? = null,
        preferredTransport: String? = null,
        senderName: String? = null
    ): MeshPacket {
        val effectiveSenderName = senderName ?: _displayName.value.ifBlank { null }
        // Use structured JSON payload so receiving devices can extract coordinates precisely
        val payload = MeshPacket.buildSosPayload(
            category = category,
            message = message,
            lat = lat ?: 0.0,
            lng = lon ?: 0.0,
            accuracy = accuracy,
            senderName = effectiveSenderName
        )
        val packet = MeshPacket(
            senderId = localNodeId,
            recipientId = MeshPacket.BROADCAST_ADDRESS,
            ttl = 10,
            type = PacketType.SOS_BEACON,
            payload = payload,
        )
        // Transmit to mesh peers via active transports (or targeted transport)
        routingEngine.sendOutboundPacket(packet, preferredTransport)
        // Add to local sosAlerts for display on THIS device's SOS center
        val current = _sosAlerts.value.toMutableList()
        if (current.none { it.packetId == packet.packetId }) {
            current.add(0, packet)
            _sosAlerts.value = current
        }
        return packet
    }

    /**
     * Broadcasts an environmental hazard alert (e.g. waterlogging, submerged underpass, heatwave)
     * over the mesh network and updates local state.
     */
    fun triggerHazardBeacon(
        category: String,
        waterDepthCm: Int = 0,
        passability: String = "ALL_PASSABLE",
        message: String = "",
        lat: Double? = null,
        lon: Double? = null,
        accuracy: Float? = null,
        preferredTransport: String? = null,
        senderName: String? = null
    ): MeshPacket {
        val effectiveSenderName = senderName ?: _displayName.value.ifBlank { null }
        val payload = MeshPacket.buildHazardPayload(
            category = category,
            waterDepthCm = waterDepthCm,
            passability = passability,
            message = message,
            lat = lat ?: 0.0,
            lng = lon ?: 0.0,
            accuracy = accuracy,
            senderName = effectiveSenderName
        )
        val packet = MeshPacket(
            senderId = localNodeId,
            recipientId = MeshPacket.BROADCAST_ADDRESS,
            ttl = 10,
            type = PacketType.HAZARD_BEACON,
            payload = payload,
        )
        // Transmit to mesh peers
        routingEngine.sendOutboundPacket(packet, preferredTransport)

        // Store locally in _hazardAlerts
        val alert = packet.toHazardAlert() ?: HazardAlert(
            packetId = packet.packetId,
            senderId = packet.senderId,
            category = category,
            waterDepthCm = waterDepthCm,
            passability = passability,
            message = message,
            lat = lat ?: 0.0,
            lng = lon ?: 0.0,
            accuracy = accuracy,
            senderName = effectiveSenderName,
            timestamp = packet.timestamp
        )

        synchronized(stateLock) {
            val current = _hazardAlerts.value.toMutableList()
            if (current.none { it.packetId == alert.packetId }) {
                current.add(0, alert)
                val now = System.currentTimeMillis()
                _hazardAlerts.value = current.filter { now - it.timestamp <= 10_800_000L }
            }
        }

        return packet
    }

    /**
     * Removes hazard alerts older than 3 hours (10,800,000 ms).
     */
    fun pruneExpiredHazards() {
        val now = System.currentTimeMillis()
        synchronized(stateLock) {
            val current = _hazardAlerts.value
            val valid = current.filter { now - it.timestamp <= 10_800_000L }
            if (valid.size != current.size) {
                _hazardAlerts.value = valid
            }
        }
    }

    var lastUserLocation: Pair<Double, Double>? = null
        private set

    /**
     * Updates local device location and re-evaluates proximity geofences.
     */
    fun updateUserLocation(lat: Double, lng: Double) {
        lastUserLocation = Pair(lat, lng)
        computeProximityWarning(lat, lng)
    }

    /**
     * Computes proximity to active hazards within 500m geofence and updates [_proximityWarning].
     */
    fun computeProximityWarning(userLat: Double, userLng: Double) {
        val activeHazards = _hazardAlerts.value
        val severeHazards = activeHazards.filter {
            it.waterDepthCm >= 30 || it.passability == "IMPASSABLE" || it.category == "SUBMERGED_UNDERPASS"
        }
        val nearest = severeHazards.mapNotNull { h ->
            val results = FloatArray(1)
            Location.distanceBetween(userLat, userLng, h.lat, h.lng, results)
            val d = results[0]
            if (d <= 500f) Pair(h, d) else null
        }.minByOrNull { it.second }

        if (nearest != null) {
            _proximityWarning.value = ProximityWarning(
                alertId = nearest.first.packetId,
                category = nearest.first.category,
                distanceMeters = nearest.second,
                waterDepthCm = nearest.first.waterDepthCm,
                passability = nearest.first.passability,
                hazardLat = nearest.first.lat,
                hazardLng = nearest.first.lng
            )
        } else {
            _proximityWarning.value = null
        }
    }

    /**
     * Injects a cloud/FCM SOS alert received for an emergency contact or remote event into local state.
     * Allows Emergency Center to log and track cloud-dispatched emergencies alongside mesh alerts.
     * Tagged with isCloud=true in the JSON payload so the UI can display it in the "Relative / Family SOS" section.
     */
    fun recordExternalSosAlert(
        sosId: String,
        senderName: String,
        category: String,
        message: String,
        lat: Double,
        lng: Double,
        accuracy: Float? = null,
        timestamp: Long = System.currentTimeMillis()
    ) {
        val payload = MeshPacket.buildSosPayload(
            category = category,
            message = message,
            lat = lat,
            lng = lng,
            accuracy = accuracy,
            senderName = senderName,
            isCloud = true  // tag so SosCenterScreen puts it in the Relative/Family section
        )
        val packet = MeshPacket(
            packetId = sosId.ifBlank { UUID.randomUUID().toString() },
            senderId = senderName.ifBlank { "Cloud Emergency" },
            recipientId = localNodeId,
            ttl = 1,
            hopCount = 0,
            type = PacketType.SOS_BEACON,
            payload = payload,
            timestamp = timestamp
        )
        synchronized(stateLock) {
            val current = _sosAlerts.value.toMutableList()
            val existingIndex = current.indexOfFirst { it.packetId == packet.packetId }
            if (existingIndex != -1) {
                current[existingIndex] = packet
            } else {
                current.add(0, packet)
            }
            _sosAlerts.value = current
        }
    }

    private fun handleIncomingPacket(packet: MeshPacket) {
        val localSuffix = localNodeId.removePrefix("NODE-")
        if (packet.senderId.equals(localNodeId, ignoreCase = true) ||
            packet.senderId.removePrefix("NODE-").equals(localSuffix, ignoreCase = true)
        ) {
            return // Never process self packet
        }

        when (packet.type) {
            PacketType.PEER_DISCOVERY -> {
                if (packet.senderId != localNodeId) {
                    val peerAlias = if (packet.payload.isNotBlank()) packet.payload.trim() else "Peer ${packet.senderId.takeLast(4)}"
                    if (packet.payload.isNotBlank()) {
                        messageStore.savePeerAlias(packet.senderId, packet.payload.trim())
                    }
                    val isDirect = packet.hopCount <= 0
                    val hops = (packet.hopCount + 1).coerceAtLeast(1)
                    val peerNode = MeshNode(
                        nodeId = packet.senderId,
                        alias = peerAlias,
                        rssi = if (isDirect) -35 else (-60 - (hops * 10)).coerceAtLeast(-95),
                        transportType = if (isDirect) MeshNode.TRANSPORT_BLE else MeshNode.TRANSPORT_MULTI_HOP,
                        bleRssi = if (isDirect) -35 else null,
                        lastSeenTimestamp = System.currentTimeMillis(),
                        hopDistance = hops,
                        isDirectNeighbor = isDirect,
                        availableTransports = mutableSetOf(MeshNode.TRANSPORT_BLE)
                    )
                    peerTable.updateOrAddPeer(peerNode)
                    updateConnectedPeers()
                }
            }
            PacketType.SOS_BEACON -> {
                // Only process alerts from OTHER nodes — we already stored our own in triggerSosBeacon
                if (packet.senderId == localNodeId) return
                var shouldNotify = false
                synchronized(stateLock) {
                    val current = _sosAlerts.value.toMutableList()
                    if (current.none { it.packetId == packet.packetId }) {
                        current.add(0, packet)
                        _sosAlerts.value = current
                        shouldNotify = true
                    }
                }
                if (shouldNotify) {
                    // Show system heads-up notification only for REMOTE beacons
                    com.example.zerogrid.service.MeshForegroundService.showSosNotification(
                        context,
                        packet.senderId,
                        packet.payload
                    )

                    // Data Mule: Enqueue remote SOS into WorkManager for opportunistic cloud upload
                    val coords = packet.getSosCoordinates()
                    if (coords != null && (coords.first != 0.0 || coords.second != 0.0)) {
                        val muleRequest = SosDispatchRequest(
                            lat = coords.first,
                            lng = coords.second,
                            accuracy = packet.getSosAccuracy(),
                            category = packet.getSosCategory(),
                            message = packet.getSosMessage(),
                            transport = "BLE_MESH_MULE",
                            packetId = packet.packetId
                        )
                        SosUploadWorker.enqueue(context, muleRequest)
                    }
                }
            }
            PacketType.HAZARD_BEACON -> {
                if (packet.senderId == localNodeId) return

                // 1. Temporal filter: discard if older than 3 hours (10,800,000 ms)
                val now = System.currentTimeMillis()
                if (now - packet.timestamp > 10_800_000L) {
                    Log.d(TAG, "Dropped expired HAZARD_BEACON ${packet.packetId}")
                    return
                }

                val alert = packet.toHazardAlert()
                var shouldNotify = false
                if (alert != null) {
                    synchronized(stateLock) {
                        val current = _hazardAlerts.value.toMutableList()
                        if (current.none { it.packetId == alert.packetId }) {
                            current.add(0, alert)
                            _hazardAlerts.value = current.filter { now - it.timestamp <= 10_800_000L }
                            shouldNotify = true
                        }
                    }

                    // 2. Proximity & Severity evaluation
                    val myLoc = LocationHelper.getLastKnownLocation(context)
                    var distanceM: Float? = null
                    if (myLoc != null && alert.lat != 0.0 && alert.lng != 0.0) {
                        val results = FloatArray(1)
                        Location.distanceBetween(myLoc.lat, myLoc.lng, alert.lat, alert.lng, results)
                        distanceM = results[0]
                    }

                    // Update proximity warning if within 500m geofence and severe
                    if (distanceM != null && distanceM <= 500f &&
                        (alert.waterDepthCm >= 30 || alert.passability == "IMPASSABLE" || alert.category == "SUBMERGED_UNDERPASS")
                    ) {
                        _proximityWarning.value = ProximityWarning(
                            alertId = alert.packetId,
                            category = alert.category,
                            distanceMeters = distanceM,
                            waterDepthCm = alert.waterDepthCm,
                            passability = alert.passability
                        )
                    }

                    // 3. System heads-up notification for remote hazards
                    if (shouldNotify) {
                        val distStr = if (distanceM != null) "${distanceM.toInt()}m away" else "nearby"
                        val notifTitle = "⚠️ HAZARD ALERT: ${alert.category.replace('_', ' ')}"
                        val notifMsg = if (alert.waterDepthCm > 0) {
                            "${alert.waterDepthCm}cm water logged ($distStr)! Passability: ${alert.passability.replace('_', ' ')}"
                        } else {
                            "${alert.message.ifBlank { alert.category }} ($distStr)"
                        }
                        MeshForegroundService.showHazardNotification(context, notifTitle, notifMsg)
                    }

                    // 4. Opportunistic Data Mule Enqueue
                    if (alert.lat != 0.0 || alert.lng != 0.0) {
                        val muleRequest = SosDispatchRequest(
                            lat = alert.lat,
                            lng = alert.lng,
                            accuracy = alert.accuracy,
                            category = alert.category,
                            message = alert.message,
                            transport = "BLE_MESH_MULE",
                            waterDepthCm = alert.waterDepthCm,
                            passability = alert.passability,
                            packetId = alert.packetId
                        )
                        SosUploadWorker.enqueue(context, muleRequest)
                    }
                }
            }
            PacketType.DIRECT_MESSAGE -> {
                // Update in-memory receivedMessages flow
                synchronized(stateLock) {
                    val current = _receivedMessages.value.toMutableList()
                    if (current.none { it.packetId == packet.packetId }) {
                        current.add(packet)
                        _receivedMessages.value = current
                    }
                }

                // If sender has an alias in peerTable, ensure it is persisted in messageStore
                val knownPeer = peerTable.getPeer(packet.senderId)
                if (knownPeer != null && !knownPeer.alias.startsWith("Peer ") && knownPeer.alias.isNotBlank()) {
                    messageStore.savePeerAlias(packet.senderId, knownPeer.alias)
                }

                // Persist to MessageStore keyed by sender (single chat thread)
                val stored = StoredMessage(
                    id = packet.packetId,
                    senderId = packet.senderId,
                    recipientId = packet.recipientId,
                    text = packet.payload,
                    timestamp = packet.timestamp,
                    hopCount = packet.hopCount,
                    isMine = false,
                    status = MessageStatus.DELIVERED
                )
                persistAndUpdateConversation(packet.senderId, stored)

                // Show notification for incoming direct chat message
                val senderDisplayName = knownPeer?.alias?.takeIf { !it.startsWith("Peer ") && it.isNotBlank() }
                    ?: messageStore.getPeerDisplayName(packet.senderId)
                MeshForegroundService.showMessageNotification(
                    context = context,
                    peerId = packet.senderId,
                    senderName = senderDisplayName,
                    payload = packet.payload
                )

                // Send ACK back to sender to confirm delivery
                val ackPacket = MeshPacket(
                    senderId = localNodeId,
                    recipientId = packet.senderId,
                    type = PacketType.ACK,
                    payload = packet.packetId
                )
                routingEngine.sendOutboundPacket(ackPacket, _activeChannelMode.value.transportName)
            }
            PacketType.ACK -> {
                val originalMsgId = packet.payload
                if (originalMsgId.isNotBlank()) {
                    updateMessageStatus(packet.senderId, originalMsgId, MessageStatus.DELIVERED)
                }
            }
            PacketType.CHANNEL_BROADCAST -> {
                synchronized(stateLock) {
                    val current = _receivedMessages.value.toMutableList()
                    if (current.none { it.packetId == packet.packetId }) {
                        current.add(packet)
                        _receivedMessages.value = current
                    }
                }
            }
            else -> {
                Log.d(TAG, "Received packet type ${packet.type}")
            }
        }
        if (packet.senderId != localNodeId) {
            _packetsRelayedCount.value = _packetsRelayedCount.value + 1
        }
    }

    /** Persist a message to disk and update the in-memory _conversations StateFlow safely. */
    private fun persistAndUpdateConversation(peerId: String, msg: StoredMessage) = synchronized(stateLock) {
        messageStore.appendMessage(peerId, msg)
        val updated = _conversations.value.toMutableMap()
        val existing = updated[peerId]?.toMutableList() ?: mutableListOf()
        if (existing.none { it.id == msg.id }) {
            existing.add(msg)
        }
        updated[peerId] = existing
        _conversations.value = updated
    }
}
