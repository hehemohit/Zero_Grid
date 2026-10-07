package com.example.zerogrid.network

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Interface providing the current JWT auth token.
 */
interface TokenStore {
    fun getToken(): String?
}

/**
 * OkHttp Interceptor that automatically attaches Authorization: Bearer <token>
 * to all outgoing requests if an authenticated session exists.
 */
class AuthInterceptor(private val tokenStore: TokenStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenStore.getToken()
        val request = if (!token.isNullOrBlank()) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        return chain.proceed(request)
    }
}
