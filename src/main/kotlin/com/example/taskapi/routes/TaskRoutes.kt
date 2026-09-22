package com.example.taskapi.routes

import com.example.taskapi.error.InvalidParameterException
import com.example.taskapi.model.CreateTaskRequest
import com.example.taskapi.model.TaskPriority
import com.example.taskapi.model.TaskStatus
import com.example.taskapi.model.UpdateTaskRequest
import com.example.taskapi.service.SortField
import com.example.taskapi.service.SortOrder
import com.example.taskapi.service.TaskQuery
import com.example.taskapi.service.TaskService
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

/**
 * REST-маршруты для работы с задачами.
 *
 *  GET    /api/tasks                 — список с фильтрами, сортировкой и пагинацией (query-параметры)
 *  GET    /api/tasks/{id}            — одна задача по id (path-параметр)
 *  GET    /api/tasks/status/{status} — задачи с конкретным статусом (enum в path)
 *  POST   /api/tasks                 — создание задачи
 *  PUT    /api/tasks/{id}            — частичное обновление задачи
 *  DELETE /api/tasks/{id}            — удаление одной задачи
 *  DELETE /api/tasks?status=DONE     — массовое удаление по статусу (query-параметр)
 */
fun Route.taskRoutes(service: TaskService) {
    route("/api/tasks") {

        // GET /api/tasks?status=TODO&priority=HIGH&q=ktor&page=1&size=10&sort=TITLE&order=DESC
        get {
            val query = TaskQuery(
                status = call.queryEnum<TaskStatus>("status"),
                priority = call.queryEnum<TaskPriority>("priority"),
                search = call.queryString("q"),
                page = call.queryInt("page", default = 1, min = 1),
                size = call.queryInt("size", default = 20, min = 1, max = TaskService.MAX_PAGE_SIZE),
                sort = call.queryEnum<SortField>("sort") ?: SortField.ID,
                order = call.queryEnum<SortOrder>("order") ?: SortOrder.ASC,
            )
            call.respond(HttpStatusCode.OK, service.list(query))
        }

        // GET /api/tasks/42
        get("/{id}") {
            val id = call.pathInt("id")
            call.respond(HttpStatusCode.OK, service.getById(id))
        }

        // GET /api/tasks/status/IN_PROGRESS
        get("/status/{status}") {
            val status = call.pathEnum<TaskStatus>("status")
            val page = call.queryInt("page", default = 1, min = 1)
            val size = call.queryInt("size", default = 20, min = 1, max = TaskService.MAX_PAGE_SIZE)
            call.respond(HttpStatusCode.OK, service.list(TaskQuery(status = status, page = page, size = size)))
        }

        // POST /api/tasks
        post {
            val request = call.receive<CreateTaskRequest>()
            val created = service.create(request)
            call.response.header(HttpHeaders.Location, "/api/tasks/${created.id}")
            call.respond(HttpStatusCode.Created, created)
        }

        // PUT /api/tasks/42
        put("/{id}") {
            val id = call.pathInt("id")
            val request = call.receive<UpdateTaskRequest>()
            call.respond(HttpStatusCode.OK, service.update(id, request))
        }

        // DELETE /api/tasks/42 -> 204 No Content
        delete("/{id}") {
            val id = call.pathInt("id")
            service.delete(id)
            call.respond(HttpStatusCode.NoContent)
        }

        // DELETE /api/tasks?status=DONE -> 200 с количеством удалённых
        delete {
            val status = call.queryEnum<TaskStatus>("status")
                ?: throw InvalidParameterException(
                    "Для массового удаления укажите query-параметр 'status', например /api/tasks?status=DONE",
                )
            call.respond(HttpStatusCode.OK, service.deleteByStatus(status))
        }
    }
}
