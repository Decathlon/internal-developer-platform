---
title: Entity Dynamic Mappings
description: Understand Dynamic mappings and JSLT expressions.
---

## Overview

A mapping targets one Entity Template and describes how to derive entity fields from the incoming JSON payload with a JSLT filter and entity projections.

## Entity Dynamic Mapping Fields

| Field                        | Required | Description                                                                                                          |
| ---------------------------- | -------- | ---------------------------------------------------------------------------------------------------------------------|
| `identifier`                 | ✅       | Unique key for this mapping                                                                                          |
| `name`                       | ✅       | Human-readable name                                                                                                  |
| `description`                |          | Purpose of the mapping                                                                                               |
| `entity_template_identifier` | ✅       | Target Entity Template identifier                                                                                    |
| `filter`                     | ✅       | JSLT expression. Payload is skipped if it returns false                                                              |
| `action`                     | ✅       | The mutation logic applied to the entity (`UPDATE_ENTITY`, `UPDATE_PROPERTIES`, `UPDATE_RELATIONS`, `DELETE_ENTITY`) |
| `entity`                     | ✅       | Entity mapping configuration (see below)                                                                             |

## Entity Mapping Configuration

| Field        | Required | Description                                              |
| ------------ | -------- | -------------------------------------------------------- |
| `identifier` | ✅       | JSLT expression to generate the entity identifier        |
| `name`       | ✅       | JSLT expression for the entity name                      |
| `properties` |          | Map of property names to JSLT expressions                |
| `relations`  |          | Array of relation definitions (see below)                |

### Relation Definition

| Field                       | Required | Description                                              |
| --------------------------- | -------- | -------------------------------------------------------- |
| `name`                      | ✅       | Relation name from the Entity Template                   |
| `target_entity_identifiers` | ✅       | Array of JSLT expressions to extract target identifiers  |

### Mapping Actions

| Action              | Behaviour                                                                   |
| ------------------  | ----------------------------------------------------------------------------|
| `UPDATE_ENTITY`     | Creates the entity if absent, otherwise patches all fields                  |
| `UPDATE_PROPERTIES` | Creates the entity if absent, otherwise patches name and properties only    |
| `UPDATE_RELATIONS`  | Creates the entity if absent, otherwise patches relations only              |
| `DELETE_ENTITY`     | Deletes the entity if it exists                                             |

### Filtering Payloads with `filter`

The `filter` field determines whether a mapping applies to a payload:

```jslt
# Apply mapping only to private projects
.visibility == "private"

# Apply mapping only if webhook action is create/edit
(.action == "created" or .action == "edited")

# Apply mapping only if critical metrics exist
(.measures[] | select(.metric == "new_violations") | .value) > 0

# Complex: apply only if specific conditions are met
(.visibility == "private" and .status == "active" and (.language // "java") == "java")
```

## API Reference

| Method   | Endpoint                                       | Purpose                                      |
| -------- | ---------------------------------------------- | -------------------------------------------- |
| `POST`   | `/api/v1/entity_dynamic_mappings`              | Create an entity dynamic mapping             |
| `GET`    | `/api/v1/entity_dynamic_mappings`              | List entity dynamic mappings (paginated)     |
| `GET`    | `/api/v1/entity_dynamic_mappings/{identifier}` | Read one entity dynamic mapping              |
| `PUT`    | `/api/v1/entity_dynamic_mappings/{identifier}` | Update one entity dynamic mapping            |
| `DELETE` | `/api/v1/entity_dynamic_mappings/{identifier}` | Delete one entity dynamic mapping            |
| `POST`   | `/api/v1/entity_dynamic_mappings/dry-run`      | Test a mapping with sample payload           |

## JSLT Expressions

Entity Dynamic Mappings use **JSLT (JSON Streaming Language)** to transform incoming
JSON payloads. [JSLT](https://github.com/schibsted/jslt) is a powerful DSL for JSON manipulation designed for filtering,
selecting, and transforming JSON data without code.

### Custom JSLT Functions

IDP-Core can integrate and expose custom JSLT functions, using Java, for common webhook transformations. For example:

| Function        | Usage                           | Description                                   | Example                                                   |
| --------------- | ------------------------------- | --------------------------------------------- | --------------------------------------------------------- |
| `base64-decode` | `base64-decode(.payload_data)`  | Decode Base64-encoded strings in the payload  | `base64-decode("SGVsbG8gV29ybGQ=")` → `"Hello World"`     |

#### JSLT Expression Examples

##### Basic Field Extraction

```jslt
# Extract simple field
.repository.name

# Extract with type conversion
string(.project_id)

# Extract with fallback
.language // "Unknown"
```

##### Array Operations

```jslt
# Extract all metric values
[for (.measures) .value]

# Extract specific metric value
[for (.measures) .value if (.metric == "ncloc")] | .[0]

# Transform array elements
[for (.components) { "name": .name, "version": string(.version) }]

# Count matching items
size([for (.measures) . if (.metric == "new_violations")])
```

##### String Manipulation

```jslt
# Normalize identifier (remove spaces, replace special chars)
replace(.project_key, " ", "-")

# Extract domain from URL
split(.repository_url, "/")[2]

# Conditional string generation
if (.is_private == true) "PRIVATE_" + .name else .name
```

##### Complex Filtering and Transformation

```jslt
# Extract and transform multiple fields
{
  "identifier": re-replace(string(.key), "\\s", "_"),
  "name": .name,
  "language": fallback(.language, "Unknown"),
  "metrics": {
    "violations": [for (.measures) .value if (.metric == "new_violations")] | .[0],
    "loc": [for (.measures) .value if (.metric == "ncloc")] | .[0]
  }
}

## Dynamic Mapping Example

```json
{
  "template": "github_repository",
  "identifier": "mapping-github",
  "name": "mapping github",
  "description": "mapping github description",
  "action": "UPDATE_ENTITY",
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

> [!TIP]
> Use the **dry-run endpoint** (POST `/api/v1/entity_dynamic_mappings/dry-run`) to test
> your JSLT expressions before deploying them to production. This helps catch syntax
> errors and unexpected transformations early.
> [!TIP]
> For comprehensive JSLT documentation, refer to the
> [official JSLT GitHub repository](https://github.com/schibsted/jslt). The platform
> supports JSLT version 0.1.x built-in functions and custom extensions.
