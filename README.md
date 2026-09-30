# ktor-task-api — REST API на Ktor (КТ-1 + КТ-2)

Учебный REST-сервис управления задачами на **Kotlin + Ktor**.

* **КТ-1:** маршруты `GET`, `POST`, `PUT`, `DELETE`, JSON через
  `ContentNegotiation` + `kotlinx.serialization`, параметры пути и query,
  осмысленные HTTP-статусы.
* **КТ-2:** регистрация с BCrypt-хэшем пароля, вход с выдачей JWT и защита
  маршрутов создания, обновления и удаления. Чтение остаётся публичным.

---

## Что такое Ktor и зачем он здесь

**Ktor** — это фреймворк от JetBrains (создателей Kotlin и IntelliJ IDEA) для
построения серверных и клиентских HTTP-приложений на Kotlin. Его ключевые
особенности:

* **Асинхронность на корутинах.** Ktor не блокирует поток на время ожидания
  ввода-вывода: каждый обработчик запроса — это `suspend`-функция. Одна и та же
  машина держит существенно больше одновременных соединений, чем классический
  блокирующий сервер, потому что потоки не простаивают.
* **Модульность вместо «всё включено».** В отличие от Spring Boot, Ktor по
  умолчанию не умеет почти ничего: сериализацию, логирование, CORS, аутентификацию
  и обработку ошибок вы подключаете как отдельные **плагины** через `install(...)`.
  Приложение получается лёгким, а зависимости — явными. В этом проекте
  подключены плагины `ContentNegotiation`, `StatusPages`, `CallLogging`,
  `CallId`, `DefaultHeaders`, `CORS` и `Authentication` (JWT).
* **Маршрутизация как Kotlin DSL.** Дерево маршрутов описывается вложенными
  блоками `route / get / post / delete`, без аннотаций и без рефлексии — это
  обычный Kotlin-код, который проверяет компилятор.
* **Сменный движок (engine).** Под капотом может работать Netty, CIO, Jetty или
  Tomcat; прикладной код при этом не меняется. Здесь используется **Netty**.
* **Ktor-приложение — это обычный `main()`.** Не нужен контейнер сервлетов и
  внешний сервер приложений: сервер поднимается прямо из процесса JVM, поэтому
  его удобно упаковывать в Docker-образ.

Коротко: Ktor — это лёгкий, явный и асинхронный способ написать HTTP-сервис на
Kotlin, когда тяжёлый «магический» фреймворк избыточен.

### Как устроен запрос в этом проекте

```
HTTP-запрос
   → CallId / CallLogging      (идентификатор запроса и лог)
   → ContentNegotiation        (JSON → data class и обратно)
   → Authentication (JWT)      (только POST / PUT / DELETE задач)
   → Routing                   (подбор маршрута, разбор path/query параметров)
   → TaskService / AuthService (валидация, BCrypt, выдача токена)
   → TaskRepository / UserRepository
   → StatusPages               (исключение → JSON-ошибка с нужным HTTP-кодом)
HTTP-ответ
```

---

## Стек

| Компонент | Версия |
|---|---|
| Kotlin | 2.2.20 |
| Ktor | 3.6.0 |
| Движок | Netty |
| JSON | kotlinx.serialization 1.9.0 |
| Сборка | Gradle 9.7.1 (Kotlin DSL, wrapper в репозитории) |
| JDK | 17 |
| Пароли | BCrypt (`at.favre.lib:bcrypt` 0.10.2) |
| JWT | Ktor Authentication + HMAC256 |
| Логи | Logback 1.5.18 |
| Тесты | JUnit 5 + `ktor-server-test-host` |

---

## Структура проекта

```
ktor-task-api/
├── build.gradle.kts              # зависимости и плагины Gradle
├── settings.gradle.kts
├── gradle.properties             # версии библиотек
├── gradlew / gradlew.bat         # Gradle wrapper (Gradle ставить не нужно)
├── Dockerfile                    # multi-stage сборка образа
├── docker-compose.yml
├── requests.http                 # готовые запросы для HTTP-клиента IDEA
├── .run/                         # готовая конфигурация запуска для IDEA
└── src
    ├── main
    │   ├── kotlin/com/example/taskapi
    │   │   ├── Application.kt            # точка входа и сборка модуля
    │   │   ├── plugins
    │   │   │   ├── Serialization.kt      # ContentNegotiation + kotlinx.serialization
    │   │   │   ├── Routing.kt            # регистрация маршрутов и catch-all 404
    │   │   │   ├── StatusPages.kt        # исключения → HTTP-статусы
    │   │   │   ├── Security.kt           # Authentication + JWT, claim userId
    │   │   │   └── Monitoring.kt         # логирование, CallId, CORS, заголовки
    │   │   ├── routes
    │   │   │   ├── TaskRoutes.kt         # CRUD задач (запись требует JWT)
    │   │   │   ├── AuthRoutes.kt         # регистрация и вход
    │   │   │   ├── SystemRoutes.kt       # / и /health
    │   │   │   └── RequestParams.kt      # разбор path- и query-параметров
    │   │   ├── service/                  # TaskService, AuthService
    │   │   ├── repository/               # TaskRepository, UserRepository
    │   │   ├── security/                 # PasswordHasher (BCrypt), JwtService
    │   │   ├── model/                    # задачи, пользователи, DTO запросов
    │   │   └── error/ApiExceptions.kt    # прикладные исключения
    │   └── resources
    │       ├── application.yaml          # порт, хост, модуль приложения
    │       └── logback.xml
    └── test/kotlin/com/example/taskapi/  # CRUD, JWT, BCrypt
```

