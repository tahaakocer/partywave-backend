# PartyWave – Redis Architecture

This document defines the **Redis architecture** for the **PartyWave** backend.

Scope:
- PostgreSQL remains the authoritative persistent store for users, rooms, chat messages, votes, and user statistics.
- Redis is a fast, in‑memory state store used for:
  1. **Playlist items and track metadata** (redis‑only, cleaned up when rooms close)
  2. **Like/dislike statistics** for playlist items (redis‑only, cleaned up when rooms close)
  3. **Room playlist ordering**
  4. **Room playback state** (redis‑only)
  5. **Online room members**
  6. **TTL‑based cleanup** of room runtime keys

All keys are prefixed with:

```text
partywave:
```

In examples below, `{roomId}`, `{playlistItemId}`, `{userId}` etc. are UUID strings.

---

## 1. Playlist Items and Track Metadata

PartyWave stores **all playlist items and track metadata in Redis only**. These are runtime-only data that are cleaned up when rooms close. Tracks are **always appended to the end of the playlist**.

### 1.1 Key: Playlist Item Hash

```text
partywave:room:{roomId}:playlist:item:{playlistItemId}   (HASH)
```

**Type:** `HASH`

**Fields:**

```text
id                    = UUID (playlist item ID)
room_id               = UUID (room ID)
added_by_id           = UUID (user ID who added the track)
sequence_number       = Integer (sequential number indicating order of addition, starts at 1)
status                = String (QUEUED, PLAYING, PLAYED, SKIPPED)
added_at_ms           = Long (UTC epoch millis when added)
source_id             = String (Spotify track ID)
source_uri            = String (Spotify URI, e.g., spotify:track:...)
name                  = String (track title)
artist                = String (artist name)
album                 = String (album name)
duration_ms           = Long (track duration in milliseconds)
```

**Semantics:**
- Each playlist item is stored as a separate hash in Redis.
- Track metadata is embedded directly in the playlist item hash (no separate track table).
- Status can be: `QUEUED`, `PLAYING`, `PLAYED`, `SKIPPED`.
- **`sequence_number`**: A sequential integer that indicates the order in which tracks were added to the room. The first track added gets `sequence_number = 1`, the second gets `sequence_number = 2`, and so on. This ensures a clear, unambiguous ordering even when tracks are removed from the active playlist list.
- **History Preservation**: Playlist item hashes are **NOT deleted** when tracks finish (`PLAYED`) or are skipped (`SKIPPED`). They remain in Redis until room cleanup to preserve history and allow users to view past tracks with their like/dislike statistics.

### 1.2 Playlist Item Status State Machine

**Status Values:**
- `QUEUED`: Track is waiting in the playlist, not yet played.
- `PLAYING`: Track is currently being played.
- `PLAYED`: Track has finished playing naturally (reached end).
- `SKIPPED`: Track was skipped before completion (via vote or manual action).

**State Transition Rules:**

1. **QUEUED → PLAYING**:
   - When a track starts playing (selected from playlist).
   - Only tracks with status `QUEUED` can transition to `PLAYING`.

2. **PLAYING → PLAYED**:
   - When a track finishes naturally (reaches end of duration).
   - Track is removed from playlist after this transition.

3. **PLAYING → SKIPPED**:
   - When a track is skipped before completion (via vote threshold or manual action).
   - Track is removed from playlist after this transition.

4. **Final States**:
   - `PLAYED`: **Final state** - A track with status `PLAYED` **cannot** transition back to `PLAYING`.
   - `SKIPPED`: **Final state** - A track with status `SKIPPED` **cannot** transition back to `PLAYING`.

**Important Business Rules:**

- **A `PLAYED` track cannot become `PLAYING` again**: Once a playlist item reaches `PLAYED` status, it is removed from the playlist and cannot be replayed as the same item. The same Spotify track can be added again later, but it will be a **new playlist item** with a new UUID and status `QUEUED`.

- **Playlist progression**: When a track is `PLAYED` or `SKIPPED`, it is removed from the playlist, and the next `QUEUED` track becomes `PLAYING`.

- **Re-adding tracks**: The same Spotify track (same `source_id` or `source_uri`) can be added to the playlist multiple times. Each addition creates a **new playlist item** with:
  - New UUID
  - Status `QUEUED`
  - New `added_at_ms` timestamp
  - New `added_by_id` (may be same or different user)

**State Transition Diagram:**

```
QUEUED → PLAYING → PLAYED (final)
                ↓
              SKIPPED (final)
```

