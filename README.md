# WebTools Kanban Backend

REST API for the WebTools Kanban app: multiple boards per user, board-level roles (`VIEWER` / `EDITOR` / `OWNER`), ordered columns and cards, card comments and archiving, and whole-board import/export. Authentication is centralized in the Webtools Identity Server; this service owns only Kanban-local profiles and application data.

It is one service in a small microservice setup. It registers with Eureka, pulls its configuration from a Spring Cloud Config Server, and **expects an API gateway in front of it** that validates JWTs and forwards the caller's identity in an `X-User-Id` header. See [Trust model](#trust-model) before exposing it anywhere.

## Contents

- [Tech stack](#tech-stack)
- [Trust model](#trust-model)
- [Getting started](#getting-started)
- [Configuration](#configuration)
- [API reference](#api-reference)
- [Roles and permissions](#roles-and-permissions)
- [Error responses](#error-responses)
- [Data model](#data-model)
- [Board import and export](#board-import-and-export)
- [Rich text fields](#rich-text-fields)
- [Email](#email)
- [Testing, CI and Docker](#testing-ci-and-docker)
- [Project layout](#project-layout)

## Tech stack

| Area | Choice |
|---|---|
| Language / build | Java 21, Maven (wrapper included: `./mvnw`) |
| Framework | Spring Boot 3.4.2 (Web, Data JPA, Validation, Actuator) |
| Service infra | Spring Cloud 2024.0.0: Eureka client, Config client, LoadBalancer |
| Database | PostgreSQL 17, Flyway migrations, `JSONB` for card labels |
| Identity | Central identity UUID supplied by the trusted gateway; local Kanban profile provisioning. No provider subjects, passwords or JWT handling |
| API docs | springdoc-openapi 2.8.5 (Swagger UI) |
| Observability | Actuator, Micrometer Tracing (Brave) with Zipkin reporter |
| Quality | JUnit 5 + Mockito, JaCoCo, SonarCloud in CI |

## Trust model

This service **does not validate JWTs** on incoming requests. There is no security filter chain and no JWT filter.

- Sign-in is handled by a separate auth server, and tokens are validated by the gateway. This service never sees a password or a token.
- The gateway validates the Auth Server's JWT signature, issuer, expiry, exact Kanban API audience and required Kanban scopes, then replaces caller-supplied identity headers with values from the verified token: `X-User-Id`, `X-User-Email`, `X-User-Display-Name` and `X-User-Avatar-Url`.
- `X-User-Id` is the stable central identity UUID (`sub` / compatibility `uid`), not a provider subject. `AuthUserArgumentResolver` provisions a Kanban-local `user_entity` row on first authenticated API use, keyed by that UUID.
- Board roles and all business permissions remain local to Kanban. This service never accepts provider identity data and does not call the Auth Server's identity database.

**If this service is reachable without going through the gateway, anyone can impersonate any user by sending forged headers.** Restrict it at the network level (private subnet, security group, internal-only ingress).

## Getting started

### Prerequisites

- JDK 21
- Docker and Docker Compose (for PostgreSQL)
- Optionally a running Spring Cloud Config Server and Eureka (see below)

### 1. Start PostgreSQL

```bash
docker compose up -d
```

This starts `postgres:17-alpine` on `localhost:5432` with database `kanban`, user `kanban`, password `kanban_dev_only`, and a named volume `kanban_pg_data`. Flyway creates the schema on first start.

### 2. Provide configuration

The repository deliberately contains **no datasource URL, internal API key or Eureka settings**; they come from the Config Server (or environment). With no config in place the app will not start. Pick one:

**Option A: Config Server.** The default `application.yaml` imports `optional:configserver:http://localhost:8089` with basic-auth `user` / `password`. Run a Config Server there that serves the properties in [Configuration](#configuration) for application `webtools-kanban-backend`.

**Option B: environment variables only.** Spring's relaxed binding maps each property to an env var. The Config Server import is `optional:`, so an unreachable server only logs a warning.

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/kanban
export SPRING_DATASOURCE_USERNAME=kanban
export SPRING_DATASOURCE_PASSWORD=kanban_dev_only
export EUREKA_CLIENT_ENABLED=false
```

### 3. Run

```bash
./mvnw spring-boot:run
```

The server listens on port 8080 unless your config says otherwise.

| What | URL |
|---|---|
| Health | `http://localhost:8080/actuator/health` |
| Swagger UI | `http://localhost:8080/swagger-ui/index.html` |
| API | `http://localhost:8080/v1/...` |

Notes:

- Controllers are mounted at **`/v1/...`**. Nothing in this repo adds an `/api` prefix. If your deployment exposes `/api/v1/...`, that prefix comes from the gateway or from Config Server settings.
- Outside a git checkout, the build can fail in the `git-commit-id` plugin because it looks for a `.git` directory. Build from a clone.
- `mvn verify` runs `sortpom`, which may reformat `pom.xml`.

## Configuration

Spring profiles and the config files in `src/main/resources`:

| File / profile | Behaviour |
|---|---|
| `application.yaml` (default) | App name `webtools-kanban-backend`; Config Server at `http://localhost:8089`, credentials `user` / `password` |
| `application-public.yaml`, `application-test.yaml` | Config Server location and credentials from `CONFIG_SERVER_URL`, `CONFIG_USER`, `CONFIG_PASSWORD` |
| `private` (set by the Dockerfile) | No `application-private.yaml` exists in this repo; supply the settings externally (mounted file, env vars, or Config Server) |

Properties this service needs:

| Property | Required | Purpose |
|---|---|---|
| `spring.datasource.url` / `username` / `password` | yes | PostgreSQL connection |
| `webtools.email.base-url` | no | Eureka name of the email service; default `http://webtools-email` |
| `webtools.email.api-key` | no | Sent as `X-Internal-Api-Key` when set |
| `webtools.email.connect-timeout` / `read-timeout` | no | Defaults `2s` / `5s` |
| `eureka.client.*`, `management.zipkin.*`, `server.port` | as needed | Service discovery, tracing, port |

## API reference

All paths below are relative to the service root (e.g. `/v1/boards`). Every `/v1` request needs an `X-User-Id` header. Interactive docs are in Swagger UI.

### Users: `/v1/users`

| Method | Path | Notes |
|---|---|---|
| GET | `/me` | Needs `X-User-Id`. Returns `{id, email, displayName, avatarUrl}` |

### Boards: `/v1/boards`

| Method | Path | Min role | Notes |
|---|---|---|---|
| GET | `/` | member | Boards the caller belongs to, with their role |
| POST | `/` | any user | Body: `name`. Caller becomes `OWNER` |
| GET | `/{boardId}` | `VIEWER` | Full normalized board state |
| GET | `/{boardId}/export` | `VIEWER` | Same payload as GET (see [import/export](#board-import-and-export)) |
| PUT | `/{boardId}/import` | `EDITOR` | Replaces all columns and cards |
| PATCH | `/{boardId}` | `EDITOR` | Rename. Body: `name` |
| DELETE | `/{boardId}` | `OWNER` | `204`; cascades to columns, cards, members |

### Members: `/v1/boards/{boardId}/members`

| Method | Path | Min role | Notes |
|---|---|---|---|
| GET | `/` | `VIEWER` | `{userId, email, displayName, avatarUrl, role}` per member |
| POST | `/` | `OWNER` | Body: `email`, `role`. The user must already have an account |
| PATCH | `/{userId}` | `OWNER` | Body: `role` |
| DELETE | `/{userId}` | `OWNER`, or yourself | Any member can leave; the last `OWNER` can't be removed or demoted |

### Columns: `/v1/columns`

| Method | Path | Min role | Notes |
|---|---|---|---|
| POST | `/` | `EDITOR` | Body: `boardId`, `title`. Appended at the end |
| PATCH | `/{id}` | `EDITOR` | Body: `title` |
| PATCH | `/reorder` | `EDITOR` | Body: `{columns: [{id, position}]}`; all columns must be on one board |
| DELETE | `/{id}?transferTo={columnId}` | `EDITOR` | If the column has cards, `transferTo` is required, otherwise `409 COLUMN_NOT_EMPTY` |

### Cards: `/v1/cards`

| Method | Path | Min role | Notes |
|---|---|---|---|
| POST | `/` | `EDITOR` | Body: `columnId`, `title`, optional `description`, `priority` (`LOW`/`MEDIUM`/`HIGH`, default `MEDIUM`), `dueDate`, `labels`. Appended to the column |
| PATCH | `/{id}` | `EDITOR` | Partial update: omitted/`null` fields are left unchanged (so `dueDate` can't be cleared this way) |
| DELETE | `/{id}` | `EDITOR` | `204`; re-indexes the column |
| PATCH | `/{id}/move` | `EDITOR` | Body: `columnId`, `position`; within or across columns |
| POST | `/{id}/archive` | `EDITOR` | Sets `archived` and `archivedAt` |
| POST | `/{id}/restore` | `EDITOR` | Clears them |
| GET | `/board/{boardId}/archived` | `VIEWER` | Archived cards of a board |

### Comments: `/v1/comments`

| Method | Path | Min role | Notes |
|---|---|---|---|
| GET | `/card/{cardId}` | `VIEWER` | Oldest first; includes `userName` |
| POST | `/card/{cardId}` | `EDITOR` | Body: `content` |
| DELETE | `/{id}` | member | Allowed for the comment's author or a board `OWNER` |

## Roles and permissions

Roles are ordered `VIEWER < EDITOR < OWNER`. A board always has at least one `OWNER`.

| Action | Minimum role |
|---|---|
| View board, export, list members, list archived cards, read comments | `VIEWER` |
| Create/edit/move/delete/archive/restore cards; create/rename/reorder/delete columns; rename or import a board; add comments | `EDITOR` |
| Delete board; add members, change roles, remove other members | `OWNER` |
| Leave a board (remove yourself) | any member |
| Delete a comment | its author, or `OWNER` |

## Error responses

Handled by `GlobalExceptionHandler`.

| Situation | Status | Body |
|---|---|---|
| Resource not found | `404` | `{"error": "NOT_FOUND", "message": "..."}` |
| Non-empty column deleted without `transferTo` | `409` | `{"error": "COLUMN_NOT_EMPTY", "message": "...", "cardCount": N}` |
| Invalid import payload | `400` | `{"error": "INVALID_IMPORT", "message": "..."}` |
| Bean Validation failure (`@Valid`) | `400` | `{"<field>": "<message>"}` |
| Any other `RuntimeException` | `500` | `{"error": "INTERNAL_SERVER_ERROR", "message": "..."}` |

**Known issue:** the catch-all `RuntimeException` handler also swallows exceptions that carry their own `@ResponseStatus` (`BadRequestException`, `ForbiddenBoardAccessException`, `BadCredentialsException`) and the `ResponseStatusException` used for missing/invalid `X-User-Id`. Spring resolves `@ExceptionHandler` methods before `@ResponseStatus`, so these likely come back as `500 INTERNAL_SERVER_ERROR` with the real reason in `message`, rather than the `400` / `403` / `401` their declarations suggest. No test covers the HTTP layer, so verify against a running instance before relying on specific codes for these cases.

## Data model

Flyway migrations in `src/main/resources/db/migration` (`V1`–`V6`).

| Table | Purpose |
|---|---|
| `user_entity` | Users: unique `email`, `display_name`, `avatar_url` |
| `board` | Board name, `created_by`, `created_at` |
| `board_member` | `(board_id, user_id)` unique, `role` in `OWNER`/`EDITOR`/`VIEWER` |
| `column_entity` | Columns of a board, ordered by integer `position` |
| `card` | Cards of a column: `description`, `priority`, `due_date`, `labels` (`JSONB` array of strings), `position`, `archived`, `archived_at` |
| `card_comment` | Comments on a card, ordered by `created_at` |

The Kanban-local `user_entity.id` is assigned from the central identity UUID; other application data IDs default to `uuidv7()` from `V1`. `V2` drops the former app-owned `user_provider` table because provider-subject mappings now live only in the Identity Server. The flattened `V1` must be applied to an empty database; older pre-flattened databases may need a reset as described by their migration history. Deleting a board cascades to its columns, cards and comments.

## Board import and export

`GET /boards/{id}` and `/export` return a normalized shape that `PUT /boards/{id}/import` accepts:

```jsonc
{
  "id": "…", "name": "Roadmap", "role": "OWNER",
  "columns": { "<columnId>": { "id": "…", "title": "To do", "position": 0, "cardIds": ["<cardId>"] } },
  "cards":   { "<cardId>":   { "id": "…", "title": "…", "description": "", "priority": "MEDIUM",
                               "dueDate": null, "labels": [], "position": 0, "archived": false } }
}
```

Import runs in a single transaction. It **deletes every existing column and card of the board (archived ones and their comments included)**, optionally renames the board, then recreates columns and cards with **new IDs**; IDs in the payload are only used to link columns to cards. Card order comes from each column's `cardIds` order. Requires `columns`; every column and card needs a non-blank title, otherwise `400 INVALID_IMPORT`.

Behaviours to be aware of when consuming these responses:

- Archived cards are left out of the top-level `cards` map, but their IDs can still appear in a column's `cardIds`. Clients should skip IDs with no matching card.
- Export therefore does not include archived cards.

## Rich text fields

`card.description` and `card_comment.content` are unbounded `TEXT` columns that the API stores and returns as opaque strings. **There is no server-side sanitization or escaping.** The official frontend writes a small HTML subset produced by its Quill editor and sanitizes with DOMPurify at render time. Any other client (another UI, an MCP agent, a script) may write plain text or its own HTML, but **must sanitize before rendering it as HTML**.

The only server-side check is a length cap via Bean Validation, returning `400` with a `{"field": "message"}` body:

| Field | Max length |
|---|---|
| Card `description` | 50,000 characters |
| Comment `content` | 10,000 characters |

This is an application-layer guard against oversized payloads, not a DB constraint and not an HTML validity check.

## Email

Nothing sends email at the moment; the password-reset flow was removed together with the password login. The `webtools-email` client is kept for future features (for example invitations): the service calls it **directly** via Eureka and Spring Cloud LoadBalancer (not back through the gateway): `POST /email/v1` with `{recipient, msgBody, subject}` (see `client/EmailClient` and `SendEmailRequest`). Settings are listed under [Configuration](#configuration).

## Testing, CI and Docker

```bash
./mvnw test                          # all tests
./mvnw test -Dtest=CardServiceTest   # one class
./mvnw -B clean verify               # what CI runs (adds JaCoCo, sortpom)
```

The tests are plain unit tests (Mockito for services, plus a Bean Validation test for the size limits). Testcontainers dependencies are on the test classpath, but no test uses them yet, and there are no controller or database integration tests.

GitHub Actions (`.github/workflows/maven.yml`) runs on pushes and pull requests to `master`: build with JDK 21, `mvn verify` plus SonarCloud analysis ([summary](https://sonarcloud.io/summary/overall?id=ikoyski_webtools-kanban-backend)), then builds and pushes `ikoyski/webtools-kanban-backend:latest` to Docker Hub. Required secrets: `SONAR_TOKEN`, `DOCKER_USERNAME`, `DOCKER_PASSWORD`.

The `Dockerfile` copies `target/*.jar` onto `eclipse-temurin:21-jre-jammy`, exposes 8080 and starts with `-Dspring.profiles.active=private`. Build the jar first (`./mvnw -B clean package`).

## Project layout

```
src/main/java/com/ikoyki/webtools/kanban/backend
├── controller/   thin REST controllers (/v1/...)
├── service/      business logic, transactions, authorization checks
├── repository/   Spring Data JPA repositories
├── entity/       JPA entities and enums (BoardRole, Priority)
├── dto/          request/ and response/ records and beans
├── mapper/       BoardMapper (entities → normalized responses)
├── security/     @AuthUser + resolver (central identity headers)
├── config/       email client + executor
├── client/       EmailClient (declarative HTTP client)
└── exception/    domain exceptions and GlobalExceptionHandler
src/main/resources/db/migration/   Flyway V1 (flattened) + V2 (drop obsolete provider table)
```

## License

See [LICENSE](LICENSE).
