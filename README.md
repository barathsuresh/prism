# Prism – Distributed Video Streaming Platform

Prism is a cloud-native, developer-focused video infrastructure platform (similar to Mux or Cloudinary) that enables programmatic video upload, transcoding, and secure streaming.

Built with **Spring Boot 3.4 Microservices**, it features a **Smart Proxy Architecture** for zero-trust storage security and a complete **Observability Stack** (Zipkin, Loki, Grafana) for production-grade monitoring.

---

## 🚀 Key Features

### 🛡️ Smart Proxy Streaming (Zero-Trust)

Unlike simple S3 redirects, Prism uses a **Reactive Stream Service (Spring WebFlux)** to proxy video segments.

- **Private Storage:** MinIO bucket is locked (Private). Only the internal Stream Service has access.
- **Granular Control:** Validates API keys on _every single video segment_ request.
- **Analytics Ready:** Enables precise bandwidth tracking per tenant.

### ⚡ Event-Driven Transcoding

- Decouples high-latency video processing from user uploads using **RabbitMQ**.
- Automatically triggers **FFmpeg** jobs to generate multi-variant HLS playlists (360p, 720p, 1080p).

### 🔍 Full Observability Stack

- **Distributed Tracing (Zipkin):** Visualizes the full lifecycle of a request across microservices (Gateway → Auth → Stream → Storage).
- **Centralized Logging (Loki & Grafana):** Aggregates logs from all 7 services into a single queryable dashboard.

---

## 🏗️ Architecture

### Core Workflow

1.  **Ingest:** Client uploads video → `prism-upload` streams raw bytes to MinIO → Publishes `VideoUploadedEvent`.
2.  **Process:** `prism-transcoder` consumes event → Downloads raw video → FFmpeg generates HLS (`.m3u8` + `.ts`) → Uploads back to MinIO.
3.  **Stream:** Player requests Master Manifest → `prism-gateway` routes to `prism-stream` → Service rewrites manifest URLs to point to itself (Proxy) → Streams content securely.

### Service Mesh

| Service              | Port   | Description                                                          |
| :------------------- | :----- | :------------------------------------------------------------------- |
| **prism-gateway**    | `8080` | Entry point. Handles routing and API Key validation.                 |
| **prism-auth**       | `8081` | Identity Provider. Manages Users, Apps, and API Keys.                |
| **prism-catalog**    | `8082` | Metadata service. Stores video details in MongoDB.                   |
| **prism-upload**     | `8083` | Handles binary uploads to MinIO (Ingest).                            |
| **prism-transcoder** | `8084` | Background worker. Runs FFmpeg jobs triggered by RabbitMQ.           |
| **prism-stream**     | `8085` | **Smart Proxy**. Streams HLS content via WebFlux (Non-blocking I/O). |
| **prism-discovery**  | `8761` | Netflix Eureka (Service Discovery).                                  |
| **prism-config**     | `8888` | Centralized Configuration Server.                                    |

---

## 🛠️ Tech Stack

- **Core:** Java 21, Spring Boot 3.4, Spring Cloud (Gateway, Eureka, Config Server).
- **Reactive:** Spring WebFlux (Reactive APIs, WebClient for inter-service communication).
- **Data:** MongoDB (Per-service databases).
- **Messaging:** RabbitMQ (Event-driven architecture).
- **Storage:** MinIO (S3-compatible Object Storage).
- **Media Processing:** FFmpeg.
- **Observability:**
  - **Zipkin:** Distributed Tracing.
  - **Loki:** Log Aggregation.
  - **Grafana:** Metrics & Log Visualization.
  - **Micrometer:** Metrics collection.

---

## 🐳 Running with Docker

The entire platform (Microservices + Databases + Observability) is containerized.

### Prerequisites

- Docker & Docker Compose
- Java 21 (for local development only)

### Quick Start

1.  **Clone the repository:**

    ```bash
    git clone [https://github.com/barathsuresh/prism.git](https://github.com/barathsuresh/prism.git)
    cd prism
    ```

2.  **Build & Start:**

    ```bash
    docker-compose up -d --build
    ```

3.  **Access Infrastructure:**
    - **API Gateway:** `http://localhost:8080`
    - **Eureka Dashboard:** `http://localhost:8761`
    - **Zipkin (Tracing):** `http://localhost:9411`
    - **Grafana (Logs/Metrics):** `http://localhost:3000` (User: `admin` / Pass: `admin`)
    - **MinIO Console:** `http://localhost:9001`

---

## 📡 API Usage Example

### 1. Upload a Video

```bash
curl -X POST http://localhost:8080/api/uploads \
  -H "X-API-KEY: pk_live_12345" \
  -F "file=@my_video.mp4"

```

### 2. Stream (HLS)

The response from upload provides a `videoId`. Use it to stream:

```bash
# Get Master Manifest
GET http://localhost:8080/api/stream/{videoId}/master.m3u8
# Header: X-API-KEY: pk_live_12345

```

_Note: The Stream Service will act as a proxy, fetching secure content from MinIO and serving it to the player._

---
