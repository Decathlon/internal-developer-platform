---
title: Docker Deployment
description: Deploy IDP-Core with Docker and Docker Compose
---

Deploy IDP-Core using Docker for development, testing, or small-scale production environments.

## Prerequisites

- Docker 20.10+
- Docker Compose 2.0+ (optional)
- 1 GB available memory

---

## Quick Start with Docker Compose

### 1. Clone the Repository

```bash
git clone https://github.com/decathlon/idp-core.git
cd idp-core
```

### 2. Start Services

```bash
docker compose up -d
```

This starts:

- **PostgreSQL** on port 5432

### 3. Verify

```bash
# Check health
curl http://localhost:8080/actuator/health

# View logs
docker compose logs -f idp-core
```

---

## Docker Compose Configuration

### Default Configuration

In addition to the `docker-compose.yml` file provided, you can customize your setup. Here is a basic example:

```yaml title="docker-compose.yml"
version: "3.8"

services:
  idp-core:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=local
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/idp
      - SPRING_DATASOURCE_USERNAME=idp
      - SPRING_DATASOURCE_PASSWORD=idp
    depends_on:
      postgres:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3

  postgres:
    image: postgres:18-alpine
    environment:
      - POSTGRES_DB=idp
      - POSTGRES_USER=idp
      - POSTGRES_PASSWORD=idp
    volumes:
      - postgres_data:/var/lib/postgresql/data
    ports:
      - "5432:5432"
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U idp"]
      interval: 10s
      timeout: 5s
      retries: 5

volumes:
  postgres_data:
```

---

## Docker Run (Standalone)

### Basic Run

```bash
docker run -d \
  --name idp-core \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=dev \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/idp \
  -e SPRING_DATASOURCE_USERNAME=idp \
  -e SPRING_DATASOURCE_PASSWORD=idp \
  ghcr.io/decathlon/idp-core:latest
```

### With Volume Mounts

```bash
docker run -d \
  --name idp-core \
  -p 8080:8080 \
  -v $(pwd)/config:/app/config:ro \
  -v $(pwd)/logs:/app/logs \
  -e SPRING_CONFIG_ADDITIONAL_LOCATION=/app/config/ \
  ghcr.io/decathlon/idp-core:latest
```

### With External Network

```bash
# Create network
docker network create idp-network

# Run PostgreSQL
docker run -d \
  --name postgres \
  --network idp-network \
  -e POSTGRES_DB=idp \
  -e POSTGRES_USER=idp \
  -e POSTGRES_PASSWORD=idp \
  postgres:18-alpine

# Run IDP-Core
docker run -d \
  --name idp-core \
  --network idp-network \
  -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/idp \
  -e SPRING_DATASOURCE_USERNAME=idp \
  -e SPRING_DATASOURCE_PASSWORD=idp \
  ghcr.io/decathlon/idp-core:latest
```

---

## Building the Image

### From Docker File

To build the Docker image locally, first build the JAR file:

```bash
mvn clean package -DskipTests
```

Then build and run the Docker image:

```bash
# Build
docker build -t idp-core:local .

# Run
docker run -d -p 8080:8080 idp-core:local
```

### Multi-stage Build

In addition of the provided Dockerfile, here is an example of a multi-stage build:

```dockerfile title="Dockerfile"
# Build stage
FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN ./mvnw clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

# Create non-root user
RUN addgroup -g 1000 idp && \
    adduser -u 1000 -G idp -s /bin/sh -D idp

COPY --from=build /app/target/*.jar app.jar
RUN chown -R idp:idp /app

USER idp

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## Environment Variables

### Application Settings

| Variable                 | Description           | Default |
| ------------------------ | --------------------- | ------- |
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | `dev`   |
| `SERVER_PORT`            | HTTP port             | `8080`  |
| `JAVA_OPTS`              | JVM options           | -       |

### Database

| Variable                     | Description | Required |
| ---------------------------- | ----------- | -------- |
| `SPRING_DATASOURCE_URL`      | JDBC URL    | Yes      |
| `SPRING_DATASOURCE_USERNAME` | DB username | Yes      |
| `SPRING_DATASOURCE_PASSWORD` | DB password | Yes      |

### Observability

| Variable                      | Description    | Default    |
| ----------------------------- | -------------- | ---------- |
| `OTEL_EXPORTER_OTLP_ENDPOINT` | OTLP endpoint  | -          |
| `OTEL_SERVICE_NAME`           | Service name   | `idp-core` |
| `LOG_LEVEL`                   | Root log level | `INFO`     |

---

## Docker Compose Commands

### Start Services

```bash
# Start all
docker compose up -d

# Start specific service
docker compose up -d idp-core

# View logs
docker compose logs -f idp-core
```

### Stop Services

```bash
# Stop all
docker compose down

# Stop and remove volumes
docker compose down -v
```

### Update

```bash
# Pull latest images
docker compose pull

# Recreate containers
docker compose up -d --force-recreate
```

### Scale

```bash
# Run 3 instances
docker compose up -d --scale idp-core=3
```

---

## Database Management

### Initialize Database

The database schema is managed by Flyway and applies automatically on startup.

### Backup

```bash
docker exec postgres pg_dump -U idp idp > backup.sql
```

### Restore

```bash
cat backup.sql | docker exec -i postgres psql -U idp idp
```

---

## Troubleshooting

### Container Won't Start

```bash
# Check logs
docker logs idp-core

# Common issues:
# - Database not ready: ensure depends_on with healthcheck
# - Port conflict: change port mapping
# - Memory: increase Docker memory limit
```

### Database Connection Failed

```bash
# Verify database is running
docker exec postgres pg_isready -U idp

# Check network
docker network inspect bridge

# Test connection from app container
docker exec idp-core nc -zv postgres 5432
```

---

## Next Steps

- **[Kubernetes](kubernetes.md)** - Production deployment
- **[Configuration](configuration.md)** - All configuration options
- **[Observability](observability.md)** - Monitoring and logging
