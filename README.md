# Task Management System

Task Management System is a Spring Boot REST API for managing users, roles, projects, tasks, and task comments. It includes CRUD modules, authentication, ownership checks, PostgreSQL persistence, and Swagger/OpenAPI documentation.

## Technologies

- Java 17
- Spring Boot 3
- Spring Web
- Spring Data JPA
- Spring Security
- PostgreSQL
- Maven Wrapper
- Lombok
- Bean Validation
- JUnit 5
- Mockito
- Docker / Docker Compose
- Swagger / OpenAPI with springdoc-openapi

## Configuration

The application reads database and JWT settings from environment variables.

| Variable | Default |
| --- | --- |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5433/task_management_db` |
| `SPRING_DATASOURCE_USERNAME` | `postgres` |
| `SPRING_DATASOURCE_PASSWORD` | `postgres` |
| `JWT_SECRET` | no default, must be provided |
| `JWT_EXPIRATION` | `86400000` |
| `BACKEND_PORT` | `8080` |
| `FRONTEND_PORT` | `5173` |
| `POSTGRES_PORT` | `5433` |

Generate a local JWT secret with:

```bash
openssl rand -base64 32
```

Do not commit real secrets. Use `.env.example` as a template only.

## Full Docker Setup

This starts PostgreSQL, the Spring Boot backend, and the React/Vite frontend with one command.

```bash
export JWT_SECRET="<generated-secret>"
export JWT_EXPIRATION=86400000
docker compose up --build
```

Services run at:

```text
Frontend: http://localhost:5173
Backend API: http://localhost:8080
PostgreSQL: localhost:5433
```

If those ports are already in use, override them:

```bash
BACKEND_PORT=18080 FRONTEND_PORT=15173 POSTGRES_PORT=15433 JWT_SECRET="<generated-secret>" docker compose up --build
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

Stop the containers and delete the PostgreSQL volume:

```bash
docker compose down -v
```

`docker compose down` does not delete database data. `docker compose down -v` deletes the named volume and removes the database data.

## Local App With Docker PostgreSQL

Use this mode when you want PostgreSQL in Docker and the Spring Boot app running locally.

```bash
docker compose up -d db
export JWT_SECRET="<generated-secret>"
export JWT_EXPIRATION=86400000
./mvnw spring-boot:run
```

In this mode the application uses the default local database URL:

```text
jdbc:postgresql://localhost:5433/task_management_db
```

## Docker Services

`docker-compose.yml` defines:

- `db`: PostgreSQL 16, mapped from host port `5433` to container port `5432`
- `backend`: Spring Boot app built from `backend/Dockerfile`, mapped to host port `8080`
- `frontend`: React/Vite app built from `frontend/Dockerfile`, mapped to host port `5173`
- `postgres_data`: named volume for persistent PostgreSQL data

Inside Docker, the app connects to PostgreSQL with:

```text
jdbc:postgresql://db:5432/task_management_db
```

The Docker frontend is built with:

```text
VITE_API_BASE_URL=http://localhost:8080
```

## Run Tests

Run this before building an image or pushing changes:

```bash
./mvnw clean test
```

## Health Check

After startup, verify these endpoints:

- `POST http://localhost:8080/api/auth/register`
- `POST http://localhost:8080/api/auth/login`
- `GET http://localhost:8080/swagger-ui/index.html`

## Main Endpoints

### Users

- `POST /api/users` - Create user
- `GET /api/users?page=0&size=10&sort=id,desc` - List users with pagination and sorting
- `GET /api/users/{id}` - Get user by id
- `PUT /api/users/{id}` - Update user
- `DELETE /api/users/{id}` - Delete user

### Projects

- `POST /api/projects` - Create project
- `GET /api/projects?page=0&size=10&sort=createdAt,desc` - List projects with pagination and sorting
- `GET /api/projects/{id}` - Get project by id
- `PUT /api/projects/{id}` - Update project
- `DELETE /api/projects/{id}` - Delete project

### Tasks

- `POST /api/tasks` - Create task
- `GET /api/tasks/{id}` - Get task by id
- `GET /api/tasks?page=0&size=10&sort=createdAt,desc` - List tasks with pagination and sorting
- `GET /api/tasks?status=TODO&priority=HIGH&projectId=1&assignedUserId=2` - List tasks with filters
- `PUT /api/tasks/{id}` - Update task
- `DELETE /api/tasks/{id}` - Delete task
- `PATCH /api/tasks/{id}/status?status=DONE` - Update task status
- `PATCH /api/tasks/{id}/assign?userId=1` - Assign task to user

### Comments

- `POST /api/tasks/{taskId}/comments` - Add comment to task
- `GET /api/tasks/{taskId}/comments` - List comments by task id
- `PUT /api/tasks/{taskId}/comments/{commentId}` - Update comment
- `DELETE /api/tasks/{taskId}/comments/{commentId}` - Delete comment
