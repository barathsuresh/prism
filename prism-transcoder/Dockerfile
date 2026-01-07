# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -DskipTests

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM ubuntu:22.04

WORKDIR /app

# Install Java 21 and FFmpeg from official Ubuntu repos
RUN apt-get update && \
    apt-get install -y \
    openjdk-21-jre-headless \
    ffmpeg \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Verify FFmpeg installation
RUN ffmpeg -version

# Copy JAR from builder stage
COPY --from=builder /app/target/prism-transcoder-*.jar app.jar

# Expose port
EXPOSE 8084

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8084/actuator/health || exit 1

# Run the application with increased memory for transcoding
ENTRYPOINT ["java", "-Xms1024m", "-Xmx3072m", "-XX:+UseG1GC", "-XX:MaxGCPauseMillis=200", "-jar", "app.jar"]
