package com.example.taskapi.service

import com.example.taskapi.error.ConflictException
import com.example.taskapi.error.NotFoundException
import com.example.taskapi.error.ValidationException
import com.example.taskapi.model.BulkDeleteResponse
import com.example.taskapi.model.CreateTaskRequest
import com.example.taskapi.model.PageResponse
import com.example.taskapi.model.Task
import com.example.taskapi.model.TaskPriority
import com.example.taskapi.model.TaskStatus
import com.example.taskapi.model.UpdateTaskRequest
import com.example.taskapi.repository.TaskRepository

/**
 * Параметры выборки, собранные из query-строки запроса GET /api/tasks.
 */
data class TaskQuery(
    val status: TaskStatus? = null,
    val priority: TaskPriority? = null,
    val search: String? = null,
    val page: Int = 1,
    val size: Int = 20,
    val sort: SortField = SortField.ID,
    val order: SortOrder = SortOrder.ASC,
)

enum class SortField { ID, TITLE, STATUS, PRIORITY, CREATED_AT }

enum class SortOrder { ASC, DESC }

/**
 * Бизнес-логика: валидация, фильтрация, сортировка и пагинация.
 * Слой маршрутов занимается только HTTP, а слой сервиса — правилами.
 */
class TaskService(private val repository: TaskRepository) {

    companion object {
        const val MAX_PAGE_SIZE = 100
        const val MAX_TITLE_LENGTH = 100
        const val MAX_DESCRIPTION_LENGTH = 500
    }

    fun list(query: TaskQuery): PageResponse<Task> {
        val filtered = repository.findAll()
            .filter { query.status == null || it.status == query.status }
            .filter { query.priority == null || it.priority == query.priority }
            .filter { task ->
                val term = query.search?.trim()?.lowercase()
                term.isNullOrEmpty() ||
                    task.title.lowercase().contains(term) ||
                    task.description?.lowercase()?.contains(term) == true
            }

        val comparator: Comparator<Task> = when (query.sort) {
            SortField.ID -> compareBy { it.id }
            SortField.TITLE -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.title }
            SortField.STATUS -> compareBy { it.status.ordinal }
            SortField.PRIORITY -> compareBy { it.priority.ordinal }
            SortField.CREATED_AT -> compareBy { it.createdAt }
        }
        val sorted = filtered.sortedWith(
            if (query.order == SortOrder.DESC) comparator.reversed() else comparator,
        )

        val totalItems = sorted.size
        val totalPages = if (totalItems == 0) 0 else (totalItems + query.size - 1) / query.size
        val items = sorted.drop((query.page - 1) * query.size).take(query.size)

        return PageResponse(
            items = items,
            page = query.page,
            size = query.size,
            totalItems = totalItems,
            totalPages = totalPages,
        )
    }

    fun getById(id: Int): Task =
        repository.findById(id) ?: throw NotFoundException("Задача с id=$id не найдена")

    fun create(request: CreateTaskRequest): Task {
        val title = request.title?.trim().orEmpty()
        val details = buildList {
            if (title.isEmpty()) add("Поле 'title' обязательно и не может быть пустым")
            if (title.length > MAX_TITLE_LENGTH) add("Поле 'title' не может быть длиннее $MAX_TITLE_LENGTH символов")
            val descriptionLength = request.description?.length ?: 0
            if (descriptionLength > MAX_DESCRIPTION_LENGTH) {
                add("Поле 'description' не может быть длиннее $MAX_DESCRIPTION_LENGTH символов")
            }
        }
        if (details.isNotEmpty()) throw ValidationException(details)

        if (repository.existsByTitle(title)) {
            throw ConflictException("Задача с заголовком '$title' уже существует")
        }

        return repository.create(
            title = title,
            description = request.description?.trim()?.ifEmpty { null },
            status = request.status,
            priority = request.priority,
        )
    }

    fun update(id: Int, request: UpdateTaskRequest): Task {
        val existing = getById(id)

        val newTitle = request.title?.trim()
        val details = buildList {
            if (newTitle != null && newTitle.isEmpty()) add("Поле 'title' не может быть пустым")
            if ((newTitle?.length ?: 0) > MAX_TITLE_LENGTH) {
                add("Поле 'title' не может быть длиннее $MAX_TITLE_LENGTH символов")
            }
            if ((request.description?.length ?: 0) > MAX_DESCRIPTION_LENGTH) {
                add("Поле 'description' не может быть длиннее $MAX_DESCRIPTION_LENGTH символов")
            }
        }
        if (details.isNotEmpty()) throw ValidationException(details)

        if (newTitle != null && repository.existsByTitle(newTitle, exceptId = id)) {
            throw ConflictException("Задача с заголовком '$newTitle' уже существует")
        }

        return repository.update(
            existing.copy(
                title = newTitle ?: existing.title,
                description = request.description ?: existing.description,
                status = request.status ?: existing.status,
                priority = request.priority ?: existing.priority,
            ),
        )
    }

    fun delete(id: Int) {
        if (!repository.deleteById(id)) {
            throw NotFoundException("Задача с id=$id не найдена, удалять нечего")
        }
    }

    fun deleteByStatus(status: TaskStatus): BulkDeleteResponse {
        val deleted = repository.deleteByStatus(status)
        return BulkDeleteResponse(deleted = deleted, status = status, remaining = repository.count())
    }

    fun count(): Int = repository.count()
}
