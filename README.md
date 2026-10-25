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
Copy-Item .env.example .env     # Ajustar JWT_SECRET y SEED_ADMIN_PASSWORD
docker compose up -d            # Levanta PostgreSQL en localhost:5432
.\mvnw.cmd spring-boot:run      # API en http://localhost:8080
```

La aplicación importa `.env` automáticamente (`spring.config.import`). `JWT_SECRET` es obligatorio
(mínimo 32 caracteres) y `SEED_ADMIN_PASSWORD` es obligatorio cuando el seed de desarrollo está activo.
El perfil `dev` (por defecto) siembra un usuario administrador y publicaciones de ejemplo si no existen;
en producción no se siembra nada.

## Endpoints

| Método | Ruta | Acceso |
|---|---|---|
| `POST` | `/api/v1/auth/login` | Público |
| `GET` | `/api/v1/auth/me` | Requiere `Authorization: Bearer <token>` |
| `GET` | `/api/v1/public/posts?page=&size=` | Público (solo publicados, `publishedAt` DESC) |
| `GET` | `/api/v1/public/posts/{slug}` | Público (404 si el post no está publicado) |
| `GET` | `/api/v1/posts?status=&page=&size=` | ADMIN/EDITOR (todos los estados) |
| `GET` | `/api/v1/posts/{id}` | ADMIN/EDITOR |
| `POST` | `/api/v1/posts` | ADMIN/EDITOR (incluye canales y galería) |
| `PUT` | `/api/v1/posts/{id}` | ADMIN/EDITOR |
| `PATCH` | `/api/v1/posts/{id}/status` | ADMIN/EDITOR (publicar/despublicar) |
| `DELETE` | `/api/v1/posts/{id}` | ADMIN/EDITOR |
| `POST` | `/api/v1/media` | ADMIN/EDITOR (multipart, campo `file`) |
| `GET` | `/api/v1/media/files/{storageKey}` | Público (archivo subido) |
| `GET` | `/api/v1/public/events?from=&to=` | Público (solo publicados, `startsAt` ASC) |
| `GET` | `/api/v1/events?status=&from=&to=&page=&size=` | ADMIN/EDITOR (todos los estados) |
| `GET` | `/api/v1/events/{id}` | ADMIN/EDITOR |
| `POST` | `/api/v1/events` | ADMIN/EDITOR |
| `PUT` | `/api/v1/events/{id}` | ADMIN/EDITOR |
| `PATCH` | `/api/v1/events/{id}/status` | ADMIN/EDITOR (publicar/despublicar) |
| `DELETE` | `/api/v1/events/{id}` | ADMIN/EDITOR |
| `GET` | `/actuator/health` | Público |

`status` es `DRAFT` o `PUBLISHED` (enum `PublicationStatus`). Los posts declaran sus destinos en
`channels`: `TELEGRAM`, `FACEBOOK` o `INSTAGRAM`, y adjuntan una galería con `mediaIds` (IDs devueltos
por `POST /api/v1/media`, máximo 10). La primera imagen de la galería es la portada (`imageUrl`).
`title` es opcional (se deriva del contenido) y `imageUrl` se acepta como URL externa de respaldo.
Cada destino queda en `post_channels` con estado `PENDING` hasta que existan las integraciones reales.
`category` de eventos es una de `MEETING`, `CULTURAL`, `SPORTS`, `HOLIDAY`, `ACADEMIC`, `OTHER`
(el front decide etiqueta y color); `allDay` es opcional y por defecto es `false`.
Cada evento tiene `kind` (`CALENDAR` o `EPHEMERIS`, por defecto `CALENDAR`), `imageUrl` opcional
(obligatoria para efemérides) y `featured` para destacarlo como aviso en la cartelera.
El listado público acepta los filtros opcionales `kind` y `featured`.
Las fechas (`from`, `to`, `startsAt`, `endsAt`, `publishedAt`) son ISO-8601 UTC (`2026-11-20T13:00:00Z`).
El listado público de eventos devuelve los publicados que **solapan** con el rango pedido.

### Ejemplos

```powershell
# Login y token (usar la contraseña definida en SEED_ADMIN_PASSWORD)
curl.exe -X POST http://localhost:8080/api/v1/auth/login -H "Content-Type: application/json" -d "{\"email\":\"director@ueclca.edu.ve\",\"password\":\"<tu-contraseña>\"}"

