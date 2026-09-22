package com.example.taskapi

import com.example.taskapi.model.CreateTaskRequest
import com.example.taskapi.model.ErrorResponse
import com.example.taskapi.model.PageResponse
import com.example.taskapi.model.Task
import com.example.taskapi.model.TaskPriority
import com.example.taskapi.model.TaskStatus
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
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

/**
 * Тесты проверяют главное по критериям КТ-1: маршруты, формат JSON,
 * разбор параметров и корректность HTTP-статусов.
 *
 * Каждый testApplication поднимает изолированный экземпляр сервера
 * со своим in-memory хранилищем, поэтому тесты не влияют друг на друга.
 */
class TaskApiTest {

    /** Поднимает приложение и возвращает клиент, умеющий читать и писать JSON. */
    private fun ApplicationTestBuilder.startApp(): HttpClient {
        environment { config = MapApplicationConfig() }
        application { module() }
        return createClient {
            install(ContentNegotiation) { json() }
        }
    }

    @Test
    fun `health endpoint returns 200 and JSON`() = testApplication {
        val http = startApp()
        val response = http.get("/health")

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.contentType()?.match(ContentType.Application.Json) == true)
        assertTrue(response.bodyAsText().contains("\"status\""))
    }

    @Test
    fun `root endpoint lists available endpoints`() = testApplication {
        val http = startApp()
        val response = http.get("/")

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("/api/tasks"))
    }

    @Test
    fun `GET tasks returns a page of tasks`() = testApplication {
        val http = startApp()
        val page: PageResponse<Task> = http.get("/api/tasks").body()

        assertEquals(1, page.page)
        assertEquals(5, page.totalItems)
        assertTrue(page.items.isNotEmpty())
    }

    @Test
    fun `GET tasks filters by status query parameter`() = testApplication {
        val http = startApp()
        val page: PageResponse<Task> = http.get("/api/tasks?status=DONE").body()

        assertTrue(page.items.isNotEmpty())
        assertTrue(page.items.all { it.status == TaskStatus.DONE })
        assertEquals(page.items.size, page.totalItems)
    }

    @Test
    fun `GET tasks searches by q query parameter`() = testApplication {
        val http = startApp()
        val page: PageResponse<Task> = http.get("/api/tasks?q=docker").body()

        assertEquals(1, page.totalItems)
        assertTrue(page.items.single().title.contains("Docker"))
    }

    @Test
    fun `GET tasks supports pagination`() = testApplication {
        val http = startApp()
        val page: PageResponse<Task> = http.get("/api/tasks?page=2&size=2").body()

        assertEquals(2, page.page)
        assertEquals(2, page.size)
        assertEquals(3, page.totalPages)
        assertEquals(2, page.items.size)
    }

    @Test
    fun `GET tasks supports sorting`() = testApplication {
        val http = startApp()
        val page: PageResponse<Task> = http.get("/api/tasks?sort=ID&order=DESC").body()

        assertEquals(5, page.items.first().id)
    }

    @Test
    fun `non numeric query parameter returns 400`() = testApplication {
        val http = startApp()
        val response = http.get("/api/tasks?page=abc")

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val error: ErrorResponse = response.body()
        assertEquals(400, error.status)
        assertTrue(error.message.contains("page"))
    }

    @Test
    fun `unknown enum value in query returns 400`() = testApplication {
        val http = startApp()
        val response = http.get("/api/tasks?status=UNKNOWN_STATUS")

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val error: ErrorResponse = response.body()
        assertTrue(error.message.contains("IN_PROGRESS"))
    }

    @Test
    fun `page size above the limit returns 400`() = testApplication {
        val http = startApp()
        val response = http.get("/api/tasks?size=1000")

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `GET task by existing id returns 200`() = testApplication {
        val http = startApp()
        val task: Task = http.get("/api/tasks/1").body()

        assertEquals(1, task.id)
    }

    @Test
    fun `GET task by missing id returns 404 as JSON`() = testApplication {
        val http = startApp()
        val response = http.get("/api/tasks/9999")

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertTrue(response.contentType()?.match(ContentType.Application.Json) == true)
        val error: ErrorResponse = response.body()
        assertEquals(404, error.status)
    }

    @Test
    fun `non numeric path parameter returns 400`() = testApplication {
        val http = startApp()
        val response = http.get("/api/tasks/not-a-number")

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `GET tasks by status path parameter returns only that status`() = testApplication {
        val http = startApp()
        val page: PageResponse<Task> = http.get("/api/tasks/status/IN_PROGRESS").body()

        assertTrue(page.items.isNotEmpty())
        assertTrue(page.items.all { it.status == TaskStatus.IN_PROGRESS })
    }

    @Test
    fun `POST creates a task and returns 201 with Location header`() = testApplication {
        val http = startApp()
        val response = http.post("/api/tasks") {
            contentType(ContentType.Application.Json)
            setBody(CreateTaskRequest(title = "Новая задача", priority = TaskPriority.HIGH))
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val created: Task = response.body()
        assertEquals("Новая задача", created.title)
        assertEquals(TaskPriority.HIGH, created.priority)
        assertEquals("/api/tasks/${created.id}", response.headers[HttpHeaders.Location])
    }

    @Test
    fun `POST with blank title returns 422 with validation details`() = testApplication {
        val http = startApp()
        val response = http.post("/api/tasks") {
            contentType(ContentType.Application.Json)
            setBody(CreateTaskRequest(title = "   "))
        }

        assertEquals(HttpStatusCode.UnprocessableEntity, response.status)
        val error: ErrorResponse = response.body()
        assertTrue(error.details.isNotEmpty())
    }

    @Test
    fun `POST with duplicate title returns 409`() = testApplication {
        val http = startApp()
        val response = http.post("/api/tasks") {
            contentType(ContentType.Application.Json)
            setBody(CreateTaskRequest(title = "Изучить Ktor"))
        }

        assertEquals(HttpStatusCode.Conflict, response.status)
    }

    @Test
    fun `POST with malformed JSON returns 400`() = testApplication {
        val http = startApp()
        val response = http.post("/api/tasks") {
            contentType(ContentType.Application.Json)
            setBody("{ this is not json }")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `POST without Content-Type returns 415`() = testApplication {
        val http = startApp()
        val response = http.post("/api/tasks") {
            setBody("{\"title\":\"Без типа\"}")
        }

        assertEquals(HttpStatusCode.UnsupportedMediaType, response.status)
    }

    @Test
    fun `DELETE existing task returns 204 and the task disappears`() = testApplication {
        val http = startApp()

        assertEquals(HttpStatusCode.NoContent, http.delete("/api/tasks/1").status)
        assertEquals(HttpStatusCode.NotFound, http.get("/api/tasks/1").status)
    }

    @Test
    fun `DELETE missing task returns 404`() = testApplication {
        val http = startApp()
        val response = http.delete("/api/tasks/9999")

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `DELETE without required status query parameter returns 400`() = testApplication {
        val http = startApp()
        val response = http.delete("/api/tasks")

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `DELETE by status removes the whole group`() = testApplication {
        val http = startApp()
        val before: PageResponse<Task> = http.get("/api/tasks?status=TODO").body()
        val response = http.delete("/api/tasks?status=TODO")

        assertEquals(HttpStatusCode.OK, response.status)
        val after: PageResponse<Task> = http.get("/api/tasks?status=TODO").body()
        assertTrue(before.totalItems > 0)
        assertEquals(0, after.totalItems)
    }

    @Test
    fun `unknown route returns 404 as JSON`() = testApplication {
        val http = startApp()
        val response = http.get("/api/unknown")

        assertEquals(HttpStatusCode.NotFound, response.status)
        val error: ErrorResponse = response.body()
        assertEquals(404, error.status)
        assertNotNull(error.path)
    }
}
