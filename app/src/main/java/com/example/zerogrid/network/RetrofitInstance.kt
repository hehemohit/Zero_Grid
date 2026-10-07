package com.example.zerogrid.network

import com.example.zerogrid.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitInstance {

    @Volatile
    private var tokenStore: TokenStore? = null

    fun initialize(tokenStore: TokenStore) {
        this.tokenStore = tokenStore
    }

    private val authInterceptor = AuthInterceptor(object : TokenStore {
        override fun getToken(): String? = tokenStore?.getToken()
    })

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        // Log clean request line and response status in debug builds (avoids dumping multi-thousand-line JSON bodies)
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BASIC
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val authApi: AuthApiService by lazy {
        retrofit.create(AuthApiService::class.java)
    }

    val contactsApi: ContactsApiService by lazy {
        retrofit.create(ContactsApiService::class.java)
    }

    val sosApi: SosApiService by lazy {
        retrofit.create(SosApiService::class.java)
    }

    val familyApi: FamilyApiService by lazy {
        retrofit.create(FamilyApiService::class.java)
    }

    val adminApi: com.example.zerogrid.admin.data.AdminApiService by lazy {
        retrofit.create(com.example.zerogrid.admin.data.AdminApiService::class.java)
    }
}
