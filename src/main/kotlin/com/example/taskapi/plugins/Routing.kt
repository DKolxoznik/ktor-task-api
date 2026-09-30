package com.example.taskapi.plugins

import com.example.taskapi.routes.authRoutes
import com.example.taskapi.routes.systemRoutes
import com.example.taskapi.routes.taskRoutes
import com.example.taskapi.service.AuthService
import com.example.taskapi.service.TaskService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

/**
 * Регистрация всех маршрутов приложения.
 */
fun Application.configureRouting(
    taskService: TaskService,
    authService: AuthService,
    version: String,
) {
    routing {
        systemRoutes(taskService, version)
        authRoutes(authService)
        taskRoutes(taskService)

        route("{...}") {
            handle {
                call.respondError(
                    HttpStatusCode.NotFound,
                    "Маршрут ${call.request.httpMethod.value} ${call.request.path()} не существует",
                )
            }
        }
    }
}
