# Spotify Track Search + Playback Integration API (For AI Agents)

This document describes all Spotify Web API endpoints required to:

- Build a **search bar** on the frontend
- Let users search for **Spotify tracks** in real time
- Retrieve **URIs, metadata, and album images**
- Play selected tracks on **any Spotify player** (Web Playback SDK, device, mobile, desktop)

This file is optimized for AI agents, automation, and backend/frontend integration.

---

# 0. Base Information

**Base URL**
```
https://api.spotify.com/v1
```

**Required Header**
```
Authorization: Bearer <ACCESS_TOKEN>
Content-Type: application/json
```

**Scopes required:**
- `user-read-private` (recommended)
- `user-read-email` (optional)
- `user-read-playback-state` (player info)
- `user-modify-playback-state` (play/pause/skip)

---

# 1. Search Tracks (Main Search Bar Endpoint)

## Endpoint
```
GET /v1/search
```

## Purpose
Search Spotify for tracks using user input from frontend.
Returns full metadata:
- track URI (for playback)
- title
- artists
- album
- album images
- duration
- preview URL

## Query Parameters
| Name | Required | Description |
|------|----------|-------------|
| `q`  | Yes | Search query: song name, artist, album |
| `type` | Yes | Must be `track` |
| `limit` | No | Default 20 (max 50) |
| `offset` | No | For pagination |
| `market` | No | Region code (e.g. TR, US) |

---

## Example Request
```
GET https://api.spotify.com/v1/search?q=rammstein+du+hast&type=track&limit=10&market=TR
Authorization: Bearer <ACCESS_TOKEN>
```

---

