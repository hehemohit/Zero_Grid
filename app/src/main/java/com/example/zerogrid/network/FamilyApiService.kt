package com.example.zerogrid.network

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.*

data class FamilyLinkDto(
    @SerializedName("id")          val id: String,
    @SerializedName("childId")     val childId: String? = null,
    @SerializedName("parentId")    val parentId: String? = null,
    @SerializedName("childName")   val childName: String? = null,
    @SerializedName("childEmail")  val childEmail: String? = null,
    @SerializedName("parentName")  val parentName: String? = null,
    @SerializedName("parentEmail") val parentEmail: String? = null,
    @SerializedName("status")      val status: String = "PENDING", // PENDING, ACCEPTED, REVOKED
    @SerializedName("requestedAt") val requestedAt: String? = null
)

data class LinkRequest(
    @SerializedName("childEmail") val childEmail: String
)

data class LinkResponse(
    @SerializedName("message") val message: String,
    @SerializedName("link")    val link: FamilyLinkDto? = null
)

data class FamilyLinksListResponse(
    @SerializedName("links") val links: List<FamilyLinkDto> = emptyList()
)

data class ChildLocationDto(
    @SerializedName("child")          val child: UserDto? = null,
    @SerializedName("location")       val location: SosLocationDto? = null,
    @SerializedName("lastLocationAt") val lastLocationAt: String? = null
)

interface FamilyApiService {

    @POST("${ApiConstants.FAMILY}/link-request")
    suspend fun requestLink(
        @Body body: LinkRequest
    ): Response<LinkResponse>

    @PUT("${ApiConstants.FAMILY}/link/{id}/accept")
    suspend fun acceptLink(
        @Path("id") id: String
    ): Response<LinkResponse>

    @PUT("${ApiConstants.FAMILY}/link/{id}/revoke")
    suspend fun revokeLink(
        @Path("id") id: String
    ): Response<SimpleMessageResponse>

    @GET("${ApiConstants.FAMILY}/child/{childId}/location")
    suspend fun getChildLocation(
        @Path("childId") childId: String
    ): Response<ChildLocationDto>

    @GET("${ApiConstants.FAMILY}/links")
    suspend fun getFamilyLinks(): Response<FamilyLinksListResponse>
}
