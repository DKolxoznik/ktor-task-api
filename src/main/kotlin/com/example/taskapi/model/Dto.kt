package com.example.taskapi.model

import kotlinx.serialization.Serializable

/**
 * Тело запроса на создание задачи (POST /api/tasks).
 */
@Serializable
data class CreateTaskRequest(
    val title: String? = null,
    val description: String? = null,
    val status: TaskStatus = TaskStatus.TODO,
    val priority: TaskPriority = TaskPriority.MEDIUM,
)

/**
 * Тело запроса на полное обновление задачи (PUT /api/tasks/{id}).
 * Все поля опциональны — обновляются только переданные.
 */
@Serializable
data class UpdateTaskRequest(
    val title: String? = null,
    val description: String? = null,
    val status: TaskStatus? = null,
    val priority: TaskPriority? = null,
)

/**
 * Страница результатов для GET /api/tasks с пагинацией.
 */
@Serializable
data class PageResponse<T>(
    val items: List<T>,
    val page: Int,
    val size: Int,
    val totalItems: Int,
    val totalPages: Int,
)

/**
 * Единый формат тела ошибки для всех кодов 4xx/5xx.
 */
@Serializable
data class ErrorResponse(
    val status: Int,
    val error: String,
    val message: String,
    val details: List<String> = emptyList(),
    val path: String? = null,
    val timestamp: String,
)

/**
 * Результат массового удаления (DELETE /api/tasks?status=DONE).
 */
@Serializable
data class BulkDeleteResponse(
    val deleted: Int,
    val status: TaskStatus,
    val remaining: Int,
)

/**
 * Ответ health-check.
 */
@Serializable
data class HealthResponse(
    val status: String,
    val service: String,
    val version: String,
    val tasksInStorage: Int,
)

/**
 * Краткое описание API для корневого маршрута.
 */
@Serializable
data class ApiInfoResponse(
    val service: String,
    val version: String,
    val description: String,
    val endpoints: List<String>,
)
