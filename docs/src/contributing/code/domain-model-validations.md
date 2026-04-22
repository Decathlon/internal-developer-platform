---
title: Domain Model Validations
description: Domain model validation strategies and best practices for IDP-Core
---

## Validation Strategy

We perform validation at multiple layers, each with a distinct responsibility:

1. **Domain Layer (Primary Source of Truth):**
   - Enforces core business rules and invariants.
   - Domain models and services must guarantee that only valid business states are possible, using Jakarta Validation annotations (for example, `@NotNull`, `@Size`) and explicit logic.
   - All critical validation should reside here, as the domain is responsible for business correctness.

2. **Infrastructure Layer:**
   - **DTOs:** Handle syntactic and structural validation (for example, `@NotBlank`, `@Email`, format checks) to ensure only well-formed data reaches the domain.
   - **Database:** Enforces technical constraints (for example, `NOT NULL`, uniqueness) for data quality and integrity, but should not be relied upon for business rule enforcement.

 The domain layer is the principal authority for business validation. Infrastructure validations (DTOs, database) act as a first line of defense and for technical integrity, but cannot replace domain-level enforcement.

## Key Annotations

| Annotation             | Usage                                          |
| ---------------------- | ---------------------------------------------- |
| `@NotBlank`            | Strings must not be empty.                     |
| `@NotNull`             | Required fields.                               |
| `@Min` / `@Max`        | Numeric ranges.                                |
| `@Valid`               | Trigger nested validation on objects or lists. |
| `@Column(unique=true)` | Database-level uniqueness, handled by DB.      |

## Business Rules

- **Identifiers**: Must be unique.
- **Properties**: Must have unique names within a template.
- **Relations**: Must reference existing templates (no circular dependencies).
