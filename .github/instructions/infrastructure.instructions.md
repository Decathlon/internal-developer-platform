---
description: 'Guidelines for modifying the Infrastructure layer'
applyTo: '**/infrastructure/**/*.java'
---

# Infrastructure Layer Instructions

## General Principles

- The Infrastructure layer contains **everything technical**: Spring Boot, REST, database, security, and serialization.
- Infrastructure adapters **implement or drive** the domain ports. They must never contain business logic.
- Infrastructure may depend on Domain models, ports, and exceptions. Domain must **never** depend on Infrastructure.
- Each adapter type (REST, Messaging, CLI) must define its **own input/output models**. Never reuse a Web DTO in a Messaging listener.

## API Adapter (Driving—Inbound)

### Controllers

- Adhere to RESTful principles. Use appropriate HTTP methods (`GET`, `POST`, `PUT`, `DELETE`).
- Controllers handle **orchestration and validation only**. All business logic belongs in Domain Services.
- Return proper status codes: `200 OK`, `201 Created`, `400 Bad Request`, `404 Not Found`, `409 Conflict`, `500 Internal Server Error`.
- Paginated responses return `200 OK` when successfully retrieved.
- Never expose Domain models directly in API responses. Always use DTOs.

### DTOs

- Prefer Java Records for all DTOs. Java classes are permitted when mutability is required.
- Use Jakarta Validation annotations on DTOs for syntactic validation (`@NotNull`, `@NotBlank`, `@Size`).
- Always use constants from `SwaggerDescription` for `@Schema` descriptions in DTOs to ensure consistency and avoid duplication.
- Separate input DTOs (`dto/in/`) from output DTOs (`dto/out/`).
- If a validation rule (for example, a complex regex) is shared across multiple adapters, extract it into a Domain Value Object.

### Mappers

- Convert between DTOs and Domain models. Mappers must be null-safe with no side effects.
- Use MapStruct for straightforward conversions.
- Use Record Patterns for complex transformations requiring business logic.
- Never use `ObjectMapper` or reflection-based libraries for internal layer mapping.

### Exception Handling

- Map domain exceptions to HTTP status codes in a centralized `@ControllerAdvice` class (`ApiExceptionHandler`).
- Domain exceptions carry business meaning; the handler translates them to HTTP semantics.

### Configuration

- Centralize Spring configuration in `infrastructure/adapters/api/configuration/`.
- This includes Security (JWT, CORS), Swagger/OpenAPI, pagination, and serialization settings.

## Persistence Adapter (Driven—Outbound)

### Adapter Classes

- Implement domain port interfaces (for example, `PostgresEntityTemplateAdapter implements EntityTemplateRepositoryPort`).
- Adapter classes are annotated with `@Component`.
- Convert between Domain models and JPA entities using persistence mappers.

### JPA Entities

- JPA entities are **mutable** and may differ from domain models.
- Avoid Lombok `@Data` on JPA entities, use `@Getter`, `@Setter`, `@ToString`, `@EqualsAndHashCode` separately.
- For mutable collection fields (`Set`, `List`), suppress Lombok-generated accessors with `@Getter(AccessLevel.NONE)` and `@Setter(AccessLevel.NONE)`, then provide **defensive copy** getters and setters.
- Prefer `UUID` identifiers and align `@Table`/`@Column` names with the Flyway-created schema (`snake_case`).
- Avoid relying on Hibernate proxies for equality; use getClass() instead of `instanceof` in equals to prevent proxy-related bugs.

### Audited Collections: Set vs List

When using Hibernate Envers (`@Audited`), the collection type significantly impacts your audit table schema design:

**Use `Set` (Recommended)**

- Order of elements does not need to be tracked
- Simpler audit table structure: composite primary key is `(entity_id, rev, element_id)`
- Better query performance on audit tables
- No additional tracking columns required
- Example: `private Set<TargetEntity> targets;`

**Use `List` (Only When Order Matters)**

- Element order must be preserved and tracked in audit history
- Hibernate Envers adds a `SETORDINAL` column to track position
- Composite primary key becomes: `(entity_id, rev, setordinal)`
- Higher storage overhead and more complex schema
- Always use `@OrderColumn(name="column_name")` to explicitly define the order column
- Example: `@OrderColumn(name="item_index") private List<TargetEntity> targets;`

### Schema Alignment

- Flyway migration must match Envers expectations for the chosen collection type
- Changing from `List` to `Set` or vice versa after data exists requires audit table restructuring
- Plan collection types early; retrofitting is costly and error-prone
- Audit table primary key structure must include order column only for `List` collections

**Decision Matrix:**

| Use Case                    | Collection Type | Rationale                             |
|-----------------------------|-----------------|---------------------------------------|
| Many-to-many relationships  | `Set`           | Natural fit, no order tracking needed |
| Element order insignificant | `Set`           | Simpler schema, better performance    |
| Preserve insertion order    | `List`          | Use with `@OrderColumn`               |
| Ranked/prioritized items    | `List`          | Order is business-critical            |

### Relationships & Fetching

- Prefer `LAZY` relationships by default; avoid `EAGER` unless you have a proven reason.
- Avoid `N+1` queries: use purpose-built repository queries, `JOIN FETCH`, or `@EntityGraph`.
- For `orphanRemoval = true` collections, always mutate through the **setter** (using `clear()` + `addAll()` internally) to preserve the Hibernate-managed `PersistentSet`.

### Schema & Migrations

- Do not rely on Hibernate to create or update the schema (`ddl-auto: none`).
- All schema changes must use Flyway migrations. See `database.instructions.md`.

### Transactions

- Place transaction boundaries at the Domain Service layer using `@Transactional`.
- Use `@Transactional(readOnly = true)` for read-only operations.

## Package Structure

Every adapter **must** include a `mapper/` package for model conversion. Add a `configuration/` package when the adapter requires Spring `@Configuration` classes (for example, security, serialization, or bean wiring).

```text
infrastructure/
└── adapters/
    ├── api/                          # Driving adapter (inbound — REST API)
    │   ├── configuration/            # Spring Config, Security, Swagger, JWT, CORS
    │   ├── controller/               # REST controllers
    │   ├── dto/
    │   │   ├── in/                   # Request DTOs
    │   │   └── out/                  # Response DTOs
    │   ├── handler/                  # Global exception → HTTP status mapping
    │   └── mapper/                   # DTO ↔ Domain mapping
    └── persistence/                  # Driven adapter (outbound — database)
        ├── mapper/                   # Domain ↔ JPA entity mapping
        ├── model/                    # JPA entities
        └── repository/              # Spring Data JPA interfaces
```

> Future adapters (for example, `messaging/`, `etl/`) must follow the same convention: always include a `mapper/` package and add a `configuration/` package only when Spring configuration is needed.
