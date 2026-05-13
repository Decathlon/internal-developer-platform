---
title: Filtering entities
description: Query and filter template-scoped entities using the q= filter DSL
---

Entity filtering extends the existing list-entities endpoint with an optional `q` query parameter. It lets you narrow down results by attributes, property values, and relations using a simple DSL with AND logic.

> [!WARNING]
> This is a first version of entity filtering with known constraints: `<` and `>` operators are not supported for relation filters, each key can appear at most once per query, and OR logic between criteria is not supported. These limitations will be addressed in future versions.

## Endpoint

The `q` parameter is an optional addition to the existing list-entities endpoint:

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

## Syntax

Each filter is a semicolon-separated list of criteria:

```text
<key><operator><value>[;<key><operator><value>...]
```

All criteria are combined with **AND** logic, meaning every criterion must match for an entity to appear in the results.

### Operators

| Operator      | Symbol | Behavior                           | Example                 |
| ------------- | ------ | ---------------------------------- | ----------------------- |
| Equals        | `=`    | Exact match (case-insensitive)     | `name=payment-service`  |
| Contains      | `:`    | Partial match (case-insensitive)   | `name:payment`          |
| Less than     | `<`    | Less than comparison               | `property.port<9000`    |
| Greater than  | `>`    | Greater than comparison            | `property.port>1000`    |

> [!WARNING]
> `<` and `>` are only supported for attribute and property filters. Using them on relation filters returns an HTTP `400 Bad Request`.

---

## Key Types

### Attribute Filters

Filter by a direct entity field. Supported fields are `identifier` and `name`.

```text
identifier=checkout-service
name:api
```

### Property Filters

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

### Reverse Relation Filters

Use `relations_as_target.<name>.<property>` to find entities that *appear as targets* in a relation of type `<name>`. The `<property>` must be `identifier` or `name` and refers to the **source** entity in that relation.

```text
relations_as_target.owned_by.name:platform-team
relations_as_target.uses.identifier=service-1
```

---

## Combining Criteria

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

## Examples

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

## Known Constraints

This first version of entity filtering has the following constraints:

| Constraint                    | Detail                                                                     |
| ----------------------------- | -------------------------------------------------------------------------- |
| Operators on relation filters | `<` and `>` are not supported for any relation filter type                 |
| Logic                         | AND only—no OR/NOT between criteria                                        |
| Duplicate criteria            | Each key can appear at most once per query                                 |
| Max criteria per query        | 10                                                                         |
| Max key length                | 255 characters                                                             |
| Max value length              | 255 characters                                                             |
| Property value type-awareness | All comparisons are string-based regardless of property type               |

Exceeding the numeric limits returns an HTTP `400 Bad Request` with a descriptive error message.

---

## Next Steps

- **[Entities](entities.md)** - Entity structure and core fields
- **[Properties](properties.md)** - Property types and validation rules
- **[Relations](relations.md)** - How entities connect into a graph
- **[API Reference](../api/index.md)** - Interactive API documentation
