package com.example.taskapi.plugins

import io.ktor.server.application.Application
import io.ktor.server.plugins.openapi.openAPI
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.routing.routing

/**
 * OpenAPI-спецификация и интерактивный Swagger UI (КТ-3).
 *
 *  GET /swagger  — веб-интерфейс Swagger UI
 *  GET /openapi  — сырой YAML/JSON OpenAPI
 *
 * Спецификация лежит в `resources/openapi/documentation.yaml`
 * и описывает все маршруты API, схемы DTO и коды ответов.
 */
fun Application.configureDocumentation() {
    routing {
        swaggerUI(path = "swagger", swaggerFile = "openapi/documentation.yaml")
        openAPI(path = "openapi", swaggerFile = "openapi/documentation.yaml")
    }
}
