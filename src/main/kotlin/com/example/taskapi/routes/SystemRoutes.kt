package com.example.taskapi.routes

import com.example.taskapi.model.ApiInfoResponse
import com.example.taskapi.model.HealthResponse
import com.example.taskapi.service.TaskService
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

/**
 * Служебные маршруты: корневая страница со списком эндпоинтов и health-check,
 * который использует Docker HEALTHCHECK.
 */
fun Route.systemRoutes(service: TaskService, version: String) {
    get("/") {
        call.respond(
            HttpStatusCode.OK,
            ApiInfoResponse(
                service = "ktor-task-api",
                version = version,
                description = "REST API для управления задачами на Ktor (КТ-1)",
                endpoints = listOf(
                    "GET    /health",
                    "GET    /api/tasks?status=&priority=&q=&page=&size=&sort=&order=",
                    "GET    /api/tasks/{id}",
                    "GET    /api/tasks/status/{status}",
                    "POST   /api/tasks",
                    "PUT    /api/tasks/{id}",
                    "DELETE /api/tasks/{id}",
                    "DELETE /api/tasks?status={status}",
                ),
            ),
        )
    }

    get("/health") {
        call.respond(
            HttpStatusCode.OK,
            HealthResponse(
                status = "UP",
                service = "ktor-task-api",
                version = version,
                tasksInStorage = service.count(),
            ),
        )
    }
}
