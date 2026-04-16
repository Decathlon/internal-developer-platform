# Domain-Infrastructure Separation (Hexagonal Architecture)

## The Golden Rule

**The Domain Layer must never depend on the Infrastructure Layer.**
Dependencies always point inward: Adapters → Ports → Domain.

## Architecture Overview

This project follows **Hexagonal Architecture** (Ports & Adapters):

- **Domain** Pure business logic, no framework dependencies
- **Ports** Interfaces that define the contracts between the domain and the outside world
- **Adapters** Concrete implementations that connect the domain to external systems (REST API, database, etc.)

```text
┌─────────────────────────────────────────────────────┐
│                   Infrastructure                     │
│  ┌──────────────┐                ┌────────────────┐  │
│  │  API Adapter  │               │  Persistence   │  │
│  │  (Driving)    │               │  Adapter       │  │
│  │  Controller ──┤               │  (Driven)      │  │
│  │  DTO in/out   │    ┌──────┐   │  JPA Entity    │  │
│  │  Mapper       ├───▶│Domain│◀──┤  Mapper        │  │
│  │  Handler      │    │      │   │  Repository    │  │
│  └──────────────┘    │Ports ◀┤   └────────────────┘  │
│                      │Service│                        │
│                      │Model  │                        │
│                      └──────┘                         │
└─────────────────────────────────────────────────────┘
```

---

## Domain Layer (`com.decathlon.idp_core.domain`)

Pure Java implementation — **no Spring annotations, no framework imports**.
Contains all business rules, invariants, and contracts.

### 📂 Domain Structure

```text
domain/
├── constant/            # Validation message constants (single source of truth)
├── exception/           # Domain-specific exceptions (EntityTemplateNotFoundException)
├── model/
│   ├── entity/          # Core business records: Entity, Property, Relation, EntitySummary
│   ├── entity_template/ # Template records: EntityTemplate, PropertyDefinition, PropertyRules, RelationDefinition
│   └── enums/           # Business enums: PropertyType, PropertyFormat
├── ports/               # Port interfaces—contracts for driven adapters
│   ├── EntityRepositoryPort
│   ├── EntityTemplateRepositoryPort
│   └── RelationRepositoryPort
└── service/             # Domain services — business logic orchestration
    ├── EntityService
    ├── EntityTemplateService
    └── RelationService
```

### Domain Layer Responsibilities

| Concern | Responsibility |
| --- | --- |
| **Models** | Immutable Java records enforcing business invariants via validation annotations |
| **Ports** | Interfaces defining the **contract** between domain and infrastructure. No implementation details |
| **Services** | Orchestrate business operations, enforce rules, delegate persistence to ports |
| **Exceptions** | Domain-specific error types. Protocol-agnostic (mapped to HTTP status codes by adapters) |
| **Constants** | Centralized validation messages used by model annotations and exception formatting |

### Key Constraints

- **No Spring annotations** in models, ports, or exceptions
- Services may use `@Service` solely for DI — no other framework coupling
- Domain exceptions carry business meaning, not HTTP semantics

---

## Infrastructure Layer (`com.decathlon.idp_core.infrastructure`)

Everything technical: Spring Boot, REST, database, security, serialization.
Organized as **adapters** that implement or drive the domain ports.

### 📂 Infrastructure Structure

```text
infrastructure/
└── adapters/
    ├── api/                          # Driving adapter (inbound — REST API)
    │   ├── configuration/            # Spring Config, Security, Swagger, JWT, CORS
    │   │   ├── JwtConfiguration
    │   │   ├── SecurityConfiguration
    │   │   ├── SpringDataWebConfiguration
    │   │   ├── SwaggerConfiguration
    │   │   ├── SwaggerDescription
    │   │   └── WebConfiguration
    │   ├── controller/               # REST controllers — thin entry points
    │   │   ├── EntityController
    │   │   └── EntityTemplateController
    │   ├── dto/
    │   │   ├── in/                   # Request DTOs (input from client)
    │   │   └── out/                  # Response DTOs (output to client)
    │   ├── handler/                  # Global exception → HTTP status mapping
    │   │   └── ApiExceptionHandler
    │   └── mapper/                   # DTO ↔ Domain mapping
    │       ├── entity/               # EntityDtoInMapper, EntityDtoOutMapper
    │       └── entitytemplate/       # EntityTemplateMapper
    │
    └── persistence/                  # Driven adapter (outbound — database)
        ├── PostgresEntityAdapter             # Implements EntityRepositoryPort
        ├── PostgresEntityTemplateAdapter     # Implements EntityTemplateRepositoryPort
        ├── PostgresRelationAdapter           # Implements RelationRepositoryPort
        ├── mapper/                   # Domain ↔ JPA entity mapping
        │   ├── EntityPersistenceMapper
        │   └── EntityTemplatePersistenceMapper
        ├── model/                    # JPA entities (database representation)
        │   ├── entity/
        │   └── entity_template/
        └── repository/               # Spring Data JPA interfaces
            ├── JpaEntityRepository
            ├── JpaEntityTemplateRepository
            └── JpaRelationRepository
```

### Infrastructure Layer Responsibilities

#### API Adapter (Driving — Inbound)

| Concern | Responsibility |
| --- | --- |
| **Controllers** | Receive HTTP requests, delegate to domain services, return DTOs. **No business logic.** |
| **DTOs** | Define the API contract shape (request/response). Carry validation annotations. |
| **Mappers** | Convert between DTOs and domain models. Null-safe, no side effects. |
| **Handler** | Map domain exceptions to HTTP status codes (`EntityTemplateNotFoundException` → 404) |
| **Configuration** | Security (JWT, CORS), Swagger/OpenAPI, pagination, serialization settings |

#### Persistence Adapter (Driven—Outbound)

| Concern | Responsibility |
| --- | --- |
| **Adapter classes** | Implement domain port interfaces using JPA repositories and persistence mappers |
| **JPA Entities** | Database representation—may differ from domain models ( mutable, JPA annotations) |
| **Persistence Mappers** | Convert between domain records and JPA entities |
| **JPA Repositories** | Spring Data interfaces for database queries |

---

## Data Flow

### Read (GET request)

```text
HTTP Request
  → Controller (validates params)
    → Domain Service (applies business rules)
      → Port interface
        → Persistence Adapter (JPA query → domain mapping)
    ← Domain model
  ← DTO Mapper → HTTP Response
```

### Write (POST/PUT request)

```text
HTTP Request + JSON body
  → Controller (DTO deserialization + Bean Validation)
    → DTO-to-Domain Mapper
      → Domain Service (enforces invariants, uniqueness check)
        → Port interface
          → Persistence Adapter (domain → JPA mapping → save)
      ← Domain model
    ← Domain-to-DTO Mapper
  ← HTTP Response (201/200)
```

### Error Flow

```text
Domain exception thrown (EntityTemplateAlreadyExistsException)
  → ApiExceptionHandler catches
    → Maps to HTTP status (409 Conflict) + ErrorResponse DTO
  ← JSON error response
```

---

## Dependency Rules

| From | May depend on | Must NOT depend on |
| --- | --- | --- |
| Domain models | Java standard library only | Spring, JPA, infrastructure |
| Domain ports | Domain models | Any implementation |
| Domain services | Ports + models + exceptions | Adapters, Spring (except `@Service`) |
| API adapter | Domain services + ports + models | Persistence adapter |
| Persistence adapter | Domain ports + models | API adapter |
