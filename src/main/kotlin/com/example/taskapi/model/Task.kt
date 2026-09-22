package com.example.taskapi.model

import kotlinx.serialization.Serializable

/**
 * Статус задачи. Используется и в теле запроса, и как значение query-параметра `status`.
 */
enum class TaskStatus {
    TODO,
    IN_PROGRESS,
    DONE,
}

/**
 * Приоритет задачи.
 */
enum class TaskPriority {
    LOW,
    MEDIUM,
    HIGH,
}

/**
 * Доменная модель задачи. `@Serializable` позволяет kotlinx.serialization
 * автоматически превращать объект в JSON и обратно.
 */
@Serializable
data class Task(
    val id: Int,
    val title: String,
    val description: String? = null,
    val status: TaskStatus = TaskStatus.TODO,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val createdAt: String,
    val updatedAt: String,
)
