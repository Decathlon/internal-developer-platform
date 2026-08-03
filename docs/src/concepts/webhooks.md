---
title: Webhooks
description: Understand webhook connectors, security strategies, and dynamic mappings in IDP-Core
---

Webhooks let external systems push JSON events to the Internal Developer Platform through a generic HTTP endpoint. You configure a webhook connector at runtime, choose a security strategy, and define mappings that translate incoming payloads into entity data with JSLT expressions.

## Overview

A webhook connector combines three concerns:

- **Connector metadata** - Identifier, name, description, and enabled flag
- **Security** - How IDP-Core authenticates incoming requests
- **Mappings** - How the payload maps to an Entity Template

```mermaid
flowchart LR
    S[External system] --> E[POST /webhooks/{configurationId}]
    E --> H[InboundWebhookHandler]
    H --> D[Security dispatcher]
    D --> C[WebhookConnector]
    C --> M[Dynamic mappings]
    M --> T[Entity Template]
```

## Webhook Connector

A webhook connector is the runtime configuration stored by IDP-Core for one inbound integration.

| Field                 | Type    | Description                                            |
| --------------------- | ------- | ------------------------------------------------------ |
| `identifier`          | String  | Stable key used in the webhook URL and management APIs |
| `name`                | String  | Human-readable name                                    |
| `description`         | String  | Optional explanation of the connector purpose          |
| `enabled`             | Boolean | Enables or disables request processing                 |
| `mapping_identifiers` | Array   | One or more dynamic mapping identifier                 |
| `security`            | Object  | Authentication strategy and configuration              |

### Webhook Connector Example

```json
{
  "identifier": "github-repositories",
  "name": "GitHub repositories",
  "description": "Receives repository events from GitHub",
  "enabled": true,
  "mapping_identifiers": [],
  "security": {
    "type": "HMAC_SHA256",
    "config": {
      "header_name": "X-Hub-Signature-256",
      "secret_alias": "GITHUB_WEBHOOK_SECRET",
      "prefix": "sha256="
    }
  }
}
```

## Dynamic Mappings

Each connector contains at least one dynamic mapping. A mapping targets one Entity Template and describes how to derive entity fields from the incoming JSON payload with a JSLT filter and entity projections.

| Field         | Type   | Description                                                                 |
| ------------- | ------ | --------------------------------------------------------------------------- |
| `template`    | String | Identifier of the target Entity Template                                    |
| `identifier`  | String | Stable and unique key for this specific mapping                             |
| `name`        | String | Human-readable name of the mapping                                          |
| `description` | String | Optional explanation of the mapping purpose                                 |
| `filter`      | String | JSLT boolean expression to evaluate if the payload should be processed      |
| `entity`      | Object | JSLT projections defining how to map the payload to the entity's attributes |

### Dynamic Mapping Example

```json
{
  "template": "github_repository",
  "identifier": "mapping-github",
  "name": "mapping github",
  "description": "mapping github description",
  "filter": ".repository != null",
  "entity": {
    "identifier": "replace(.repository.name, \" \", \"-\")",
    "name": ".repository.name",
    "properties": {
      "name": ".repository.name",
      "url": ".repository.html_url",
      "stars": "\"\" + .repository.stargazers_count",
      "is_public": "if (.repository.private) \"false\" else \"true\""
    },
    "relations": {}
  }
}
```

### Validation Rules

When you create or update a connector, the IDP validates each mapping against the target Entity Template.

It checks that:

- The referenced template exists
- Every mapped property exists in the template
- Every required property is mapped
- Every mapped relation exists in the template
- Every required relation is mapped

This validation keeps the connector configuration aligned with the current data model.

## Security Strategies

Each connector declares one security type. IDP-Core validates the configuration at creation time and validates requests again at runtime.

