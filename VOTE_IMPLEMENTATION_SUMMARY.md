# Vote Implementation Summary

This document summarizes the implementation of vote-based skip track and kick user features for PartyWave.

**Date**: 2025-11-24  
**Based on**: PROJECT_OVERVIEW.md sections 2.8 and 2.9

---

## Overview

Implemented five voting features:

1. **Vote to Skip Track** - Room members can vote to skip the currently playing track
2. **Vote to Kick User** - Room members can vote to kick another user from the room
3. **Get Vote Status** - Room members can view current vote counts
4. **Withdraw Skip Vote** - Room members can withdraw their skip track vote
5. **Withdraw Kick Vote** - Room members can withdraw their kick user vote

**Vote Threshold**: 50% of online members (configurable via `VOTE_THRESHOLD_PERCENTAGE` constant)

---

## Files Created

### DTOs

1. **`KickUserRequestDTO.java`**

   - Input DTO for kick user vote requests
   - Fields: `targetUserId` (UUID)
   - Validation: `@NotNull` on targetUserId

2. **`VoteResponseDTO.java`**

   - Response DTO for vote operations (skip/kick)
   - Fields:
     - `voteRecorded` - whether vote was successfully recorded
     - `currentVoteCount` - current votes for this action
     - `requiredVoteCount` - votes required to reach threshold
     - `onlineMemberCount` - total online members
     - `thresholdReached` - whether action was executed
     - `message` - user-friendly message
     - `targetPlaylistItemId` - for skip votes
     - `targetUserId` - for kick votes

3. **`VoteStatusResponseDTO.java`**
   - Response DTO for GET /votes endpoint
   - Contains:
     - `SkipTrackVoteStatus` - current skip track vote status (if track is playing)
     - `List<KickUserVoteStatus>` - kick votes for each user being voted on
   - Nested classes:
     - `SkipTrackVoteStatus` - skip vote info with track metadata
     - `KickUserVoteStatus` - kick vote info with user metadata

### Service

4. **`VoteService.java`**
   - Business logic for vote operations
   - Methods:
     - `voteSkipTrack(UUID roomId, UUID userId)` - Vote to skip current track
     - `voteKickUser(UUID roomId, UUID userId, KickUserRequestDTO request)` - Vote to kick user
     - `getVoteStatus(UUID roomId, UUID userId)` - Get current vote counts
     - `withdrawSkipVote(UUID roomId, UUID userId)` - Withdraw skip track vote
     - `withdrawKickVote(UUID roomId, UUID userId, KickUserRequestDTO request)` - Withdraw kick user vote
   - Threshold calculation: `Math.max(1, Math.ceil(onlineMemberCount * 0.5))`
   - Automatic action execution when threshold is reached

### Controller

5. **`VoteController.java`**
   - REST endpoints for vote operations
   - Endpoints:
     - `POST /api/rooms/{roomId}/votes/skip` - Vote to skip track
     - `POST /api/rooms/{roomId}/votes/kick` - Vote to kick user
     - `GET /api/rooms/{roomId}/votes` - Get vote status
     - `DELETE /api/rooms/{roomId}/votes/skip` - Withdraw skip vote
     - `DELETE /api/rooms/{roomId}/votes/kick` - Withdraw kick vote
   - All endpoints require JWT authentication
   - User ID is extracted from JWT 'sub' claim

---

## Files Modified

### Repositories

1. **`VoteRepository.java`**

   - Added custom query methods:
     - `existsByRoomIdAndVoterIdAndPlaylistItemId()` - Check duplicate skip vote
     - `existsByRoomIdAndVoterIdAndTargetUserId()` - Check duplicate kick vote
     - `countByRoomIdAndPlaylistItemId()` - Count skip votes for a track
     - `countByRoomIdAndTargetUserId()` - Count kick votes for a user
     - `findAllKickUserVotesByRoomId()` - Get all kick votes with eager loading
     - `deleteSkipTrackVotesByPlaylistItemId()` - Clean up skip votes (after skip/finish)
     - `deleteKickUserVotesByTargetUserId()` - Clean up kick votes (after kick/leave)
     - `deleteSkipTrackVoteByVoter()` - Delete specific user's skip vote (for withdraw)
     - `deleteKickUserVoteByVoter()` - Delete specific user's kick vote (for withdraw)

