# PartyWave – Redis Architecture

This document defines the **Redis architecture** for the **PartyWave** backend.

Scope:
- PostgreSQL remains the authoritative persistent store for users, rooms, tracks, playlist items, etc.
- Redis is a fast, in‑memory state store used for:
  1. **Room playlist ordering**
  2. **Room playback state** (redis‑only)
  3. **Online room members**
  4. **TTL‑based cleanup** of room runtime keys

All keys are prefixed with:

```text
partywave:
```

In examples below, `{roomId}`, `{playlistItemId}`, `{userId}` etc. are UUID strings from the relational database.

---

## 1. Room Playlist Ordering

PartyWave stores the **ordered playlist per room** in Redis, while all metadata stays in PostgreSQL (`playlist_item`, `track`, etc.). Tracks are **always appended to the end of the playlist**.

### 1.1 Key: Room Playlist

```text
partywave:room:{roomId}:playlist   (LIST)
```

**Type:** `LIST`

**Element:** `playlist_item.id` (UUID as string)

**Semantics:**
- Represents the **current ordered playlist** of a room.
- The list order is the playback order.
- New tracks are always added to the **tail** of the list.

**Write flow – add track to playlist**

1. Application inserts a new `playlist_item` row in PostgreSQL.
2. Application appends the item ID to the Redis list:

   ```bash
   RPUSH partywave:room:{roomId}:playlist {playlistItemId}
   ```

**Read flow – get playlist order**

- To obtain the full ordered list of playlist item IDs:

  ```bash
  LRANGE partywave:room:{roomId}:playlist 0 -1
  ```

The application then joins these IDs with PostgreSQL data to build DTOs for clients.

**Skip / removal behavior**

There are two common strategies; PartyWave can choose one:

1. **Remove from list when skipped/finished**  
   - After the current track finishes or is skipped, remove its ID:

     ```bash
     LREM partywave:room:{roomId}:playlist 1 {playlistItemId}
     ```

2. **Keep full history and track a head pointer**  
   - Keep all IDs and store the "current index" in playback state.  
   - This is useful if you want to preserve a full queue history in Redis.

The architecture supports either approach as long as the service layer is consistent.

---

## 2. Room Playback State (Redis‑Only)

PartyWave keeps the **entire playback state for each room in Redis only**. There is no dedicated playback table in PostgreSQL.

Important business rules:
- A track can be **started** or **skipped**.
- Tracks **cannot be paused**.

### 2.1 Key: Room Playback State

```text
partywave:room:{roomId}:playback   (HASH)
```

**Type:** `HASH`

**Fields (canonical):**

```text
current_playlist_item_id   = UUID (playlist_item currently playing)
started_at_ms              = Long (UTC epoch millis when playback started)
track_duration_ms          = Long (track duration in milliseconds)
updated_at_ms              = Long (UTC epoch millis of the last state change)
```

There are **no pause‑related fields**. Once started, a track is either:
- Played until its duration elapses (client‑side), or
- Explicitly skipped by the application.

### 2.2 Start Track Flow

When the backend wants to start playing a track in a room:

1. In PostgreSQL:
   - Mark the selected `playlist_item` as `PLAYING`.
   - Mark the previously playing item (if any) as `PLAYED` or `SKIPPED`.

2. In Redis, update the playback hash:

   ```bash
   HSET partywave:room:{roomId}:playback \
     current_playlist_item_id {playlistItemId} \
     started_at_ms {nowEpochMs} \
     track_duration_ms {durationMs} \
     updated_at_ms {nowEpochMs}
   ```

3. Emit a `TRACK_START` event via WebSocket including these fields.
4. Clients compute `elapsedMs = now - started_at_ms` and seek in their Spotify player accordingly.

### 2.3 Skip Track Flow

When the backend decides to skip the current track (e.g. after a vote or manual action):

1. Determine the next playlist item:
   - Use `partywave:room:{roomId}:playlist` to retrieve the ordered IDs.
   - Apply the application’s skip strategy (remove current from list, or move head pointer).
2. Update PostgreSQL `playlist_item` statuses.
3. Update the playback hash with the new track’s data (same as in the start flow).
4. Emit a new `TRACK_START` event.

If there is **no next item**, the service may either:
- Delete the playback hash for this room, or
- Leave it with the last values and rely on TTL (see section 4).

---

## 3. Online Room Members

PostgreSQL table `room_member` describes which users belong to which rooms. Redis only tracks **currently online members per room**, i.e. users who are actively connected (usually via WebSocket).

### 3.1 Key: Online Members Set

```text
partywave:room:{roomId}:members:online   (SET)
```

**Type:** `SET`

**Element:** `app_user.id` (UUID as string)

**Semantics:**
- Represents the set of users that are **currently online in the room**.
- Used for displaying online counts and for any logic that depends on active listeners (e.g. vote thresholds, if implemented elsewhere).

**Join flow (user enters a room and binds WebSocket):**

```bash
SADD partywave:room:{roomId}:members:online {userId}
```

**Leave flow (WebSocket disconnect / explicit leave):**

```bash
SREM partywave:room:{roomId}:members:online {userId}
```

**Read:**

- Online member count:

  ```bash
  SCARD partywave:room:{roomId}:members:online
  ```

- Online member IDs:

  ```bash
  SMEMBERS partywave:room:{roomId}:members:online
  ```

This set is purely runtime state; if Redis is flushed, the backend can rebuild it progressively as users reconnect.

---

## 4. TTL and Cleanup Strategy

Runtime keys in Redis should not live forever. PartyWave uses **TTL‑based cleanup** to reclaim memory for inactive rooms.

### 4.1 Which Keys Get TTL?

For each room, the following keys are considered runtime state and good candidates for TTL:

- Playlist ordering:

  ```text
  partywave:room:{roomId}:playlist
  ```

- Playback state:

  ```text
  partywave:room:{roomId}:playback
  ```

- Online members set:

  ```text
  partywave:room:{roomId}:members:online
  ```

### 4.2 Setting TTLs

TTL can be updated whenever the room becomes inactive or periodically by a background job. Example values:

```bash
EXPIRE partywave:room:{roomId}:playlist 86400      # 24 hours
EXPIRE partywave:room:{roomId}:playback 3600       # 1 hour
EXPIRE partywave:room:{roomId}:members:online 3600 # 1 hour
```

A simple policy could be:
- As long as at least one user is online, **do not** expire keys.
- When the last user leaves (online set becomes empty), set TTLs for that room’s keys.

### 4.3 Hard Cleanup on Room Deletion

When a room is deleted at the application level, TTL is not enough; the backend should **explicitly delete** all known Redis keys for that room to free memory immediately.

Suggested cleanup logic (`RoomCleanupService`):

```bash
DEL partywave:room:{roomId}:playlist
DEL partywave:room:{roomId}:playback
DEL partywave:room:{roomId}:members:online
```

If the application later adds more room‑scoped keys, they should be included in this cleanup routine.

---

## 5. Summary

PartyWave uses Redis as a focused, minimal runtime state layer:

- **Playlist ordering** per room is stored as a Redis `LIST` of `playlist_item` IDs.
- **Playback state** is stored **only in Redis** as a `HASH` – no pause state, only start and skip.
- **Online members** per room are stored as a Redis `SET` of `app_user` IDs.
- **TTL + explicit cleanup** ensure that runtime keys do not linger indefinitely when rooms become inactive or are deleted.

PostgreSQL remains the system of record for all persistent entities. Redis is purely an in‑memory performance and coordination layer for the PartyWave backend.

