# PartyWave – PostgreSQL Data Model

This document defines the **relational data model** for the PartyWave backend.

It is written for AI agents and backend developers. The goal is to provide a clear, implementation‑oriented description of all tables, columns, enums and relationships that appear in the PostgreSQL schema.

PartyWave uses PostgreSQL as the **system of record** for all persistent entities:

* Users & profiles
* Rooms & membership
* Tracks & playlist items
* Likes / dislikes for playlist items
* Chat messages
* Votes (skip / kick)

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
* `playlist_item_status`: `QUEUED`, `PLAYING`, `PLAYED`, `SKIPPED`
* `playlist_item_stats_enum`: `LIKE`, `DISLIKE`
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
* `app_user_images: List<AppUserImages>` – Logical one‑to‑many to profile images.
* `app_user_stats: UUID` – Foreign key to `app_user_stats`.

**Relationships**

* One `app_user` can have many `app_user_images`.
* One `app_user` has one `app_user_stats` row.
* `room_member.app_user_id` references `app_user.id`.
* `playlist_item.added_by_id`, `playlist_item_stats.user_id`, `vote.voter_id`, `vote.target_user_id`, `chat_message.sender_id` all reference `app_user.id`.

### 2.2 `app_user_status` (enum)

Represents a user’s state in the system:

* `ONLINE`
* `OFFLINE`
* `BANNED`

Used by `app_user.statusUserStatus`.

### 2.3 `app_user_stats`

Aggregated statistics for a user.

**Columns**

* `id: UUID` – Primary key.
* `total_like: Integer` – Total number of likes received (e.g. for tracks the user added).
* `total_dislike: Integer` – Total number of dislikes.

Agents can update these counters when new like/dislike rows are written to `playlist_item_stats`.

### 2.4 `app_user_images`

Profile images associated with an `app_user`.

**Columns**

* `id: UUID` – Primary key.
* `url: String` – Image URL.
* `height: String` – Height as returned by Spotify (often numeric string).
* `width: String` – Width as returned by Spotify.

**Relationships**

* Many images belong to one `app_user`.

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
* One‑to‑many with `playlist_item`, `chat_message`, `vote`, `playlist_item_stats` (via foreign keys).

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

---

## 4. Track & Playlist Domain

### 4.1 `track`

Metadata about a track from an external source (Spotify, etc.).

**Columns**

* `id: UUID` – Primary key.
* `source_id: String` – External source identifier (e.g. Spotify track ID).
* `source_uri: String` – External URI for the track.
* `name: String` – Track title.
* `artist: String` – Artist name.
* `album: String` – Album name.
* `duration_ms: Long` – Duration in milliseconds.

One `track` can be referenced by many `playlist_item` rows across different rooms.

### 4.2 `playlist_item`

Represents one track inside a room’s playlist.

**Columns**

* `id: UUID` – Primary key.
* `room_id: UUID` – FK → `room.id`.
* `track_id: UUID` – FK → `track.id`.
* `added_by_id: UUID` – FK → `app_user.id` (who added the track).
* `stats` – Conceptual one‑to‑many relationship to `playlist_item_stats` (not a physical column; implemented via the `playlist_item_stats.playlist_item_id` foreign key).
* `position: Integer` – Position within the room playlist (optional if Redis order is canonical; still useful for DB queries).
* `status: playlist_item_status` – Queue state (`QUEUED`, `PLAYING`, `PLAYED`, `SKIPPED`).

**Relationships**

* Many `playlist_item` per `room`.
* Many `playlist_item` per `track`.
* Many `playlist_item` per `app_user` (as adder).
* One‑to‑many to `playlist_item_stats` (likes/dislikes per user).

### 4.3 `playlist_item_status` (enum)

Possible states of a playlist item:

* `QUEUED` – In the queue, not yet playing.
* `PLAYING` – Currently playing in the room.
* `PLAYED` – Finished playing.
* `SKIPPED` – Skipped before finishing.

Used by `playlist_item.status`.

### 4.4 `playlist_item_stats`

Per‑user like/dislike for a specific playlist item.

**Columns**

* `id: UUID` – Primary key.
* `playlist_item_id: UUID` – FK → `playlist_item.id`.
* `user_id: UUID` – FK → `app_user.id`.
* `stat_type: playlist_item_stats` – `LIKE` or `DISLIKE`.

**Business rules**

* A given `(playlist_item_id, user_id)` pair should be unique.
* `LIKE` increments and `DISLIKE` increments can be aggregated into `app_user_stats` for the user who added the track (`playlist_item.added_by_id`).

### 4.5 `playlist_item_stats` (enum)

Values:

* `LIKE`
* `DISLIKE`

Used by `playlist_item_stats.stat_type`.

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
* `playlist_item_id: UUID` – FK → `playlist_item.id` (used when `vote_type = SKIPTRACK`).
* `target_user_id: UUID` – FK → `app_user.id` (used when `vote_type = KICKUSER`).

**Business rules**

* For `SKIPTRACK` votes, `(room_id, voter_id, playlist_item_id)` should be unique.
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

* **Playlist ordering**

  * Redis: `partywave:room:{roomId}:playlist` holds an ordered list of `playlist_item.id` values.
  * PostgreSQL: `playlist_item` holds full metadata and status for each entry.

* **Playback state**

  * Redis stores the active playback hash per room.
  * PostgreSQL does **not** maintain a dedicated playback table; only `playlist_item.status` is persisted.

* **Online members**

  * Redis: `partywave:room:{roomId}:members:online` contains the set of online `app_user.id`s.
  * PostgreSQL: `room_member` stores long‑term membership records.

* **Votes, likes, dislikes**

  * PostgreSQL stores the detailed per‑user records (`vote` and `playlist_item_stats`).

This division of responsibilities keeps PostgreSQL as the **authoritative history store**, while Redis provides a **fast runtime state layer** for PartyWave.
