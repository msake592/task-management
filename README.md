# Task Management System

Task Management System is a Spring Boot REST API for managing users, roles, projects, tasks, and task comments. It provides layered backend modules for creating projects, assigning tasks to users, tracking task status and priority, adding comments, and querying tasks with pagination, sorting, and basic filters.

## Technologies

- Java 17
- Spring Boot 3
- Spring Web
- Spring Data JPA
- Spring Security
- BCrypt password hashing
- PostgreSQL
- Maven
- Lombok
- Bean Validation
- JUnit 5
- Mockito
- Docker Compose
- Swagger / OpenAPI with springdoc-openapi

## Start PostgreSQL With Docker

The project includes a `docker-compose.yml` file for PostgreSQL.

```bash
docker compose up -d
```

Default database settings:

- Database: `task_management_db`
- Username: `postgres`
- Password: `postgres`
- Host port: `5433`

To stop PostgreSQL:

```bash
docker compose down
```

## Run the Application

Start PostgreSQL first, then run the application:

```bash
./mvnw spring-boot:run
```

The application runs on:

```text
http://localhost:8080
```

Swagger UI is available at:

```text
http://localhost:8080/swagger-ui/index.html
```

OpenAPI JSON is available at:

```text
http://localhost:8080/v3/api-docs
```

## Security

The current security setup uses Spring Security with BCrypt password hashing and HTTP Basic authentication.

- Public endpoints: `/api/auth/**`
- Public documentation: `/swagger-ui/**`, `/v3/api-docs/**`
- Other API endpoints require authentication
- JWT is not implemented yet

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
- `DELETE /api/tasks/{taskId}/comments/{commentId}` - Delete comment

## Branch Structure

- `main` - Stable branch for production-ready code
- `develop` - Integration branch for active development
- `feature/*` - Feature branches for individual tasks or modules
- `bugfix/*` - Fix branches for defects

Example workflow:

```bash
git checkout develop
git pull origin develop
git checkout -b feature/task-module
```

## Run Tests

Run all tests:

```bash
./mvnw test
```

Run tests without starting external services:

```bash
./mvnw -Dtest='*ServiceImplTest' test
```

Compile without running tests:

```bash
./mvnw -DskipTests compile
```
