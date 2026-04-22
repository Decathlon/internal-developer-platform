---
title: Code Architecture Overview
description: Understanding the architecture and design principles of IDP-Core
---

IDP Core is a Spring Boot app built following the principles of the **Hexagonal Architecture**.

## Key Technologies

- **Spring Boot 4**
- **Spring Security**
- **Spring Data JPA** & **PostgreSQL**
- **Docker** & **Testcontainers library**
- **Flyway**

## Sections

<div class="grid cards" markdown>

- 🏢 [**Domain & Infrastructure**](domain-infrastructure.md)

    Separation of concerns and folder structure.

- ⚠️ [**Exception Handling**](exception-handling.md)

    Global strategy and error response formats.

- ✅ [**Validations**](domain-model-validations.md)

    DTO vs Entity validation rules.

- 🎨 [**Code Conventions**](code-conventions.md)

    Coding standards and style guide.

- ⭐ [**Best Practices**](best-practices.md)

    Checklist for architecture, DB, and security.

</div>

## Architecture Principles

We strictly separate the **Domain** (Business Logic) from the **Infrastructure** (Technical concerns).

### Hexagonal Architecture

```mermaid
flowchart LR
    A[Infrastructure - Input Ports] --> B[Domain - Core]
    B --> C[Infrastructure - Output Ports]

    subgraph A[Infrastructure - Input Ports]
        A1[REST API]
        A2[Controllers]
        A3[DTOs]
    end

    subgraph B[Domain - Core]
        B1[Entities]
        B2[Services]
        B3[Exceptions]
    end

    subgraph C[Infrastructure - Output Ports]
        C1[Database]
        C2[Repositories]
        C3[External APIs]
    end
```

### Core Rules

1. **Dependency Rule**: Infrastructure depends on Domain. Domain depends on **nothing**.
2. **Testing**: Domain is unit-tested. Infrastructure is integration-tested in addition.
