---
description: 'Clean architecture for medium-sized modular monoliths prioritizing maintainability, testability, and separation of concerns over minimizing files.'
applyTo: '**/*.java, **/*.kt'
---

# Clean Architecture for Modular Monoliths

Architecture for Spring Boot applications targeting medium-sized modular monoliths. Prioritizes maintainability, readability, testability, and separation of concerns.

## Core Principles

- **Package by Feature, Not Layer**: Organize packages by business domain/feature (auth, user, product) rather than technical layers
- **Separation of Concerns**: Each layer has single responsibility; distinct boundaries between layers
- **Dependency Inversion**: Depend on abstractions (interfaces), not concrete implementations
- **SOLID Compliance**: Apply Single Responsibility, Open/Closed, Liskov, Interface Segregation, and Dependency Inversion principles
- **Domain-Driven Design**: Structure mirrors business domains; code organization reflects business concepts

## Directory Structure

```
src/main/java/com/example/spring_boot_boilerplate/
├── config/                    # Global Spring configuration
│   ├── ApplicationConfig.java
│   ├── SecurityConfig.java
│   ├── WebConfig.java
│   └── DatabaseConfig.java
├── shared/                    # Cross-cutting, shared concerns
│   ├── dto/                   # Shared DTOs across modules
│   ├── exception/             # Global exception hierarchy
│   ├── util/                  # Utility classes (final, private constructor)
│   ├── event/                 # Domain events
│   └── mapper/                # Shared mappers/converters
├── [domain-module]/           # Feature-based modules (e.g., auth, user, product)
│   ├── controller/            # REST endpoints
│   │   └── AuthController.java
│   ├── service/               # Business logic layer
│   │   ├── AuthService.java   # Interface (abstraction)
│   │   └── AuthServiceImpl.java # Implementation
│   ├── repository/            # Data access abstraction
│   │   └── UserRepository.java (extends JpaRepository)
│   ├── entity/                # JPA entities (domain models)
│   │   └── User.java
│   ├── dto/                   # Module-specific DTOs
│   │   ├── LoginRequest.java
│   │   └── AuthResponse.java
│   ├── mapper/                # Entity ↔ DTO mappers
│   │   └── UserMapper.java
│   └── event/                 # Domain-specific events
│       └── UserCreatedEvent.java
└── DemoApplication.java       # Entry point
```

## Layer Responsibilities

### Controller Layer
- Handle HTTP requests/responses only
- Input validation using JSR-380 (`@NotNull`, `@Size`, etc.)
- Delegate business logic to service layer
- Keep controllers thin and HTTP-focused
- Use constructor injection for all dependencies

**Example:**
```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    
    public AuthController(AuthService authService) {
        this.authService = authService;
    }
    
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.authenticate(request));
    }
}
```

### Service Layer
- Encapsulate all business logic
- Orchestrate domain operations and workflows
- Declare service as interface; provide implementation class with `@Service`
- Use constructor injection; mark fields `private final`
- Stateless and testable design
- Apply `@Transactional` at service method level
- Do not expose repository entities directly; use DTOs or domain objects

**Example:**
```java
public interface AuthService {
    AuthResponse authenticate(LoginRequest request);
    void registerUser(RegisterRequest request);
}

@Service
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final JwtTokenProvider tokenProvider;
    private final ApplicationEventPublisher eventPublisher;
    
    public AuthServiceImpl(UserRepository userRepository, 
                         JwtTokenProvider tokenProvider,
                         ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.tokenProvider = tokenProvider;
        this.eventPublisher = eventPublisher;
    }
    
    @Transactional
    @Override
    public void registerUser(RegisterRequest request) {
        // Business logic
        User user = new User(request.getUsername());
        userRepository.save(user);
        eventPublisher.publishEvent(new UserCreatedEvent(user));
    }
}
```

### Repository Layer
- Abstract data access logic
- Extend Spring Data JPA interfaces (`JpaRepository`, `CrudRepository`)
- Keep repository methods simple and focused
- Use `Optional` return types for single entity queries
- Let Spring Data generate implementations

**Example:**
```java
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    List<User> findByActiveTrue();
}
```

### Entity Layer
- JPA domain models representing persistent data
- Embed business rules and constraints
- Use JSR-303/JSR-380 annotations (`@NotBlank`, `@Email`, etc.)
- Define relationships and constraints
- Implement `equals()`, `hashCode()` for identity

**Example:**
```java
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    @NotBlank(message = "Username is required")
    private String username;
    
    @Email
    private String email;
    
    // Constructor, getters, business methods
}
```

### DTO Layer
- Request/Response objects for API contracts
- Input validation using JSR-380 annotations
- Isolated from entity changes; prevents layer leakage
- Use records for immutability (Java 16+) or classes with constructors

**Example:**
```java
public record LoginRequest(
    @NotBlank(message = "Username required") String username,
    @NotBlank(message = "Password required") String password
) {}

public record AuthResponse(
    String token,
    Long userId,
    String username
) {}
```

### Mapper/Converter Layer
- Convert between entities and DTOs
- Isolate presentation concerns from domain models
- Use component-level mappers for module-specific conversions

**Example:**
```java
@Component
public class UserMapper {
    public UserDto toDto(User user) {
        return new UserDto(user.getId(), user.getUsername(), user.getEmail());
    }
    
    public User toEntity(UserDto dto) {
        return new User(dto.getUsername(), dto.getEmail());
    }
}
```

### Exception Handling Layer
- Centralized exception handling via `@RestControllerAdvice`
- Custom exception hierarchy (extend `RuntimeException` for unchecked exceptions)
- Consistent error response format
- Log exceptions with sufficient context for debugging

