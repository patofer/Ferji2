package com.ferji.inspecciones.data.network.interceptors


import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

// Si no usas Hilt para el interceptor directamente, puedes omitir @Singleton y @Inject aquí
// y construirlo manualmente al crear OkHttpClient.
// Por ahora, lo dejaré simple sin Hilt para el interceptor en sí mismo.
class AuthorizationInterceptor(private val apiKey: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val newRequest = originalRequest.newBuilder()
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json") // Aunque Retrofit lo añade, es bueno ser explícito
            .build()
        return chain.proceed(newRequest)
    }
}