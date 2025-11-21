# Spotify User Library & Profile API (For AI Agents)

This document describes Spotify Web API endpoints required for:

- Adding a track to the **current user’s liked/saved tracks** ("Your Music" library)
- Adding a track to a **playlist**
- Getting the **currently logged-in user’s profile**
- Getting **another user’s public profile**

All examples use **full field structures** (no simplified responses) so agents can understand the complete data shape.

Common base info (for all endpoints):

```text
Base URL: https://api.spotify.com/v1
Header:  Authorization: Bearer <ACCESS_TOKEN>
         Content-Type: application/json
```

Required scopes are specified per endpoint.

---

## 1. Save Track(s) to Current User’s Library (Liked/Saved)

### 1.1 Endpoint
```http
PUT /me/tracks
```

**Purpose**  
Save one or more tracks to the current user’s **“Your Music” library** (liked/saved tracks).

**Required scope**  
- `user-library-modify`

### 1.2 Request (Query Param Variant)

You can pass track IDs via query string:

```http
PUT https://api.spotify.com/v1/me/tracks?ids={id1},{id2},...
Authorization: Bearer <ACCESS_TOKEN>
```

- `ids`: comma-separated list of track IDs (max 50)  
  Example: `ids=4iV5W9uYEdYUVa79Axb7Rh,6habFhsOp2NvshLv26DqMb`

### 1.3 Request (JSON Body Variant)

Alternatively, send IDs in the JSON body:

```http
PUT https://api.spotify.com/v1/me/tracks
Authorization: Bearer <ACCESS_TOKEN>
Content-Type: application/json

{
  "ids": [
    "4iV5W9uYEdYUVa79Axb7Rh",
    "6habFhsOp2NvshLv26DqMb"
  ]
}
```

### 1.4 Response

On success:
- HTTP status: `200 OK` or `201/204` depending on Spotify’s implementation/version
- **No response body** (empty)

On error (example structure):

```json
{
  "error": {
    "status": 401,
    "message": "The access token expired"
  }
}
```

---

## 2. Add Track(s) to a Playlist

### 2.1 Endpoint
```http
POST /playlists/{playlist_id}/tracks
```

**Purpose**  
Add one or more items (tracks or episodes) to a playlist.

**Required scopes** (one of):
- `playlist-modify-public` (for public playlists)
- `playlist-modify-private` (for private playlists)

### 2.2 Path Parameter

- `playlist_id` — The Spotify ID of the playlist (not URI).  
  Example: `37i9dQZF1DXcBWIGoYBM5M`

### 2.3 Query Parameters (optional)

- `position` — Zero-based index to insert items. If omitted, items are appended to the end.

Example:
```http
POST https://api.spotify.com/v1/playlists/37i9dQZF1DXcBWIGoYBM5M/tracks?position=0
```

### 2.4 Request Body (JSON)

You provide URIs and/or track IDs:

```json
{
  "uris": [
    "spotify:track:4iV5W9uYEdYUVa79Axb7Rh",
    "spotify:track:6habFhsOp2NvshLv26DqMb"
  ],
  "position": 0
}
```

- `uris` — Array of track or episode URIs.
- `position` — Same as query param; if both exist, Spotify’s behavior may prefer body.

### 2.5 Successful Response

On success, Spotify returns a JSON object including a playlist snapshot ID:

```json
{
  "snapshot_id": "JbtmHBdbqQKFHVG4y4lA3PqRmU9Cw8f+lm3PCrDvKqGkd9fX2Sgkq9s9LwUjWc0t"
}
```

- `snapshot_id` — A version identifier for the playlist’s current state.  
  Useful if you later want to reorder or remove items using optimistic concurrency.

On error, a typical error structure:

```json
{
  "error": {
    "status": 403,
    "message": "User not authorized to modify this playlist"
  }
}
```

---

## 3. Get Current User’s Profile (Logged-In User)

### 3.1 Endpoint
```http
GET /me
```

**Purpose**  
Get detailed profile information about the **current authenticated user**, based on the access token.

**Required scopes**  
- `user-read-email` (to receive `email`)
- `user-read-private` (to receive full private profile fields like `country`, `product`)

### 3.2 Example Request

```http
GET https://api.spotify.com/v1/me
Authorization: Bearer <ACCESS_TOKEN>
```

