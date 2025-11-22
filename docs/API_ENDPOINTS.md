# PartyWave – API Endpoints Documentation

Bu dokümantasyon, PartyWave backend API'sinin tüm endpoint'lerini detaylı bir şekilde açıklar. Her endpoint için request/response formatları, authentication gereksinimleri, hata kodları ve örnekler içerir.

**Base URL**: `https://api.partywave.com` (veya geliştirme ortamı için `http://localhost:8080`)

**API Version**: `v1`

---

## İçindekiler

1. [Authentication Endpoints](#1-authentication-endpoints)
2. [Room Management Endpoints](#2-room-management-endpoints)
3. [Playlist Endpoints](#3-playlist-endpoints)
4. [Playback Endpoints](#4-playback-endpoints)
5. [Chat Endpoints](#5-chat-endpoints)
6. [Vote Endpoints](#6-vote-endpoints)
7. [User Endpoints](#7-user-endpoints)
8. [Spotify Proxy Endpoints](#8-spotify-proxy-endpoints)
9. [WebSocket Events](#9-websocket-events)

---

## 1. Authentication Endpoints

### 1.1 Spotify OAuth Login

Spotify OAuth2 akışını başlatır ve kullanıcıyı Spotify yetkilendirme sayfasına yönlendirir.

**Endpoint**: `GET /auth/spotify/login`

**Authentication**: Gerekli değil (public endpoint)

**Query Parameters**:
- `redirect_uri` (optional): OAuth callback URL'i (default: backend callback URL'i)

**Response**: 
- HTTP 302 Redirect to Spotify authorization URL

**Example Request**:
```http
GET /auth/spotify/login?redirect_uri=https://app.partywave.com/callback
```

**Example Redirect**:
```
Location: https://accounts.spotify.com/authorize?
  client_id=YOUR_CLIENT_ID
  &response_type=code
  &redirect_uri=https://api.partywave.com/auth/spotify/callback
  &scope=user-read-email%20user-read-private%20user-modify-playback-state%20user-read-playback-state
  &state=CSRF_TOKEN
```

**Error Responses**:
- `500 Internal Server Error`: OAuth configuration hatası

---

### 1.2 Spotify OAuth Callback

Spotify'dan dönen authorization code'u işler, kullanıcıyı oluşturur/günceller ve JWT token'ları döner.

**Endpoint**: `GET /auth/spotify/callback`

**Authentication**: Gerekli değil (public endpoint)

**Query Parameters**:
- `code` (required): Spotify'dan dönen authorization code
- `state` (required): CSRF koruması için state token

**Response**: 
- HTTP 302 Redirect to frontend with tokens (veya JSON response)

**Example Request**:
```http
GET /auth/spotify/callback?code=AUTH_CODE&state=CSRF_TOKEN
```

**Example Response (Redirect)**:
```
Location: https://app.partywave.com/auth/callback#access_token=JWT_TOKEN&refresh_token=REFRESH_TOKEN
```

**Example Response (JSON - Alternative)**:
```json
{
  "user": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "spotify_user_id": "spotify_user_123",
    "display_name": "User Name",
    "email": "user@example.com",
    "country": "TR",
    "images": [
      {
        "url": "https://i.scdn.co/image/...",
        "height": 300,
        "width": 300
      }
    ],
    "status": "ONLINE"
  },
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expires_in": 900
}
```

**Error Responses**:
- `400 Bad Request`: Geçersiz veya eksik parametreler
  ```json
  {
    "error": "INVALID_REQUEST",
    "message": "Missing or invalid authorization code"
  }
  ```
- `401 Unauthorized`: State token doğrulama hatası
  ```json
  {
    "error": "CSRF_ERROR",
    "message": "Invalid state parameter"
  }
  ```
- `500 Internal Server Error`: Token exchange veya kullanıcı oluşturma hatası

---

### 1.3 Refresh Access Token

JWT access token'ı yeniler.

**Endpoint**: `POST /auth/refresh`

**Authentication**: Refresh token gereklidir (Cookie veya Body'de)

**Request Body** (optional, eğer cookie'de yoksa):
```json
{
  "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response**:
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expires_in": 900
}
```

**Error Responses**:
- `401 Unauthorized`: Geçersiz veya süresi dolmuş refresh token
  ```json
  {
    "error": "UNAUTHORIZED",
    "message": "Invalid or expired refresh token"
  }
  ```

---

### 1.4 Logout

Kullanıcıyı çıkış yapar ve token'ları geçersiz kılar.

**Endpoint**: `POST /auth/logout`

**Authentication**: JWT Access Token gereklidir

**Headers**:
```
Authorization: Bearer <JWT_ACCESS_TOKEN>
```

**Response**:
```json
{
  "message": "Successfully logged out"
}
```

**Error Responses**:
- `401 Unauthorized`: Geçersiz token

---

## 2. Room Management Endpoints

### 2.1 Create Room

Yeni bir oda oluşturur. Oluşturan kullanıcı otomatik olarak OWNER rolüne sahip olur.

**Endpoint**: `POST /api/rooms`

**Authentication**: JWT Access Token gereklidir

**Request Body**:
```json
{
  "name": "Chill Vibes",
  "description": "Relaxing music for studying",
  "tags": ["lofi", "chill", "study"],
  "max_participants": 50,
  "is_public": true
}
```

**Request Fields**:
- `name` (required, string): Oda adı
- `description` (optional, string): Oda açıklaması
- `tags` (optional, array of strings): Oda etiketleri (örn: ["lofi", "90s", "turkish-rap"])
- `max_participants` (required, integer): Maksimum katılımcı sayısı (min: 1)
- `is_public` (required, boolean): Oda herkese açık mı?

**Response**:
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "Chill Vibes",
  "description": "Relaxing music for studying",
  "tags": [
    {
      "id": "tag-uuid-1",
      "name": "lofi"
    },
    {
      "id": "tag-uuid-2",
      "name": "chill"
    }
  ],
  "max_participants": 50,
  "is_public": true,
  "created_at": "2024-01-15T10:30:00Z",
  "owner": {
    "id": "user-uuid",
    "display_name": "User Name",
    "images": [...]
  },
  "member_count": 1,
  "online_member_count": 1
}
```

**Error Responses**:
- `400 Bad Request`: Geçersiz input
  ```json
  {
    "error": "VALIDATION_ERROR",
    "message": "Room name cannot be empty"
  }
  ```
- `401 Unauthorized`: Geçersiz token

---

### 2.2 List Public Rooms

Herkese açık odaları listeler. Filtreleme ve arama desteği vardır.

**Endpoint**: `GET /api/rooms`

**Authentication**: JWT Access Token gereklidir

**Query Parameters**:
- `tags` (optional, comma-separated): Etiketlere göre filtreleme (örn: `?tags=lofi,chill`)
- `search` (optional, string): İsim/açıklama araması (örn: `?search=chill`)
- `page` (optional, integer): Sayfa numarası (default: 1)
- `size` (optional, integer): Sayfa başına kayıt sayısı (default: 20, max: 100)

**Response**:
```json
{
  "rooms": [
    {
      "id": "room-uuid-1",
      "name": "Chill Vibes",
      "description": "Relaxing music",
      "tags": [...],
      "max_participants": 50,
      "is_public": true,
      "member_count": 15,
      "online_member_count": 8,
      "created_at": "2024-01-15T10:30:00Z"
    },
    ...
  ],
  "pagination": {
    "page": 1,
    "size": 20,
    "total": 45,
    "total_pages": 3
  }
}
```

**Error Responses**:
- `401 Unauthorized`: Geçersiz token

---

### 2.3 Get Room Details

Belirli bir odanın detaylarını getirir.

**Endpoint**: `GET /api/rooms/{roomId}`

**Authentication**: JWT Access Token gereklidir

**Path Parameters**:
- `roomId` (required, UUID): Oda ID'si

**Response**:
```json
{
  "id": "room-uuid",
  "name": "Chill Vibes",
  "description": "Relaxing music for studying",
  "tags": [...],
  "max_participants": 50,
  "is_public": true,
  "created_at": "2024-01-15T10:30:00Z",
  "owner": {
    "id": "user-uuid",
    "display_name": "User Name",
    "images": [...]
  },
  "member_count": 15,
  "online_member_count": 8,
  "current_user_role": "PARTICIPANT",
  "current_user_joined_at": "2024-01-15T11:00:00Z"
}
```

**Error Responses**:
- `401 Unauthorized`: Geçersiz token
- `403 Forbidden`: Private oda ve kullanıcının erişim izni yok
- `404 Not Found`: Oda bulunamadı
  ```json
  {
    "error": "NOT_FOUND",
    "message": "Room not found"
  }
  ```

---

### 2.4 Join Room

Kullanıcıyı bir odaya ekler. Public odalar için otomatik, private odalar için erişim kontrolü yapılır.

**Endpoint**: `POST /api/rooms/{roomId}/join`

**Authentication**: JWT Access Token gereklidir

**Path Parameters**:
- `roomId` (required, UUID): Oda ID'si

**Request Body** (optional, private rooms için):
```json
{
  "invitation_token": "invitation-token-string"
}
```

**Response**:
```json
{
  "message": "Successfully joined room",
  "room": {
    "id": "room-uuid",
    "name": "Chill Vibes",
    ...
  },
  "playlist": [
    {
      "id": "playlist-item-uuid",
      "sequence_number": 1,
      "status": "PLAYING",
      "name": "Track Name",
      "artist": "Artist Name",
      "album": "Album Name",
      "duration_ms": 240000,
      "source_uri": "spotify:track:...",
      "added_by": {
        "id": "user-uuid",
        "display_name": "User Name"
      },
      "added_at_ms": 1705312200000,
      "like_count": 5,
      "dislike_count": 1
    },
    ...
  ],
  "playback_state": {
    "current_playlist_item_id": "playlist-item-uuid",
    "started_at_ms": 1705312200000,
    "track_duration_ms": 240000
  },
  "chat_history": [
    {
      "id": "message-uuid",
      "sender": {
        "id": "user-uuid",
        "display_name": "User Name"
      },
      "content": "Hello!",
      "sent_at": "2024-01-15T11:00:00Z"
    },
    ...
  ]
}
```

**Error Responses**:
- `400 Bad Request`: Oda dolu veya kullanıcı zaten üye
  ```json
  {
    "error": "ROOM_FULL",
    "message": "Room has reached maximum capacity"
  }
  ```
- `401 Unauthorized`: Geçersiz token
- `403 Forbidden`: Private oda ve erişim izni yok
- `404 Not Found`: Oda bulunamadı
- `400 Bad Request`: Geçersiz invitation token
  ```json
  {
    "error": "INVALID_INVITATION",
    "message": "Invitation token is invalid, expired, or has reached max uses"
  }
  ```

---

### 2.5 Leave Room

Kullanıcıyı odadan çıkarır.

**Endpoint**: `POST /api/rooms/{roomId}/leave`

**Authentication**: JWT Access Token gereklidir

**Path Parameters**:
- `roomId` (required, UUID): Oda ID'si

**Response**:
```json
{
  "message": "Successfully left room"
}
```

**Error Responses**:
- `401 Unauthorized`: Geçersiz token
- `404 Not Found`: Oda bulunamadı veya kullanıcı üye değil

---

### 2.6 Delete Room

Odayı siler (sadece OWNER).

**Endpoint**: `DELETE /api/rooms/{roomId}`

**Authentication**: JWT Access Token gereklidir

**Path Parameters**:
- `roomId` (required, UUID): Oda ID'si

**Response**:
```json
{
  "message": "Room deleted successfully"
}
```

**Error Responses**:
- `401 Unauthorized`: Geçersiz token
- `403 Forbidden`: Kullanıcı OWNER değil
- `404 Not Found`: Oda bulunamadı

---

### 2.7 Get Room Members

Odanın üyelerini listeler.

**Endpoint**: `GET /api/rooms/{roomId}/members`

**Authentication**: JWT Access Token gereklidir

**Path Parameters**:
- `roomId` (required, UUID): Oda ID'si

**Query Parameters**:
- `online_only` (optional, boolean): Sadece online üyeleri getir (default: false)

**Response**:
```json
{
  "members": [
    {
      "id": "user-uuid",
      "display_name": "User Name",
      "images": [...],
      "role": "OWNER",
      "joined_at": "2024-01-15T10:30:00Z",
      "last_active_at": "2024-01-15T11:30:00Z",
      "is_online": true
    },
    ...
  ],
  "total_count": 15,
  "online_count": 8
}
```

**Error Responses**:
- `401 Unauthorized`: Geçersiz token
- `403 Forbidden`: Private oda ve erişim izni yok
- `404 Not Found`: Oda bulunamadı

---

### 2.8 Grant Access to Private Room

Private odaya erişim izni verir (OWNER veya MODERATOR).

**Endpoint**: `POST /api/rooms/{roomId}/access`

**Authentication**: JWT Access Token gereklidir

**Path Parameters**:
- `roomId` (required, UUID): Oda ID'si

**Request Body**:
```json
{
  "target_user_id": "user-uuid-to-grant-access"
}
```

**Response**:
```json
{
  "message": "Access granted successfully",
  "access": {
    "id": "access-uuid",
    "room_id": "room-uuid",
    "app_user_id": "user-uuid",
    "granted_by_id": "grantor-uuid",
    "granted_at": "2024-01-15T11:00:00Z"
  }
}
```

**Error Responses**:
- `400 Bad Request`: Kullanıcı zaten erişime sahip veya zaten üye
- `401 Unauthorized`: Geçersiz token
- `403 Forbidden`: Yetki yok (OWNER/MODERATOR değil) veya oda public
- `404 Not Found`: Oda veya kullanıcı bulunamadı

---

### 2.9 Create Invitation Token

Private oda için davet token'ı oluşturur (OWNER veya MODERATOR).

**Endpoint**: `POST /api/rooms/{roomId}/invitations`

**Authentication**: JWT Access Token gereklidir

**Path Parameters**:
- `roomId` (required, UUID): Oda ID'si

**Request Body**:
```json
{
  "expires_at": "2024-02-15T11:00:00Z",
  "max_uses": 10
}
```

**Request Fields**:
- `expires_at` (optional, ISO 8601 datetime): Token son kullanma tarihi (null = süresiz)
- `max_uses` (optional, integer): Maksimum kullanım sayısı (null = sınırsız)

**Response**:
```json
{
  "invitation": {
    "id": "invitation-uuid",
    "room_id": "room-uuid",
    "token": "unique-invitation-token-string",
    "created_by_id": "user-uuid",
    "created_at": "2024-01-15T11:00:00Z",
    "expires_at": "2024-02-15T11:00:00Z",
    "max_uses": 10,
    "used_count": 0,
    "is_active": true
  },
  "invitation_url": "https://app.partywave.com/join?invite=unique-invitation-token-string"
}
```

**Error Responses**:
- `401 Unauthorized`: Geçersiz token
- `403 Forbidden`: Yetki yok (OWNER/MODERATOR değil) veya oda public
- `404 Not Found`: Oda bulunamadı

---

### 2.10 Revoke Invitation

Davet token'ını iptal eder (OWNER veya MODERATOR).

**Endpoint**: `DELETE /api/rooms/{roomId}/invitations/{invitationId}`

**Authentication**: JWT Access Token gereklidir

**Path Parameters**:
- `roomId` (required, UUID): Oda ID'si
- `invitationId` (required, UUID): Davet ID'si

**Response**:
```json
{
  "message": "Invitation revoked successfully"
}
```

**Error Responses**:
- `401 Unauthorized`: Geçersiz token
- `403 Forbidden`: Yetki yok
- `404 Not Found`: Oda veya davet bulunamadı

---

## 3. Playlist Endpoints

### 3.1 Add Track to Playlist

Odaya yeni bir şarkı ekler. Şarkı her zaman listenin sonuna eklenir.

**Endpoint**: `POST /api/rooms/{roomId}/playlist/items`

**Authentication**: JWT Access Token gereklidir

**Path Parameters**:
- `roomId` (required, UUID): Oda ID'si

**Request Body**:
```json
{
  "source_id": "spotify-track-id",
  "source_uri": "spotify:track:4iV5W9uYEdYUVa79Axb7Rh",
  "name": "Track Name",
  "artist": "Artist Name",
  "album": "Album Name",
  "duration_ms": 240000
}
```

**Request Fields**:
- `source_id` (required, string): Spotify track ID
- `source_uri` (required, string): Spotify track URI (örn: `spotify:track:...`)
- `name` (required, string): Şarkı adı
- `artist` (required, string): Sanatçı adı
- `album` (optional, string): Albüm adı
- `duration_ms` (required, integer): Şarkı süresi (milisaniye)

**Response**:
```json
{
  "playlist_item": {
    "id": "playlist-item-uuid",
    "room_id": "room-uuid",
    "added_by_id": "user-uuid",
    "sequence_number": 5,
    "status": "QUEUED",
    "added_at_ms": 1705312200000,
    "source_id": "spotify-track-id",
    "source_uri": "spotify:track:4iV5W9uYEdYUVa79Axb7Rh",
    "name": "Track Name",
    "artist": "Artist Name",
    "album": "Album Name",
    "duration_ms": 240000,
    "like_count": 0,
    "dislike_count": 0
  },
  "added_by": {
    "id": "user-uuid",
    "display_name": "User Name",
    "images": [...]
  }
}
```

**Error Responses**:
- `400 Bad Request`: Geçersiz input
- `401 Unauthorized`: Geçersiz token
- `403 Forbidden`: Kullanıcı oda üyesi değil
- `404 Not Found`: Oda bulunamadı

**Notlar**:
- Şarkı her zaman listenin sonuna eklenir (FIFO queue)
- Yeni eklenen şarkılar `QUEUED` status'ü ile başlar
- Eğer playlist boşsa ve hiç şarkı çalmıyorsa, backend otomatik olarak ilk şarkıyı başlatır

---

### 3.2 Get Playlist

Odanın playlist'ini getirir. Tüm şarkılar (aktif ve geçmiş) dahildir.

**Endpoint**: `GET /api/rooms/{roomId}/playlist`

**Authentication**: JWT Access Token gereklidir

**Path Parameters**:
- `roomId` (required, UUID): Oda ID'si

**Query Parameters**:
- `status` (optional, string): Status'e göre filtrele (`QUEUED`, `PLAYING`, `PLAYED`, `SKIPPED`)
- `limit` (optional, integer): Maksimum kayıt sayısı (default: tümü)

**Response**:
```json
{
  "playlist": [
    {
      "id": "playlist-item-uuid-1",
      "sequence_number": 1,
      "status": "PLAYED",
      "name": "Track Name 1",
      "artist": "Artist Name",
      "album": "Album Name",
      "duration_ms": 240000,
      "source_uri": "spotify:track:...",
      "added_by": {
        "id": "user-uuid",
        "display_name": "User Name",
        "images": [...]
      },
      "added_at_ms": 1705312000000,
      "like_count": 5,
      "dislike_count": 1
    },
    {
      "id": "playlist-item-uuid-2",
      "sequence_number": 2,
      "status": "PLAYING",
      "name": "Track Name 2",
      "artist": "Artist Name 2",
      "album": "Album Name 2",
      "duration_ms": 200000,
      "source_uri": "spotify:track:...",
      "added_by": {
        "id": "user-uuid-2",
        "display_name": "User Name 2",
        "images": [...]
      },
      "added_at_ms": 1705312100000,
      "like_count": 3,
      "dislike_count": 0
    },
    {
      "id": "playlist-item-uuid-3",
      "sequence_number": 3,
      "status": "QUEUED",
      "name": "Track Name 3",
      "artist": "Artist Name 3",
      "album": "Album Name 3",
      "duration_ms": 180000,
      "source_uri": "spotify:track:...",
      "added_by": {
        "id": "user-uuid-3",
        "display_name": "User Name 3",
        "images": [...]
      },
      "added_at_ms": 1705312200000,
      "like_count": 0,
      "dislike_count": 0
    }
  ],
  "total_count": 3,
  "active_count": 2,
  "history_count": 1
}
```

**Error Responses**:
- `401 Unauthorized`: Geçersiz token
- `403 Forbidden`: Private oda ve erişim izni yok
- `404 Not Found`: Oda bulunamadı

**Notlar**:
- Playlist listesi tüm şarkıları içerir (aktif ve geçmiş)
- `status` parametresi ile filtreleme yapılabilir
- Aktif şarkılar: `status = QUEUED` veya `status = PLAYING`
- Geçmiş şarkılar: `status = PLAYED` veya `status = SKIPPED`

---

### 3.3 Like Track

Bir playlist item'ı beğenir.

**Endpoint**: `POST /api/rooms/{roomId}/playlist/items/{itemId}/like`

**Authentication**: JWT Access Token gereklidir

**Path Parameters**:
- `roomId` (required, UUID): Oda ID'si
- `itemId` (required, UUID): Playlist item ID'si

**Response**:
```json
{
  "message": "Track liked successfully",
  "playlist_item": {
    "id": "playlist-item-uuid",
    "like_count": 6,
    "dislike_count": 0,
    "user_rating": "like"
  }
}
```

**Error Responses**:
- `400 Bad Request`: Kullanıcı zaten beğenmiş
- `401 Unauthorized`: Geçersiz token
- `403 Forbidden`: Kullanıcı oda üyesi değil
- `404 Not Found`: Oda veya playlist item bulunamadı

**Notlar**:
- Eğer kullanıcı daha önce dislike yaptıysa, dislike kaldırılır ve like eklenir
- Beğeni sayısı şarkıyı ekleyen kullanıcının (`added_by_id`) istatistiklerine eklenir

---

### 3.4 Dislike Track

Bir playlist item'ı beğenmez.

**Endpoint**: `POST /api/rooms/{roomId}/playlist/items/{itemId}/dislike`

**Authentication**: JWT Access Token gereklidir

**Path Parameters**:
- `roomId` (required, UUID): Oda ID'si
- `itemId` (required, UUID): Playlist item ID'si

**Response**:
```json
{
  "message": "Track disliked successfully",
  "playlist_item": {
    "id": "playlist-item-uuid",
    "like_count": 5,
    "dislike_count": 1,
    "user_rating": "dislike"
  }
}
```

**Error Responses**:
- `400 Bad Request`: Kullanıcı zaten beğenmemiş
- `401 Unauthorized`: Geçersiz token
- `403 Forbidden`: Kullanıcı oda üyesi değil
- `404 Not Found`: Oda veya playlist item bulunamadı

**Notlar**:
- Eğer kullanıcı daha önce like yaptıysa, like kaldırılır ve dislike eklenir
- Beğenmeme sayısı şarkıyı ekleyen kullanıcının (`added_by_id`) istatistiklerine eklenir

---

### 3.5 Remove Like

Bir playlist item'ın beğenisini kaldırır.

**Endpoint**: `DELETE /api/rooms/{roomId}/playlist/items/{itemId}/like`

**Authentication**: JWT Access Token gereklidir

**Path Parameters**:
- `roomId` (required, UUID): Oda ID'si
- `itemId` (required, UUID): Playlist item ID'si

**Response**:
```json
{
  "message": "Like removed successfully",
  "playlist_item": {
    "id": "playlist-item-uuid",
    "like_count": 4,
    "dislike_count": 0,
    "user_rating": null
  }
}
```

**Error Responses**:
- `400 Bad Request`: Kullanıcı beğenmemiş
- `401 Unauthorized`: Geçersiz token
- `403 Forbidden`: Kullanıcı oda üyesi değil
- `404 Not Found`: Oda veya playlist item bulunamadı

---

### 3.6 Remove Dislike

Bir playlist item'ın beğenmemesini kaldırır.

**Endpoint**: `DELETE /api/rooms/{roomId}/playlist/items/{itemId}/dislike`

**Authentication**: JWT Access Token gereklidir

**Path Parameters**:
- `roomId` (required, UUID): Oda ID'si
- `itemId` (required, UUID): Playlist item ID'si

**Response**:
```json
{
  "message": "Dislike removed successfully",
  "playlist_item": {
    "id": "playlist-item-uuid",
    "like_count": 5,
    "dislike_count": 0,
    "user_rating": null
  }
}
```

**Error Responses**:
- `400 Bad Request`: Kullanıcı beğenmemiş
- `401 Unauthorized`: Geçersiz token
- `403 Forbidden`: Kullanıcı oda üyesi değil
- `404 Not Found`: Oda veya playlist item bulunamadı

---

## 4. Playback Endpoints

### 4.1 Get Playback State

Odanın mevcut playback durumunu getirir.

**Endpoint**: `GET /api/rooms/{roomId}/playback`

**Authentication**: JWT Access Token gereklidir

**Path Parameters**:
- `roomId` (required, UUID): Oda ID'si

**Response**:
```json
{
  "current_playlist_item_id": "playlist-item-uuid",
  "started_at_ms": 1705312200000,
  "track_duration_ms": 240000,
  "updated_at_ms": 1705312200000,
  "current_track": {
    "id": "playlist-item-uuid",
    "name": "Track Name",
    "artist": "Artist Name",
    "album": "Album Name",
    "duration_ms": 240000,
    "source_uri": "spotify:track:...",
    "status": "PLAYING"
  }
}
```

**Response (No playback)**:
```json
{
  "current_playlist_item_id": null,
  "started_at_ms": null,
  "track_duration_ms": null,
  "updated_at_ms": null,
  "current_track": null
}
```

**Error Responses**:
- `401 Unauthorized`: Geçersiz token
- `403 Forbidden`: Private oda ve erişim izni yok
- `404 Not Found`: Oda bulunamadı

---

### 4.2 Start Playback

Odaya eklenmiş bir şarkıyı başlatır. Sadece OWNER veya MODERATOR yapabilir.

**Endpoint**: `POST /api/rooms/{roomId}/playback/start`

**Authentication**: JWT Access Token gereklidir

**Path Parameters**:
- `roomId` (required, UUID): Oda ID'si

**Request Body** (optional):
```json
{
  "playlist_item_id": "playlist-item-uuid"
}
```

**Request Fields**:
- `playlist_item_id` (optional, UUID): Başlatılacak playlist item ID'si. Eğer belirtilmezse, ilk `QUEUED` şarkı başlatılır.

**Response**:
```json
{
  "message": "Playback started",
  "playback_state": {
    "current_playlist_item_id": "playlist-item-uuid",
    "started_at_ms": 1705312200000,
    "track_duration_ms": 240000,
    "updated_at_ms": 1705312200000
  },
  "track": {
    "id": "playlist-item-uuid",
    "name": "Track Name",
    "artist": "Artist Name",
    "album": "Album Name",
    "duration_ms": 240000,
    "source_uri": "spotify:track:...",
    "status": "PLAYING"
  }
}
```

**Error Responses**:
- `400 Bad Request`: Geçersiz playlist item veya status `QUEUED` değil
  ```json
  {
    "error": "INVALID_STATUS",
    "message": "Track must have QUEUED status to start playback"
  }
  ```
- `401 Unauthorized`: Geçersiz token
- `403 Forbidden`: Yetki yok (OWNER/MODERATOR değil) veya oda üyesi değil
- `404 Not Found`: Oda veya playlist item bulunamadı

**Notlar**:
- Sadece `QUEUED` status'ündeki şarkılar başlatılabilir
- `PLAYED` veya `SKIPPED` status'ündeki şarkılar başlatılamaz (final state)
- Şarkı başlatıldığında WebSocket üzerinden `TRACK_START` eventi gönderilir

---

### 4.3 Skip Track

Şu anda çalan şarkıyı atlar ve bir sonraki şarkıya geçer. Sadece OWNER veya MODERATOR yapabilir.

**Endpoint**: `POST /api/rooms/{roomId}/playback/skip`

**Authentication**: JWT Access Token gereklidir

**Path Parameters**:
- `roomId` (required, UUID): Oda ID'si

**Response**:
```json
{
  "message": "Track skipped",
  "skipped_track": {
    "id": "playlist-item-uuid",
    "name": "Track Name",
    "status": "SKIPPED"
  },
  "next_track": {
    "id": "next-playlist-item-uuid",
    "name": "Next Track Name",
    "status": "PLAYING"
  }
}
```

**Response (No next track)**:
```json
{
  "message": "Track skipped, no more tracks in queue",
  "skipped_track": {
    "id": "playlist-item-uuid",
    "name": "Track Name",
    "status": "SKIPPED"
  },
  "next_track": null
}
```

**Error Responses**:
- `400 Bad Request`: Şu anda çalan şarkı yok veya status `PLAYING` değil
  ```json
  {
    "error": "NO_CURRENT_TRACK",
    "message": "No track is currently playing"
  }
  ```
- `401 Unauthorized`: Geçersiz token
- `403 Forbidden`: Yetki yok (OWNER/MODERATOR değil) veya oda üyesi değil
- `404 Not Found`: Oda bulunamadı

**Notlar**:
- Sadece `PLAYING` status'ündeki şarkılar atlanabilir
- Atlanan şarkı `SKIPPED` status'üne geçer (final state)
- Eğer sırada başka şarkı varsa, otomatik olarak başlatılır
- Şarkı atlandığında WebSocket üzerinden `TRACK_SKIPPED` eventi gönderilir

---

## 5. Chat Endpoints

### 5.1 Send Chat Message

Odaya chat mesajı gönderir.

**Endpoint**: `POST /api/rooms/{roomId}/chat/messages`

**Authentication**: JWT Access Token gereklidir

**Path Parameters**:
- `roomId` (required, UUID): Oda ID'si

**Request Body**:
```json
{
  "content": "Hello everyone!"
}
```

**Request Fields**:
- `content` (required, string): Mesaj içeriği (max length: 1000 karakter)

**Response**:
```json
{
  "message": {
    "id": "message-uuid",
    "room_id": "room-uuid",
    "sender": {
      "id": "user-uuid",
      "display_name": "User Name",
      "images": [...]
    },
    "content": "Hello everyone!",
    "sent_at": "2024-01-15T11:30:00Z"
  }
}
```

**Error Responses**:
- `400 Bad Request`: Mesaj içeriği boş veya çok uzun
  ```json
  {
    "error": "VALIDATION_ERROR",
    "message": "Message content cannot be empty or exceed 1000 characters"
  }
  ```
- `401 Unauthorized`: Geçersiz token
- `403 Forbidden`: Kullanıcı oda üyesi değil
- `404 Not Found`: Oda bulunamadı

**Notlar**:
- Mesaj gönderildiğinde WebSocket üzerinden `CHAT_MESSAGE` eventi gönderilir
- Tüm chat mesajları PostgreSQL'de saklanır

---

### 5.2 Get Chat History

Odanın chat geçmişini getirir.

**Endpoint**: `GET /api/rooms/{roomId}/chat/messages`

**Authentication**: JWT Access Token gereklidir

**Path Parameters**:
- `roomId` (required, UUID): Oda ID'si

**Query Parameters**:
- `limit` (optional, integer): Maksimum mesaj sayısı (default: 100, max: 500)
- `before` (optional, ISO 8601 datetime): Bu tarihten önceki mesajları getir (pagination)

**Response**:
```json
{
  "messages": [
    {
      "id": "message-uuid-1",
      "sender": {
        "id": "user-uuid",
        "display_name": "User Name",
        "images": [...]
      },
      "content": "Hello everyone!",
      "sent_at": "2024-01-15T11:30:00Z"
    },
    {
      "id": "message-uuid-2",
      "sender": {
        "id": "user-uuid-2",
        "display_name": "User Name 2",
        "images": [...]
      },
      "content": "Great music!",
      "sent_at": "2024-01-15T11:29:00Z"
    }
  ],
  "total_count": 150,
  "has_more": true
}
```

**Error Responses**:
- `401 Unauthorized`: Geçersiz token
- `403 Forbidden`: Private oda ve erişim izni yok
- `404 Not Found`: Oda bulunamadı

---

## 6. Vote Endpoints

### 6.1 Vote to Skip Track

Şu anda çalan şarkıyı atlamak için oy verir.

**Endpoint**: `POST /api/rooms/{roomId}/votes/skip`

**Authentication**: JWT Access Token gereklidir

**Path Parameters**:
- `roomId` (required, UUID): Oda ID'si

**Request Body**:
```json
{
  "playlist_item_id": "playlist-item-uuid"
}
```

**Request Fields**:
- `playlist_item_id` (required, UUID): Atlanacak şarkının playlist item ID'si

**Response**:
```json
{
  "message": "Vote cast successfully",
  "vote": {
    "id": "vote-uuid",
    "room_id": "room-uuid",
    "voter_id": "user-uuid",
    "vote_type": "SKIPTRACK",
    "playlist_item_id": "playlist-item-uuid",
    "created_at": "2024-01-15T11:30:00Z"
  },
  "vote_count": 5,
  "online_member_count": 10,
  "threshold": 5,
  "threshold_reached": true,
  "action_taken": true
}
```

**Response (Threshold not reached)**:
```json
{
  "message": "Vote cast successfully",
  "vote": {
    "id": "vote-uuid",
    "room_id": "room-uuid",
    "voter_id": "user-uuid",
    "vote_type": "SKIPTRACK",
    "playlist_item_id": "playlist-item-uuid",
    "created_at": "2024-01-15T11:30:00Z"
  },
  "vote_count": 3,
  "online_member_count": 10,
  "threshold": 5,
  "threshold_reached": false,
  "action_taken": false
}
```

**Error Responses**:
- `400 Bad Request`: Kullanıcı zaten oy vermiş
  ```json
  {
    "error": "ALREADY_VOTED",
    "message": "You have already voted to skip this track"
  }
  ```
- `400 Bad Request`: Şarkı şu anda çalmıyor
  ```json
  {
    "error": "INVALID_TRACK",
    "message": "Track is not currently playing"
  }
  ```
- `401 Unauthorized`: Geçersiz token
- `403 Forbidden`: Kullanıcı oda üyesi değil
- `404 Not Found`: Oda veya playlist item bulunamadı

**Notlar**:
- Her kullanıcı bir şarkı için sadece bir kez oy verebilir
- Threshold genellikle online üye sayısının %50'si olarak ayarlanır
- Threshold'a ulaşıldığında şarkı otomatik olarak atlanır
- Oy verildiğinde WebSocket üzerinden `VOTE_CAST` eventi gönderilir
- Threshold'a ulaşıldığında `VOTE_THRESHOLD_REACHED` ve `TRACK_SKIPPED` eventleri gönderilir

---

### 6.2 Vote to Kick User

Bir kullanıcıyı odadan atmak için oy verir.

**Endpoint**: `POST /api/rooms/{roomId}/votes/kick`

**Authentication**: JWT Access Token gereklidir

**Path Parameters**:
- `roomId` (required, UUID): Oda ID'si

**Request Body**:
```json
{
  "target_user_id": "user-uuid-to-kick"
}
```

**Request Fields**:
- `target_user_id` (required, UUID): Atılacak kullanıcının ID'si

**Response**:
```json
{
  "message": "Vote cast successfully",
  "vote": {
    "id": "vote-uuid",
    "room_id": "room-uuid",
    "voter_id": "user-uuid",
    "vote_type": "KICKUSER",
    "target_user_id": "target-user-uuid",
    "created_at": "2024-01-15T11:30:00Z"
  },
  "vote_count": 6,
  "online_member_count": 10,
  "threshold": 5,
  "threshold_reached": true,
  "action_taken": true,
  "kicked_user": {
    "id": "target-user-uuid",
    "display_name": "Kicked User"
  }
}
```

**Error Responses**:
- `400 Bad Request`: Kullanıcı zaten oy vermiş
  ```json
  {
    "error": "ALREADY_VOTED",
    "message": "You have already voted to kick this user"
  }
  ```
- `400 Bad Request`: Kullanıcı kendini atamaz
  ```json
  {
    "error": "INVALID_TARGET",
    "message": "You cannot vote to kick yourself"
  }
  ```
- `400 Bad Request`: OWNER atılamaz
  ```json
  {
    "error": "INVALID_TARGET",
    "message": "Room owner cannot be kicked"
  }
  ```
- `401 Unauthorized`: Geçersiz token
- `403 Forbidden`: Kullanıcı oda üyesi değil
- `404 Not Found`: Oda veya hedef kullanıcı bulunamadı

**Notlar**:
- Her kullanıcı bir hedef kullanıcı için sadece bir kez oy verebilir
- Kullanıcı kendini atamaz
- OWNER atılamaz
- Threshold genellikle online üye sayısının %50'si olarak ayarlanır
- Threshold'a ulaşıldığında kullanıcı otomatik olarak odadan çıkarılır
- Oy verildiğinde WebSocket üzerinden `VOTE_CAST` eventi gönderilir
- Threshold'a ulaşıldığında `VOTE_THRESHOLD_REACHED` ve `USER_KICKED` eventleri gönderilir

---

## 7. User Endpoints

### 7.1 Get Current User

Giriş yapmış kullanıcının profil bilgilerini getirir.

**Endpoint**: `GET /api/users/me`

**Authentication**: JWT Access Token gereklidir

**Response**:
```json
{
  "id": "user-uuid",
  "spotify_user_id": "spotify_user_123",
  "display_name": "User Name",
  "email": "user@example.com",
  "country": "TR",
  "href": "https://api.spotify.com/v1/users/spotify_user_123",
  "url": "https://open.spotify.com/user/spotify_user_123",
  "type": "user",
  "status": "ONLINE",
  "last_active_at": "2024-01-15T11:30:00Z",
  "images": [
    {
      "id": "image-uuid",
      "url": "https://i.scdn.co/image/...",
      "height": 300,
      "width": 300
    }
  ],
  "stats": {
    "total_like": 150,
    "total_dislike": 10
  }
}
```

**Error Responses**:
- `401 Unauthorized`: Geçersiz token

---

### 7.2 Get User Profile

Belirli bir kullanıcının profil bilgilerini getirir.

**Endpoint**: `GET /api/users/{userId}`

**Authentication**: JWT Access Token gereklidir

**Path Parameters**:
- `userId` (required, UUID): Kullanıcı ID'si

**Response**:
```json
{
  "id": "user-uuid",
  "spotify_user_id": "spotify_user_123",
  "display_name": "User Name",
  "country": "TR",
  "href": "https://api.spotify.com/v1/users/spotify_user_123",
  "url": "https://open.spotify.com/user/spotify_user_123",
  "type": "user",
  "status": "ONLINE",
  "last_active_at": "2024-01-15T11:30:00Z",
  "images": [
    {
      "id": "image-uuid",
      "url": "https://i.scdn.co/image/...",
      "height": 300,
      "width": 300
    }
  ],
  "stats": {
    "total_like": 150,
    "total_dislike": 10
  }
}
```

**Notlar**:
- Email adresi sadece kendi profilinde görünür (`/api/users/me`)
- Diğer kullanıcıların profillerinde email gösterilmez

**Error Responses**:
- `401 Unauthorized`: Geçersiz token
- `404 Not Found`: Kullanıcı bulunamadı

---

### 7.3 Get User Statistics

Kullanıcının istatistiklerini getirir.

**Endpoint**: `GET /api/users/{userId}/stats`

**Authentication**: JWT Access Token gereklidir

**Path Parameters**:
- `userId` (required, UUID): Kullanıcı ID'si

**Response**:
```json
{
  "user_id": "user-uuid",
  "total_like": 150,
  "total_dislike": 10,
  "net_rating": 140
}
```

**Error Responses**:
- `401 Unauthorized`: Geçersiz token
- `404 Not Found`: Kullanıcı bulunamadı

---

## 8. Spotify Proxy Endpoints

Bu endpoint'ler Spotify API'sine proxy görevi görür. Backend, kullanıcının Spotify access token'ını kullanarak Spotify API'sine istek yapar ve sonuçları döner.

### 8.1 Search Tracks

Spotify'da şarkı araması yapar.

**Endpoint**: `GET /api/spotify/search`

**Authentication**: JWT Access Token gereklidir

**Query Parameters**:
- `q` (required, string): Arama sorgusu (örn: `artist:name+track:title`)
- `type` (required, string): Arama tipi (örn: `track`)
- `limit` (optional, integer): Sonuç sayısı (default: 20, max: 50)
- `offset` (optional, integer): Pagination offset (default: 0)
- `market` (optional, string): Market kodu (örn: `TR`, `US`)

**Response**:
```json
{
  "tracks": {
    "href": "https://api.spotify.com/v1/search?q=...",
    "limit": 20,
    "next": "https://api.spotify.com/v1/search?q=...&offset=20",
    "offset": 0,
    "previous": null,
    "total": 100,
    "items": [
      {
        "id": "spotify-track-id",
        "name": "Track Name",
        "artists": [
          {
            "id": "artist-id",
            "name": "Artist Name",
            "type": "artist",
            "uri": "spotify:artist:..."
          }
        ],
        "album": {
          "id": "album-id",
          "name": "Album Name",
          "images": [
            {
              "url": "https://i.scdn.co/image/...",
              "height": 300,
              "width": 300
            }
          ],
          "uri": "spotify:album:..."
        },
        "duration_ms": 240000,
        "uri": "spotify:track:4iV5W9uYEdYUVa79Axb7Rh",
        "preview_url": "https://p.scdn.co/mp3-preview/...",
        "is_playable": true
      }
    ]
  }
}
```

**Error Responses**:
- `400 Bad Request`: Geçersiz query parametreleri
- `401 Unauthorized`: Geçersiz token veya Spotify token'ı süresi dolmuş
- `429 Too Many Requests`: Spotify API rate limit aşıldı
- `500 Internal Server Error`: Spotify API hatası

**Notlar**:
- Backend, kullanıcının Spotify access token'ını kullanır
- Token süresi dolmuşsa, backend otomatik olarak refresh token ile yeniler
- Rate limiting uygulanabilir

---

### 8.2 Get Track Details

Belirli bir şarkının detaylarını getirir.

**Endpoint**: `GET /api/spotify/tracks/{trackId}`

**Authentication**: JWT Access Token gereklidir

**Path Parameters**:
- `trackId` (required, string): Spotify track ID

**Query Parameters**:
- `market` (optional, string): Market kodu

**Response**:
```json
{
  "id": "spotify-track-id",
  "name": "Track Name",
  "artists": [...],
  "album": {...},
  "duration_ms": 240000,
  "uri": "spotify:track:4iV5W9uYEdYUVa79Axb7Rh",
  "preview_url": "https://p.scdn.co/mp3-preview/...",
  "is_playable": true
}
```

**Error Responses**:
- `401 Unauthorized`: Geçersiz token
- `404 Not Found`: Şarkı bulunamadı
- `500 Internal Server Error`: Spotify API hatası

---

### 8.3 Save Track to Library

Şu anda çalan şarkıyı kullanıcının Spotify kütüphanesine ekler.

**Endpoint**: `POST /api/spotify/me/tracks`

**Authentication**: JWT Access Token gereklidir

**Request Body**:
```json
{
  "track_id": "spotify-track-id"
}
```

**Request Fields**:
- `track_id` (required, string): Spotify track ID

**Response**:
```json
{
  "message": "Track saved to library successfully"
}
```

**Error Responses**:
- `400 Bad Request`: Geçersiz track ID
- `401 Unauthorized`: Geçersiz token veya Spotify token'ı süresi dolmuş
- `403 Forbidden`: Spotify scope eksik (`user-library-modify`)
- `500 Internal Server Error`: Spotify API hatası

**Notlar**:
- Bu endpoint Spotify API'sinin `PUT /me/tracks` endpoint'ini çağırır
- Kullanıcının Spotify access token'ı `user-library-modify` scope'una sahip olmalıdır

---

### 8.4 Add Track to Playlist

Şu anda çalan şarkıyı kullanıcının bir Spotify playlist'ine ekler.

**Endpoint**: `POST /api/spotify/playlists/{playlistId}/tracks`

**Authentication**: JWT Access Token gereklidir

**Path Parameters**:
- `playlistId` (required, string): Spotify playlist ID

**Request Body**:
```json
{
  "track_uri": "spotify:track:4iV5W9uYEdYUVa79Axb7Rh",
  "position": 0
}
```

**Request Fields**:
- `track_uri` (required, string): Spotify track URI
- `position` (optional, integer): Playlist'teki pozisyon (default: sona ekler)

**Response**:
```json
{
  "message": "Track added to playlist successfully",
  "snapshot_id": "playlist-snapshot-id"
}
```

**Error Responses**:
- `400 Bad Request`: Geçersiz track URI veya playlist ID
- `401 Unauthorized`: Geçersiz token
- `403 Forbidden`: Kullanıcı playlist'i düzenleme yetkisine sahip değil
- `404 Not Found`: Playlist bulunamadı
- `500 Internal Server Error`: Spotify API hatası

**Notlar**:
- Bu endpoint Spotify API'sinin `POST /playlists/{playlist_id}/tracks` endpoint'ini çağırır
- Kullanıcının Spotify access token'ı `playlist-modify-public` veya `playlist-modify-private` scope'una sahip olmalıdır

---

## 9. WebSocket Events

PartyWave, gerçek zamanlı iletişim için WebSocket kullanır. Tüm WebSocket bağlantıları JWT token ile authenticate edilmelidir.

### 9.1 WebSocket Connection

**Endpoint**: `ws://api.partywave.com/ws` (veya `wss://` for HTTPS)

**Authentication**: 
- Query parameter: `?token=<JWT_ACCESS_TOKEN>`
- Veya ilk mesaj olarak authentication gönderilebilir

**Connection Flow**:
1. Client WebSocket bağlantısı açar
2. Backend JWT token'ı doğrular
3. Authentication başarılıysa, `AUTH_SUCCESS` eventi gönderilir
4. Client odaya subscribe olur

**Authentication Message** (alternative):
```json
{
  "type": "AUTHENTICATE",
  "token": "JWT_ACCESS_TOKEN"
}
```

**Authentication Success Response**:
```json
{
  "type": "AUTH_SUCCESS",
  "user_id": "user-uuid"
}
```

**Authentication Error Response**:
```json
{
  "type": "AUTH_ERROR",
  "error": "Invalid or expired token",
  "code": "WS_AUTH_FAILED"
}
```

---

### 9.2 Room Subscription

Odaya subscribe olmak için:

**Subscribe Message**:
```json
{
  "type": "SUBSCRIBE_ROOM",
  "room_id": "room-uuid"
}
```

**Subscribe Success Response**:
```json
{
  "type": "SUBSCRIBE_SUCCESS",
  "room_id": "room-uuid",
  "room_state": {
    "playlist": [...],
    "playback_state": {...},
    "chat_history": [...]
  }
}
```

**Subscribe Error Response**:
```json
{
  "type": "SUBSCRIBE_ERROR",
  "error": "Not a member of this room",
  "code": "WS_NOT_MEMBER"
}
```

**Unsubscribe Message**:
```json
{
  "type": "UNSUBSCRIBE_ROOM",
  "room_id": "room-uuid"
}
```

---

### 9.3 Playback Events

#### TRACK_START
Yeni bir şarkı başladığında gönderilir.

```json
{
  "type": "TRACK_START",
  "room_id": "room-uuid",
  "playlist_item_id": "playlist-item-uuid",
  "track": {
    "id": "playlist-item-uuid",
    "name": "Track Name",
    "artist": "Artist Name",
    "album": "Album Name",
    "duration_ms": 240000,
    "source_uri": "spotify:track:..."
  },
  "started_at_ms": 1705312200000,
  "track_duration_ms": 240000
}
```

#### TRACK_SKIPPED
Şarkı atlandığında gönderilir.

```json
{
  "type": "TRACK_SKIPPED",
  "room_id": "room-uuid",
  "playlist_item_id": "playlist-item-uuid",
  "track": {
    "id": "playlist-item-uuid",
    "name": "Track Name",
    "status": "SKIPPED"
  }
}
```

#### TRACK_FINISHED
Şarkı doğal olarak bittiğinde gönderilir (optional).

```json
{
  "type": "TRACK_FINISHED",
  "room_id": "room-uuid",
  "playlist_item_id": "playlist-item-uuid",
  "track": {
    "id": "playlist-item-uuid",
    "name": "Track Name",
    "status": "PLAYED"
  }
}
```

---

### 9.4 Playlist Events

#### PLAYLIST_ITEM_ADDED
Yeni bir şarkı playlist'e eklendiğinde gönderilir.

```json
{
  "type": "PLAYLIST_ITEM_ADDED",
  "room_id": "room-uuid",
  "playlist_item": {
    "id": "playlist-item-uuid",
    "sequence_number": 5,
    "status": "QUEUED",
    "name": "Track Name",
    "artist": "Artist Name",
    "album": "Album Name",
    "duration_ms": 240000,
    "source_uri": "spotify:track:...",
    "added_by": {
      "id": "user-uuid",
      "display_name": "User Name"
    },
    "added_at_ms": 1705312200000,
    "like_count": 0,
    "dislike_count": 0
  }
}
```

#### PLAYLIST_ITEM_STATS_UPDATED
Bir şarkının like/dislike sayıları güncellendiğinde gönderilir.

```json
{
  "type": "PLAYLIST_ITEM_STATS_UPDATED",
  "room_id": "room-uuid",
  "playlist_item_id": "playlist-item-uuid",
  "like_count": 6,
  "dislike_count": 1
}
```

---

### 9.5 Social Events

#### CHAT_MESSAGE
Yeni bir chat mesajı gönderildiğinde gönderilir.

```json
{
  "type": "CHAT_MESSAGE",
  "room_id": "room-uuid",
  "message": {
    "id": "message-uuid",
    "sender": {
      "id": "user-uuid",
      "display_name": "User Name",
      "images": [...]
    },
    "content": "Hello everyone!",
    "sent_at": "2024-01-15T11:30:00Z"
  }
}
```

#### USER_JOINED
Bir kullanıcı odaya katıldığında gönderilir.

```json
{
  "type": "USER_JOINED",
  "room_id": "room-uuid",
  "user": {
    "id": "user-uuid",
    "display_name": "User Name",
    "images": [...]
  },
  "joined_at": "2024-01-15T11:30:00Z"
}
```

#### USER_LEFT
Bir kullanıcı odadan ayrıldığında gönderilir.

```json
{
  "type": "USER_LEFT",
  "room_id": "room-uuid",
  "user": {
    "id": "user-uuid",
    "display_name": "User Name"
  },
  "left_at": "2024-01-15T11:35:00Z"
}
```

#### USER_KICKED
Bir kullanıcı odadan atıldığında gönderilir.

```json
{
  "type": "USER_KICKED",
  "room_id": "room-uuid",
  "user": {
    "id": "user-uuid",
    "display_name": "User Name"
  },
  "kicked_at": "2024-01-15T11:35:00Z"
}
```

---

### 9.6 Vote Events

#### VOTE_CAST
Yeni bir oy verildiğinde gönderilir.

```json
{
  "type": "VOTE_CAST",
  "room_id": "room-uuid",
  "vote": {
    "id": "vote-uuid",
    "voter": {
      "id": "user-uuid",
      "display_name": "User Name"
    },
    "vote_type": "SKIPTRACK",
    "playlist_item_id": "playlist-item-uuid",
    "created_at": "2024-01-15T11:30:00Z"
  },
  "vote_count": 5,
  "threshold": 5,
  "threshold_reached": true
}
```

#### VOTE_THRESHOLD_REACHED
Oy threshold'ına ulaşıldığında gönderilir.

```json
{
  "type": "VOTE_THRESHOLD_REACHED",
  "room_id": "room-uuid",
  "vote_type": "SKIPTRACK",
  "action_taken": true,
  "playlist_item_id": "playlist-item-uuid"
}
```

---

### 9.7 Error Events

#### TOKEN_EXPIRED
Token süresi dolduğunda gönderilir.

```json
{
  "type": "TOKEN_EXPIRED",
  "message": "Please refresh your token and reconnect"
}
```

#### MESSAGE_ERROR
Mesaj işlenirken hata oluştuğunda gönderilir.

```json
{
  "type": "MESSAGE_ERROR",
  "error": "Not authorized to perform this action",
  "code": "WS_UNAUTHORIZED"
}
```

---

## 10. Error Codes

Tüm endpoint'ler aşağıdaki standart hata formatını kullanır:

```json
{
  "error": "ERROR_CODE",
  "message": "Human-readable error message",
  "details": {} // Optional additional error details
}
```

### Common Error Codes

| Error Code | HTTP Status | Description |
|------------|-------------|-------------|
| `UNAUTHORIZED` | 401 | Geçersiz veya eksik authentication token |
| `FORBIDDEN` | 403 | Yetki yok (authenticated ama yetkisiz) |
| `NOT_FOUND` | 404 | Kaynak bulunamadı |
| `VALIDATION_ERROR` | 400 | Geçersiz input parametreleri |
| `ROOM_FULL` | 400 | Oda kapasitesi dolu |
| `ALREADY_VOTED` | 400 | Kullanıcı zaten oy vermiş |
| `INVALID_STATUS` | 400 | Geçersiz playlist item status |
| `INVALID_INVITATION` | 400 | Geçersiz invitation token |
| `CSRF_ERROR` | 401 | CSRF token doğrulama hatası |
| `INTERNAL_SERVER_ERROR` | 500 | Sunucu hatası |

---

## 11. Rate Limiting

API endpoint'leri rate limiting ile korunur:

- **Authentication endpoints**: 5 request/minute per IP
- **API endpoints**: 100 request/minute per user
- **WebSocket messages**: 60 messages/minute per connection

Rate limit aşıldığında:
```json
{
  "error": "RATE_LIMIT_EXCEEDED",
  "message": "Too many requests. Please try again later.",
  "retry_after": 60
}
```

---

## 12. Pagination

Liste döndüren endpoint'ler pagination kullanır:

**Query Parameters**:
- `page` (optional, integer): Sayfa numarası (default: 1)
- `size` (optional, integer): Sayfa başına kayıt sayısı (default: 20, max: 100)

**Response Format**:
```json
{
  "data": [...],
  "pagination": {
    "page": 1,
    "size": 20,
    "total": 100,
    "total_pages": 5,
    "has_next": true,
    "has_previous": false
  }
}
```

---

## 13. Timestamps

Tüm timestamp'ler ISO 8601 formatında UTC timezone'unda döner:

```
2024-01-15T11:30:00Z
```

Millisecond precision için:
```
1705312200000  // UTC epoch milliseconds
```

---

## 14. Authentication

Tüm protected endpoint'ler JWT access token gerektirir:

**Header**:
```
Authorization: Bearer <JWT_ACCESS_TOKEN>
```

**Token Format**:
- Access Token: 15 dakika geçerlilik
- Refresh Token: 7 gün geçerlilik

Token yenileme için `POST /auth/refresh` endpoint'ini kullanın.

---

## 15. WebSocket Authentication

WebSocket bağlantıları için:

**Connection URL**:
```
ws://api.partywave.com/ws?token=<JWT_ACCESS_TOKEN>
```

**Alternative**: İlk mesaj olarak authentication gönderilebilir:
```json
{
  "type": "AUTHENTICATE",
  "token": "<JWT_ACCESS_TOKEN>"
}
```

---

Bu dokümantasyon, PartyWave API'sinin tüm endpoint'lerini kapsar. Daha fazla bilgi için diğer dokümantasyon dosyalarına bakın:
- `PROJECT_OVERVIEW.md` - Genel proje açıklaması
- `AUTHENTICATION.md` - Authentication detayları
- `POSTGRES_SCHEMA.md` - Veritabanı şeması
- `REDIS_ARCHITECTURE.md` - Redis mimarisi
- `SPOTIFY_*.md` - Spotify API entegrasyonu