---

## Запуск

### 1. IntelliJ IDEA 2025.3 (основной способ)

1. `File → Open…` → выбрать папку проекта → **Open as Project**.
2. Дождаться окончания синхронизации Gradle (IDEA сама скачает Gradle 9.7.1 и
   зависимости; интернет нужен только при первом запуске).
3. Убедиться, что в `File → Project Structure → Project` выбран **JDK 17**
   (или новее).
4. Запустить любым из способов:
   * готовая конфигурация **Run Ktor Server** в списке конфигураций сверху;
   * зелёная стрелка слева от функции `main()` в `Application.kt`;
   * панель Gradle → `Tasks → application → run`.
5. В консоли появится `Responding at http://0.0.0.0:8080`.
6. Открыть `requests.http` и «потыкать» API прямо из IDEA кнопкой ▶.

### 2. Командная строка

```bash
# Windows
gradlew.bat run

# Linux / macOS
./gradlew run
```

Тесты:

```bash
gradlew.bat test          # Windows
./gradlew test            # Linux / macOS
```

Сборка дистрибутива:

```bash
./gradlew installDist
build/install/ktor-task-api/bin/ktor-task-api
```

### 3. Docker

```bash
# сборка образа
docker build -t ktor-task-api:2.0.0 .

# запуск
docker run --rm -p 8080:8080 --name ktor-task-api ktor-task-api:2.0.0
```

Или одной командой через Compose:

```bash
docker compose up --build
```

Проверка:

```bash
curl http://localhost:8080/health
```

Порт меняется переменной окружения `PORT`:

```bash
docker run --rm -p 9090:9090 -e PORT=9090 ktor-task-api:2.0.0
```

---

## API

Базовый адрес: `http://localhost:8080`

| Метод | Путь | Описание | Успех |
|---|---|---|---|
| `GET` | `/` | информация об API и список маршрутов | `200` |
| `GET` | `/health` | health-check (используется в Docker HEALTHCHECK) | `200` |
| `GET` | `/api/tasks` | список задач с фильтрами, сортировкой и пагинацией | `200` |
| `GET` | `/api/tasks/{id}` | одна задача по идентификатору | `200` |
| `GET` | `/api/tasks/status/{status}` | задачи с указанным статусом | `200` |
| `POST` | `/api/auth/register` | регистрация (пароль хранится как BCrypt-хэш) | `201` |
| `POST` | `/api/auth/login` | вход, сверка хэша, выдача JWT | `200` |
| `POST` | `/api/tasks` | создать задачу, **нужен JWT** | `201` + заголовок `Location` |
| `PUT` | `/api/tasks/{id}` | обновить задачу, **нужен JWT** | `200` |
| `DELETE` | `/api/tasks/{id}` | удалить одну задачу, **нужен JWT** | `204` |
| `DELETE` | `/api/tasks?status={status}` | удалить все задачи со статусом, **нужен JWT** | `200` |

### Параметры пути (path)

| Параметр | Где | Тип | Поведение при ошибке |
|---|---|---|---|
| `id` | `/api/tasks/{id}` | целое число | не число → `400`, нет такой задачи → `404` |
| `status` | `/api/tasks/status/{status}` | `TODO` \| `IN_PROGRESS` \| `DONE` | неизвестное значение → `400` со списком допустимых |

### Параметры query-строки

| Параметр | Тип | По умолчанию | Описание |
|---|---|---|---|
| `status` | enum | — | фильтр по статусу: `TODO`, `IN_PROGRESS`, `DONE` |
| `priority` | enum | — | фильтр по приоритету: `LOW`, `MEDIUM`, `HIGH` |
| `q` | строка | — | поиск подстроки в `title` и `description` |
| `page` | целое ≥ 1 | `1` | номер страницы |
| `size` | целое 1..100 | `20` | размер страницы |
| `sort` | enum | `ID` | поле сортировки: `ID`, `TITLE`, `STATUS`, `PRIORITY`, `CREATED_AT` |
| `order` | enum | `ASC` | направление: `ASC`, `DESC` |

Значения enum нечувствительны к регистру: `status=done` работает так же, как
`status=DONE`.

### Создание и обновление: отдельные DTO без `id`

`POST` принимает `CreateTaskRequest` (`title`, `description`, `status`, `priority`) —
идентификатор назначает сервер. `PUT` принимает `UpdateTaskRequest`, все поля
необязательны, обновляются только переданные.

### Аутентификация (КТ-2)

1. `POST /api/auth/register` с `{ "username", "password" }`. Пароль хэшируется
   BCrypt (cost 12) и в хранилище попадает только хэш. Повторный логин → `409`.
   В ответе нет ни пароля, ни хэша.
