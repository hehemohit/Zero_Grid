package com.example.zerogrid.mesh.engine

import org.json.JSONObject
import java.util.UUID

/**
 * Data packet structure for ZeroGrid off-grid mesh transport and multi-hop routing.
 */
data class MeshPacket(
    val packetId: String = UUID.randomUUID().toString(),
    val senderId: String,
    val recipientId: String = BROADCAST_ADDRESS,
    var ttl: Int = DEFAULT_TTL,
    var hopCount: Int = 0,
    val type: PacketType,
    val payload: String,
    val timestamp: Long = System.currentTimeMillis(),
    val signature: String = ""
) {
    fun toJson(): String {
        val safePayload = payload.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
        return """{"packetId":"$packetId","senderId":"$senderId","recipientId":"$recipientId","ttl":$ttl,"hopCount":$hopCount,"type":"${type.name}","payload":"$safePayload","timestamp":$timestamp,"signature":"$signature"}"""
    }

    fun toByteArray(): ByteArray = toJson().toByteArray(Charsets.UTF_8)

    /**
     * Parses GPS coordinates from an SOS_BEACON or HAZARD_BEACON payload.
     *
     * Supports two formats:
     * 1. Structured JSON: {"category":"...","message":"...","lat":12.97,"lng":77.59,"accuracy":15.0}
     * 2. Legacy string:   "Category: MEDICAL | Msg: ... | Lat: 12.97, Lon: 77.59"
     *
     * Returns null if coordinates are absent, zero, or unparseable.
     */
    fun getSosCoordinates(): Pair<Double, Double>? {
        if (type != PacketType.SOS_BEACON && type != PacketType.HAZARD_BEACON) return null
        return try {
            val json = JSONObject(payload)
            val lat = json.optDouble("lat", 0.0)
            val lng = json.optDouble("lng", 0.0)
            if (lat != 0.0 || lng != 0.0) Pair(lat, lng) else null
        } catch (_: Exception) {
            // Fallback: parse legacy pipe-delimited string
            val lat = Regex("Lat:\\s*([\\-0-9.]+)").find(payload)?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
            val lng = Regex("Lon:\\s*([\\-0-9.]+)").find(payload)?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
            if (lat != 0.0 || lng != 0.0) Pair(lat, lng) else null
        }
    }

    /** Parses GPS accuracy (meters) from a structured JSON SOS_BEACON or HAZARD_BEACON payload. */
    fun getSosAccuracy(): Float? {
        if (type != PacketType.SOS_BEACON && type != PacketType.HAZARD_BEACON) return null
        return try {
            val json = JSONObject(payload)
            if (json.has("accuracy")) json.getDouble("accuracy").toFloat() else null
        } catch (_: Exception) { null }
    }

    /** Parses water depth (in cm) from structured HAZARD_BEACON payload. Defaults to 0. */
    fun getHazardDepth(): Int {
        return try {
            JSONObject(payload).optInt("waterDepthCm", 0)
        } catch (_: Exception) { 0 }
    }

    /** Parses passability condition from structured HAZARD_BEACON payload. Defaults to ALL_PASSABLE. */
    fun getHazardPassability(): String {
        return try {
            JSONObject(payload).optString("passability", "ALL_PASSABLE")
        } catch (_: Exception) { "ALL_PASSABLE" }
    }

    /** Alias for coordinates on hazard beacons */
    fun getHazardCoordinates(): Pair<Double, Double>? = getSosCoordinates()

    /** Converts this packet to a HazardAlert if it represents a hazard or SOS with location */
    fun toHazardAlert(): HazardAlert? {
        if (type != PacketType.HAZARD_BEACON && type != PacketType.SOS_BEACON) return null
        val coords = getSosCoordinates() ?: return null
        return HazardAlert(
            packetId = packetId,
            senderId = senderId,
            category = getSosCategory(),
            waterDepthCm = getHazardDepth(),
            passability = getHazardPassability(),
            message = getSosMessage(),
            lat = coords.first,
            lng = coords.second,
            accuracy = getSosAccuracy(),
            senderName = getSosSenderName(),
            timestamp = timestamp
        )
    }

    /** Parses the SOS category from structured or legacy payload. */
    fun getSosCategory(): String {
        return try {
            JSONObject(payload).optString("category", "OTHER")
        } catch (_: Exception) {
            Regex("Category:\\s*([^|]+)").find(payload)?.groupValues?.get(1)?.trim() ?: "OTHER"
        }
    }

    /** Parses the SOS message text from structured or legacy payload. */
    fun getSosMessage(): String {
        return try {
            JSONObject(payload).optString("message", "")
        } catch (_: Exception) {
            Regex("Msg:\\s*([^|]+)").find(payload)?.groupValues?.get(1)?.trim() ?: payload
        }
    }

    /** Parses the SOS sender display name if present in structured payload. */
    fun getSosSenderName(): String? {
        return try {
            val name = JSONObject(payload).optString("senderName", "")
            if (name.isNotBlank()) name else null
        } catch (_: Exception) {
            Regex("Sender:\\s*([^|]+)").find(payload)?.groupValues?.get(1)?.trim()
        }
    }

    /**
     * Returns true if this SOS packet was injected from the cloud/FCM (relative/family alert).
     * Returns false if it was received over the local BLE/Wi-Fi mesh.
     */
    fun isCloudSos(): Boolean {
        return try {
            JSONObject(payload).optBoolean("isCloud", false)
        } catch (_: Exception) {
            false
        }
    }

    companion object {
        const val BROADCAST_ADDRESS = "*"
        const val DEFAULT_TTL = 5

        /**
         * Builds a structured JSON SOS beacon payload string.
         * Receivers use [getSosCoordinates] / [getSosCategory] to extract fields.
         */
        fun buildSosPayload(
            category: String,
            message: String,
            lat: Double,
            lng: Double,
            accuracy: Float? = null,
            senderName: String? = null,
            isCloud: Boolean = false
        ): String {
            val json = JSONObject()
            json.put("category", category)
            json.put("message", message)
            json.put("lat", lat)
            json.put("lng", lng)
            if (accuracy != null) json.put("accuracy", accuracy.toDouble())
            if (!senderName.isNullOrBlank()) json.put("senderName", senderName)
            if (isCloud) json.put("isCloud", true)
            json.put("ts", System.currentTimeMillis())
            return json.toString()
        }

        fun fromJson(jsonStr: String): MeshPacket? {
            return try {
                val json = JSONObject(jsonStr)
                val sender = json.optString("senderId", "")
                if (sender.isEmpty()) return parseJsonFallback(jsonStr)

                MeshPacket(
                    packetId = json.optString("packetId", UUID.randomUUID().toString()),
                    senderId = sender,
                    recipientId = json.optString("recipientId", BROADCAST_ADDRESS),
                    ttl = json.optInt("ttl", DEFAULT_TTL),
                    hopCount = json.optInt("hopCount", 0),
                    type = PacketType.valueOf(json.optString("type", PacketType.DIRECT_MESSAGE.name)),
                    payload = json.optString("payload", ""),
                    timestamp = json.optLong("timestamp", System.currentTimeMillis()),
                    signature = json.optString("signature", "")
                )
            } catch (e: Exception) {
                parseJsonFallback(jsonStr)
            }
        }

        /**
         * Builds a structured JSON HAZARD beacon payload string.
         */
        fun buildHazardPayload(
            category: String,
            waterDepthCm: Int = 0,
            passability: String = "ALL_PASSABLE",
            message: String,
            lat: Double,
            lng: Double,
            accuracy: Float? = null,
            senderName: String? = null
        ): String {
            val json = JSONObject()
            json.put("category", category)
            json.put("waterDepthCm", waterDepthCm)
            json.put("passability", passability)
            json.put("message", message)
            json.put("lat", lat)
            json.put("lng", lng)
            if (accuracy != null) json.put("accuracy", accuracy.toDouble())
            if (!senderName.isNullOrBlank()) json.put("senderName", senderName)
            json.put("ts", System.currentTimeMillis())
            return json.toString()
        }

        private fun parseJsonFallback(jsonStr: String): MeshPacket? {
            return try {
                fun extractString(key: String): String {
                    val regex = "\"$key\"\\s*:\\s*\"([^\"]*)\"".toRegex()
                    return regex.find(jsonStr)?.groupValues?.get(1) ?: ""
                }
                fun extractInt(key: String, default: Int): Int {
                    val regex = "\"$key\"\\s*:\\s*(\\d+)".toRegex()
                    return regex.find(jsonStr)?.groupValues?.get(1)?.toIntOrNull() ?: default
                }
                fun extractLong(key: String, default: Long): Long {
                    val regex = "\"$key\"\\s*:\\s*(\\d+)".toRegex()
                    return regex.find(jsonStr)?.groupValues?.get(1)?.toLongOrNull() ?: default
                }

                val sender = extractString("senderId")
                if (sender.isEmpty()) return null

                MeshPacket(
                    packetId = extractString("packetId").ifEmpty { UUID.randomUUID().toString() },
                    senderId = sender,
                    recipientId = extractString("recipientId").ifEmpty { BROADCAST_ADDRESS },
                    ttl = extractInt("ttl", DEFAULT_TTL),
                    hopCount = extractInt("hopCount", 0),
                    type = PacketType.valueOf(extractString("type").ifEmpty { PacketType.DIRECT_MESSAGE.name }),
                    payload = extractString("payload"),
                    timestamp = extractLong("timestamp", System.currentTimeMillis()),
                    signature = extractString("signature")
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}

/**
 * Parsed environmental hazard alert stored in temporal cache.
 */
data class HazardAlert(
    val packetId: String,
    val senderId: String,
    val category: String,
    val waterDepthCm: Int = 0,
    val passability: String = "ALL_PASSABLE",
    val message: String = "",
    val lat: Double,
    val lng: Double,
    val accuracy: Float? = null,
    val senderName: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Calculated proximity warning relative to current device location.
 */
data class ProximityWarning(
    val alertId: String,
    val category: String,
    val distanceMeters: Float,
    val waterDepthCm: Int,
    val passability: String,
    val hazardLat: Double = 0.0,
    val hazardLng: Double = 0.0
)

