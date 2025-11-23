# PartyWave – Project Overview & Features

This document provides a comprehensive overview of the **PartyWave** application, its features, and workflows. It is written for AI agents and developers who need to understand the system's functionality and business logic.

PartyWave is a **synchronized music listening platform** where users can create rooms, listen to Spotify tracks together in real-time, chat, vote on tracks, and interact socially.

---

## 1. System Architecture Overview

PartyWave uses a **dual-storage architecture**:

- **PostgreSQL**: Authoritative persistent store for all persistent entities (users, rooms, chat messages, votes, user statistics)
- **Redis**: Fast in-memory runtime state layer (playlist items, track metadata, like/dislike statistics, playlist ordering, playback state, online members). Playlist data is cleaned up when rooms close.
- **Spotify Web SDK**: Client-side music playback synchronization
- **WebSocket**: Real-time communication for chat, playback events, and room state updates

**Important**: 
- Playlist items, track metadata, and like/dislike statistics are stored **only in Redis** and are cleaned up when rooms close.
- **Tracks are never removed from the playlist list**: When a track finishes (`PLAYED`) or is skipped (`SKIPPED`), only its `status` field is updated. The track remains in the playlist list for history. Filter by `status` to get active tracks (`QUEUED`/`PLAYING`) or history (`PLAYED`/`SKIPPED`).

For detailed technical specifications, refer to:
- `POSTGRES_SCHEMA.md` – Database schema and relationships
- `REDIS_ARCHITECTURE.md` – Redis data structures and patterns
- `SPOTIFY_*.md` – Spotify API integration endpoints
- `AUTHENTICATION.md` – JWT-based authentication and WebSocket security

---

## 1.1 Project Package Structure

PartyWave backend follows a layered architecture with clear separation of concerns. The project is organized into the following packages:

### Package Organization

```
com.partywave
├── controller/          # REST API endpoints and WebSocket handlers
│   ├── auth/           # Authentication endpoints (Spotify OAuth, JWT)
│   ├── room/           # Room management endpoints
│   ├── playlist/       # Playlist operations (add, search tracks)
│   ├── chat/           # Chat message endpoints
│   ├── user/           # User profile endpoints
│   └── websocket/      # WebSocket connection handlers and event emitters
│
├── dto/                # Data Transfer Objects (request/response models)
│   ├── request/        # Request DTOs (input validation)
│   ├── response/       # Response DTOs (API output)
│   └── websocket/      # WebSocket message DTOs
│
├── entity/             # JPA/Hibernate entities (PostgreSQL models)
│                       # Entities for: users, rooms, room members, chat messages, votes, etc.
│
├── repository/         # Data access layer (JPA repositories)
│                       # Repository interfaces for database CRUD operations
│
├── service/            # Business logic layer
│   ├── auth/           # Authentication services (OAuth, JWT)
│   ├── room/           # Room management services
│   ├── playlist/       # Playlist operations (Redis operations)
│   ├── playback/       # Playback synchronization services
│   ├── chat/           # Chat message services
│   ├── user/           # User profile services
│   ├── spotify/        # Spotify API integration services
│   └── websocket/      # WebSocket event services
│
├── mapper/             # Entity-DTO mapping utilities
│                       # Mappers for converting between entities and DTOs
│
├── config/             # Configuration classes
│                       # Spring configuration: Security, JWT, Redis, WebSocket, Spotify, CORS
│
├── security/           # Security and authentication components
│   ├── jwt/            # JWT token generation and validation
│   ├── oauth/          # Spotify OAuth handlers
│   └── websocket/      # WebSocket authentication
│
├── utils/              # Utility classes and helpers
│                       # Utilities for: Redis keys, token encryption, timestamps, validation, exceptions
│
├── exception/          # Custom exception classes
│                       # Custom exceptions for error handling (business, not found, unauthorized, validation)
│
└── redis/              # Redis-specific components
    ├── service/        # Redis service layer
    │                   # Services for: playlist operations, playback state, online members
    └── model/          # Redis data models (if needed)
```

### Package Responsibilities

- **`controller/`**: Handles HTTP requests, validates input via DTOs, delegates to services, returns responses. Also handles WebSocket connections and message routing.

- **`dto/`**: Defines request/response models for API endpoints. Used for input validation and output serialization. Separate DTOs for WebSocket messages.

- **`entity/`**: JPA entities representing PostgreSQL tables. Defines relationships, constraints, and database mappings.

- **`repository/`**: Spring Data JPA repositories for database access. Provides CRUD operations and custom queries.

- **`service/`**: Contains business logic. Coordinates between repositories (PostgreSQL) and Redis services. Handles complex workflows (e.g., room creation, track addition, vote processing).

- **`mapper/`**: Converts between entities and DTOs. Uses libraries like MapStruct or manual mapping.

- **`config/`**: Spring configuration classes for security, Redis, WebSocket, Spotify API client, CORS, etc.

- **`security/`**: JWT token handling, OAuth flow implementation, WebSocket authentication interceptors.

- **`utils/`**: Reusable utility functions (Redis key building, token encryption, timestamp conversion, validation helpers).

- **`exception/`**: Custom exception classes for error handling with appropriate HTTP status codes.

- **`redis/`**: Redis-specific services and models. Handles all Redis operations (playlist items, playback state, online members, like/dislike sets).

### Layer Flow

1. **Request Flow**: `Controller` → `DTO` (validation) → `Service` → `Repository` (PostgreSQL) / `RedisService` (Redis) → `Entity` / Redis operations
2. **Response Flow**: `Entity` / Redis data → `Mapper` → `DTO` → `Controller` → JSON response
3. **WebSocket Flow**: `WebSocketHandler` → `Security` (JWT validation) → `Service` → `RedisService` / `Repository` → Emit events