2. **`RoomMemberRepository.java`**
   - Added custom query method:
     - `isRoomOwner(UUID roomId, UUID userId)` - Check if user is room owner

---

## Implementation Details

### Vote to Skip Track (`voteSkipTrack`)

**Workflow**:

1. Validate room exists
2. Validate user is an active room member
3. Check a track is currently playing (via `PlaybackRedisService.getCurrentPlaylistItemId()`)
4. Check user hasn't already voted for this track
5. Create vote record in PostgreSQL (`Vote` entity with type `SKIPTRACK`)
6. Count votes for this track
7. Get online member count from Redis
8. Calculate required vote count (50% threshold)
9. **If threshold reached**:
   - Call `PlaybackRedisService.skipTrack()` to skip track
   - Delete old skip votes for this track
   - Return success response with `thresholdReached = true`
10. **If threshold not reached**:
    - Return response with current vote count

**Exception Handling**:

- `ResourceNotFoundException` - room doesn't exist
- `ForbiddenException` - user is not a room member
- `InvalidRequestException` - no track playing or already voted

**TODO**: Emit WebSocket events (`TRACK_SKIPPED`, `VOTE_CAST`)

---

### Vote to Kick User (`voteKickUser`)

**Workflow**:

1. Validate room exists
2. Validate user (voter) is an active room member
3. Validate target user is an active room member
4. Validate user is not voting to kick themselves
5. Validate target user is not the room owner (via `RoomMemberRepository.isRoomOwner()`)
6. Check user hasn't already voted to kick this target
7. Create vote record in PostgreSQL (`Vote` entity with type `KICKUSER`)
8. Count votes for this target user
9. Get online member count from Redis
10. Calculate required vote count (50% threshold)
11. **If threshold reached**:
    - Soft delete: set `RoomMember.isActive = false`, update `lastActiveAt`
    - Remove user from Redis online members set
    - Delete old kick votes for this user
    - Return success response with `thresholdReached = true`
12. **If threshold not reached**:
    - Return response with current vote count

**Exception Handling**:

- `ResourceNotFoundException` - room or target user doesn't exist
- `ForbiddenException` - user is not a room member
- `InvalidRequestException` - validation fails (self-kick, owner kick, already voted, target not member)

**TODO**: Close WebSocket connection for kicked user, emit events (`USER_KICKED`, `VOTE_CAST`)

---

### Get Vote Status (`getVoteStatus`)

**Workflow**:

1. Validate room exists
2. Validate user is an active room member
3. Get online member count from Redis
4. Calculate required vote count (50% threshold)
5. **Get skip track vote status**:
   - Check if a track is currently playing
   - If yes, count skip votes for that track
   - Fetch track metadata from Redis (name, artist)
   - Create `SkipTrackVoteStatus` DTO
6. **Get kick user vote statuses**:
   - Query all `KICKUSER` votes for this room
   - Group by target user and count votes
   - Fetch target user display names
   - Create list of `KickUserVoteStatus` DTOs
7. Return `VoteStatusResponseDTO` with both skip and kick vote info

**Exception Handling**:

- `ResourceNotFoundException` - room doesn't exist
- `ForbiddenException` - user is not a room member

---

### Withdraw Skip Vote (`withdrawSkipVote`)

**Workflow**:

1. Validate room exists
2. Validate user is an active room member
3. Check a track is currently playing
4. Check user HAS voted for this track (opposite of vote check)
5. Delete the vote record (returns count of deleted rows)
6. If deletion failed (count = 0), throw error
7. Get updated vote counts
8. Return response with `voteRecorded = false` and updated counts

