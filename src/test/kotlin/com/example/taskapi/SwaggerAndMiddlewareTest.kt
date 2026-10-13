package com.example.taskapi

import com.example.taskapi.model.ErrorResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private fun ApplicationTestBuilder.startApp(): HttpClient {
    environment {
        config = MapApplicationConfig(
            "jwt.secret" to "test-secret-key-for-unit-tests",
            "jwt.issuer" to "ktor-task-api",
            "jwt.audience" to "ktor-task-api-users",
            "jwt.realm" to "ktor-task-api",
            "jwt.expiresInSeconds" to "3600",
            "app.version" to "3.0.0",
        )
    }
    application { module() }
    return createClient {
        install(ContentNegotiation) { json() }
    }
}

/**
 * Тесты КТ-3: Swagger/OpenAPI доступны, StatusPages отдаёт единый JSON,
 * middleware выставляет служебные заголовки.
 */
class SwaggerAndMiddlewareTest {

    @Test
    fun `swagger UI is available`() = testApplication {
        val http = startApp()
        val response = http.get("/swagger")

        assertTrue(
            response.status == HttpStatusCode.OK || response.status == HttpStatusCode.Found,
            "Swagger должен отвечать 200 или редиректом, получено ${response.status}",
        )
        val body = response.bodyAsText()
        // HTML Swagger UI либо редирект на index.html
        assertTrue(
            body.contains("swagger", ignoreCase = true) ||
                response.headers["Location"]?.contains("swagger") == true ||
                response.status == HttpStatusCode.Found,
        )
    }

    @Test
    fun `openapi specification is available and documents key paths`() = testApplication {
        val http = startApp()
        val response = http.get("/openapi")

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("openapi") || body.contains("swagger"))
        assertTrue(body.contains("/api/tasks"))
        assertTrue(body.contains("/api/auth/login"))
        assertTrue(body.contains("/api/auth/register"))
        assertTrue(body.contains("bearerAuth") || body.contains("Bearer"))
        assertTrue(body.contains("ErrorResponse") || body.contains("error"))
    }

    @Test
    fun `status pages return unified JSON error for 404`() = testApplication {
        val http = startApp()
        val response = http.get("/api/tasks/9999")

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertTrue(response.contentType()?.match(ContentType.Application.Json) == true)
        val error: ErrorResponse = response.body()
        assertEquals(404, error.status)
        assertNotNull(error.message)
        assertNotNull(error.timestamp)
        assertEquals("/api/tasks/9999", error.path)
    }

    @Test
    fun `status pages return unified JSON error for 400`() = testApplication {
        val http = startApp()
        val response = http.get("/api/tasks/not-a-number")

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val error: ErrorResponse = response.body()
        assertEquals(400, error.status)
        assertTrue(error.message.contains("id"))
    }

    @Test
    fun `status pages return unified JSON error for 401 on protected route`() = testApplication {
        val http = startApp()
        val response = http.post("/api/tasks") {
            contentType(ContentType.Application.Json)
            setBody("""{"title":"x"}""")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        val error: ErrorResponse = response.body()
        assertEquals(401, error.status)
    }

    @Test
    fun `middleware sets X-Response-Time and X-Service headers`() = testApplication {
        val http = startApp()
        val response = http.get("/health")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("ktor-task-api", response.headers["X-Service"])
        val timing = response.headers["X-Response-Time"]
        assertNotNull(timing, "middleware должен выставить X-Response-Time")
        assertTrue(timing.endsWith("ms"), "ожидался формат '<число>ms', получено '$timing'")
    }

    @Test
    fun `middleware assigns X-Request-Id`() = testApplication {
        val http = startApp()
        val response = http.get("/health")

        assertNotNull(response.headers["X-Request-Id"])
        assertTrue(response.headers["X-Request-Id"]!!.isNotBlank())
    }

    @Test
    fun `root lists swagger endpoint`() = testApplication {
        val http = startApp()
        val body = http.get("/").bodyAsText()

        assertTrue(body.contains("/swagger"))
        assertTrue(body.contains("/openapi"))
    }
}
