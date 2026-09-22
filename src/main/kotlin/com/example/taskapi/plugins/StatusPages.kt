package com.example.taskapi.plugins

import com.example.taskapi.error.ConflictException
import com.example.taskapi.error.InvalidParameterException
import com.example.taskapi.error.NotFoundException
import com.example.taskapi.error.ValidationException
import com.example.taskapi.model.ErrorResponse
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.JsonConvertException
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.CannotTransformContentToTypeException
import io.ktor.server.plugins.UnsupportedMediaTypeException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.path
import io.ktor.server.response.respond
import java.time.Instant

/**
 * Единая точка превращения исключений в JSON-ответы с корректными HTTP-статусами.
 * Благодаря этому маршруты не содержат try/catch, а клиент всегда получает
 * предсказуемое тело ошибки.
 */
fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<NotFoundException> { call, cause ->
            call.respondError(HttpStatusCode.NotFound, cause.message)
        }

        exception<InvalidParameterException> { call, cause ->
            call.respondError(HttpStatusCode.BadRequest, cause.message)
        }

        exception<ValidationException> { call, cause ->
            call.respondError(HttpStatusCode.UnprocessableEntity, cause.message, cause.details)
        }

        exception<ConflictException> { call, cause ->
            call.respondError(HttpStatusCode.Conflict, cause.message)
        }

        // Тело пришло без Content-Type или с типом, для которого нет конвертера.
        exception<UnsupportedMediaTypeException> { call, _ ->
            call.respondError(
                HttpStatusCode.UnsupportedMediaType,
                "Требуется заголовок Content-Type: application/json",
            )
        }

        exception<CannotTransformContentToTypeException> { call, _ ->
            call.respondError(
                HttpStatusCode.UnsupportedMediaType,
                "Требуется заголовок Content-Type: application/json",
            )
        }

        // Некорректный JSON в теле запроса или несовпадение типов полей.
        exception<JsonConvertException> { call, cause ->
            call.respondError(
                HttpStatusCode.BadRequest,
                "Тело запроса не является корректным JSON",
                listOfNotNull(cause.message),
            )
        }

        // Ktor оборачивает ошибки разбора тела и параметров в BadRequestException.
        exception<BadRequestException> { call, cause ->
            call.respondError(
                HttpStatusCode.BadRequest,
                "Некорректный запрос",
                listOfNotNull(cause.cause?.message ?: cause.message),
            )
        }

        exception<Throwable> { call, cause ->
            call.application.log.error("Необработанная ошибка на ${call.request.path()}", cause)
            call.respondError(
                HttpStatusCode.InternalServerError,
                "Внутренняя ошибка сервера",
            )
        }
    }
}

/**
 * Отправляет тело ошибки в едином формате [ErrorResponse].
 */
suspend fun ApplicationCall.respondError(
    status: HttpStatusCode,
    message: String?,
    details: List<String> = emptyList(),
) {
    respond(
        status,
        ErrorResponse(
            status = status.value,
            error = status.description,
            message = message ?: status.description,
            details = details,
            path = request.path(),
            timestamp = Instant.now().toString(),
        ),
    )
}