**Valid Transitions:**
- `QUEUED` → `PLAYING` (when track starts)
- `PLAYING` → `PLAYED` (when track finishes naturally)
- `PLAYING` → `SKIPPED` (when track is skipped)

**Invalid Transitions (must be prevented):**
- `PLAYED` → `PLAYING` ❌
- `SKIPPED` → `PLAYING` ❌
- `PLAYED` → `QUEUED` ❌
- `SKIPPED` → `QUEUED` ❌

**AI Agent Notes:**
- **Always validate status before transitions**: Before changing status to `PLAYING`, ensure current status is `QUEUED`.
- **Prevent invalid transitions**: Reject attempts to play tracks with status `PLAYED` or `SKIPPED`.
- **New playlist item for re-added tracks**: When the same Spotify track is added again, create a new playlist item with new UUID and status `QUEUED`.

### 1.1 Key: Room Playlist

```text
partywave:room:{roomId}:playlist   (LIST)
```

**Type:** `LIST`

**Element:** `playlist_item.id` (UUID as string)

**Semantics:**
- Represents the **complete playlist** of a room, containing **all tracks** regardless of status (`QUEUED`, `PLAYING`, `PLAYED`, `SKIPPED`).
- The list order reflects the chronological order of addition (maintained by `sequence_number` in each item hash).
- New tracks are always added to the **tail** of the list.
- **Important**: Tracks are **never removed** from this list. When a track finishes (`PLAYED`) or is skipped (`SKIPPED`), only its `status` field is updated in the hash. The item ID remains in the list.
- To get active tracks only, filter by `status` field: iterate through list items and check each item's status in its hash.

**Write flow – add track to playlist**

1. Application generates a new UUID for the playlist item.
2. Application gets the next sequence number for this room:
   - Query all existing playlist item hashes for this room (pattern: `partywave:room:{roomId}:playlist:item:*`)
   - Find the maximum `sequence_number` value
   - Next sequence number = max + 1 (or 1 if no items exist)
   - **Alternative**: Use a Redis counter key: `INCR partywave:room:{roomId}:playlist:sequence_counter` (starts at 0, so first track gets 1)
3. Application creates the playlist item hash in Redis:

   ```bash
   HSET partywave:room:{roomId}:playlist:item:{playlistItemId} \
     id {playlistItemId} \
     room_id {roomId} \
     added_by_id {userId} \
     sequence_number {nextSequenceNumber} \
     status QUEUED \
     added_at_ms {nowEpochMs} \
     source_id {spotifyTrackId} \
     source_uri {spotifyUri} \
     name "{trackName}" \
     artist "{artistName}" \
     album "{albumName}" \
     duration_ms {durationMs}
   ```

4. Application appends the item ID to the Redis list:

   ```bash
   RPUSH partywave:room:{roomId}:playlist {playlistItemId}
   ```

**Read flow – get playlist**

- To obtain the complete playlist (all tracks):

  ```bash
  LRANGE partywave:room:{roomId}:playlist 0 -1
  ```

- To get a specific playlist item's data:

  ```bash
  HGETALL partywave:room:{roomId}:playlist:item:{playlistItemId}
  ```

- To filter by status (e.g., get only active tracks):
  1. Get all item IDs from list: `LRANGE partywave:room:{roomId}:playlist 0 -1`
  2. For each item ID, fetch hash and check `status` field
  3. Filter items where `status = QUEUED` or `status = PLAYING`

- To get like/dislike counts for an item:

  ```bash
  SCARD partywave:room:{roomId}:playlist:item:{playlistItemId}:likes
  SCARD partywave:room:{roomId}:playlist:item:{playlistItemId}:dislikes
  ```

**AI Agent Notes:**
- The playlist list contains **all tracks** regardless of status. Use `status` field in each item hash to filter.
- When a user joins a room, return the complete playlist from the list, sorted by `sequence_number` (or use list order which matches chronological order).
- Include like/dislike counts for all items.
- To get only active tracks (`QUEUED` or `PLAYING`), filter by checking each item's `status` field.
- The list order matches chronological order (newest at tail), and `sequence_number` provides explicit ordering.


**Skip / completion behavior**

When a track is skipped or finished:

1. Update the playlist item status in Redis:

   ```bash
   HSET partywave:room:{roomId}:playlist:item:{playlistItemId} status SKIPPED
   # or
   HSET partywave:room:{roomId}:playlist:item:{playlistItemId} status PLAYED
   ```

2. **DO NOT remove the item ID from the playlist list** - it remains in the list with updated status.

