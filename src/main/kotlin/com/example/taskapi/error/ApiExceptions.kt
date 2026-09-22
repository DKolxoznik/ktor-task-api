package com.example.taskapi.error

/**
 * Базовый класс прикладных ошибок. Каждый наследник маппится в StatusPages
 * на конкретный HTTP-статус, поэтому бизнес-код может просто бросать исключение.
 */
sealed class ApiException(message: String) : RuntimeException(message)

/** 404 — запрошенный ресурс не существует. */
class NotFoundException(message: String) : ApiException(message)

/** 400 — некорректный параметр пути или query-строки. */
class InvalidParameterException(message: String) : ApiException(message)

/** 422 — тело запроса разобрано, но не прошло бизнес-валидацию. */
class ValidationException(
    val details: List<String>,
    message: String = "Тело запроса не прошло валидацию",
) : ApiException(message)

/** 409 — конфликт состояния, например дубликат заголовка задачи. */
class ConflictException(message: String) : ApiException(message)
