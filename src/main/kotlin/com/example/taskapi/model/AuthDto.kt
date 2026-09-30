package com.example.taskapi.model

import kotlinx.serialization.Serializable

/** Тело запроса регистрации (POST /api/auth/register). */
@Serializable
data class RegisterRequest(
    val username: String? = null,
    val password: String? = null,
)

/** Тело запроса входа (POST /api/auth/login). */
@Serializable
data class LoginRequest(
    val username: String? = null,
    val password: String? = null,
)

/** Ответ после регистрации: без хэша пароля. */
@Serializable
data class UserResponse(
    val id: Int,
    val username: String,
    val createdAt: String,
)

/** Ответ после успешного входа: JWT и метаданные. */
@Serializable
data class TokenResponse(
    val accessToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long,
    val userId: Int,
    val username: String,
)
