# Modular Monolith Backend Template — Design

**Status:** Approved
**Date:** 2026-07-16

## Purpose

Build a generic, reusable Spring Boot backend template implementing a modular monolith: multiple bounded-context "modules" inside a single deployable, with strict internal boundaries enforced by static analysis. Minimal footprint — no domain-specific business logic beyond a `user` module (auth/identity) and a `sample` module (demonstrates the pattern for a real bounded context). Each module must be extractable into its own microservice later by swapping in-process calls for network calls, without other modules changing.

This is not a project for a specific product — it's a starter template. YAGNI applies throughout: no feature is added unless it's needed either to make the template functional or to prove the modular pattern works end-to-end (auth, one CRUD module, one cross-module call, one cross-module event).

## Architecture

Single Gradle module (not a multi-module Gradle build). Package-by-feature under `com.nizamiftahul.spring_boilerplate`:

```
com.nizamiftahul.spring_boilerplate
├── SpringBoilerplateApplication.java   (@EnableJpaAuditing)
├── platform/
│   ├── security/
│   ├── exception/
│   └── persistence/
└── modules/
    ├── user/
    │   ├── api/          (public — only package other modules may import)
    │   ├── domain/
    │   ├── application/
    │   ├── web/
    │   └── event/
    └── sample/
        ├── api/
        ├── domain/
        ├── application/
        ├── web/
        └── event/
```

Rules:
- Only a module's `api` package may be imported by another module. `domain`, `application`, `web`, `event` are module-internal.
- `platform.*` has zero dependency on any `modules.*` class.
- No cross-module JPA relations. A module referencing another module's entity stores a plain ID column (e.g. `ownerId: Long`), never a `@ManyToOne` across module boundaries.
- Cross-module synchronous calls go through a public interface in the target module's `api` package (e.g. `UserApi.findByUsername(String)`).
- Cross-module async/side-effect notifications go through Spring `ApplicationEvent` + `@EventListener` (e.g. `UserRegisteredEvent`), not a custom event bus.
- These rules are enforced by an ArchUnit test, not by Gradle module isolation — chosen for simplicity; a future graduation path could split into multi-module Gradle or separate services once a module actually needs independent deployment.

## Modules

### `platform` (shared kernel, no module dependencies)

- `security/SecurityConfig.java` — stateless `SecurityFilterChain`, `PasswordEncoder` (BCrypt) bean, registers `JwtAuthenticationFilter`. Depends only on Spring's own `UserDetailsService` interface (implemented inside `user` module) — never imports `modules.*`.
- `security/JwtService.java` — issues/parses/validates access + refresh JWTs (jjwt library).
- `security/JwtAuthenticationFilter.java` — `OncePerRequestFilter`; reads `Authorization: Bearer <token>`, sets `SecurityContext`.
- `exception/ApiError.java` — consistent error response shape (`timestamp`, `status`, `error`, `message`, `path`).
- `exception/GlobalExceptionHandler.java` — `@RestControllerAdvice`, maps `ResourceNotFoundException` → 404, `DomainException` → 400, validation errors → 400, everything else → 500. All map to `ApiError`.
- `exception/ResourceNotFoundException.java`, `exception/DomainException.java`.
- `persistence/BaseEntity.java` — `@MappedSuperclass`: `id` (Long, generated), `createdAt`/`updatedAt` via `@CreatedDate`/`@LastModifiedDate` (JPA auditing, enabled on the main application class).

No generic `ApiResponse<T>` wrapper — controllers return DTOs (records) directly on success; only failures get the consistent `ApiError` shape. Avoids an abstraction the template doesn't need.

### `modules.user`

Responsibilities: registration, login, JWT issuance, JWT refresh/revocation, exposes user lookups to other modules.

