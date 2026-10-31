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
- The app won't start without datasource properties. They come from a Config Server (default `http://localhost:8089`, `user`/`password`, `optional:`) or env vars. No datasource defaults live in the repo.
- Profiles: `application.yaml` (default), `application-public.yaml` / `application-test.yaml` (Config Server from `CONFIG_SERVER_URL`/`CONFIG_USER`/`CONFIG_PASSWORD`). The Dockerfile uses profile `private`, for which there is no file in this repo.

## Architecture in one screen

`controller` (thin, `@AuthUser UUID`) → `service` (`@Transactional`, all rules, all authorization) → `repository` (Spring Data JPA) → PostgreSQL (Flyway `V1` baseline, then `V2` central-identity transition). Apply `V1` to an empty database. `V2` is intentionally destructive before first production use: it truncates `user_entity` with `CASCADE` to clear old app-owned user IDs and all dependent user data, then drops the obsolete `user_provider` table. Do not run it where existing users or boards must be retained. `BoardMapper` turns entities into the normalized `BoardResponse` (`columns` and `cards` as ID-keyed maps, columns carry `cardIds`). Controllers are mounted at `/v1/...` (no `/api` prefix in this repo).

Other pieces: `UserProvisioningService` (creates this app's local profile for a central identity UUID), `UserService`/`UserController` (`GET /v1/users/me`), `EmailClient` (calls `webtools-email` via Eureka; nothing sends mail right now), Eureka/Config clients, Micrometer tracing to Zipkin. No `spring-boot-starter-security` in the pom and no `SecurityFilterChain`; there is no password or JWT code in this service.

## Authentication and trust boundary

**Read before touching `@AuthUser`, `AuthUserArgumentResolver`, or anything auth-related.**

- Sign-in happens at the **Webtools Identity Server**. It authenticates through configured providers, owns the central person identity and issues signed tokens. The **API gateway** validates token signature, issuer, exact resource audience and application scopes, then replaces caller-provided identity headers with claims from the verified token.
- The verified `X-User-Id` is the central UUID (`sub` / compatibility `uid`). `X-User-Email`, `X-User-Display-Name` and `X-User-Avatar-Url` are also forwarded from signed claims. This service never sees a password or token and never validates JWTs.
- All `/v1/**` endpoints use `@AuthUser UUID`. `AuthUserArgumentResolver` invokes `UserProvisioningService` to create/load a Kanban-local `user_entity` row keyed by that central UUID. The provisioning upsert is idempotent and preserves app-edited display name/avatar values once set.
- Kanban board roles and permissions remain local. Keep every board-scoped permission check in `BoardAccessService`; the identity server never decides whether someone is an OWNER/EDITOR/VIEWER.
- This service must be reachable only through the trusted gateway. If direct access is possible, forged `X-User-*` headers can impersonate users; enforce network isolation at ingress. Do not add ad hoc token parsing to controllers.

## Authorization (RBAC)

- `BoardRole` is declared `VIEWER, EDITOR, OWNER`. `BoardAccessService.requireAtLeast` compares with `ordinal()`, so **don't reorder the enum** without making the comparison explicit.
- **Every board-scoped service method must call `BoardAccessService` before reading or mutating**, after resolving the owning board:
  - `requireMembership(boardId, userId)`: any role; returns it. Use for reads.
  - `requireAtLeast(boardId, userId, minRole)`: throws `ForbiddenBoardAccessException`. Use for mutations.
  - For columns/cards/comments, resolve up to the board first (`column.getBoard().getId()`, `card.getColumn().getBoard().getId()`). There is no shortcut.
- Thresholds: `VIEWER` reads (including comments, archived cards); `EDITOR` column/card CRUD, archive/restore, board rename and import, **adding comments**; `OWNER` board deletion and member management. Leaving a board (removing yourself) needs no role. The last `OWNER` can't be removed or demoted. Deleting a comment: its author or an `OWNER`.
- **Known gap:** `CardService.moveCard` and `ColumnService.deleteColumn(transferTo)` load the destination column by ID and check the role only on the *source* board. They don't verify the destination column belongs to the same board, so an `EDITOR` who knows a foreign column UUID could move cards across boards. If you touch these, add that check (and a test).

## Error handling: read this before relying on status codes

`GlobalExceptionHandler` has explicit handlers for `ResourceNotFoundException` (404), `ColumnNotEmptyException` (409, includes `cardCount`), `InvalidImportException` (400) and `MethodArgumentNotValidException` (400, `{field: message}`), **plus a catch-all `@ExceptionHandler(RuntimeException.class)` → 500**. Spring runs `@ExceptionHandler` resolution before `@ResponseStatus`, so `BadRequestException`, `ForbiddenBoardAccessException`, the `ResponseStatusException(401)` from the resolver, malformed JSON and bad path UUIDs all most likely end up as `500 INTERNAL_SERVER_ERROR` despite their annotations. Existing tests only assert that services throw; none exercise the HTTP layer. When changing or relying on error behavior, add a `@WebMvcTest`/integration test first, or add specific handlers (403/400/401) ahead of the catch-all.

Also: "last owner" violations throw `BadRequestException`, not a 409; `deleteColumn` same-column transfer throws `IllegalArgumentException`; comments with `null` content aren't rejected by validation (`CommentRequest` has only `@Size`).

## Conventions

- **Naming:** `[Entity]Entity`, `[Entity]Repository`, `[Entity]Service`, `[Entity]Controller`, DTOs `[Action][Entity]Request` / `[Entity]Response`.
- **Patterns:** Lombok for boilerplate; services own logic and `@Transactional`; controllers stay thin; one `@RestControllerAdvice`; mutating endpoints return the updated resource (deletes return `204`).
- **IDs:** The Kanban-local `UserEntity.id` is assigned from the central identity UUID. Other entity IDs are DB-generated via `uuidv7()` from `V1` (entities use `@Generated(INSERT)`, `insertable=false`). Exception: `BoardMemberEntity` IDs are assigned in code with `UUID.randomUUID()` (v4).
- **Schema changes:** add a new `V<next>__description.sql`; never edit applied migrations. (The original `V1`–`V6` were flattened into one `V1` while the database was still empty; that was a one-off.) Schema is managed by Flyway.
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
- **Nothing sends email today.** The password-reset flow (and `EmailService`, the event and its listener) was removed with the password login. `EmailClient`, `EmailClientConfig` and the bounded `emailExecutor` remain for future features such as invitations. If you add a sender, keep it best-effort and after commit, and **never log tokens or message bodies**; for guaranteed delivery use an outbox table plus a scheduled dispatcher.

## Testing

- Existing tests are Mockito unit tests (`@ExtendWith(MockitoExtension.class)`, `@InjectMocks` service under test, mocked repositories and `BoardAccessService`), plus `CardContentSizeValidationTest` (plain Validator). Follow this style for service logic.
- Testcontainers (`spring-boot-testcontainers`, `postgresql`) is on the classpath but unused; use it for repository/migration/controller integration tests if you add them. There are no `@SpringBootTest`/`@WebMvcTest` tests today, so HTTP behavior and Flyway migrations are untested.
- When adding a service method that touches a board, add a test that the right `BoardAccessService` method and minimum role are called, and that a `ForbiddenBoardAccessException` propagates.

## CI / deploy

`.github/workflows/maven.yml` (push/PR to `master`): JDK 21 → `mvn -B clean verify` + SonarCloud → Docker Hub login → build and push `ikoyski/webtools-kanban-backend:latest`. The Docker steps have no branch condition, so **pull-request runs also push `latest`**. Dockerfile: `eclipse-temurin:21-jre-jammy`, `ADD target/*.jar`, `-Dspring.profiles.active=private`, port 8080.

## Don'ts

- Don't add token validation to individual controllers or trust `X-User-Id` more than the docs above say.
- Don't skip `BoardAccessService`, or check the role against the wrong board (always resolve the *owning* board; mind destination columns).
- Don't reorder `BoardRole`, and don't edit applied Flyway migrations.
- Don't sanitize or transform rich-text fields server-side without cross-repo agreement.
- Don't log tokens, secrets or email bodies, and don't route the email client through the gateway.
- Don't store provider subjects in this database or merge identities by email; the central Identity Server owns provider identities.
- Don't commit real secrets: the compose password is dev-only; everything else belongs in Config Server or env.


### Board member profile responses

- The board members API response includes `avatarUrl` alongside `userId`, `email`, `displayName` and `role`.
- Map `avatarUrl` from `UserEntity.avatarUrl` in list, add-member and role-update responses so clients can consistently display each member's profile image.