## Example Response 
```json
{
  "tracks": {
    "href": "https://api.spotify.com/v1/me/shows?offset=0&limit=20",
    "limit": 20,
    "next": "https://api.spotify.com/v1/me/shows?offset=1&limit=1",
    "offset": 0,
    "previous": "https://api.spotify.com/v1/me/shows?offset=1&limit=1",
    "total": 4,
    "items": [
      {
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
      }
    ]
  },
  "artists": {
    "href": "https://api.spotify.com/v1/me/shows?offset=0&limit=20",
    "limit": 20,
    "next": "https://api.spotify.com/v1/me/shows?offset=1&limit=1",
    "offset": 0,
    "previous": "https://api.spotify.com/v1/me/shows?offset=1&limit=1",
    "total": 4,
    "items": [
      {
        "external_urls": {
          "spotify": "string"
        },
        "followers": {
          "href": "string",
          "total": 0
        },
        "genres": [
          "Prog rock",
          "Grunge"
        ],
        "href": "string",
        "id": "string",
        "images": [
          {
            "url": "https://i.scdn.co/image/ab67616d00001e02ff9ca10b55ce82ae553c8228",
            "height": 300,
            "width": 300
          }
        ],
        "name": "string",
        "popularity": 0,
        "type": "artist",
        "uri": "string"
      }
    ]
  },
  "albums": {
    "href": "https://api.spotify.com/v1/me/shows?offset=0&limit=20",
    "limit": 20,
    "next": "https://api.spotify.com/v1/me/shows?offset=1&limit=1",
    "offset": 0,
    "previous": "https://api.spotify.com/v1/me/shows?offset=1&limit=1",
    "total": 4,
    "items": [
      {
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
      }
    ]
  },
  "playlists": {
    "href": "https://api.spotify.com/v1/me/shows?offset=0&limit=20",
    "limit": 20,
    "next": "https://api.spotify.com/v1/me/shows?offset=1&limit=1",
    "offset": 0,
    "previous": "https://api.spotify.com/v1/me/shows?offset=1&limit=1",
    "total": 4,
    "items": [
      {
        "collaborative": false,
        "description": "string",
        "external_urls": {
          "spotify": "string"
        },
        "href": "string",
        "id": "string",
        "images": [
          {
            "url": "https://i.scdn.co/image/ab67616d00001e02ff9ca10b55ce82ae553c8228",
            "height": 300,
            "width": 300
          }
        ],
        "name": "string",
        "owner": {
          "external_urls": {
            "spotify": "string"
          },
          "href": "string",
          "id": "string",
          "type": "user",
          "uri": "string",
          "display_name": "string"
        },
        "public": false,
        "snapshot_id": "string",
        "tracks": {
          "href": "string",
          "total": 0
        },
        "type": "string",
        "uri": "string"
      }
    ]
  },
  "shows": {
    "href": "https://api.spotify.com/v1/me/shows?offset=0&limit=20",
    "limit": 20,
    "next": "https://api.spotify.com/v1/me/shows?offset=1&limit=1",
    "offset": 0,
    "previous": "https://api.spotify.com/v1/me/shows?offset=1&limit=1",
    "total": 4,
    "items": [
      {
        "available_markets": [
          "string"
        ],
        "copyrights": [
          {
            "text": "string",
            "type": "string"
          }
        ],
        "description": "string",
        "html_description": "string",
        "explicit": false,
        "external_urls": {
          "spotify": "string"
        },
        "href": "string",
        "id": "string",
        "images": [
          {
            "url": "https://i.scdn.co/image/ab67616d00001e02ff9ca10b55ce82ae553c8228",
            "height": 300,
            "width": 300
          }
        ],
        "is_externally_hosted": false,
        "languages": [
          "string"
        ],
        "media_type": "string",
        "name": "string",
        "publisher": "string",
        "type": "show",
        "uri": "string",
        "total_episodes": 0
      }
    ]
  },
  "episodes": {
    "href": "https://api.spotify.com/v1/me/shows?offset=0&limit=20",
    "limit": 20,
    "next": "https://api.spotify.com/v1/me/shows?offset=1&limit=1",
    "offset": 0,
    "previous": "https://api.spotify.com/v1/me/shows?offset=1&limit=1",
    "total": 4,
    "items": [
      {
        "audio_preview_url": "https://p.scdn.co/mp3-preview/2f37da1d4221f40b9d1a98cd191f4d6f1646ad17",
        "description": "A Spotify podcast sharing fresh insights on important topics of the moment—in a way only Spotify can. You’ll hear from experts in the music, podcast and tech industries as we discover and uncover stories about our work and the world around us.",
        "html_description": "<p>A Spotify podcast sharing fresh insights on important topics of the moment—in a way only Spotify can. You’ll hear from experts in the music, podcast and tech industries as we discover and uncover stories about our work and the world around us.</p>",
        "duration_ms": 1686230,
        "explicit": false,
        "external_urls": {
          "spotify": "string"
        },
        "href": "https://api.spotify.com/v1/episodes/5Xt5DXGzch68nYYamXrNxZ",
        "id": "5Xt5DXGzch68nYYamXrNxZ",
        "images": [
          {
            "url": "https://i.scdn.co/image/ab67616d00001e02ff9ca10b55ce82ae553c8228",
            "height": 300,
            "width": 300
          }
        ],
        "is_externally_hosted": false,
        "is_playable": false,
        "language": "en",
        "languages": [
          "fr",
          "en"
        ],
        "name": "Starting Your Own Podcast: Tips, Tricks, and Advice From Anchor Creators",
        "release_date": "1981-12-15",
        "release_date_precision": "day",
        "resume_point": {
          "fully_played": false,
          "resume_position_ms": 0
        },
        "type": "episode",
        "uri": "spotify:episode:0zLhl3WsOCQHbe1BPTiHgr",
        "restrictions": {
          "reason": "string"
        }
      }
    ]
  },
  "audiobooks": {
    "href": "https://api.spotify.com/v1/me/shows?offset=0&limit=20",
    "limit": 20,
    "next": "https://api.spotify.com/v1/me/shows?offset=1&limit=1",
    "offset": 0,
    "previous": "https://api.spotify.com/v1/me/shows?offset=1&limit=1",
    "total": 4,
    "items": [
      {
        "authors": [
          {
            "name": "string"
          }
        ],
        "available_markets": [
          "string"
        ],
        "copyrights": [
          {
            "text": "string",
            "type": "string"
          }
        ],
        "description": "string",
        "html_description": "string",
        "edition": "Unabridged",
        "explicit": false,
        "external_urls": {
          "spotify": "string"
        },
        "href": "string",
        "id": "string",
        "images": [
          {
            "url": "https://i.scdn.co/image/ab67616d00001e02ff9ca10b55ce82ae553c8228",
            "height": 300,
            "width": 300
          }
        ],
        "languages": [
          "string"
        ],
        "media_type": "string",
        "name": "string",
        "narrators": [
          {
            "name": "string"
          }
        ],
        "publisher": "string",
        "type": "audiobook",
        "uri": "string",
        "total_chapters": 0
      }
    ]
  }
}
```