2. `POST /api/auth/login` сверяет пароль через `BCrypt.verify`, а не сравнением
   строк. При успехе возвращает JWT (`HMAC256`) со сроком жизни 1 час и claim
   `userId`. Неверный логин или пароль → `401`.
3. `POST`, `PUT` и `DELETE` задач требуют заголовок
   `Authorization: Bearer <token>`. Без токена, с битым токеном или без claim
   `userId` сервер отвечает `401`. `GET` публичный.

Секрет подписи берётся из `jwt.secret` в `application.yaml` и переопределяется
переменной `JWT_SECRET`.

Пример токена:

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9....",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "userId": 1,
  "username": "alice"
}
```

### Модель задачи

```json
{
  "id": 1,
  "title": "Изучить Ktor",
  "description": "Разобраться с маршрутизацией и плагинами",
  "status": "DONE",
  "priority": "HIGH",
  "createdAt": "2026-09-22T16:40:11.512Z",
  "updatedAt": "2026-09-22T16:40:11.512Z"
}
```

---

## HTTP-статусы и формат ошибок

| Код | Когда возвращается |
|---|---|
| `200 OK` | успешное чтение, обновление, вход, массовое удаление |
| `201 Created` | задача или пользователь созданы |
| `204 No Content` | задача удалена, тела ответа нет |
| `400 Bad Request` | некорректный path/query-параметр, битый JSON |
| `401 Unauthorized` | неверный пароль, нет токена или токен недействителен |
| `404 Not Found` | задачи с таким `id` нет либо маршрут не существует |
| `409 Conflict` | задача с таким `title` уже есть либо логин занят |
| `415 Unsupported Media Type` | тело отправлено без `Content-Type: application/json` |
| `422 Unprocessable Entity` | JSON корректен, но не прошёл валидацию (пустой или слишком длинный `title`) |
| `500 Internal Server Error` | непредвиденная ошибка (логируется на сервере) |

Все ошибки приходят в едином формате:

```json
{
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "Тело запроса не прошло валидацию",
  "details": [
    "Поле 'title' обязательно и не может быть пустым"
  ],
  "path": "/api/tasks",
  "timestamp": "2026-09-22T16:41:03.881Z"
}
```

---

## Примеры запросов

```bash
# Регистрация -> 201 Created (пароль сохраняется как BCrypt-хэш)
curl -i -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"alice\",\"password\":\"secret1\"}"

# Вход -> 200 OK и JWT с claim userId
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"alice\",\"password\":\"secret1\"}"

# Неверный пароль -> 401
curl -i -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"alice\",\"password\":\"wrong!!\"}"

# Список задач — публично, токен не нужен
curl http://localhost:8080/api/tasks

# Одна задача (path-параметр) — тоже публично
curl http://localhost:8080/api/tasks/1

# Создание без токена -> 401
curl -i -X POST http://localhost:8080/api/tasks \
  -H "Content-Type: application/json" \
  -d "{\"title\":\"Новая задача\",\"priority\":\"HIGH\"}"

# Создание с JWT -> 201 Created
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"alice\",\"password\":\"secret1\"}" | jq -r .accessToken)

curl -i -X POST http://localhost:8080/api/tasks \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d "{\"title\":\"Новая задача\",\"priority\":\"HIGH\"}"

# Обновление -> 200 OK
curl -X PUT http://localhost:8080/api/tasks/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d "{\"status\":\"DONE\"}"

# Удаление -> 204 No Content
curl -i -X DELETE http://localhost:8080/api/tasks/5 \
  -H "Authorization: Bearer $TOKEN"

# Битый токен -> 401
curl -i -X DELETE http://localhost:8080/api/tasks/5 \
  -H "Authorization: Bearer not.a.token"
```

На Windows PowerShell токен удобнее взять из ответа `/api/auth/login` руками
и подставить в заголовок `Authorization: Bearer ...`. Готовые запросы лежат
в `requests.http`.

---

## Тесты

```bash
./gradlew test
```

Тесты на `ktor-server-test-host` покрывают CRUD, BCrypt (хэш ≠ пароль,
`verify` принимает верный пароль и отвергает неверный), выдачу JWT и ответ
`401` без токена и с недействительным токеном. HTML-отчёт:
`build/reports/tests/test/index.html`.

---

## Ветки репозитория

| Ветка | Назначение |
|---|---|
| `main` | стабильное состояние, проверенный результат |
| `develop` | интеграционная ветка разработки |
| `feature/kt1-ktor-rest-api` | КТ-1: REST без аутентификации |
| `feature/kt2-auth-jwt` | КТ-2: регистрация, BCrypt, JWT |

Для следующих контрольных точек заводятся ветки `feature/kt2-...`, `feature/kt3-...`
и вливаются в `develop`, затем в `main`.

---

## Данные

Хранилище задач и пользователей — in-memory (`ConcurrentHashMap`). Задачи при
старте заполняются пятью демонстрационными записями, пользователи — пустым
списком. Пароли хранятся только как BCrypt-хэш. После перезапуска данные
возвращаются к исходному набору.
