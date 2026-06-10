---
title: Filtering entities
description: Query and filter entities
---

## Filtering options

| Feature                | Option 1: GET Query DSL (`q=`)         | Option 2: POST JSON Search                                |
| :--------------------- | :------------------------------------- | :-------------------------------------------------------- |
| **Best for**           | Quick, simple, template-scoped filters | Complex, nested logic, or global cross-template discovery |
| **Endpoint scope**     | Scoped to a single template            | Cross-template (Global system search)                     |
| **Logical connectors** | AND only                               | AND / OR                                                  |
| **Operators**          | `=`, `:`, `<`, `>`                     | `EQ`, `NEQ`, `CONTAINS`, `STARTS_WITH`, `GT`, `LTE`, etc. |
| **Global text search** | No, strictly filtering                 | Yes, via optional free-text `query` property              |

## Option 1: Simple GET Filter DSL (`q` parameter)

Entity filtering extends the existing list-entities endpoint with an optional `q` query parameter. It lets you narrow down results by attributes, property values, and relations using a simple DSL with AND logic.

```http
GET /api/v1/entities/{templateIdentifier}?q=<filter>
```

Without `q`, the endpoint returns all entities for the template (paginated). With `q`, it returns only the entities that match every criterion:

```bash
# Without filter — returns all services
curl "http://localhost:8084/api/v1/entities/service"

# With filter — returns only matching services
curl "http://localhost:8084/api/v1/entities/service?q=name:api;property.status=production"
```

---

### DSL Syntax

Each filter is a semicolon-separated list of criteria:

```text
<key><operator><value>[;<key><operator><value>...]
```

All criteria are combined with **AND** logic, meaning every criterion must match for an entity to appear in the results. It also means users cannot duplicate criteria, each key can appear at most once per filter `q`expression.

### Operators

| Operator     | Symbol | Behavior                         | Example                |
| ------------ | ------ | -------------------------------- | ---------------------- |
| Equals       | `=`    | Exact match (case-insensitive)   | `name=payment-service` |
| Contains     | `:`    | Partial match (case-insensitive) | `name:payment`         |
| Less than    | `<`    | Less than comparison             | `property.port<9000`   |
| Greater than | `>`    | Greater than comparison          | `property.port>1000`   |

> [!WARNING]
> `<` and `>` are only supported for property filters (of type NUMBER). Using them on attributes (identifier, name) or relation filters returns an HTTP `400 Bad Request`.

---

### Key Types

#### Attribute Filters

Filter by a direct entity field. Supported fields are `identifier` and `name`.

```text
identifier=checkout-service
name:api
```

#### Property Filters

Filter by a property value using `property.<name>`, where `<name>` is the property's name as defined in the template.

```text
property.status=production
property.language:java
property.team:platform
```

### Relation Filters

**Filter by relation existence:** match entities that have a relation with the given name:

```text
relation=owns
relation:api
```

**Filter by relation target entity:** match entities whose relation `<name>` references a specific entity identifier:

```text
relation.database=my-postgres-1
relation.owned_by:platform
```

**Filter by a property of the related entity:** match entities via their related entity's `identifier` or `name`:

```text
relation.database.identifier=my-postgres-1
relation.database.name:prod
```

#### Reverse Relation Filters

Use `relations_as_target.<name>.<property>` to find entities that _appear as targets_ in a relation of type `<name>`. The `<property>` must be `identifier` or `name` and refers to the **source** entity in that relation.

```text
relations_as_target.owned_by.name:platform-team
relations_as_target.uses.identifier=service-1
```

---

### Combining Criteria

Join multiple criteria with `;` to narrow results further:

```bash
# Java services with production status
curl "http://localhost:8084/api/v1/entities/service?q=property.language=java;property.status=production"

# Services owned by a team whose name contains "platform"
curl "http://localhost:8084/api/v1/entities/service?q=relation.owned_by.name:platform"

# Services using a PostgreSQL database, owned by the platform team
curl "http://localhost:8084/api/v1/entities/service?q=relation.database=my-postgres-1;relation.owned_by=platform-team"
```

---

### Examples

