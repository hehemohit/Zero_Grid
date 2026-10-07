package com.example.zerogrid.admin.data

import com.example.zerogrid.admin.data.model.AdminUserDto
import com.example.zerogrid.admin.data.model.PromoteAdminRequest
import com.example.zerogrid.network.RetrofitInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AdminUserRepository(
    private val api: AdminApiService = RetrofitInstance.adminApi
) {

    /**
     * Search user directory.
     */
    suspend fun fetchUsers(query: String = ""): Result<List<AdminUserDto>> =
        withContext(Dispatchers.IO) {
            try {
                val q = query.trim().ifBlank { null }
                val response = api.getUsers(query = q, limit = 50)
                if (response.isSuccessful) {
                    Result.success(response.body()?.users.orEmpty())
                } else {
                    Result.failure(Exception("Failed to fetch users: ${response.code()} ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Promote user to Admin by email.
     */
    suspend fun promoteAdmin(email: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.promoteAdmin(PromoteAdminRequest(email = email.trim()))
                if (response.isSuccessful) {
                    Result.success(response.body()?.message ?: "User promoted to Admin successfully.")
                } else {
                    Result.failure(Exception("Failed to promote admin: ${response.code()} ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Revoke admin privileges for a user.
     */
    suspend fun revokeAdmin(userId: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.removeAdmin(userId)
                if (response.isSuccessful) {
                    Result.success(response.body()?.message ?: "Admin privileges revoked.")
                } else {
                    Result.failure(Exception("Failed to revoke admin: ${response.code()} ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
