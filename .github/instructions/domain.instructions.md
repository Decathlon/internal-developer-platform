---
description: 'Guidelines for modifying the Domain layer'
applyTo: '**/domain/**/*.java'
---

# Domain Layer Instructions

## General Principles

- The Domain layer contains **all business logic, invariants, and contracts**.
- Dependencies always point inward: the Domain **must never** depend on the Infrastructure layer.
- The Domain may depend only on the Java standard library, Jakarta Validation, and Spring stereotype annotations (`@Service`, `@Transactional`).
- **No JPA, Kafka, Security, or HTTP** imports are permitted in this layer.

## Services

- Place business logic in `@Service`-annotated classes.
- Services must be stateless and testable.
- Always use constructor injection. Field injection (`@Autowired` on fields) is strictly forbidden.
- Use `@Transactional` for write operations and `@Transactional(readOnly = true)` for reads.
- Only create a Domain Service when logic involves multiple aggregates or does not naturally belong to a single entity.

## Models

- Use **Java Records** for all domain models to enforce immutability.
- Records must be self-validating using Jakarta Validation annotations (`@NotNull`, `@NotBlank`, `@Size`).
- Design domain objects so they **cannot be instantiated in an invalid state** (Always Valid pattern).
- Use compact constructors to enforce invariants that go beyond annotation-based validation.
- Domain models carry **business meaning only**. No HTTP semantics, no persistence metadata.

## Ports

- Define all external interactions as **Port interfaces** in the `domain/port/` package.
- Ports accept and return **domain models or simple primitives only**, never DTOs, JPA entities, or framework types.
- Port interfaces define the **contract** between domain and infrastructure. They contain no implementation details.
- Name ports with a `Port` suffix (for example, `EntityRepositoryPort`, `EntityTemplateRepositoryPort`).

## Exceptions

### General Rules

- Create **specific unchecked exceptions** for each business rule violation (for example, `EntityTemplateNotFoundException`, `EntityAlreadyExistsException`).
- Domain exceptions must **not** contain HTTP status codes or REST-specific information.
- Map domain exceptions to HTTP status codes exclusively in the Infrastructure layer (`@ControllerAdvice`).

### Exception Clarity

- **Always prefer specific exceptions over generic ones**. Never throw `IllegalArgumentException` or `IllegalStateException` for business rule violations.
- Exception names must describe **what went wrong** from a business perspective (for example, `EntityTemplateNotFoundException`, not `TemplateException`).
- Exception messages must include **context**: what entity, what identifier, what operation was attempted.

### Validation Service Pattern

When a service method needs to validate preconditions (for example, "entity template must exist before creating entity"):

1. **Extract validation into a dedicated service** (for example, `EntityTemplateValidationService`)
2. **Use explicit method names** that describe the validation (for example, `validateTemplateExists`, `validateTemplateNotExists`)
3. **Throw specific exceptions** that carry business meaning (for example, `EntityTemplateNotFoundException`)
4. **Call validation first** (fail-fast) before executing the main operation

**Benefits:**

- **Clear error messages**: `EntityTemplateNotFoundException("web-service")` vs generic `IllegalArgumentException("Invalid template")`
- **Better HTTP mapping**: specific exceptions map to appropriate status codes (404 for not found, 409 for conflict)
- **Reusable validation**: multiple services can call `validateTemplateExists` without duplicating logic
- **Fail-fast**: validation happens before expensive operations (database queries, graph traversal)

### Exception Naming Convention

| Pattern                           | Example                                 | When to Use                    |
| --------------------------------- | --------------------------------------- | ------------------------------ |
| `<Entity>NotFoundException`       | `EntityTemplateNotFoundException`       | Resource doesn't exist (404)   |
| `<Entity>AlreadyExistsException`  | `EntityTemplateAlreadyExistsException`  | Duplicate key violation (409)  |
| `<Entity>ValidationException`     | `PropertyValidationException`           | Business rule violation (400)  |
| `<Operation>NotAllowedException`  | `EntityDeletionNotAllowedException`     | Operation forbidden (403/409)  |

### Exception Structure

