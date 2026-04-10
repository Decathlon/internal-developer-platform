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

* **Maintainability:** Clear separation between business rules and infrastructure logic.
* **Testability:** High confidence in core logic through fast, isolated unit tests.
* **Developer Velocity (Pragmatism):** Reducing boilerplate by allowing Spring Boot 4's dependency injection and transaction management within the domain.
* **Future-Proofing:** Decoupling the "standardization engine" from specific protocols (Kafka, HTTP, etc.) and allowing for future side effects (outbound events).
* **Contributing in Open Source Mode:** Ensuring the codebase is modular and "inviting" for external contributors to add new adapters without risking core stability.

## Considered Options

1. **Pragmatic Hexagonal Architecture (Ports and Adapters with Spring annotations)**
2. **Traditional MVC (Layered) Architecture**

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

This version keeps the "Port and Adapter" split but allows `@Service`, `@Transactional`, and `@Autowired` in the Domain.

* Good, because it provides a **Unified Ingestion Port**: Every adapter maps its specific technical payload into the same Domain Model before calling an `IngestionService`. (Maintainability / Contributing in open source mode)
* Good, because it isolates **Protocol Translation**: Mapping technical formats (JSON, Avro, Protobuf) into Domain Entities happens strictly in Adapters. (Complexity of implementation)
* Good, because it **Decouples Throughput from Logic**: High-volume sources (Kafka) and low-latency sources (Webhooks) can scale independently at the adapter level using Java 25 Virtual Threads. (FinOps / Complexity of implementation)
* Good, because it facilitates **"Driven Ports" for Event Emission**: Downstream notifications (Kafka, SNS) are defined as ports, keeping the Domain focused on the *intent* of the event rather than the delivery. (Complexity of implementation / User experience)
* Good, because it enables **"Dry Run" Testing of Side Effects**: We can verify event emission by mocking the outbound port in unit tests without a live message broker. (Testability)
* Good, because it enables **"Pluggability" for OS Users**: Community members can contribute new Inbound or Outbound adapters with minimal friction. (Contributing in open source mode)
* Good, because the **Folder Structure is the Map**: The split between `domain` (The Why) and `infrastructure` (The How) acts as a visual guide for external developers. (Contributing in open source mode)
* Bad, because of **"Conceptual Overhead"**: Not every contributor is familiar with Hexagonal Architecture; clear documentation is required. (Contributing in open source mode / Complexity of implementation)
* Bad, because it requires **"Double Mapping"**: We must often map from an "Incoming DTO" to a "Domain Entity." (Complexity of implementation)
* Bad, because it creates **Package Fragmentation**: Navigating between `domain.ports`, `domain.services`, and `infrastructure.adapters`. (Complexity of implementation)

### 2. Traditional MVC (Layered) Architecture

* Good, because of the **"Lowest Common Denominator"**: Almost every Java developer on GitHub understands MVC. (Contributing in open source mode)
* Good, because it is **Extremely Fast to Scaffold**: Fewer files and no interfaces required for internal calls. (Developer Velocity)
* Bad, because of **Leaky Abstractions**: Infrastructure details (SQL types, REST DTOs) inevitably migrate into the Service layer. (Maintainability)
* Bad, because it lacks **"Contribution Guardrails"**: No structural rule prevents a contributor from putting Kafka-specific logic directly into a Service class. (Contributing in open source mode / Maintainability)
* Bad, because it **Couples Business Logic to Outbound Protocols**: Calling a `KafkaTemplate` directly in a service locks the project into Kafka, forcing community forks for other brokers. (Contributing in open source mode / Portability)
* Bad, because it creates **Brittle Tests**: Business logic tests become dependent on infrastructure mocks. (Testability)

---

## More Information

### The "Pragmatic" Boundaries
1. **No External Library Imports:** The Domain may use Spring libraries, but it may NOT import third-party integration libraries (e.g., specific Kafka clients or mapping engines like Jackson-JQ).
2. **Ports as Contracts:** Every interaction with the "Outside World" must go through a Port interface.