3. **DO NOT delete the playlist item hash** - it must remain in Redis for history:
   - The hash preserves track metadata, `sequence_number`, and status.
   - Like/dislike sets remain accessible for displaying statistics.
   - Hash is only deleted during room cleanup (see section 5.3).

**AI Agent Notes:**
- **No removal from list**: Tracks are never removed from the playlist list. Only the `status` field is updated.
- **Status-based filtering**: To get active tracks, iterate through the list and filter by `status = QUEUED` or `status = PLAYING`.
- **History preservation**: All tracks remain in the list and can be viewed with their like/dislike statistics.
- Use `sequence_number` to maintain chronological order (though list order already reflects this).

---

## 2. Like/Dislike Statistics

PartyWave stores **like/dislike statistics for playlist items in Redis only**. These statistics are used to display real-time feedback while the room is active, and are cleaned up when rooms close.


### 2.1 Key: Playlist Item Like/Dislike Set

```text
partywave:room:{roomId}:playlist:item:{playlistItemId}:likes      (SET)
partywave:room:{roomId}:playlist:item:{playlistItemId}:dislikes  (SET)
```

**Type:** `SET`

**Element:** `app_user.id` (UUID as string)

**Semantics:**
- Each playlist item has two sets: one for likes, one for dislikes.
- Each set contains the user IDs who liked/disliked that item.
- A user can only be in one set at a time (like and dislike are mutually exclusive).

**Write flow – add like**

1. Remove user from dislikes set (if present):

   ```bash
   SREM partywave:room:{roomId}:playlist:item:{playlistItemId}:dislikes {userId}
   ```

2. Add user to likes set:

   ```bash
   SADD partywave:room:{roomId}:playlist:item:{playlistItemId}:likes {userId}
   ```

3. **Update PostgreSQL `app_user_stats`**:
   - Get `added_by_id` from the playlist item hash.
   - Increment `total_like` and decrement `total_dislike` (if user previously disliked) in `app_user_stats` table.

**Write flow – add dislike**

1. Remove user from likes set (if present):

   ```bash
   SREM partywave:room:{roomId}:playlist:item:{playlistItemId}:likes {userId}
   ```

2. Add user to dislikes set:

   ```bash
   SADD partywave:room:{roomId}:playlist:item:{playlistItemId}:dislikes {userId}
   ```

3. **Update PostgreSQL `app_user_stats`**:
   - Get `added_by_id` from the playlist item hash.
   - Increment `total_dislike` and decrement `total_like` (if user previously liked) in `app_user_stats` table.

**Write flow – remove like/dislike**

1. Remove user from both sets:

   ```bash
   SREM partywave:room:{roomId}:playlist:item:{playlistItemId}:likes {userId}
   SREM partywave:room:{roomId}:playlist:item:{playlistItemId}:dislikes {userId}
   ```

2. **Update PostgreSQL `app_user_stats`**:
   - Get `added_by_id` from the playlist item hash.
   - Decrement the appropriate counter (`total_like` or `total_dislike`) in `app_user_stats` table.

**Read flow – get like/dislike counts**

- Like count:

  ```bash
  SCARD partywave:room:{roomId}:playlist:item:{playlistItemId}:likes
  ```

- Dislike count:

  ```bash
  SCARD partywave:room:{roomId}:playlist:item:{playlistItemId}:dislikes
  ```

- Check if user liked/disliked:

  ```bash
  SISMEMBER partywave:room:{roomId}:playlist:item:{playlistItemId}:likes {userId}
  SISMEMBER partywave:room:{roomId}:playlist:item:{playlistItemId}:dislikes {userId}
  ```

### 2.2 Race Condition & Atomicity Problem

**Critical Issue**: Like/dislike updates require **synchronization between Redis and PostgreSQL**, but there is **no atomic distributed transaction** between these systems.

**Problem Statement**:

When a user likes or dislikes a playlist item, the system must:
1. **Update Redis**: Add/remove user from likes/dislikes sets (runtime state, visible to users).
2. **Update PostgreSQL**: Update `app_user_stats.total_like` or `app_user_stats.total_dislike` for the track adder (persistent statistics).

**Failure Scenarios**:

1. **Redis succeeds, PostgreSQL fails**:
   - Redis shows like/dislike (users see correct counts).
   - PostgreSQL statistics are incorrect (persistent inconsistency).

2. **PostgreSQL succeeds, Redis fails**:
   - Statistics are correct in PostgreSQL.
   - Redis state is inconsistent (users see incorrect counts).

3. **Partial Redis failure**:
   - One set update succeeds, other fails (e.g., add to likes succeeds, remove from dislikes fails).

