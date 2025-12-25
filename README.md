# Prism - Video Infrastructure Platform (B2B)

## 1. Project Overview
Prism is a developer-focused **Video Infrastructure as a Service** (like Mux or Cloudinary).
It allows other developers to integrate video streaming into their applications via simple APIs.

**Core Concept:**
1.  **Developer** signs up to the Prism Dashboard.
2.  **Developer** creates an "App" (Project).
3.  **Prism** generates an `API Key` (e.g., `pk_live_...`).
4.  **Developer** uses this key to upload, transcode, and stream videos programmatically.

### Core Tech Stack
* **Language:** Java 21
* **Framework:** Spring Boot 3.4
* **Architecture:** Event-Driven Microservices
* **Gateway:** Spring Cloud Gateway (Java DSL)
* **Database:** MongoDB (Database-per-service)
* **Storage:** MinIO (S3 Compatible)
* **Messaging:** RabbitMQ
* **Video Engine:** FFmpeg

---

## 2. System Architecture

The system distinguishes between **Dashboard Traffic** (Humans) and **API Traffic** (Machines).

### A. The Data Hierarchy
1.  **User (Developer):** The human managing the account.
2.  **App (Project):** A container for videos (e.g., "Fitness App Production").
3.  **API Key:** The credential used to authenticate API requests for that App.
4.  **Video:** Belongs to an `App` (not directly to a User).

### B. Request Flow
1.  **External Request:** `POST /uploads` with Header `x-api-key: pk_live_123`.
2.  **Gateway:** Intercepts request $\rightarrow$ Calls Auth Service to validate key.
3.  **Gateway:** If valid, injects `x-app-id: <app_id>` into headers and forwards to **Upload Service**.
4.  **Services:** Trust the `x-app-id` header to know which "App" owns the data.

---

## 3. Service Inventory

| Service | Port | Type | Responsibility |
| :--- | :--- | :--- | :--- |
| **prism-discovery** | `8761` | Infra | Eureka Server. |
| **prism-config** | `8888` | Infra | Config Server. |
| **prism-gateway** | `8080` | Infra | **Gatekeeper.** Validates API Keys & Routes traffic. |
| **prism-auth** | `8081` | Core | **Identity.** Manages Users, Apps, and API Key generation. |
| **prism-catalog** | `8082` | Core | **Metadata.** Stores video data linked to an `App ID`. |
| **prism-upload** | `8083` | Core | **Ingest.** Accepts file uploads for a specific `App ID`. |
| **prism-transcoder** | `8084` | Worker | **Processing.** Converts raw files to HLS. |
| **prism-stream** | `8085` | Core | **Delivery.** Streams video to end-users. |

---

## 4. Implementation Phases

### ✅ Phase 1: Infrastructure (Completed)
* Docker containers (Mongo, MinIO, RabbitMQ) are running.
* Discovery, Config, and Gateway services are online.
* Gateway configured with Java DSL Routes.

### ⏳ Phase 2: Authentication Service (`prism-auth`)
**Goal:** Identity & Access Management.
* **Entities:** `User` (Dev), `App` (Project), `ApiKey`.
* **Dashboard API:** Register/Login (returns JWT for human use).
* **App API:** Create App, Generate API Key, Revoke Key.
* **Internal API:** `POST /internal/validate-key` (Used by Gateway to check keys).

### ⏳ Phase 3: Catalog Service (`prism-catalog`)
**Goal:** Video Management.
* **Context:** All videos belong to an `appId`.
* **API:** `GET /videos` (List videos for this App), `POST /videos` (Create metadata).

### ⏳ Phase 4: Upload Service (`prism-upload`)
**Goal:** High-performance Ingest.
* **Context:** Receives `x-app-id` from Gateway.
* **Tech:** WebFlux + S3 Async.
* **Action:** Stream file to MinIO bucket `prism-raw/{appId}/{videoId}.mp4`.

### ⏳ Phase 5: Transcoder Service (`prism-transcoder`)
**Goal:** FFmpeg Processing.
* **Action:** Convert raw file $\rightarrow$ HLS playlist (`.m3u8`).
* **Output:** `prism-stream/{appId}/{videoId}/...`

### ⏳ Phase 6: Streaming Service (`prism-stream`)
**Goal:** Public Playback.
* **Action:** Proxy HLS segments from MinIO to viewers.

---

## 5. Configuration & Setup
* **Config Repo:** `prism/config-repo` contains all YAMLs.
* **Gateway Routing:** Defined in `PrismGatewayApplication.java`.
* **Local Dev:** Requires `ffmpeg` binary in `prism-transcoder/bin/`.