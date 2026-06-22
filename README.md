# Task Management System

Task Management System is a full-stack task management application built with Spring Boot 3, Java 17, React/Vite, and PostgreSQL. It supports authentication, role-based task assignment, project and task management, task comments, pagination, sorting, filtering, and Docker-based local startup.

## Tech Stack

- Java 17
- Spring Boot 3
- Spring Web
- Spring Security with JWT
- Spring Data JPA
- PostgreSQL
- Maven Wrapper
- Lombok
- Bean Validation
- JUnit 5 and Mockito
- React with Vite
- Axios
- Docker and Docker Compose
- Swagger/OpenAPI with springdoc-openapi

## Main Features

- User registration and login with JWT authentication.
- Role-based access behavior for `ADMIN` and `USER`.
- Project CRUD.
- Task CRUD with status updates and assignee support.
- Role-restricted task assignment:
  - `ADMIN` can assign tasks to any user or leave tasks unassigned.
  - `USER` can create tasks only for themselves.
  - `USER` cannot assign tasks to another user.
  - `USER` cannot change a task assignee while editing.
- Task list pagination, sorting, and filtering.
- Task detail view with assigned user, created date, updated date, and comments.
- Task comments:
  - List comments for a task.
  - Add a new comment to a task.
  - Comment content is validated and limited to 1000 characters.
- PostgreSQL data persistence through a Docker volume.

## Configuration

The application reads database, JWT, Docker port, and frontend API settings from environment variables.

| Variable | Default |
| --- | --- |
| `POSTGRES_DB` | `task_management_db` |
| `POSTGRES_USER` | `postgres` |
| `POSTGRES_PASSWORD` | `postgres` |
| `POSTGRES_PORT` | `5433` |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5433/task_management_db` |
| `SPRING_DATASOURCE_USERNAME` | `postgres` |
| `SPRING_DATASOURCE_PASSWORD` | `postgres` |
| `JWT_SECRET` | required |
| `JWT_EXPIRATION` | `86400000` |
| `BACKEND_PORT` | `8080` |
| `FRONTEND_PORT` | `5173` |
| `VITE_API_BASE_URL` | `http://localhost:8080` |

Generate a local JWT secret with:

```bash
openssl rand -base64 32
```

Do not commit real secrets. Use `.env.example` as a template.

## Run With Docker

This starts PostgreSQL, the Spring Boot backend, and the React/Vite frontend with one command.

```bash
cp .env.example .env
# Edit JWT_SECRET in .env before starting the stack.
docker compose up --build
```

Services run at:

```text
Frontend: http://localhost:5173
Backend API: http://localhost:8080
PostgreSQL: localhost:5433
```

Swagger UI:

```text
http://localhost:8080/swagger-ui/index.html
```

OpenAPI JSON:

```text
http://localhost:8080/v3/api-docs
```

Stop the containers:

```bash
docker compose down
```

Stop the containers and delete the PostgreSQL data volume:

```bash
docker compose down -v
```

`docker compose down` keeps database data. `docker compose down -v` deletes the named PostgreSQL volume and removes the database data.

## Docker Services

`docker-compose.yml` defines:

- `db`: PostgreSQL 16, mapped from host port `5433` to container port `5432`.
- `backend`: Spring Boot app built from `backend/Dockerfile`, mapped to host port `8080`.
- `frontend`: React/Vite app built from `frontend/Dockerfile`, served through port `5173`.
- `postgres_data`: named Docker volume for persistent PostgreSQL data.

Inside Docker, the backend connects to PostgreSQL with:

```text
jdbc:postgresql://db:5432/task_management_db
```

The Docker frontend is built with:

```text
VITE_API_BASE_URL=http://localhost:8080
```

If the default ports are already in use, override them:

```bash
BACKEND_PORT=18080 FRONTEND_PORT=15173 POSTGRES_PORT=15433 docker compose up --build
```

## Local Development

Run only PostgreSQL in Docker:

```bash
docker compose up -d db
```

Run the backend locally:

```bash
export JWT_SECRET="<generated-secret>"
export JWT_EXPIRATION=86400000
./mvnw spring-boot:run
```

