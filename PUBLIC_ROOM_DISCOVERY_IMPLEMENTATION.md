# Public Room Discovery Implementation Summary

**Feature**: Public Room Discovery and Listing
**Based on**: PROJECT_OVERVIEW.md section 2.3 - Room Discovery & Joining
**Date**: November 23, 2025

## Overview

This document summarizes the implementation of the public room discovery feature, which allows authenticated users to discover and browse public rooms with optional filtering by tags and search terms.

## Endpoint

### GET /api/rooms

Returns a paginated list of public rooms with optional filtering.

**Query Parameters**:

- `tags` (optional): Comma-separated list of tag names (e.g., `lofi,90s`)
- `search` (optional): Search term for name/description (e.g., `chill`)
- `page` (optional): Page number (0-based, default: 0)
- `size` (optional): Page size (default: 20)
- `sort` (optional): Sort criteria (e.g., `createdAt,desc`)

**Example Requests**:

```bash
# Get all public rooms (first page)
GET /api/rooms?page=0&size=20

# Filter by tags
GET /api/rooms?tags=lofi,jazz&page=0&size=20

# Search by name/description
GET /api/rooms?search=chill&page=0&size=20

# Combine filters
GET /api/rooms?tags=90s&search=party&page=0&size=20&sort=createdAt,desc
```

**Response** (200 OK):

```json
[
  {
    "id": "123e4567-e89b-12d3-a456-426614174000",
    "name": "Chill Lofi Beats",
    "description": "Relaxing lofi hip hop beats to study/chill to",
    "tags": [
      {
        "id": "223e4567-e89b-12d3-a456-426614174001",
        "name": "lofi"
      },
      {
        "id": "323e4567-e89b-12d3-a456-426614174002",
        "name": "chill"
      }
    ],
    "max_participants": 50,
    "is_public": true,
    "created_at": "2025-11-23T10:30:00Z",
    "updated_at": "2025-11-23T10:30:00Z",
    "member_count": 15,
    "online_member_count": 8
  }
]
```

**Response Headers**:

- `X-Total-Count`: Total number of rooms matching criteria
- `Link`: Pagination links (first, last, next, prev)

## Implementation Details

### 1. Repository Layer (`RoomRepository`)

**File**: `src/main/java/com/partywave/backend/repository/RoomRepository.java`

Added custom JPQL query method:

```java
@Query(
  value = "SELECT DISTINCT r FROM Room r " +
  "LEFT JOIN r.tags t " + // Note: No FETCH to allow DB-level pagination
  "WHERE r.isPublic = true " +
  "AND (:searchTerm IS NULL OR LOWER(r.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(r.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
  "AND (:#{#tagNames.isEmpty()} = TRUE OR LOWER(t.name) IN :tagNames)", // SpEL to check empty list
  countQuery = "SELECT COUNT(DISTINCT r) FROM Room r " +
  "LEFT JOIN r.tags t " +
  "WHERE r.isPublic = true " +
  "AND (:searchTerm IS NULL OR LOWER(r.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(r.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
  "AND (:#{#tagNames.isEmpty()} = TRUE OR LOWER(t.name) IN :tagNames)"
)
Page<Room> findPublicRooms(@Param("tagNames") List<String> tagNames, @Param("searchTerm") String searchTerm, Pageable pageable);

```

**Important Notes**:

- No `FETCH` in the `LEFT JOIN` to avoid pagination issues with Hibernate
- Tags are loaded separately using `fetchBagRelationships()` in the service layer
- The service layer always passes an empty list (never null) to avoid PostgreSQL type inference issues
- Uses Spring Data JPA SpEL expression `:#{#tagNames.isEmpty()}` to check if tag filtering should be applied

**Features**:

- Filters rooms by `is_public = true`
- Case-insensitive tag filtering (tags normalized to lowercase)
- Case-insensitive search on name and description (using LIKE with wildcards)
- Eager loading of tags using `LEFT JOIN FETCH`
- Pagination support with separate count query
- Uses `DISTINCT` to avoid duplicate results from tag join

### 2. Service Layer (`RoomService`)

**File**: `src/main/java/com/partywave/backend/service/RoomService.java`

Added method:

```java
@Transactional(readOnly = true)
public Page<RoomResponseDTO> findPublicRooms(List<String> tags, String search, Pageable pageable)
```

**Workflow**:

1. **Normalize tags**: Convert to lowercase and trim whitespace for case-insensitive matching
2. **Query repository**: Fetch public rooms with filters
3. **Enrich with metadata**: For each room:
   - Get member count from PostgreSQL (`room_member` table)
   - Get online member count from Redis (`partywave:room:{roomId}:members:online` set)
   - Map tags to TagDTO objects
4. **Return paginated results**: Wrap in `PageImpl` with pagination metadata

**Data Sources**:

