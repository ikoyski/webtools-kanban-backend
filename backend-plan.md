# PLAN.md — Multi-board + membership (backend)

Repo: `webtools-kanban-backend` (Spring Boot 3.4.2 / Java 21 / Postgres / Flyway)
Companion doc: `frontend-plan.md` in the `webtools-kanban` repo. The **API Contract**
section below is duplicated verbatim in both files — keep them in sync if either changes.

## Context / why

Today there is exactly one board, hardcoded as a constant in `BoardController`:

```java
private final UUID FIRST_EVER_BOARD_ID = UUID.fromString("e36425fc-6c46-43f7-b263-809494502937");
```

seeded by `V3__seed_default_board.sql`. `board` has no relationship to `user_entity` at
all. `ColumnService`/`CardService` resolve columns/cards purely by their own id, with no
board-membership check anywhere.

Auth exists (`AuthController`/`AuthService`/`JwtTokenProvider`) but only issues JWTs —
nothing in this service validates them. That's by design: **an API gateway sits in front
of this backend, validates the JWT, and forwards trusted `X-User-Id` / `X-User-Email`
headers** on every proxied request. The commented-out `@RequestHeader("X-User-Email")` /
`@RequestHeader("X-User-Id")` params already present on every controller method are the
stubs for exactly this — uncomment and wire them up rather than reinventing anything.

**Trust boundary — read this before writing any code:** nothing downstream re-validates
those headers. If this service is ever reachable directly (bypassing the gateway), anyone
can forge `X-User-Id` and impersonate any user. This must stay enforced at the network/
ingress level (internal-only service, security group, etc.) — it is not something code in
this repo can fix, but every reviewer/operator needs to know it's a hard requirement, not
an implementation detail.

We're early-stage and can drop the DB and let Flyway rebuild from scratch, so migrations
below **replace** V1–V3 outright rather than layering additive V4/V5 backfills.

---

## Phase 0 — Current-user resolution from gateway headers

- Add a `CurrentUser` holder — either a request-scoped bean populated by a
  `HandlerInterceptor`, or a custom `HandlerMethodArgumentResolver` bound to a new
  `@AuthUser` annotation so controllers can declare `@AuthUser UUID currentUserId`
  instead of reading headers manually in every method.
- New interceptor/resolver logic: read `X-User-Id` (and `X-User-Email` if useful for
  logging) on every `/v1/**` request except `/v1/auth/**`. If missing/blank, respond
  `401` immediately — cheap defense-in-depth even though the gateway is the real
  enforcement point.
- **Header names confirmed**: `X-User-Id` and `X-User-Email` — matches the commented-out
  stubs already in the controllers, no further check needed before starting Phase 0.
