# CLAUDE.md: WebTools Kanban Backend

Spring Boot 3.4.2 / Java 21 REST API for a multi-board Kanban app. Package root: `com.ikoyki.webtools.kanban.backend`. Human-facing docs (endpoints, config, setup) are in `README.md`; this file is the working guide: commands, invariants, conventions, and known gaps. Verify claims against the code before relying on them; the code wins over any doc.

## Commands

```bash
docker compose up -d                  # Postgres 17 on :5432 (db/user kanban, pw kanban_dev_only)
./mvnw spring-boot:run                # run (needs config, see below)
./mvnw test                           # unit tests
./mvnw test -Dtest=CardServiceTest    # one class
./mvnw -B clean verify                # CI build (JaCoCo + sortpom; sortpom may rewrite pom.xml)
```

- Builds from a source archive without `.git` can fail in `git-commit-id-plugin`; use a clone.
- The app won't start without datasource, JWT, Turnstile and `webtools.frontend.reset-password-url` properties. They come from a Config Server (default `http://localhost:8089`, `user`/`password`, `optional:`) or env vars. No defaults live in the repo. `webtools.frontend.reset-password-url` has **no default** (`@Value` without fallback). Env-var example is in `README.md`.
- Profiles: `application.yaml` (default), `application-public.yaml` / `application-test.yaml` (Config Server from `CONFIG_SERVER_URL`/`CONFIG_USER`/`CONFIG_PASSWORD`). The Dockerfile uses profile `private`, for which there is no file in this repo.

## Architecture in one screen

`controller` (thin, `@AuthUser UUID`) → `service` (`@Transactional`, all rules, all authorization) → `repository` (Spring Data JPA) → PostgreSQL (Flyway `V1`–`V6`). `BoardMapper` turns entities into the normalized `BoardResponse` (`columns` and `cards` as ID-keyed maps, columns carry `cardIds`). Controllers are mounted at `/v1/...` (no `/api` prefix in this repo).

Other pieces: `TurnstileService` (Cloudflare siteverify via the plain `RestTemplate` bean), `JwtTokenProvider` (jjwt, HMAC), `EmailClient` + `EmailService` (calls `webtools-email` via Eureka), Eureka/Config clients, Micrometer tracing to Zipkin. No `spring-boot-starter-security` in the pom: only the crypto module (BCrypt) is used, and there is no `SecurityFilterChain`.

## Authentication and trust boundary

**Read before touching `@AuthUser`, `AuthUserArgumentResolver`, or anything auth-related.**

- Signup/login issue a JWT, but **this service never validates one**. `SecurityConfig` only defines `PasswordEncoder` and `RestTemplate` beans.
- All other endpoints use `@AuthUser UUID`, resolved by `AuthUserArgumentResolver` from the `X-User-Id` header: it must be a UUID and exist in `user_entity`. That is identity *by assertion*; `X-User-Email` is only logged.
- By design a gateway validates the JWT and injects these headers. If the service is reachable directly, anyone can impersonate anyone. That must be fixed at network/ingress level, not in controllers. Don't add ad hoc JWT checks to individual endpoints; changing this is an architecture decision, not a local patch. (A `backend-plan.md` was once referenced here; it is not in the repo.)
- **Turnstile** is enforced on `signup`, `login` and `forgot-password` only (via `TurnstileService`, before any credential check or user creation). `PATCH /password` and `POST /reset-password` do not use it. `forgot-password` passes `"unknown"` as the remote IP. Controllers pass `getRemoteAddr()`, which behind a proxy is the proxy's address.
- `signup`/`login` controllers don't use `@Valid`, so the `@NotBlank` annotations on `RegisterRequest`/`LoginRequest` are not enforced there. `ResetPasswordRequest.newPassword` has no length rule (unlike `ChangePasswordRequest`, min 8).
- `forgot-password` always returns `202` (no account enumeration); `signup` does reveal "Email already in use".

## Authorization (RBAC)

