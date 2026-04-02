---
title: Best Practices
description: Architecture, database, security, and testing best practices for IDP-Core
---

## 🏗️ Architecture

* **Dependency Rule**: Domain depends on nothing.
* **Interface Segregation**: Keep interfaces focused, for example `EntityTemplateRepository` vs generic `Repository`.
* **Exceptions**: Throw business exceptions in Domain, handle them in Infrastructure.

## 💾 Database

* **Migrations**: Always use Flyway. Never modify existing migration files, they are cumulative and immutable.
* **JPA**: Use `FetchType.LAZY` by default. Use `JOIN FETCH` for performance only where necessary.

## 🔒 Security

* **Validation**: Validate at DTO level (`@Valid`) and Database level (`unique=true`).

## 🧪 Testing

* **Data**: Use dedicated JSON files for test data.
* **Builders**: Use Builder pattern for creating test objects.
