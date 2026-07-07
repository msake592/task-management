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
- MinIO object storage
- Swagger/OpenAPI with springdoc-openapi
- MinIO (Object Storage)
## Main Features

- Public user registration and login with JWT authentication.
- Admin-only user management, including creating users with an assigned role.
- Role-based access behavior for `ADMIN` and `USER`.
- Project CRUD.
- Project membership management.
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
- Task attachments:
  - Upload files from the task detail page.
  - List and download existing task attachments.
  - Store file content in MinIO and persist metadata in PostgreSQL.
  - Create the configured MinIO bucket automatically during backend startup.
- PostgreSQL data persistence through a Docker volume.
- Assign users to projects during project creation and update.
- File attachment support backed by MinIO object storage.

## Configuration

The application reads database, JWT, MinIO, Docker port, and frontend API settings from environment variables.

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
| `MINIO_ENDPOINT` | `http://localhost:9000` locally; `http://minio:9000` in Docker |
| `MINIO_ACCESS_KEY` | `minioadmin` |
| `MINIO_SECRET_KEY` | `minioadmin` |
| `MINIO_BUCKET_NAME` | `task-attachments` |
| `MINIO_ROOT_USER` | `minioadmin` |
| `MINIO_ROOT_PASSWORD` | `minioadmin` |
| `BACKEND_PORT` | `8080` |
| `FRONTEND_PORT` | `5173` |
| `VITE_API_BASE_URL` | `http://localhost:8080` |
| `MINIO_ENDPOINT` | `http://localhost:9000` |
| `MINIO_ACCESS_KEY` | `minioadmin` |
| `MINIO_SECRET_KEY` | `minioadmin` |
| `MINIO_BUCKET` | `task-management` |
Generate a local JWT secret with:

```bash
openssl rand -base64 32
```

Do not commit real secrets. Use `.env.example` as a template.

## Run With Docker

This starts PostgreSQL, MinIO, the Spring Boot backend, and the React/Vite frontend with one command.

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
MinIO API: http://localhost:9000
MinIO Console: http://localhost:9001
```

Swagger UI:

```text
http://localhost:8080/swagger-ui/index.html
```

OpenAPI JSON:

```text
http://localhost:8080/v3/api-docs
```
```text
MinIO API: http://localhost:9000
MinIO Console: http://localhost:9001
```
Stop the containers:

```bash
docker compose down
```

Stop the containers and delete the PostgreSQL and MinIO data volumes:

```bash
docker compose down -v
```

`docker compose down` keeps database and attachment data. `docker compose down -v` deletes the named
PostgreSQL and MinIO volumes and removes their persisted data.

## Docker Services

`docker-compose.yml` defines:

- `db`: PostgreSQL 16, mapped from host port `5433` to container port `5432`.
- `minio`: MinIO object storage, with API port `9000` and console port `9001`.
- `backend`: Spring Boot app built from `backend/Dockerfile`, mapped to host port `8080`.
- `frontend`: React/Vite app built from `frontend/Dockerfile`, served through port `5173`.
- `postgres_data`: named Docker volume for persistent PostgreSQL data.
- `minio-data`: named Docker volume for persistent attachment data.

Inside Docker, the backend connects to PostgreSQL with:

```text
jdbc:postgresql://db:5432/task_management_db
```

Inside the Docker Compose network, the backend connects to MinIO with:

```text
http://minio:9000
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

Run PostgreSQL and MinIO in Docker:

```bash
docker compose up -d db minio
docker compose up -d db minio
```

Run the backend locally:

```bash
export JWT_SECRET="<generated-secret>"
export JWT_EXPIRATION=86400000
./mvnw spring-boot:run
```

The local backend uses `http://localhost:9000` for MinIO unless `MINIO_ENDPOINT` is overridden.

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

## MinIO Task Attachments

For local backend development, start MinIO with:

```bash
docker compose up -d minio
```

MinIO connection details:

