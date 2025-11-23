# PartyWave – PostgreSQL Data Model

This document defines the **relational data model** for the PartyWave backend.

It is written for AI agents and backend developers. The goal is to provide a clear, implementation‑oriented description of all tables, columns, enums and relationships that appear in the PostgreSQL schema.

PartyWave uses PostgreSQL as the **system of record** for all persistent entities:

* Users & profiles
* User tokens (Spotify OAuth)
* Rooms & membership
* Chat messages
* Votes (skip / kick)

**Note**: 
- Tracks, playlist items, and likes/dislikes are stored **only in Redis** as runtime state and are cleaned up when rooms close. See `REDIS_ARCHITECTURE.md` for details.
- **Tracks are never removed from the playlist list**: When a track finishes (`PLAYED`) or is skipped (`SKIPPED`), only its `status` field is updated in Redis. The track remains in the playlist list for history. Filter by `status` to get active tracks or history.

Runtime state (playlist ordering, playback state, online members, TTL cleanup) is handled in **Redis** and is documented separately in the `PartyWave – Redis Architecture` document.

---

## 1. Conventions

### 1.1 Primary Keys

* All main entities use **UUID** as primary key:

  * `id UUID PRIMARY KEY`
* UUID values are generated in the application or by PostgreSQL extensions.

### 1.2 Auditing Fields (Abstract Base)

Most entities conceptually extend a common auditing base (represented as `default` in the diagram):

* `createdBy: String`
* `lastModifiedBy: String`
* `createdDate: DateTime`
* `lastModifiedDate: DateTime`

These can be implemented either as:

* columns directly on each table, or
* inherited via a mapped superclass / auditing base class in JPA.

For brevity, they are omitted on individual table listings below, but agents can assume they exist on all major business tables.

### 1.3 Enums

The schema uses several enums, modelled as PostgreSQL enum types or constrained string fields:

* `VOTE_TYPE`: `SKIPTRACK`, `KICKUSER`
* `room_member_role`: e.g. `OWNER`, `DJ`, `MODERATOR`, `PARTICIPANT` (name only is shown in the diagram)
* `app_user_status`: `ONLINE`, `OFFLINE`, `BANNED`

Agents should treat these as **closed sets of values** used for business logic decisions.

---

## 2. User & Profile Domain

### 2.1 `app_user`

Represents a PartyWave user linked to a Spotify account.

**Columns**

* `id: UUID` – Primary key.
* `spotify_user_id: String` – Spotify user ID.
* `display_name: String` – Display name (from Spotify profile).
* `email: String` – User email.
* `country: String` – Country code (from Spotify profile, optional).
* `href: String` – Spotify API href for the user.
* `url: String` – Spotify profile URL.
* `type: String` – Spotify object type (usually `user`).
* `ipAddress: String` – ip address from request
* `last_active_at: Instant` – Last time user was active in the system.
* `status: app_user_status` – Current status: `ONLINE`, `OFFLINE`, `BANNED`.
* `app_user_images: List<AppUserImage>` – Logical one‑to‑many to profile images.
* `app_user_stats: UUID` – Foreign key to `app_user_stats`.

**Relationships**

* One `app_user` can have many `app_user_images`.
* One `app_user` has one `app_user_stats` row.
* One `app_user` has one `user_tokens` row (for Spotify OAuth tokens).
* `room_member.app_user_id` references `app_user.id`.
* `vote.voter_id`, `vote.target_user_id`, `chat_message.sender_id` all reference `app_user.id`.

**Note**: `playlist_item.added_by_id` and `playlist_item_stats.user_id` no longer exist in PostgreSQL; playlist items and their stats are stored in Redis only.

### 2.2 `app_user_status` (enum)

Represents a user’s state in the system:

* `ONLINE`
* `OFFLINE`
* `BANNED`

Used by `app_user.status`.

### 2.3 `app_user_stats`

Aggregated statistics for a user.

**Columns**

