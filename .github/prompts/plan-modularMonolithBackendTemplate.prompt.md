# Plan: Modular Monolith Spring Boot Template

TL;DR: Single Gradle module, package-by-feature "modules" (user, sample) each with
api/domain/application/web/event sublayers; only `api` package is public across
modules, enforced by ArchUnit. Shared `platform` package holds security (JWT),
global exception handling, and base JPA entity. Postgres + Flyway for schema,
Spring Security stateless JWT auth, Docker Compose for local dev. Cross-module
calls go through `UserApi` interface + Spring `ApplicationEvent`s
(`UserRegisteredEvent`) — no cross-module JPA relations (reference by ID only).
This structure lets a module later be extracted into its own microservice by
swapping the in-process `Api` call for an HTTP/gRPC client and events for a
broker, without other modules changing.

## Steps

### Phase 0 — Build & config (no deps)
1. Update `build.gradle`: add `spring-boot-starter-web`, `-data-jpa`,
   `-security`, `-validation`, `-actuator`; `org.postgresql:postgresql`
   (runtime); `org.flywaydb:flyway-core` + `flyway-database-postgresql`;
   `io.jsonwebtoken:jjwt-api/jjwt-impl/jjwt-jackson`; `org.springdoc:springdoc-openapi-starter-webmvc-ui`;
   test-only `com.tngtech.archunit:archunit-junit5`.
2. Update `src/main/resources/application.yaml`: datasource (env-var
   placeholders e.g. `${DB_HOST:localhost}`), `jpa.hibernate.ddl-auto: validate`,
   flyway enabled, jwt secret/expiration props, actuator endpoints
   (health, info), springdoc path.

### Phase 1 — Shared platform/kernel (*depends on Phase 0*)
Package `com.nizamiftahul.spring_boilerplate.platform`:
3. `security/SecurityConfig.java` — stateless `SecurityFilterChain`,
   `PasswordEncoder` bean, registers `JwtAuthenticationFilter`. Relies on
   Spring's own `UserDetailsService` interface (implemented later inside the
   user module) — platform package has zero import of any `modules.*` class.
4. `security/JwtService.java` — issue/parse/validate JWT (jjwt).
5. `security/JwtAuthenticationFilter.java` — `OncePerRequestFilter`, reads
   Bearer token, sets `SecurityContext`.
6. `exception/ApiError.java`, `exception/GlobalExceptionHandler.java`
   (`@RestControllerAdvice`), `exception/ResourceNotFoundException.java`,
   `exception/DomainException.java`.
7. `persistence/BaseEntity.java` — `id`, `createdAt`, `updatedAt` via
   `@CreatedDate`/`@LastModifiedDate`; enable JPA auditing on main
   application class.