**Exception Handling**:

- `ResourceNotFoundException` - room doesn't exist
- `ForbiddenException` - user is not a room member
- `InvalidRequestException` - no track playing or user hasn't voted

**TODO**: Emit WebSocket event (`VOTE_WITHDRAWN`)

---

### Withdraw Kick Vote (`withdrawKickVote`)

**Workflow**:

1. Validate room exists
2. Validate user (voter) is an active room member
3. Validate target user exists (they might have left, but we still allow withdrawal)
4. Check user HAS voted to kick this target (opposite of vote check)
5. Delete the vote record (returns count of deleted rows)
6. If deletion failed (count = 0), throw error
7. Get updated vote counts
8. Return response with `voteRecorded = false` and updated counts

**Exception Handling**:

- `ResourceNotFoundException` - room or target user doesn't exist
- `ForbiddenException` - user is not a room member
- `InvalidRequestException` - user hasn't voted to kick this target

**TODO**: Emit WebSocket event (`VOTE_WITHDRAWN`)

---

## API Endpoints

### 1. POST /api/rooms/{roomId}/votes/skip

**Description**: Vote to skip the currently playing track.

**Authentication**: Required (JWT)

**Request**: No body

**Response**: `VoteResponseDTO`

```json
{
  "voteRecorded": true,
  "currentVoteCount": 3,
  "requiredVoteCount": 5,
  "onlineMemberCount": 10,
  "thresholdReached": false,
  "message": "Vote recorded. 3/5 votes to skip",
  "targetPlaylistItemId": "uuid-of-track"
}
```

**Success Cases**:

- `200 OK` - Vote recorded successfully
- If threshold reached, track is skipped and next track starts

**Error Cases**:

- `400 Bad Request` - No track playing or user already voted
- `401 Unauthorized` - Not authenticated
- `403 Forbidden` - User is not a room member
- `404 Not Found` - Room doesn't exist

---

### 2. POST /api/rooms/{roomId}/votes/kick

**Description**: Vote to kick a user from the room.

**Authentication**: Required (JWT)

**Request Body**: `KickUserRequestDTO`

```json
{
  "targetUserId": "uuid-of-user-to-kick"
}
```

**Response**: `VoteResponseDTO`

```json
{
  "voteRecorded": true,
  "currentVoteCount": 4,
  "requiredVoteCount": 5,
  "onlineMemberCount": 10,
  "thresholdReached": false,
  "message": "Vote recorded. 4/5 votes to kick",
  "targetUserId": "uuid-of-target-user"
}
```

**Success Cases**:

- `200 OK` - Vote recorded successfully
- If threshold reached, user is kicked from room

**Error Cases**:

- `400 Bad Request` - Validation fails (self-kick, owner kick, already voted, target not member)
- `401 Unauthorized` - Not authenticated
- `403 Forbidden` - User is not a room member
- `404 Not Found` - Room or target user doesn't exist

---

### 3. GET /api/rooms/{roomId}/votes

**Description**: Get current vote status for the room.

**Authentication**: Required (JWT)

**Request**: No body

**Response**: `VoteStatusResponseDTO`

```json
{
  "skipTrackVote": {
    "playlistItemId": "uuid-of-track",
    "trackName": "Song Name",
    "artistName": "Artist Name",
    "currentVoteCount": 3,
    "requiredVoteCount": 5,
    "onlineMemberCount": 10
  },
  "kickUserVotes": [
    {
      "targetUserId": "uuid-of-user",
      "targetUserDisplayName": "User Display Name",
      "currentVoteCount": 2,
      "requiredVoteCount": 5,
      "onlineMemberCount": 10
    }
  ]
}
```

**Success Cases**:

- `200 OK` - Vote status returned successfully
- If no track is playing, `skipTrackVote` is `null`
- If no kick votes exist, `kickUserVotes` is empty array

