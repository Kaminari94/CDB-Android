package com.example.centraledellebolle.network

import com.example.centraledellebolle.data.TokenStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val tokenStore: TokenStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath

        // Non aggiungere l'header per le rotte di login e health
        if (path.contains("api/token") || path.contains("api/health")) {
            return chain.proceed(request)
        }

        // runBlocking è usato qui perché l'interceptor non è una suspend function.
        // L'operazione è veloce perché il token è in memoria o letto da un file.
        val accessToken = runBlocking {
            tokenStore.accessToken.first()
        }

        val newRequest = if (accessToken != null) {
            request.newBuilder()
                .header("Authorization", "Bearer $accessToken")
                .build()
        } else {
            request
        }

        return chain.proceed(newRequest)
    }
}
