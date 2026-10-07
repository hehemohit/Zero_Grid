package com.example.zerogrid.network

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

// ── Request Bodies ───────────────────────────────────────────────────────────

data class SosDispatchRequest(
    @SerializedName("lat")               val lat: Double,
    @SerializedName("lng")               val lng: Double,
    @SerializedName("accuracy")          val accuracy: Float? = null,
    @SerializedName("category")          val category: String = "OTHER",
    @SerializedName("message")           val message: String? = null,
    @SerializedName("transport")         val transport: String = "BOTH",
    @SerializedName("batteryPercentage") val batteryPercentage: Int? = null,
    @SerializedName("waterDepthCm")      val waterDepthCm: Int? = null,
    @SerializedName("passability")       val passability: String? = null,
    @SerializedName("packetId")          val packetId: String? = null
)

/**
 * Body sent when acknowledging an SOS.
 * confirmedSafe = true  → Relative/Family SOS: "Are you sure he/she is safe?"
 * confirmedSafe = false → Local Area SOS:      "Are you sure the situation is attended to?"
 */
data class AcknowledgeSosRequest(
    @SerializedName("confirmedSafe") val confirmedSafe: Boolean = true
)

// ── Response Bodies ──────────────────────────────────────────────────────────

data class SosUserSummaryDto(
    @SerializedName("id")          val id: String,
    @SerializedName("displayName") val displayName: String,
    @SerializedName("phoneNumber") val phoneNumber: String? = null
)

data class SosLocationDto(
    @SerializedName("type")        val type: String = "Point",
    @SerializedName("coordinates") val coordinates: List<Double> // [lng, lat]
)

/** Per-user acknowledgment entry returned by the backend */
data class SosAckEntryDto(
    @SerializedName("userId")        val userId: String? = null,
    @SerializedName("displayName")   val displayName: String = "",
    @SerializedName("confirmedSafe") val confirmedSafe: Boolean = true,
    @SerializedName("acknowledgedAt") val acknowledgedAt: String? = null
)

data class SosEventDto(
    @SerializedName("id")                  val id: String,
    @SerializedName("triggeredBy")         val triggeredBy: SosUserSummaryDto? = null,
    @SerializedName("location")            val location: SosLocationDto? = null,
    @SerializedName("accuracyMeters")      val accuracyMeters: Float? = null,
    @SerializedName("category")            val category: String = "OTHER",
    @SerializedName("message")             val message: String? = null,
    @SerializedName("transport")           val transport: String = "BOTH",
    @SerializedName("batteryPercentage")   val batteryPercentage: Int? = null,
    @SerializedName("status")              val status: String = "ACTIVE",
    @SerializedName("acknowledgedBy")      val acknowledgedBy: String? = null,
    @SerializedName("acknowledgedByUsers") val acknowledgedByUsers: List<SosAckEntryDto> = emptyList(),
    @SerializedName("isAcknowledgedByMe")  val isAcknowledgedByMe: Boolean = false,
    @SerializedName("resolvedBy")          val resolvedBy: String? = null,
    @SerializedName("createdAt")           val createdAt: String? = null,
    @SerializedName("updatedAt")           val updatedAt: String? = null,
    @SerializedName("waterDepthCm")        val waterDepthCm: Int? = null,
    @SerializedName("passability")         val passability: String? = null,
    @SerializedName("packetId")            val packetId: String? = null
)

data class SosDispatchResponse(
    @SerializedName("message") val message: String,
    @SerializedName("sos")     val sos: SosEventDto
)

data class SosDetailResponse(
    @SerializedName("sos") val sos: SosEventDto
)

data class SosActiveResponse(
    @SerializedName("events") val events: List<SosEventDto> = emptyList()
)

// ── Data Mule & Agentic Detour DTOs ─────────────────────────────────────────

data class MulePacketDto(
    @SerializedName("packetId")     val packetId: String,
    @SerializedName("lat")          val lat: Double,
    @SerializedName("lng")          val lng: Double,
    @SerializedName("category")     val category: String,
    @SerializedName("waterDepthCm") val waterDepthCm: Int = 0,
    @SerializedName("passability")  val passability: String = "ALL_PASSABLE",
    @SerializedName("message")      val message: String = "",
    @SerializedName("transport")    val transport: String = "MESH",
    @SerializedName("ts")           val ts: Long = System.currentTimeMillis()
)

data class BulkMuleRequest(
    @SerializedName("packets") val packets: List<MulePacketDto>
)

data class BulkMuleResponse(
    @SerializedName("accepted")   val accepted: Int = 0,
    @SerializedName("duplicates") val duplicates: Int = 0
)

data class DetourRequest(
    @SerializedName("originLat") val originLat: Double,
    @SerializedName("originLng") val originLng: Double,
    @SerializedName("destLat")   val destLat: Double,
    @SerializedName("destLng")   val destLng: Double
)

data class DetourResponse(
    @SerializedName("safeRouteGeoJson")    val safeRouteGeoJson: String? = null,
    @SerializedName("warningMessage")      val warningMessage: String = "",
    @SerializedName("avoidedHazardsCount") val avoidedHazardsCount: Int = 0,
    @SerializedName("agentReasoning")      val agentReasoning: String? = null
)

// ── Retrofit Service ─────────────────────────────────────────────────────────

interface SosApiService {

    @POST(ApiConstants.SOS)
    suspend fun dispatchSos(
        @Body body: SosDispatchRequest
    ): Response<SosDispatchResponse>

    @POST("${ApiConstants.SOS}/bulk-mule")
    suspend fun bulkMule(
        @Body body: BulkMuleRequest
    ): Response<BulkMuleResponse>

    @POST("api/routes/detour")
    suspend fun requestDetour(
        @Body body: DetourRequest
    ): Response<DetourResponse>

    @GET("${ApiConstants.SOS}/active")
    suspend fun getActiveSos(): Response<SosActiveResponse>

    /** Returns SOS events that THIS user has personally acknowledged */
    @GET("${ApiConstants.SOS}/acknowledged")
    suspend fun getAcknowledgedSos(): Response<SosActiveResponse>

    @GET("${ApiConstants.SOS}/{id}")
    suspend fun getSosById(
        @Path("id") id: String
    ): Response<SosDetailResponse>

    /**
     * Acknowledge an SOS alert.
     * Pass confirmedSafe=true for relative/family SOS ("he is safe"),
     * or confirmedSafe=false for local area/mesh SOS ("situation is attended to").
     */
    @PUT("${ApiConstants.SOS}/{id}/acknowledge")
    suspend fun acknowledgeSos(
        @Path("id") id: String,
        @Body body: AcknowledgeSosRequest
    ): Response<SosDetailResponse>
}

