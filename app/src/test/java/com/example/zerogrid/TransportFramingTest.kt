package com.example.zerogrid

import org.junit.Assert.*
import org.junit.Test
import java.nio.ByteBuffer
import java.util.UUID

class TransportFramingTest {

    // --- TCP Framing Tests ---

    @Test
    fun `test TCP framing and deframing`() {
        val payload = "Hello ZeroGrid Mesh".toByteArray()
        val buffer = ByteBuffer.allocate(4 + payload.size)
        buffer.putInt(payload.size)
        buffer.put(payload)
        
        val bytes = buffer.array()
        
        // Simulate reading
        val readBuffer = ByteBuffer.wrap(bytes)
        val length = readBuffer.int
        assertEquals(payload.size, length)
        
        val readPayload = ByteArray(length)
        readBuffer.get(readPayload)
        assertArrayEquals(payload, readPayload)
    }

    @Test
    fun `test TCP multiple frames in one stream`() {
        val p1 = "First".toByteArray()
        val p2 = "Second".toByteArray()
        
        val totalSize = (4 + p1.size) + (4 + p2.size)
        val buffer = ByteBuffer.allocate(totalSize)
        buffer.putInt(p1.size)
        buffer.put(p1)
        buffer.putInt(p2.size)
        buffer.put(p2)
        
        val bytes = buffer.array()
        val readBuffer = ByteBuffer.wrap(bytes)
        
        // Read first
        val l1 = readBuffer.int
        val b1 = ByteArray(l1)
        readBuffer.get(b1)
        assertArrayEquals(p1, b1)
        
        // Read second
        val l2 = readBuffer.int
        val b2 = ByteArray(l2)
        readBuffer.get(b2)
        assertArrayEquals(p2, b2)
    }

    // --- BLE Framing Tests ---

    data class BleFrame(
        val transmissionId: UUID,
        val sequence: Int,
        val flags: Byte,
        val payload: ByteArray
    ) {
        companion object {
            const val HEADER_SIZE = 16 + 4 + 1
            
            fun fromBytes(bytes: ByteArray): BleFrame {
                val buffer = ByteBuffer.wrap(bytes)
                val mostSig = buffer.long
                val leastSig = buffer.long
                val seq = buffer.int
                val flag = buffer.get()
                val payload = ByteArray(buffer.remaining())
                buffer.get(payload)
                return BleFrame(UUID(mostSig, leastSig), seq, flag, payload)
            }
        }
        
        fun toBytes(): ByteArray {
            val buffer = ByteBuffer.allocate(HEADER_SIZE + payload.size)
            buffer.putLong(transmissionId.mostSignificantBits)
            buffer.putLong(transmissionId.leastSignificantBits)
            buffer.putInt(sequence)
            buffer.put(flags)
            buffer.put(payload)
            return buffer.array()
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is BleFrame) return false
            if (transmissionId != other.transmissionId) return false
            if (sequence != other.sequence) return false
            if (flags != other.flags) return false
            return payload.contentEquals(other.payload)
        }

        override fun hashCode(): Int {
            var result = transmissionId.hashCode()
            result = (31 * result) + sequence
            result = (31 * result) + flags.toInt()
            result = (31 * result) + payload.contentHashCode()
            return result
        }
    }

    @Test
    fun `test BLE frame serialization`() {
        val id = UUID.randomUUID()
        val seq = 42
        val flag: Byte = 1
        val payload = "ChunkData".toByteArray()
        
        val frame = BleFrame(id, seq, flag, payload)
        val bytes = frame.toBytes()
        
        assertEquals(BleFrame.HEADER_SIZE + payload.size, bytes.size)
        
        val decoded = BleFrame.fromBytes(bytes)
        assertEquals(id, decoded.transmissionId)
        assertEquals(seq, decoded.sequence)
        assertEquals(flag, decoded.flags)
        assertArrayEquals(payload, decoded.payload)
    }

    @Test
    fun `test BLE fragmentation and reassembly across different MTUs`() {
        val originalData = "This is a long message that needs to be fragmented into multiple BLE frames.".toByteArray()
        
        // Test with various MTU sizes: minimum required (25), typical (100), and large (512)
        val mtus = listOf(25, 100, 512)
        
        for (mtu in mtus) {
            val transmissionId = UUID.randomUUID()
            val maxPayloadPerFrame = mtu - 3 - BleFrame.HEADER_SIZE 
            
            // Validate that we calculated a positive payload size for the given MTU
            assertTrue("MTU $mtu is too small to carry framing header (Header: ${BleFrame.HEADER_SIZE})", 
                maxPayloadPerFrame > 0)
            
            val frames = mutableListOf<ByteArray>()
            var offset = 0
            var sequence = 0
            
            while (offset < originalData.size) {
                val chunkSize = minOf(maxPayloadPerFrame, originalData.size - offset)
                val chunk = originalData.copyOfRange(offset, offset + chunkSize)
                
                val flags: Byte = when {
                    (offset == 0 && offset + chunkSize == originalData.size) -> 0x00 // Single
                    (offset == 0) -> 0x01 // Start
                    (offset + chunkSize == originalData.size) -> 0x03 // End
                    else -> 0x02 // Middle
                }
                
                frames.add(BleFrame(transmissionId, sequence++, flags, chunk).toBytes())
                offset += chunkSize
            }
            
            // Reassemble
            val reassembly = mutableMapOf<Int, ByteArray>()
            frames.forEach { bytes ->
                val frame = BleFrame.fromBytes(bytes)
                if (frame.transmissionId == transmissionId) {
                    reassembly[frame.sequence] = frame.payload
                }
            }
            
            val resultBuffer = ByteBuffer.allocate(originalData.size)
            reassembly.keys.sorted().forEach { seq ->
                reassembly[seq]?.let { resultBuffer.put(it) }
            }
            
            assertArrayEquals("Failed for MTU $mtu", originalData, resultBuffer.array())
        }
    }
}
