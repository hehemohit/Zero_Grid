package com.example.zerogrid

import com.example.zerogrid.mesh.engine.DeduplicationCache
import com.example.zerogrid.mesh.engine.MeshNode
import com.example.zerogrid.mesh.engine.MeshPacket
import com.example.zerogrid.mesh.engine.MeshRoutingEngine
import com.example.zerogrid.mesh.engine.PacketType
import com.example.zerogrid.mesh.transport.MeshTransport
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MeshProtocolTest {

    class MockTransport(override val transportName: String) : MeshTransport {
        override var isRunning: Boolean = true
        private val _packetFlow = MutableSharedFlow<MeshPacket>(extraBufferCapacity = 64)
        override val packetFlow: SharedFlow<MeshPacket> = _packetFlow.asSharedFlow()
        private val _peerDiscoveryFlow = MutableSharedFlow<MeshNode>(extraBufferCapacity = 64)
        override val peerDiscoveryFlow: SharedFlow<MeshNode> = _peerDiscoveryFlow.asSharedFlow()

        val sentPackets = mutableListOf<MeshPacket>()

        override fun startDiscovery() {}
        override fun stopDiscovery() {}
        override fun sendPacket(packet: MeshPacket, targetPeerId: String?): Boolean {
            sentPackets.add(packet)
            return true
        }
    }

    @Test
    fun testPacketSerialization() {
        val packet = MeshPacket(
            senderId = "NODE-ALICE",
            recipientId = "NODE-BOB",
            ttl = 5,
            type = PacketType.DIRECT_MESSAGE,
            payload = "Hello Mesh!"
        )

        val json = packet.toJson()
        val deserialized = MeshPacket.fromJson(json)

        assertNotNull(deserialized)
        assertEquals(packet.packetId, deserialized?.packetId)
        assertEquals("NODE-ALICE", deserialized?.senderId)
        assertEquals("NODE-BOB", deserialized?.recipientId)
        assertEquals(5, deserialized?.ttl)
        assertEquals(PacketType.DIRECT_MESSAGE, deserialized?.type)
        assertEquals("Hello Mesh!", deserialized?.payload)
    }

    @Test
    fun testDeduplicationCacheLoopPrevention() {
        val cache = DeduplicationCache()
        val packetId = "PACKET-123"

        // First time processing packet -> Should record and return false (not duplicate)
        val firstResult = cache.isDuplicateAndRecord(packetId)
        assertFalse(firstResult)

        // Second time processing same packet -> Should detect duplicate and return true
        val secondResult = cache.isDuplicateAndRecord(packetId)
        assertTrue(secondResult)
    }

    @Test
    fun testRoutingEngineDuplicateDrop() {
        val routingEngine = MeshRoutingEngine(localNodeId = "NODE-CHARLIE")
        val packet = MeshPacket(
            senderId = "NODE-ALICE",
            recipientId = "NODE-CHARLIE",
            type = PacketType.DIRECT_MESSAGE,
            payload = "Test Message"
        )

        // Process first time
        routingEngine.processInboundPacket(packet)

        // Process duplicate second time -> Should be caught by deduplication cache
        routingEngine.processInboundPacket(packet)
    }

    @Test
    fun testRelayLogicAndSourceExclusion() {
        val nodeB = MeshRoutingEngine(localNodeId = "NODE-B")
        val bleTransport = MockTransport("BLE")
        val wifiTransport = MockTransport("WIFI")
        
        nodeB.registerTransport(bleTransport)
        nodeB.registerTransport(wifiTransport)
        
        val packetFromA = MeshPacket(
            senderId = "NODE-A",
            recipientId = "NODE-C",
            ttl = 5,
            type = PacketType.DIRECT_MESSAGE,
            payload = "Relay me!"
        )
        
        // Node B receives packet from A via BLE
        nodeB.processInboundPacket(packetFromA, bleTransport)
        
        // Node B should relay it to WIFI, but NOT back to BLE
        assertEquals(0, bleTransport.sentPackets.size)
        assertEquals(1, wifiTransport.sentPackets.size)
        
        val relayed = wifiTransport.sentPackets[0]
        assertEquals(4, relayed.ttl)
        assertEquals(1, relayed.hopCount)
    }

    @Test
    fun testTtlExpiration() {
        val nodeB = MeshRoutingEngine(localNodeId = "NODE-B")
        val wifiTransport = MockTransport("WIFI")
        nodeB.registerTransport(wifiTransport)
        
        val packetWithLowTtl = MeshPacket(
            senderId = "NODE-A",
            recipientId = "NODE-C",
            ttl = 1, // Will be 0 after decrement
            type = PacketType.DIRECT_MESSAGE,
            payload = "Don't relay me!"
        )
        
        nodeB.processInboundPacket(packetWithLowTtl)
        
        // Should NOT relay because TTL reached 1 (terminal for forwarding)
        assertEquals(0, wifiTransport.sentPackets.size)
    }
}