* `id: UUID` – Primary key.
* `total_like: Integer` – Total number of likes received (e.g. for tracks the user added).
* `total_dislike: Integer` – Total number of dislikes.

**Business Rules & Consistency**

* Statistics are **updated in real-time** when playlist items (stored in Redis) receive likes/dislikes.
* **Critical**: When a like/dislike is added/removed in Redis, the `app_user_stats` table must be updated **atomically** for the track adder (`added_by_id`).

**Race Condition & Atomicity Problem**:

When updating like/dislike statistics:
1. **Redis** updates like/dislike sets (runtime state).
2. **PostgreSQL** updates `app_user_stats` (persistent state).

These updates are **not atomic** because Redis and PostgreSQL do not share a distributed transaction coordinator. If Redis succeeds but PostgreSQL fails (or vice versa), **data inconsistency occurs**.

**Solution Approaches**:

See `REDIS_ARCHITECTURE.md` section 2.2 for detailed solution approaches:

1. **Compensation Pattern** (Recommended): Update PostgreSQL first, then Redis. If Redis fails, compensate by reverting PostgreSQL update.

2. **Outbox Pattern**: Update PostgreSQL within transaction, write event to `outbox` table. Background job processes outbox events and updates Redis.

3. **Saga Pattern**: Orchestrate both updates with compensation logic.

**AI Agent Notes**:
- **Always use transactions** when updating `app_user_stats` to ensure atomicity within PostgreSQL.
- **Implement retry logic** for transient failures (network, deadlocks).
- **Monitor for inconsistencies** between Redis and PostgreSQL (can create a reconciliation job).
- **Ensure idempotency**: Like/dislike operations should be idempotent (multiple identical requests have same effect as one).

### 2.4 `app_user_images`

Profile images associated with an `app_user`.

**Columns**

* `id: UUID` – Primary key.
* `url: String` – Image URL.
* `height: String` – Height as returned by Spotify (often numeric string).
* `width: String` – Width as returned by Spotify.

**Relationships**

* Many images belong to one `app_user`.

### 2.5 `user_tokens`

Stores Spotify OAuth access and refresh tokens for each user. Tokens are used to authenticate Spotify API requests on behalf of users.

**Columns**

* `id: UUID` – Primary key.
* `app_user_id: UUID` – FK → `app_user.id` (unique, one token record per user).
* `access_token: String` – Spotify access token (should be encrypted at rest).
* `refresh_token: String` – Spotify refresh token (should be encrypted at rest).
* `token_type: String` – Token type (usually `Bearer`).
* `expires_at: Instant` – When the access token expires (used to determine when to refresh).
* `scope: String` – OAuth scopes granted (comma-separated, e.g., `user-read-email,user-read-private`).

**Relationships**

* One `user_tokens` row per `app_user` (one-to-one relationship).

**Business rules**

* `(app_user_id)` should be unique (one token record per user).
* Access tokens should be encrypted at rest for security.
* When access token expires, use `refresh_token` to obtain a new access token via Spotify Token API.
* Tokens are updated during OAuth callback and token refresh operations.

### 2.6 `refresh_tokens` (Optional)

**Note**: This table is **optional** and only needed if you want to implement refresh token storage and blacklisting for PartyWave JWT tokens. See `AUTHENTICATION.md` section 2.3 for details.

Stores PartyWave JWT refresh tokens for token revocation and blacklisting support.

**Columns**

* `id: UUID` – Primary key.
* `app_user_id: UUID` – FK → `app_user.id`.
* `token_hash: String` – Hashed refresh token (unique, for lookup).
* `expires_at: Instant` – When the refresh token expires.
* `created_at: Instant` – When the token was created.
* `revoked_at: Instant` – When the token was revoked (NULL if active).
* `device_info: String` – Optional: Device/browser information for security audit.
* `ip_address: String` – Optional: IP address for audit trail.

**Relationships**

* Many `refresh_tokens` rows per `app_user` (one-to-many, allows multiple active refresh tokens per user for different devices).

**Business rules**