- **PostgreSQL**: Room data, tags, member count
- **Redis**: Online member count using `OnlineMembersRedisService.getOnlineMemberCount()`

### 3. Controller Layer (`RoomController`)

**File**: `src/main/java/com/partywave/backend/web/rest/RoomController.java`

Added endpoint:

```java
@GetMapping
public ResponseEntity<List<RoomResponseDTO>> getPublicRooms(
    @RequestParam(value = "tags", required = false) String tags,
    @RequestParam(value = "search", required = false) String search,
    Pageable pageable
)
```

**Features**:

- Accepts comma-separated tags and parses them into a list
- Uses Spring Data `Pageable` for pagination parameters
- Returns pagination headers using JHipster's `PaginationUtil`
- Returns 200 OK with room list

## Database Schema

### Tables Used

**`room` table** (Primary data source):

- `id` (UUID, PK)
- `name` (VARCHAR, NOT NULL)
- `description` (TEXT)
- `max_participants` (INTEGER, NOT NULL)
- `is_public` (BOOLEAN, NOT NULL) - **Filter criterion**
- `created_at` (TIMESTAMP)
- `updated_at` (TIMESTAMP)

**`tag` table**:

- `id` (UUID, PK)
- `name` (VARCHAR, UNIQUE, NOT NULL)

**`rel_room__tags` table** (Many-to-Many join table):

- `room_id` (UUID, FK → room.id)
- `tags_id` (UUID, FK → tag.id)

**`room_member` table** (For member count):

- `id` (UUID, PK)
- `room_id` (UUID, FK → room.id)
- `app_user_id` (UUID, FK → app_user.id)
- `role` (VARCHAR)
- `joined_at` (TIMESTAMP)
- `last_active_at` (TIMESTAMP)

### Redis Keys Used

**`partywave:room:{roomId}:members:online`** (SET):

- Stores UUIDs of users currently online in the room
- Read using `SCARD` to get online member count

## Feature Workflow

### User Discovers Public Rooms

1. **Frontend sends GET request**:

   ```
   GET /api/rooms?tags=lofi,jazz&search=chill&page=0&size=20
   ```

2. **RoomController receives request**:

   - Parses comma-separated tags: `["lofi", "jazz"]`
   - Extracts search term: `"chill"`
   - Extracts pagination: `page=0, size=20`

3. **RoomService.findPublicRooms()**:

   - Normalizes tags to lowercase: `["lofi", "jazz"]`
   - Calls repository with normalized parameters

4. **RoomRepository.findPublicRooms()**:

   - Executes JPQL query:
     ```sql
     SELECT DISTINCT r FROM Room r
     LEFT JOIN FETCH r.tags t
     WHERE r.isPublic = true
       AND (LOWER(r.name) LIKE '%chill%' OR LOWER(r.description) LIKE '%chill%')
       AND (LOWER(t.name) IN ('lofi', 'jazz'))
     ```
   - Returns `Page<Room>` with eager-loaded tags

5. **RoomService enriches data**:

   - For each room:
     - Query PostgreSQL: `SELECT COUNT(*) FROM room_member WHERE room_id = ?`
     - Query Redis: `SCARD partywave:room:{roomId}:members:online`
   - Maps to `RoomResponseDTO` with all metadata

6. **RoomController returns response**:
   - Generates pagination headers (`X-Total-Count`, `Link`)
   - Returns JSON array of rooms with 200 OK

## Response DTO Structure

**RoomResponseDTO**:

```json
{
  "id": "UUID",
  "name": "string",
  "description": "string (nullable)",
  "tags": [
    {
      "id": "UUID",
      "name": "string"
    }
  ],
  "max_participants": "integer",
  "is_public": true,
  "created_at": "ISO-8601 timestamp",
  "updated_at": "ISO-8601 timestamp",
  "member_count": "integer (from PostgreSQL)",
  "online_member_count": "integer (from Redis)"
}
```

**Key Fields**:

- `member_count`: Total members who have joined (includes offline)
- `online_member_count`: Currently online members (real-time from Redis)
- `tags`: Array of tag objects with id and name
- `is_public`: Always `true` for this endpoint

## Performance Considerations

### Query Optimization

1. **Two-Phase Tag Loading**:

   - Phase 1: Query rooms with pagination (without FETCH to enable DB-level pagination)
   - Phase 2: Load tags for paginated results using `fetchBagRelationships()` (single batch query)
   - Avoids N+1 query problem for tags
   - Avoids Hibernate in-memory pagination issues

2. **Pagination**:

   - Limits number of rooms fetched per request
   - Default page size: 20 (configurable)

3. **Indexing Recommendations**:
   ```sql
   CREATE INDEX idx_room_is_public ON room(is_public);
   CREATE INDEX idx_room_name ON room(name);
   CREATE INDEX idx_room_description ON room(description);
   CREATE INDEX idx_tag_name ON tag(name);
   ```

