---
title: Exception Handling Strategy
description: Global exception handling strategy and error response formats for IDP-Core
---

## Strategy

1. **Domain**: Throw specific business exceptions, for example `EntityTemplateNotFoundException`.
2. **Infrastructure**: Catch exceptions in `ApiExceptionHandler` with `@RestControllerAdvice`.
3. **API**: Return RFC 7807 `ProblemDetail` responses with legacy extension fields.

## Exception Mapping

| Exception Type                         | HTTP Status        | Description                  |
| -------------------------------------- | ------------------ | ---------------------------- |
| `EntityTemplateNotFoundException`      | 404 Not Found      | Entity not found             |
| `EntityTemplateAlreadyExistsException` | 409 Conflict       | Duplicate identifier         |
| `ConstraintViolationException`         | 400 Bad Request    | Validation failed            |
| `MethodArgumentNotValidException`      | 400 Bad Request    | Invalid request body         |
| `HttpMessageNotReadableException`      | 400 Bad Request    | Framework JSON parsing error |
| `Exception`                            | 500 Internal Error | Unexpected error             |

## Error Response Format

The API now returns `application/problem+json` responses Standard RFC 7807 fields are
present alongside legacy compatibility fields :

```json
{
  "type": "about:blank",
  "title": "Not Found",
  "status": 404,
  "detail": "Template with ID 'invalid-id' not found",
  "instance": "/api/v1/entity-templates/invalid-id",
  "error": "NOT_FOUND",
  "error_description": "Template with ID 'invalid-id' not found",
  "timestamp": "2025-11-28T10:30:00Z"
}
```

The `error` and `error_description` fields preserve the legacy contract for existing
clients. `timestamp` is included as an `Instant` for traceability.

> [!WARNING]
> Strict clients may reject the new media type or the extra extension fields. They should
> be updated to parse `application/problem+json` and tolerate RFC 7807 extensions.

### Intentional differences

- Spring MVC validation and JSON parsing errors now use the framework's native problem detail handling.
- The manual JSON parsing logic was removed, so some malformed payload messages may differ from the previous custom wording.

### Migration note

Communicate the response shape change to API consumers before they depend on the new contract.
Keep compatibility tests in place while clients migrate, and verify both the standard
ProblemDetail fields and the legacy extension properties.
