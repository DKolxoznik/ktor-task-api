package com.example.taskapi

import com.example.taskapi.plugins.configureMonitoring
import com.example.taskapi.plugins.configureRouting
import com.example.taskapi.plugins.configureSecurity
import com.example.taskapi.plugins.configureSerialization
import com.example.taskapi.plugins.configureStatusPages
import com.example.taskapi.repository.TaskRepository
import com.example.taskapi.repository.UserRepository
import com.example.taskapi.security.JwtConfig
import com.example.taskapi.security.JwtService
import com.example.taskapi.service.AuthService
import com.example.taskapi.service.TaskService
import io.ktor.server.application.Application
import io.ktor.server.application.log
import io.ktor.server.netty.EngineMain

fun main(args: Array<String>) = EngineMain.main(args)

fun Application.module() {
    val version = environment.config.propertyOrNull("app.version")?.getString() ?: "2.0.0"

    val jwtConfig = JwtConfig(
        secret = environment.config.property("jwt.secret").getString(),
        issuer = environment.config.property("jwt.issuer").getString(),
        audience = environment.config.property("jwt.audience").getString(),
        realm = environment.config.property("jwt.realm").getString(),
        expiresInSeconds = environment.config.property("jwt.expiresInSeconds").getString().toLong(),
    )

    val taskService = TaskService(TaskRepository())
    val authService = AuthService(UserRepository(), JwtService(jwtConfig))

    configureSerialization()
    configureMonitoring()
    configureStatusPages()
    configureSecurity(jwtConfig)
    configureRouting(taskService, authService, version)

    log.info("ktor-task-api $version запущен, задач в хранилище: ${taskService.count()}")
}
