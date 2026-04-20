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

- Define all external interactions as **Port interfaces** in the `domain/ports/` package.
- Ports accept and return **domain models or simple primitives only**, never DTOs, JPA entities, or framework types.
- Port interfaces define the **contract** between domain and infrastructure. They contain no implementation details.
- Name ports with a `Port` suffix (for example, `EntityRepositoryPort`, `EntityTemplateRepositoryPort`).

## Exceptions

- Create specific unchecked exceptions for business rule violations (for example, `EntityTemplateNotFoundException`, `EntityTemplateAlreadyExistsException`).
- Domain exceptions must **not** contain HTTP status codes or REST-specific information.
- Map domain exceptions to HTTP status codes exclusively in the Infrastructure layer (`@ControllerAdvice`).

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

## Mapping

- Never use `ObjectMapper` or reflection-based libraries for internal layer mapping.
- Use MapStruct for straightforward conversion between JPA entities and domain objects.
- Use Record Patterns for manual mapping when business logic or complex transformations are required.

## Package Structure

```text
domain/
├── constant/            # Validation message constants
├── exception/           # Domain-specific exceptions
├── model/
│   ├── entity/          # Core business records
│   ├── entity_template/ # Template records
│   └── enums/           # Business enums
├── ports/               # Port interfaces (contracts for driven adapters)
└── service/             # Domain services (orchestration)
```
