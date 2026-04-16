---
description: 'Guidelines for modifying Infrastructure layer'
applyTo: '**/insfrastructure/*.java'
---

# Infrastructure Layer related instructions

## General Instructions

- Layer Separation: Use Jakarta Validation annotations on DTOs in the Infrastructure/Web layer to handle syntactic validation (e.g., "Field is required," "String length"). 
- Adapter-Specific DTOs: Every input adapter (REST, Messaging, Cron, CLI) must define its own input models/DTOs. Never reuse a Web DTO in a Messaging listener. 
- Adapter Level (Syntactic): Perform "cheap" checks (nulls, empty strings, regex) using the tools native to that adapter (e.g., Jakarta Validation for Web/Messaging).
- Shared Constraints: If a validation rule (like a complex Regex) is used across multiple adapters, extract it into a Domain Value Object and let the constructor handle the validation. This avoids duplicating the Regex logic in every DTO.
- Prefer Java Records for all DTOs. Leverage Record Patterns in controllers for clean data extraction. Java classes are permitted as well.
    
## APIs development

- When developing on APIs, adhere to RESTful principles.
- Use appropriate HTTP methods (GET, POST, PUT, DELETE) based on the operation.
- Ensure proper status codes are returned for different scenarios (for example, 200 OK, 201 Created, 400 Bad Request, 404 Not Found, 500 Internal Server Error).
- Paginated responses have 200 OK HTTP code when successfully retrieved.
- Controllers should only handle orchestration and validation; all business logic belongs in domain Service classes.
- Use RFC 7807 (Problem Details) for all API errors. Define a consistent error response structure and map domain exceptions to appropriate HTTP status codes in a centralized `@ControllerAdvice` class.
- Use DTOs for all API inputs and outputs. Never expose Domain entities directly in API.


## JPA / Hibernate

- Do not rely on Hibernate to create/update the schema (`ddl-auto: none`). All schema changes must be done via Flyway migrations.
- Keep domain models and persistence models separate: avoid leaking JPA entities outside persistence adapters.
- Prefer `UUID` identifiers and align `@Table`/`@Column` names with the actual Flyway-created schema (snake_case in the database).
- Prefer `LAZY` relationships by default; avoid `EAGER` unless you have a proven reason.
- Avoid `N+1` queries: use purpose-built repository queries, `JOIN FETCH`, or `@EntityGraph` where appropriate.
- Put transaction boundaries at the service/application layer: use `@Transactional` and `@Transactional(readOnly = true)` for reads.
- Be careful with `equals`/`hashCode` on entities (Hibernate proxies). Avoid Lombok `@Data` on entities.


