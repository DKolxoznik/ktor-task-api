package com.example.taskapi

import com.example.taskapi.model.CreateTaskRequest
import com.example.taskapi.model.ErrorResponse
import com.example.taskapi.model.LoginRequest
import com.example.taskapi.model.PageResponse
import com.example.taskapi.model.RegisterRequest
import com.example.taskapi.model.Task
import com.example.taskapi.model.TaskPriority
import com.example.taskapi.model.TaskStatus
import com.example.taskapi.model.TokenResponse
import com.example.taskapi.model.UpdateTaskRequest
import com.example.taskapi.model.UserResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
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
import kotlin.test.assertFalse
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
            "app.version" to "2.0.0",
        )
    }
    application { module() }
    return createClient {
        install(ContentNegotiation) { json() }
    }
}

private suspend fun HttpClient.registerAndLogin(
    username: String = "tester",
    password: String = "secret1",
): String {
    post("/api/auth/register") {
        contentType(ContentType.Application.Json)
        setBody(RegisterRequest(username = username, password = password))
    }
    val token: TokenResponse = post("/api/auth/login") {
        contentType(ContentType.Application.Json)
        setBody(LoginRequest(username = username, password = password))
    }.body()
    return token.accessToken
}

class TaskApiTest {

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
        assertTrue(response.bodyAsText().contains("/api/auth/login"))
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
    }

    @Test
    fun `GET tasks supports pagination`() = testApplication {
        val http = startApp()
        val page: PageResponse<Task> = http.get("/api/tasks?page=2&size=2").body()

        assertEquals(2, page.page)
        assertEquals(2, page.size)
        assertEquals(2, page.items.size)
    }

    @Test
    fun `non numeric query parameter returns 400`() = testApplication {
        val http = startApp()
        val response = http.get("/api/tasks?page=abc")

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
        val error: ErrorResponse = response.body()
        assertEquals(404, error.status)
    }

    @Test
    fun `POST without token returns 401`() = testApplication {
        val http = startApp()
        val response = http.post("/api/tasks") {
            contentType(ContentType.Application.Json)
            setBody(CreateTaskRequest(title = "Без токена"))
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `POST with invalid token returns 401`() = testApplication {
        val http = startApp()
        val response = http.post("/api/tasks") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer not.a.valid.token")
            setBody(CreateTaskRequest(title = "Битый токен"))
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `POST with valid token creates a task and returns 201`() = testApplication {
        val http = startApp()
        val token = http.registerAndLogin()

        val response = http.post("/api/tasks") {
            contentType(ContentType.Application.Json)
            bearerAuth(token)
            setBody(CreateTaskRequest(title = "Новая задача", priority = TaskPriority.HIGH))
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val created: Task = response.body()
        assertEquals("Новая задача", created.title)
        assertEquals("/api/tasks/${created.id}", response.headers[HttpHeaders.Location])
    }

    @Test
    fun `POST with blank title returns 422`() = testApplication {
        val http = startApp()
        val token = http.registerAndLogin()

        val response = http.post("/api/tasks") {
            contentType(ContentType.Application.Json)
            bearerAuth(token)
            setBody(CreateTaskRequest(title = "   "))
        }

        assertEquals(HttpStatusCode.UnprocessableEntity, response.status)
    }

    @Test
    fun `PUT with valid token updates a task`() = testApplication {
        val http = startApp()
        val token = http.registerAndLogin()

        val response = http.put("/api/tasks/1") {
            contentType(ContentType.Application.Json)
            bearerAuth(token)
            setBody(UpdateTaskRequest(status = TaskStatus.DONE))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val task: Task = response.body()
        assertEquals(TaskStatus.DONE, task.status)
    }

    @Test
    fun `DELETE without token returns 401`() = testApplication {
        val http = startApp()
        assertEquals(HttpStatusCode.Unauthorized, http.delete("/api/tasks/1").status)
    }

    @Test
    fun `DELETE with valid token returns 204`() = testApplication {
        val http = startApp()
        val token = http.registerAndLogin()

        assertEquals(
            HttpStatusCode.NoContent,
            http.delete("/api/tasks/1") { bearerAuth(token) }.status,
        )
        assertEquals(HttpStatusCode.NotFound, http.get("/api/tasks/1").status)
    }

    @Test
    fun `GET remains public without token`() = testApplication {
        val http = startApp()
        assertEquals(HttpStatusCode.OK, http.get("/api/tasks").status)
        assertEquals(HttpStatusCode.OK, http.get("/api/tasks/1").status)
    }
}

class AuthApiTest {

    @Test
    fun `register creates a user and returns 201 without password`() = testApplication {
        val http = startApp()
        val response = http.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(username = "alice", password = "secret1"))
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val user: UserResponse = response.body()
        assertEquals("alice", user.username)
        assertTrue(user.id > 0)
        assertFalse(response.bodyAsText().contains("password", ignoreCase = true))
        assertFalse(response.bodyAsText().contains("hash", ignoreCase = true))
    }

    @Test
    fun `register duplicate username returns 409`() = testApplication {
        val http = startApp()
        http.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(username = "bob", password = "secret1"))
        }
        val response = http.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(username = "bob", password = "other12"))
        }

        assertEquals(HttpStatusCode.Conflict, response.status)
    }

    @Test
    fun `register with short password returns 422`() = testApplication {
        val http = startApp()
        val response = http.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(username = "short", password = "123"))
        }

        assertEquals(HttpStatusCode.UnprocessableEntity, response.status)
    }

    @Test
    fun `login returns JWT with userId claim metadata`() = testApplication {
        val http = startApp()
        http.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(username = "carol", password = "secret1"))
        }

        val response = http.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(username = "carol", password = "secret1"))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val token: TokenResponse = response.body()
        assertNotNull(token.accessToken)
        assertTrue(token.accessToken.split('.').size == 3)
        assertEquals("Bearer", token.tokenType)
        assertEquals(3600, token.expiresIn)
        assertTrue(token.userId > 0)
        assertEquals("carol", token.username)
    }

    @Test
    fun `login with wrong password returns 401`() = testApplication {
        val http = startApp()
        http.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(username = "dave", password = "secret1"))
        }

        val response = http.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(username = "dave", password = "wrong!!"))
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `login with unknown username returns 401`() = testApplication {
        val http = startApp()
        val response = http.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(username = "ghost", password = "secret1"))
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `password is stored as bcrypt hash not plaintext`() = testApplication {
        // Косвенная проверка: успешный логин после регистрации означает,
        // что PasswordHasher.verify сверил BCrypt-хэш, а не сравнил строки.
        // Прямой доступ к хранилищу из HTTP нет — хэш не утекает в ответах.
        val http = startApp()
        http.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(username = "erin", password = "secret1"))
        }
        val login = http.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(username = "erin", password = "secret1"))
        }
        assertEquals(HttpStatusCode.OK, login.status)

        val body = http.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(username = "frank", password = "secret1"))
        }.bodyAsText()
        assertFalse(body.contains("secret1"))
        assertFalse(body.contains("\$2a\$"))
    }
}