- `domain/User.java` (extends `BaseEntity`): `username`, `email`, `passwordHash`, `role` (enum `Role { USER, ADMIN }`).
- `domain/Role.java` — enum, two values: `USER`, `ADMIN`.
- `domain/UserRepository.java` — Spring Data JPA, module-internal (not exported).
- `domain/RefreshToken.java` (extends `BaseEntity`): `tokenHash` (String, SHA-256 hash of the raw refresh token — raw token is never stored), `userId` (Long), `expiresAt` (Instant), `revoked` (boolean, default false).
- `domain/RefreshTokenRepository.java` — Spring Data JPA, module-internal.
- `application/UserDetailsServiceImpl.java` — implements Spring's `UserDetailsService`; module-internal, wired into `platform.security.SecurityConfig` via the standard `UserDetailsService` bean type (no direct class reference across the boundary).
- `application/AuthService.java`:
  - `register(RegisterRequest) -> AuthResponse` — creates `User` with `role = USER`, hashes password (BCrypt), publishes `UserRegisteredEvent`, issues access + refresh token pair.
  - `login(LoginRequest) -> AuthResponse` — authenticates via `AuthenticationManager`, issues access + refresh token pair, persists new `RefreshToken` row.
  - `refresh(RefreshRequest) -> AuthResponse` — looks up `RefreshToken` by hash of the presented token, rejects if missing/expired/revoked, revokes the old row, issues a new access + refresh pair (rotation), persists the new `RefreshToken` row.
  - `logout(RefreshRequest) -> void` — revokes the matching `RefreshToken` row.
- `application/UserService.java` — implements `api/UserApi.java`.
- `api/UserApi.java` (public interface — the only type other modules may depend on): `findByUsername(String) -> Optional<UserSummary>`, `existsByEmail(String) -> boolean`, `getById(Long) -> Optional<UserSummary>`.
- `api/UserSummary.java` — public DTO record: `id`, `username`, `email`, `role`.
- `web/AuthController.java`:
  - `POST /api/v1/auth/register` — body `RegisterRequest(username, email, password)` → `AuthResponse(accessToken, refreshToken, expiresIn)`.
  - `POST /api/v1/auth/login` — body `LoginRequest(username, password)` → `AuthResponse`.
  - `POST /api/v1/auth/refresh` — body `RefreshRequest(refreshToken)` → `AuthResponse`.
  - `POST /api/v1/auth/logout` — body `RefreshRequest(refreshToken)` → 204.
- `web/UserController.java` — `GET /api/v1/users/me` (authenticated) → `UserSummary`.
- `event/UserRegisteredEvent.java` — record: `userId`, `username`, `email`.
- `db/migration/V1__create_users_table.sql`, `db/migration/V2__create_refresh_tokens_table.sql`.

Access token expiry and refresh token expiry are both configurable via env-backed properties (e.g. `jwt.access-token-expiration`, `jwt.refresh-token-expiration`), with sane defaults (access: 1 hour, refresh: 30 days).

### `modules.sample`

Responsibilities: demonstrate the reusable module pattern — CRUD, cross-module read via `UserApi`, event reaction, one RBAC-restricted endpoint.

