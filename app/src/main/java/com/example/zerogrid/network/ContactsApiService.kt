package com.example.zerogrid.network

import androidx.compose.runtime.Immutable
import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.*

// ── Contact DTOs ───────────────────────────────────────────────────────────

@Immutable
data class ContactUserDto(
    @SerializedName("id")          val id: String? = null,
    @SerializedName("displayName") val displayName: String? = null,
    @SerializedName("email")       val email: String? = null,
    @SerializedName("phoneNumber") val phoneNumber: String? = null,
    @SerializedName("role")        val role: String? = null,
    @SerializedName("photoUrl")    val photoUrl: String? = null
)

@Immutable
data class ContactDto(
    @SerializedName("id")               val rawId: String? = null,
    @SerializedName("_id")              val mongoId: String? = null,
    @SerializedName("name")             val rawName: String? = null,
    @SerializedName("phoneNumber")      val rawPhoneNumber: String? = null,
    @SerializedName("relationship")     val rawRelationship: String? = null,
    @SerializedName("label")            val label: String? = null,
    @SerializedName("contactUserId")    val contactUserId: String? = null,
    @SerializedName("contactUser")      val contactUser: ContactUserDto? = null,
    @SerializedName("isRegisteredUser") val isRegisteredUser: Boolean? = false,
    @SerializedName("createdAt")        val createdAt: String? = null
) {
    val id: String
        get() = rawId ?: mongoId ?: ""

    val name: String
        get() = rawName?.takeIf { it.isNotBlank() }
            ?: contactUser?.displayName?.takeIf { it.isNotBlank() }
            ?: contactUser?.email
            ?: "Emergency Contact"

    val phoneNumber: String
        get() = rawPhoneNumber?.takeIf { it.isNotBlank() }
            ?: contactUser?.phoneNumber?.takeIf { it.isNotBlank() }
            ?: contactUser?.email
            ?: "No phone registered"

    val relationship: String
        get() = rawRelationship?.takeIf { it.isNotBlank() }
            ?: label?.takeIf { it.isNotBlank() }
            ?: "Emergency Contact"
}

data class AddContactRequest(
    @SerializedName("contactEmailOrPhone") val contactEmailOrPhone: String,
    @SerializedName("label")               val label: String = "Emergency Contact",
    @SerializedName("name")                val name: String? = null,
    @SerializedName("phoneNumber")         val phoneNumber: String? = null,
    @SerializedName("relationship")        val relationship: String? = label
)

data class ContactsListResponse(
    @SerializedName("contacts") val contacts: List<ContactDto> = emptyList()
)

data class AddContactResponse(
    @SerializedName("message") val message: String = "",
    @SerializedName("contact") val contact: ContactDto
)

data class DeleteContactResponse(
    @SerializedName("message") val message: String = "",
    @SerializedName("id")      val id: String = ""
)

// ── Retrofit Service ───────────────────────────────────────────────────────

interface ContactsApiService {

    @GET(ApiConstants.CONTACTS)
    suspend fun getContacts(): Response<ContactsListResponse>

    @POST(ApiConstants.CONTACTS)
    suspend fun addContact(
        @Body body: AddContactRequest
    ): Response<AddContactResponse>

    @DELETE("${ApiConstants.CONTACTS}/{id}")
    suspend fun deleteContact(
        @Path("id") id: String
    ): Response<DeleteContactResponse>
}
