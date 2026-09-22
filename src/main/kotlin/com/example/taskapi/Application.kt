package com.example.taskapi

import com.example.taskapi.plugins.configureMonitoring
import com.example.taskapi.plugins.configureRouting
import com.example.taskapi.plugins.configureSerialization
import com.example.taskapi.plugins.configureStatusPages
import com.example.taskapi.repository.TaskRepository
import com.example.taskapi.service.TaskService
import io.ktor.server.application.Application
import io.ktor.server.application.log
import io.ktor.server.netty.EngineMain

/**
 * Точка входа. EngineMain поднимает Netty и читает настройки
 * (порт, хост, список модулей) из `src/main/resources/application.yaml`.
 */
fun main(args: Array<String>) = EngineMain.main(args)

/**
 * Модуль приложения: собирает зависимости и подключает плагины Ktor.
 * Имя модуля указано в application.yaml, поэтому Ktor вызывает его сам.
 */
fun Application.module() {
    val version = environment.config.propertyOrNull("app.version")?.getString() ?: "1.0.0"
    val service = TaskService(TaskRepository())

    configureSerialization()
    configureMonitoring()
    configureStatusPages()
    configureRouting(service, version)

    log.info("ktor-task-api $version запущен, задач в хранилище: ${service.count()}")
}
