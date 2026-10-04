# WebTools Kanban Backend

A Spring Boot 3 backend for the WebTools Kanban board application. This service provides a REST API for managing multiple Kanban boards with user membership and role-based access control (RBAC), featuring robust positional ordering and transactional board imports.

## 🚀 Tech Stack

- **Language**: Java 21
- **Framework**: Spring Boot 3.4.2
- **Cloud**: Spring Cloud (Eureka, Config)
- **Build Tool**: Maven
- **Database**: PostgreSQL 17
- **Migrations**: Flyway
- **Bot Protection**: Cloudflare Turnstile
- **Auth**: Spring Security (BCrypt password hashing) + JWT (jjwt) for local signup/login
- **API Documentation**: SpringDoc OpenAPI (Swagger UI)
- **Observability**: Spring Boot Actuator, Micrometer Tracing (Brave, Zipkin)

## 🛠️ Setup & Installation

### Prerequisites
- Java 21 JDK
- Maven
- Docker & Docker Compose

### Quick Start
1. **Start the Database**:
   ```bash
   docker compose up -d
   ```

2. **Run the Application**:
   ```bash
   mvn spring-boot:run
   ```

The server will start on `http://localhost:8080`.

## 📖 API Documentation

Once the application is running, you can access the interactive API documentation (Swagger UI) at:
`http://localhost:8080/swagger-ui/index.html`

### Base URL
`http://localhost:8080/api/v1`

### Authentication
- `POST /auth/signup`: Register a new local user (email + password). **Requires a valid Cloudflare Turnstile token** to prevent bot registrations. Returns a JWT upon success.
    - `POST /auth/login`: Authenticate a local user. **Requires a valid Cloudflare Turnstile token** to prevent brute-force/bot attacks. Returns a JWT upon success.
    - `PATCH /auth/password`: Change password for the current authenticated user.
    - `POST /auth/forgot-password`: Request a password reset link via email. **Requires a valid Cloudflare Turnstile token**.
    - `POST /auth/reset-password`: Reset password using a secure token from an email.

> **Bot Protection**: The `/auth` endpoints are protected by Cloudflare Turnstile. The backend validates the Turnstile token via `TurnstileService` by calling the Cloudflare siteverify API before processing authentication requests.

> **Note**: this service does not currently validate the JWT on subsequent requests. Every
> other endpoint identifies the caller via a trusted `X-User-Id` header, which is expected to
> be set by an upstream API gateway after validating the JWT. This service must not be exposed
> directly to untrusted clients — see `CLAUDE.md` for details.

### Key Endpoints
- `GET /boards`: List boards the current user belongs to.
- `POST /boards`: Create a new board.
- `GET /boards/{boardId}`: Fetch full state of a specific board.
- `GET /boards/{boardId}/export`: Export full state of a specific board.
- `PUT /boards/{boardId}/import`: Transactionally replace entire board state.
- `PATCH /boards/{boardId}`: Rename a board.
- `DELETE /boards/{boardId}`: Delete a board (OWNER only).
- `GET /boards/{boardId}/members`: List members and roles of a board.
- `POST /boards/{boardId}/members`: Add a member to a board (OWNER only).
- `PATCH /boards/{boardId}/members/{userId}`: Update a member's role (OWNER only).
- `DELETE /boards/{boardId}/members/{userId}`: Remove a member from a board (OWNER, or self).
- `POST /columns`: Create a new column.
- `PATCH /columns/{id}`: Rename a column.
- `PATCH /columns/reorder`: Reorder columns within a board.
- `DELETE /columns/{id}?transferTo={id}`: Delete a column and optionally transfer cards.
- `POST /cards`: Create a new card.
- `PATCH /cards/{id}`: Update a card.
- `PATCH /cards/{id}/move`: Move a card within or across columns.
- `DELETE /cards/{id}`: Delete a card.
- `GET /comments/card/{cardId}`: List comments on a card, oldest first.
- `POST /comments/card/{cardId}`: Add a comment to a card.
- `DELETE /comments/{id}`: Delete a comment (`OWNER`, or the comment's own author).

### Roles & Permissions
Every board has members with one of three roles: `VIEWER`, `EDITOR`, `OWNER` (in ascending
order of privilege).

| Action | Minimum role |
|---|---|
| View board / export / list members | `VIEWER` |
| Create/edit/delete/move cards, create/rename/delete columns, reorder columns | `EDITOR` |
| Rename board, import board | `EDITOR` |
| Delete board | `OWNER` |
| Add/remove members, change member roles | `OWNER` |
| Leave a board (remove yourself) | none — any member can leave |

A board must always have at least one `OWNER`; demoting or removing the last owner is blocked.

## 🛡️ Quality Assurance

The project is automatically analyzed for bugs, vulnerabilities, and code smells as part of the CI pipeline via GitHub Actions. You can view the current quality status and detailed reports on the [SonarCloud Summary Page](https://sonarcloud.io/summary/overall?id=ikoyski_webtools-kanban-backend).

## 📉 Database Schema

- `user_entity`: User profiles and identities.
- `user_provider`: Authentication providers per user (e.g. local email/password credentials).
- `board`: Top-level board metadata.
- `board_member`: User-to-board membership with roles (OWNER, EDITOR, VIEWER).
- `column_entity`: Columns belonging to a board, ordered by `position`.
- `card`: Cards belonging to a column, ordered by `position`. Labels are stored as `JSONB`.
- `card_comment`: Comments on a card, ordered by `created_at`.

## 📝 Rich Text (`description` / comment `content`)

`card.description` and `card_comment.content` are unbounded `TEXT` columns. The API stores and
returns them as opaque strings — there is no server-side sanitization or HTML escaping of
either field, and none is planned here. The official frontend writes a small, fixed subset of
HTML to these fields (produced by its Quill-based WYSIWYG editor) and sanitizes with DOMPurify
on every render, client-side — see the frontend's `CLAUDE.md`/`README.md`. Any other client
(another UI, an MCP agent, a script hitting the API directly) is free to write plain text or its
own HTML here, but **must sanitize before rendering it as HTML**, since this service will echo
back whatever was stored.

The only server-side check is a request-level length cap, enforced via Bean Validation
(`@Size`, returns `400` with a field-error body on violation — see `CreateCardRequest` /
`UpdateCardRequest` / `CommentRequest`):

| Field | Max length |
|---|---|
| Card `description` | 50,000 characters |
| Comment `content` | 10,000 characters |

This exists only to reject pathologically large payloads; it's an application-layer guard, not
a DB constraint, and has nothing to do with HTML validity.

## 🩺 Health Check
Check the service status:
`http://localhost:8080/actuator/health`
