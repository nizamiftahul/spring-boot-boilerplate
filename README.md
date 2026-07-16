# spring-boilerplate

A modular monolith Spring Boot template: multiple bounded-context "modules" inside a single deployable, with strict internal boundaries enforced by static analysis (ArchUnit). Includes a working `user` module (auth/identity, JWT) and a `sample` module (CRUD, cross-module call, event reaction, RBAC) demonstrating the pattern end-to-end.

## Module structure convention

```
com.nizamiftahul.spring_boilerplate
├── platform/            shared kernel — security, exception handling, persistence base. No dependency on modules.*
└── modules/
    └── <module>/
        ├── api/         public — the only package other modules may import
        ├── domain/      entities, repositories — module-internal
        ├── application/ services, use cases — module-internal
        ├── web/         controllers, request/response DTOs — module-internal
        └── event/       ApplicationEvent records + listeners — the module's public eventing surface
```

Rules (enforced by `ModularityRulesTest`, an ArchUnit test run as part of `./gradlew test`):
- A module's `domain`, `application`, and `web` packages may only be accessed from within that same module.
- `api` and `event` are the two intentional public surfaces: `api` for synchronous cross-module calls (e.g. `UserApi.findByUsername`), `event` for async cross-module reactions (Spring `ApplicationEvent` + `@EventListener`).
- No cyclic dependencies between `modules.*` slices.
- `platform.*` never depends on `modules.*`.
- No cross-module JPA relations — a module referencing another module's entity stores a plain ID column (e.g. `ownerId: Long`), never a `@ManyToOne` across module boundaries.

## Adding a new module

1. Copy the `sample` package shape (`domain`, `application`, `api`, `web`, `event`).
2. Add its Flyway migration(s) under `src/main/resources/db/migration/`, numbered after the existing highest version.
3. `ModularityRulesTest` automatically covers the new module — the rule is structural (matches any `modules.<name>.*` package), not hardcoded to `user`/`sample`.

## Extracting a module into its own microservice

1. Replace in-process `Api` interface calls with an HTTP or gRPC client implementing the same contract.
2. Replace `ApplicationEvent`/`@EventListener` with a message broker (e.g. Kafka, RabbitMQ) — publisher and listener code shapes stay the same.
3. No other module needs to change, since they only ever depended on the `api`/`event` contracts, never on `domain`/`application`/`web` internals.

## Local development

```bash
cp .env.example .env   # edit JWT_SECRET at minimum
docker-compose up -d --build
```

- API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Health check: `http://localhost:8080/actuator/health`

## Running tests

```bash
./gradlew test
```

Run just the architecture boundary test:

```bash
./gradlew test --tests "*ModularityRulesTest*"
```

## Auth flow

```
POST /api/v1/auth/register {username, email, password} -> AuthResponse
POST /api/v1/auth/login    {username, password}         -> AuthResponse
POST /api/v1/auth/refresh  {refreshToken}                -> AuthResponse (rotates the refresh token)
POST /api/v1/auth/logout   {refreshToken}                -> 204
GET  /api/v1/users/me      (Authorization: Bearer <accessToken>) -> UserSummary
```

Refresh tokens are DB-backed (SHA-256 hash, revocable) rather than stateless, so logout/revoke works and a stolen refresh token is bounded to a single use (rotation-on-use).

## Sample module (RBAC demo)

```
GET    /api/v1/samples        any authenticated user
GET    /api/v1/samples/{id}   any authenticated user
POST   /api/v1/samples        any authenticated user (ownerId = current user)
PUT    /api/v1/samples/{id}   ADMIN only
DELETE /api/v1/samples/{id}   ADMIN only
```
