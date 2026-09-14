# CLAUDE.md - WebTools Kanban Backend

## Project Overview
Spring Boot 3 backend for a Kanban board application.
- **Java Version**: 21
- **Framework**: Spring Boot 3.4.2
- **Cloud**: Spring Cloud (Eureka, Config)
- **Database**: PostgreSQL 17 (using JSONB for labels)
- **Observability**: Micrometer Tracing (Brave, Zipkin)
- **Quality Analysis**: Monitored via SonarCloud for bugs, vulnerabilities, and code smells.
- **Schema Management**: Flyway
- **API**: RESTful, versioned at `/api/v1`
- **Package**: `com.ikoyki.webtools.kanban.backend`

## Build & Run Commands
- **Build**: `mvn clean compile`
- **Run**: `mvn spring-boot:run`
- **Test**: `mvn test`
- **DB Setup**: `docker compose up -d`

## Coding Guidelines
- **Naming**: 
    - Entities: `PascalCase` (e.g., `BoardEntity`, `ColumnEntity`, `CardEntity`)
    - Repositories: `[Entity]Repository`
    - Services: `[Entity]Service`
    - Controllers: `[Entity]Controller`
    - DTOs: `[Action][Entity]Request` / `[Entity]Response`
- **Patterns**:
    - Use Lombok for boilerplate (`@Data`, `@Builder`, `@RequiredArgsConstructor`).
    - Service layer handles all business logic and transactions (`@Transactional`).
    - Controllers are thin and delegate to services.
    - Global error handling via `@RestControllerAdvice`.
    - Use `UUID` with `uuidv7()` (database-generated) for primary keys of columns and cards.
- **Ordering Logic**:
    - Use simple sequential integers (`0, 1, 2...`) for `position`.
    - Re-index fully on every mutation that changes order.
- **API Design**:
    - All mutating endpoints return the updated resource.
    - Return `404 Not Found` for missing resources.
    - Return `409 Conflict` for `ColumnNotEmptyException`.
    - Return `400 Bad Request` for validation/import errors.
