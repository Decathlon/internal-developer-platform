---
description: 'Guidelines for building Java base applications'
applyTo: '**/*.java'
---

# Java Development

## General Instructions

- Use Java 25 syntax and features.
- Address code smells proactively during development rather than accumulating technical debt.
- Focus on readability, maintainability, and performance when refactoring identified issues.
- Use IDE and code editor warnings and suggestions to catch common patterns early in development.
- Add JavaDoc comments for all public classes and methods to enhance code understandability.
- Follow the Java Language Specification and standard conventions for code style and formatting.

## Best practices

- **Records**: For classes primarily intended to store data (for example, Data Transfer Objects, immutable data structures), **Java Records should be used instead of traditional classes**.
- **Pattern Matching**: Utilize pattern matching for `instanceof` and `switch` expression to simplify conditional logic and type casting.
- **Type Inference**: Use `var` for local variable declarations to improve readability, but only when the type is explicitly clear from the right-hand side of the expression.
- **Immutability**: Favor immutable objects. Make classes and fields `final` where possible. Use collections from `List.of()`/`Map.of()` for fixed data. Use `Stream.toList()` to create immutable lists.
- **Streams and Lambdas**: Use the Streams API and lambda expressions for collection processing. Employ method references (for example, `stream.map(Foo::toBar)`).
- **Null Handling**: Avoid returning or accepting `null`. Use `Optional<T>` for possibly absent values and `Objects` utility methods like `equals()` and `requireNonNull()`.

### Naming Conventions

- Follow Google's Java style guide:
  - `UpperCamelCase` for class and interface names.
  - `lowerCamelCase` for method and variable names.
  - `UPPER_SNAKE_CASE` for constants.
  - `lowercase` for package names.
- Use nouns for classes (`UserService`) and verbs for methods (`getUserById`).
- Avoid abbreviations and Hungarian notation.

### Common Bug Patterns

- Resource management—Always close resources (files, sockets, streams). Use try-with-resources where possible so resources are closed automatically.
- Equality checks—Compare object equality with `.equals()` or `Objects.equals(...)` rather than `==` for non-primitives; this avoids reference-equality bugs.
- Redundant casts—Remove unnecessary casts; prefer correct generic typing and let the compiler infer types where possible.
- Reachable conditions—Avoid conditional expressions that are always true or false; they indicate bugs or dead code and should be corrected.

### Common Code Smells

- Parameter count—Keep method parameter lists short. If a method needs many parameters, consider grouping into a value object or using the builder pattern.
- Method size—Keep methods focused and small. Extract helper methods to improve readability and testability.
- Cognitive complexity—Reduce nested conditionals and heavy branching by extracting methods, using polymorphism, or applying the Strategy pattern.
- Duplicated literals—Extract repeated strings and numbers into named constants or enumerations to reduce errors and ease changes.
- Dead code—Remove unused variables and assignments. They confuse readers and can hide bugs.
- Magic numbers—Replace numeric literals with named constants that explain intent (for example, MAX_RETRIES).

## Build and Verification

- After adding or modifying code, verify the project continues to build successfully by running `mvn clean verify`.
- Ensure all tests pass as part of the build.

## Persistence (JPA)

- For JPA/Hibernate specifics (transactions, entity mapping, fetch strategy), follow `springboot.instructions.md`.
- For schema changes, follow `database.instructions.md` and use Flyway migrations.

## Testing

- You need to reach at least 80% code coverage with unit tests.