```java
public class EntityTemplateNotFoundException extends RuntimeException {

  private final String identifier;

  public EntityTemplateNotFoundException(String identifier) {
    super(String.format("Entity template with identifier '%s' not found", identifier));
    this.identifier = identifier;
  }

  public String getIdentifier() {
    return identifier;
  }
}
```

**Rules:**

- Extend `RuntimeException` (unchecked) for business exceptions
- Include a formatted message with all relevant context
- Store identifiers/keys as fields if needed for logging or error responses
- Never include stack traces in exception messages

## Constants

- Use a dedicated constants class (for example, `ValidationMessages.java`) for all validation messages.
- Store constants in the `domain/constant/` package as a single source of truth.
- Reference these constants from model validation annotations and exception formatting.

## Naming

- Use **Ubiquitous Language**: naming conventions must reflect the business domain, not technical implementation.
- Use nouns for models (`EntityTemplate`, `PropertyDefinition`) and verbs for service methods (`putEntityTemplate`, `validateEntity`).

## Validation

- **Intrinsic Validation**: enforce domain invariants within the class itself.
- **Adapter-Level vs. Domain-Level**: syntactic checks (nulls, empty strings) belong on DTOs in the Infrastructure layer. Semantic checks (uniqueness, cross-field rules) belong in Domain Services.
- Throw a custom `DomainValidationException` (or similar unchecked exception) when rules are violated.

### Creating Validation Services

When validation logic is reused across multiple domain services:

1. **Create a dedicated validation service** (for example, `EntityTemplateValidationService`)
2. **Extract validation methods** with clear names: `validateTemplateExists`, `validateTemplateNotExists`, `validateTemplateNotReferenced`
3. **Always call validation first** before the main operation (fail-fast principle)

**Example validation service:**

```java
@Service
@RequiredArgsConstructor
public class EntityTemplateValidationService {

  private final EntityTemplateRepositoryPort repository;

  public void validateTemplateExists(String identifier) {
    if (!repository.existsByIdentifier(identifier)) {
      throw new EntityTemplateNotFoundException(identifier);
    }
  }

  public void validateTemplateNotExists(String identifier) {
    if (repository.existsByIdentifier(identifier)) {
      throw new EntityTemplateAlreadyExistsException(identifier);
    }
  }

  public void validateTemplateNotReferenced(String identifier) {
    if (repository.hasEntities(identifier)) {
      throw new EntityTemplateReferencedException(identifier,
          "Cannot delete template that is referenced by entities");
    }
  }
}
```

**Usage (fail-fast):**

```java
@Service
@RequiredArgsConstructor
public class EntityService {

  private final EntityTemplateValidationService templateValidation;
  private final EntityRepositoryPort entityRepository;

  @Transactional
  public Entity createEntity(String templateIdentifier, String entityIdentifier, ...) {
    // Validate template exists FIRST (fail-fast)
    templateValidation.validateTemplateExists(templateIdentifier);

    // Validate entity doesn't already exist
    if (entityRepository.existsByIdentifier(entityIdentifier)) {
      throw new EntityAlreadyExistsException(entityIdentifier);
    }

    // Main operation
    Entity entity = new Entity(...);
    return entityRepository.save(entity);
  }
}
```

## Mapping

- Never use `ObjectMapper` or reflection-based libraries for internal layer mapping.
- Use MapStruct for straightforward conversion between JPA entities and domain objects.
- Use Record Patterns for manual mapping when business logic or complex transformations are required.

## Package Structure

```text
domain/
├── constant/            # Validation message constants
├── exception/           # Domain-specific exceptions
│   ├── entity/          # Entity-related exceptions
│   ├── entity_template/ # Template-related exceptions
│   └── property/        # Property-related exceptions│
├── model/
│   ├── entity/          # Core business records
│   ├── entity_template/ # Template records
│   └── enums/           # Business enums
├── port/                # Port interfaces (contracts for driven adapters)
└── service/             # Domain services (orchestration)
    ├── entity/          # Entity services
    ├── entity_template/ # Template validation services
    └── entity_graph/    # Graph services
```

### Exception Package Organization

- Organize exceptions by aggregate/subdomain (for example, `entity/`, `entity_template/`, `property/`)
- Each exception class should have a clear, descriptive name that follows the naming conventions above
- Keep exception hierarchy flat — avoid deep inheritance trees