```bash
# Find a service by exact identifier
curl "http://localhost:8084/api/v1/entities/service?q=identifier=checkout-service"

# Find all services with "api" in their name
curl "http://localhost:8084/api/v1/entities/service?q=name:api"

# Find all production services
curl "http://localhost:8084/api/v1/entities/service?q=property.status=production"

# Find services linked to a specific database entity
curl "http://localhost:8084/api/v1/entities/service?q=relation.database=my-postgres-1"

# Find services targeted by an "api-link" relation from an entity named "gateway"
curl "http://localhost:8084/api/v1/entities/service?q=relations_as_target.api-link.name:gateway"
```

---

## Option 2: POST /api/v1/entities/search (JSON advanced search)

For more complex queries across all templates you can use the JSON search endpoint which accepts a nested filter tree, free-text `query`, pagination and sorting.

Request shape (`EntitySearchRequestDtoIn`):

```json
{
  "query": "checkout",
  "filter": {
    "connector": "AND",
    "criteria": [
      { "field": "template", "operation": "EQ", "value": "microservice" },
      {
        "connector": "OR",
        "criteria": [
          { "field": "property.language", "operation": "EQ", "value": "JAVA" },
          { "field": "property.language", "operation": "EQ", "value": "KOTLIN" }
        ]
      }
    ]
  },
  "page": 0,
  "size": 20,
  "sort": "identifier:asc"
}
```

### Request body syntax

- `filter` is a tree of group nodes and criterion nodes. A group node has `connector` (one of `AND`,`OR`) and a non-empty `criteria` array. A criterion node must include `field`, `operation` and `value`.
- Supported `operation` values: `EQ`, `NEQ`, `CONTAINS`, `NOT_CONTAINS`, `STARTS_WITH`, `ENDS_WITH`, `GT`, `GTE`, `LT`, `LTE`.
- Free-text `query` (optional) performs a case-insensitive contains search across identifier, name, template identifier and all property values.
- Pagination: `page` is zero-based, `size` defaults to 20. Maximum allowed `size` is 500.

### What it does

- Execute cross-template searches with precise logical compositions and full-text assistance.
- Combine `query` and `filter` to first narrow by free-text and then apply structured criteria (or vice-versa).

Quick curl example

```bash
curl -sS -X POST "http://localhost:8084/api/v1/entities/search" \
  -H 'Content-Type: application/json' \
  -d '{"query":"checkout","filter":{"connector":"AND","criteria":[{"field":"template","operation":"EQ","value":"microservice"}]},"page":0,"size":20}'
```

### Field reference

- `query` (optional): free-text string (max 255 chars) searched case-insensitively in `identifier`, `name`, `templateIdentifier`, and property values.
- `filter` (optional): nested `FilterNodeDtoIn` JSON structure. Use a `template` criterion to scope results to a specific template when needed.
- `page` / `size`: pagination (zero-based `page`). `size` defaults to 20 and the server enforces a maximum of 500.
- `sort`: `field:asc|desc`. Allowed sort fields: `identifier`, `name`, `templateIdentifier`.

### Operator semantics

| Operator                    | Meaning                                                         |
| --------------------------- | --------------------------------------------------------------- |
| `EQ`                        | Exact, case-insensitive equality                                |
| `NEQ`                       | Not equal (case-insensitive)                                    |
| `CONTAINS`                  | Case-insensitive string match                                   |
| `NOT_CONTAINS`              | Negated string match                                            |
| `STARTS_WITH` / `ENDS_WITH` | Prefix / suffix match                                           |
| `GT` / `GTE` / `LT` / `LTE` | Ordering comparisons (string or numeric depending on the field) |

### Enforced limits

- Maximum filter nesting depth: 5 levels. Requests exceeding this fail with `400 Bad Request` and a descriptive error message.
- Maximum total criterion count across the tree: 50. Requests exceeding this fail with `400 Bad Request`.
- Maximum `query` length: 255 characters.
- Maximum `size` (page size): 500.

## Next Steps

- **[Entities](entities.md)** - Entity structure and core fields
- **[Properties](properties.md)** - Property types and validation rules
- **[Relations](relations.md)** - How entities connect into a graph
- **[API Reference](../api/index.md)** - Interactive API documentation