**Error Cases**:

- `401 Unauthorized` - Not authenticated
- `403 Forbidden` - User is not a room member
- `404 Not Found` - Room doesn't exist

---

### 4. DELETE /api/rooms/{roomId}/votes/skip

**Description**: Withdraw vote to skip the currently playing track.

**Authentication**: Required (JWT)

**Request**: No body

**Response**: `VoteResponseDTO`

```json
{
  "voteRecorded": false,
  "currentVoteCount": 2,
  "requiredVoteCount": 5,
  "onlineMemberCount": 10,
  "thresholdReached": false,
  "message": "Vote withdrawn successfully. 2/5 votes to skip",
  "targetPlaylistItemId": "uuid-of-track"
}
```

**Success Cases**:

- `200 OK` - Vote withdrawn successfully
- Updated vote count is returned

**Error Cases**:

- `400 Bad Request` - No track playing or user hasn't voted
- `401 Unauthorized` - Not authenticated
- `403 Forbidden` - User is not a room member
- `404 Not Found` - Room doesn't exist

---

### 5. DELETE /api/rooms/{roomId}/votes/kick

**Description**: Withdraw vote to kick a user from the room.

**Authentication**: Required (JWT)

**Request Body**: `KickUserRequestDTO`

```json
{
  "targetUserId": "uuid-of-user"
}
```

**Response**: `VoteResponseDTO`

```json
{
  "voteRecorded": false,
  "currentVoteCount": 1,
  "requiredVoteCount": 5,
  "onlineMemberCount": 10,
  "thresholdReached": false,
  "message": "Vote withdrawn successfully. 1/5 votes to kick",
  "targetUserId": "uuid-of-target-user"
}
```

**Success Cases**:

- `200 OK` - Vote withdrawn successfully
- Updated vote count is returned

**Error Cases**:

- `400 Bad Request` - User hasn't voted to kick this target
- `401 Unauthorized` - Not authenticated
- `403 Forbidden` - User is not a room member
- `404 Not Found` - Room or target user doesn't exist

---

## Business Rules

1. **Vote Threshold**: 50% of online members (configurable)

   - Calculated as: `Math.max(1, Math.ceil(onlineMemberCount * 0.5))`
   - Minimum 1 vote required

2. **Duplicate Votes**: Users can only vote once per track (skip) or per target user (kick)

3. **Skip Track**:

   - A track must be currently playing
   - When threshold reached, track is skipped immediately
   - Old skip votes are cleaned up after skip

4. **Kick User**:

   - Target user must be an active room member
   - User cannot vote to kick themselves
   - Room owner cannot be kicked
   - When threshold reached, user is kicked immediately (soft delete)
   - Old kick votes are cleaned up after kick

5. **Vote Cleanup**:

   - Skip votes are deleted when track is skipped or finished
   - Kick votes are deleted when user is kicked or leaves

6. **Automatic Execution**: When threshold is reached, action is executed immediately (skip track or kick user)

7. **Vote Withdrawal**:
   - Users can withdraw their votes at any time before threshold is reached
   - Once threshold is reached and action is executed, votes are cleaned up automatically
   - Withdrawal is only allowed if user has previously voted
   - Withdrawal returns updated vote counts to show current status

---

## Database Impact

### PostgreSQL

**Vote Table**:

- INSERT: New vote records created for each vote
- DELETE: Old votes cleaned up after action execution
- Query: Count votes, check duplicates, get all kick votes

**RoomMember Table**:

- UPDATE: Soft delete (set `isActive = false`) when user is kicked

### Redis

**Playback State**:

- Read: Get current playing track ID for skip votes
- Modify: Skip track when threshold reached

**Online Members Set**:

- Read: Get online member count for threshold calculation
- Modify: Remove kicked user from online members set

**Playlist Items**:

- Read: Get track metadata for vote status response

---

## Testing Notes

### Manual Testing

