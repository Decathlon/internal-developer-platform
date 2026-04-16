---
description: 'Guidelines for building Spring Boot base applications'
applyTo: '**/*.java'
---

# Spring Boot Development

## General Instructions

- Make only high confidence suggestions when reviewing code changes.
- Write code with good maintainability practices, including comments on why certain design decisions were made.
- Handle edge cases and write clear exception handling.
- For libraries or external dependencies, mention their usage and purpose in comments.
- Use the newest stable version of Spring Boot unless there is a specific reason not to.

## Related Instructions

- For Java language and style rules, also follow `java.instructions.md`.
- For schema changes, also follow `database.instructions.md` and use Flyway migrations (no Hibernate auto-DDL).

## Spring Boot Instructions

### Dependency Injection

- Use constructor injection for all required dependencies.
- Declare dependency fields as `private final`.

### Configuration

- Use YAML files (`application.yml`) for externalized configuration.
- Environment Profiles: Use Spring profiles for different environments (local, test, prod)
- Configuration Properties: Use @ConfigurationProperties for type-safe configuration binding
- Secrets Management: Externalize secrets using environment variables or secret management systems

### Code Organization

- Package Structure: Organize rather by layer and functionality
- Separation of Concerns: Keep controllers thin, services focused, and repositories simple
- Utility Classes: Make utility classes final with private constructors
           
### Observability

- Use OpenTelemetry for tracing and metrics.
- Use the Opentelemetry Spring Boot Starter for easy integration.
- Ensure all significant operations are traced, including external calls and database interactions.
- Avoid logging too much, use it judiciously to capture **really** important events and errors.

### Testing

- Prefer RestTestClient over MockMvc: For all integration tests involving controllers and API endpoints, use RestTestClient. It provides a more readable, fluent API and better aligns with the RestClient used in production code.
- Fluent Assertions: Leverage the .expectStatus(), .expectHeader(), and .expectBody() chains for clear, declarative assertions.
- Record Mapping: Use .expectBody(RecordName.class) to automatically deserialize responses into Java Records, avoiding manual JSON path expressions where possible.
- Virtual Thread Validation: Ensure tests are run with spring.threads.virtual.enabled=true to verify that controllers behave correctly under the Virtual Thread executor.

### Security & Input Handling

- Use parameterized queries. Always use Spring Data JPA or `NamedParameterJdbcTemplate` to prevent SQL injection.
- Validate request bodies and parameters using JSR-380 (`@NotNull`, `@Size`, etc.) annotations and `BindingResult`

### Profiles

- Use Spring Profiles to separate the concurrent features that lead to scaling issues. For example, set up different profiles for API servers, batch processors, and workers.
- Annotate beans with `@Profile("profileName")` to load them conditionally based on the active profile
- Common profiles: `api`, `batch-xxx`, `worker-xxx`, `test`, `local`

## Build and Verification

- After adding or modifying code, verify the project continues to build successfully.
- The project uses Maven, run `mvn clean verify`.
- Ensure all tests pass as part of the build.

## Useful Commands

| Maven Command                  | Description                                  |
|:-------------------------------|:---------------------------------------------|
| `mvn spring-boot:run`          | Run the app.                                 |
| `mvn clean verify`             | Build and run all tests and checks.          |
| `mvn test`                     | Run unit tests only.                         |