---
title: Webhooks
description: Understand webhook connectors, security strategies, and dynamic mappings in IDP-Core
---

Webhooks let external systems push JSON events to IDP-Core through a generic HTTP endpoint. You configure a webhook connector at runtime, choose a security strategy, and define mappings that translate incoming payloads into entity data with JSLT expressions.

## Overview

A webhook connector combines three concerns:

- **Connector metadata** - Identifier, title, description, and enabled flag
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

| Field | Type | Description |
| --- | --- | --- |
| `identifier` | String | Stable key used in the webhook URL and management APIs |
| `title` | String | Human-readable name |
| `description` | String | Optional explanation of the connector purpose |
| `enabled` | Boolean | Enables or disables request processing |
| `mappings` | Array | One or more dynamic mapping rules |
| `security` | Object | Authentication strategy and configuration |

### Example

```json
{
  "identifier": "github-repositories",
  "title": "GitHub repositories",
  "description": "Receives repository events from GitHub",
  "enabled": true,
  "mappings": [
    {
      "template": "github_repository",
      "filter": ".action == \"created\" or .action == \"edited\"",
      "entity": {
        "identifier": ".repository.full_name | gsub(\"/\"; \"_\")",
        "title": ".repository.name",
        "properties": {
          "name": ".repository.name",
          "url": ".repository.html_url",
          "language": ".repository.language // \"Unknown\""
        },
        "relations": {
          "owner": ".repository.owner.login"
        }
      }
    }
  ],
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

| Field | Description |
| --- | --- |
| `template` | Target Entity Template identifier |
| `filter` | Expression that decides whether the mapping applies |
| `entity.identifier` | Expression that generates the entity identifier |
| `entity.title` | Expression that generates the entity title |
| `entity.properties` | Map of template property names to extraction expressions |
| `entity.relations` | Map of template relation names to extraction expressions |

### Validation Rules

When you create or update a connector, IDP-Core validates each mapping against the target Entity Template.

It checks that:

- The referenced template exists
- Every mapped property exists in the template
- Every required property is mapped
- Every mapped relation exists in the template
- Every required relation is mapped

This validation keeps the connector configuration aligned with the current data model.

## Security Strategies

Each connector declares one security type. IDP-Core validates the configuration at creation time and validates requests again at runtime.

| Type | Required configuration keys | Runtime behavior |
| --- | --- | --- |
| `HMAC_SHA256` | `header_name`, `secret_alias`, `prefix` | Computes the SHA-256 HMAC of the raw body and compares it with the request header |
| `STATIC_TOKEN` | `header_name`, `secret_alias` | Compares a header value with a secret loaded from the environment |
| `BASIC_AUTH` | `username`, `secret_alias` | Compares the `Authorization: Basic ...` header with the configured username and secret |
| `JWT_BEARER` | `jwks_uri` | Validates the bearer token against a JWKS endpoint |
| `NONE` | none | Skips authentication |

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

| HTTP Method | Endpoint | Purpose |
| --- | --- | --- |
| `POST` | `/api/v1/inbound-webhooks` | Create connector |
| `GET` | `/api/v1/inbound-webhooks` | List connectors |
| `GET` | `/api/v1/inbound-webhooks/{identifier}` | Get connector |
| `PUT` | `/api/v1/inbound-webhooks/{identifier}` | Update connector |
| `DELETE` | `/api/v1/inbound-webhooks/{identifier}` | Delete connector |

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