**AI Agent Notes**:
- **Separation of Concerns**: Controllers should be thin (validation + delegation). Business logic belongs in services.
- **DTO Usage**: Always use DTOs for API input/output. Never expose entities directly to controllers.
- **Redis Operations**: All playlist-related operations should go through `RedisService` classes, not directly in controllers.
- **Transaction Management**: Use `@Transactional` on service methods that modify PostgreSQL data. Redis operations are atomic by design.
- **Error Handling**: Use custom exceptions and handle them via `@ControllerAdvice` for consistent error responses.
- **Security**: All controllers (except public OAuth endpoints) should require JWT authentication via Spring Security filters.

---

## 2. Core Features & Workflows

### 2.1 User Authentication & Registration

**Feature**: Users authenticate via Spotify OAuth and are automatically registered on first login.

**Workflow**:

1. User clicks "Login with Spotify" on the frontend.
2. Frontend sends request to backend API (e.g., `GET /auth/spotify/login`).
3. Backend constructs Spotify authorization URL with `client_id`, `redirect_uri`, `scope`, and `state` (for CSRF protection), then redirects user to Spotify OAuth authorization endpoint (`https://accounts.spotify.com/authorize`).
4. User grants permissions to PartyWave on Spotify's authorization page.
5. Spotify redirects back to backend callback endpoint (e.g., `GET /auth/spotify/callback?code=AUTH_CODE&state=STATE`) with authorization code.
6. Backend validates `state` parameter (CSRF protection), then exchanges authorization code for access token and refresh token via Spotify Token API (`POST https://accounts.spotify.com/api/token`).
7. Backend fetches user profile from Spotify API (`GET /v1/me` endpoint) using the access token.
8. Backend checks if `app_user` exists with matching `spotify_user_id`:
   - **If exists**: Update `last_active_at`, return user data.
   - **If not exists**: Create new `app_user` record with:
     - `spotify_user_id` (from Spotify)
     - `display_name` (from Spotify profile)
     - `email` (from Spotify profile)
     - `country`, `href`, `url`, `type` (from Spotify profile)
     - `app_user_images` (from Spotify profile images)
     - Initialize `app_user_stats` with `total_like = 0`, `total_dislike = 0`
     - Set `status = ONLINE`
9. Backend stores Spotify access/refresh tokens securely in `user_tokens` table:
   - Create or update `user_tokens` record with:
     - `app_user_id` = user ID
     - `access_token` = Spotify access token (encrypted at rest)
     - `refresh_token` = Spotify refresh token (encrypted at rest)
     - `token_type` = `Bearer`
     - `expires_at` = token expiration time (from Spotify response)
     - `scope` = granted OAuth scopes
10. Backend generates PartyWave JWT tokens:
    - Generate JWT access token (15 min expiration) with user claims (see `AUTHENTICATION.md` section 2.1)
    - Generate JWT refresh token (7 days expiration)
    - Store refresh token hash in database (if refresh token storage implemented)
11. Backend returns user data and JWT tokens to frontend (via redirect to frontend with token, or API response, or httpOnly cookies).

**Database Impact**:
- `app_user` table: INSERT (new users) or UPDATE (existing users)
- `app_user_images` table: INSERT (for new users)
- `app_user_stats` table: INSERT (for new users)
- `user_tokens` table: INSERT or UPDATE (for all users, new or existing)

**AI Agent Notes**:
- **Backend handles OAuth flow**: Frontend should NOT directly redirect to Spotify. Instead, frontend calls backend API (e.g., `/auth/spotify/login`), and backend constructs and redirects to Spotify authorization URL. This keeps `client_secret` secure on backend.
- **Callback endpoint**: Spotify redirects to backend callback endpoint (e.g., `/auth/spotify/callback`) with authorization code. Backend validates `state` parameter for CSRF protection.
- Use Spotify User API endpoints (see `SPOTIFY_AUTH_ENDPOINTS.md`).
- Store Spotify access/refresh tokens securely in `user_tokens` table (see `POSTGRES_SCHEMA.md`). Tokens should be encrypted at rest.
- Handle token refresh automatically before API calls: Check `user_tokens.expires_at` before making Spotify API requests. If expired, use `refresh_token` to obtain a new access token via Spotify Token API, then update `user_tokens` record.
- **JWT Authentication**: After Spotify OAuth, backend must generate PartyWave JWT tokens for application-level authentication. All API requests and WebSocket connections require JWT authentication. See `AUTHENTICATION.md` for detailed JWT and WebSocket authentication specifications.

---

### 2.2 Room Creation

**Feature**: Users can create rooms with customizable settings (name, description, tags, max participants, visibility).

**Workflow**:

1. **Authenticated user** (JWT token required) requests to create a room with:
   - `name: String` (required)
   - `description: String` (optional)
   - `tags: List<String>` (optional, e.g., `["lofi", "90s", "turkish-rap"]`)
   - `max_participants: Integer` (required, e.g., 10, 50, 100)
   - `is_public: Boolean` (required, `true` = public, `false` = private)
2. Backend validates input:
   - `name` must not be empty.
   - `max_participants` must be > 0.
   - `tags` must exist in `tag` table (create if missing).
3. Backend creates:
   - `room` record with generated UUID.
   - `room_member` record with:
     - `room_id` = new room ID
     - `app_user_id` = creator's ID
     - `role` = `OWNER` (resolve via `room_member_role` table)
     - `joined_at` = current timestamp
     - `last_active_at` = current timestamp
   - `room_tag` records for each tag (many-to-many relationship).
4. Backend initializes Redis state:
   - Create empty playlist list: `partywave:room:{roomId}:playlist` (LIST)
   - Create empty playback hash: `partywave:room:{roomId}:playback` (HASH)
   - Create empty online members set: `partywave:room:{roomId}:members:online` (SET)
   - Add creator to online members: `SADD partywave:room:{roomId}:members:online {userId}`

**Note**: No playlist items or tracks are created in PostgreSQL. All playlist data is stored in Redis only.
5. Backend returns room data to frontend.

