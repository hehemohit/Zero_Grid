package com.example.zerogrid.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT

// ── Request bodies ─────────────────────────────────────────────────────────

data class RegisterRequest(
    val email: String,
    val password: String,
    val displayName: String,
    val role: String           // "CITIZEN" or "ADMIN"
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class CompleteProfileRequest(
    val phoneNumber: String,
    val dateOfBirth: String    // ISO format: "YYYY-MM-DD"
)

data class UpdateProfileRequest(
    val displayName: String? = null,
    val phoneNumber: String? = null,
    val dateOfBirth: String? = null    // ISO format: "YYYY-MM-DD"
)

// ── Response bodies ────────────────────────────────────────────────────────

data class UserDto(
    val id: String,
    val email: String,
    val displayName: String,
    val role: String,                     // "CITIZEN" or "ADMIN"
    val profileComplete: Boolean = false,
    val phoneNumber: String? = null,
    val dateOfBirth: String? = null,      // "YYYY-MM-DD" from backend
    val accountType: String? = "STANDARD",
    val photoUrl: String? = null
)

data class AuthResponse(
    val token: String,
    val user: UserDto
)

data class ProfileResponse(
    val user: UserDto
)

data class ApiErrorBody(
    val message: String
)

data class FcmTokenRequest(
    val fcmToken: String
)

data class SimpleMessageResponse(
    val message: String
)

// ── Retrofit interface ─────────────────────────────────────────────────────

interface AuthApiService {

    @POST(ApiConstants.REGISTER)
    suspend fun register(@Body body: RegisterRequest): Response<AuthResponse>

    @POST(ApiConstants.LOGIN)
    suspend fun login(@Body body: LoginRequest): Response<AuthResponse>

    @GET(ApiConstants.GET_ME)
    suspend fun getMe(): Response<ProfileResponse>

    @PUT(ApiConstants.UPDATE_ME)
    suspend fun updateProfile(
        @Body body: UpdateProfileRequest
    ): Response<ProfileResponse>

    @PUT(ApiConstants.COMPLETE_PROFILE)
    suspend fun completeProfile(
        @Body body: CompleteProfileRequest
    ): Response<ProfileResponse>

    @PUT(ApiConstants.FCM_TOKEN)
    suspend fun updateFcmToken(
        @Body body: FcmTokenRequest
    ): Response<SimpleMessageResponse>
}
