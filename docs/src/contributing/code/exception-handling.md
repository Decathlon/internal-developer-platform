---
title: Exception Handling Strategy
description: Global exception handling strategy and RFC 9457 "Problem Details" error responses for IDP-Core
---

IDP-Core returns RFC 9457 Problem Details for API errors and retains the former
error fields to ease consumer migration.

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

The exception will automatically be caught, logged, and returned as a
`ProblemDetail` with the mapped HTTP status. Register every known domain
exception; unregistered exceptions are logged and returned as generic HTTP 500
errors.

## Error Response Format

API errors handled by `ApiExceptionHandler`, servlet security filters, and
webhook ingestion follow RFC 9457 "Problem Details for HTTP APIs":

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Template identifier cannot be changed",
  "instance": "/api/v1/entity-templates",
  "error": "BAD_REQUEST",
  "error_description": "Template identifier cannot be changed",
  "timestamp": "2026-11-23T10:48:00Z"
}
```

**Content-Type:** `application/problem+json`

### Response Fields

| Field | Meaning |
| --- | --- |
| `type` | URI reference to problem documentation; currently `about:blank` |
| `title` | Human-readable HTTP status name |
| `status` | HTTP status code |
| `detail` | Human-readable explanation of the problem |
| `instance` | Request path that produced the error |
| `error` | Legacy code; webhook responses retain their webhook-specific values |
| `error_description` | Legacy error message, equal to `detail` |
| `timestamp` | Time the error response was created, in ISO 8601 format |

The `error`, `error_description`, and `timestamp` fields remain temporarily for
backward compatibility. The response also includes the RFC 9457 fields, so
clients must tolerate additional fields and should migrate to `type`, `title`,
`status`, `detail`, and `instance`.

## Common HTTP Status Mappings

| Status | Meaning | Domain Exception Examples |
| --- | --- | --- |
| 400 Bad Request | Malformed input or business rule violation client can fix | `InvalidFilterDslException`, `EntityValidationException`, `PropertyNameAlreadyExistsException` |
| 401 Unauthorized | Credentials could not be verified | `WebhookAuthenticationException`, `MissingPrincipalIdentifierException` |
| 403 Forbidden | Identity is valid but cannot perform the operation | `PrincipalNotAuthorizedException` |
| 404 Not Found | Resource does not exist | `EntityTemplateNotFoundException`, `EntityNotFoundException` |
| 409 Conflict | Request conflicts with resource state | `EntityTemplateAlreadyExistsException`, `EntityDeletionBlockedException` |
| 422 Unprocessable Content | Syntactically valid but cannot be processed | `ExpressionEvaluationFailedException`, `EntityDynamicMappingJsltErrorException` |
| 500 Internal Error | Unexpected error (not registered in map) | (any exception not in `STATUS_BY_EXCEPTION` map) |
| 503 Service Unavailable | Principal verification could not be completed | Temporary JIT provisioning failures |
