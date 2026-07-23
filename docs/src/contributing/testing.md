---
title: Testing Strategy
description: Unit and Integration testing strategy for IDP-Core
---

## 🧪 Testing Pyramid

### 1. Unit Tests (Fast)

* **Scope**: Domain logic, Services, Mappers.
* **Tool**: JUnit 5 testing framework with Mockito library.
* **Rule**: Mock all external dependencies (Repositories).

### 2. Integration Tests (Reliable)

* **Scope**: Controllers, Repositories, Database constraints.
* **Tool**: Testcontainers library for PostgreSQL database.
* **Rule**: Spin up a real database. Test the full HTTP request/response cycle.

## Test Categories

* **API Tests**: Verify HTTP status codes and JSON bodies.
* **Domain Tests**: Verify business rules and invariants.
* **Security Tests**: Verify auth/authz and API vulnerabilities (DAST).

### Security Tests (DAST)

* **Scope**: API security scanning, vulnerability detection.
* **Tool**: OWASP ZAP via Testcontainers for Dynamic Application Security Testing.
* **Rule**: Run on demand via the `/run-owasp-zap` PR comment or `workflow_dispatch` (not part of the default test suite).

→ See **[OWASP ZAP Security Scanning](owasp-zap-security.md)** for complete details.
