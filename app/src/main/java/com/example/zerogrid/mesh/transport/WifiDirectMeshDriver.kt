package com.example.zerogrid.mesh.transport

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.NetworkInfo
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.zerogrid.mesh.engine.MeshNode
import com.example.zerogrid.mesh.engine.MeshPacket
import com.example.zerogrid.mesh.engine.PacketType
import com.zerogrid.mesh.app.service.MeshPeerResolver
import com.zerogrid.mesh.app.service.NetworkInterfaceType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.json.JSONObject
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.*
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

/**
 * High-performance Wi-Fi Mesh Driver for ZeroGrid.
 * Supports both:
 * 1. Local Wi-Fi (LAN / WLAN / Mobile Hotspot) via UDP Broadcast Discovery & TCP streaming.
 * 2. Wi-Fi Direct (P2P) via Android WifiP2pManager DNS-SD & TCP sockets.
 */
class WifiDirectMeshDriver(
    private val context: Context,
    private val localNodeId: String,
    @Volatile var localDisplayName: String = "User",
) : MeshTransport {

    init {
        Log.d(TAG, "Initializing WifiDirectMeshDriver for $localNodeId ($localDisplayName)")
    }

    companion object {
        private const val TAG = "WifiDirectMeshDriver"
        private const val TCP_PORT = 8888
        private const val UDP_PORT = 8889
        private const val SERVICE_TYPE = "_presence._tcp"
        private const val SERVICE_NAME = "_zerogrid"
    }

    override val transportName: String = MeshNode.TRANSPORT_WIFI_DIRECT
    override var isRunning: Boolean = false
        private set

    private val _packetFlow = MutableSharedFlow<MeshPacket>(extraBufferCapacity = 128)
    override val packetFlow: SharedFlow<MeshPacket> = _packetFlow.asSharedFlow()

    private val _peerDiscoveryFlow = MutableSharedFlow<MeshNode>(extraBufferCapacity = 128)
    override val peerDiscoveryFlow: SharedFlow<MeshNode> = _peerDiscoveryFlow.asSharedFlow()

    private val wifiP2pManager: WifiP2pManager? by lazy {
        try { context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager } catch (_: Exception) { null }
    }
    private val wifiManager: WifiManager? by lazy {
        try { context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager } catch (_: Exception) { null }
    }

    private var channel: WifiP2pManager.Channel? = null
    private var serverSocket: ServerSocket? = null
    private var udpSocket: DatagramSocket? = null
    private var multicastLock: WifiManager.MulticastLock? = null

    private var groupOwnerAddress: String? = null
    private var isGroupOwner: Boolean = false

    private val peerSockets = ConcurrentHashMap<String, Socket>()
    private val nodeToIpMap = ConcurrentHashMap<String, String>()
    private val ipToNodeMap = ConcurrentHashMap<String, String>()
    private var serviceRequest: WifiP2pDnsSdServiceRequest? = null

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var udpBeaconJob: Job? = null
    private var udpReceiverJob: Job? = null

    private val p2pReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                    wifiP2pManager?.requestPeers(channel) { peers ->
                        peers.deviceList.forEach { handlePeerDeviceDiscovered(it) }
                    }
                }
                WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                    @Suppress("DEPRECATION")
                    val isConnected = intent.getParcelableExtra<NetworkInfo>(WifiP2pManager.EXTRA_NETWORK_INFO)?.isConnected == true
                    
                    if (isConnected) {
                        wifiP2pManager?.requestConnectionInfo(channel) { info ->
                            handleConnectionInfo(info)
                        }
                    } else {
                        groupOwnerAddress = null
                        isGroupOwner = false
                        clearPeerSockets()
                    }
                }
            }
        }
    }

    fun updateDisplayName(name: String) {
        if (name.isBlank() || name == localDisplayName) return
        localDisplayName = name
        if (isRunning) {
            registerLocalDnsSdService()
            sendUdpBeacon()
        }
    }

    /** Returns true if this peer has an active known IP or TCP socket over Wi-Fi */
    fun isPeerReachable(peerId: String): Boolean {
        if (!isRunning) return false
        return nodeToIpMap.containsKey(peerId) || peerSockets.containsKey(peerId) || (groupOwnerAddress != null)
    }

    private fun handleConnectionInfo(info: WifiP2pInfo) {
        groupOwnerAddress = info.groupOwnerAddress?.hostAddress
        isGroupOwner = info.isGroupOwner
        Log.d(TAG, "Wi-Fi Direct connection info: GO=$isGroupOwner, GO_ADDR=$groupOwnerAddress")
    }

    private fun clearPeerSockets() {
        peerSockets.values.forEach { try { it.close() } catch (_: Exception) {} }
        peerSockets.clear()
    }

    @SuppressLint("MissingPermission")
    override fun startDiscovery() {
        if (isRunning) {
            Log.d(TAG, "Wi-Fi discovery already running")
            return
        }

        isRunning = true

        // 1. Acquire Wi-Fi MulticastLock for LAN UDP broadcast reception
        try {
            multicastLock = wifiManager?.createMulticastLock("ZeroGridMulticastLock")?.apply {
                setReferenceCounted(true)
                acquire()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not acquire MulticastLock: ${e.message}")
        }

        // 2. Start TCP socket listener (port 8888)
        startServerSocketListener()

        // 3. Start Local LAN UDP broadcast discovery & receiver (port 8889)
        startLanDiscovery()

        // 4. Start Wi-Fi Direct P2P discovery if available
        try {
            if (hasRequiredPermissions() && wifiP2pManager != null) {
                channel = wifiP2pManager?.initialize(context, context.mainLooper, null)

                val filter = IntentFilter().apply {
                    addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
                    addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
                    addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
                    addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
                }
                context.registerReceiver(p2pReceiver, filter)

                registerLocalDnsSdService()
                setupDnsSdServiceDiscovery()

                wifiP2pManager?.discoverPeers(channel, object : WifiP2pManager.ActionListener {
                    override fun onSuccess() { Log.d(TAG, "Wi-Fi Direct peer discovery initiated") }
                    override fun onFailure(reasonCode: Int) { Log.e(TAG, "Wi-Fi Direct peer discovery failed code: $reasonCode") }
                })
            }
        } catch (e: Exception) {
            Log.w(TAG, "Wi-Fi Direct P2P initialization skipped/failed: ${e.message}")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Local LAN (Wi-Fi / Hotspot) UDP Discovery Engine
    // ─────────────────────────────────────────────────────────────────────────

    private fun startLanDiscovery() {
        // Start UDP Receiver
        udpReceiverJob?.cancel()
        udpReceiverJob = scope.launch {
            try {
                udpSocket = DatagramSocket(null).apply {
                    reuseAddress = true
                    broadcast = true
                    bind(InetSocketAddress(UDP_PORT))
                }
                Log.d(TAG, "UDP broadcast listener active on port $UDP_PORT")

                val buffer = ByteArray(4096)
                while (isRunning && udpSocket?.isClosed == false) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    try {
                        udpSocket?.receive(packet)
                    } catch (_: SocketException) {
                        break
                    }
                    val msg = String(packet.data, 0, packet.length, Charsets.UTF_8)
                    val senderIp = packet.address?.hostAddress ?: continue
                    handleIncomingUdpMessage(msg, senderIp)
                }
            } catch (e: Exception) {
                Log.e(TAG, "UDP Receiver error", e)
            }
        }

        // Start Periodic UDP Beacon (every 4 seconds)
        udpBeaconJob?.cancel()
        udpBeaconJob = scope.launch {
            while (isRunning) {
                try {
                    sendUdpBeacon()
                } catch (e: Exception) {
                    Log.w(TAG, "Error sending UDP beacon: ${e.message}")
                }
                delay(4000L)
            }
        }
    }

    private fun sendUdpBeacon() {
        val payload = JSONObject().apply {
            put("type", "BEACON")
            put("nodeId", localNodeId)
            put("alias", localDisplayName)
            put("tcpPort", TCP_PORT)
        }.toString().toByteArray(Charsets.UTF_8)

        val broadcastTargets = getBroadcastAddresses()
        broadcastTargets.forEach { addr ->
            try {
                val datagram = DatagramPacket(payload, payload.size, addr, UDP_PORT)
                udpSocket?.send(datagram)
            } catch (_: Exception) {}
        }
    }

    private fun handleIncomingUdpMessage(raw: String, senderIp: String) {
        try {
            val json = JSONObject(raw)
            val type = json.optString("type")
            val remoteNodeId = json.optString("nodeId")
            val remoteAlias = json.optString("alias")

            val localSuffix = localNodeId.removePrefix("NODE-")
            val isSelf = remoteNodeId.equals(localNodeId, ignoreCase = true) ||
                remoteNodeId.removePrefix("NODE-").equals(localSuffix, ignoreCase = true) ||
                remoteAlias.equals(localDisplayName, ignoreCase = true) ||
                remoteAlias.equals(android.os.Build.MODEL, ignoreCase = true)

            if (remoteNodeId.isNotBlank() && !isSelf) {
                nodeToIpMap[remoteNodeId] = senderIp
                ipToNodeMap[senderIp] = remoteNodeId

                val alias = if (remoteAlias.isNotBlank()) remoteAlias else "Wi-Fi ${remoteNodeId.takeLast(4)}"
                handleResolvedPeer(
                    nodeId = remoteNodeId,
                    alias = alias,
                    macAddress = senderIp,
                    rssi = -35 // Local Wi-Fi / Hotspot link quality
                )

                // If this is a BEACON, send an instant unicast reply so sender gets our IP immediately
                if (type == "BEACON") {
                    scope.launch {
                        try {
                            val reply = JSONObject().apply {
                                put("type", "REPLY")
                                put("nodeId", localNodeId)
                                put("alias", localDisplayName)
                                put("tcpPort", TCP_PORT)
                            }.toString().toByteArray(Charsets.UTF_8)
                            val targetAddr = InetAddress.getByName(senderIp)
                            val datagram = DatagramPacket(reply, reply.size, targetAddr, UDP_PORT)
                            udpSocket?.send(datagram)
                        } catch (_: Exception) {}
                    }
                }
            }

            // Handle encapsulated broadcast DATA packet
            if (type == "DATA") {
                val packetJson = json.optString("packet")
                if (packetJson.isNotBlank()) {
                    val meshPacket = MeshPacket.fromJson(packetJson)
                    if (meshPacket != null) {
                        val senderSuffix = meshPacket.senderId.removePrefix("NODE-")
                        if (!meshPacket.senderId.equals(localNodeId, ignoreCase = true) &&
                            !senderSuffix.equals(localSuffix, ignoreCase = true)) {
                            nodeToIpMap[meshPacket.senderId] = senderIp
                            scope.launch { _packetFlow.emit(meshPacket) }
                        }
                    }
                }
            }
        } catch (_: Exception) {}
    }

    private fun getBroadcastAddresses(): List<InetAddress> {
        val list = mutableListOf<InetAddress>()
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return listOf(InetAddress.getByName("255.255.255.255"))
            for (intf in Collections.list(interfaces)) {
                if (intf.isLoopback || !intf.isUp) continue
                for (addr in intf.interfaceAddresses) {
                    val bcast = addr.broadcast
                    if (bcast != null) list.add(bcast)
                }
            }
        } catch (_: Exception) {}
        if (list.isEmpty()) {
            try { list.add(InetAddress.getByName("255.255.255.255")) } catch (_: Exception) {}
        }
        return list
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Wi-Fi Direct (P2P) DNS-SD Setup
    // ─────────────────────────────────────────────────────────────────────────

    @SuppressLint("MissingPermission")
    private fun registerLocalDnsSdService() {
        val mgr = wifiP2pManager ?: return
        val ch = channel ?: return

        try {
            val record = HashMap<String, String>()
            record["nodeId"] = localNodeId
            record["alias"] = localDisplayName

            val serviceInfo = WifiP2pDnsSdServiceInfo.newInstance(
                SERVICE_NAME,
                SERVICE_TYPE,
                record
            )

            mgr.clearLocalServices(ch, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    mgr.addLocalService(ch, serviceInfo, object : WifiP2pManager.ActionListener {
                        override fun onSuccess() {
                            Log.d(TAG, "Wi-Fi Direct DNS-SD service registered (nodeId=$localNodeId, alias=$localDisplayName)")
                        }
                        override fun onFailure(code: Int) {
                            Log.w(TAG, "Failed to add DNS-SD local service: $code")
                        }
                    })
                }
                override fun onFailure(code: Int) {
                    mgr.addLocalService(ch, serviceInfo, null)
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error registering DNS-SD service", e)
        }
    }

    @SuppressLint("MissingPermission")
    private fun setupDnsSdServiceDiscovery() {
        val mgr = wifiP2pManager ?: return
        val ch = channel ?: return

        try {
            mgr.setDnsSdResponseListeners(
                ch,
                { instanceName, registrationType, srcDevice ->
                    Log.d(TAG, "DNS-SD instance: $instanceName from ${srcDevice.deviceName}")
                },
                { _, recordMap, srcDevice ->
                    Log.d(TAG, "DNS-SD TXT record received from ${srcDevice.deviceName}: $recordMap")
                    val remoteNodeId = recordMap["nodeId"]
                    val remoteAlias = recordMap["alias"]
                    if (!remoteNodeId.isNullOrBlank() && remoteNodeId != localNodeId) {
                        nodeToIpMap[remoteNodeId] = srcDevice.deviceAddress
                        val alias = remoteAlias?.takeIf { it.isNotBlank() } ?: srcDevice.deviceName
                        handleResolvedPeer(
                            nodeId = remoteNodeId,
                            alias = alias,
                            macAddress = srcDevice.deviceAddress,
                            rssi = -50
                        )
                    }
                }
            )

            val req = WifiP2pDnsSdServiceRequest.newInstance()
            serviceRequest = req
            mgr.clearServiceRequests(ch, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    mgr.addServiceRequest(ch, req, object : WifiP2pManager.ActionListener {
                        override fun onSuccess() {
                            mgr.discoverServices(ch, object : WifiP2pManager.ActionListener {
                                override fun onSuccess() {
                                    Log.d(TAG, "Wi-Fi Direct DNS-SD service discovery initiated")
                                }
                                override fun onFailure(reason: Int) {
                                    Log.w(TAG, "discoverServices failed: $reason")
                                }
                            })
                        }
                        override fun onFailure(reason: Int) {
                            Log.w(TAG, "addServiceRequest failed: $reason")
                        }
                    })
                }
                override fun onFailure(reason: Int) {
                    mgr.addServiceRequest(ch, req, null)
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up DNS-SD service discovery", e)
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val nearbyDevices = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.NEARBY_WIFI_DEVICES) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        return fineLocation && nearbyDevices
    }

    @SuppressLint("MissingPermission")
    override fun stopDiscovery() {
        if (!isRunning) return
        isRunning = false

        udpBeaconJob?.cancel()
        udpReceiverJob?.cancel()
        try { udpSocket?.close() } catch (_: Exception) {}
        udpSocket = null

        try {
            if (multicastLock?.isHeld == true) {
                multicastLock?.release()
            }
        } catch (_: Exception) {}

        try {
            context.unregisterReceiver(p2pReceiver)
            wifiP2pManager?.stopPeerDiscovery(channel, null)
            serviceRequest?.let { wifiP2pManager?.removeServiceRequest(channel, it, null) }
            serviceRequest = null
            wifiP2pManager?.clearServiceRequests(channel, null)
            wifiP2pManager?.clearLocalServices(channel, null)
            serverSocket?.close()
            serverSocket = null
            clearPeerSockets()
            nodeToIpMap.clear()
            ipToNodeMap.clear()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping Wi-Fi Direct discovery", e)
        }
    }

    private fun resolveIpForNodeId(target: String): String? {
        return nodeToIpMap[target]
            ?: nodeToIpMap[target.removePrefix("NODE-")]
            ?: nodeToIpMap["NODE-$target"]
            ?: nodeToIpMap.entries.firstOrNull { it.key.equals(target, ignoreCase = true) }?.value
    }

    override fun sendPacket(packet: MeshPacket, targetPeerId: String?): Boolean {
        if (!isRunning) return false

        val target = targetPeerId ?: if (packet.recipientId != MeshPacket.BROADCAST_ADDRESS && packet.recipientId != "*") packet.recipientId else null

        scope.launch {
            val targetIp = target?.let { resolveIpForNodeId(it) }

            // ── LAYER 1: Rapid UDP Datagram Delivery (Bypasses AP/Hotspot TCP isolation) ──
            try {
                val dataJson = JSONObject().apply {
                    put("type", "DATA")
                    put("packet", packet.toJson())
                }.toString().toByteArray(Charsets.UTF_8)

                // 1A. Direct Unicast UDP to peer's known Wi-Fi IP
                if (targetIp != null) {
                    try {
                        val peerAddr = InetAddress.getByName(targetIp)
                        val datagram = DatagramPacket(dataJson, dataJson.size, peerAddr, UDP_PORT)
                        udpSocket?.send(datagram)
                        Log.d(TAG, "Sent direct UDP unicast for packet ${packet.packetId} to $targetIp:$UDP_PORT")
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed sending UDP unicast to $targetIp: ${e.message}")
                    }
                }

                // 1B. Subnet Broadcast UDP (guarantees delivery across all Wi-Fi clients)
                getBroadcastAddresses().forEach { bcastAddr ->
                    try {
                        val datagram = DatagramPacket(dataJson, dataJson.size, bcastAddr, UDP_PORT)
                        udpSocket?.send(datagram)
                    } catch (_: Exception) {}
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error packaging UDP DATA packet: ${e.message}")
            }

            // ── LAYER 2: High-Bandwidth TCP Socket Streaming ──
            if (targetIp != null) {
                transmitToAddress(targetIp, packet)
            } else if (target == null) {
                nodeToIpMap.values.toSet().forEach { ip ->
                    transmitToAddress(ip, packet)
                }
            }

            // ── LAYER 3: Wi-Fi Direct P2P Group Owner Fallback ──
            val goAddr = groupOwnerAddress
            if (goAddr != null) {
                if (isGroupOwner) {
                    peerSockets.values.forEach { transmitOverSocket(it, packet) }
                } else {
                    transmitToAddress(goAddr, packet)
                }
            }
        }
        return true
    }

    private suspend fun transmitToAddress(address: String, packet: MeshPacket) = withContext(Dispatchers.IO) {
        try {
            var socket = peerSockets[address]
            if ((socket == null) || socket.isClosed) {
                val newSocket = Socket()
                newSocket.connect(InetSocketAddress(address, TCP_PORT), 3000)
                peerSockets[address] = newSocket
                handleClientSocket(newSocket) // Listen for responses on this socket as well
                socket = newSocket

                // Send immediate presence handshake
                val handshake = MeshPacket(
                    senderId = localNodeId,
                    recipientId = MeshPacket.BROADCAST_ADDRESS,
                    type = PacketType.PEER_DISCOVERY,
                    payload = localDisplayName
                )
                transmitOverSocket(newSocket, handshake)
            }
            transmitOverSocket(socket, packet)
        } catch (e: Exception) {
            Log.d(TAG, "TCP connection to $address unavailable (UDP delivered): ${e.message}")
            peerSockets.remove(address)
        }
    }

    private suspend fun transmitOverSocket(socket: Socket, packet: MeshPacket) = withContext(Dispatchers.IO) {
        try {
            val output = DataOutputStream(socket.getOutputStream())
            val bytes = packet.toByteArray()
            output.writeInt(bytes.size)
            output.write(bytes)
            output.flush()
            Log.d(TAG, "Sent TCP packet to ${socket.inetAddress?.hostAddress} (${bytes.size} bytes)")
        } catch (e: Exception) {
            Log.e(TAG, "Error writing to socket", e)
            try { socket.close() } catch (_: Exception) {}
        }
    }

    private fun startServerSocketListener() {
        scope.launch {
            try {
                serverSocket = ServerSocket(TCP_PORT)
                Log.d(TAG, "Socket server listening on port $TCP_PORT")
                while (isRunning && (serverSocket?.isClosed == false)) {
                    val socket = serverSocket?.accept() ?: break
                    val clientAddress = socket.inetAddress?.hostAddress
                    clientAddress?.let { peerSockets[it] = socket }

                    // Send presence handshake to connecting client
                    val handshake = MeshPacket(
                        senderId = localNodeId,
                        recipientId = MeshPacket.BROADCAST_ADDRESS,
                        type = PacketType.PEER_DISCOVERY,
                        payload = localDisplayName
                    )
                    transmitOverSocket(socket, handshake)

                    handleClientSocket(socket)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Server socket exception", e)
            }
        }
    }

    private fun handleClientSocket(socket: Socket) {
        scope.launch {
            try {
                val input = DataInputStream(socket.getInputStream())
                while (isRunning && !socket.isClosed) {
                    val length = try { input.readInt() } catch (_: Exception) { -1 }
                    if (length <= 0 || length > 1024 * 1024) break // Max 1MB packet
                    
                    val payload = ByteArray(length)
                    input.readFully(payload)
                    
                    val jsonStr = String(payload, Charsets.UTF_8)
                    val packet = MeshPacket.fromJson(jsonStr)
                    if (packet != null) {
                        if (packet.senderId.isNotBlank() && packet.senderId != localNodeId) {
                            val clientHost = socket.inetAddress?.hostAddress ?: "wifi-client"
                            nodeToIpMap[packet.senderId] = clientHost
                            ipToNodeMap[clientHost] = packet.senderId

                            val alias = if (packet.type == PacketType.PEER_DISCOVERY && packet.payload.isNotBlank()) {
                                packet.payload.trim()
                            } else {
                                "Peer ${packet.senderId.takeLast(4)}"
                            }

                            handleResolvedPeer(
                                nodeId = packet.senderId,
                                alias = alias,
                                macAddress = clientHost,
                                rssi = -35
                            )
                        }
                        _packetFlow.emit(packet)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reading from client socket", e)
            } finally {
                try { socket.close() } catch (_: Exception) {}
            }
        }
    }

    fun handleResolvedPeer(nodeId: String, alias: String, macAddress: String, rssi: Int) {
        val localSuffix = localNodeId.removePrefix("NODE-")
        if (nodeId.equals(localNodeId, ignoreCase = true) ||
            nodeId.removePrefix("NODE-").equals(localSuffix, ignoreCase = true) ||
            alias.equals(localDisplayName, ignoreCase = true) ||
            alias.equals(android.os.Build.MODEL, ignoreCase = true)
        ) return

        MeshPeerResolver.getInstance().recordEndpoint(
            uniqueDeviceId = nodeId,
            interfaceType = NetworkInterfaceType.WIFI_DIRECT,
            address = macAddress,
            rssi = rssi,
            metadata = mapOf("alias" to alias)
        )

        val node = MeshNode(
            nodeId = nodeId,
            alias = alias,
            rssi = rssi,
            transportType = MeshNode.TRANSPORT_WIFI_DIRECT,
            wifiRssi = rssi,
            lastSeenTimestamp = System.currentTimeMillis(),
            hopDistance = 1,
            isDirectNeighbor = true,
            availableTransports = mutableSetOf(MeshNode.TRANSPORT_WIFI_DIRECT)
        )
        scope.launch {
            _peerDiscoveryFlow.emit(node)
        }
    }

    fun handlePeerDeviceDiscovered(device: WifiP2pDevice) {
        try {
            val deviceAddress = try { device.deviceAddress } catch (_: SecurityException) { null } ?: "00:00:00:00:00:00"
            val deviceName = try { device.deviceName } catch (_: SecurityException) { null } ?: "Wi-Fi Peer"

            // Filter out self device
            if (deviceName.equals(localDisplayName, ignoreCase = true) ||
                deviceName.equals(android.os.Build.MODEL, ignoreCase = true)) {
                return
            }

            val canonicalNodeId = ipToNodeMap[deviceAddress]
            if (canonicalNodeId != null) {
                handleResolvedPeer(
                    nodeId = canonicalNodeId,
                    alias = deviceName.ifBlank { "Wi-Fi ${canonicalNodeId.takeLast(4)}" },
                    macAddress = deviceAddress,
                    rssi = -50
                )
            } else {
                MeshPeerResolver.getInstance().recordEndpoint(
                    uniqueDeviceId = deviceAddress,
                    interfaceType = NetworkInterfaceType.WIFI_DIRECT,
                    address = deviceAddress,
                    rssi = -50,
                    metadata = mapOf("alias" to deviceName)
                )
                val node = MeshNode(
                    nodeId = deviceAddress,
                    alias = deviceName.ifBlank { "Wi-Fi ${deviceAddress.takeLast(4)}" },
                    rssi = -50,
                    transportType = MeshNode.TRANSPORT_WIFI_DIRECT,
                    wifiRssi = -50,
                    lastSeenTimestamp = System.currentTimeMillis(),
                    hopDistance = 1,
                    isDirectNeighbor = true,
                    availableTransports = mutableSetOf(MeshNode.TRANSPORT_WIFI_DIRECT)
                )
                scope.launch {
                    _peerDiscoveryFlow.emit(node)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling discovered peer device", e)
        }
    }
}
