---
name: implement-audit-logging
description: 'Implement audit tracking and history endpoints for an existing domain object using Hibernate Envers.'
---

# Implement Audit Logging

Implement Hibernate Envers audit logging and a history retrieval endpoint for an existing domain object within the Hexagonal Architecture.

This skill leverages the native Envers entity tracking (`track_entities_changed_in_revision: true`) and relies on the global `envers_transaction_log` without creating custom tracking tables.

## Inputs

- **EntityName**: `${input:EntityName}`—The name of the domain object (for example `Property`, `EntityTemplate`).
- **IdentifierType**: `${input:IdentifierType}`—The primary identifier type of the object (for example `UUID`, `String`).

## Input Validation

If the entity name or identifier type cannot be determined from the conversation, ask the user before proceeding. Verify that the target JPA entity exists in the `infrastructure.adapters.persistence.model` package before attempting any modifications.

## Requirements

- **Architecture**: Strict adherence to Hexagonal Architecture (Domain, Ports, Adapters).
- **Audit Framework**: Hibernate Envers with native tracking. Do not manually map elements into `revinfo` or custom tracking tables.
- **DTO Naming**: Use Jackson `@JsonNaming(SnakeCaseStrategy.class)` for all API outputs.

## Workflow

### 1. JPA Entity Update

Locate the target `[EntityName]JpaEntity` in `infrastructure/adapters/persistence/model/`.

- Add the `org.hibernate.envers.Audited` annotation to the class.
- Apply `@NotAudited` only to lazy relationships or fields that should strictly not trigger audit records.

### 2. Flyway Migration Generation

Create a new migration script in `src/main/resources/db/migration/`.

- Generate SQL to create a table named `[entity_name]_aud`.
- Include the base audit columns: `id` (or primary key), `rev` (`BIGINT NOT NULL`), and `revtype` (`SMALLINT`).
- Add the foreign key constraint: `CONSTRAINT fk_[entity_name]_aud_revinfo FOREIGN KEY (rev) REFERENCES envers_transaction_log (rev) ON DELETE CASCADE`.
- Replicate the audited columns from the base table.

### 3. Domain Port

Create an interface `[EntityName]AuditPort` in `domain/port/audit/`.

- Define the retrieval method: `List<[EntityName]AuditInfo> getAuditHistory([IdentifierType] id);`.

### 4. Persistence Adapter

Create `Postgres[EntityName]AuditAdapter` in `infrastructure/adapters/persistence/` implementing the port.

- Use `AuditReaderFactory.get(entityManager).createQuery().forRevisionsOfEntity([EntityName]JpaEntity.class, false, true)` to fetch revisions.
- Map the resulting `Object[]` containing the Entity snapshot, `CustomRevisionEntity`, and `RevisionType` to the domain `AuditInfo` model.
- Map `CustomRevisionEntity.getAuthId()` to the `modifiedBy` field.

### 5. Domain Service

Create `[EntityName]AuditService` in `domain/service/[entity_name]/`.

- Annotate with `@Transactional(readOnly = true)`.
- Inject the port and implement the retrieval method, adding necessary business logic or template validations.

### 6. API Adapter & DTOs

Create `[EntityName]AuditDtoOut` in `infrastructure/adapters/api/dto/out/`.

- Annotate with `@Data`, `@Builder`, and `@JsonNaming(SnakeCaseStrategy.class)`. Include fields: `revisionNumber`, `revisionDate`, `revisionType`, `modifiedBy`, and `snapshot`.
- Create or update the `AuditController` to define `@GetMapping("entities/[object-plural]/{id}")`.
- Apply Swagger annotations (`@Operation`, `@ApiResponse`).

## Validation

- Verify that no manual entity tracking code was added.
- Ensure the Flyway migration syntax is strictly correct for PostgreSQL.
- Check that the API outputs map correctly to `SnakeCaseStrategy`.
