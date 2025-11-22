# Spotify Player API Endpoint Documentation

This document describes the **Spotify Player API** endpoints used to **control playback** (play, pause, next, previous, seek, volume, queue, etc.) from a frontend or backend service. It is optimized for AI agents and automation workflows.

---

## 0. Common Information

**Base URL**
```text
https://api.spotify.com/v1
```

**Common Headers**
```http
Authorization: Bearer <ACCESS_TOKEN>
Content-Type: application/json
```

- `<ACCESS_TOKEN>` must be obtained via Spotify OAuth2 (Authorization Code Flow).
- The token must have appropriate scopes, e.g. `user-modify-playback-state`, `user-read-playback-state`, `user-read-currently-playing`.

---

## 1. Start / Resume Playback

**Endpoint**
```http
PUT /v1/me/player/play
```

**Purpose**
Start or resume playback on a user’s Spotify player.

**Query Parameters (optional)**
- `device_id` — ID of the target device (e.g. Web Playback SDK player id).

**Request Body (optional)**
At least one of `context_uri` or `uris` may be provided. If omitted, resumes the current context.

```json
{
  "context_uri": "spotify:playlist:37i9dQZF1DXcBWIGoYBM5M",
  "uris": ["spotify:track:4iV5W9uYEdYUVa79Axb7Rh"],
  "offset": { "position": 0 },
  "position_ms": 0
}
```

- `context_uri` — album / playlist / artist Spotify URI.
- `uris` — list of track URIs.
- `offset.position` — index of the track to start with.
- `position_ms` — initial playback position in milliseconds.

**Example Request**
```http
PUT https://api.spotify.com/v1/me/player/play?device_id=<DEVICE_ID>
Authorization: Bearer <ACCESS_TOKEN>
Content-Type: application/json

{
  "uris": ["spotify:track:4iV5W9uYEdYUVa79Axb7Rh"],
  "position_ms": 0
}
```

**Response**
- `204 No Content` on success.

---

## 2. Pause Playback

**Endpoint**
```http
PUT /v1/me/player/pause
```

**Purpose**
Pause the user’s currently active playback.

**Query Parameters (optional)**
- `device_id` — ID of the target device.

**Example Request**
```http
PUT https://api.spotify.com/v1/me/player/pause?device_id=<DEVICE_ID>
Authorization: Bearer <ACCESS_TOKEN>
```

**Response**
- `204 No Content` on success.

---

## 3. Skip to Next Track

**Endpoint**
```http
POST /v1/me/player/next
```

**Purpose**
Skip to the next track in the current context.

**Query Parameters (optional)**
- `device_id` — ID of the target device.

**Example Request**
```http
POST https://api.spotify.com/v1/me/player/next?device_id=<DEVICE_ID>
Authorization: Bearer <ACCESS_TOKEN>
```

**Response**
- `204 No Content` on success.

---

## 4. Skip to Previous Track

**Endpoint**
```http
POST /v1/me/player/previous
```

**Purpose**
Skip to the previous track in the current context.

**Query Parameters (optional)**
- `device_id` — ID of the target device.

**Example Request**
```http
POST https://api.spotify.com/v1/me/player/previous?device_id=<DEVICE_ID>
Authorization: Bearer <ACCESS_TOKEN>
```

**Response**
- `204 No Content` on success.

---

## 5. Seek To Position in Currently Playing Track

**Endpoint**
```http
PUT /v1/me/player/seek
```

**Purpose**
Seek to a given position in the user’s currently playing track.

**Query Parameters (required)**
- `position_ms` — Target position in milliseconds (integer).

**Query Parameters (optional)**
- `device_id` — ID of the target device.

**Example Request**
```http
PUT https://api.spotify.com/v1/me/player/seek?position_ms=60000&device_id=<DEVICE_ID>
Authorization: Bearer <ACCESS_TOKEN>
```

**Response**
- `204 No Content` on success.

---

## 6. Set Volume for User’s Playback

**Endpoint**
```http
PUT /v1/me/player/volume
```

**Purpose**
Set the volume of the user’s currently active device.

**Query Parameters (required)**
- `volume_percent` — Volume as integer between 0 and 100.

**Query Parameters (optional)**
- `device_id` — ID of the target device.

**Example Request**
```http
PUT https://api.spotify.com/v1/me/player/volume?volume_percent=50&device_id=<DEVICE_ID>
Authorization: Bearer <ACCESS_TOKEN>
```

**Response**
- `204 No Content` on success.

