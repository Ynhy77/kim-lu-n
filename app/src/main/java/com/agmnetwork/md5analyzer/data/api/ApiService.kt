package com.agmnetwork.md5analyzer.data.api

import com.agmnetwork.md5analyzer.data.model.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

interface ApiService {

    @POST("api/key/create")
    suspend fun createKey(
        @Body request: CreateKeyRequest
    ): Response<CreateKeyResponse>

    @POST("api/key/verify")
    suspend fun verifyKey(
        @Body request: VerifyKeyRequest
    ): Response<VerifyKeyResponse>

    @POST("api/access/check")
    suspend fun checkToken(
        @Header("Authorization") token: String,
        @Body request: CheckTokenRequest
    ): Response<CheckTokenResponse>

    @POST("api/access/logout")
    suspend fun logout(
        @Header("Authorization") token: String,
        @Body request: CheckTokenRequest
    ): Response<CheckTokenResponse>
}

object RetrofitClient {
    // Falls back to the workspace Dev App URL provided in metadata
    private const val DEFAULT_BASE_URL = "https://ais-dev-3cr6s4tzlkk3wkdq67zux4-161216122920.asia-southeast1.run.app/"

    private var currentBaseUrl = DEFAULT_BASE_URL

    fun setBaseUrl(url: String) {
        currentBaseUrl = if (url.endsWith("/")) url else "$url/"
        retrofitInstance = null
    }

    fun getBaseUrl(): String = currentBaseUrl

    private var retrofitInstance: ApiService? = null

    fun getService(): ApiService {
        if (retrofitInstance == null) {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(currentBaseUrl)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()

            retrofitInstance = retrofit.create(ApiService::class.java)
        }
        return retrofitInstance!!
    }
}