- `BoardRole` is declared `VIEWER, EDITOR, OWNER`. `BoardAccessService.requireAtLeast` compares with `ordinal()`, so **don't reorder the enum** without making the comparison explicit.
- **Every board-scoped service method must call `BoardAccessService` before reading or mutating**, after resolving the owning board:
  - `requireMembership(boardId, userId)`: any role; returns it. Use for reads.
  - `requireAtLeast(boardId, userId, minRole)`: throws `ForbiddenBoardAccessException`. Use for mutations.
  - For columns/cards/comments, resolve up to the board first (`column.getBoard().getId()`, `card.getColumn().getBoard().getId()`). There is no shortcut.
- Thresholds: `VIEWER` reads (including comments, archived cards); `EDITOR` column/card CRUD, archive/restore, board rename and import, **adding comments**; `OWNER` board deletion and member management. Leaving a board (removing yourself) needs no role. The last `OWNER` can't be removed or demoted. Deleting a comment: its author or an `OWNER`.
- **Known gap:** `CardService.moveCard` and `ColumnService.deleteColumn(transferTo)` load the destination column by ID and check the role only on the *source* board. They don't verify the destination column belongs to the same board, so an `EDITOR` who knows a foreign column UUID could move cards across boards. If you touch these, add that check (and a test).

## Error handling: read this before relying on status codes

`GlobalExceptionHandler` has explicit handlers for `ResourceNotFoundException` (404), `ColumnNotEmptyException` (409, includes `cardCount`), `InvalidImportException` (400) and `MethodArgumentNotValidException` (400, `{field: message}`), **plus a catch-all `@ExceptionHandler(RuntimeException.class)` → 500**. Spring runs `@ExceptionHandler` resolution before `@ResponseStatus`, so `BadRequestException`, `ForbiddenBoardAccessException`, `BadCredentialsException`, the `ResponseStatusException(401)` from the resolver, malformed JSON and bad path UUIDs all most likely end up as `500 INTERNAL_SERVER_ERROR` despite their annotations. Existing tests only assert that services throw; none exercise the HTTP layer. When changing or relying on error behavior, add a `@WebMvcTest`/integration test first, or add specific handlers (403/400/401) ahead of the catch-all.

Also: "last owner" violations throw `BadRequestException`, not a 409; `deleteColumn` same-column transfer throws `IllegalArgumentException`; comments with `null` content aren't rejected by validation (`CommentRequest` has only `@Size`).

## Conventions

- **Naming:** `[Entity]Entity`, `[Entity]Repository`, `[Entity]Service`, `[Entity]Controller`, DTOs `[Action][Entity]Request` / `[Entity]Response`.
- **Patterns:** Lombok for boilerplate; services own logic and `@Transactional`; controllers stay thin; one `@RestControllerAdvice`; mutating endpoints return the updated resource (deletes return `204`).
- **IDs:** DB-generated via the `uuidv7()` function from `V1` (entities use `@Generated(INSERT)`, `insertable=false`). Exception: `BoardMemberEntity` IDs are assigned in code with `UUID.randomUUID()` (v4).
- **Schema changes:** add a new `V<next>__description.sql`; never edit applied migrations (`V4`→`V5` is the precedent: a fix as a new migration). Schema is managed by Flyway.
- **Ordering (`position`):** plain integers `0..n-1`. Card create/delete/move and column-transfer re-index the affected columns. But: column deletion doesn't re-index remaining columns, archive/restore don't re-index, `reorderColumns` stores the client's positions as given, and import uses the payload's column positions. Archived cards stay in their column's position sequence and are still returned by `findByColumnIdOrderByPositionAsc`, so they count toward "column not empty" and get transferred with the rest.
- **Partial updates:** `PATCH /cards/{id}` ignores `null` fields, so `dueDate` can't be cleared; clearing `description` works by sending `""`.
- **Read DTO quirk:** `BoardMapper` omits archived cards from the `cards` map but not from a column's `cardIds`. Don't "fix" one side without updating the frontend contract.
- `listBoardsForUser` reports `createdAt` as `updatedAt` (no real `updated_at` on boards).

