package com.agmnetwork.md5analyzer.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CreateKeyRequest(
    val installationId: String
)

@JsonClass(generateAdapter = true)
data class CreateKeyResponse(
    val success: Boolean,
    val sessionId: String?,
    val shortUrl: String?,
    val expiresAt: String?,
    val message: String? = null
)

@JsonClass(generateAdapter = true)
data class VerifyKeyRequest(
    val sessionId: String,
    val installationId: String,
    val key: String
)

@JsonClass(generateAdapter = true)
data class VerifyKeyResponse(
    val success: Boolean,
    val accessToken: String?,
    val expiresAt: String?,
    val message: String? = null
)

@JsonClass(generateAdapter = true)
data class CheckTokenRequest(
    val installationId: String
)

@JsonClass(generateAdapter = true)
data class CheckTokenResponse(
    val success: Boolean,
    val message: String? = null
)

@JsonClass(generateAdapter = true)
data class HistoryEntry(
    val id: String,
    val md5: String,
    val dice1: Int,
    val dice2: Int,
    val dice3: Int,
    val total: Int,
    val result: String,
    val timestamp: Long
)