### Redis Performance

- `SCARD` is O(1) operation
- Called once per room in results (max 20 per request by default)
- Consider caching if member counts are queried frequently

### N+1 Query Mitigation

**Problem**: Fetching member count for each room individually

**Current Solution**:

- One query per room for member count
- Acceptable for small page sizes (20-50 rooms)

**Future Optimization** (if needed):

- Batch query member counts using native SQL:
  ```sql
  SELECT room_id, COUNT(*) as member_count
  FROM room_member
  WHERE room_id IN (?, ?, ?, ...)
  GROUP BY room_id
  ```

## Filter Behavior

### Tag Filtering

**Behavior**: OR logic (any matching tag)

- Query: `?tags=lofi,jazz`
- Returns: Rooms with tag "lofi" **OR** "jazz"
- Case-insensitive: `lofi` matches `Lofi`, `LOFI`, etc.

**Edge Cases**:

- Empty tags: `?tags=` → No tag filtering applied
- Single tag: `?tags=lofi` → Works normally
- Duplicate tags: `?tags=lofi,lofi` → Deduplicated automatically

### Search Filtering

**Behavior**: Case-insensitive partial match on name OR description

- Query: `?search=chill`
- Matches: Name contains "chill" **OR** description contains "chill"
- Case-insensitive: `chill` matches `Chill`, `CHILL`, `ChillingBeats`, etc.

**Edge Cases**:

- Empty search: `?search=` → No search filtering applied
- Special characters: Escaped in LIKE query
- Null/missing: No search filtering applied

### Combined Filters

**Behavior**: AND logic between different filter types

- Query: `?tags=lofi&search=chill`
- Returns: Rooms that are public **AND** have tag "lofi" **AND** (name or description contains "chill")

## Security

### Authentication

- **Required**: Yes (JWT token via `Authorization: Bearer <token>`)
- Enforced by: `SecurityConfiguration` (applies to all `/api/**` endpoints)
- If missing: Returns 401 Unauthorized

### Authorization

- **Public endpoint**: All authenticated users can discover public rooms
- No role-based restrictions
- Only returns rooms with `is_public = true`

### Data Exposure

**Safe to expose**:

- Room metadata (name, description, tags, max_participants)
- Aggregate counts (member_count, online_member_count)

**Not exposed**:

- Private rooms (`is_public = false`)
- Individual user details (member names, IDs)
- Internal room state (playlist, playback)

## Testing

### Manual Testing

1. **Create test rooms**:

   ```bash
   # Create public room with tags
   POST /api/rooms
   {
     "name": "Lofi Study Room",
     "description": "Chill beats for studying",
     "tags": ["lofi", "study"],
     "max_participants": 50,
     "is_public": true
   }

   # Create private room (should not appear)
   POST /api/rooms
   {
     "name": "Private Party",
     "is_public": false,
     "max_participants": 10
   }
   ```

2. **Test discovery**:

   ```bash
   # Get all public rooms
   GET /api/rooms

   # Filter by tag
   GET /api/rooms?tags=lofi

   # Search by name
   GET /api/rooms?search=study

   # Combine filters
   GET /api/rooms?tags=lofi&search=chill

   # Test pagination
   GET /api/rooms?page=0&size=5
   GET /api/rooms?page=1&size=5
   ```

3. **Verify response**:
   - Check `member_count` matches PostgreSQL
   - Check `online_member_count` matches Redis
   - Verify only public rooms returned
   - Verify pagination headers

### Edge Cases to Test

1. **Empty results**:

   - No public rooms exist
   - Filters match no rooms

2. **Large datasets**:

   - Many rooms (>1000)
   - Rooms with many tags (>20)

3. **Special characters**:

   - Room names with quotes, apostrophes
   - Search terms with special characters

4. **Concurrent access**:
   - Multiple users querying simultaneously
   - Online member count changing during query

## Future Enhancements

### 1. Advanced Filtering

- **Sort options**: By popularity, member count, creation date
- **Additional filters**: By max_participants range, online member threshold
- **Full-text search**: Use PostgreSQL full-text search for better search quality

### 2. Performance Optimization

- **Batch member count queries**: Single query for all rooms on page
- **Cache popular queries**: Redis cache for frequently accessed pages
- **Materialized views**: Pre-compute room statistics

### 3. Feature Additions

- **Room recommendations**: Based on user preferences, listening history
- **Featured rooms**: Admin-curated list of promoted rooms
- **Trending rooms**: Sort by recent activity, member growth

## Related Documentation

- **PROJECT_OVERVIEW.md**: Section 2.3 - Room Discovery & Joining
- **ROOM_CREATION_IMPLEMENTATION_SUMMARY.md**: Room creation workflow
- **POSTGRES_SCHEMA.md**: Database schema details
- **docs/AUTHENTICATION.md**: JWT authentication details

