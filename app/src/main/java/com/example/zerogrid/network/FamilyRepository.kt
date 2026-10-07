package com.example.zerogrid.network

import com.example.zerogrid.BuildConfig
import com.google.gson.Gson

sealed class FamilyResult<out T> {
    data class Success<out T>(val data: T) : FamilyResult<T>()
    data class Error(val message: String, val code: Int = 0) : FamilyResult<Nothing>()
}

class FamilyRepository(
    private val api: FamilyApiService = RetrofitInstance.familyApi
) {
    private val gson = Gson()

    private fun parseErrorMessage(errorBody: String?): String {
        return try {
            gson.fromJson(errorBody, ApiErrorBody::class.java)?.message ?: "An unexpected error occurred."
        } catch (_: Exception) {
            "An unexpected error occurred."
        }
    }

    suspend fun requestLink(childEmail: String): FamilyResult<FamilyLinkDto?> {
        return try {
            val response = api.requestLink(LinkRequest(childEmail.trim()))
            if (response.isSuccessful) {
                FamilyResult.Success(response.body()?.link)
            } else {
                val msg = parseErrorMessage(response.errorBody()?.string())
                FamilyResult.Error(msg, response.code())
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) e.printStackTrace()
            FamilyResult.Error("No connection. Check your internet and try again.")
        }
    }

    suspend fun acceptLink(linkId: String): FamilyResult<FamilyLinkDto?> {
        return try {
            val response = api.acceptLink(linkId)
            if (response.isSuccessful) {
                FamilyResult.Success(response.body()?.link)
            } else {
                val msg = parseErrorMessage(response.errorBody()?.string())
                FamilyResult.Error(msg, response.code())
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) e.printStackTrace()
            FamilyResult.Error("No connection. Check your internet and try again.")
        }
    }

    suspend fun revokeLink(linkId: String): FamilyResult<String> {
        return try {
            val response = api.revokeLink(linkId)
            if (response.isSuccessful) {
                FamilyResult.Success(linkId)
            } else {
                val msg = parseErrorMessage(response.errorBody()?.string())
                FamilyResult.Error(msg, response.code())
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) e.printStackTrace()
            FamilyResult.Error("No connection. Check your internet and try again.")
        }
    }

    suspend fun getChildLocation(childId: String): FamilyResult<ChildLocationDto> {
        return try {
            val response = api.getChildLocation(childId)
            if (response.isSuccessful && response.body() != null) {
                FamilyResult.Success(response.body()!!)
            } else {
                val msg = parseErrorMessage(response.errorBody()?.string())
                FamilyResult.Error(msg, response.code())
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) e.printStackTrace()
            FamilyResult.Error("No connection. Check your internet and try again.")
        }
    }

    suspend fun getFamilyLinks(): FamilyResult<List<FamilyLinkDto>> {
        return try {
            val response = api.getFamilyLinks()
            if (response.isSuccessful) {
                FamilyResult.Success(response.body()?.links ?: emptyList())
            } else {
                val msg = parseErrorMessage(response.errorBody()?.string())
                FamilyResult.Error(msg, response.code())
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) e.printStackTrace()
            FamilyResult.Error("No connection. Check your internet and try again.")
        }
    }
}
