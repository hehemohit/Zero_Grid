package com.example.zerogrid

import com.example.zerogrid.mesh.transport.BleMeshDriver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.util.UUID

class BleUuidTest {

    @Test
    fun `test BLE service UUID validity`() {
        val serviceUuid = BleMeshDriver.SERVICE_UUID
        assertNotNull("Service UUID should not be null", serviceUuid)
        
        // Verify it can be parsed back from its string representation
        val parsed = UUID.fromString(serviceUuid.toString())
        assertEquals(serviceUuid, parsed)
        
        // Expected deterministic value
        assertEquals("0000a701-0000-1000-8000-00805f9b34fb", serviceUuid.toString().lowercase())
    }

    @Test
    fun `test BLE characteristic UUID validity`() {
        val characteristicUuid = BleMeshDriver.CHARACTERISTIC_UUID
        assertNotNull("Characteristic UUID should not be null", characteristicUuid)
        
        // Verify it can be parsed back from its string representation
        val parsed = UUID.fromString(characteristicUuid.toString())
        assertEquals(characteristicUuid, parsed)
        
        // Expected deterministic value
        assertEquals("0000a702-0000-1000-8000-00805f9b34fb", characteristicUuid.toString().lowercase())
    }
}
