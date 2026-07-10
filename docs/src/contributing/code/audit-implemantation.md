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
@Audited(withModifiedFlag=true) // <--- Add this annotation to track changes and flag column modified
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

### Collection Type Considerations for Auditing

When your entity contains collections (`@ElementCollection` or `@OneToMany`), choose the appropriate collection type:

**Use `Set` (Recommended):**

```java
@ElementCollection(fetch = FetchType.EAGER)
@CollectionTable(name = "related_items", joinColumns = @JoinColumn(name = "parent_id"))
@Audited(withModifiedFlag = true)
private Set<RelatedItemEntity> relatedItems;  // Order doesn't matter
```

**Use `List` (Only When Order Matters):**

```java
@ElementCollection(fetch = FetchType.EAGER)
@CollectionTable(name = "ordered_items", joinColumns = @JoinColumn(name = "parent_id"))
@OrderColumn(name = "item_order")  // <--- Explicitly track order
@Audited(withModifiedFlag = true)
private List<OrderedItemEntity> orderedItems;  // Order is significant
```

**Key Difference in Audit Schema:**

- **Set**: Composite PK is `(parent_id, rev, item_id)` - simpler
- **List**: Composite PK is `(parent_id, rev, item_order)` - adds order tracking column

See Infrastructure Layer instructions for complete Set vs List guidance and when to use each type.

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

## Step 4: Add/Update Domain Service and REST Endpoint

Depending on the domain design, you can either reuse existing files to minimize boilerplate, or create dedicated audit files for clean separation of concerns.

1. **Service:** Create `PropertyAuditService` to handle business logic (like ensuring the parent object exists before querying history).
2. **Controller:** Expose the endpoint, using standard DTOs formatted with Jackson's `SnakeCaseStrategy`.

### Approach 1: Reusing Existing Structures (Recommended for simple resources)

If you already have a PropertyService and a PropertyController, simply append the new functionality directly to them to keep things concise.

1. **Service:** In PropertyService: Inject the PropertyAuditPort and add a getPropertyHistory(UUID id) method.
2. **Controller:** In PropertyController: Expose a nested route following clean RESTful guidelines.

```java
// Within your existing PropertyController.java
@GetMapping("/{propertyId}/history")
public List<PropertyAuditDtoOut> getPropertyHistory(@PathVariable UUID propertyId) {
return auditMapper.fromDomainList(propertyService.getPropertyHistory(propertyId));
}
```

### Approach 2: Creating Dedicated Audit Handlers (Recommended for complex auditing rules)

If retrieving audit details requires dedicated permissions, complex filtering, or distinct business rules, decouple them into dedicated files.

1. **Service:** Establish a PropertyAuditService to guarantee domain invariant verification (for example verifying object access permissions before compiling historical records).
2. **Controller:** Build a focused controller parsing outputs cleanly with Jackson's SnakeCaseStrategy.

```java
package com.company.project.infrastructure.adapters.api;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

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
