# 0004 - Domain Specific Language (DSL) for Dynamic Mapping implementation

* Status: Accepted
* Deciders:
  * maintainers team: Andrés BRAND, Matthieu WALTERSPIELER, Eve BERNHARD, Ferial OUKOUKES, Renny VANDOMBER
  * contributors team: `N/A`
* Consulted: Étienne JACQUOT
* Informed: `N/A`
* Date: 2026-04-27

## Context and Problem Statement

Mapping source objects to a target object is a central functionality that enables some IDP features like data connectors, calculated properties and Scorecard definitions among others. That’s why we need to do a prior analysis in order to implement, what we call, a mapping engine. This engine must allow dynamic mapping based on a user given query. This query should be user friendly and standardized.

Dynamic mapping allows an application to change how it interprets incoming data without a recompilation or redeployment. This is typically achieved by using a Domain Specific Language (DSL) stored in a database or configuration file. Given that the IDP-Core is a SpringBoot application, in this study we will discuss two main DSL; JSLT and JQ.

## Decision Drivers

* Security
* User experience
* Complexity of implementation
* FinOps
* Maintainability
* Modularity
* Readability
* Extensibility
* Scalability

## Decision Outcome

Chosen option: option 1, **"JSLT (JSON Standard Transformation Language),"** because JSLT provides a capable DSL that meets our requirements for modularity, maintainability, scalability and extensibility. Its explicit use of helper functions for complex logic, while more verbose than JQ, results in clearer, more maintainable scripts and better long-term debugging.

### Positive Consequences

* **Modular Logic:** Explicit helper functions enhance script modularity and long-term maintainability.
* **Reusable Transformations:** Logic is easily reused across multiple mappings via defined helpers, supporting extensibility.
* **Open Source Friendly:** The DSL approach allows contributors to extend mapping logic without modifying core Java code.

### Negative Consequences

* **Learning Curve:** JSLT is less common than JQ; contributors and final users may need time to learn its XSLT-inspired syntax.
* **Verbosity:** Simple mappings require more lines of code than equivalent JQ expressions.
* **Complex Use Cases:** Certain scenarios (complex grouping/aggregation, cross-referencing arrays, recursive structures, date/time math, dynamic key generation) remain difficult or unreadable, potentially requiring custom Java functions.

## Considered Options

1. **Use the JSLT (JSON Standard Transformation Language) Java library**
2. **Use the Jackson-JQ Java Library**

## Pros and Cons of the Options

### 1. Use JSLT (JSON Standard Transformation Language) Java library

JSLT is a JSON transformation language inspired by XSLT, designed for transforming JSON data using a declarative, functional approach. It supports variables, functions, conditionals, and modular script organization. JSLT uses the pipe operator (`|`) for default values and relies on `if...else` blocks, often inside custom helper functions, for conditional logic. Is used for Data ingestion pipelines, ETL, API payload transformation, and scenarios where mapping logic should be externalized from the core application.

```json
{
  "identifier": .metadata.namespace + "-" + string(.metadata.id),
  "status": if (any([for (.analysis.findings) .severity == "critical"]))
              "BLOCK"
            else
              "ALLOW",
  "summary": {
    "total_bugs": size([for (.analysis.findings) if (.type == "bug") .]),
    "critical_count": size([for (.analysis.findings) if (.severity == "critical") .])
  },
  "tags": join([for (.labels) uppercase(.)], ", "),
  "last_seen": .analysis.timestamps.finished_at | .analysis.timestamps.started_at
}
```

