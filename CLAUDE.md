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
- **Auth**: BCrypt password hashing (Spring Security's `PasswordEncoder`) + JWT issuance (`jjwt`). Supports local signup, login, password changes, and secure "forgot password" reset flows.
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

## Rich Text (`description` / comment `content`)
- `CardEntity.description` and `CardCommentEntity.content` are plain `TEXT` columns. This
  service treats them as opaque strings end-to-end: no HTML escaping or sanitization in
  `CardService`/`CommentService`, no transformation in `BoardMapper`. What goes in via
  `PATCH`/`POST` is exactly what comes back out in the JSON response — the one exception is the
  length cap below, which rejects oversized input but never alters valid input.
- The frontend writes a small, fixed subset of HTML here (its Quill editor's output, cleaned up
  client-side) and sanitizes with DOMPurify on every render — see the frontend's `CLAUDE.md`.
  **Do not add server-side sanitization that assumes a particular HTML shape** (e.g. stripping
  tags, enforcing the frontend's allow-list) without checking both repos' `CLAUDE.md` first —
  the frontend's sanitize step is what actually protects renders, and it runs independently of
  whatever is in the database.
- If you add a new client of this API (e.g. an MCP server) that writes to these fields, it may
  write plain text or its own HTML — this service won't object either way — but whoever renders
  the value as HTML is responsible for sanitizing it first, since this service doesn't.
- **Size limit**: `CreateCardRequest`/`UpdateCardRequest#description` and
  `CommentRequest#content` carry `@Size(max = ...)` — 50,000 chars for a description, 10,000
  for a comment. `CardController#updateCardEntity` and `CommentController#addComment` must keep
  `@Valid` on the request body for this to fire (both were missing it before this limit was
  added; `createCardEntity` and `moveCardEntity` already had it). A violation is caught by the
  existing `GlobalExceptionHandler#handleValidation` and returned as `400` with a
  `{"description": "size must be..."}`-shaped body, same as any other `@Size`/`@NotBlank`
  failure in this codebase — no new error-handling path was added. These limits are purely
  defensive (oversized-payload protection); they are not a statement about what counts as valid
  HTML, and raising them is safe at any time since the DB column is unbounded `TEXT`.
  Covered by `CardContentSizeValidationTest`.

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

## Email
- Outgoing email goes through `EmailClient` (declarative `@HttpExchange`, `POST /email/v1`) to the `webtools-email`
  microservice via Eureka (`@LoadBalanced RestClient.Builder` in `EmailClientConfig`). Never route this
  back through the API gateway.
- Do **not** mark the shared `RestTemplate` bean (used by `TurnstileService` for Cloudflare) as
  `@LoadBalanced`; the email client has its own builder.
- Send path is best-effort: services publish an event (e.g. `PasswordResetRequestedEvent`) inside
  their transaction; `EmailService` handles it `@Async` + `AFTER_COMMIT`. Never log tokens or bodies.
  If delivery must become reliable, replace this with an outbox table + scheduled dispatcher.
