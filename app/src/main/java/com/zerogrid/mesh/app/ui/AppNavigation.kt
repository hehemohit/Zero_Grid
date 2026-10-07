package com.zerogrid.mesh.app.ui

import android.content.Context
import android.content.SharedPreferences
import com.example.zerogrid.network.TokenStore

/**
 * Navigation routes for role-selection and dashboard routing.
 */
enum class AppRoute {
    ROUTE_ROLE_SELECTION,
    ROUTE_USER_DASHBOARD,
    ROUTE_AUTHORITY_DASHBOARD
}

/**
 * Operational mode / role in the ZeroGrid network.
 */
enum class UserRole {
    CITIZEN,
    ADMIN
}

/**
 * User session & role persistence manager.
 * Stores auth token, role, profile fields across app restarts.
 */
class UserSessionManager(context: Context) : TokenStore {

    companion object {
        private const val PREFS_NAME          = "zerogrid_role_session"
        private const val KEY_USER_NAME       = "saved_user_name"
        private const val KEY_USER_ROLE       = "saved_user_role"
        private const val KEY_AUTH_TOKEN      = "auth_token"
        private const val KEY_USER_ID         = "user_id"
        private const val KEY_USER_EMAIL      = "user_email"
        private const val KEY_PROFILE_COMPLETE = "profile_complete"
        private const val KEY_PHONE_NUMBER    = "phone_number"
        private const val KEY_DATE_OF_BIRTH   = "date_of_birth"
        private const val KEY_ACCOUNT_TYPE    = "account_type"
        private const val KEY_FCM_TOKEN       = "fcm_token"

        @Volatile
        private var INSTANCE: UserSessionManager? = null

        fun getInstance(context: Context): UserSessionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserSessionManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ── Display name ────────────────────────────────────────────────────────

    fun getUserName(): String = prefs.getString(KEY_USER_NAME, "") ?: ""
    fun setUserName(name: String) = prefs.edit().putString(KEY_USER_NAME, name.trim()).apply()
    fun getUserDisplayName(): String = getUserName()
    fun setUserDisplayName(name: String) = setUserName(name)

    // ── Role ────────────────────────────────────────────────────────────────

    fun getUserRole(): UserRole? {
        val roleStr = prefs.getString(KEY_USER_ROLE, null) ?: return null
        return try { UserRole.valueOf(roleStr) } catch (_: Exception) { null }
    }

    fun setUserRole(role: UserRole?) {
        if (role == null) prefs.edit().remove(KEY_USER_ROLE).apply()
        else prefs.edit().putString(KEY_USER_ROLE, role.name).apply()
    }

    // ── Auth token & server identity ────────────────────────────────────────

    override fun getToken(): String? = getAuthToken()
    fun getAuthToken(): String? = prefs.getString(KEY_AUTH_TOKEN, null)
    fun setAuthToken(token: String) = prefs.edit().putString(KEY_AUTH_TOKEN, token).apply()

    fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)
    fun setUserId(id: String) = prefs.edit().putString(KEY_USER_ID, id).apply()

    fun getUserEmail(): String? = prefs.getString(KEY_USER_EMAIL, null)
    fun setUserEmail(email: String) = prefs.edit().putString(KEY_USER_EMAIL, email).apply()

    fun isLoggedIn(): Boolean = !getAuthToken().isNullOrEmpty()

    // ── Profile completion ──────────────────────────────────────────────────

    fun isProfileComplete(): Boolean = prefs.getBoolean(KEY_PROFILE_COMPLETE, false)
    fun setProfileComplete(complete: Boolean) =
        prefs.edit().putBoolean(KEY_PROFILE_COMPLETE, complete).apply()

    // ── Profile fields ──────────────────────────────────────────────────────

    fun getPhoneNumber(): String = prefs.getString(KEY_PHONE_NUMBER, "") ?: ""
    fun setPhoneNumber(phone: String) = prefs.edit().putString(KEY_PHONE_NUMBER, phone).apply()

    fun getDateOfBirth(): String = prefs.getString(KEY_DATE_OF_BIRTH, "") ?: ""
    fun setDateOfBirth(dob: String) = prefs.edit().putString(KEY_DATE_OF_BIRTH, dob).apply()

    fun getAccountType(): String = prefs.getString(KEY_ACCOUNT_TYPE, "STANDARD") ?: "STANDARD"
    fun setAccountType(type: String) = prefs.edit().putString(KEY_ACCOUNT_TYPE, type).apply()

    fun getFcmToken(): String? = prefs.getString(KEY_FCM_TOKEN, null)
    fun setFcmToken(token: String) = prefs.edit().putString(KEY_FCM_TOKEN, token.trim()).apply()

    // ── Clear all ───────────────────────────────────────────────────────────

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
