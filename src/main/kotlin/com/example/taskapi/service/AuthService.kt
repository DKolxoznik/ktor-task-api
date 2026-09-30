package com.example.taskapi.service

import com.example.taskapi.error.ConflictException
import com.example.taskapi.error.UnauthorizedException
import com.example.taskapi.error.ValidationException
import com.example.taskapi.model.LoginRequest
import com.example.taskapi.model.RegisterRequest
import com.example.taskapi.model.TokenResponse
import com.example.taskapi.model.UserResponse
import com.example.taskapi.repository.UserRepository
import com.example.taskapi.security.JwtService
import com.example.taskapi.security.PasswordHasher

/**
 * Регистрация и вход.
 * Пароль хэшируется BCrypt при регистрации и проверяется через verify при входе —
 * сравнение строк `password == hash` здесь намеренно не используется.
 */
class AuthService(
    private val users: UserRepository,
    private val jwtService: JwtService,
) {

    companion object {
        const val MIN_USERNAME_LENGTH = 3
        const val MAX_USERNAME_LENGTH = 32
        const val MIN_PASSWORD_LENGTH = 6
        const val MAX_PASSWORD_LENGTH = 72
        private val USERNAME_PATTERN = Regex("^[a-zA-Z0-9_]+$")
    }

    fun register(request: RegisterRequest): UserResponse {
        val username = request.username?.trim().orEmpty()
        val password = request.password.orEmpty()

        val details = buildList {
            if (username.length < MIN_USERNAME_LENGTH) {
                add("Поле 'username' должно быть не короче $MIN_USERNAME_LENGTH символов")
            }
            if (username.length > MAX_USERNAME_LENGTH) {
                add("Поле 'username' не может быть длиннее $MAX_USERNAME_LENGTH символов")
            }
            if (username.isNotEmpty() && !USERNAME_PATTERN.matches(username)) {
                add("Поле 'username' может содержать только латинские буквы, цифры и '_'")
            }
            if (password.length < MIN_PASSWORD_LENGTH) {
                add("Поле 'password' должно быть не короче $MIN_PASSWORD_LENGTH символов")
            }
            if (password.length > MAX_PASSWORD_LENGTH) {
                add("Поле 'password' не может быть длиннее $MAX_PASSWORD_LENGTH символов")
            }
        }
        if (details.isNotEmpty()) throw ValidationException(details)

        if (users.existsByUsername(username)) {
            throw ConflictException("Пользователь с логином '$username' уже зарегистрирован")
        }

        val user = users.create(
            username = username,
            passwordHash = PasswordHasher.hash(password),
        )
        return UserResponse(id = user.id, username = user.username, createdAt = user.createdAt)
    }

    fun login(request: LoginRequest): TokenResponse {
        val username = request.username?.trim().orEmpty()
        val password = request.password.orEmpty()

        if (username.isEmpty() || password.isEmpty()) {
            throw ValidationException(
                listOf("Поля 'username' и 'password' обязательны"),
            )
        }

        val user = users.findByUsername(username)
            ?: throw UnauthorizedException("Неверный логин или пароль")

        // Сверка хэша через BCrypt.verify — не прямое сравнение строк.
        if (!PasswordHasher.verify(password, user.passwordHash)) {
            throw UnauthorizedException("Неверный логин или пароль")
        }

        return TokenResponse(
            accessToken = jwtService.createToken(user.id, user.username),
            expiresIn = jwtService.expiresInSeconds(),
            userId = user.id,
            username = user.username,
        )
    }
}