8. `event/DomainEvent.java` — marker interface for convention (actual pub/sub
   uses Spring's `ApplicationEventPublisher`/`@EventListener`, no custom bus).

### Phase 2 — User module (*depends on Phase 1*)
Package `modules.user`:
9. `domain/User.java` (extends `BaseEntity`), `domain/Role.java` (enum),
   `domain/UserRepository.java` (Spring Data JPA, module-internal).
10. `application/UserDetailsServiceImpl.java` — implements Spring's
    `UserDetailsService`, package-private-ish (not exposed as api).
11. `application/AuthService.java` — register/login, publishes
    `UserRegisteredEvent` on registration, issues JWT via `JwtService`.
12. `application/UserService.java` — implements `api/UserApi.java`.
13. `api/UserApi.java` (public interface: `findByUsername`, `existsByEmail`,
    etc.) + `api/UserSummary.java` (record DTO) — the ONLY types other modules
    may import from this module.
14. `web/AuthController.java` (`POST /api/v1/auth/register`,
    `/api/v1/auth/login`), `web/UserController.java` (`GET /api/v1/users/me`),
    request/response DTOs as records.
15. `event/UserRegisteredEvent.java`.
16. `db/migration/V1__create_users_table.sql` (Flyway).

### Phase 3 — Sample module (*depends on Phase 2, parallel with nothing since it consumes UserApi*)
Package `modules.sample` — demonstrates the reusable pattern:
17. `domain/SampleItem.java` (extends `BaseEntity`, stores `ownerId` as plain
    `Long` — NOT a JPA relation to `User`, to keep modules decoupled),
    `domain/SampleItemRepository.java`.
18. `application/SampleService.java` — injects `UserApi` (cross-module call)
    to validate owner exists; `event/UserRegisteredEventListener.java`
    (`@EventListener` on `UserRegisteredEvent`) to show event-driven reaction
    without a direct dependency edge.
19. `web/SampleController.java` — secured CRUD demo endpoints
    (`/api/v1/samples`).
20. `db/migration/V2__create_sample_items_table.sql`.

### Phase 4 — Boundary enforcement (*depends on Phase 3*)
21. `src/test/java/.../architecture/ModularityRulesTest.java` (ArchUnit):
    - internal packages (`domain`, `application`, `web`, `event` — excluding
      `api`) of one module must not be referenced from another module.
    - no cyclic dependencies between `modules.*` slices
      (`SlicesRuleDefinition`).
    - `platform.*` must not depend on any `modules.*` class.

### Phase 5 — Containerization (*depends on Phase 0, parallel with Phases 1-4*)
22. `Dockerfile` — multi-stage (Gradle build stage → slim JRE runtime stage).
23. `docker-compose.yml` — `app` (build from Dockerfile, port 8080, env vars
    for datasource/JWT secret, `depends_on: postgres` with healthcheck) +
    `postgres:16` service with volume + healthcheck.
24. `.env.example` — `DB_HOST`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`,
    `JWT_SECRET`.

### Phase 6 — Docs (*depends on all above*, optional, confirm before creating)
25. `README.md` — module structure convention, how to add a new module, how
    to extract a module into its own microservice later, local dev via
    `docker-compose up`, running tests.

## Relevant files
- `build.gradle` — add dependencies (Phase 0).
- `src/main/resources/application.yaml` — datasource/flyway/jwt/actuator config.
- `src/main/java/com/nizamiftahul/spring_boilerplate/SpringBoilerplateApplication.java` — add `@EnableJpaAuditing`.
- New: `platform/**`, `modules/user/**`, `modules/sample/**`,
  `src/main/resources/db/migration/**`,
  `src/test/java/.../architecture/ModularityRulesTest.java`,
  `Dockerfile`, `docker-compose.yml`, `.env.example`.

## Verification
1. `./gradlew build` — compiles, unit tests + `ModularityRulesTest` pass.
2. `./gradlew test --tests "*ModularityRulesTest*"` — boundary rules hold.
3. `docker-compose up -d --build` then:
   - `curl -X POST localhost:8080/api/v1/auth/register -d '{...}'`
   - `curl -X POST localhost:8080/api/v1/auth/login -d '{...}'` → JWT
   - `curl -H "Authorization: Bearer <token>" localhost:8080/api/v1/samples`
4. `curl localhost:8080/actuator/health` — DB connectivity UP.
5. Flyway migrations applied — check `flyway_schema_history` table or app
   startup logs.
6. `localhost:8080/swagger-ui.html` reachable — OpenAPI docs render.

## Decisions
- Single Gradle module (package-by-feature), not multi-module Gradle build —
  simplest to evolve; boundaries enforced via ArchUnit instead of build
  isolation.
- Cross-module refs by ID only (no JPA relations across modules) — required
  for clean future extraction into microservices.
- No generic `ApiResponse<T>` wrapper — controllers return DTOs directly;
  only errors get a consistent `ApiError` shape. Avoids unnecessary
  abstraction.
- No Spring Profiles proliferation — single `application.yaml` with env-var
  placeholders works for both local and docker-compose.
- Skipped Testcontainers for now — can be added later if integration-test
  fidelity becomes a real need (per "add complexity only when justified").

## Further Considerations
1. README.md (Phase 6) documents the template itself, not "changes" —
   confirm you still want it generated, since instructions say not to create
   markdown docs unless requested.
2. Module names `user`/`sample` are placeholders per your choice — rename
   `sample` to a real bounded context once one exists.