| Setting | Value |
| --- | --- |
| API endpoint | `http://localhost:9000` |
| Console | `http://localhost:9001` |
| Username | `minioadmin` |
| Password | `minioadmin` |
| Bucket | `task-attachments` |

When Spring Boot runs locally, it connects to the MinIO API at `http://localhost:9000` by default.
The hostname `minio` (for example, `http://minio:9000`) is only resolvable from containers on the
Docker Compose network. The MinIO console is available from the host at `http://localhost:9001`.
Set `MINIO_ENDPOINT` to override the API endpoint for another runtime environment.

The `task-attachments` bucket is checked and created automatically when the Spring Boot application
starts.
Attachment files are stored in MinIO. PostgreSQL stores only attachment metadata such as the original
file name, content type, size, object key, uploader, and related task.

Uploads are limited to 10 MB. Supported content types are PDF, PNG, JPEG, plain text, and DOCX.
The frontend sends files to the authenticated backend API; it never connects to MinIO directly.

Authenticated task attachment endpoints:

| Method | Path | Request | Response |
| --- | --- | --- | --- |
| `POST` | `/api/tasks/{taskId}/attachments` | `multipart/form-data` with a `file` part | `201` and attachment metadata |
| `GET` | `/api/tasks/{taskId}/attachments` | None | `200` and an array of attachment metadata |
| `GET` | `/api/tasks/{taskId}/attachments/{attachmentId}/download` | None | `200` and the file body with `Content-Disposition: attachment` |
| `DELETE` | `/api/tasks/{taskId}/attachments/{attachmentId}` | None | `204` |

Attachment metadata has the shape:

```json
{
  "id": 10,
  "originalFileName": "report.pdf",
  "contentType": "application/pdf",
  "fileSize": 1024,
  "uploadedAt": "2026-07-06T12:00:00",
  "uploadedById": 2,
  "uploadedByUsername": "user@example.com",
  "taskId": 1
}
```

## API Overview

### Authentication

- `POST /api/auth/register` - Public self-registration. The backend always assigns the `USER` role.
- `POST /api/auth/login` - Login and receive a JWT.

### Users

- `GET /api/users?page=0&size=10&sort=id,desc` - List users with pagination and sorting (`ADMIN` only).
- `GET /api/users/options` - List user options for authenticated `USER` and `ADMIN` accounts.
- `GET /api/users/{id}` - Get a user by id (`ADMIN` only).
- `PUT /api/users/{id}` - Update a user and role (`ADMIN` only).
- `DELETE /api/users/{id}` - Delete a user (`ADMIN` only).

The public registration endpoint does not accept `roleId`. Role selection is available only through
the admin-protected user management endpoint.

### Projects

- `POST /api/projects - Create a project and optionally assign users.
- `GET /api/projects?page=0&size=10&sort=createdAt,desc` - List projects with pagination and sorting.
- `GET /api/projects/{id}` - Get a project by id.
- `PUT /api/projects/{id}` - Update a project.
- `DELETE /api/projects/{id}` - Delete a project.
- `POST /api/projects/{projectId}/members` - Add a member to a project.
- `GET /api/projects/{projectId}/members` - List project members.

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
- `POST /api/tasks/{taskId}/comments` - Add a comment to a task as the authenticated user.

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

### Task Attachments

- `POST /api/tasks/{taskId}/attachments` - Upload a multipart file using the `file` form field.
- `GET /api/tasks/{taskId}/attachments` - List attachment metadata for a task.
- `GET /api/tasks/{taskId}/attachments/{attachmentId}/download` - Download an attachment.
- `DELETE /api/tasks/{taskId}/attachments/{attachmentId}` - Delete an attachment and its metadata.

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
## Project Response Notes

Project responses include:

- `assigned users`
- `createdAt`
- `updatedAt`
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
- Task detail attachment panel for selecting, uploading, listing, and downloading files.
- Attachment requests use the authenticated backend API without exposing MinIO to the browser.
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
