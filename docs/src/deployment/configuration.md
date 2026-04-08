---
title: Configuration Reference
description: Complete configuration reference for IDP-Core
---

IDP-Core uses Spring Boot configuration. You can provide configuration settings via YAML files, environment variables, or command-line arguments.

## Configuration Hierarchy

Configuration loads in order, with later sources overriding earlier ones:

1. `application.yml` as default
2. `application-{profile}.yml` for profile-specific settings
3. Environment variables
4. Command-line arguments

---

## Profiles

### Available Profiles

| Profile | Purpose | Description |
| --------- | --------- | ------------- |
| `local` | Local development | Uses local PostgreSQL, debug logging |
| `dev` | Development server | Development environment settings |
| `test` | Testing | In-memory database, test fixtures |
| `prod` | Production | Production-optimized settings |

### Activate Profile

```bash
# Environment variable
export SPRING_PROFILES_ACTIVE=prod

# Command line
java -jar idp-core.jar --spring.profiles.active=prod

# Docker
docker run -e SPRING_PROFILES_ACTIVE=prod ...
```

---

## Server Configuration

```yaml
server:
  port: 8080
  shutdown: graceful

  # Compression
  compression:
    enabled: true
    mime-types: application/json,text/html,text/plain
    min-response-size: 1024

  # Tomcat settings
  tomcat:
    max-threads: 200
    min-spare-threads: 10
    accept-count: 100
    max-connections: 10000
    connection-timeout: 20000
```

| Property | Description | Default |
| ---------- | ------------- | --------- |
| `server.port` | HTTP port | `8080` |
| `server.shutdown` | Shutdown mode (`graceful`/`immediate`) | `graceful` |
| `server.tomcat.max-threads` | Max worker threads | `200` |

---

## Database Configuration

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/idp
    username: idp
    password: ${DATABASE_PASSWORD}
    driver-class-name: org.postgresql.Driver

    # Connection pool (HikariCP)
    hikari:
      minimum-idle: 5
      maximum-pool-size: 20
      idle-timeout: 300000
      connection-timeout: 20000
      max-lifetime: 1200000
      pool-name: IDP-HikariPool

  # JPA settings
  jpa:
    hibernate:
      ddl-auto: none  # Flyway manages schema
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        jdbc:
          batch_size: 50
        order_inserts: true
        order_updates: true
```

| Property | Description | Default |
| ---------- | ------------- | --------- |
| `spring.datasource.url` | JDBC URL | Required |
| `spring.datasource.hikari.maximum-pool-size` | Max connections | `20` |
| `spring.jpa.hibernate.ddl-auto` | Schema management | `none` |

### Database Migration (Flyway)

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
    validate-on-migrate: true
```

---

## Application Settings

```yaml
app:
  idp-core-prefix-url: http://localhost:8084
```

---

## Caching

```yaml
spring:
  cache:
    type: caffeine
    caffeine:
      spec: maximumSize=10000,expireAfterWrite=5m

# Or use Redis
  cache:
    type: redis
  redis:
    host: localhost
    port: 6379
    password: ${REDIS_PASSWORD:}
    timeout: 2000ms
```

---

## Observability

### OpenTelemetry

```yaml
management:
  tracing:
    enabled: true
    sampling:
      probability: 1.0

otel:
  exporter:
    otlp:
      endpoint: http://otel-collector:4317
  service:
    name: idp-core
  resource:
    attributes:
      deployment.environment: production
```

---

### OAuth 2.0

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://auth.example.com
          jwk-set-uri: https://auth.example.com/.well-known/jwks.json
```

---

## Environment Variables

All properties can be set via environment variables:

```bash
# Convert property path to env var:
# spring.datasource.url → SPRING_DATASOURCE_URL

export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/idp
export SPRING_DATASOURCE_USERNAME=idp
export SPRING_DATASOURCE_PASSWORD=secret
export IDP_WEBHOOKS_ENABLED=true
export IDP_MCP_ENABLED=true
```

### Common Environment Variables

| Variable | Property | Description |
| ---------- | ---------- | ------------- |
| `SPRING_PROFILES_ACTIVE` | `spring.profiles.active` | Active profile |
| `SERVER_PORT` | `server.port` | HTTP port |
| `SPRING_DATASOURCE_URL` | `spring.datasource.url` | Database URL |
| `SPRING_DATASOURCE_USERNAME` | `spring.datasource.username` | DB username |
| `SPRING_DATASOURCE_PASSWORD` | `spring.datasource.password` | DB password |
| `LOG_LEVEL` | `logging.level.root` | Log level |
| `JAVA_OPTS` | - | JVM options |

---

## Profile-Specific Files

### `application-local.yml`

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/idp
    username: idp
    password: idp

logging:
  level:
    com.decathlon.idp_core: DEBUG
    org.hibernate.SQL: DEBUG

idp:
  webhooks:
    enabled: true
```

### `application-prod.yml`

```yaml
spring:
  datasource:
    url: ${DATABASE_URL}
    username: ${DATABASE_USERNAME}
    password: ${DATABASE_PASSWORD}
    hikari:
      maximum-pool-size: 50

logging:
  level:
    root: WARN
    com.decathlon.idp_core: INFO

server:
  compression:
    enabled: true

management:
  endpoints:
    web:
      exposure:
        include: health,prometheus
```

---

## Next Steps

- **[Docker](docker.md)** - Container configuration
- **[Kubernetes](kubernetes.md)** - K8s deployment settings
- **[Observability](observability.md)** - Monitoring setup
