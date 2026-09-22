package com.example.taskapi.routes

import com.example.taskapi.error.InvalidParameterException
import io.ktor.server.application.ApplicationCall

/**
 * Разбор параметров пути и query-строки.
 *
 * Любое некорректное значение превращается в [InvalidParameterException],
 * который StatusPages отдаёт клиенту как 400 Bad Request с понятным текстом.
 */

/** Обязательный числовой параметр пути, например `/api/tasks/{id}`. */
fun ApplicationCall.pathInt(name: String): Int {
    val raw = parameters[name]
        ?: throw InvalidParameterException("Параметр пути '$name' обязателен")
    return raw.toIntOrNull()
        ?: throw InvalidParameterException("Параметр пути '$name' должен быть целым числом, получено '$raw'")
}

/** Числовой query-параметр со значением по умолчанию и проверкой диапазона. */
fun ApplicationCall.queryInt(
    name: String,
    default: Int,
    min: Int = Int.MIN_VALUE,
    max: Int = Int.MAX_VALUE,
): Int {
    val raw = request.queryParameters[name]?.trim()
    if (raw.isNullOrEmpty()) return default

    val value = raw.toIntOrNull()
        ?: throw InvalidParameterException("Query-параметр '$name' должен быть целым числом, получено '$raw'")
    if (value < min || value > max) {
        throw InvalidParameterException("Query-параметр '$name' должен быть в диапазоне от $min до $max, получено $value")
    }
    return value
}

/** Необязательный строковый query-параметр; пустая строка трактуется как отсутствие. */
fun ApplicationCall.queryString(name: String): String? =
    request.queryParameters[name]?.trim()?.ifEmpty { null }

/** Необязательный enum-параметр query-строки (регистр не важен). */
inline fun <reified E : Enum<E>> ApplicationCall.queryEnum(name: String): E? {
    val raw = queryString(name) ?: return null
    return parseEnum<E>(name, raw)
}

/** Обязательный enum-параметр пути, например `/api/tasks/status/{status}`. */
inline fun <reified E : Enum<E>> ApplicationCall.pathEnum(name: String): E {
    val raw = parameters[name]?.trim()?.ifEmpty { null }
        ?: throw InvalidParameterException("Параметр пути '$name' обязателен")
    return parseEnum<E>(name, raw)
}

/** Общая часть разбора enum: понятная ошибка со списком допустимых значений. */
inline fun <reified E : Enum<E>> parseEnum(name: String, raw: String): E =
    enumValues<E>().firstOrNull { it.name.equals(raw, ignoreCase = true) }
        ?: throw InvalidParameterException(
            "Недопустимое значение '$raw' для параметра '$name'. " +
                "Допустимые значения: ${enumValues<E>().joinToString(", ") { it.name }}",
        )