### 3.3 Example Response (Full Structure)

> Field values here are illustrative; structure is representative.

```json
{
  "country": "TR",
  "display_name": "Zeynep",
  "email": "zeynep@example.com",
  "explicit_content": {
    "filter_enabled": false,
    "filter_locked": false
  },
  "external_urls": {
    "spotify": "https://open.spotify.com/user/spotify_user_id"
  },
  "followers": {
    "href": null,
    "total": 42
  },
  "href": "https://api.spotify.com/v1/users/spotify_user_id",
  "id": "spotify_user_id",
  "images": [
    {
      "url": "https://profile-images.scdn.co/images/user-image-1",
      "height": null,
      "width": null
    }
  ],
  "product": "premium",
  "type": "user",
  "uri": "spotify:user:spotify_user_id"
}
```

### 3.4 Important Fields

- `id` — User’s Spotify ID (used in `/users/{user_id}` and playlist ownership etc.)
- `display_name` — Name shown in the UI.
- `email` — Email address (requires `user-read-email`).
- `country` — ISO 3166-1 alpha-2 country code.
- `product` — User’s subscription level (e.g. `free`, `premium`).
- `images[]` — Profile images.
- `followers.total` — Number of followers.

---

## 4. Get Another User’s Public Profile

### 4.1 Endpoint
```http
GET /users/{user_id}
```

**Purpose**  
Get **public profile information** for any Spotify user.

**Required scopes**  
- None for basic public data. (Some private info is never exposed.)

### 4.2 Path Parameter

- `user_id` — Spotify user ID, e.g. `party`, `wizzler`, or any ID obtained from `/me` or elsewhere.

Example:
```http
GET https://api.spotify.com/v1/users/party
Authorization: Bearer <ACCESS_TOKEN>
```

### 4.3 Example Response (Full Structure)

```json
{
  "display_name": "Party User",
  "external_urls": {
    "spotify": "https://open.spotify.com/user/party"
  },
  "followers": {
    "href": null,
    "total": 128
  },
  "href": "https://api.spotify.com/v1/users/party",
  "id": "party",
  "images": [
    {
      "url": "https://profile-images.scdn.co/images/user-image-party",
      "height": 300,
      "width": 300
    }
  ],
  "type": "user",
  "uri": "spotify:user:party"
}
```

### 4.4 Differences vs `/me`

- `/users/{user_id}` returns **public** profile only:
  - No `email` field.
  - No `country`, `product`, or `explicit_content` for other users.
- `/me` returns **current user’s private profile**, including email, country, product, etc.

---

## 5. Optional: Check if a Track is Already Liked (Saved)

Useful when building a UI that toggles a heart/like icon.

### 5.1 Endpoint
```http
GET /me/tracks/contains
```

**Purpose**  
Check if one or more tracks are already saved in the current user’s library.

**Required scope**  
- `user-library-read`

### 5.2 Request

```http
GET https://api.spotify.com/v1/me/tracks/contains?ids=4iV5W9uYEdYUVa79Axb7Rh,6habFhsOp2NvshLv26DqMb
Authorization: Bearer <ACCESS_TOKEN>
```

### 5.3 Response

An array of booleans corresponding to the requested IDs:

```json
[ true, false ]
```

- Here, first ID is saved, second is not.

---

## 6. Summary Table

| # | Purpose                                          | Method | Path                          | Key Scope(s)                |
|---|--------------------------------------------------|--------|-------------------------------|-----------------------------|
| 1 | Save tracks to current user’s library (likes)    | PUT    | `/me/tracks`                  | `user-library-modify`       |
| 2 | Add items to a playlist                          | POST   | `/playlists/{playlist_id}/tracks` | `playlist-modify-public`/`private` |
| 3 | Get current user’s profile                       | GET    | `/me`                         | `user-read-email`, `user-read-private` |
| 4 | Get another user’s public profile                | GET    | `/users/{user_id}`            | none (public)              |
| 5 | Check if tracks are saved in user’s library      | GET    | `/me/tracks/contains`         | `user-library-read`         |

---

This document is structured to be directly consumable by AI agents implementing Spotify-related flows: like/heart feature, add-to-playlist actions, and user profile retrieval.