**Impact**:
- **Data inconsistency** between Redis (runtime state) and PostgreSQL (persistent state).
- **User statistics become inaccurate** over time (`app_user_stats.total_like`, `total_dislike`).
- **Inconsistency persists** even after rooms close (since `app_user_stats` is in PostgreSQL).

**Best Practice**:

1. **Always update PostgreSQL first** (within transaction if supported).
2. **If PostgreSQL fails**: Return error, do not update Redis.
3. **If PostgreSQL succeeds**: Update Redis atomically (use MULTI/EXEC or Lua script).
4. **If Redis fails**: Compensate by reverting PostgreSQL update.
5. **Log all failures** for monitoring and reconciliation.

---

## 3. Room Playback State (Redis‑Only)

PartyWave keeps the **entire playback state for each room in Redis only**. There is no dedicated playback table in PostgreSQL.

Important business rules:
- A track can be **started** or **skipped**.
- Tracks **cannot be paused**.

### 3.1 Key: Room Playback State

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

### 3.2 Start Track Flow

When the backend wants to start playing a track in a room:

1. **Validate playlist item status**:
   - Check current status: `HGET partywave:room:{roomId}:playlist:item:{playlistItemId} status`
   - **Must be `QUEUED`**: Only tracks with status `QUEUED` can transition to `PLAYING`.
   - **Reject if `PLAYED` or `SKIPPED`**: If status is `PLAYED` or `SKIPPED`, reject the operation (these are final states).
   - If status is already `PLAYING`, no action needed (track is already playing).
   - **Find next track**: Iterate through playlist list, find first item with `status = QUEUED`.

2. In Redis, update the playlist item status:
   - **Mark the previously playing item (if any) as `PLAYED`**:
     ```bash
     HSET partywave:room:{roomId}:playlist:item:{previousItemId} status PLAYED
     ```
     - Previous item remains in playlist list with updated status.

   - **Mark the selected playlist item as `PLAYING`**:
     ```bash
     HSET partywave:room:{roomId}:playlist:item:{playlistItemId} status PLAYING
     ```

3. In Redis, update the playback hash:

   ```bash
   HSET partywave:room:{roomId}:playback \
     current_playlist_item_id {playlistItemId} \
     started_at_ms {nowEpochMs} \
     track_duration_ms {durationMs} \
     updated_at_ms {nowEpochMs}
   ```

4. Emit a `TRACK_START` event via WebSocket including these fields.
5. Clients compute `elapsedMs = now - started_at_ms` and seek in their Spotify player accordingly.

### 3.3 Skip Track Flow

When the backend decides to skip the current track (e.g. after a vote or manual action):

1. **Validate current track status**:
   - Get current track ID from playback hash: `HGET partywave:room:{roomId}:playback current_playlist_item_id`
   - Check current status: `HGET partywave:room:{roomId}:playlist:item:{currentItemId} status`
   - **Must be `PLAYING`**: Only tracks with status `PLAYING` can be skipped.
   - **Reject if `PLAYED` or `SKIPPED`**: These are final states and cannot be skipped.

2. **Mark current track as `SKIPPED`** (final state):
   ```bash
   HSET partywave:room:{roomId}:playlist:item:{currentItemId} status SKIPPED
   ```
   - Status `SKIPPED` is **final** - this track cannot become `PLAYING` again as the same playlist item.
   - Track remains in playlist list with updated status.

3. Determine the next playlist item:
   - Use `partywave:room:{roomId}:playlist` to retrieve all item IDs.
   - Iterate through the list, find first item with `status = QUEUED`.

4. If next track exists:
   - Validate next track status is `QUEUED` (see Start Track Flow step 1).
   - Update the playback hash with the new track's data (same as in the start flow).
   - Mark next track as `PLAYING` (see Start Track Flow step 2).
   - Emit a new `TRACK_START` event.

5. If there is **no next item**:
   - Delete or clear the playback hash for this room.
   - Emit a `PLAYBACK_STOPPED` event (optional).
   - Playback stops until a new track is added.

**AI Agent Notes**:
- **Status `SKIPPED` is final**: A skipped track cannot be played again as the same playlist item.
- **Always validate status before transitions**: Ensure current track is `PLAYING` before skipping.
- **Tracks remain in list**: Skipped tracks remain in the playlist list with updated status. Filter by status to get active tracks.

---

## 4. Online Room Members

PostgreSQL table `room_member` describes which users belong to which rooms. Redis only tracks **currently online members per room**, i.e. users who are actively connected (usually via WebSocket).

