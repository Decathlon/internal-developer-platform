# ADR-0002: Code Architecture Pattern

* Status: Proposed
* Deciders:
  * Maintainers team: Andrés BRAND, Matthieu WALTERSPIELER, Eve BERNHARD, Ferial OUKOUKES, Renny VANDOMBER
  * Contributors team: `N/A`
* Consulted: Étienne JACQUOT
* Informed: `N/A`
* Date: 2026-04-10

## Context and Problem Statement

The IDP Core is responsible for ingesting, transforming, and persisting data from a variety of external sources. Traditional Layered (MVC) architectures often lead to business logic becoming intertwined with technical details (database schemas, specific message formats, and external API quirks). We need an architecture that protects our core domain logic—specifically the entity mapping and validation—while remaining flexible enough to handle various ingress methods and future outbound events in an open-source ecosystem.

## Decision Drivers

* Security
* User experience
* Complexity of implementation
* Maintainability
* Developer experience and velocity
* Open Source contributing

## Considered Options

1. **Pragmatic Hexagonal Architecture (Ports and Adapters with Spring annotations)**
2. **Traditional MVC (Layered) Architecture**
3. **Strict Hexagonal Architecture (Pure Ports and Adapters)**

## Decision Outcome

Chosen option: option 1, **"Pragmatic Hexagonal Architecture,"** because it provides the best balance between structural integrity and development speed. By isolating the domain while permitting Spring Boot 4 dependencies, we protect our business logic without the "religious" overhead of manual dependency injection. This structure specifically supports our open-source goals by providing clear "plugs" (Adapters) for community contributions.

### Positive Consequences

* **Isolated Domain Logic:** Core mapping and validation rules are contained in a central "Domain," making the system easier to reason about.
* **Fast Feedback Loops:** Domain logic can be tested without starting heavy infrastructure (databases or brokers).
* **Simplified Infrastructure Swaps:** We can add or change ingestion methods (Webhooks, Messaging, CLI) by simply adding new Adapters without touching the Domain.
* **Developer Productivity:** Using Spring annotations in the Domain avoids the significant boilerplate associated with "Pure" Hexagonal Architecture.
* **Community-Led Scalability:** The "Adapter" model allows the community to extend the project's capabilities (new sources/destinations) without increasing the complexity of the core engine.
* **Contributor Confidence:** High testability of the Domain layer ensures that external PRs are less likely to introduce regressions.
* **Vendor Agnosticism:** Users are not "locked in" to specific infrastructure choices, facilitating wider adoption.

### Negative Consequences

* **Framework Coupling:** The Domain is technically coupled to Spring Boot 4, making it harder to move to a different framework.
* **Increased Initial Setup:** Requires more packages and interfaces (Ports) upfront compared to a flat MVC structure.

---

## Pros and Cons of the Options

### 1. Pragmatic Hexagonal Architecture (Ports and Adapters)

This version keeps the "Port and Adapter" split but allows `@Service`, `@Transactional` in the Domain.

```text
com.decathlon.idp
├── domain/                         <-- The "Inside" (Pure Logic)
│   ├── model/
│   │   ├── Entity.java             # Logic: validateInternal()
│   │   └── EntityTemplate.java     # Metadata: JQ scripts, etc.
│   ├── ports/                      # The "Plugs" (Interfaces)
│   │   ├── EntityRepository.java   # Driven Port
│   │   └── EventPublisher.java     # Driven Port
│   └── service/
│       └── IngestionService.java   # The "Chef": Orchestrates the Story
│
└── infrastructure/                 <-- The "Outside" (Tools/Adapters)
    ├── adapters/                             # Implementations of Ports
    │   ├── api/
    │   │   ├── EntityController.java         # Driving Adapter (Kafka/Webhooks)
    │   │   └── EntityTemplateController.java # Metadata: JQ scripts, etc.
    │   ├── persistence/
    │   │   └── PostgresAdapter.java          # Implementation of EntityRepository
    │   └── messaging/
    │       └── KafkaEventAdapter.java        # Implementation of EventPublisher
    └── config/                               # Spring Boot @Configuration
```

* Good, because it provides a **Unified Ingestion Port**: Every adapter maps its specific technical payload into the same Domain Model before calling an `IngestionService`. (Maintainability / Contributing in open source mode)
* Good, because it isolates **Protocol Translation**: Mapping technical formats into Domain Entities happens strictly in Adapters. (Complexity of implementation)
* Good, because it **Decouples Throughput from Logic**: High-volume sources (Kafka) and low-latency sources (Webhooks) can scale independently at the adapter level using Java 25 Virtual Threads. (FinOps / Complexity of implementation)
* Good, because it facilitates **"Driven Ports" for Event Emission**: Downstream notifications (Kafka, SNS) are defined as ports, keeping the Domain focused on the *intent* of the event rather than the delivery. (Complexity of implementation / User experience)
* Good, because it enables **"Dry Run" Testing of Side Effects**: We can verify event emission by mocking the outbound port in unit tests without a live message broker. (Testability)
* Good, because it enables **"Pluggability" for OS Users**: Community members can contribute new Inbound or Outbound adapters with minimal friction. (Contributing in open source mode)
* Good, because the **Folder Structure is the Map**: The split between `domain` (The Why) and `infrastructure` (The How) acts as a visual guide for external developers. (Contributing in open source mode)
* Bad, because of **"Conceptual Overhead"**: Not every contributor is familiar with Hexagonal Architecture; clear documentation is required. (Contributing in open source mode / Complexity of implementation)
* Bad, because it requires **"Double Mapping"**: We must often map from an "Incoming DTO" to a "Domain Entity." (Complexity of implementation)
* Bad, because it creates **Package Fragmentation**: Navigating between `domain.ports`, `domain.services`, and `infrastructure.adapters`. (Complexity of implementation)