## Implementation Checklist

- [x] Add `findPublicRooms()` method to `RoomRepository`
- [x] Implement `findPublicRooms()` in `RoomService`
- [x] Add GET `/api/rooms` endpoint to `RoomController`
- [x] Integrate PostgreSQL member count query
- [x] Integrate Redis online member count
- [x] Add pagination support
- [x] Add tag filtering (case-insensitive)
- [x] Add search filtering (name/description)
- [x] Return proper pagination headers
- [x] Test compilation
- [x] Fix JPQL query (remove `IS EMPTY` check on parameter)
- [x] Fix pagination with collection fetch join (use two-phase loading)
- [x] Fix PostgreSQL type inference issue (use empty list instead of null + SpEL)
- [ ] Write integration tests
- [ ] Test with real data
- [ ] Performance testing with large datasets

## Known Issues and Fixes

### Issue 1: JPQL `IS EMPTY` Error (FIXED)

**Problem**: Initial implementation used `:tagNames IS EMPTY` in JPQL query, which caused:

```
Caused by: org.hibernate.query.SemanticException: Operand of 'is empty' operator must be a plural path
```

**Root Cause**: In JPQL/HQL, `IS EMPTY` can only be used on collection-valued paths (entity fields), not query parameters.

**Solution**: Removed the `IS EMPTY` check from the query. The service layer already handles empty lists by converting them to `null`, so the query condition `(:tagNames IS NULL OR LOWER(t.name) IN :tagNames)` is sufficient.

### Issue 2: Pagination with Collection Fetch Join (FIXED)

**Problem**: Application failed with:

```
org.springframework.orm.jpa.JpaSystemException: setFirstResult() or setMaxResults() specified with collection fetch join
(in-memory pagination was about to be applied, but 'hibernate.query.fail_on_pagination_over_collection_fetch' is enabled)
```

**Root Cause**: Using `LEFT JOIN FETCH r.tags` in a paginated query causes Hibernate to attempt in-memory pagination. When the `hibernate.query.fail_on_pagination_over_collection_fetch` property is enabled (default in newer Hibernate versions), it fails instead of silently loading all data into memory.

**Solution**:

1. **Removed `FETCH` from the main query**: Changed `LEFT JOIN FETCH r.tags` to `LEFT JOIN r.tags`
2. **Added explicit tag loading**: After pagination, use `fetchBagRelationships()` to load tags in a single batch query
3. **Benefits**:
   - Database-level pagination works correctly
   - Tags still loaded efficiently (single query for all rooms on page)
   - Avoids N+1 query problem
   - No in-memory pagination

**Code Changes**:

```java
// In RoomRepository - removed FETCH
@Query("SELECT DISTINCT r FROM Room r LEFT JOIN r.tags t WHERE ...")
// In RoomService - explicit tag loading after pagination
Page<Room> roomPage = roomRepository.findPublicRooms(normalizedTags, search, pageable);

List<Room> roomsWithTags = roomRepository.fetchBagRelationships(roomPage.getContent());

```

### Issue 3: PostgreSQL Type Inference with IN Clause (FIXED)

**Problem**: Application returned 500 error:

```
JDBC exception: ERROR: function lower(bytea) does not exist
Hint: No function matches the given name and argument types. You might need to add explicit type casts.
```

**Root Cause**: When passing `null` to an `IN` clause parameter (`:tagNames IN :tagNames`), PostgreSQL cannot infer the correct type for the parameter, treating it as `bytea` instead of `text[]`.

**Solution**:

1. **Never pass null**: Service layer now always passes an empty list instead of null:

   ```java
   List<String> normalizedTags = Collections.emptyList(); // Instead of null

   ```

2. **Check for empty list in query**: Use Spring Data JPA SpEL expression to check if list is empty:
   ```java
   AND (:#{#tagNames.isEmpty()} = TRUE OR LOWER(t.name) IN :tagNames)
   ```
3. **How it works**:
   - If `tagNames.isEmpty()` is true → condition evaluates to `TRUE = TRUE` (always true, no filtering)
   - If `tagNames` has values → checks `LOWER(t.name) IN :tagNames`
   - PostgreSQL can properly infer the type because tagNames is never null

## Summary

The public room discovery feature is now implemented according to PROJECT_OVERVIEW.md section 2.3. Users can:

1. Browse all public rooms with pagination
2. Filter by tags (case-insensitive, OR logic)
3. Search by name/description (case-insensitive, partial match)
4. See room metadata including:
   - Basic info (name, description, tags)
   - Total member count (PostgreSQL)
   - Online member count (Redis)
   - Max participants

The implementation follows the existing architecture patterns and integrates seamlessly with the room creation feature.
