package com.example.zerogrid.admin.data.model

import com.google.gson.annotations.SerializedName

/**
 * User directory record returned by /api/admin/users.
 */
data class AdminUserDto(
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
    @SerializedName("role")
    val role: String = "CITIZEN",
    @SerializedName("adminApproved")
    val adminApproved: Boolean? = null,
    @SerializedName("profileComplete")
    val profileComplete: Boolean? = null,
    @SerializedName("createdAt")
    val createdAt: String? = null
) {
    val userId: String
        get() = id ?: _id ?: ""

    val isAdmin: Boolean
        get() = role.equals("ADMIN", ignoreCase = true)

    val nameDisplay: String
        get() = displayName?.ifBlank { null } ?: email?.ifBlank { null } ?: "ZeroGrid Node"

    val shortId: String
        get() = if (userId.length >= 4) userId.takeLast(4).uppercase() else "81FA"

    val nodeAddress: String
        get() = "ZG-0x$shortId"

    val statusDisplay: String
        get() = if (adminApproved == true || profileComplete == true) "Active" else "Standby"
}

data class AdminUsersResponse(
    @SerializedName("users")
    val users: List<AdminUserDto> = emptyList()
)

data class PromoteAdminRequest(
    @SerializedName("email")
    val email: String? = null,
    @SerializedName("userId")
    val userId: String? = null
)

data class AdminActionResponse(
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("user")
    val user: AdminUserDto? = null
)
