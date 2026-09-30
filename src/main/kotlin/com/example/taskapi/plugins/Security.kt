package com.example.taskapi.plugins

import com.auth0.jwt.interfaces.Payload
import com.example.taskapi.security.JwtConfig
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt

const val JWT_AUTH = "auth-jwt"

/**
 * Плагин Authentication с JWT-провайдером.
 *
 * Токен ожидается в заголовке `Authorization: Bearer <token>`.
 * В валидном токене должен быть claim `userId` — иначе principal не создаётся
 * и клиент получает 401.
 */
fun Application.configureSecurity(jwtConfig: JwtConfig) {
    install(Authentication) {
        jwt(JWT_AUTH) {
            realm = jwtConfig.realm
            verifier(jwtConfig.verifier)
            validate { credential ->
                val userId = credential.payload.userIdOrNull()
                if (userId != null) JWTPrincipal(credential.payload) else null
            }
            challenge { _, _ ->
                call.respondError(
                    HttpStatusCode.Unauthorized,
                    "Требуется заголовок Authorization: Bearer <token> с валидным JWT",
                )
            }
        }
    }
}

/** Достаёт целочисленный claim `userId` из payload JWT. */
fun Payload.userIdOrNull(): Int? = getClaim("userId")?.asInt()
