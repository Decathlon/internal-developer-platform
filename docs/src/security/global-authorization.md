---
title: Global Authorization
description: Managing administrators and break-glass access in Phase 1
---

IDP-Core uses global authorization in Phase 1. Every authenticated principal has
the `READER` role by default. Full access is granted only when the principal
entity has the `is_admin` property set to `true`.

## Authorization tiers

| Principal | Access |
| --- | --- |
| Principal with `is_admin=true` | Full CRUD access |
| Principal without `is_admin=true` | Read-only access (`READER`) |
| Unauthenticated request | 401 Unauthorized |

## Promoting an administrator

Set the administrator flag through the entities API:

```bash
curl -X PATCH http://localhost:8084/api/v1/entities/principal/{user-uuid} \
  -H "Authorization: Bearer <admin-token>" \
  -H "Content-Type: application/json" \
  -d '{"properties":[{"name":"is_admin","value":"true"}]}'
```

Principal records may be created by JIT authentication or the user ingestion
webhook. JIT creates a stable UUID-based identifier and defaults `is_admin` to
`false`; webhook updates are authoritative for the display name and properties.

## Break-glass access

Configure emergency administrator identifiers with environment variables:

```yaml
app:
  security:
    authorization:
      mode: GLOBAL
      global-principal-identifiers:
        - ${IDP_SUPER_ADMIN_UUID:}
        - ${IDP_PLATFORM_ADMIN_UUID:}
```
