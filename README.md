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

El perfil `dev` (por defecto) siembra un usuario administrador si no existe:

| Email | Contraseña |
|---|---|
| `admin@ueclca.edu.ve` | `admin1234` |

Cambiables con `SEED_ADMIN_EMAIL` / `SEED_ADMIN_PASSWORD` (solo perfil dev; en producción no se siembra nada).

## Endpoints iniciales

| Método | Ruta | Acceso |
|---|---|---|
| `POST` | `/api/v1/auth/login` | Público |
| `GET` | `/api/v1/auth/me` | Requiere `Authorization: Bearer <token>` |
| `GET` | `/actuator/health` | Público |

Ejemplo:

```powershell
curl.exe -X POST http://localhost:8080/api/v1/auth/login -H "Content-Type: application/json" -d "{\"email\":\"admin@ueclca.edu.ve\",\"password\":\"admin1234\"}"
```

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
