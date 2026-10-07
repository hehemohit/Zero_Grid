package com.example.zerogrid.admin.data.model

import com.google.gson.annotations.SerializedName

/**
 * GeoJSON Point representation matching MongoDB [lng, lat].
 */
data class GeoPointDto(
    @SerializedName("type")
    val type: String = "Point",
    @SerializedName("coordinates")
    val coordinates: List<Double> = emptyList()
) {
    val latitude: Double
        get() = coordinates.getOrNull(1) ?: 0.0

    val longitude: Double
        get() = coordinates.getOrNull(0) ?: 0.0

    val hasValidCoordinates: Boolean
        get() = coordinates.size >= 2 && (latitude != 0.0 || longitude != 0.0)
}

/**
 * User profile associated with SOS event.
 */
data class SosUserDto(
    @SerializedName("_id")
    val _id: String? = null,
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("displayName")
    val displayName: String? = null,
    @SerializedName("email")
    val email: String? = null,
    @SerializedName("phoneNumber")
    val phoneNumber: String? = null,
    @SerializedName("photoUrl")
    val photoUrl: String? = null,
    @SerializedName("role")
    val role: String? = null
) {
    val resolvedId: String
        get() = id ?: _id ?: ""
}

/**
 * Case dispatch note appended to an SOS incident.
 */
data class SosNoteDto(
    @SerializedName("_id")
    val _id: String? = null,
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("authorId")
    val authorId: Any? = null, // Can be String ID or populated map/object
    @SerializedName("author")
    val author: String? = null,
    @SerializedName("text")
    val text: String = "",
    @SerializedName("timestamp")
    val timestamp: String? = null,
    @SerializedName("createdAt")
    val createdAt: String? = null
) {
    val noteId: String
        get() = id ?: _id ?: ""

    val effectiveTime: String
        get() = timestamp ?: createdAt ?: ""

    val authorName: String
        get() {
            if (!author.isNullOrBlank()) return author
            if (authorId is Map<*, *>) {
                val name = authorId["displayName"] as? String
                if (!name.isNullOrBlank()) return name
                val email = authorId["email"] as? String
                if (!email.isNullOrBlank()) return email
            }
            return "Dispatch Lead"
        }
}

/**
 * Acknowledged responder record.
 */
data class SosAckEntryDto(
    @SerializedName("userId")
    val userId: String? = null,
    @SerializedName("displayName")
    val displayName: String? = null,
    @SerializedName("confirmedSafe")
    val confirmedSafe: Boolean = false,
    @SerializedName("acknowledgedAt")
    val acknowledgedAt: String? = null
)

/**
 * Core SOS Event DTO populated from /api/admin/sos, /api/admin/sos/history, or /api/sos/:id.
 */
data class AdminSosEventDto(
    @SerializedName("_id")
    val _id: String? = null,
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("triggeredBy")
    val triggeredBy: SosUserDto? = null,
    @SerializedName("location")
    val location: GeoPointDto? = null,
    @SerializedName("accuracyMeters")
    val accuracyMeters: Double? = null,
    @SerializedName("category")
    val category: String = "OTHER",
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("transport")
    val transport: String? = null,
    @SerializedName("batteryPercentage")
    val batteryPercentage: Int? = null,
    @SerializedName("status")
    val status: String = "ACTIVE",
    @SerializedName("acknowledgedBy")
    val acknowledgedBy: Any? = null,
    @SerializedName("acknowledgedByUsers")
    val acknowledgedByUsers: List<SosAckEntryDto> = emptyList(),
    @SerializedName("resolvedBy")
    val resolvedBy: Any? = null,
    @SerializedName("notes")
    val notes: List<SosNoteDto> = emptyList(),
    @SerializedName("waterDepthCm")
    val waterDepthCm: Int? = null,
    @SerializedName("passability")
    val passability: String? = null,
    @SerializedName("createdAt")
    val createdAt: String? = null,
    @SerializedName("updatedAt")
    val updatedAt: String? = null
) {
    val eventId: String
        get() = id ?: _id ?: ""

    val displayId: String
        get() = if (eventId.length >= 6) "sos-${eventId.takeLast(4)}" else (eventId.ifEmpty { "sos-0000" })

    val userName: String
        get() = triggeredBy?.displayName?.ifBlank { null }
            ?: triggeredBy?.email?.ifBlank { null }
            ?: "Citizen Node"

    val userPhone: String?
        get() = triggeredBy?.phoneNumber?.ifBlank { null }

    val userEmail: String?
        get() = triggeredBy?.email?.ifBlank { null }

    val isActive: Boolean
        get() = status.equals("ACTIVE", ignoreCase = true)

    val isAcknowledged: Boolean
        get() = status.equals("ACKNOWLEDGED", ignoreCase = true)

    val isResolved: Boolean
        get() = status.equals("RESOLVED", ignoreCase = true)

    val formattedBattery: String
        get() = if (batteryPercentage != null && batteryPercentage in 0..100) {
            "$batteryPercentage%"
        } else {
            "Not Found"
        }
}

data class AdminPaginationDto(
    @SerializedName("total")
    val total: Int = 0,
    @SerializedName("page")
    val page: Int = 1,
    @SerializedName("limit")
    val limit: Int = 50,
    @SerializedName("pages")
    val pages: Int = 1
)

data class AdminSosResponse(
    @SerializedName("events")
    val events: List<AdminSosEventDto> = emptyList(),
    @SerializedName("pagination")
    val pagination: AdminPaginationDto? = null
)

data class SingleSosResponse(
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("sos")
    val sos: AdminSosEventDto? = null
)

data class AcknowledgeSosRequest(
    @SerializedName("confirmedSafe")
    val confirmedSafe: Boolean = false
)

data class AddNoteRequest(
    @SerializedName("text")
    val text: String
)