- **Identity validation is two separate checks — do both, they catch different things:**
  1. *Well-formed + real*: the interceptor parses `X-User-Id` as a UUID and calls
     `userRepository.findById(userId)`; 401 if malformed or if it doesn't resolve to a
     real `user_entity` row. Cheap, catches bugs and stale IDs.
  2. *Forgery*: (1) does **not** protect against something upstream sending a forged-but-
     real `X-User-Id` (someone else's actual ID) — that's a network/trust-boundary
     problem, not something this interceptor can solve, since by the time a header
     arrives here it's indistinguishable from a legitimate one. The backend must not be
     reachable except through the gateway (network/ingress-level enforcement — security
     group, private subnet, internal-only service). If the backend can't be fully
     isolated from anything but the gateway, add a shared-secret HMAC over the identity
     header (gateway signs, backend verifies, secret distributed via the existing Spring
     Cloud Config server) as defense-in-depth — but don't add this speculatively if
     network isolation already holds; it's solving a threat model you may not have.
  3. *This interceptor only establishes **who** is calling — it does not check what that
     caller is allowed to touch.* That's a separate, per-resource check — see Phase 2.
- `SecurityConfig` stays exactly as it is today (just the `PasswordEncoder` bean) — no
  `spring-boot-starter-security`, no `SecurityFilterChain` needed. `jjwt` stays in
  `pom.xml` unchanged since `AuthService.generateToken` still needs it for login/signup.
- Replace every commented-out `@RequestHeader(...)` block in `BoardController`,
  `ColumnController`, `CardController` with `@AuthUser UUID currentUserId`.

## Phase 1 — Rewritten schema (replace V1–V3, not additive)

- **`V1__init_schema.sql`**: keep `uuidv7()`, `board`, `column_entity`, `card` as today,
  with two tweaks:
  - Drop `DEFAULT 'My Board'` on `board.name` (no longer a meaningful default once every
    board is user-created).
  - Add `created_by UUID` to `board` (nullable) — not the source of truth for ownership
    (that's `board_member.role = 'OWNER'`), just handy for auditing.
- **`V2__users_and_boards_membership.sql`**: merge the old V2 (`user_entity`,
  `user_provider`) with the new membership table into one migration, since they're one
  "identity + access" concern:
  ```sql
  -- user_entity, user_provider: unchanged from the current V2

  CREATE TYPE board_role AS ENUM ('OWNER', 'EDITOR', 'VIEWER');

  CREATE TABLE board_member (
      id          UUID PRIMARY KEY DEFAULT uuidv7(),
      board_id    UUID NOT NULL REFERENCES board(id) ON DELETE CASCADE,
      user_id     UUID NOT NULL REFERENCES user_entity(id) ON DELETE CASCADE,
      role        board_role NOT NULL,
      created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
      UNIQUE (board_id, user_id)
  );

  CREATE INDEX idx_board_member_user ON board_member(user_id);
  CREATE INDEX idx_board_member_board ON board_member(board_id);
  ```
- **Delete `V3__seed_default_board.sql` entirely.** No orphaned board to backfill —
  starting from empty means no `FIRST_EVER_BOARD_ID` constant is needed anywhere. If a
  seeded board is useful for local dev, do it via a `data.sql`/Testcontainers fixture or a
  `dev` Spring profile, not a Flyway migration that ships to every environment.

## Phase 2 — Authorization

- New `BoardRole` enum (`OWNER`, `EDITOR`, `VIEWER`) matching the Postgres enum, ordered
  so `EDITOR.ordinal() < OWNER.ordinal()` etc. can drive a simple `>=` comparison, or use
  an explicit rank map if you'd rather not rely on declaration order.
- New `BoardMemberEntity` (id, `@ManyToOne board`, `@ManyToOne user`, `role`, `createdAt`).
- New `BoardMemberRepository`: `findByBoardIdAndUserId`, `findAllByUserId` (drives the
  board list, ordered by board's `updatedAt`/`createdAt`), `existsByBoardIdAndUserId`,
  `countByBoardIdAndRole` (for last-owner protection).
- New `BoardAccessService`:
  ```java
  BoardRole requireMembership(UUID boardId, UUID userId);            // throws Forbidden if absent
  void requireAtLeast(UUID boardId, UUID userId, BoardRole minRole); // throws Forbidden if below
  ```
  Fits the existing convention of putting business logic in the service layer. This is
  the check that answers "does this board/column/card actually belong to this
  `X-User-Id`?" — distinct from (and layered on top of) Phase 0's identity check, which
  only establishes who's calling, not what they're allowed to touch.
- **This must run per-method, in the service layer, before any read or write** — not as
  a blanket interceptor — because the owning `boardId` for a column or card can only be
  known by fetching that entity first, which the service method is doing anyway:

  | Resource | Resolve owning `boardId` via | Check |
  |---|---|---|
  | `board` (rename/delete/import/members) | it *is* the boardId — comes from the path | `requireAtLeast(boardId, userId, role)` directly |
  | `column_entity` | existing FK: `column.getBoard().getId()` | resolve column → check |
  | `card` | one hop further: `card.getColumn().getBoard().getId()` | resolve card → column → board → check |
  | new column/card (`create*`) | no entity exists yet — resolve the *target* board/column instead | resolve target → check |

  Example (`CardService.updateCard`):
  ```java
  CardEntity card = cardRepository.findById(id)
          .orElseThrow(() -> new ResourceNotFoundException("Card not found"));

  UUID boardId = card.getColumn().getBoard().getId();
  boardAccessService.requireAtLeast(boardId, currentUserId, BoardRole.EDITOR);

  // ...proceed with the update
  ```
  Same shape for `deleteCard`/`moveCard` (resolve card → check), `createCard` (resolve
  the target *column* → its board → check, since there's no card yet), and the column
  methods (resolve column → its board → check; `createColumn` already takes `boardId`
  directly in the request body, so no extra resolution hop is needed there).
- New `ForbiddenBoardAccessException` → `403` in `GlobalExceptionHandler`, following the
  existing `ErrorResponse` pattern used for `ColumnNotEmptyException` etc.
- **While touching these methods anyway**, fix the existing inconsistency where
  `BoardService`/`ColumnService`/`CardService` throw raw `RuntimeException("X not found")`
  — currently caught by the generic `RuntimeException` handler and mapped to `500`
  instead of `404`. Switch these to the existing `ResourceNotFoundException` at minimum
  for every method modified in this work.

## Phase 3 — Endpoints

**`BoardController`** (`/v1/boards`) — remove `FIRST_EVER_BOARD_ID` entirely:
- `GET /v1/boards` → **list** of boards the caller belongs to: `id`, `name`, `role`,
  `updatedAt`. Replaces today's "the one board" response — **this is the breaking
  change**, coordinate the release with the frontend.
- `POST /v1/boards` → create a board; caller becomes `OWNER` in the same transaction.
- `GET /v1/boards/{boardId}` → today's full-state response (`BoardMapper.toResponse`),
  now requires `requireAtLeast(boardId, userId, VIEWER)`. Add a `role` field to
  `BoardResponse`/`BoardMapper.toResponse` (the caller's own role on this board) — the
  frontend relies on both the list and detail responses being self-sufficient for
  `role`, since a bookmarked/shared `?board=` link can hit this endpoint directly
  without having called the list first.
- `GET /v1/boards/{boardId}/export` → same auth as above.
- `PUT /v1/boards/{boardId}/import` → requires `EDITOR`.
- `PATCH /v1/boards/{boardId}` (rename) → requires `EDITOR`. `DELETE /v1/boards/{boardId}`
  → requires `OWNER` (destructive; kept stricter than rename deliberately).

**New `BoardMemberController`** (`/v1/boards/{boardId}/members`):
- `GET` → list members + roles, requires `VIEWER`.
- `POST` → add member by email + role, requires `OWNER`. 404 if email doesn't match a
  `user_entity`, 409 if already a member.
- `PATCH /{userId}` → change role, requires `OWNER`; blocked outright (409) if it would
  demote the last remaining `OWNER` — same reasoning as the removal case below.
- `DELETE /{userId}` → remove member, requires `OWNER` for removing someone else; allow
  any member to remove themselves ("leave board"). Removing/demoting the **last**
  `OWNER` is blocked outright (409) — no self-serve "transfer ownership" flow for v1;
  recoverable via a manual DB fix if it's ever actually hit, an acceptable trade at this
  stage.

**`ColumnController`/`CardController`** — stay on their current flat paths
(`/v1/columns`, `/v1/cards`) since `boardId`/`columnId` already travel in the request
bodies; no need to force a `/v1/boards/{boardId}/...` nesting. Every method now resolves
the owning board and calls `boardAccessService.requireAtLeast(boardId, currentUserId,
EDITOR)` before mutating.

## Phase 4 — Tests

- Update `BoardServiceTest`, `ColumnServiceTest`, `CardServiceTest` for the new
  authorization checks and the removal of `FIRST_EVER_BOARD_ID`.
- New `BoardAccessServiceTest` covering the role-comparison logic directly.
- New `BoardMemberControllerTest` (or similar) covering the authorization matrix:
  OWNER/EDITOR/VIEWER × each mutating and read endpoint, plus the last-owner-removal
  guard.

---

## API Contract (shared with frontend-plan.md — keep in sync)

| Method | Path | Min role | Notes |
|---|---|---|---|
| GET | `/v1/boards` | authenticated | list: `{id, name, role, updatedAt}[]` |
| POST | `/v1/boards` | authenticated | body `{name}`; creator becomes OWNER |
| GET | `/v1/boards/{boardId}` | VIEWER | full state, same shape as today's `BoardResponse`, **plus `role`** |
| GET | `/v1/boards/{boardId}/export` | VIEWER | |
| PUT | `/v1/boards/{boardId}/import` | EDITOR | |
| PATCH | `/v1/boards/{boardId}` | EDITOR | rename |
| DELETE | `/v1/boards/{boardId}` | OWNER | |
| GET | `/v1/boards/{boardId}/members` | VIEWER | `{userId, email, displayName, role}[]` |
| POST | `/v1/boards/{boardId}/members` | OWNER | body `{email, role}` |
| PATCH | `/v1/boards/{boardId}/members/{userId}` | OWNER | body `{role}`; 409 if demoting last OWNER |
| DELETE | `/v1/boards/{boardId}/members/{userId}` | OWNER (or self) | 409 if removing last OWNER |
| POST | `/v1/columns` | EDITOR (of body's `boardId`) | unchanged shape |
| PATCH/DELETE | `/v1/columns/{id}` | EDITOR | unchanged shape |
| PATCH | `/v1/columns/reorder` | EDITOR | unchanged shape |
| POST/PATCH/DELETE | `/v1/cards*` | EDITOR | unchanged shape |

Identity headers consumed from the gateway: `X-User-Id` (required), `X-User-Email`
(optional, logging only). **Never trust these if this service becomes reachable outside
the gateway.**

Role enum values, exactly as strings: `OWNER`, `EDITOR`, `VIEWER`.
