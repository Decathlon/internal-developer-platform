---
title: Architecture Tests
description: How ArchUnit guards the hexagonal boundaries and prevents maintainability issues in IDP-Core.
---

IDP-Core enforces its architectural guidelines automatically with [ArchUnit](https://www.archunit.org/). The tests fail the build when code drifts from the architecture decided in [ADR-0002](../adrs/0002-code-architecture-pattern.md).

## What the Tests Enforce

The rules live in two classes under `src/test/java/com/decathlon/idp_core/architecture/`. `HexagonalArchitectureTest` guards the boundaries defined by our hexagonal architecture. `MaintainabilityArchitectureTest` prevents cyclic dependencies and enforces general code conventions (dependency injection, logging, exceptions, naming).

### Hexagonal Constraints

| Rule | Scope |
| ---- | ----- |
| `DOMAIN_MUST_NOT_DEPEND_ON_INFRASTRUCTURE` | The domain never depends on `infrastructure`. |
| `DOMAIN_MUST_NOT_DEPEND_ON_FORBIDDEN_TECHNOLOGY` | The domain never depends on JPA, Spring Web, Spring Data JPA/repositories, Hibernate, MapStruct, JSLT, Jackson, Kafka, or HTTP clients. |
| `DOMAIN_MUST_NOT_DEPEND_ON_JPA_ENTITIES` | The domain never depends on `*JpaEntity` persistence types. |
| `LAYER_DEPENDENCIES_POINT_INWARD` | `infrastructure` may depend on `domain`, never the reverse. |
| `PORTS_MUST_BE_INTERFACES` | Every class in `domain.port` is an interface. |
| `PORT_NAMED_CLASSES_MUST_LIVE_IN_PORT_PACKAGE` | Every `*Port` contract lives in `domain.port`. |
| `PORTS_IMPLEMENTED_ONLY_IN_ADAPTERS` | A port is implemented only by an `infrastructure.adapters` class. |
| `DTOS_RESIDE_IN_API_DTO_PACKAGE` | Every `*Dto`, `*DtoIn`, `*DtoOut` lives in `adapters.api.dto`. |
| `CONTROLLERS_MUST_NOT_ACCESS_PERSISTENCE` | Controllers never depend on the persistence adapter. |
| `TRANSACTIONAL_NOT_ON_CONTROLLERS` | Controller classes are never `@Transactional`. |
| `TRANSACTIONAL_METHODS_NOT_ON_CONTROLLERS` | Controller methods are never `@Transactional`. |

The domain may use the Java standard library, Jakarta Validation, and Spring stereotype/transaction annotations. The Spring Data pagination value types (`Pageable`, `Page`, `Sort`, `PageRequest`) are also allowed as a pragmatic abstraction the domain exposes on its ports.

### Maintainability Conventions

| Rule | Scope |
| ---- | ----- |
| `FREE_OF_CYCLES` | No package cycles across the whole application. |
| `DOMAIN_FREE_OF_CYCLES` | No cycles between `domain` sub-packages. |
| `ADAPTERS_FREE_OF_CYCLES` | No cycles between `infrastructure.adapters` sub-packages. |
| `NO_FIELD_INJECTION` | Beans use constructor injection, never field/`@Value`/setter injection. |
| `NO_STANDARD_STREAMS` | No `System.out`/`System.err`/`printStackTrace`; use the SLF4J logger. |
| `NO_JAVA_UTIL_LOGGING` | No `java.util.logging`; use SLF4J. |
| `DOMAIN_MUST_NOT_LOG` | The domain never depends on a logging framework. |
| `RUNTIME_EXCEPTIONS_ARE_NAMED_EXCEPTION` | Every runtime exception type is named `*Exception`. |
| `EXCEPTIONS_LIVE_IN_DOMAIN_EXCEPTION_PACKAGE` | Every `*Exception` lives in `domain.exception`. |
| `SERVICES_LIVE_IN_DOMAIN_SERVICE_PACKAGE` | Every `*Service` lives in `domain.service`. |
| `CONTROLLERS_LIVE_IN_API_CONTROLLER_PACKAGE` | Every `*Controller` lives in `adapters.api.controller`. |
| `MAPPERS_LIVE_IN_MAPPER_PACKAGE` | Every `*Mapper` lives in a `mapper` package. |

## Run the Tests

The rules run automatically inside `mvn clean verify`. To run only the architecture tests:

```bash
mvn test -Dtest=HexagonalArchitectureTest,MaintainabilityArchitectureTest
```

## Read a Failure

Each rule carries a `because(...)` clause, so a failure names the offending class and the reason. For example, importing a JPA annotation into a domain class fails with:

```text
Rule 'no classes that reside in a package '..domain..' should depend on classes
that reside in any package ['jakarta.persistence..', ...]' was violated (1 times):
Class <...domain.model.enums.PropertyType> depends on <jakarta.persistence.Entity>
```

To fix a violation, move the technical dependency into an adapter under `infrastructure` and interact with the domain through a port.

---

## Next Steps

- **[Domain-Infrastructure Separation](domain-infrastructure.md)**
- **[ADR-0002 Code Architecture Pattern](../adrs/0002-code-architecture-pattern.md)**
