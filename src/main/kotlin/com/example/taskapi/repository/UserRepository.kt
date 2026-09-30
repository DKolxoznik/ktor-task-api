package com.example.taskapi.repository

import com.example.taskapi.model.User
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Хранилище пользователей в памяти.
 * Пароли сюда уже приходят в виде BCrypt-хэша — открытый текст здесь не появляется.
 */
class UserRepository {

    private val byId = ConcurrentHashMap<Int, User>()
    private val byUsername = ConcurrentHashMap<String, User>()
    private val idSequence = AtomicInteger(0)

    fun findById(id: Int): User? = byId[id]

    fun findByUsername(username: String): User? = byUsername[username.lowercase()]

    fun existsByUsername(username: String): Boolean = byUsername.containsKey(username.lowercase())

    fun create(username: String, passwordHash: String): User {
        val user = User(
            id = idSequence.incrementAndGet(),
            username = username,
            passwordHash = passwordHash,
            createdAt = Instant.now().toString(),
        )
        byId[user.id] = user
        byUsername[username.lowercase()] = user
        return user
    }

    fun count(): Int = byId.size
}