---

# 2. Important Fields for Playback

Each track from `/v1/search` includes the key fields necessary for playback:

### **Track URI (CRITICAL)**
```
spotify:track:<TRACK_ID>
```
Used in Player API:
```
PUT /v1/me/player/play
POST /v1/me/player/queue
```

### Additional Useful Fields
- `name` → Track title
- `artists[].name` → Artist names
- `album.name` → Album
- `album.images[]` → Cover images for UI
- `duration_ms` → Track duration
- `preview_url` → Optional 30-second audio preview

---

# 3. Get Single Track by ID

## Endpoint
```
GET /v1/tracks/{id}
```

## Purpose
Retrieve **full metadata** for a specific track.
Useful when you only store the track ID and want fresh data.

---

## Example Request
```
GET https://api.spotify.com/v1/tracks/4iV5W9uYEdYUVa79Axb7Rh
Authorization: Bearer <ACCESS_TOKEN>
```

**Response fields are identical to `/search` results.**

---

# 4. Get Multiple Tracks

## Endpoint
```
GET /v1/tracks
```

## Query Parameters
| Name | Description |
|------|-------------|
| `ids` | Comma-separated list of up to 50 track IDs |

---

## Example Request
```
GET https://api.spotify.com/v1/tracks?ids=4iV5...,6habF...
Authorization: Bearer <ACCESS_TOKEN>
```

---

# 5. Using Search Results to Play Tracks

Once you have:
```
uri = "spotify:track:4iV5W9uYEdYUVa79Axb7Rh"
```

You can play it in **any Spotify player** (Web Playback SDK or a device):

## Play Track
```
PUT /v1/me/player/play?device_id=<DEVICE_ID>
Content-Type: application/json

{
  "uris": ["spotify:track:4iV5W9uYEdYUVa79Axb7Rh"],
  "position_ms": 0
}
```

## Add to Queue
```
POST /v1/me/player/queue?uri=spotify:track:4iV5W9uYEdYUVa79Axb7Rh&device_id=<DEVICE_ID>
```

---

# 6. Summary of All Required Endpoints

| Purpose | Method | Endpoint |
|---------|--------|----------|
| Search tracks | GET | `/v1/search` |
| Get one track | GET | `/v1/tracks/{id}` |
| Get multiple tracks | GET | `/v1/tracks` |
| Play a track | PUT | `/v1/me/player/play` |
| Add to queue | POST | `/v1/me/player/queue` |

---

# 7. Typical Frontend Search-to-Play Flow
```
[User types into Search Bar]
        ↓
Frontend calls GET /v1/search?q=<text>&type=track
        ↓
User selects a track from results
        ↓
Frontend sends track.uri and device_id to Player API
        ↓
PUT /v1/me/player/play?device_id=<id>
        ↓
Track starts playing on chosen Spotify device
```

---

This document is structured for AI agent processing and automation-driven Spotify integration workflows.

