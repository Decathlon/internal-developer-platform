---
title: Exception Handling Strategy
description: Global exception handling strategy and RFC 9457 "Problem Details" error responses for IDP-Core
---

## Strategy

1. **Domain**: Throw specific business exceptions, for example `EntityTemplateNotFoundException`.
2. **Infrastructure**: Centralized handling in `ApiExceptionHandler` via the `STATUS_BY_EXCEPTION` map.
3. **API**: Consistent RFC 9457 "Problem Details" JSON error responses (RFC 9457, formerly RFC 7807).

## Exception Mapping

To add support for a new domain exception:

1. Create your domain exception class in `src/main/java/com/decathlon/idp_core/domain/exception/{category}/`.
2. Add one entry to `ApiExceptionHandler.STATUS_BY_EXCEPTION`:

```java
entry(MyNewException.class, BAD_REQUEST),  // or other HttpStatus
```

The exception will automatically be caught, logged, and returned as a `ProblemDetail` with the mapped HTTP status. **All domain exceptions must be registered**, unregistered exceptions silently degrade to HTTP 500.

## Error Response Format

API responses always follow RFC 9457 "Problem Details for HTTP APIs":

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Template identifier cannot be changed"
}
```

**Content-Type:** `application/problem+json`

### Response Fields

| Field | Meaning |
| --- | --- |
| `type` | URI reference to documentation (currently `about:blank`; future enhancement to include machine-readable error codes) |
| `title` | Human-readable HTTP status name |
| `status` | HTTP status code |
| `detail` | Human-readable error message from the domain exception |

## Common HTTP Status Mappings

| Status | Meaning | Domain Exception Examples |
| --- | --- | --- |
| 400 Bad Request | Malformed input or business rule violation client can fix | `InvalidFilterDslException`, `EntityValidationException`, `PropertyNameAlreadyExistsException` |
| 401 Unauthorized | Credentials could not be verified | `WebhookAuthenticationException` |
| 404 Not Found | Resource does not exist | `EntityTemplateNotFoundException`, `EntityNotFoundException` |
| 409 Conflict | Request conflicts with resource state | `EntityTemplateAlreadyExistsException`, `EntityDeletionBlockedException` |
| 422 Unprocessable Content | Syntactically valid but cannot be processed | `ExpressionEvaluationFailedException`, `EntityDynamicMappingJsltErrorException` |
| 500 Internal Error | Unexpected error (not registered in map) | (any exception not in `STATUS_BY_EXCEPTION` map) |