* `token_hash` should be unique.
* Only non-revoked tokens (`revoked_at IS NULL`) and non-expired tokens (`expires_at > NOW()`) are valid.
* When a refresh token is used, optionally revoke the old token and create a new one (token rotation).
* Tokens can be revoked on logout or security incidents by setting `revoked_at`.

**AI Agent Notes**:
- This table is optional. JWT tokens can be stateless without database storage.
- If implemented, use it for token blacklisting and revocation support.
- Hash refresh tokens before storing (use SHA-256 or bcrypt).
- Consider cleanup job to remove expired tokens periodically.

---

## 3. Room & Membership Domain

### 3.1 `room`

Represents a PartyWave room where users listen together.

**Columns**

* `id: UUID` – Primary key.
* `name: String` – Room name.
* `description: String` – Room description.
* `max_participants: Integer` – Maximum number of participants allowed.
* `is_public: boolean` – Whether the room is public.
* `room_tags: List<RoomTag>` – Logical many‑to‑many to `tag`.
* `room_members: List<RoomMember>` – Logical one‑to‑many to `room_member`.

**Relationships**

* Many‑to‑many with `tag` via `room_tag`.
* One‑to‑many with `room_member`.
* One‑to‑many with `room_access` (for private rooms).
* One‑to‑many with `room_invitation` (for private rooms).
* One‑to‑many with `chat_message`, `vote` (via foreign keys).

### 3.2 `tag`

Simple label that categorizes rooms.

**Columns**

* `id: UUID` – Primary key.
* `name: String` – Tag name (e.g. `lofi`, `90s`, `turkish-rap`).

**Relationships**

* Many‑to‑many with `room` via `room_tag`.

### 3.3 `room_tag`

Join table for the many‑to‑many relation between `room` and `tag`.

**Columns**

* `id: UUID` – Primary key.
* `room_id: UUID` – FK → `room.id`.
* `tag_id: UUID` – FK → `tag.id`.

Each row states that a given tag is applied to a given room.

### 3.4 `room_member`

Represents a user’s membership in a room.

**Columns**

* `id: UUID` – Primary key.
* `room_id: UUID` – FK → `room.id`.
* `app_user_id: UUID` – FK → `app_user.id`.
* `role: UUID` – FK → `room_member_role.id`.
* `joined_at: Instant` – When the user joined this room.
* `last_active_at: Instant` – Last activity in this room.

**Relationships**

* Many `room_member` rows per `room`.
* Many `room_member` rows per `app_user`.
* Role is resolved via `room_member_role`.

Agents can assume that `(room_id, app_user_id)` is logically unique (user cannot join the same room multiple times).

### 3.5 `room_member_role`

Defines available roles for room membership.

**Columns**

* `id: UUID` – Primary key.
* `name: String` – e.g. `OWNER`, `DJ`, `MODERATOR`, `PARTICIPANT`.

Used by `room_member.role`.

### 3.6 `room_access`

Explicit access grants for private rooms. When a room is private (`is_public = false`), users must either:
- Be granted explicit access via this table, or
- Use a valid invitation token (see `room_invitation`).

**Columns**

* `id: UUID` – Primary key.
* `room_id: UUID` – FK → `room.id`.
* `app_user_id: UUID` – FK → `app_user.id` (user granted access).
* `granted_by_id: UUID` – FK → `app_user.id` (who granted the access, typically room owner).
* `granted_at: Instant` – When access was granted.

**Relationships**

* Many `room_access` rows per `room` (one per granted user).
* Many `room_access` rows per `app_user` (user can have access to multiple private rooms).

**Business rules**

* A given `(room_id, app_user_id)` pair should be unique.
* When a user joins a private room via `room_access`, they should also be added to `room_member`.
* Room owners and moderators can grant access to other users.

### 3.7 `room_invitation`

Invitation tokens for private rooms. Allows room owners to generate shareable invitation links/tokens.

**Columns**