# Crear un post (borrador) con canales e imagen externa
curl.exe -X POST http://localhost:8080/api/v1/posts -H "Authorization: Bearer <token>" -H "Content-Type: application/json" -d "{\"title\":\"Feria de Ciencias 2026\",\"caption\":\"Los estudiantes presentaron sus proyectos.\",\"imageUrl\":\"https://example.com/feria.jpg\",\"channels\":[\"TELEGRAM\",\"FACEBOOK\"]}"

# Subir una imagen y usar su id en la galería del post
curl.exe -X POST http://localhost:8080/api/v1/media -H "Authorization: Bearer <token>" -F "file=@C:\ruta\foto.png"
curl.exe -X POST http://localhost:8080/api/v1/posts -H "Authorization: Bearer <token>" -H "Content-Type: application/json" -d "{\"caption\":\"Anuncio con galería.\",\"mediaIds\":[1,2],\"channels\":[\"TELEGRAM\",\"INSTAGRAM\"]}"

# Publicar / despublicar
curl.exe -X PATCH http://localhost:8080/api/v1/posts/1/status -H "Authorization: Bearer <token>" -H "Content-Type: application/json" -d "{\"status\":\"PUBLISHED\"}"

# Listado público (sin token)
curl.exe "http://localhost:8080/api/v1/public/posts"
curl.exe "http://localhost:8080/api/v1/public/posts/feria-de-ciencias-2026"

# Crear un evento
curl.exe -X POST http://localhost:8080/api/v1/events -H "Authorization: Bearer <token>" -H "Content-Type: application/json" -d "{\"title\":\"Festival cultural\",\"location\":\"Patio central\",\"category\":\"CULTURAL\",\"kind\":\"CALENDAR\",\"allDay\":true,\"startsAt\":\"2026-11-20T00:00:00Z\",\"endsAt\":\"2026-11-20T23:59:00Z\"}"

# Crear una efeméride destacada (requiere imagen y mensaje)
curl.exe -X POST http://localhost:8080/api/v1/events -H "Authorization: Bearer <token>" -H "Content-Type: application/json" -d "{\"title\":\"Día de la Tierra\",\"description\":\"Cuidado del planeta\",\"category\":\"CULTURAL\",\"kind\":\"EPHEMERIS\",\"imageUrl\":\"https://example.com/tierra.jpg\",\"featured\":true,\"allDay\":true,\"startsAt\":\"2026-04-22T00:00:00Z\",\"endsAt\":\"2026-04-22T23:59:00Z\"}"

# Eventos públicos de un rango (sin token)
curl.exe "http://localhost:8080/api/v1/public/events?from=2026-11-01T00:00:00Z&to=2026-11-30T23:59:00Z"

# Efemérides publicadas
curl.exe "http://localhost:8080/api/v1/public/events?kind=EPHEMERIS"
```

Formas de respuesta: los listados paginados devuelven `{ content, page, size, totalElements, totalPages, last }`.
Errores con formato `ProblemDetail` (RFC 7807) y mensajes en español, incluidos los `401` y `403`
emitidos por el filtro de seguridad.

## Tests

```powershell
.\mvnw.cmd test
```

Los tests de integración (`*IT`) usan Testcontainers: requieren Docker corriendo y levantan su
propio PostgreSQL (no tocan la base de desarrollo).

## Variables de entorno

Ver `.env.example`. Las principales: `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD`,
`JWT_SECRET` (obligatorio, mínimo 32 caracteres), `JWT_ISSUER`, `JWT_EXPIRATION_MINUTES`,
`CORS_ALLOWED_ORIGINS`, `SEED_ADMIN_EMAIL`, `SEED_ADMIN_PASSWORD`, `SEED_ADMIN_FULL_NAME`,
`MEDIA_STORAGE_PATH` (por defecto `./uploads`), `MEDIA_PUBLIC_BASE_URL` (base de las URLs públicas
de archivos) y `MEDIA_MAX_FILE_SIZE_BYTES` (50 MB por defecto).
En producción no se deben usar los valores de ejemplo de `.env.example`.

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
