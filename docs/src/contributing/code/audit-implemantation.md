# Adding Audit Logging to Domain Objects

This guide explains how to integrate our Hibernate Envers audit logging architecture for a new or existing domain object, and how to expose an endpoint to retrieve its history. This is part of our Hexagonal Architecture and ensures we keep a strict separation between database tracking and domain logic.

## Architecture Overview

We use **Hibernate Envers** with native entity tracking enabled (`track_entities_changed_in_revision: true`).

- **Global Transaction Log:** `envers_transaction_log` tracks the revision number, timestamp, and the user's `auth_id`.
- **Modified Entities Log:** `envers_modified_entities` natively tracks which JPA entity classes were modified in a given transaction.
- **Entity-Specific Audit Tables:** (for example `entity_aud`, `property_aud`). These store the actual state snapshots of the modified rows.

---

## Step 1: Add `@Audited` to the JPA Entity

Navigate to your JPA Entity class (for example `PropertyJpaEntity`) in the `infrastructure.adapters.persistence.model` package.

```java
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

@Entity
@Table(name = "property")
@Audited // <--- Add this annotation to track changes
public class PropertyJpaEntity {

    @Id
    private UUID id;

    private String name;

    // Use @NotAudited on fields or lazy relationships you DO NOT want to track
    @NotAudited
    @OneToMany(mappedBy = "property")
    private Set<ChildEntityJpaEntity> children;
}
```

## Step 2: Create the Flyway Migration

Create a new Flyway migration script (for example `V4_2__audit_property.sql`) in `src/main/resources/db/migration/` to define the specific `_aud` table for your entity.

**Rules for Audit Tables:**

1. Suffix the table name with `_aud` (for example `property_aud`).
2. Add a `rev` column (`BIGINT NOT NULL`).
3. Add a `revtype` column (`SMALLINT`) to track the operation (0=ADD, 1=MOD, 2=DEL).
4. Replicate the fields from the base table that you are auditing.
5. Make the Primary Key a composite of the entity's ID and `rev`.
6. Add the Foreign Key linking to `envers_transaction_log`.

```sql
CREATE TABLE property_aud
(
    id      UUID   NOT NULL,
    rev     BIGINT NOT NULL,
    revtype SMALLINT,
    name    VARCHAR(255),
    value   VARCHAR(255),
    PRIMARY KEY (id, rev),
    CONSTRAINT fk_property_aud_revinfo FOREIGN KEY (rev) REFERENCES envers_transaction_log (rev) ON DELETE CASCADE
);

CREATE INDEX idx_property_aud_rev ON property_aud (rev);
```

## Step 3: Implement the Domain Port and Persistence Adapter

Create your port in the domain (for example `PropertyAuditPort`), then implement the adapter using Hibernate Envers' `AuditReader`.

```java
@Component
@RequiredArgsConstructor
public class PostgresPropertyAuditAdapter implements PropertyAuditPort {

  private final EntityManager entityManager;

  @Override
  public List<AuditInfo> getPropertyAuditHistory(UUID propertyId) {
    AuditReader auditReader = AuditReaderFactory.get(entityManager);

    // Query Envers for the specific entity class
    @SuppressWarnings("unchecked")
    List<Object[]> revisions = auditReader.createQuery()
        .forRevisionsOfEntity(PropertyJpaEntity.class, false, true)
        .add(AuditEntity.id().eq(propertyId))
        .addOrder(AuditEntity.revisionNumber().desc())
        .getResultList();

    return revisions.stream().map(this::mapToDomainAuditInfo).toList();
  }

  private AuditInfo mapToDomainAuditInfo(Object[] revision) {
      PropertyJpaEntity snapshot = (PropertyJpaEntity) revision[0];
      CustomRevisionEntity revEntity = (CustomRevisionEntity) revision[1];
      RevisionType revType = (RevisionType) revision[2];

      // Map to your domain record...
  }
}
```

## Step 4: Add Domain Service and REST Endpoint

Finally, orchestrate the retrieval through a Domain Service and expose it via your API adapter.

1. **Service:** Create `PropertyAuditService` to handle business logic (like ensuring the parent object exists before querying history).
2. **Controller:** Expose the endpoint, using standard DTOs formatted with Jackson's `SnakeCaseStrategy`.

```java
@RestController
@RequestMapping("/api/v1/audit/")
@RequiredArgsConstructor
public class PropertyAuditController {

  private final PropertyAuditService auditService;
  private final PropertyAuditDtoOutMapper mapper;

  @GetMapping("properties/{propertyId}")
  public List<PropertyAuditDtoOut> getPropertyAuditHistory(@PathVariable UUID propertyId) {
    return mapper.fromDomainList(auditService.getHistory(propertyId));
  }
}
```