### 2. Traditional MVC (Layered) Architecture

```text
  com.decathlon.idp
  ├── api/                            # Controllers for Webhooks
  ├── messaging/                      # Kafka Consumers & Producers
  ├── service/
  │   ├── IngestionService.java       # "God Class": Does mapping, logic, and saving
  │   └── ValidationService.java      # Specific logic usually called by the IngestionService
  ├── repository/
  │   └── EntityRepository.java       # Direct Spring Data JPA (tightly coupled to DB schema)
  ├── model/
  │   ├── SoftwareEntity.java         # POJO / Entity with Getters & Setters
  │   └── Blueprint.java              # Database representation of a Blueprint
  └── config/                         # Centralized Spring & Framework configuration
```

* Good, because of the **"Lowest Common Denominator"**: Almost every Java developer on GitHub understands MVC. (Contributing in open source mode)
* Good, because it is **Extremely Fast to Scaffold**: Fewer files and no interfaces required for internal calls. (Developer Velocity)
* Bad, because of **Leaky Abstractions**: Infrastructure details (SQL types, REST DTOs) inevitably migrate into the Service layer. (Maintainability)
* Bad, because it lacks **"Contribution Guardrails"**: No structural rule prevents a contributor from putting Kafka-specific logic directly into a Service class. (Contributing in open source mode / Maintainability)
* Bad, because it **Couples Business Logic to Outbound Protocols**: Calling a `KafkaTemplate` directly in a service locks the project into Kafka, forcing community forks for other brokers. (Contributing in open source mode / Portability)
* Bad, because it creates **Brittle Tests**: Business logic tests become dependent on infrastructure mocks. (Testability)

### 3. Strict Hexagonal Architecture (Pure Ports and Adapters)

This version enforces a pristine Domain layer with absolutely zero dependencies on external frameworks, including Spring Boot. All dependency injection, transaction management, and configuration must be handled explicitly in the Infrastructure layer.

```text
com.decathlon.idp
├── domain/                         <-- Pure Java, ZERO framework imports
│   ├── model/
│   │   ├── Entity.java
│   │   └── EntityTemplate.java
│   ├── ports/
│   │   ├── EntityRepository.java
│   │   └── EventPublisher.java
│   └── service/
│       └── IngestionService.java   # Pure Java class, no @Service or @Transactional
│
└── infrastructure/                 <-- All Frameworks (Spring Boot, etc.)
    ├── adapters/
    │   ├── api/
    │   ├── persistence/
    │   └── messaging/
    └── config/                     # Manual bean wiring for Domain classes
```

* Good, because of **Absolute Framework Independence**: The core logic is pristine Java, meaning migrating away from Spring Boot to Quarkus, Micronaut, or Jakarta EE requires absolutely zero changes to the domain. (Portability / Future-Proofing)
* Good, because of **Maximum Testability**: Domain tests run instantly as pure JUnit tests without any need for Spring Context, component scanning, or framework mocking. (Testability)
* Good, because of **Enforced Boundaries**: It is structurally impossible to leak framework-specific logic (like Spring Data or Spring Web annotations) into the business logic. (Maintainability)
* Bad, because of **High Boilerplate**: Requires manual Bean configuration in the infrastructure layer to instantiate domain services and manually inject infrastructure adapters. (Complexity of implementation)
* Bad, because of **Developer Friction**: Developers cannot use well-known and highly productive conveniences like `@Transactional` or `@Service` in their core logic, slowing down development velocity. (Developer Velocity)
* Bad, because of **Transaction Management Complexity**: Handling transaction boundaries programmatically across pure domain boundaries without framework annotations is tedious and prone to errors. (Complexity of implementation)
* Bad, because of **Onboarding Difficulty**: The stark absence of standard Spring conventions in the core logic might alienate typical Java/Spring engineers and increase the learning curve for open-source contributors. (Contributing in open source mode)

---

## More Information

### The "Pragmatic" Boundaries

1. **No External Library Imports:** The Domain may use Spring libraries, but it may NOT import third-party integration libraries.
2. **Ports as Contracts:** Every interaction with the "Outside World" must go through a Port interface.
