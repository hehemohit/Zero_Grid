package com.example.zerogrid.admin.data

import com.example.zerogrid.admin.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface AdminApiService {

    /**
     * GET /api/admin/sos?status=ACTIVE&page=1&limit=50
     * Returns list of SOS events filtered by status (ACTIVE, ACKNOWLEDGED, RESOLVED).
     */
    @GET("api/admin/sos")
    suspend fun getActiveSosEvents(
        @Query("status") status: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50
    ): Response<AdminSosResponse>

    /**
     * GET /api/admin/sos/history?page=1&limit=20&category=...
     * Returns resolved SOS events history with optional filters.
     */
    @GET("api/admin/sos/history")
    suspend fun getSosHistory(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50,
        @Query("category") category: String? = null,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null
    ): Response<AdminSosResponse>

    /**
     * GET /api/sos/:id
     * Returns full populated SOS payload.
     */
    @GET("api/sos/{id}")
    suspend fun getSosById(
        @Path("id") id: String
    ): Response<SingleSosResponse>

    /**
     * PUT /api/sos/:id/acknowledge
     * Acknowledges an active alert.
     */
    @PUT("api/sos/{id}/acknowledge")
    suspend fun acknowledgeSos(
        @Path("id") id: String,
        @Body body: AcknowledgeSosRequest = AcknowledgeSosRequest()
    ): Response<SingleSosResponse>

    /**
     * PUT /api/sos/:id/resolve
     * Resolves an emergency incident (Admin only).
     */
    @PUT("api/sos/{id}/resolve")
    suspend fun resolveSos(
        @Path("id") id: String
    ): Response<SingleSosResponse>

    /**
     * POST /api/sos/:id/notes
     * Appends a dispatcher note to the incident timeline.
     */
    @POST("api/sos/{id}/notes")
    suspend fun addNote(
        @Path("id") id: String,
        @Body body: AddNoteRequest
    ): Response<SingleSosResponse>

    /**
     * GET /api/admin/users?q={query}&limit=50
     * Searchable user directory.
     */
    @GET("api/admin/users")
    suspend fun getUsers(
        @Query("q") query: String? = null,
        @Query("limit") limit: Int = 50
    ): Response<AdminUsersResponse>

    /**
     * POST /api/admin/admins
     * Promotes a user to authority Admin.
     */
    @POST("api/admin/admins")
    suspend fun promoteAdmin(
        @Body body: PromoteAdminRequest
    ): Response<AdminActionResponse>

    /**
     * DELETE /api/admin/admins/:userId
     * Revokes admin privileges from a user.
     */
    @DELETE("api/admin/admins/{userId}")
    suspend fun removeAdmin(
        @Path("userId") userId: String
    ): Response<AdminActionResponse>
}
