package com.example.taskapi.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.JWTVerifier
import java.util.Date

/**
 * Настройки JWT, читаемые из application.yaml / переменных окружения.
 */
data class JwtConfig(
    val secret: String,
    val issuer: String,
    val audience: String,
    val realm: String,
    val expiresInSeconds: Long,
) {
    val algorithm: Algorithm = Algorithm.HMAC256(secret)

    val verifier: JWTVerifier = JWT
        .require(algorithm)
        .withIssuer(issuer)
        .withAudience(audience)
        .build()
}

/**
 * Выдача JWT-токенов.
 * В payload кладётся полезный claim `userId` (и `username` для удобства).
 */
class JwtService(private val config: JwtConfig) {

    fun createToken(userId: Int, username: String): String {
        val now = System.currentTimeMillis()
        return JWT.create()
            .withIssuer(config.issuer)
            .withAudience(config.audience)
            .withClaim("userId", userId)
            .withClaim("username", username)
            .withIssuedAt(Date(now))
            .withExpiresAt(Date(now + config.expiresInSeconds * 1000))
            .sign(config.algorithm)
    }

    fun expiresInSeconds(): Long = config.expiresInSeconds
}
