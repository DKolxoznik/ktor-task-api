package com.example.taskapi.model

/**
 * Пользователь в хранилище.
 * Пароль никогда не хранится в открытом виде — только BCrypt-хэш.
 * В JSON-ответы эта модель не попадает (есть отдельный [UserResponse]).
 */
data class User(
    val id: Int,
    val username: String,
    val passwordHash: String,
    val createdAt: String,
)
