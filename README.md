# The Chat Backend

Spring Boot backend for The Chat, using Java 21, PostgreSQL, Spring Security, JWT, JPA, Flyway, and validation.

## Structure

```text
src/main/java/com/thechat
  auth/       Auth controller, service, DTOs, auth-specific exceptions
  common/     Shared API error handling
  security/   Stateless JWT security and typed app properties
  user/       User entity and repository
```

## Local Setup

Start PostgreSQL:

```bash
docker compose up -d
```

Run the app:

```bash
mvn spring-boot:run
```

The default API base URL is `http://localhost:8080`.

## Environment

Dummy development values are already in `src/main/resources/application.yml`. Copy `.env.example` values into your deployment environment and replace secrets before production.

Required settings:

```bash
DB_URL=jdbc:postgresql://localhost:5432/the_chat
DB_USERNAME=postgres
DB_PASSWORD=postgres
CORS_ALLOWED_ORIGINS=http://localhost:3000
AUTH_COOKIE_NAME=access_token
AUTH_COOKIE_SECURE=false
AUTH_COOKIE_SAME_SITE=Lax
AUTH_COOKIE_PATH=/
JWT_SECRET=replace-this-dummy-secret-with-at-least-32-characters
```

## Auth Routes

Register:

```http
POST /api/v1/auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "name": "User",
  "password": "password123"
}
```

Login:

```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123"
}
```

Both routes set an HTTP-only `access_token` cookie and return:

```json
{
  "expiresInSeconds": 3600,
  "user": {
    "id": "...",
    "email": "user@example.com",
    "name": "User"
  }
}
```

Logout clears the auth cookie:

```http
POST /api/v1/auth/logout
```

<!-- Stop-Process  -->