- `domain/SampleItem.java` (extends `BaseEntity`): `name`, `description`, `ownerId` (plain `Long`, not a JPA relation to `User`).
- `domain/SampleItemRepository.java` — Spring Data JPA, module-internal.
- `application/SampleService.java` — CRUD operations; on create, injects `UserApi` and calls `getById(ownerId)` to validate the owner exists (cross-module call demo) before persisting.
- `event/UserRegisteredEventListener.java` — `@EventListener` on `UserRegisteredEvent`; logs/reacts (e.g. could pre-provision a welcome sample item) to demonstrate event-driven reaction without a compile-time dependency on `modules.user`.
- `web/SampleController.java`:
  - `GET /api/v1/samples`, `GET /api/v1/samples/{id}` — any authenticated user.
  - `POST /api/v1/samples` — any authenticated user (creates with `ownerId` = current user's ID).
  - `PUT /api/v1/samples/{id}`, `DELETE /api/v1/samples/{id}` — restricted to `ADMIN` role via `@PreAuthorize("hasRole('ADMIN')")` — this is the RBAC demo.
- `db/migration/V3__create_sample_items_table.sql`.

## Boundary Enforcement

`src/test/java/.../architecture/ModularityRulesTest.java` (ArchUnit, JUnit 5):
1. For each module, classes in `domain`, `application`, `web`, `event` sub-packages must not be accessed from outside that module's package tree.
2. No cyclic dependencies between `modules.*` slices (`SlicesRuleDefinition.slices().matching("modules.(*)..")` must be free of cycles).
3. No class under `platform.*` may depend on any class under `modules.*`.

This test is part of the normal test suite (`./gradlew test`) — a boundary violation fails the build like any other test failure.

## Persistence

- PostgreSQL (prod and local dev, via Docker Compose).
- Flyway is the single source of schema truth: `spring.jpa.hibernate.ddl-auto: validate` (Hibernate never mutates schema; it only validates entities match Flyway-applied schema).
- Datasource config via env-var placeholders in `application.yaml` (e.g. `${DB_HOST:localhost}`, `${DB_PORT:5432}`, `${DB_NAME:spring_boilerplate}`, `${DB_USER:postgres}`, `${DB_PASSWORD:postgres}`).
- Migrations numbered sequentially and owned per-module by convention (`V1`/`V2` = user module, `V3` = sample module), all living under the single `src/main/resources/db/migration/` directory (Flyway doesn't support per-module migration folders without extra config, and splitting isn't justified yet).

## Security

- Spring Security, fully stateless (`SessionCreationPolicy.STATELESS`), CSRF disabled (no cookies/sessions in play), CORS left permissive-but-configurable for template purposes.
- Passwords hashed with BCrypt (`PasswordEncoder` bean).
- JWT access token: short-lived, signed (HMAC via jjwt), carries `sub` (username) and `role` claims. Validated on every request by `JwtAuthenticationFilter`.
- JWT refresh token: longer-lived, opaque to the resource server — only ever exchanged at `/api/v1/auth/refresh`. Stored server-side as `RefreshToken` (SHA-256 hash of the raw token, never the raw token itself) to allow revocation and rotation. On refresh, the old row is revoked and a new one issued (rotation-on-use), limiting replay of a stolen refresh token to a single use.
- `Role` enum (`USER`, `ADMIN`) drives `@PreAuthorize` method security (enabled via `@EnableMethodSecurity` in `SecurityConfig`). `sample` module's mutating admin-only endpoints are the concrete demonstration.
- `jwt.secret` is env-var-backed (`${JWT_SECRET}`), no default committed to `application.yaml` for the actual secret value — only `.env.example` documents the variable name.

## Error Handling

All exceptions funnel through `platform.exception.GlobalExceptionHandler` (`@RestControllerAdvice`) to a single `ApiError` response shape:

```json
{
  "timestamp": "2026-07-16T10:00:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "User with id 42 not found",
  "path": "/api/v1/users/42"
}
```

Mappings: `ResourceNotFoundException` → 404, `DomainException` → 400, `MethodArgumentNotValidException` (bean validation) → 400 with field errors folded into `message`, `AuthenticationException`/`AccessDeniedException` → 401/403, anything unhandled → 500 (message genericized, not leaking stack traces).

## Containerization

- `Dockerfile` — multi-stage: Gradle build stage (JDK 25) → slim JRE 25 runtime stage, non-root user, `EXPOSE 8080`.
- `docker-compose.yml` — `app` service (build from `Dockerfile`, port `8080:8080`, env vars for datasource + JWT secret, `depends_on: postgres` with a healthcheck condition) + `postgres:16` service (volume for data persistence, healthcheck via `pg_isready`).
- `.env.example` — documents `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`, `JWT_ACCESS_TOKEN_EXPIRATION`, `JWT_REFRESH_TOKEN_EXPIRATION`.

## Testing

- Unit tests for `AuthService` (register/login/refresh/logout logic, mocked repositories) and `SampleService` (mocked `UserApi`).
- `ModularityRulesTest` (ArchUnit) — boundary enforcement, runs as part of `./gradlew test`.
- No Testcontainers for now — integration behavior (Flyway migrations actually applying, JPA mappings actually working against real Postgres) is verified manually via `docker-compose up` + curl during this template's build-out, and can be automated with Testcontainers later if a real project built on this template needs that fidelity (YAGNI — avoids adding Docker-in-test-runtime complexity to a template whose primary audience needs to understand it quickly).

## Dependencies (Gradle)

Added to `build.gradle` on top of the existing `spring-boot-starter` baseline:
- `spring-boot-starter-web`
- `spring-boot-starter-data-jpa`
- `spring-boot-starter-security`
- `spring-boot-starter-validation`
- `spring-boot-starter-actuator`
- `org.postgresql:postgresql` (runtime)
- `org.flywaydb:flyway-core`, `org.flywaydb:flyway-database-postgresql`
- `io.jsonwebtoken:jjwt-api`, `io.jsonwebtoken:jjwt-impl` (runtime), `io.jsonwebtoken:jjwt-jackson` (runtime)
- `org.springdoc:springdoc-openapi-starter-webmvc-ui`
- test-only: `com.tngtech.archunit:archunit-junit5`

## Documentation

`README.md` at repo root: module structure convention, how to add a new module (copy the `sample` package shape, register migrations, add ArchUnit will auto-cover it since the rule is structural not name-specific), how to extract a module into its own microservice (swap `Api` interface call for HTTP/gRPC client, swap `ApplicationEvent` for a broker — no other module changes), local dev via `docker-compose up`, running tests (`./gradlew test`), running the ArchUnit boundary test in isolation (`./gradlew test --tests "*ModularityRulesTest*"`).

## Verification Plan

1. `./gradlew build` — compiles, all unit tests + `ModularityRulesTest` pass.
2. `./gradlew test --tests "*ModularityRulesTest*"` — boundary rules hold in isolation.
3. `docker-compose up -d --build`, then:
   - `curl -X POST localhost:8080/api/v1/auth/register -d '{"username":"alice","email":"alice@example.com","password":"..."}'` → 200 + `AuthResponse`.
   - `curl -X POST localhost:8080/api/v1/auth/login -d '{"username":"alice","password":"..."}'` → 200 + `AuthResponse`.
   - `curl -X POST localhost:8080/api/v1/auth/refresh -d '{"refreshToken":"..."}'` → 200 + new `AuthResponse`; old refresh token now rejected on reuse.
   - `curl -H "Authorization: Bearer <accessToken>" localhost:8080/api/v1/samples` → 200.
   - `curl -X PUT ... /api/v1/samples/{id}` as non-admin user → 403; as admin → 200.
4. `curl localhost:8080/actuator/health` → `{"status":"UP"}`, DB connectivity included.
5. Flyway migrations applied — check `flyway_schema_history` table or startup logs.
6. `localhost:8080/swagger-ui.html` reachable, renders both modules' endpoints.

## Decisions Log

- Single Gradle module (package-by-feature), not multi-module Gradle build — simplest to evolve; boundaries enforced via ArchUnit instead of build isolation.
- Cross-module refs by ID only (no JPA relations across modules) — required for clean future extraction into microservices.
- No generic `ApiResponse<T>` wrapper — controllers return DTOs directly; only errors get a consistent `ApiError` shape. Avoids unnecessary abstraction.
- No Spring Profiles proliferation — single `application.yaml` with env-var placeholders works for both local and docker-compose.
- Skipped Testcontainers for now — can be added later if integration-test fidelity becomes a real need.
- Refresh tokens are DB-backed (hashed, revocable) rather than stateless — chosen over stateless refresh JWTs specifically so logout/revoke works and a stolen refresh token has bounded blast radius (rotation-on-use).
- Two roles (`USER`, `ADMIN`) with one RBAC-restricted endpoint in `sample` — minimal but proves `@PreAuthorize` pattern works end-to-end.
- README.md included — documents the template itself (structure, extension, extraction path), not a changelog of "changes made."

## Out of Scope (for this template)

- Testcontainers-based integration tests.
- Multi-module Gradle build / separate deployables.
- Rate limiting, account lockout, email verification, password reset flows.
- Multi-tenancy.
- Observability beyond Actuator health/info (no metrics/tracing stack wired up).
- CI pipeline changes (existing `.github/` content untouched by this design).
