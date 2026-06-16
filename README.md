# Task Management System

This project is an internal task management REST API developed as part of the internship program.

## Purpose

To develop a backend API that enables company users to work on projects and tasks, allowing them to perform operations such as creating tasks, assigning tasks, updating task statuses, and adding comments.

## Tech Stack

- Java 17
- Spring Boot 3.x
- Spring Data JPA
- PostgreSQL
- Spring Security
- JWT Authentication
- Maven
- JUnit & Mockito
- Docker
- Swagger/OpenAPI

## Branch Structure

- `main`: Stable and production-ready release branch
- `develop`: Active development branch
- `feature/*`: Temporary branches used for specific feature developments

## Development Workflow

To create a new feature branch, follow these steps:

```bash
# Switch to the develop branch
git checkout develop

# Pull the latest changes from the remote repository
git pull origin develop

# Create and switch to a new feature branch
git checkout -b feature/your-feature-name
