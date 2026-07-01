# Spring Boot Boilerplate

Minimal Spring Boot boilerplate for building services/APIs with modular structure, JWT authentication, Flyway migrations, and example seeders.

## Features

- Modular structure: auth, user, common, security
- JWT authentication and token provider
- Database migrations with Flyway
- Example data seeder
- Unit tests example

## Requirements

- Java 17+
- Gradle (wrapper included)
- PostgreSQL (or another DB configured in `application.yml`)

## Quick Start

1. Copy example config (if present) and edit DB settings:

```bash
cp src/main/resources/application.yml.example src/main/resources/application.yml
# or edit
```

2. Update database and JWT properties in application.yml:

- `spring.datasource.url`
- `spring.datasource.username`
- `spring.datasource.password`
- `jwt.secret`
- `jwt.expirationMs`

3. Run the application:

```bash
./gradlew bootRun
```

4. Build JAR:

```bash
./gradlew bootJar
```

## Migrations & Seeders

- Flyway migrations: migration
- Example seeder: seeder

## Configuration Files

- Main config: application.yml
- Resource folder: resources

## Run Tests

```bash
./gradlew test
```

## Project Structure (short)

- java — application code
  - `auth` — controllers, DTOs, services, repositories for authentication
  - `user` — user entity, repository, seeder
  - `common` — shared configs, DTOs, exception handling
  - `security` — JWT provider, policies
- resources — configs, migrations, templates
- build.gradle — build configuration

## Development Tips

- Use the Gradle wrapper: gradlew for consistent builds.
- Apply migrations before startup when changing DB schema.
- Add integration tests for auth flows and seeder behavior.

## Contributing

- Fork, create branch `feature/your-feature`, open a PR to `main`.
- Include tests for logic changes.
- Keep commits small and focused.

## License

Add a `LICENSE` file to this repository and set the desired license.