## Import (`PUT /boards/{id}/import`)

`BoardService.importBoard` is transactional and destructive: it bulk-deletes all cards (archived too, comments via FK cascade) and columns of the board, then recreates them with **new IDs** from the payload. `request.getCards()` isn't null-checked (a payload with `columns` but no `cards` fails with an NPE → 500). Card dates are parsed with `LocalDate.parse` (bad format → 500). Keep it atomic.

## Rich text (`description` / comment `content`)

- Both are plain `TEXT`, treated as **opaque strings** end to end: no sanitization or escaping in `CardService`, `CommentService` or `BoardMapper`. What goes in is what comes out.
- The frontend writes a small HTML subset (Quill) and sanitizes with DOMPurify at render time. **Don't add server-side sanitization that assumes a particular HTML shape** without checking the frontend repo's `CLAUDE.md`. New clients (e.g. an MCP server) may write plain text or HTML; whoever renders it as HTML must sanitize.
- Only guard: `@Size(max = 50_000)` on card `description` (`CreateCardRequest`, `UpdateCardRequest`), `@Size(max = 10_000)` on `CommentRequest#content`. It only fires if the controller parameter has `@Valid` (`CardController#updateCardEntity`, `CommentController#addComment` must keep it). Violations return `400 {"field": "..."}` through `handleValidation`. Limits are defensive and safe to raise (the DB column is unbounded). Covered by `CardContentSizeValidationTest`.

## Email

- `EmailClient` is a declarative `@HttpExchange("/email/v1")` client built in `EmailClientConfig` on a **`@LoadBalanced` `RestClient.Builder`** so `http://webtools-email` resolves through Eureka. Never route this through the API gateway.
- The plain `RestTemplate` bean (used by `TurnstileService` for Cloudflare) must **not** be `@LoadBalanced`; the email client has its own builder.
- Send path is best-effort: services publish an event (`PasswordResetRequestedEvent`) inside their transaction; `EmailService` handles it with `@Async("emailExecutor")` + `@TransactionalEventListener(AFTER_COMMIT)`. The executor is bounded (core 2, max 4, queue 100) and drops on overflow. Failures are logged, never thrown. **Never log tokens or message bodies** (note `EmailService` currently logs the recipient and subject). For guaranteed delivery, replace this with an outbox table plus a scheduled dispatcher.

## Testing

- Existing tests are Mockito unit tests (`@ExtendWith(MockitoExtension.class)`, `@InjectMocks` service under test, mocked repositories and `BoardAccessService`), plus `CardContentSizeValidationTest` (plain Validator). Follow this style for service logic.
- Testcontainers (`spring-boot-testcontainers`, `postgresql`) is on the classpath but unused; use it for repository/migration/controller integration tests if you add them. There are no `@SpringBootTest`/`@WebMvcTest` tests today, so HTTP behavior and Flyway migrations are untested.
- When adding a service method that touches a board, add a test that the right `BoardAccessService` method and minimum role are called, and that a `ForbiddenBoardAccessException` propagates.

## CI / deploy

`.github/workflows/maven.yml` (push/PR to `master`): JDK 21 → `mvn -B clean verify` + SonarCloud → Docker Hub login → build and push `ikoyski/webtools-kanban-backend:latest`. The Docker steps have no branch condition, so **pull-request runs also push `latest`**. Dockerfile: `eclipse-temurin:21-jre-jammy`, `ADD target/*.jar`, `-Dspring.profiles.active=private`, port 8080.

## Don'ts

- Don't add JWT validation to individual controllers or trust `X-User-Id` more than the docs above say.
- Don't skip `BoardAccessService`, or check the role against the wrong board (always resolve the *owning* board; mind destination columns).
- Don't reorder `BoardRole`, and don't edit applied Flyway migrations.
- Don't sanitize or transform rich-text fields server-side without cross-repo agreement.
- Don't make the Turnstile `RestTemplate` load-balanced, and don't log secrets, reset tokens or email bodies.
- Don't commit real secrets: the compose password is dev-only; everything else belongs in Config Server or env.
