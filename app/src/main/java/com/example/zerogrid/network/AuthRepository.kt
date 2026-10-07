package com.example.zerogrid.network

import com.example.zerogrid.BuildConfig
import com.google.gson.Gson
import com.zerogrid.mesh.app.ui.UserRole
import com.zerogrid.mesh.app.ui.UserSessionManager

// ── Result type ────────────────────────────────────────────────────────────

sealed class AuthResult {
    data class Success(val token: String, val user: UserDto, val role: UserRole) : AuthResult()
    data class Error(val message: String, val code: Int = 0) : AuthResult()
    data class AdminPending(val message: String) : AuthResult()
}

sealed class ProfileResult {
    data class Success(val user: UserDto) : ProfileResult()
    data class Error(val message: String, val code: Int = 0) : ProfileResult()
}

// ── Repository ─────────────────────────────────────────────────────────────

class AuthRepository(
    private val sessionManager: UserSessionManager,
    private val api: AuthApiService = RetrofitInstance.authApi
) {

    private val gson = Gson()

    /** Maps backend role string → app UserRole enum */
    private fun parseRole(roleString: String): UserRole =
        if (roleString.equals("ADMIN", ignoreCase = true)) UserRole.ADMIN else UserRole.CITIZEN

    /** Extracts error message from a failed Response body */
    private fun parseErrorMessage(errorBody: String?): String {
        return try {
            gson.fromJson(errorBody, ApiErrorBody::class.java)?.message ?: "An unexpected error occurred."
        } catch (_: Exception) {
            "An unexpected error occurred."
        }
    }

    /** Persist successful auth session including profile fields */
    private fun persistSession(token: String, user: UserDto, role: UserRole) {
        sessionManager.setAuthToken(token)
        sessionManager.setUserId(user.id)
        sessionManager.setUserEmail(user.email)
        sessionManager.setUserName(user.displayName)
        sessionManager.setUserRole(role)
        sessionManager.setProfileComplete(user.profileComplete)
        user.phoneNumber?.let { sessionManager.setPhoneNumber(it) }
        user.dateOfBirth?.let { sessionManager.setDateOfBirth(it) }
        user.accountType?.let { sessionManager.setAccountType(it) }
    }

    /** Bearer token header */
    private fun bearerToken(): String = "Bearer ${sessionManager.getAuthToken() ?: ""}"

    suspend fun register(
        email: String,
        password: String,
        displayName: String,
        role: UserRole
    ): AuthResult {
        return try {
            val backendRole = if (role == UserRole.ADMIN) "ADMIN" else "CITIZEN"
            val response = api.register(RegisterRequest(email, password, displayName, backendRole))

            when {
                response.isSuccessful -> {
                    val body = response.body()!!
                    val appRole = parseRole(body.user.role)
                    persistSession(body.token, body.user, appRole)
                    AuthResult.Success(body.token, body.user, appRole)
                }
                response.code() == 409 -> AuthResult.Error("An account with this email already exists.", 409)
                response.code() == 400 -> {
                    val msg = parseErrorMessage(response.errorBody()?.string())
                    AuthResult.Error(msg, 400)
                }
                else -> AuthResult.Error("Registration failed. Please try again.", response.code())
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) e.printStackTrace()
            AuthResult.Error("No connection. Check your internet and try again.")
        }
    }

    suspend fun login(email: String, password: String): AuthResult {
        return try {
            val response = api.login(LoginRequest(email, password))

            when {
                response.isSuccessful -> {
                    val body = response.body()!!
                    val appRole = parseRole(body.user.role)
                    persistSession(body.token, body.user, appRole)
                    AuthResult.Success(body.token, body.user, appRole)
                }
                response.code() == 403 -> AuthResult.AdminPending(
                    "Your admin account is pending approval. You will be able to log in once approved."
                )
                response.code() == 401 -> AuthResult.Error("Invalid email or password.", 401)
                response.code() == 400 -> AuthResult.Error("Email and password are required.", 400)
                else -> AuthResult.Error("Login failed. Please try again.", response.code())
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) e.printStackTrace()
            AuthResult.Error("No connection. Check your internet and try again.")
        }
    }

    suspend fun completeProfile(phoneNumber: String, dateOfBirth: String): ProfileResult {
        return try {
            val response = api.completeProfile(
                body = CompleteProfileRequest(phoneNumber, dateOfBirth)
            )
            when {
                response.isSuccessful -> {
                    val user = response.body()!!.user
                    sessionManager.setProfileComplete(user.profileComplete)
                    user.phoneNumber?.let { sessionManager.setPhoneNumber(it) }
                    user.dateOfBirth?.let { sessionManager.setDateOfBirth(it) }
                    ProfileResult.Success(user)
                }
                response.code() == 400 -> {
                    val msg = parseErrorMessage(response.errorBody()?.string())
                    ProfileResult.Error(msg, 400)
                }
                response.code() == 401 -> ProfileResult.Error("Session expired. Please log in again.", 401)
                else -> ProfileResult.Error("Failed to complete profile. Please try again.", response.code())
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) e.printStackTrace()
            ProfileResult.Error("No connection. Check your internet and try again.")
        }
    }

    suspend fun getProfile(): ProfileResult {
        return try {
            val response = api.getMe()
            when {
                response.isSuccessful -> {
                    val user = response.body()!!.user
                    // Refresh session cache
                    sessionManager.setUserName(user.displayName)
                    sessionManager.setProfileComplete(user.profileComplete)
                    user.phoneNumber?.let { sessionManager.setPhoneNumber(it) }
                    user.dateOfBirth?.let { sessionManager.setDateOfBirth(it) }
                    user.accountType?.let { sessionManager.setAccountType(it) }
                    ProfileResult.Success(user)
                }
                response.code() == 401 -> ProfileResult.Error("Session expired. Please log in again.", 401)
                response.code() == 404 -> ProfileResult.Error("User account not found.", 404)
                else -> ProfileResult.Error("Failed to load profile.", response.code())
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) e.printStackTrace()
            ProfileResult.Error("No connection. Check your internet and try again.")
        }
    }

    suspend fun updateProfile(
        displayName: String? = null,
        phoneNumber: String? = null,
        dateOfBirth: String? = null
    ): ProfileResult {
        return try {
            val response = api.updateProfile(
                body = UpdateProfileRequest(displayName, phoneNumber, dateOfBirth)
            )
            when {
                response.isSuccessful -> {
                    val user = response.body()!!.user
                    sessionManager.setUserName(user.displayName)
                    user.phoneNumber?.let { sessionManager.setPhoneNumber(it) }
                    user.dateOfBirth?.let { sessionManager.setDateOfBirth(it) }
                    ProfileResult.Success(user)
                }
                response.code() == 400 -> {
                    val msg = parseErrorMessage(response.errorBody()?.string())
                    ProfileResult.Error(msg, 400)
                }
                response.code() == 401 -> ProfileResult.Error("Session expired. Please log in again.", 401)
                else -> ProfileResult.Error("Failed to update profile. Please try again.", response.code())
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) e.printStackTrace()
            ProfileResult.Error("No connection. Check your internet and try again.")
        }
    }

    /**
     * Send updated FCM push token to server for emergency notifications.
     */
    suspend fun updateFcmToken(fcmToken: String): Boolean {
        return try {
            val response = api.updateFcmToken(FcmTokenRequest(fcmToken.trim()))
            response.isSuccessful
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) e.printStackTrace()
            false
        }
    }
}