| Type           | Required configuration keys             | Runtime behavior                                                                       |
| -------------- | --------------------------------------- | -------------------------------------------------------------------------------------- |
| `HMAC_SHA256`  | `header_name`, `secret_alias`, `prefix` | Computes the SHA-256 HMAC of the raw body and compares it with the request header      |
| `STATIC_TOKEN` | `header_name`, `secret_alias`           | Compares a header value with a secret loaded from the environment                      |
| `BASIC_AUTH`   | `username`, `secret_alias`              | Compares the `Authorization: Basic ...` header with the configured username and secret |
| `JWT_BEARER`   | `jwks_uri`                              | Validates the bearer token against a JWKS endpoint                                     |
| `NONE`         | none                                    | Skips authentication                                                                   |

> [!IMPORTANT]
> Security configuration keys accept `snake_case` and `camelCase` variants for the supported fields.
> [!WARNING]
> `secret_alias` must reference an environment variable alias in `UPPER_SNAKE_CASE`. It does not store the raw secret value in the connector configuration.

### Example Security Configurations

=== "HMAC_SHA256"

```json
{
  "type": "HMAC_SHA256",
  "config": {
    "header_name": "X-Hub-Signature-256",
    "secret_alias": "GITHUB_WEBHOOK_SECRET",
    "prefix": "sha256="
  }
}
```

=== "STATIC_TOKEN"

```json
{
  "type": "STATIC_TOKEN",
  "config": {
    "header_name": "X-Webhook-Token",
    "secret_alias": "WEBHOOK_SHARED_TOKEN"
  }
}
```

=== "BASIC_AUTH"

```json
{
  "type": "BASIC_AUTH",
  "config": {
    "username": "webhook-user",
    "secret_alias": "WEBHOOK_PASSWORD"
  }
}
```

=== "JWT_BEARER"

```json
{
  "type": "JWT_BEARER",
  "config": {
    "jwks_uri": "https://issuer.example.com/.well-known/jwks.json"
  }
}
```

## Runtime Flow

The webhook runtime uses a single generic endpoint:

```text
POST /webhooks/{configurationId}
```

The request flow is:

1. IDP-Core receives the request on the generic webhook endpoint.
2. The `configurationId` resolves the stored `WebhookConnector`.
3. If the connector is disabled, IDP-Core ignores the event.
4. The security dispatcher selects the matching strategy for the connector security type.
5. The strategy validates the headers and, when needed, the raw request body.
6. After authentication, the event is accepted for downstream processing.

> [!IMPORTANT]
> The connector model, security validation, management APIs, and mapping validation are implemented now.

## Management API Methods

You manage webhook connectors through the inbound webhook management API, which exposes standard CRUD methods.

| HTTP Method | Endpoint                                  | Purpose          |
| ----------- | ----------------------------------------- | ---------------- |
| `POST`      | `/api/v1/inbound_webhooks`                | Create connector |
| `GET`       | `/api/v1/inbound_webhooks`                | List connectors  |
| `GET`       | `/api/v1/inbound_webhooks/{identifier}`   | Get connector    |
| `PUT`       | `/api/v1/inbound_webhooks/{identifier}`   | Update connector |
| `DELETE`    | `/api/v1/inbound_webhooks/{identifier}`   | Delete connector |

This separation keeps configuration management under versioned API routes while the event ingestion endpoint stays simple for external systems.

## When to Use Webhooks

Use webhooks when an external system can push JSON events over HTTP and you want to:

- Ingest updates without redeploying IDP-Core
- Reuse one generic endpoint for multiple providers
- Apply connector-specific authentication rules
- Map external payloads to your own Entity Templates at runtime

---

## Next Steps

- **[Entity Templates](entity-templates.md)** - Define the target structures that mappings reference
- **[Entities](entities.md)** - Understand the records produced by successful ingestion
- **[Relations](relations.md)** - Model links that webhook mappings can populate
- **[Data Integration](../features/data-integration.md)** - Explore the broader ingestion roadmap