1. **Skip Track Vote**:

   - Create a room with 2+ users
   - Add a track to playlist
   - Vote to skip from different users
   - Verify threshold calculation and track skip

2. **Kick User Vote**:

   - Create a room with 3+ users
   - Vote to kick a user from different members
   - Verify owner cannot be kicked
   - Verify user cannot vote to kick themselves
   - Verify threshold calculation and user kick

3. **Vote Status**:

   - Get vote status with and without active votes
   - Verify skip vote status when track is/isn't playing
   - Verify kick vote status with multiple targets

4. **Vote Withdrawal**:
   - Vote to skip a track, then withdraw the vote
   - Verify vote count decreases
   - Vote to kick a user, then withdraw the vote
   - Verify vote count decreases
   - Try to withdraw without having voted (should fail)
   - Try to withdraw after threshold reached (vote should be gone)

### Edge Cases to Test

- Vote when room has 1 online member (threshold = 1)
- Vote when room has 2 online members (threshold = 1)
- Vote when room has 3 online members (threshold = 2)
- Vote to skip when no track is playing
- Vote to kick user who is not a member
- Vote to kick yourself
- Vote to kick room owner
- Duplicate vote attempts
- Withdraw vote without having voted
- Withdraw skip vote when no track is playing
- Withdraw kick vote for user who has left the room
- Vote, withdraw, then vote again (should work)
- Multiple users withdraw votes (verify counts update correctly)

---

## TODO Items

1. **WebSocket Events**:

   - Emit `TRACK_SKIPPED` event when track is skipped via vote
   - Emit `USER_KICKED` event when user is kicked via vote
   - Emit `VOTE_CAST` event when vote is recorded (threshold not reached)

2. **WebSocket Connection Management**:

   - Close WebSocket connection for kicked user
   - Notify kicked user with appropriate message

3. **Vote Cleanup on Track Change**:

   - Implement automatic cleanup of skip votes when track finishes naturally (not skipped)
   - This should be handled in playback service when track status changes to `PLAYED`

4. **Configuration**:

   - Make vote threshold percentage configurable (e.g., via application properties)
   - Consider different thresholds for skip vs kick

5. **Rate Limiting**:

   - Implement rate limiting on vote endpoints to prevent abuse

6. **Audit Trail**:
   - Consider logging vote actions for audit purposes
   - Track which users voted for what actions

---

## Dependencies

- **Spring Boot**: Core framework
- **Spring Data JPA**: PostgreSQL operations
- **Spring Data Redis**: Redis operations
- **Jakarta Validation**: DTO validation
- **Spring Security**: JWT authentication

---

## Architecture Notes

### Separation of Concerns

- **Controllers**: Handle HTTP requests, extract JWT user ID, delegate to services
- **Services**: Business logic, validation, threshold calculation, action execution
- **Repositories**: Database queries (PostgreSQL)
- **Redis Services**: Runtime state operations (online members, playback, playlist)
- **DTOs**: Request/response data structures

### Error Handling

All custom exceptions are handled by global exception handler (assumed to exist in project):

- `ResourceNotFoundException` → 404 Not Found
- `ForbiddenException` → 403 Forbidden
- `InvalidRequestException` → 400 Bad Request

### Transaction Management

- `VoteService` is annotated with `@Transactional` for atomic PostgreSQL operations
- `getVoteStatus()` is marked `@Transactional(readOnly = true)` for optimization
- Redis operations are atomic by design (no explicit transactions needed)

### Security

- All endpoints require JWT authentication
- User ID is extracted from JWT 'sub' claim in controller
- Room membership validation is performed in service layer

---

## References

- **PROJECT_OVERVIEW.md** - Section 2.8 (Skip Track Voting) and 2.9 (Kick User Voting)
- **POSTGRES_SCHEMA.md** - Section 6.1 (Vote Table)
- **REDIS_ARCHITECTURE.md** - Section 4 (Online Members Set)
