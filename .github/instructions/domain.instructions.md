---
description: 'Guidelines for modifying Domain layer'
applyTo: '**/domain/*.java'
---

# Domain Layer related instructions

## General Instructions

- Place business logic in `@Service` annotated classes.
- Services should be stateless and testable.
- Always use constructor injection. Field injection (@Autowired on fields) is strictly forbidden.
- Lightweight Spring Dependency: It is permissible to use Spring Stereotype annotations (@Service, @Component) within the Domain layer to reduce configuration boilerplate.
- Business Language (Ubiquitous Language): Use naming conventions that reflect the business domain, not technical implementation.
- Domain Exceptions: Create specific, checked or unchecked exceptions for business rule violations (for example: EntityNotFoundException).
- No Web/Network Metadata: Domain exceptions must not contain HTTP status codes or REST-specific information.
- Self-Validating Records: Perform basic domain validation using Jakarat validation annotations to ensure the domain object is never in an invalid state.
- Domain Services: Only create a DomainService when a piece of logic involves multiple aggregates or doesn't naturally belong to a single Entity.
- Aggregates and Entities: Use standard classes for Entities that have a lifecycle and identity. Ensure all state changes happen through well-defined business methods.
- Inbound Data: Input ports should accept domain-specific objects or simple primitives.

## Mapping

- Prohibited when mapping: Never use ObjectMapper or reflection-based libraries for internal layer mapping.
- Standard Mapping: Use MapStruct for straightforward conversion between JPA entities and Domain objects.
- Complex Mapping: Use Record Patterns for manual mapping when business logic or complex transformations are required during conversion.

## Validation

- Intrinsic Validation: Enforce domain invariants within the class itself. For Records, use compact constructors to validate data upon instantiation. For Entities, validate state transitions within business methods.
- Always Valid Pattern: Design domain objects so they cannot be instantiated in an invalid state. Throw custom DomainValidationException (or a similar unchecked domain exception) when rules are violated.
- Validation Messages: Use constants for validation messages to ensure consistency and maintainability. Store these in a dedicated class (for example: `ValidationsMessages.java`) within the domain layer.