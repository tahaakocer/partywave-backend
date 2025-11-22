# Spotify OAuth2 Login Endpoint Documentation

This document provides a clean, structured, AI-agent-friendly reference for implementing **Spotify Login** using the **Authorization Code Flow**. It includes all required endpoints, parameters, request/response formats, and explanations.

---

# 1. Authorization Endpoint

**URL:**
```
GET https://accounts.spotify.com/authorize
```

**Purpose:**
Redirects the user to Spotify’s login and permissions screen.

**Required Query Parameters:**
- `client_id` — Your Spotify Client ID
- `response_type=code` — Always "code"
- `redirect_uri` — URL to return the user after login
- `scope` — Space-separated list of permissions
- `state` — Optional, recommended for CSRF protection

**Example Request:**
```
GET https://accounts.spotify.com/authorize?
  client_id=yourClientId
  &response_type=code
  &redirect_uri=https://yourapp.com/callback
  &scope=user-read-email%20user-read-private
  &state=xyz123
```

**Success Redirect:**
```
https://yourapp.com/callback?code=AUTH_CODE&state=xyz123
```

---

# 2. Token Exchange Endpoint

**URL:**
```
POST https://accounts.spotify.com/api/token
```

**Purpose:**
Exchanges the `code` for an Access Token + Refresh Token.

**Headers:**
```
Authorization: Basic base64(client_id:client_secret)
Content-Type: application/x-www-form-urlencoded
```

**Body:**
```
grant_type=authorization_code
code=AUTH_CODE
redirect_uri=https://yourapp.com/callback
```

**Example Response:**
```json
{
  "access_token": "BQDX...",
  "token_type": "Bearer",
  "expires_in": 3600,
  "refresh_token": "AQDX...",
  "scope": "user-read-email user-read-private"
}
```

---

# 3. Refresh Token Endpoint

**URL:**
```
POST https://accounts.spotify.com/api/token
```

**Purpose:**
Retrieve a new Access Token using a Refresh Token.

**Body:**
```
grant_type=refresh_token
refresh_token=REFRESH_TOKEN
```

**Example Response:**
```json
{
  "access_token": "new_access_token",
  "token_type": "Bearer",
  "expires_in": 3600
}
```

---

# 4. Current User Profile Endpoint

**URL:**
```
GET https://api.spotify.com/v1/me
```

**Purpose:**
Retrieve the authenticated user’s profile.

**Headers:**
```
Authorization: Bearer ACCESS_TOKEN
```

**Example Response:**
```json
{
  "display_name": "User Name",
  "email": "user@example.com",
  "id": "spotify_user_id",
  "images": [],
  "product": "premium"
}
```

---

# 5. Complete Login Flow Overview

```
[Client App]
   ↓ Redirect user
[Spotify Authorization]
   ↓ User logs in and approves
[Client Redirect URI]
   ↓ Sends ?code to backend
[Backend]
   ↓ Exchanges code → token
[Spotify API]
   ↓ Returns access/refresh token
[Backend]
   ↓ Fetches /v1/me user profile
[Database]
   ↓ Stores/updates user
[Client]
   → User is logged in
```

---

# Summary of Endpoints

| Purpose | URL | Method |
|---------|------|--------|
| Request user authorization | `https://accounts.spotify.com/authorize` | GET |
| Exchange code for tokens | `https://accounts.spotify.com/api/token` | POST |
| Refresh access token | `https://accounts.spotify.com/api/token` | POST |
| Get current user info | `https://api.spotify.com/v1/me` | GET |

---

This file is optimized for AI agents, automation, and backend integration workflows.

