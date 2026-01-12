# Admin API Reference (To Be Implemented)

This document outlines the Admin API capabilities to be implemented.

## 1. Prism Auth Service (Public Admin API)

These endpoints are exposed to the Admin Dashboard (Frontend) via the Gateway.
**Base Path**: `/api/admin`
**Security**: Requires `ROLE_ADMIN`

### User Management

| Method | Path                  | Description                                               |
|:-------|:----------------------|:----------------------------------------------------------|
| `GET`  | `/users`              | List users with pagination & filtering (email, username). |
| `GET`  | `/users/{id}`         | Get detailed profile of a specific user.                  |
| `PUT`  | `/users/{id}/ban`     | Suspend a user account (prevents login).                  |
| `PUT`  | `/users/{id}/unban`   | Reactivate a suspended user account.                      |
| `PUT`  | `/users/{id}/promote` | Grant `ROLE_ADMIN` privileges.                            |
| `PUT`  | `/users/{id}/demote`  | Revoke `ROLE_ADMIN` privileges.                           |

### Video Management (Facade)

*Proxies requests to Prism Catalog.*

| Method | Path                   | Description                                                  |
|:-------|:-----------------------|:-------------------------------------------------------------|
| `GET`  | `/videos`              | List all videos (including private/unlisted) for moderation. |
| `PUT`  | `/videos/{id}/block`   | Block a video (hides it from public feeds).                  |
| `PUT`  | `/videos/{id}/unblock` | Unblock a video (restores visibility).                       |

---

## 2. Prism Catalog Service (Internal API)

These endpoints are **NOT** public. They are only callable by other microservices (like Prism Auth).
**Base Path**: `/api/catalog/internal`
**Security**: Network-level trust (or internal network only).

### Video Operations

| Method | Path                  | Description                                                |
|:-------|:----------------------|:-----------------------------------------------------------|
| `GET`  | `/videos`             | Internal search for videos (ignores `visibility` filters). |
| `PUT`  | `/videos/{id}/status` | Update video status (used to set `BLOCKED`/`READY`).       |

---

## 3. Data Models (Enum Updates)

### User Status (prism-auth)

- `ACTIVE`: Normal user.
- `SUSPENDED`: Banned user.
- `DELETED`: Soft-deleted user.

### Video Status (prism-catalog)

- `READY`: Normal video.
- `BLOCKED`: Moderated/Banned video (NEW STATUS).
- `PENDING/PROCESSING/FAILED`: System states.
