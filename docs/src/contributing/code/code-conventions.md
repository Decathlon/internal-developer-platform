---
title: Code Conventions
description: Coding standards and style guide for IDP-Core
---

Following consistent coding conventions ensures readable, maintainable code. This guide covers all standards for IDP-Core development.

## General Principles

1. **Readability** - Developers read code more than they write it
2. **Simplicity** - Prefer simple solutions
3. **Consistency** - Follow existing patterns
4. **Testability** - Write testable code

---

## Java Conventions

### Naming

| Element    | Convention        | Example                  |
| ---------- | ----------------- | ------------------------ |
| Classes    | PascalCase        | `EntityTemplate`         |
| Interfaces | PascalCase        | `EntityRepository`       |
| Methods    | camelCase pattern | `findById()`             |
| Variables  | camelCase pattern | `entityCount`            |
| Constants  | SCREAMING_SNAKE   | `MAX_PAGE_SIZE`          |
| Packages   | lowercase         | `com.decathlon.idp_core` |

### Records for Data Transfer Objects

Use Java records for immutable data transfer objects:

```java
// ✅ Good: Record for command
public record CreateEntityTemplateCommand(
    String identifier,
    String title,
    String description,
    List<PropertyDefinitionRequest> properties
) {}

// ✅ Good: Record for response
public record EntityTemplateResponse(
    UUID id,
    String identifier,
    String title,
    Instant createdAt
) {}
```

### Optional Usage

```java
// ✅ Good: Return Optional for nullable results
public Optional<EntityTemplate> findByIdentifier(String identifier) {
    return repository.findByIdentifier(identifier);
}

// ✅ Good: Handle Optional properly
EntityTemplate template = findByIdentifier(id)
    .orElseThrow(() -> new NotFoundException(id));

// ❌ Bad: Optional as parameter
public void process(Optional<String> filter) { }  // Don't do this

// ❌ Bad: Optional.get() without check
template = findByIdentifier(id).get();  // Dangerous!
```

### Null Safety

```java
// ✅ Good: Validate inputs
public EntityTemplate create(CreateCommand command) {
    Objects.requireNonNull(command, "command must not be null");
    Objects.requireNonNull(command.identifier(), "identifier must not be null");
    // ...
}

// ✅ Good: Use annotations
public @NonNull EntityTemplate findById(@NonNull EntityTemplateId id) {
    // ...
}
```

---

## Exception Handling

### Domain Exceptions

```java
// Base domain exception
public abstract class DomainException extends RuntimeException {
    private final String code;

    protected DomainException(String code, String message) {
        super(message);
        this.code = code;
    }
}

// Specific exceptions
public class EntityNotFoundException extends DomainException {
    public EntityNotFoundException(EntityId id) {
        super("ENTITY_NOT_FOUND", "Entity with id '%s' not found".formatted(id));
    }
}

public class InvalidIdentifierException extends DomainException {
    public InvalidIdentifierException(String identifier) {
        super("INVALID_IDENTIFIER", "Identifier '%s' is invalid".formatted(identifier));
    }
}
```

### Global Exception Handler

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(EntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        var errors = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .toList();
        return ResponseEntity.badRequest()
            .body(new ErrorResponse("VALIDATION_ERROR", String.join(", ", errors)));
    }
}
```

---

## Code Formatting

### Spotless Configuration

Code formatting is enforced by Spotless. Run before committing:

```bash
mvn spotless:apply
```

### Import Order

```java
// Standard library
import java.util.List;
import java.util.Map;

// Third-party
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

// Project
import com.decathlon.idp_core.domain.model.EntityTemplate;
```

### Line Length

- Maximum 120 characters
- Break long lines at logical points

```java
// ✅ Good: Break at parameters
public EntityTemplate create(
        String identifier,
        String title,
        List<PropertyDefinition> properties) {
    // ...
}

// ✅ Good: Break at method chain
return entities.stream()
    .filter(e -> e.isActive())
    .map(this::toResponse)
    .collect(toList());
```

---

## Documentation

### Java Documentation

```java
/**
 * Creates a new entity template with the given specification.
 *
 * <p>The identifier must be unique across all templates and follow
 * the pattern: lowercase letters, numbers, hyphens, and underscores.
 *
 * @param command the creation command containing template details
 * @return the created entity template
 * @throws DuplicateIdentifierException if identifier already exists
 * @throws InvalidIdentifierException if identifier format is invalid
 */
public EntityTemplate create(CreateCommand command) {
    // ...
}
```

### When to Document

- ✅ Public API methods
- ✅ Complex business logic
- ✅ Non-obvious implementations
- ❌ Obvious getters/setters
- ❌ Self-explanatory code

---

## Next Steps

- **[Testing](../testing.md)** - Testing conventions
- **[Pull Requests](../pull-requests.md)** - PR guidelines
