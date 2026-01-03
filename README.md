# Prism

Developer-focused video infrastructure (Mux/Cloudinary-style) with APIs to upload, transcode to HLS, and stream via a gateway. Built as Spring Boot microservices with S3-compatible storage and FFmpeg processing.

## What Prism Does

- Issues API keys per “App” so developers can programmatically upload and stream video.
- Ingests uploads to MinIO, then transcodes to multi-variant HLS with FFmpeg.
- Serves HLS manifests/segments through a streaming proxy behind the API gateway.
- Separates dashboard (humans) from API traffic (machines) and injects `x-app-id` for tenancy.

## Core Workflow

1. Developer creates an App and gets an API key (e.g., `pk_live_*`).
2. Client uploads video with the API key.
3. Upload service stores raw media to MinIO; a job triggers FFmpeg transcode to HLS.
4. Transcoded HLS (master + variants) is proxied by the streaming service through the gateway.

## Services

- prism-discovery (8761): Eureka
- prism-config (8888): Config Server
- prism-gateway (8080): Validates API keys, injects `x-app-id`, routes
- prism-auth (8081): Users, Apps, API keys, key validation
- prism-catalog (8082): Video metadata per app
- prism-upload (8083): Ingest to MinIO
- prism-transcoder (8084): FFmpeg to HLS variants
- prism-stream (8085): HLS proxy for manifests/segments

## Tech Stack

- Java 21, Spring Boot 3.4, Spring Cloud Gateway (Java DSL)
- MongoDB (per service), RabbitMQ, MinIO (S3)
- FFmpeg for multi-variant HLS

## Request Model

- Developer (user) owns Apps.
- Each App has API keys.
- Videos belong to an App (identified by `x-app-id`).

## Typical Call Flow

`POST /uploads` with `x-api-key` → Gateway validates via Auth → injects `x-app-id` → Upload streams to MinIO → Transcoder produces HLS → Stream service proxies HLS through Gateway.

## Notes

- Config files live in `config-repo/`.
- Gateway routes are in `prism-gateway` (Java DSL).
- FFmpeg binary is required for transcoder in local dev.

## Run with Docker Compose

1. Prereqs: Docker + Docker Compose.
2. Copy env: `cp .env.example .env` (adjust if needed).
3. Build and start: `docker compose up -d --build`.
4. Check: `docker compose ps` and `docker compose logs -f prism-gateway`.

Key endpoints (localhost):

- Gateway: 8080
- MinIO: 9000 (console 9001)
- RabbitMQ: 15672
- MongoDB: 27017
- Zipkin: 9411
