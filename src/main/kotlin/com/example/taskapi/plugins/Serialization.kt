package com.example.taskapi.plugins

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import kotlinx.serialization.json.Json

/**
 * ContentNegotiation + kotlinx.serialization.
 *
 * Плагин смотрит на заголовки `Content-Type` / `Accept` и сам решает,
 * каким конвертером разобрать тело запроса и сериализовать ответ.
 * Здесь зарегистрирован единственный конвертер — JSON.
 */
fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json(
            Json {
                prettyPrint = true          // читаемый JSON — удобно «потыкать» руками
                isLenient = false           // строгий парсинг входящего JSON
                ignoreUnknownKeys = true    // лишние поля в запросе не роняют сервер
                encodeDefaults = true       // поля со значениями по умолчанию попадают в ответ
                explicitNulls = false       // null-поля не засоряют ответ
            },
        )
    }
}