* Good, because **Performance:** It is a native Java implementation designed for the JVM, performing significantly faster than the Jackson-JQ emulation. (Performance)
* Good, because **Helper Functions:** Requires defining helpers (`def function-name(...)`), which makes the main transformation body cleaner and enhances long-term script modularity and debugging. (Maintainability, Extensibility)
* Good, because **Extensibility:** Logic is easy to reuse across multiple mappings via defined helpers. (Extensibility)
* Good, because **Standardized Reusability:** By identifying frequently used transformations, we can centralize them into a "Core IDP Library." This ensures that if a transformation rule changes (for example, a naming convention update), we update it in one place instead of modifying hundreds of mapping scripts. (Maintainability / Extensibility)
* Good, because **User-Driven Extensibility:** Allowing users to contribute their own functions to the library empowers the community to solve niche problems while keeping the core engine clean and high-performing. (Contributing in open source mode)
* Good, because **Multilayered Extensibility:** Supports function definition at three levels: Java-native (for performance), Imported Library (for platform standards), and Inline DSL (for user-specific logic). This provides a smooth transition from "No-Code" to "Power-User" without hitting an architectural ceiling. (Extensibility / User experience)
* Good, because **Runtime Extensibility:** JSLT facilitates the injection of custom logic post-deployment via an import mechanism or dynamic script concatenation. This allows users to provide their own "Standard Library" of functions (via Docker volumes or Database records) that the engine can load at runtime, enabling high-tier extensibility in an Open Source, "No-Code" context. (Extensibility / Contributing in open source mode)
* Good, because **Robust Syntax Checking:** JSLT’s Parser.compile() returns detailed metadata (line and column numbers) on failure. This allows us to build a high-quality validation stage that tells the user exactly why and where their custom function is broken, significantly improving the "No-Code" troubleshooting experience. (User experience / Testability)
* Bad, because **Verbosity:** More verbose for complex logic due to the requirement for helper definitions. (Complexity of implementation, Readability)
* Bad, because **Limitations:** Becomes overly complex or unreadable for tasks like complex grouping/aggregation, cross-referencing arrays, recursive structures, date/time math, and dynamic key generation. (Complexity of implementation)
* Bad, because **Maintenance Overhead**: To provide a "no-code" experience for end-users, the core team must build and maintain a custom library of helper functions. This creates a permanent maintenance layer that requires versioning, testing, and documentation. (Complexity of implementation / Maintainability)
* Bad, because **Limited Built-ins:** Some advanced math, date or recursive operations may require custom Java function extensions. (Complexity of implementation)
* Bad, because **Smaller Community:** Has a smaller ecosystem and fewer online resources and answers than JQ. (Contributor and end users UX)
* Bad, because **Higher Entry Barrier:** Even with a library, the initial "blank page" experience for a new user is harder with JSLT than with the JQ simple piping syntax. (Contributor UX)
* Bad, because **Configuration Integrity Risk:** Allowing runtime or volume-mounted functions introduces the risk of "broken" configurations preventing system startup. We must implement a validation stage during the application's lifecycle to ensure all external scripts are syntactically correct before they are utilized. (Complexity of implementation / Reliability)

### 2. Use the Jackson-JQ Java Library

JQ is a lightweight, command-line JSON processor with a functional, pipeline-based syntax. It excels at concise, inline transformations and is widely used for scripting and quick data manipulation. JQ uses the alternative operator (`//`) for fallbacks and is designed for highly concise, functional chaining of filters. All logic can be written inline.

```json
{
  "identifier": "\(.metadata.namespace)-\(.metadata.id | tostring)",
  "status": (
    if .analysis.findings | any(.severity == "critical")
    then "BLOCK"
    else "ALLOW"
    end
  ),
  "summary": {
    "total_bugs": [.analysis.findings[] | select(.type == "bug")] | length,
    "critical_count": [.analysis.findings[] | select(.severity == "critical")] | length
  },
  "tags": (.labels | map(ascii_upcase) | join(", ")),
  "last_seen": (.analysis.timestamps.finished_at // .analysis.timestamps.started_at)
}
```

* Good, because **Conciseness:** Allows complex logic to be written almost entirely inline, resulting in shorter, dense code blocks. (Readability/Conciseness)
* Good, because **Ubiquity:** JQ is the "de-facto" standard for JSON manipulation; most technical users already know the syntax. (Contributor UX)
* Good, because **Logic:** Provides flexible and concise string manipulation (for example, `split`, `ascii_downcase`). (Complexity of implementation)
* Bad, because **Readability:** Deeply nested or complex inline logic can sometimes compromise immediate readability and debugging. (Readability, Maintainability)
* Bad, because **Performance Penalty:** The Jackson-JQ library is an emulation of the original C-based JQ and is notably slower on large payloads. (Performance)
* Bad, because **Maintenance Debt:** Lacks formal modularity; code reuse is usually achieved via copy-pasting, which leads to duplication across blueprints. (Maintainability, Extensibility)
* Bad, because **Incomplete Parity:** Jackson-JQ is not a 100% port of the official C-based JQ. It lacks several built-in functions and advanced filters, leading to "documentation drift" where developers write code based on official JQ manuals that fails to execute in the Java environment. (Complexity of implementation / Maintainability)
* Bad, because **Static Extension Model:** Unlike JSLT, which allows for dynamic function definitions within the query text, adding custom logic to Jackson-JQ requires implementing a low-level Java interface and registering it within a Scope. This creates a hard dependency on the Java build lifecycle, meaning new transformation functions cannot be injected at runtime (via a database or volume mount) and instead require a full application recompilation and Docker image redeploy. (Complexity of implementation / Developer Velocity)

## More Information

1. [JSLT documentation](https://github.com/schibsted/jslt)
2. [JQ documentation](https://jqlang.org)
