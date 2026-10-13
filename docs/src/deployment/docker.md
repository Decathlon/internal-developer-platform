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
git clone https://github.com/decathlon/internal-developer-platform.git
cd internal-developer-platform
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
    image: postgres:14-alpine
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
  decathlon/internal-developer-platform:latest
```

### With Volume Mounts

```bash
docker run -d \
  --name idp-core \
  -p 8080:8080 \
  -v $(pwd)/config:/app/config:ro \
  -v $(pwd)/logs:/app/logs \
  -e SPRING_CONFIG_ADDITIONAL_LOCATION=/app/config/ \
  decathlon/internal-developer-platform:latest
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
  postgres:14-alpine

# Run IDP-Core
docker run -d \
  --name idp-core \
  --network idp-network \
  -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/idp \
  -e SPRING_DATASOURCE_USERNAME=idp \
  -e SPRING_DATASOURCE_PASSWORD=idp \
  decathlon/internal-developer-platform:latest
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
docker build -t internal-developer-platform:local .

# Run
docker run -d -p 8080:8080 internal-developer-platform:local
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

IDP-Core follows the standard OpenTelemetry environment variables. Tracing and metrics export are opt-in, and the application keeps the usual OpenTelemetry fallbacks so you can set the standard variables without hard-coding a vendor-specific configuration.

The effective precedence is:

- `OTEL_EXPORTER_OTLP_TRACES_TRANSPORT` > `OTEL_EXPORTER_OTLP_PROTOCOL` > `http`
- `OTEL_EXPORTER_OTLP_METRICS_PROTOCOL` > `OTEL_EXPORTER_OTLP_PROTOCOL` > `http/protobuf`

This means the application can work in local Docker, in Kubernetes, or in a managed platform while still honoring the standard OTel variables set by the operator or deployment pipeline.

| Variable                              | Description                                              | Default                                        |
|---------------------------------------|----------------------------------------------------------|------------------------------------------------|
| `OTEL_TRACING_ENABLED`                | Enables OpenTelemetry tracing (opt-in)                   | `false`                                        |
| `OTEL_EXPORTER_OTLP_ENDPOINT`         | Base OTLP endpoint for all signals                       | `http://localhost:4318`                        |
| `OTEL_EXPORTER_OTLP_TRACES_ENDPOINT`  | Full traces URL. It wins over the base endpoint          | `<base endpoint>/v1/traces`                    |
| `OTEL_EXPORTER_OTLP_METRICS_ENDPOINT` | Full metrics URL. It wins over the base endpoint         | `<base endpoint>/v1/metrics`                   |
| `OTEL_EXPORTER_OTLP_PROTOCOL`         | OTLP protocol used as the generic fallback               | `http/protobuf` for metrics, `http` for traces |
| `OTEL_EXPORTER_OTLP_TRACES_TRANSPORT` | Transport for traces. It wins over the generic protocol  | `OTEL_EXPORTER_OTLP_PROTOCOL` or `http`        |
| `OTEL_EXPORTER_OTLP_METRICS_PROTOCOL` | Transport for metrics. It wins over the generic protocol | `OTEL_EXPORTER_OTLP_PROTOCOL`                  |
| `OTEL_METRICS_EXPORT_ENABLED`         | Enables OTLP metrics export (opt-in)                     | `false`                                        |
| `OTEL_TRACES_SAMPLER_ARG`             | Fraction of traces kept, from `0.0` to `1.0`             | `0.1`                                          |
| `OTEL_SERVICE_NAME`                   | Service name                                             | `internal-developer-platform`                  |
| `OTEL_SERVICE_NAMESPACE`              | Service namespace                                        | `idp-core`                                     |
| `OTEL_ENV`                            | Value of the `deployment.environment` attribute          | `local`                                        |
| `OTEL_RESOURCE_ATTRIBUTES`            | Extra resource attributes (`key=value,key=value`)        | -                                              |
| `LOG_LEVEL`                           | Root log level                                           | `INFO`                                         |

The signal-specific variable wins over the generic one, so the image works in the common deployments:

- **Docker Compose or a simple container:** set `OTEL_EXPORTER_OTLP_ENDPOINT`
- **Kubernetes with the OpenTelemetry Operator:** the platform injects `OTEL_EXPORTER_OTLP_TRACES_ENDPOINT`
- **Datadog or another OTLP collector:** set the standard OTel endpoint and resource attributes
- **Local development:** leave the variables unset and use the built-in `localhost:4318` fallback

> [!NOTE]
> Lower `OTEL_TRACES_SAMPLER_ARG` in high-throughput production. Capturing 100% of the traces increases CPU overhead
> and vendor costs. For example, `0.1` keeps 10% of the traces.

#### Example: Docker deployment with Datadog

This example sends traces and metrics to a Datadog Agent exposing the OTLP receiver on port `4318`.

```bash
docker run -d --name idp-core \
  -e OTEL_TRACING_ENABLED=true \
  -e OTEL_METRICS_EXPORT_ENABLED=true \
  -e OTEL_EXPORTER_OTLP_ENDPOINT=http://datadog-agent:4318 \
  -e OTEL_EXPORTER_OTLP_PROTOCOL=http/protobuf \
  -e OTEL_TRACES_SAMPLER_ARG=0.2 \
  -e OTEL_SERVICE_NAME=idp-core \
  -e OTEL_RESOURCE_ATTRIBUTES="deployment.environment=prod,service.namespace=idp-core,service.name=internal-developer-platform,service.version=1.0.0,team=platform" \
  idp-core:latest
```

`OTEL_RESOURCE_ATTRIBUTES` is the right place to pass Datadog tags such as `env`, `service`, and `version`. The application does not need hard-coded Datadog values; it just emits the standard OTel resource metadata.

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
