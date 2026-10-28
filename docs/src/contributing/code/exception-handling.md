---
title: Exception Handling Strategy
description: Global exception handling strategy and error response formats for IDP-Core
---

## Strategy

1. **Domain**: Throw specific business exceptions, for example `EntityTemplateNotFoundException`.
2. **Infrastructure**: Catch exceptions in `ApiExceptionHandler` with `@ControllerAdvice` annotation.
3. **API**: Return consistent JSON error responses.

## Exception Mapping

| Exception Type                         | HTTP Status        | Description          |
| -------------------------------------- | ------------------ | -------------------- |
| `EntityTemplateNotFoundException`      | 404 Not Found      | Entity not found     |
| `EntityTemplateAlreadyExistsException` | 409 Conflict       | Duplicate identifier |
| `ConstraintViolationException`         | 400 Bad Request    | Validation failed    |
| `MethodArgumentNotValidException`      | 400 Bad Request    | Invalid request body |
| `HttpMessageNotReadableException`      | 400 Bad Request    | JSON parsing error   |
| `Exception`                            | 500 Internal Error | Unexpected error     |

## Error Response Format

At API level, always follow the same error response structure, including errors written directly by security filters:

```json
{
  "error": "NOT_FOUND",
  "error_description": "Template with ID 'invalid-id' not found"
}
```
