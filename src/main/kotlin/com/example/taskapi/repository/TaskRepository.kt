package com.example.taskapi.repository

import com.example.taskapi.model.Task
import com.example.taskapi.model.TaskPriority
import com.example.taskapi.model.TaskStatus
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Хранилище задач в памяти. Для учебного проекта этого достаточно:
 * при перезапуске сервера данные сбрасываются к начальному набору.
 *
 * ConcurrentHashMap + AtomicInteger — потому что Ktor обрабатывает
 * запросы параллельно в нескольких потоках.
 */
class TaskRepository {

    private val storage = ConcurrentHashMap<Int, Task>()
    private val idSequence = AtomicInteger(0)

    init {
        seed()
    }

    fun findAll(): List<Task> = storage.values.sortedBy { it.id }

    fun findById(id: Int): Task? = storage[id]

    fun existsByTitle(title: String, exceptId: Int? = null): Boolean =
        storage.values.any { it.id != exceptId && it.title.equals(title, ignoreCase = true) }

    fun create(
        title: String,
        description: String?,
        status: TaskStatus,
        priority: TaskPriority,
    ): Task {
        val now = Instant.now().toString()
        val task = Task(
            id = idSequence.incrementAndGet(),
            title = title,
            description = description,
            status = status,
            priority = priority,
            createdAt = now,
            updatedAt = now,
        )
        storage[task.id] = task
        return task
    }

    fun update(task: Task): Task {
        val updated = task.copy(updatedAt = Instant.now().toString())
        storage[updated.id] = updated
        return updated
    }

    fun deleteById(id: Int): Boolean = storage.remove(id) != null

    fun deleteByStatus(status: TaskStatus): Int {
        val ids = storage.values.filter { it.status == status }.map { it.id }
        ids.forEach { storage.remove(it) }
        return ids.size
    }

    fun count(): Int = storage.size

    private fun seed() {
        create("Изучить Ktor", "Разобраться с маршрутизацией и плагинами", TaskStatus.DONE, TaskPriority.HIGH)
        create("Подключить kotlinx.serialization", "ContentNegotiation + JSON", TaskStatus.IN_PROGRESS, TaskPriority.HIGH)
        create("Написать REST-маршруты", "GET, POST, DELETE", TaskStatus.IN_PROGRESS, TaskPriority.MEDIUM)
        create("Добавить обработку ошибок", "StatusPages и корректные HTTP-коды", TaskStatus.TODO, TaskPriority.MEDIUM)
        create("Собрать Docker-образ", "Multi-stage build на JDK 17", TaskStatus.TODO, TaskPriority.LOW)
    }
}
