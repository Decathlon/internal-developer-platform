---
title: Domain Model Validations
description: Domain model validation strategies and best practices for IDP-Core
---

## Validation Strategy

We use Hibernate Validator library at two levels:

1. **Data Transfer Objects**: Structure and format validation, for example `@NotBlank`, `@Email`.
2. **Entities**: Business rule validation and database constraints.

## Key Annotations

| Annotation             | Usage                                          |
| ---------------------- | ---------------------------------------------- |
| `@NotBlank`            | Strings must not be empty.                     |
| `@NotNull`             | Required fields.                               |
| `@Min` / `@Max`        | Numeric ranges.                                |
| `@Valid`               | Trigger nested validation on objects or lists. |
| `@Column(unique=true)` | Database-level uniqueness, handled by DB.      |

## Business Rules

* **Identifiers**: Must be unique.
* **Properties**: Must have unique names within a template.
* **Relations**: Must reference existing templates (no circular dependencies).