* `id: UUID` – Primary key.
* `room_id: UUID` – FK → `room.id`.
* `token: String` – Unique invitation token (can be UUID or custom string).
* `created_by_id: UUID` – FK → `app_user.id` (who created the invitation, typically room owner).
* `created_at: Instant` – When invitation was created.
* `expires_at: Instant` – Optional expiration time (NULL = no expiration).
* `max_uses: Integer` – Maximum number of times this invitation can be used (NULL = unlimited).
* `used_count: Integer` – Current usage count (default 0).
* `is_active: Boolean` – Whether the invitation is still valid (can be revoked).

**Relationships**

* Many `room_invitation` rows per `room` (multiple invitations can exist).
* One `room_invitation` belongs to one `app_user` (creator).

**Business rules**

* `token` should be unique across all invitations.
* When a user joins via invitation token:
  - Validate token exists, is active, not expired, and hasn't exceeded `max_uses`.
  - Increment `used_count`.
  - Optionally create `room_access` record for audit trail.
  - Create `room_member` record.
* Invitations can be revoked by setting `is_active = false`.

---

## 5. Chat Domain

### 5.1 `chat_message`

Represents a chat message sent in a room.

**Columns**

* `id: UUID` – Primary key.
* `room_id: UUID` – FK → `room.id`.
* `sender_id: UUID` – FK → `app_user.id`.
* `content: String`
* `sent_at: Instant` 


All chat messages are persisted in PostgreSQL. 

---

## 6. Vote Domain

### 6.1 `vote`

Represents a vote either to **skip the current track** or to **kick a user** from a room.

**Columns**

* `id: UUID` – Primary key.
* `room_id: UUID` – FK → `room.id`.
* `voter_id: UUID` – FK → `app_user.id` (who cast the vote).
* `vote_type: VOTE_TYPE` – `SKIPTRACK` or `KICKUSER`.
* `playlist_item_id: String` – UUID as string (used when `vote_type = SKIPTRACK`). This references a playlist item ID stored in Redis, not a PostgreSQL foreign key.
* `target_user_id: UUID` – FK → `app_user.id` (used when `vote_type = KICKUSER`).

**Business rules**

* For `SKIPTRACK` votes, `(room_id, voter_id, playlist_item_id)` should be unique. Note: `playlist_item_id` is a string UUID referencing a Redis-stored playlist item.
* For `KICKUSER` votes, `(room_id, voter_id, target_user_id)` should be unique.
* Aggregation and threshold logic for skipping/kicking are handled by services and can leverage Redis counters.

### 6.2 `VOTE_TYPE` (enum)

Values:

* `SKIPTRACK` – Vote to skip the currently playing track.
* `KICKUSER` – Vote to remove a user from the room.

Used by `vote.vote_type`.

---

## 7. How This Relates to Redis

While this document is PostgreSQL‑centric, agents should be aware of how it interacts with the Redis layer:

* **Playlist items and tracks**: All playlist items, track metadata, and like/dislike statistics are stored in Redis only (see `REDIS_ARCHITECTURE.md`).
* **Playback state**: Redis stores the active playback hash per room.
* **Online members**: Redis tracks currently online members (`partywave:room:{roomId}:members:online`), while PostgreSQL `room_member` stores long‑term membership records.
* **Votes**: PostgreSQL stores vote records (`vote` table). Note: `vote.playlist_item_id` is a string UUID referencing a Redis-stored playlist item, not a PostgreSQL foreign key.
* **User statistics**: PostgreSQL `app_user_stats` stores aggregated like/dislike totals. **Important**: When a playlist item in Redis receives a like/dislike, `app_user_stats` must be updated for the track adder (see `REDIS_ARCHITECTURE.md` section 2.2 for race condition handling).
* **User tokens**: PostgreSQL `user_tokens` stores Spotify OAuth tokens for API authentication.
* **Refresh tokens** (optional): PostgreSQL `refresh_tokens` can store PartyWave JWT refresh tokens for revocation support (see `AUTHENTICATION.md`).

For detailed Redis architecture, see `REDIS_ARCHITECTURE.md`.
For authentication and security specifications, see `AUTHENTICATION.md`.
