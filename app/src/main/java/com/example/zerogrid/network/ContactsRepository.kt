package com.example.zerogrid.network

import com.example.zerogrid.BuildConfig
import com.google.gson.Gson
import com.zerogrid.mesh.app.ui.UserSessionManager

sealed class ContactsResult<out T> {
    data class Success<out T>(val data: T) : ContactsResult<T>()
    data class Error(val message: String, val code: Int = 0) : ContactsResult<Nothing>()
}

class ContactsRepository(
    private val sessionManager: UserSessionManager,
    private val api: ContactsApiService = RetrofitInstance.contactsApi
) {
    private val gson = Gson()

    private fun bearerToken(): String = "Bearer ${sessionManager.getAuthToken() ?: ""}"

    private fun parseErrorMessage(errorBody: String?): String {
        return try {
            gson.fromJson(errorBody, ApiErrorBody::class.java)?.message ?: "An unexpected error occurred."
        } catch (_: Exception) {
            "An unexpected error occurred."
        }
    }

    suspend fun getContacts(): ContactsResult<List<ContactDto>> {
        return try {
            val response = api.getContacts()
            if (response.isSuccessful) {
                ContactsResult.Success(response.body()?.contacts ?: emptyList())
            } else {
                val msg = parseErrorMessage(response.errorBody()?.string())
                ContactsResult.Error(msg, response.code())
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) e.printStackTrace()
            ContactsResult.Error("No connection. Check your internet and try again.")
        }
    }

    suspend fun addContact(emailOrPhone: String, label: String = "Emergency Contact"): ContactsResult<ContactDto> {
        return try {
            val response = api.addContact(
                body = AddContactRequest(
                    contactEmailOrPhone = emailOrPhone.trim(),
                    label = label.trim().ifEmpty { "Emergency Contact" },
                    name = label.trim(),
                    phoneNumber = emailOrPhone.trim(),
                    relationship = label.trim().ifEmpty { "Emergency Contact" }
                )
            )
            if (response.isSuccessful && response.body() != null) {
                ContactsResult.Success(response.body()!!.contact)
            } else {
                val msg = parseErrorMessage(response.errorBody()?.string())
                ContactsResult.Error(msg, response.code())
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) e.printStackTrace()
            ContactsResult.Error("No connection. Check your internet and try again.")
        }
    }

    suspend fun deleteContact(contactId: String): ContactsResult<String> {
        return try {
            val response = api.deleteContact(
                id = contactId
            )
            if (response.isSuccessful) {
                ContactsResult.Success(contactId)
            } else {
                val msg = parseErrorMessage(response.errorBody()?.string())
                ContactsResult.Error(msg, response.code())
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) e.printStackTrace()
            ContactsResult.Error("No connection. Check your internet and try again.")
        }
    }
}
