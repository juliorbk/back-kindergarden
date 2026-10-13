# AGENTS.md — back-kindergarten

Backend (API REST) del panel administrativo de la UEC Luisa Cáceres.
El frontend vive en el repositorio aparte `front-kindergarten` (Next.js).

## Stack

- Java 21, Spring Boot 4.1, Maven. Usar siempre el wrapper: `.\mvnw.cmd` (no hay Maven global instalado).
- PostgreSQL 17 + Flyway para migraciones versionadas.
- Spring Security con JWT HS256 stateless. Roles: `ADMIN`, `EDITOR`.
- Lombok, Jakarta Validation, MapStruct no (mapeo manual), JUnit 5 + Testcontainers.

**Versiones nuevas:** es Spring Boot 4 / Spring Security 7, con paquetes renombrados respecto a versiones anteriores
(p. ej. `spring-boot-starter-webmvc`, `org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc`).
Antes de usar una API, verifica la firma real contra el jar del repositorio local
(ej.: `javap -classpath ~/.m2/repository/... Clase`) en vez de asumir.

## Estructura de paquetes (`com.ueclca.backend`)

| Paquete | Contenido |
|---|---|
| `controllers/` | Endpoints REST. Finos: validan entrada, llaman al servicio y devuelven respuesta. Sin lógica de negocio ni acceso a repositorios. |
| `services/` | Lógica de negocio y transacciones. |
| `repositories/` | Interfaces Spring Data JPA. |
| `models/` | Entidades JPA (una clase por tabla). |
| `dtos/` | Records de request/response. La API nunca expone entidades. |
| `security/` | Configuración de seguridad, JWT, `UserDetailsService`. |
| `config/` | `@ConfigurationProperties` y beans transversales. |
| `exceptions/` | Excepciones propias + manejador global. |

## Reglas de código

1. **Capas:** `controller → service → repository`. Un servicio puede usar varios repositorios; llamar a otro
   servicio solo cuando haya reutilización clara (ej.: `AuthService → UserService`).
2. **Inyección:** por constructor con `@RequiredArgsConstructor` de Lombok. Nunca `@Autowired` en campos.
3. **Entidades:** `@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor`. `@Data` y `@EqualsAndHashCode`
   prohibidos en entidades. Todos los cambios de esquema van en Flyway; `ddl-auto: validate` y nunca `update`/`create`.
4. **DTOs:** records inmutables en `dtos/`, con mapeo estático `XxxResponse.from(entidad)`. Validaciones Jakarta
   (`@NotBlank`, `@Email`, `@Size`, etc.) en los DTOs de entrada, con mensajes en español.
5. **Transacciones:** `@Transactional` en escritura y `@Transactional(readOnly = true)` en lectura, a nivel de clase
   de servicio.
6. **Errores:** lanzar `ResourceNotFoundException` / `ConflictException` / `BadRequestException`; el
   `GlobalExceptionHandler` responde `ProblemDetail` (RFC 7807) con mensajes en español. Prohibido `catch` vacíos
   o genéricos que oculten errores.
7. **API:** base `/api/v1`, rutas en inglés y plural (`/posts`, `/events`, `/users`), JSON en camelCase.
   Códigos: 200/201/204, 400 validación, 401 sin autenticar, 403 sin permiso, 404, 409 conflicto.
8. **Seguridad:** todo endpoint exige JWT salvo lo declarado público en `SecurityConfig` (login y health).
   Autorización por rol con `@PreAuthorize("hasRole('ADMIN')")` a nivel de método. Nunca loggear tokens,
   contraseñas ni hashes. Los secretos solo por variables de entorno; nada hardcodeado ni commiteado.
9. **Base de datos:** tablas y columnas en `snake_case` y plural. Migraciones `V{n}__descripcion.sql`; una vez
   mergeadas son inmutables (los cambios van en una migración nueva).
10. **Idioma:** identificadores de código en inglés; mensajes de API/UI y commits en español.
11. **Tests:** unitarios `*Test` para lógica de servicios; integración `*IT` con Testcontainers contra PostgreSQL
    real (extender `AbstractIntegrationTest`). Todo bugfix incluye test de regresión.
12. **Estilo:** 4 espacios, máximo 120 columnas, sin imports con `*`, sin `System.out` (usar `@Slf4j`).
13. **Git:** ramas `feat/`, `fix/`, `chore/`; Conventional Commits en español
    (`feat: agregar CRUD de publicaciones`). Nunca commitear `.env` ni credenciales.

## Flujo para agregar una feature

1. Migración Flyway con el cambio de esquema (si aplica).
2. Entidad en `models/` + repositorio en `repositories/`.
3. DTOs en `dtos/` con validaciones.
4. Servicio en `services/` con la lógica y transacciones.
5. Controller en `controllers/` (delgado).
6. Tests: `*IT` extendiendo `AbstractIntegrationTest`.
7. `.\mvnw.cmd test` en verde antes del commit.

## Comandos

```powershell
docker compose up -d                      # PostgreSQL local (dev)
.\mvnw.cmd spring-boot:run                # API en http://localhost:8080 (perfil dev por defecto)
.\mvnw.cmd test                           # Tests (requiere Docker corriendo: Testcontainers)
.\mvnw.cmd -DskipTests compile            # Compilar
```

## Contrato con el frontend

- `POST /api/v1/auth/login` → `{ token, tokenType, expiresInSeconds, user }`.
- El front envía `Authorization: Bearer <token>` en el resto de endpoints.
- CORS permite `http://localhost:3000` por defecto (configurable con `CORS_ALLOWED_ORIGINS`).