### 4.1 Key: Online Members Set

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

## 5. TTL and Cleanup Strategy

Runtime keys in Redis should not live forever. PartyWave uses **TTL‑based cleanup** to reclaim memory for inactive rooms.

### 5.1 Which Keys Get TTL?

For each room, the following keys are considered runtime state and good candidates for TTL:

- Playlist items (all items for the room):

  ```text
  partywave:room:{roomId}:playlist:item:*
  ```

- Like/dislike sets (all sets for the room):

  ```text
  partywave:room:{roomId}:playlist:item:*:likes
  partywave:room:{roomId}:playlist:item:*:dislikes
  ```

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

### 5.2 Setting TTLs

TTL can be updated whenever the room becomes inactive or periodically by a background job. Example values:

```bash
# Set TTL for all playlist item hashes (requires iteration or pattern matching)
# Set TTL for all like/dislike sets (requires iteration or pattern matching)
EXPIRE partywave:room:{roomId}:playlist 86400      # 24 hours
EXPIRE partywave:room:{roomId}:playback 3600       # 1 hour
EXPIRE partywave:room:{roomId}:members:online 3600 # 1 hour
```

A simple policy could be:
- As long as at least one user is online, **do not** expire keys.
- When the last user leaves (online set becomes empty), set TTLs for that room's keys.

**Note**: Setting TTLs for individual playlist items and like/dislike sets requires iterating over all keys matching the pattern `partywave:room:{roomId}:playlist:item:*` and applying TTL to each.

### 5.3 Hard Cleanup on Room Deletion

When a room is deleted at the application level, TTL is not enough; the backend should **explicitly delete** all known Redis keys for that room to free memory immediately.

Suggested cleanup logic (`RoomCleanupService`):

1. Get all playlist item IDs from the playlist list:
   ```bash
   LRANGE partywave:room:{roomId}:playlist 0 -1
   ```

2. For each playlist item ID, delete:
   - The playlist item hash: `DEL partywave:room:{roomId}:playlist:item:{playlistItemId}`
   - The likes set: `DEL partywave:room:{roomId}:playlist:item:{playlistItemId}:likes`
   - The dislikes set: `DEL partywave:room:{roomId}:playlist:item:{playlistItemId}:dislikes`

4. Delete the sequence counter (if used):
   - `DEL partywave:room:{roomId}:playlist:sequence_counter`

3. Delete the main room keys:
   ```bash
   DEL partywave:room:{roomId}:playlist
   DEL partywave:room:{roomId}:playback
   DEL partywave:room:{roomId}:members:online
   ```

**Alternative approach**: Use Redis pattern matching to find and delete all keys:
```bash
# Get all keys matching the pattern
KEYS partywave:room:{roomId}:*
# Delete all matching keys (use DEL with the returned keys)
```

**Important**: Before deleting like/dislike sets, ensure that `app_user_stats` in PostgreSQL has been updated with the final counts. This should happen in real-time as likes/dislikes are added, but verify before cleanup.

If the application later adds more room‑scoped keys, they should be included in this cleanup routine.

---

## 6. Summary

PartyWave uses Redis as a focused, minimal runtime state layer:

- **Playlist items and track metadata** are stored **only in Redis** as `HASH` structures. Each playlist item contains full track metadata (no separate track table).
- **Like/dislike statistics** are stored **only in Redis** as `SET` structures (one set for likes, one for dislikes per playlist item).
- **Playlist ordering** per room is stored as a Redis `LIST` of `playlist_item` IDs.
- **Playback state** is stored **only in Redis** as a `HASH` – no pause state, only start and skip.
- **Online members** per room are stored as a Redis `SET` of `app_user` IDs.
- **TTL + explicit cleanup** ensure that runtime keys do not linger indefinitely when rooms become inactive or are deleted.

**Critical Integration Point**: When a playlist item receives a like or dislike in Redis, the `app_user_stats` table in PostgreSQL must be updated for the user who added that track (`added_by_id`). This ensures user statistics persist even after rooms close and playlist data is cleaned up from Redis.

**Race Condition & Atomicity**: Redis and PostgreSQL updates are **not atomic**. Implement compensation pattern, outbox pattern, or saga pattern to handle failures and ensure data consistency. See section 2.2 for detailed solution approaches.

PostgreSQL remains the system of record for all persistent entities (users, rooms, chat messages, votes, user statistics). Redis is purely an in‑memory runtime state layer for playlist items, tracks, and their statistics, which are cleaned up when rooms close.