---

## 7. Set Repeat Mode

**Endpoint**
```http
PUT /v1/me/player/repeat
```

**Purpose**
Set the repeat mode for the user’s playback.

**Query Parameters (required)**
- `state` — One of:
  - `track` — Repeat the current track.
  - `context` — Repeat the current context (playlist, album, etc.).
  - `off` — Turn repeat off.

**Query Parameters (optional)**
- `device_id` — ID of the target device.

**Example Request**
```http
PUT https://api.spotify.com/v1/me/player/repeat?state=track&device_id=<DEVICE_ID>
Authorization: Bearer <ACCESS_TOKEN>
```

**Response**
- `204 No Content` on success.

---

## 8. Set Shuffle Mode

**Endpoint**
```http
PUT /v1/me/player/shuffle
```

**Purpose**
Toggle shuffle on or off for the user’s playback.

**Query Parameters (required)**
- `state` — `true` or `false`.

**Query Parameters (optional)**
- `device_id` — ID of the target device.

**Example Request**
```http
PUT https://api.spotify.com/v1/me/player/shuffle?state=true&device_id=<DEVICE_ID>
Authorization: Bearer <ACCESS_TOKEN>
```

**Response**
- `204 No Content` on success.

---

## 9. Get Current Playback State

**Endpoint**
```http
GET /v1/me/player
```

**Purpose**
Retrieve information about the user’s current playback state.

**Example Request**
```http
GET https://api.spotify.com/v1/me/player
Authorization: Bearer <ACCESS_TOKEN>
```

**Example Response**
```json
{
  "device": {
    "id": "string",
    "is_active": false,
    "is_private_session": false,
    "is_restricted": false,
    "name": "Kitchen speaker",
    "type": "computer",
    "volume_percent": 59,
    "supports_volume": false
  },
  "repeat_state": "string",
  "shuffle_state": false,
  "context": {
    "type": "string",
    "href": "string",
    "external_urls": {
      "spotify": "string"
    },
    "uri": "string"
  },
  "timestamp": 0,
  "progress_ms": 0,
  "is_playing": false,
  "item": {
    "album": {
      "album_type": "compilation",
      "total_tracks": 9,
      "available_markets": [
        "CA",
        "BR",
        "IT"
      ],
      "external_urls": {
        "spotify": "string"
      },
      "href": "string",
      "id": "2up3OPMp9Tb4dAKM2erWXQ",
      "images": [
        {
          "url": "https://i.scdn.co/image/ab67616d00001e02ff9ca10b55ce82ae553c8228",
          "height": 300,
          "width": 300
        }
      ],
      "name": "string",
      "release_date": "1981-12",
      "release_date_precision": "year",
      "restrictions": {
        "reason": "market"
      },
      "type": "album",
      "uri": "spotify:album:2up3OPMp9Tb4dAKM2erWXQ",
      "artists": [
        {
          "external_urls": {
            "spotify": "string"
          },
          "href": "string",
          "id": "string",
          "name": "string",
          "type": "artist",
          "uri": "string"
        }
      ]
    },
    "artists": [
      {
        "external_urls": {
          "spotify": "string"
        },
        "href": "string",
        "id": "string",
        "name": "string",
        "type": "artist",
        "uri": "string"
      }
    ],
    "available_markets": [
      "string"
    ],
    "disc_number": 0,
    "duration_ms": 0,
    "explicit": false,
    "external_ids": {
      "isrc": "string",
      "ean": "string",
      "upc": "string"
    },
    "external_urls": {
      "spotify": "string"
    },
    "href": "string",
    "id": "string",
    "is_playable": false,
    "linked_from": {},
    "restrictions": {
      "reason": "string"
    },
    "name": "string",
    "popularity": 0,
    "preview_url": "string",
    "track_number": 0,
    "type": "track",
    "uri": "string",
    "is_local": false
  },
  "currently_playing_type": "string",
  "actions": {
    "interrupting_playback": false,
    "pausing": false,
    "resuming": false,
    "seeking": false,
    "skipping_next": false,
    "skipping_prev": false,
    "toggling_repeat_context": false,
    "toggling_shuffle": false,
    "toggling_repeat_track": false,
    "transferring_playback": false
  }
}
```

---

## 10. Get Currently Playing Track

**Endpoint**
```http
GET /v1/me/player/currently-playing
```

**Purpose**
Retrieve information about the currently playing track.

**Example Request**
```http
GET https://api.spotify.com/v1/me/player/currently-playing
Authorization: Bearer <ACCESS_TOKEN>
```

