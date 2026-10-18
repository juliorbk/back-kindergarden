# back-kindergarten

API backend del panel administrativo de la UEC Luisa Cáceres. Es el CMS que alimenta la
página del colegio y la publicación en canales (Telegram, Facebook, Instagram).

## Stack

- Java 21 + Spring Boot 4.1 (Maven wrapper incluido)
- PostgreSQL 17 + Flyway
- Spring Security + JWT (HS256, stateless)
- JUnit 5 + Testcontainers

## Requisitos

- JDK 21
- Docker Desktop (para PostgreSQL local y para los tests)

## Quickstart

```powershell
docker compose up -d            # Levanta PostgreSQL en localhost:5432
.\mvnw.cmd spring-boot:run      # API en http://localhost:8080
```

El perfil `dev` (por defecto) siembra un usuario administrador y publicaciones de ejemplo si no existen:

| Email | Contraseña |
|---|---|
| `admin@ueclca.edu.ve` | `admin1234` |

Cambiables con `SEED_ADMIN_EMAIL` / `SEED_ADMIN_PASSWORD` (solo perfil dev; en producción no se siembra nada).

## Endpoints

| Método | Ruta | Acceso |
|---|---|---|
| `POST` | `/api/v1/auth/login` | Público |
| `GET` | `/api/v1/auth/me` | Requiere `Authorization: Bearer <token>` |
| `GET` | `/api/v1/public/posts?page=&size=` | Público (solo publicados, `publishedAt` DESC) |
| `GET` | `/api/v1/public/posts/{slug}` | Público (404 si el post no está publicado) |
| `GET` | `/api/v1/posts?status=&page=&size=` | ADMIN/EDITOR (todos los estados) |
| `GET` | `/api/v1/posts/{id}` | ADMIN/EDITOR |
| `POST` | `/api/v1/posts` | ADMIN/EDITOR |
| `PUT` | `/api/v1/posts/{id}` | ADMIN/EDITOR |
| `PATCH` | `/api/v1/posts/{id}/status` | ADMIN/EDITOR (publicar/despublicar) |
| `DELETE` | `/api/v1/posts/{id}` | ADMIN/EDITOR |
| `GET` | `/api/v1/public/events?from=&to=` | Público (solo publicados, `startsAt` ASC) |
| `GET` | `/api/v1/events?status=&from=&to=&page=&size=` | ADMIN/EDITOR (todos los estados) |
| `GET` | `/api/v1/events/{id}` | ADMIN/EDITOR |
| `POST` | `/api/v1/events` | ADMIN/EDITOR |
| `PUT` | `/api/v1/events/{id}` | ADMIN/EDITOR |
| `PATCH` | `/api/v1/events/{id}/status` | ADMIN/EDITOR (publicar/despublicar) |
| `DELETE` | `/api/v1/events/{id}` | ADMIN/EDITOR |
| `GET` | `/actuator/health` | Público |

`status` es `DRAFT` o `PUBLISHED` (enum `PublicationStatus`). `category` de eventos es una de
`MEETING`, `CULTURAL`, `SPORTS`, `HOLIDAY`, `ACADEMIC`, `OTHER` (el front decide etiqueta y color);
`allDay` es opcional y por defecto es `false`.
Las fechas (`from`, `to`, `startsAt`, `endsAt`, `publishedAt`) son ISO-8601 UTC (`2026-11-20T13:00:00Z`).
El listado público de eventos devuelve los publicados que **solapan** con el rango pedido.

### Ejemplos

```powershell
# Login y token
curl.exe -X POST http://localhost:8080/api/v1/auth/login -H "Content-Type: application/json" -d "{\"email\":\"admin@ueclca.edu.ve\",\"password\":\"admin1234\"}"

# Crear un post (borrador)
curl.exe -X POST http://localhost:8080/api/v1/posts -H "Authorization: Bearer <token>" -H "Content-Type: application/json" -d "{\"title\":\"Feria de Ciencias 2026\",\"caption\":\"Los estudiantes presentaron sus proyectos.\",\"imageUrl\":\"https://example.com/feria.jpg\"}"

# Publicar / despublicar
curl.exe -X PATCH http://localhost:8080/api/v1/posts/1/status -H "Authorization: Bearer <token>" -H "Content-Type: application/json" -d "{\"status\":\"PUBLISHED\"}"

# Listado público (sin token)
curl.exe "http://localhost:8080/api/v1/public/posts"
curl.exe "http://localhost:8080/api/v1/public/posts/feria-de-ciencias-2026"

# Crear un evento
curl.exe -X POST http://localhost:8080/api/v1/events -H "Authorization: Bearer <token>" -H "Content-Type: application/json" -d "{\"title\":\"Festival cultural\",\"location\":\"Patio central\",\"category\":\"CULTURAL\",\"allDay\":true,\"startsAt\":\"2026-11-20T00:00:00Z\",\"endsAt\":\"2026-11-20T23:59:00Z\"}"

# Eventos públicos de un rango (sin token)
curl.exe "http://localhost:8080/api/v1/public/events?from=2026-11-01T00:00:00Z&to=2026-11-30T23:59:00Z"
```

Formas de respuesta: los listados paginados devuelven `{ content, page, size, totalElements, totalPages, last }`.
Errores con formato `ProblemDetail` (RFC 7807) y mensajes en español.

## Tests

```powershell
.\mvnw.cmd test
```

Los tests de integración (`*IT`) usan Testcontainers: requieren Docker corriendo y levantan su
propio PostgreSQL (no tocan la base de desarrollo).

## Variables de entorno

Ver `.env.example`. Las principales: `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD`,
`JWT_SECRET` (mínimo 32 caracteres), `JWT_EXPIRATION_MINUTES`, `CORS_ALLOWED_ORIGINS`.

## Estructura

```
src/main/java/com/ueclca/backend/
├── controllers/   # Endpoints REST
├── services/      # Lógica de negocio
├── repositories/  # Spring Data JPA
├── models/        # Entidades JPA
├── dtos/          # Request/Response records
├── security/      # JWT, SecurityConfig, UserDetailsService
├── config/        # Properties y beans transversales
└── exceptions/    # Excepciones + manejador global

src/main/resources/db/migration/  # Migraciones Flyway
```

Las convenciones de código y el flujo para agregar features están en [AGENTS.md](AGENTS.md).
