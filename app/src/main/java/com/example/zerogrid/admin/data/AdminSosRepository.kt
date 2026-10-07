package com.example.zerogrid.admin.data

import com.example.zerogrid.admin.data.model.*
import com.example.zerogrid.network.RetrofitInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

class AdminSosRepository(
    private val api: AdminApiService = RetrofitInstance.adminApi
) {

    /**
     * Fetch active and acknowledged emergency incidents.
     * If [statusFilter] is "ALL", queries both ACTIVE and ACKNOWLEDGED concurrently and merges them,
     * matching the web application admin dashboard behavior.
     */
    suspend fun fetchSosEvents(statusFilter: String = "ALL"): Result<List<AdminSosEventDto>> =
        withContext(Dispatchers.IO) {
            try {
                if (statusFilter == "ALL") {
                    coroutineScope {
                        val activeDeferred = async { api.getActiveSosEvents(status = "ACTIVE", limit = 50) }
                        val ackDeferred = async { api.getActiveSosEvents(status = "ACKNOWLEDGED", limit = 50) }

                        val activeRes = activeDeferred.await()
                        val ackRes = ackDeferred.await()

                        val activeList = if (activeRes.isSuccessful) activeRes.body()?.events.orEmpty() else emptyList()
                        val ackList = if (ackRes.isSuccessful) ackRes.body()?.events.orEmpty() else emptyList()

                        val combined = (activeList + ackList).distinctBy { it.eventId }
                            .sortedByDescending { it.createdAt ?: "" }

                        Result.success(combined)
                    }
                } else {
                    val response = api.getActiveSosEvents(status = statusFilter, limit = 50)
                    if (response.isSuccessful) {
                        Result.success(response.body()?.events.orEmpty())
                    } else {
                        Result.failure(Exception("Failed to fetch SOS events: ${response.code()} ${response.message()}"))
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Fetch resolved SOS emergency history.
     */
    suspend fun fetchSosHistory(category: String? = null, page: Int = 1): Result<List<AdminSosEventDto>> =
        withContext(Dispatchers.IO) {
            try {
                val catFilter = if (category.isNullOrBlank() || category.equals("ALL", ignoreCase = true)) null else category
                val response = api.getSosHistory(page = page, limit = 50, category = catFilter)
                if (response.isSuccessful) {
                    Result.success(response.body()?.events.orEmpty())
                } else {
                    Result.failure(Exception("Failed to fetch SOS history: ${response.code()} ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Fetch full telemetry for a specific SOS incident.
     */
    suspend fun fetchSosDetails(id: String): Result<AdminSosEventDto> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.getSosById(id)
                if (response.isSuccessful && response.body()?.sos != null) {
                    Result.success(response.body()!!.sos!!)
                } else {
                    Result.failure(Exception("Failed to fetch SOS details: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Acknowledge an active SOS incident.
     */
    suspend fun acknowledgeSos(id: String, confirmedSafe: Boolean = false): Result<AdminSosEventDto> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.acknowledgeSos(id, AcknowledgeSosRequest(confirmedSafe))
                if (response.isSuccessful && response.body()?.sos != null) {
                    Result.success(response.body()!!.sos!!)
                } else {
                    Result.failure(Exception("Acknowledge failed: ${response.code()} ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Mark an incident as fully resolved.
     */
    suspend fun resolveSos(id: String): Result<AdminSosEventDto> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.resolveSos(id)
                if (response.isSuccessful && response.body()?.sos != null) {
                    Result.success(response.body()!!.sos!!)
                } else {
                    Result.failure(Exception("Resolve failed: ${response.code()} ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Append a case dispatch note to the incident timeline.
     */
    suspend fun addNote(id: String, text: String): Result<AdminSosEventDto> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.addNote(id, AddNoteRequest(text = text.trim()))
                if (response.isSuccessful && response.body()?.sos != null) {
                    Result.success(response.body()!!.sos!!)
                } else {
                    Result.failure(Exception("Add note failed: ${response.code()} ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