**Example:**
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex) {
        logger.error("Business error occurred", ex);
        ErrorResponse response = new ErrorResponse(ex.getMessage(), "BUSINESS_ERROR");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        logger.error("Resource not found", ex);
        ErrorResponse response = new ErrorResponse(ex.getMessage(), "NOT_FOUND");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
}

public sealed class BusinessException extends RuntimeException permits {
    public BusinessException(String message) {
        super(message);
    }
}
```

## Design Patterns Application

### Strategy Pattern
Use for multiple implementations of business logic (e.g., multiple authentication providers, payment methods):
```java
public interface AuthenticationStrategy {
    AuthResponse authenticate(String username, String password);
}

@Component
public class JwtAuthenticationStrategy implements AuthenticationStrategy {
    // Implementation
}

@Component
public class OAuthAuthenticationStrategy implements AuthenticationStrategy {
    // Implementation
}
```

### Repository Pattern
Abstract data access layer; facilitate testing with mocks:
```java
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
}
```

### Mapper/Converter Pattern
Isolate DTOs from entities; prevent layer leakage:
```java
@Component
public class UserMapper {
    public UserDto toDto(User user) { /* ... */ }
    public User toEntity(UserDto dto) { /* ... */ }
}
```

### Observer Pattern (Domain Events)
Use `ApplicationEventPublisher` for loose coupling between modules:
```java
// Publisher
@Service
public class AuthServiceImpl {
    private final ApplicationEventPublisher eventPublisher;
    
    public void registerUser(User user) {
        eventPublisher.publishEvent(new UserCreatedEvent(user));
    }
}

// Listener
@Component
public class UserCreatedEventListener {
    @EventListener
    public void onUserCreated(UserCreatedEvent event) {
        // Send welcome email, etc.
    }
}
```

## Configuration Management

- Use YAML for configuration (`application.yml`)
- Leverage Spring profiles for environment-specific settings (dev, test, prod)
- Use `@ConfigurationProperties` for type-safe binding
- Externalize secrets via environment variables

**Example:**
```yaml
# application.yml
app:
  name: Spring Boot Boilerplate
  jwt:
    secret: ${JWT_SECRET}
    expiration: 86400
  security:
    cors:
      allowed-origins: http://localhost:3000
      
# application-prod.yml (production overrides)
app:
  jwt:
    secret: ${JWT_SECRET_PROD}
```

```java
@Configuration
@EnableConfigurationProperties(AppProperties.class)
public class ApplicationConfig {
    // Global beans
}

@ConfigurationProperties(prefix = "app")
public record AppProperties(
    String name,
    JwtProperties jwt,
    SecurityProperties security
) {}
```

## Testing Strategy

```
src/test/java/com/example/spring_boot_boilerplate/
├── [domain-module]/
│   ├── service/
│   │   └── AuthServiceTest.java       # Unit tests (mocked dependencies)
│   ├── controller/
│   │   └── AuthControllerTest.java    # Unit tests (mocked service)
│   └── integration/
│       └── AuthIntegrationTest.java   # Integration tests (real H2/testcontainers)
└── shared/
    └── testutil/
        └── TestDataFactory.java       # Shared test utilities
```

### Unit Testing
- Use `@ExtendWith(MockitoExtension.class)` or `@Mock` annotations
- Mock repositories and external dependencies
- Test service layer logic in isolation
- Fast execution, no database access

### Integration Testing
- Use `@SpringBootTest` with `@Testcontainers` or embedded H2
- Test module workflows end-to-end
- Verify repository queries and transactions
- Slower but comprehensive validation

## Module Addition Pattern

When adding a new feature/module:

1. **Create package structure** under `src/main/java/com/example/spring_boot_boilerplate/[newmodule]/`
2. **Define entity** in `entity/` package
3. **Create repository** extending `JpaRepository`
4. **Define service interface** in `service/` package
5. **Implement service** with business logic and dependency injection
6. **Create DTOs** in `dto/` package
7. **Implement mapper** in `mapper/` package
8. **Create controller** with REST endpoints
9. **Add tests** (unit, integration) in `src/test/`
10. **Publish domain events** for inter-module communication via `ApplicationEventPublisher`

## Dependency Injection Rules

- **Always use constructor injection**: Declare dependencies as parameters in constructor
- **Mark fields `private final`**: All injected fields immutable
- **One service per class**: Limit dependencies to 5-7; refactor if exceeding
- **Inject abstractions**: Depend on interfaces/abstract classes, not implementations
- **Lazy initialization**: Use `@Lazy` for heavy dependencies only when necessary

**Correct:**
```java
@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    
    public UserServiceImpl(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }
}
```

**Incorrect (avoid):**
```java
@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserRepository userRepository;  // Field injection, mutable
    
    @Autowired
    private UserMapper userMapper;
}
```

## Logging

- Use SLF4J with parameterized logging
- Log at appropriate levels: `DEBUG` (detailed flow), `INFO` (business events), `WARN` (recoverable issues), `ERROR` (failures)
- Include context (user ID, request ID) in log messages

**Example:**
```java
private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

logger.debug("Authenticating user: {}", username);
logger.info("User {} successfully authenticated", userId);
logger.warn("Multiple failed login attempts for user {}", username);
logger.error("Database error during authentication for user {}", username, exception);
```

## Scalability Considerations

- **Add new domains**: Create new `[domain-module]` packages without refactoring existing code
- **Shared utilities**: Place cross-module utilities in `shared/` package
- **Event-driven**: Use domain events for loose coupling between modules
- **Async processing**: Use `@Async` for long-running operations, publish events for inter-module notifications
- **Transaction boundaries**: Keep transactions small; use `@Transactional` only on service methods

This structure supports growth from 10K to 100K+ lines while maintaining clarity and testability.
