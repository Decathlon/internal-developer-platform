# Domain-Infrastructure Separation

## The Golden Rule

**The Domain Layer must never depend on the Infrastructure Layer.**

## Domain Layer Details

Pure Java implementation without Spring annotations for basic domain logic.

### 📂 Structure

```text
domain/
├── configuration/       # Configuration classes for domain-specific settings
├── constant/            # Constants used across the domain layer
├── exception/           # Custom exceptions for domain-specific errors
├── model/               # Domain models representing core business entities
├── service/             # Domain services containing business logic
└── repository/          # Port interfaces defining repository contracts
```

## Infrastructure Layer (`com.decathlon.idp_core.infrastructure`)

Everything technical: spring Boot, database, REST, external APIs.

### 📂 Infrastructure Structure

```text
infrastructure/
├── configuration/      # Spring Config, Security, Swagger
├── controller/                 # rest Controllers, API endpoints
├── dto/                        # Data Transfer Objects for mapping
├── handler/         # Global exception handlers
└── mapper/       # Object mappers for converting between Domain and DTOs
```

### Data Flow

1. **Controller**: Receives DTO, validates, and maps to Domain.
2. **Service**: Executes Business Logic.
3. **Repository**: Persists Domain Entity via JPA implementation.