**Database Impact**:
- `room` table: INSERT
- `room_member` table: INSERT
- `tag` table: INSERT (if tag doesn't exist)
- `room_tag` table: INSERT (for each tag)

**AI Agent Notes**:
- Room creator automatically becomes `OWNER` with full permissions.
- For private rooms (`is_public = false`), access is controlled via:
  - `room_access` table: Explicit access grants (room owner can grant access to specific users).
  - `room_invitation` table: Invitation tokens that can be shared (via link or token string).
- Tags should be case-insensitive and normalized (e.g., "LoFi" → "lofi").

---

### 2.3 Room Discovery & Joining

**Feature**: Users can discover public rooms (filtered by tags, search) and join them.

**Workflow**:

1. **Authenticated user** (JWT token required) requests list of public rooms (with optional filters):
   - Filter by tags (e.g., `?tags=lofi,90s`)
   - Search by name/description (e.g., `?search=chill`)
   - Pagination (e.g., `?page=1&size=20`)
2. Backend queries `room` table:
   - Filter `is_public = true`
   - Join with `room_tag` and `tag` for tag filtering
   - Join with `room_member` to get current member count
   - Join with Redis to get online member count
   - Order by creation date or popularity
3. Backend returns room list with metadata:
   - Room details (name, description, tags)
   - Current member count (from `room_member`)
   - Online member count (from Redis: `SCARD partywave:room:{roomId}:members:online`)
   - Max participants
4. User selects a room and requests to join (or uses invitation token for private rooms).
   - **Public room**: User provides `room_id`.
   - **Private room**: User provides either:
     - `room_id` + proof of access (if they have explicit access grant), OR
     - `invitation_token` (backend resolves `room_id` from token).
5. Backend validates:
   - Room exists.
   - **For public rooms** (`is_public = true`): Access is granted automatically.
   - **For private rooms** (`is_public = false`): User must have:
     - Explicit access grant in `room_access` table (`WHERE room_id = X AND app_user_id = Y`), OR
     - Valid invitation token: Query `room_invitation` table:
       - `token` matches provided token
       - `is_active = true`
       - `expires_at IS NULL OR expires_at > NOW()`
       - `max_uses IS NULL OR used_count < max_uses`
       - Extract `room_id` from invitation record
   - Room is not full: `COUNT(room_member WHERE room_id = X) < room.max_participants`
   - User is not already a member (check `room_member` table).
6. **If joining via invitation token**:
   - Increment `room_invitation.used_count` (UPDATE `room_invitation SET used_count = used_count + 1 WHERE id = X`).
   - Optionally create `room_access` record for audit trail (see section 2.4.1).
7. Backend creates `room_member` record:
   - `room_id` = target room ID
   - `app_user_id` = user ID
   - `role` = `PARTICIPANT` (default role)
   - `joined_at` = current timestamp
   - `last_active_at` = current timestamp
8. Backend updates Redis:
   - `SADD partywave:room:{roomId}:members:online {userId}`
9. Backend returns room data and current state:
   - **Complete playlist**: All items from `partywave:room:{roomId}:playlist` list, sorted by `sequence_number` (or use list order which matches chronological order)
   - For each playlist item, include:
     - Full track metadata (name, artist, album, duration, etc.)
     - `sequence_number` (for chronological ordering)
     - `status` (QUEUED, PLAYING, PLAYED, SKIPPED)
     - `added_by_id` (user who added the track)
     - Like count: `SCARD partywave:room:{roomId}:playlist:item:{playlistItemId}:likes`
     - Dislike count: `SCARD partywave:room:{roomId}:playlist:item:{playlistItemId}:dislikes`
   - Current playback state (if a track is playing)
   - Chat history (last N messages)
   - **Note**: Frontend can filter by `status` to show active tracks (`QUEUED`/`PLAYING`) separately from history (`PLAYED`/`SKIPPED`)

**Database Impact**:
- `room_member` table: INSERT
- `room_invitation` table: UPDATE `used_count` (if joining via invitation)
- `room_access` table: INSERT (optional, if joining via invitation and audit trail is desired)
- `app_user` table: UPDATE `last_active_at`

**AI Agent Notes**:
- **Private room access control**:
  - Check `room.is_public` flag. If `false`, require either:
    - `room_access` record exists for this user, OR
    - Valid `room_invitation` token (check `is_active`, `expires_at`, `used_count < max_uses`).
  - If joining via invitation token, increment `room_invitation.used_count`.
  - Optionally create `room_access` record when user joins via invitation (for audit trail).
- Consider rate limiting for room joins to prevent abuse.
- When user joins, emit WebSocket event to notify other room members.

---

### 2.4 Managing Private Room Access

**Feature**: Room owners can manage access to private rooms by granting explicit access or creating invitation tokens.

#### 2.4.1 Granting Explicit Access

**Workflow**:

1. Room owner (or moderator with permission) requests to grant access to a user:
   - `room_id`: Target private room
   - `target_user_id`: User to grant access to
2. Backend validates:
   - Room exists and `is_public = false`.
   - Requester has permission (OWNER or MODERATOR role).
   - Target user exists.
   - Target user is not already a member (check `room_member`).
   - Access is not already granted (check `room_access`).
3. Backend creates `room_access` record:
   - `room_id` = room ID
   - `app_user_id` = target user ID
   - `granted_by_id` = requester's user ID
   - `granted_at` = current timestamp
4. Backend optionally notifies target user (via WebSocket or notification system).
5. Backend returns success response.

**Database Impact**:
- `room_access` table: INSERT

**AI Agent Notes**:
- Explicit access grants allow users to join private rooms without invitation tokens.
- Users with explicit access can join the room directly (see section 2.3, step 5).

#### 2.4.2 Creating Invitation Tokens

**Workflow**:

1. Room owner (or moderator) requests to create an invitation:
   - `room_id`: Target private room
   - `expires_at`: Optional expiration time (NULL = no expiration)
   - `max_uses`: Optional maximum uses (NULL = unlimited)
2. Backend validates:
   - Room exists and `is_public = false`.
   - Requester has permission (OWNER or MODERATOR role).
3. Backend generates unique invitation token:
   - Generate UUID or custom token string.
   - Ensure token is unique (check `room_invitation` table).
4. Backend creates `room_invitation` record:
   - `room_id` = room ID
   - `token` = generated token
   - `created_by_id` = requester's user ID
   - `created_at` = current timestamp
   - `expires_at` = provided expiration (or NULL)
   - `max_uses` = provided max uses (or NULL)
   - `used_count` = 0
   - `is_active` = true
5. Backend returns invitation data to requester:
   - `token`: Can be shared as link (e.g., `/join?invite={token}`) or token string
   - `expires_at`: Expiration time (if set)
   - `max_uses`: Maximum uses (if set)
6. Requester shares invitation link/token with intended users.

**Database Impact**:
- `room_invitation` table: INSERT

**AI Agent Notes**:
- Invitation tokens can be shared via links, QR codes, or direct token strings.
- Tokens can be revoked by setting `is_active = false` (room owner can revoke).
- When token is used, `used_count` is incremented (see section 2.3, step 5).

#### 2.4.3 Revoking Invitations or Access

**Workflow**:

1. Room owner requests to revoke:
   - Invitation token: Set `room_invitation.is_active = false`
   - Explicit access: Delete `room_access` record (or mark as inactive if soft delete)
2. Backend validates requester has permission.
3. Backend updates database accordingly.
4. Backend returns success response.

**Database Impact**:
- `room_invitation` table: UPDATE `is_active`
- `room_access` table: DELETE (or soft delete)

**AI Agent Notes**:
- Revoking access does not remove existing `room_member` records (users already in room remain).
- Consider notifying affected users when access is revoked.

---

### 2.5 Adding Tracks to Playlist

**Feature**: Room members can search for Spotify tracks and add them to the room's playlist (always appended to the end).

**Playlist System Overview**:

The PartyWave playlist system uses a single unified list that contains **all tracks** regardless of status:

- **Playlist List** (`partywave:room:{roomId}:playlist` - Redis LIST):
  - Contains **all tracks** ever added to the room: `QUEUED`, `PLAYING`, `PLAYED`, and `SKIPPED`
  - Tracks are **never removed** from this list
  - List order reflects chronological order of addition
  - Each track's status is stored in its hash (`partywave:room:{roomId}:playlist:item:{playlistItemId}`)
  - To get active tracks, filter by checking each item's `status` field

**Key Concepts**:
- **Sequence Number**: Each playlist item gets a unique integer `sequence_number` (1, 2, 3, ...) indicating the order it was added. This ensures clear chronological ordering.
- **Status-Based Filtering**: The playlist list contains all tracks. Filter by `status` field to get:
  - Active tracks: `status = QUEUED` or `status = PLAYING`
  - History tracks: `status = PLAYED` or `status = SKIPPED`
- **No Removal**: Tracks are never removed from the list. When a track finishes or is skipped, only its `status` field is updated.
- **Status Lifecycle**: `QUEUED` → `PLAYING` → `PLAYED` (final) or `SKIPPED` (final). Final states cannot transition back to `PLAYING`.

**Workflow**:

1. **Authenticated user** (JWT token required) searches for tracks using Spotify Search API (see `SPOTIFY_SEARCH_ENDPOINTS.md`):
   - Query: `?q=artist:name+track:title&type=track`
   - Backend proxies request to Spotify API with user's access token.
   - Spotify returns track results.
2. Backend returns search results to frontend (track metadata: name, artist, album, duration, Spotify URI).
3. User selects a track and requests to add it to the room playlist.
4. Backend validates:
   - User is a member of the room (check `room_member` table).
   - Room exists and is active.
5. Backend generates a new UUID for the playlist item.
6. Backend gets the next sequence number for this room:
   - Use Redis counter: `INCR partywave:room:{roomId}:playlist:sequence_counter` (returns next number, starting from 1)
   - Or query existing items to find max `sequence_number` and add 1
7. Backend creates playlist item hash in Redis:
   - Create hash: `partywave:room:{roomId}:playlist:item:{playlistItemId}`
   - Fields:
     - `id` = playlist item UUID
     - `room_id` = room ID
     - `added_by_id` = user ID
     - `sequence_number` = next sequence number (integer, indicates order of addition)
     - `status` = `QUEUED`
     - `added_at_ms` = current timestamp (UTC epoch milliseconds)
     - `source_id` = Spotify track ID
     - `source_uri` = Spotify URI (e.g., `spotify:track:...`)
     - `name`, `artist`, `album`, `duration_ms` (from Spotify response)
8. Backend appends item ID to Redis playlist list:
   - `RPUSH partywave:room:{roomId}:playlist {playlistItemId}`
9. Backend emits WebSocket event to all room members: `PLAYLIST_ITEM_ADDED` with playlist item data.
10. Backend returns success response.

**AI Agent Notes**:
- Tracks are **always appended to the end** of the playlist (FIFO queue).
- Use Spotify Search API endpoints (see `SPOTIFY_SEARCH_ENDPOINTS.md`).
- All playlist items and track metadata are stored **only in Redis** (no PostgreSQL tables).
- **Status initialization**: New playlist items are always created with status `QUEUED`.
- **Sequence number**: Each playlist item gets a unique `sequence_number` that indicates the order of addition. The first track added gets `sequence_number = 1`, the second gets `sequence_number = 2`, etc. This ensures clear chronological ordering.
- **No removal from list**: Tracks are never removed from the playlist list. When status changes to `PLAYED` or `SKIPPED`, only the status field is updated. Filter by status to get active tracks.
- **Re-adding tracks**: The same Spotify track can be added multiple times. Each addition creates a **new playlist item** with:
  - New UUID (different from previous additions)
  - New `sequence_number` (incremented from previous additions)
  - Status `QUEUED`
  - New `added_at_ms` timestamp
  - New `added_by_id` (may be same or different user)
- Consider deduplication: prevent adding the same track multiple times in quick succession (optional business rule at application level).
- If playlist is empty and no track is currently playing, backend should auto-start the first track (see section 2.6).

---

### 2.6 Synchronized Playback (Spotify Web SDK)

**Feature**: All users in a room listen to the same track simultaneously, synchronized via Spotify Web SDK and backend coordination.

**Workflow**:

1. **Initial Playback Start** (when first track is added or room becomes active):
   - Backend finds first track with `status = QUEUED` from Redis playlist list:
     - Iterate through `LRANGE partywave:room:{roomId}:playlist 0 -1`
     - For each item ID, check `HGET partywave:room:{roomId}:playlist:item:{itemId} status`
     - Select first item with `status = QUEUED`
   - Backend fetches track metadata from Redis: `HGETALL partywave:room:{roomId}:playlist:item:{playlistItemId}`
   - Backend updates Redis playlist item status: `HSET partywave:room:{roomId}:playlist:item:{playlistItemId} status PLAYING`
   - Backend updates Redis playback hash:
     ```bash
     HSET partywave:room:{roomId}:playback \
       current_playlist_item_id {playlistItemId} \
       started_at_ms {currentEpochMs} \
       track_duration_ms {durationMs} \
       updated_at_ms {currentEpochMs}
     ```
   - Backend emits WebSocket event `TRACK_START` to all room members with:
     - `playlist_item_id`
     - `track` metadata (name, artist, album, duration, source_uri) from Redis hash
     - `started_at_ms` (UTC epoch milliseconds)
     - `track_duration_ms`
   - Frontend clients receive event and:
     - Use Spotify Web SDK to play track: `player.play({ uris: [track.source_uri] })`
     - Calculate elapsed time: `elapsedMs = now - started_at_ms`
     - Seek to correct position: `player.seek(elapsedMs)`
     - All clients are now synchronized.

2. **Track Completion** (when track finishes naturally):
   - Client-side: Spotify player emits `player_state_changed` event when track ends.
   - Frontend notifies backend (optional, or backend can track via `started_at_ms + duration_ms`).
   - **Backend validates status**: Ensure current track status is `PLAYING` before marking as `PLAYED`.
   - Backend updates Redis playlist item status to **final state**: `HSET partywave:room:{roomId}:playlist:item:{playlistItemId} status PLAYED`
     - **Status `PLAYED` is final**: This track cannot become `PLAYING` again as the same playlist item.
     - **Important**: The track remains in the playlist list with updated status. The playlist item hash is **NOT deleted** - it remains in Redis for history with its like/dislike statistics.
   - Backend selects next track from playlist (if exists):
     - Iterate through playlist list, find first item with `status = QUEUED`.
     - Start next track (repeat step 1 with next playlist item).
   - If playlist is empty, stop playback and clear Redis playback hash.

3. **Client Reconnection / Late Join**:
   - When a user joins a room or reconnects, backend sends current playback state:
     - Current `playlist_item_id` (from Redis playback hash)
     - `started_at_ms` (from Redis playback hash)
     - `track_duration_ms` (from Redis playback hash)
     - Track metadata (from Redis playlist item hash)
   - Frontend calculates elapsed time and seeks accordingly.

**AI Agent Notes**:
- **Tracks cannot be paused** (business rule). Once started, they play until completion or skip.
- **Status transitions**: Playlist items follow a strict state machine:
  - `QUEUED` → `PLAYING` (when track starts)
  - `PLAYING` → `PLAYED` (when track finishes naturally)
  - `PLAYING` → `SKIPPED` (when track is skipped)
  - **Important**: `PLAYED` and `SKIPPED` are **final states**. A `PLAYED` or `SKIPPED` track **cannot** become `PLAYING` again as the same playlist item.
  - **Re-adding tracks**: The same Spotify track can be added again later, but it will be a **new playlist item** with new UUID and status `QUEUED`.
- **Validate status before transitions**: Before starting a track, ensure its status is `QUEUED`. Reject attempts to play tracks with status `PLAYED` or `SKIPPED`.
- Use Spotify Player API for playback control (see `SPOTIFY_PLAYER_API_ENDPOINTS.md`).
- Backend must handle timezone and clock synchronization (use UTC epoch milliseconds).
- Consider periodic "heartbeat" events to keep clients in sync if network delays occur.

---

### 2.7 Skipping Tracks (Vote-Based)

**Feature**: Room members can vote to skip the currently playing track. When threshold is reached, track is skipped.

**Workflow**:

1. **Authenticated user** (JWT token required) requests to vote for skipping current track:
   - User must be a member of the room.
   - There must be a track currently playing (check Redis playback hash: `HGET partywave:room:{roomId}:playback current_playlist_item_id`).
2. Backend checks if user already voted:
   - Query `vote` table: `WHERE room_id = X AND voter_id = Y AND vote_type = SKIPTRACK AND playlist_item_id = Z`
   - **If exists**: Return error "Already voted".
   - **If not exists**: Proceed.
3. Backend creates `vote` record in PostgreSQL:
   - `room_id` = room ID
   - `voter_id` = user ID
   - `vote_type` = `SKIPTRACK`
   - `playlist_item_id` = current playing track ID (string UUID, references Redis-stored playlist item)
   - `target_user_id` = NULL (not used for SKIPTRACK)
4. Backend counts votes for this track:
   - Query: `COUNT(*) FROM vote WHERE room_id = X AND vote_type = SKIPTRACK AND playlist_item_id = Z`
5. Backend gets online member count:
   - `SCARD partywave:room:{roomId}:members:online`
6. Backend checks threshold (example: 50% of online members):
   - `vote_count >= (online_member_count * 0.5)`
   - **If threshold reached**: Skip track (proceed to step 7).
   - **If not reached**: Return vote count and threshold info to frontend.
7. **Skip Execution** (when threshold reached):
   - **Backend validates status**: Ensure current track status is `PLAYING` before skipping.
   - Backend updates Redis:
     - Current track status to **final state**: `HSET partywave:room:{roomId}:playlist:item:{playlistItemId} status SKIPPED`
       - **Status `SKIPPED` is final**: This track cannot become `PLAYING` again as the same playlist item.
       - **Important**: The track remains in the playlist list with updated status. The playlist item hash is **NOT deleted** - it remains in Redis for history with its like/dislike statistics.
   - Backend selects next track from playlist (if exists):
     - Iterate through playlist list, find first item with `status = QUEUED`.
     - If next track exists, start it (see section 2.6, step 1).
     - If playlist is empty, stop playback and clear Redis playback hash.
   - Backend emits WebSocket event `TRACK_SKIPPED` to all room members.
8. Backend returns vote status to requester.

**Database Impact**:
- `vote` table: INSERT (PostgreSQL)
- No playlist item updates in PostgreSQL (all in Redis)

**AI Agent Notes**:
- **Status validation**: Before skipping a track, ensure its status is `PLAYING`. Reject attempts to skip tracks with status `PLAYED` or `SKIPPED` (these are final states).
- **Final state**: Status `SKIPPED` is final - a skipped track cannot become `PLAYING` again as the same playlist item. The same Spotify track can be added again later, but it will be a new playlist item.

- Threshold logic is configurable (could be 50%, majority, or fixed number).
- Consider resetting votes when track changes (delete old SKIPTRACK votes for previous track).
- Votes are persisted in PostgreSQL for audit/history, but threshold checking can use Redis counters for performance.

---

### 2.8 Kicking Users (Vote-Based)

**Feature**: Room members can vote to kick a user from the room. When threshold is reached, user is removed.

**Workflow**:

1. **Authenticated user** (JWT token required) requests to vote for kicking another user:
   - User must be a member of the room.
   - Target user must be a member of the room.
   - User cannot vote to kick themselves.
   - Room owner cannot be kicked (optional business rule).
2. Backend checks if user already voted for this target:
   - Query `vote` table: `WHERE room_id = X AND voter_id = Y AND vote_type = KICKUSER AND target_user_id = Z`
   - **If exists**: Return error "Already voted".
   - **If not exists**: Proceed.
3. Backend creates `vote` record:
   - `room_id` = room ID
   - `voter_id` = user ID
   - `vote_type` = `KICKUSER`
   - `playlist_item_id` = NULL (not used for KICKUSER)
   - `target_user_id` = target user ID
4. Backend counts votes for this target:
   - Query: `COUNT(*) FROM vote WHERE room_id = X AND vote_type = KICKUSER AND target_user_id = Z`
5. Backend gets online member count (same as skip flow).
6. Backend checks threshold (example: 50% of online members).
   - **If threshold reached**: Kick user (proceed to step 7).
   - **If not reached**: Return vote count to frontend.
7. **Kick Execution** (when threshold reached):
   - Backend removes user from room:
     - Delete `room_member` record (or mark as inactive, depending on schema).
     - `SREM partywave:room:{roomId}:members:online {targetUserId}`
   - Backend closes user's WebSocket connection to the room.
   - Backend emits WebSocket event `USER_KICKED` to all remaining room members.
   - Backend returns success to requester.

**Database Impact**:
- `vote` table: INSERT
- `room_member` table: DELETE (or soft delete)

**AI Agent Notes**:
- Consider soft-deleting `room_member` instead of hard delete to preserve history.
- Reset votes when user is kicked (delete all KICKUSER votes for that user in that room).
- Room owner immunity is a common pattern (implement in validation step 1).

---

### 2.9 Like / Dislike Tracks

**Feature**: Room members can like or dislike tracks in the playlist. Likes/dislikes affect the track adder's profile statistics.

**Workflow**:

1. **Authenticated user** (JWT token required) requests to like or dislike a playlist item:
   - User must be a member of the room.
   - Playlist item must exist in Redis (check: `EXISTS partywave:room:{roomId}:playlist:item:{playlistItemId}`).
2. Backend checks if user already rated this item in Redis:
   - Check likes set: `SISMEMBER partywave:room:{roomId}:playlist:item:{playlistItemId}:likes {userId}`
   - Check dislikes set: `SISMEMBER partywave:room:{roomId}:playlist:item:{playlistItemId}:dislikes {userId}`
   - **If user is in likes set and request is LIKE**: Return error "Already liked".
   - **If user is in dislikes set and request is DISLIKE**: Return error "Already disliked".
   - **If user is in likes set and request is DISLIKE**: Remove from likes, add to dislikes (proceed to step 3).
   - **If user is in dislikes set and request is LIKE**: Remove from dislikes, add to likes (proceed to step 3).
   - **If user is in neither set**: Add to appropriate set (proceed to step 3).
3. Backend updates Redis like/dislike sets:
   - **For LIKE**:
     - Remove from dislikes: `SREM partywave:room:{roomId}:playlist:item:{playlistItemId}:dislikes {userId}`
     - Add to likes: `SADD partywave:room:{roomId}:playlist:item:{playlistItemId}:likes {userId}`
   - **For DISLIKE**:
     - Remove from likes: `SREM partywave:room:{roomId}:playlist:item:{playlistItemId}:likes {userId}`
     - Add to dislikes: `SADD partywave:room:{roomId}:playlist:item:{playlistItemId}:dislikes {userId}`
4. **Backend updates PostgreSQL `app_user_stats` for the track adder**:
   - Get `added_by_id` from Redis playlist item: `HGET partywave:room:{roomId}:playlist:item:{playlistItemId} added_by_id`
   - **If adding LIKE**:
     - Increment `total_like` in `app_user_stats` table.
     - If user previously disliked (removed from dislikes), decrement `total_dislike`.
   - **If adding DISLIKE**:
     - Increment `total_dislike` in `app_user_stats` table.
     - If user previously liked (removed from likes), decrement `total_like`.
   - Update `app_user_stats` record in PostgreSQL.
5. Backend gets updated counts from Redis:
   - Like count: `SCARD partywave:room:{roomId}:playlist:item:{playlistItemId}:likes`
   - Dislike count: `SCARD partywave:room:{roomId}:playlist:item:{playlistItemId}:dislikes`
6. Backend emits WebSocket event `PLAYLIST_ITEM_STATS_UPDATED` to room members with:
   - `playlist_item_id`
   - Updated like/dislike counts
7. Backend returns success response.

**Database Impact**:
- `app_user_stats` table: UPDATE (PostgreSQL)
- No `playlist_item_stats` operations (all in Redis)

**AI Agent Notes**:
- Users can change their vote (like → dislike or vice versa).
- Statistics are aggregated per user who **added** the track, not per track.
- **Critical**: When updating like/dislike in Redis, always update `app_user_stats` in PostgreSQL for the track adder. This ensures user statistics persist even after rooms close.
- Like/dislike counts are stored in Redis sets and can be queried in real-time.
- **Distributed Transaction Challenge**: Redis and PostgreSQL updates are not atomic. If Redis succeeds but PostgreSQL fails, data inconsistency occurs. See `REDIS_ARCHITECTURE.md` section 2.2 for detailed solution approaches.

---

### 2.10 Adding Tracks to Spotify (User's Library / Playlist)

**Feature**: Users can save the currently playing track to their Spotify library (liked songs) or add it to a Spotify playlist.

**Workflow**:

1. **Authenticated user** (JWT token required) requests to save current track to Spotify:
   - User must be authenticated (have valid Spotify access token).
   - There must be a track currently playing.
2. Backend fetches track metadata from Redis:
   - Get `current_playlist_item_id` from Redis playback hash: `HGET partywave:room:{roomId}:playback current_playlist_item_id`
   - Get track metadata from Redis playlist item: `HGET partywave:room:{roomId}:playlist:item:{playlistItemId} source_uri` (and `source_id` for track ID)
3. Backend determines action type:
   - **Save to Library**: Call Spotify API `PUT /me/tracks?ids={trackId}` (use `source_id` from Redis)
   - **Add to Playlist**: Call Spotify API `POST /playlists/{playlistId}/tracks` with track URI (use `source_uri` from Redis)
4. Backend uses user's Spotify access token to make API call.
5. Backend returns success/error response to frontend.

**Database Impact**:
- None (this is a Spotify API operation only, track data comes from Redis)

**AI Agent Notes**:
- Use Spotify User Library API or Playlists API (see `SPOTIFY_USER_ENDPOINTS.md`).
- Handle token refresh if access token is expired.
- This feature does not affect PartyWave database, only user's Spotify account.

---

### 2.11 Chat Messaging

**Feature**: Room members can send chat messages in real-time.

**Workflow**:

1. **Authenticated user** (JWT token required) sends chat message:
   - User must be a member of the room.
   - Message content must not be empty (and may have length limits).
2. Backend validates message (optional: profanity filter, rate limiting).
3. Backend creates `chat_message` record:
   - `room_id` = room ID
   - `sender_id` = user ID
   - `content` = message text
   - `sent_at` = current timestamp
4. Backend emits WebSocket event `CHAT_MESSAGE` to all room members with:
   - Message data (id, sender, content, sent_at)
5. Backend returns success response.

**Database Impact**:
- `chat_message` table: INSERT

**AI Agent Notes**:
- All chat messages are persisted in PostgreSQL for history.
- Consider pagination for chat history when user joins room (e.g., last 100 messages).
- WebSocket events enable real-time delivery; database provides persistence.

---

### 2.12 User Profile Viewing & Statistics

**Feature**: Users can view other users' profiles, including statistics (like/dislike totals) and basic profile information.

**Workflow**:

1. **Authenticated user** (JWT token required) requests profile data for a user ID (self or other).
2. Backend queries `app_user` table with user ID.
3. Backend fetches related data:
   - `app_user_images` (profile images)
   - `app_user_stats` (total_like, total_dislike)
   - **Note**: List of tracks added by user is not available in PostgreSQL (playlist items are stored in Redis only and cleaned up when rooms close).
4. Backend returns profile data to frontend.

**Database Impact**:
- Read-only queries

**AI Agent Notes**:
- Profile data is public (or restricted based on privacy settings, if implemented).
- Statistics (`app_user_stats`) are updated in real-time when playlist items receive likes/dislikes in Redis (see section 2.9). The `app_user_stats` table is updated in PostgreSQL immediately when a like/dislike is added/updated in Redis.
- Historical track lists are not available since playlist items are cleaned up when rooms close.
- Consider adding more statistics (e.g., tracks added, rooms created, etc.) if needed.

---

## 3. Real-Time Communication (WebSocket Events)

PartyWave uses WebSocket for real-time updates. **All WebSocket connections must be authenticated using JWT tokens** (see `AUTHENTICATION.md` section 3).

**WebSocket Connection Flow**:
1. Client connects to WebSocket endpoint with JWT token (in URL query parameter or initial message).
2. Backend validates JWT token (same validation as API requests).
3. If authentication fails, connection is closed with code `1008` (Policy Violation).
4. If authentication succeeds, client must subscribe to rooms to receive events.
5. Room subscription requires membership verification (see `AUTHENTICATION.md` section 3.2).

Common event types:

### 3.1 Playback Events

- `TRACK_START`: New track started playing (includes track metadata, started_at_ms, duration_ms)
- `TRACK_SKIPPED`: Current track was skipped
- `TRACK_FINISHED`: Track finished naturally (optional)

### 3.2 Playlist Events

- `PLAYLIST_ITEM_ADDED`: New track added to playlist
- `PLAYLIST_ITEM_REMOVED`: Track removed from playlist (optional)

### 3.3 Social Events

- `CHAT_MESSAGE`: New chat message sent
- `USER_JOINED`: User joined the room
- `USER_LEFT`: User left the room
- `USER_KICKED`: User was kicked from room
- `PLAYLIST_ITEM_STATS_UPDATED`: Like/dislike counts changed

### 3.4 Vote Events

- `VOTE_CAST`: New vote cast (skip or kick)
- `VOTE_THRESHOLD_REACHED`: Vote threshold reached, action executed

**AI Agent Notes**:
- **WebSocket Authentication**: All WebSocket connections must be authenticated using JWT tokens before allowing any operations. See `AUTHENTICATION.md` section 3 for detailed specifications.
- **Room Subscription**: Users must subscribe to rooms after authentication. Subscription requires room membership verification (PostgreSQL `room_member` table).
- **Message Authorization**: All room-related messages require room subscription and membership verification.
- WebSocket connections should be room-scoped (user subscribes to room events when joining).
- Handle reconnection gracefully (send current state on reconnect, re-authenticate with refreshed token if needed).
- Consider using a message broker (e.g., Redis Pub/Sub) for multi-server deployments.
- **Rate Limiting**: Implement rate limiting on WebSocket messages to prevent spam and abuse.

---

## 4. Business Rules Summary

1. **Playlist Ordering**: 
   - Tracks are always appended to the end of the playlist list (FIFO queue).
   - Each playlist item has a `sequence_number` indicating chronological order of addition.
   - The playlist list (`partywave:room:{roomId}:playlist`) contains **all tracks** regardless of status (`QUEUED`, `PLAYING`, `PLAYED`, `SKIPPED`).
   - Tracks are never removed from the list. Filter by `status` field to get active tracks or history.
   - All tracks preserve their like/dislike statistics in Redis.

2. **Playback**: Tracks cannot be paused. Once started, they play until completion or skip.

3. **Synchronization**: All users in a room listen to the same track at the same time via Spotify Web SDK and backend coordination.

4. **Voting**: Users can vote once per track (skip) or per user (kick). Thresholds are configurable (e.g., 50% of online members).

5. **Likes/Dislikes**: Users can change their vote (like ↔ dislike). Statistics affect the track adder's profile and are preserved in playlist history.

6. **History Preservation**: When tracks finish (`PLAYED`) or are skipped (`SKIPPED`), only their `status` field is updated. Tracks remain in the playlist list. Users can view all tracks (active and history) with their like/dislike statistics, sorted by `sequence_number`.

7. **Room Capacity**: Rooms have a maximum participant limit. Users cannot join if room is full.

8. **Room Ownership**: Room creator becomes `OWNER`. Owner may have special permissions (e.g., cannot be kicked).

9. **Online Status**: Online members are tracked in Redis. PostgreSQL `room_member` tracks long-term membership.

---

## 5. Integration Points

### 5.1 Spotify API

PartyWave integrates with Spotify for:
- **Authentication**: OAuth 2.0 flow (external authentication provider)
- **User Profile**: Fetch user data and images
- **Search**: Search for tracks to add to playlists
- **Playback**: Spotify Web SDK for synchronized playback
- **Library Management**: Save tracks to user's library or playlists

See `SPOTIFY_*.md` documents for endpoint specifications.

**Important**: Spotify OAuth2 authenticates users with Spotify, but **all PartyWave API requests and WebSocket connections require JWT-based application authentication**. See `AUTHENTICATION.md` for details.

### 5.2 Database Schema

See `POSTGRES_SCHEMA.md` for:
- Table definitions
- Relationships
- Enums and constraints
- Foreign keys

### 5.3 Redis Architecture

See `REDIS_ARCHITECTURE.md` for:
- Key naming conventions
- Data structures (LIST, HASH, SET)
- TTL and cleanup strategies
- Playback state management

---

## 6. Error Handling & Edge Cases

**AI Agents should handle**:

1. **Spotify Token Expiry**: Automatically refresh tokens before API calls.
2. **Empty Playlist**: When playlist is empty, stop playback and clear Redis playback state.
3. **User Disconnection**: Remove user from Redis online set, but keep `room_member` record.
4. **Room Deletion**: Clean up all Redis keys (playlist items, like/dislike sets, playback state, online members) and cascade delete related records in PostgreSQL (or soft delete).
5. **Concurrent Votes**: Use database constraints to prevent duplicate votes.
6. **Playlist Race Conditions**: Use Redis transactions (MULTI/EXEC) or Lua scripts for atomic operations.
7. **WebSocket Failures**: Implement reconnection logic and state synchronization on reconnect.
8. **Like/Dislike Updates - Race Condition**: Redis and PostgreSQL updates are not atomic. Implement compensation pattern, outbox pattern, or saga pattern to handle failures. See section 2.9.1 for detailed solution approaches.
9. **Room Cleanup**: Before deleting playlist items from Redis, ensure `app_user_stats` has been updated with final counts (should happen in real-time, but verify before cleanup).
10. **Playlist Management**: The playlist list contains all tracks. When returning room data, return the complete playlist sorted by `sequence_number`. Frontend can filter by `status` to separate active tracks from history. Include like/dislike counts for all items.

---

## 7. Implementation Notes for AI Agents

When implementing PartyWave features:

1. **Always authenticate requests**: All API endpoints (except public OAuth endpoints) require JWT authentication. Extract `app_user_id` from JWT `sub` claim. See `AUTHENTICATION.md` section 2.4.
2. **Always validate user membership** before allowing room actions.
3. **WebSocket authentication**: All WebSocket connections must be authenticated with JWT before allowing room subscriptions. See `AUTHENTICATION.md` section 3.
4. **Use transactions** for multi-step operations (e.g., add track = create Redis hash + update Redis list).
5. **Emit WebSocket events** after state changes to keep clients synchronized.
6. **Handle Redis failures gracefully** (Redis is critical for playlist operations; implement proper error handling).
7. **Respect Spotify API rate limits** (implement throttling/queuing if needed).
8. **Use UTC timestamps** for all time-based operations to avoid timezone issues.
9. **Implement proper error responses** with clear messages for frontend handling.
10. **Handle race conditions** when updating like/dislike statistics: Redis and PostgreSQL updates are not atomic. Use compensation pattern, outbox pattern, or saga pattern to ensure data consistency. See section 2.9.1 for detailed solution approaches.
11. **Clean up Redis keys** when rooms close or are deleted (playlist items, like/dislike sets, playback state).
12. **No PostgreSQL operations for playlist items** - all playlist data is stored in Redis only.
13. **Token validation**: Always validate JWT token signature, expiration, and user status before processing requests.
14. **Rate limiting**: Implement rate limiting on authentication endpoints and WebSocket messages to prevent abuse.

---

This document provides a high-level overview. For implementation details, refer to the technical documentation files (`POSTGRES_SCHEMA.md`, `REDIS_ARCHITECTURE.md`, `SPOTIFY_*.md`).

