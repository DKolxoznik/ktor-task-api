package com.example.taskapi.plugins

import io.ktor.http.HttpHeaders
import io.ktor.server.application.Application
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.callid.callIdMdc
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.defaultheaders.DefaultHeaders
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.util.AttributeKey
import org.slf4j.event.Level
import java.util.UUID

private val RequestStartTimeKey = AttributeKey<Long>("RequestStartTime")

/**
 * Собственный middleware (КТ-3): засекает время запроса и пишет
 * заголовок `X-Response-Time` до отправки тела ответа.
 */
val RequestTimingPlugin = createApplicationPlugin(name = "RequestTiming") {
    onCall { call ->
        call.attributes.put(RequestStartTimeKey, System.currentTimeMillis())
    }
    onCallRespond { call, _ ->
        val started = call.attributes.getOrNull(RequestStartTimeKey) ?: return@onCallRespond
        val elapsed = System.currentTimeMillis() - started
        call.response.headers.append("X-Response-Time", "${elapsed}ms")
        call.application.log.debug(
            "middleware: {} {} занял {} мс",
            call.request.httpMethod.value,
            call.request.path(),
            elapsed,
        )
    }
}

/**
 * Middleware-слой: CallId, CallLogging (логирование каждого запроса) и RequestTiming.
 */
fun Application.configureMonitoring() {
    install(CallId) {
        header(HttpHeaders.XRequestId)
        generate { UUID.randomUUID().toString() }
        verify { it.isNotBlank() }
    }

    install(CallLogging) {
        level = Level.INFO
        callIdMdc("callId")
        format { call ->
            val status = call.response.status()?.value ?: "unhandled"
            val started = call.attributes.getOrNull(RequestStartTimeKey)
            val duration = if (started != null) " ${System.currentTimeMillis() - started}ms" else ""
            "${call.request.httpMethod.value} ${call.request.path()} -> $status$duration"
        }
    }

    install(RequestTimingPlugin)

    install(DefaultHeaders) {
        header("X-Service", "ktor-task-api")
    }

    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Accept)
        allowHeader(HttpHeaders.Authorization)
        exposeHeader("X-Response-Time")
        exposeHeader(HttpHeaders.XRequestId)
        allowMethod(io.ktor.http.HttpMethod.Get)
        allowMethod(io.ktor.http.HttpMethod.Post)
        allowMethod(io.ktor.http.HttpMethod.Put)
        allowMethod(io.ktor.http.HttpMethod.Delete)
        allowMethod(io.ktor.http.HttpMethod.Options)
    }
}
