---
title: Configuration
description: Configure the Internal Developer Platform - Database, security, profiles, and environment variables
---

The Internal Developer Platform uses Spring Boot's configuration system. This guide covers all configuration options.

## Configuration Files

The Internal Developer Platform uses YAML configuration files located in `src/main/resources/`:

| File | Purpose |
| ------ | --------- |
| `application.yml` | Base configuration |
| `application-local.yml` | Local development settings |
| `application-dev.properties` | Development environment |

---

## Core Configuration

### Database

Configure PostgreSQL connection:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/idpcore
    username: idpcore
    password: ${DB_PASSWORD}
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: validate  # Use Flyway for migrations
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
```

### Flyway Migrations

Configure the database location to manage schema migrations:

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
```

---

## Server Configuration

### HTTP Server

```yaml
server:
  port: 8084
  servlet:
    context-path: /
  compression:
    enabled: true
```

### CORS (Cross-Origin)

Configure CORS if your frontend loads from a different origin or if no API gateway handles CORS:

```yaml
spring:
  web:
    cors:
      allowed-origins:
        - http://localhost:3000
        - https://your-frontend.com
      allowed-methods:
        - GET
        - POST
        - PUT
        - DELETE
      allowed-headers: "*"
```

---

## Security Configuration

### OAuth 2.0 / JWT

We recommend OAuth 2.0 with JWT tokens for authentication and authorization.

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://your-auth-server.com
          jwk-set-uri: https://your-auth-server.com/.well-known/jwks.json
```

---

## Spring Profiles

The Internal Developer Platform supports multiple profiles for different deployment scenarios:

### Available Profiles

| Profile | Purpose |
| --------- | --------- |
| `local` | Local development with sample data |
| `dev` | Development environment |
| `test` | Testing with H2 in-memory database |
| `prod` | Production settings |

### Activating Profiles

**Via environment variable:**

```bash
export SPRING_PROFILES_ACTIVE=local
```

**Via command line:**

```bash
java -jar idp-core.jar --spring.profiles.active=local
```

**Via Docker:**

```bash
docker run -e SPRING_PROFILES_ACTIVE=local decathlon/idp-core
```

---

## Environment Variables

All configuration can be overridden using environment variables:

| Variable | Description | Example |
| ---------- | ------------- | --------- |
| `SPRING_PROFILES_ACTIVE` | Active profile | `local` |
| `SPRING_DATASOURCE_URL` | Database URL | `jdbc:postgresql://...` |
| `SPRING_DATASOURCE_USERNAME` | DB username | `idpcore` |
| `SPRING_DATASOURCE_PASSWORD` | DB password | `secret` |
| `SERVER_PORT` | HTTP port | `8084` |
| `LOGGING_LEVEL_ROOT` | Log level | `INFO` |

### Naming Convention

Spring Boot converts environment variables to properties:

- `SPRING_DATASOURCE_URL` → `spring.datasource.url`
- `SERVER_PORT` → `server.port`
- `IDP_SECURITY_CLIENT_ID` → `idp.security.client-id`

---

## Logging Configuration

### Log Levels

```yaml
logging:
  level:
    root: INFO
    com.decathlon.idp_core: DEBUG
    org.springframework.web: INFO
    org.hibernate.SQL: DEBUG

### Log Format (JSON for production)

```yaml
logging:
  pattern:
    console: '{"timestamp":"%d","level":"%p","logger":"%c","message":"%m"}%n'
```

---

## OpenTelemetry Configuration

For observability, OpenTelemetry is becoming the standard. Rather than using tons of logs, we recommend configuring tracing and metrics with OpenTelemetry. By default, IDP-Core includes OpenTelemetry SDK instrumentation. Configure the exporter and sampling rate as follows:

```yaml
management:
  tracing:
    enabled: true
    sampling:
      probability: 1.0

  otlp:
    tracing:
      endpoint: http://otel-collector:4318/v1/traces

otel:
  service:
    name: idp-core
  exporter:
    otlp:
      endpoint: http://otel-collector:4317
```

---

## Sample Configuration

### Local Development (`application-local.yml`)

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5437/idpcore
    username: idpcore
    password: idpcore_password

  flyway:
    locations: classpath:db/migration,classpath:db/local

server:
  port: 8084

logging:
  level:
    com.decathlon.idp_core: DEBUG
```

### Production (`application-prod.yml`)

```yaml
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5

server:
  port: 8080

logging:
  level:
    root: WARN
    com.decathlon.idp_core: INFO

management:
  tracing:
    enabled: true
```

---

## Next Steps

- **[Deployment Guide](../deployment/index.md)** - Deploy to production
- **[Docker Configuration](../deployment/docker.md)** - Container settings
- **[Observability](../deployment/observability.md)** - Monitoring and tracing