Run the frontend locally:

```bash
cd frontend
npm install
npm run dev
```

Build the frontend:

```bash
cd frontend
npm run build
```

Run backend tests:

```bash
./mvnw test
```

## API Overview

### Authentication

- `POST /api/auth/register` - Register a user.
- `POST /api/auth/login` - Login and receive a JWT.

### Users

- `POST /api/users` - Create a user.
- `GET /api/users?page=0&size=10&sort=id,desc` - List users with pagination and sorting.
- `GET /api/users/{id}` - Get a user by id.
- `PUT /api/users/{id}` - Update a user.
- `DELETE /api/users/{id}` - Delete a user.

### Projects

- `POST /api/projects` - Create a project.
- `GET /api/projects?page=0&size=10&sort=createdAt,desc` - List projects with pagination and sorting.
- `GET /api/projects/{id}` - Get a project by id.
- `PUT /api/projects/{id}` - Update a project.
- `DELETE /api/projects/{id}` - Delete a project.

### Tasks

- `POST /api/tasks` - Create a task.
- `GET /api/tasks/{id}` - Get task details.
- `GET /api/tasks?page=0&size=10&sortBy=createdAt&direction=desc` - List tasks with pagination and sorting.
- `GET /api/tasks?status=TODO&priority=HIGH&projectId=1&assignedUserId=2` - List tasks with filters.
- `PUT /api/tasks/{id}` - Update a task.
- `DELETE /api/tasks/{id}` - Delete a task.
- `PATCH /api/tasks/{id}/status?status=DONE` - Update task status.
- `PATCH /api/tasks/{id}/assign?userId=1` - Assign a task to a user.

### Comments

- `GET /api/tasks/{taskId}/comments` - List comments for a task.
- `POST /api/tasks/{taskId}/comments?userId={userId}` - Add a comment to a task.

Comment request body:

```json
{
  "content": "Comment text"
}
```

Comment response example:

```json
{
  "id": 1,
  "content": "Comment text",
  "taskId": 4,
  "userId": 3,
  "username": "mahmut@example.com",
  "createdAt": "2026-06-22T01:34:00"
}
```

## Task Listing: Pagination, Sorting, and Filtering

The task list endpoint supports these query parameters:

| Parameter | Description | Default |
| --- | --- | --- |
| `page` | Zero-based page number | `0` |
| `size` | Page size | `10` |
| `sortBy` | Sort field | `createdAt` |
| `direction` | Sort direction: `asc` or `desc` | `desc` |
| `status` | Filter by task status | none |
| `priority` | Filter by task priority | none |
| `projectId` | Filter by project id | none |
| `assignedUserId` | Filter by assigned user id | none |

Allowed task sort fields:

```text
id, title, status, priority, dueDate, createdAt, updatedAt
```

Examples:

```text
GET /api/tasks?page=0&size=10
GET /api/tasks?status=TODO&page=0&size=10
GET /api/tasks?priority=HIGH&sortBy=createdAt&direction=desc
GET /api/tasks?assignedUserId=1&projectId=2&page=0&size=20
```

The response is a Spring `Page<TaskResponse>` response. The most useful fields are:

```text
content, number, size, totalElements, totalPages, last
```

## Task Response Notes

Task responses include:

- `assignedUserId`
- `assignedUsername`
- `assignedUserFullName`
- `createdAt`
- `updatedAt`

`assignedUsername` is populated from the assigned user's email when the project does not have a separate username field.

## Frontend Notes

The React/Vite frontend includes:

- Login and register screens.
- Task list filtering by status, priority, project, and assigned user.
- Task list sorting by created date, updated date, due date, priority, status, and title.
- Task list pagination with previous and next controls.
- Task detail page with comments, created date, updated date, and assigned user.
- Task create/edit screens that respect role-based assignment behavior.

The frontend sends API requests to:

```text
VITE_API_BASE_URL
```

For local Docker startup, this is normally:

```text
http://localhost:8080
```

## Verification

Useful checks after changes:

```bash
./mvnw test
cd frontend && npm run build
docker compose up --build
```
