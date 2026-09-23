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
- **API**: RESTful, versioned at `/api/v1` (Supports multi-board RBAC: OWNER, EDITOR, VIEWER)
- **Auth**: BCrypt password hashing (Spring Security's `PasswordEncoder`) + JWT issuance (`jjwt`).
- **Bot Protection**: Cloudflare Turnstile verification implemented in `TurnstileService` for all `/v1/auth/*` endpoints.
- **Package**: `com.ikoyki.webtools.kanban.backend`

## Build & Run Commands
- **Build**: `mvn clean compile`
- **Run**: `mvn spring-boot:run`
- **Test**: `mvn test`
- **DB Setup**: `docker compose up -d`

## Authentication & Trust Boundary
**Read this before touching `@AuthUser`, `AuthUserArgumentResolver`, or anything auth-related.**

- **Bot Defense**: Public authentication endpoints (`/v1/auth/signup`, `/v1/auth/login`) are protected by Cloudflare Turnstile. The `TurnstileService` validates the client token against Cloudflare's API before any credential check or user creation occurs.
- `POST /v1/auth/signup` / `POST /v1/auth/login` issue a JWT (`JwtTokenProvider`), but **this
  service does not validate that JWT on subsequent requests.** There is no `SecurityFilterChain`
  and no JWT filter — `SecurityConfig` only defines the `PasswordEncoder` bean.
- Every other endpoint resolves the caller via `@AuthUser`, which reads the `X-User-Id` header
  (`AuthUserArgumentResolver`) and just checks it's a well-formed UUID that matches a real
  `user_entity` row. It does **not** verify the request was actually made by that user.
- This is intentional: an API gateway in front of this service is expected to validate the JWT
  and forward trusted `X-User-Id` / `X-User-Email` headers on every proxied request.
- **Consequence**: if this service is ever reachable directly (bypassing the gateway), anyone can
  forge `X-User-Id` and impersonate any user. This must be enforced at the network/ingress level
  (internal-only service, security group, private subnet, etc.) — it is not something application
  code here can fix on its own. Do not "fix" this by adding ad hoc JWT validation to individual
  controllers; if this needs to change, it's an architecture decision (see `backend-plan.md`),
  not a local patch.
  Note: The initial identity creation and authentication endpoints (/signup, /login) are gated by TurnstileService to prevent automated bot registrations and brute-force attacks.

## Authorization (RBAC)
- Roles: `BoardRole` enum, declared `VIEWER, EDITOR, OWNER` — the declaration order matters,
  since `BoardAccessService.requireAtLeast` compares roles via `ordinal()`. Do not reorder this
  enum without updating that comparison to something explicit.
- **Every board-scoped service method must call `BoardAccessService` before reading or mutating
  anything**, resolving the owning board first:
  - `boardAccessService.requireMembership(boardId, userId)` — any role (including VIEWER) passes;
    use for read-only access.
  - `boardAccessService.requireAtLeast(boardId, userId, minRole)` — throws
    `ForbiddenBoardAccessException` if the caller's role is below `minRole`; use for mutations.
  - For nested resources (columns, cards), resolve up to the owning board first
    (`column.getBoard().getId()`, `card.getColumn().getBoard().getId()`) before calling either
    method — there is no shortcut around this per-method resolution.
- Standing role thresholds: `VIEWER` for all reads; `EDITOR` for column/card CRUD, board rename,
  and board import; `OWNER` for board deletion and all member management (add/role-change/remove
  someone else). Removing yourself from a board ("leave") does not require a role check, but
  removing/demoting the *last* `OWNER` on a board is always blocked.

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
    - Use `UUID` with `uuidv7()` (database-generated) for primary keys across all entities.
    - Board-scoped services depend on `BoardAccessService` and call it before any read/write —
      see "Authorization (RBAC)" above.
- **Ordering Logic**:
    - Use simple sequential integers (`0, 1, 2...`) for `position`.
    - Re-index fully on every mutation that changes order.
- **API Design**:
    - All mutating endpoints return the updated resource.
    - Return `404 Not Found` for missing resources.
    - Return `409 Conflict` for `ColumnNotEmptyException` and for role-related conflicts (e.g.
      demoting/removing the last board `OWNER`).
    - Return `403 Forbidden` (`ForbiddenBoardAccessException`) when a role check fails.
    - Return `400 Bad Request` for validation/import errors.
