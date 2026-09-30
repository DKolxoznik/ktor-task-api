package com.example.taskapi.routes

import com.example.taskapi.model.LoginRequest
import com.example.taskapi.model.RegisterRequest
import com.example.taskapi.service.AuthService
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

/**
 * Публичные маршруты аутентификации (КТ-2).
 *
 *  POST /api/auth/register — регистрация (пароль сохраняется как BCrypt-хэш)
 *  POST /api/auth/login    — вход, сверка хэша и выдача JWT
 */
fun Route.authRoutes(authService: AuthService) {
    route("/api/auth") {
        post("/register") {
            val request = call.receive<RegisterRequest>()
            val created = authService.register(request)
            call.respond(HttpStatusCode.Created, created)
        }

        post("/login") {
            val request = call.receive<LoginRequest>()
            call.respond(HttpStatusCode.OK, authService.login(request))
        }
    }
}