**Example Response (simplified)**
```json
{
  "is_playing": true,
  "progress_ms": 45000,
  "item": {
    "id": "4iV5W9uYEdYUVa79Axb7Rh",
    "name": "Track Name",
    "artists": [
      { "name": "Artist" }
    ]
  }
}
```

---

## 11. Get Available Devices

**Endpoint**
```http
GET /v1/me/player/devices
```

**Purpose**
Retrieve the list of a user’s available Spotify Connect devices.

**Example Request**
```http
GET https://api.spotify.com/v1/me/player/devices
Authorization: Bearer <ACCESS_TOKEN>
```

**Example Response (simplified)**
```json
{
  "devices": [
    {
      "id": "<DEVICE_ID>",
      "is_active": true,
      "is_restricted": false,
      "name": "Web Player (My App)",
      "type": "Computer",
      "volume_percent": 50
    }
  ]
}
```

---

## 12. Transfer Playback to Another Device

**Endpoint**
```http
PUT /v1/me/player
```

**Purpose**
Transfer playback to a specific device (e.g. Web Playback SDK player).

**Request Body**
```json
{
  "device_ids": ["<DEVICE_ID>"],
  "play": true
}
```

- `device_ids` — Array with one or more device IDs.
- `play` — If `true`, starts playback after transferring.

**Example Request**
```http
PUT https://api.spotify.com/v1/me/player
Authorization: Bearer <ACCESS_TOKEN>
Content-Type: application/json

{
  "device_ids": ["<DEVICE_ID>"],
  "play": true
}
```

**Response**
- `204 No Content` on success.

---

## 13. Add Item to Playback Queue

**Endpoint**
```http
POST /v1/me/player/queue
```

**Purpose**
Add a track or episode to the end of the current playback queue.

**Query Parameters (required)**
- `uri` — Spotify URI of the item to add (e.g. `spotify:track:...`).

**Query Parameters (optional)**
- `device_id` — ID of the target device.

**Example Request**
```http
POST "https://api.spotify.com/v1/me/player/queue?uri=spotify:track:4iV5W9uYEdYUVa79Axb7Rh&device_id=<DEVICE_ID>"
Authorization: Bearer <ACCESS_TOKEN>
```

**Response**
- `204 No Content` on success.

---

## 14. High-Level Frontend Integration Flow (Web Playback)

Typical flow when controlling Spotify playback from a web frontend:

1. User logs in via Spotify OAuth2 → obtain `access_token`.
2. Frontend initializes **Spotify Web Playback SDK** → gets a `device_id` representing the web player.
3. Use the Player API endpoints with that `device_id` to control playback:
   - `PUT /v1/me/player/play?device_id=<DEVICE_ID>`
   - `PUT /v1/me/player/pause?device_id=<DEVICE_ID>`
   - `POST /v1/me/player/next?device_id=<DEVICE_ID>`
   - `POST /v1/me/player/queue?uri=...&device_id=<DEVICE_ID>`
4. Optionally poll `GET /v1/me/player` or `GET /v1/me/player/currently-playing` to sync UI state.

---

## Endpoint Summary Table

| #  | Purpose                          | Method | Path                                   |
|----|----------------------------------|--------|----------------------------------------|
| 1  | Start / Resume playback          | PUT    | `/v1/me/player/play`                   |
| 2  | Pause playback                   | PUT    | `/v1/me/player/pause`                  |
| 3  | Next track                       | POST   | `/v1/me/player/next`                   |
| 4  | Previous track                   | POST   | `/v1/me/player/previous`               |
| 5  | Seek position                    | PUT    | `/v1/me/player/seek`                   |
| 6  | Set volume                       | PUT    | `/v1/me/player/volume`                 |
| 7  | Set repeat mode                  | PUT    | `/v1/me/player/repeat`                 |
| 8  | Set shuffle mode                 | PUT    | `/v1/me/player/shuffle`                |
| 9  | Get playback state               | GET    | `/v1/me/player`                        |
| 10 | Get currently playing item       | GET    | `/v1/me/player/currently-playing`      |
| 11 | Get available devices            | GET    | `/v1/me/player/devices`                |
| 12 | Transfer playback to a device    | PUT    | `/v1/me/player`                        |
| 13 | Add item to queue                | POST   | `/v1/me/player/queue`                  |

---

This document is designed to be directly consumable by AI agents and backend/frontend automation scripts that need to control Spotify playback programmatically.

