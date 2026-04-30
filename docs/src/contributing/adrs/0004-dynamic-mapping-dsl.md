# 0004 - Domain Specific Language (DSL) for Dynamic Mapping implementation

* Status: Accepted
* Deciders:
  * maintainers team: Andrés BRAND, Matthieu WALTERSPIELER, Eve BERNHARD, Ferial OUKOUKES, Renny VANDOMBER
  * contributors team: `N/A`
* Consulted: Étienne JACQUOT
* Informed: `N/A`
* Date: 2026-04-27

## Context and Problem Statement

Mapping source objects to a target model is a core capability that enables IDP features such as data connectors, calculated properties, and Scorecard definitions. To implement what we call a "mapping engine," we must support dynamic transformations based on user-provided queries. This query language should be user-friendly, standardized, and late-bound. Dynamic mapping allows the application to change how it interprets incoming data without requiring recompilation or redeployment; this is achieved by using a Domain Specific Language (DSL) stored in a database or configuration file. Given that the IDP-Core is a Spring Boot application, this study evaluates two primary DSL options: JSLT and JQ.

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
* User Experience (UX)

## Decision Outcome

Chosen option: option 1, **"JSLT (JSON Standard Transformation Language),"** because it provides a robust, JVM-native DSL that meets our requirements for perfomance, modularity, maintainability, and scalability. Its explicit use of helper functions for complex logic—while occasionally more verbose than JQ—results in clearer, more maintainable scripts and superior long-term debuggability.

### Positive Consequences

* **Modular Logic:** Explicit helper functions enhance script modularity and simplify long-term maintenance.
* **Reusable Transformations:** Logic is easily shared across multiple mappings via defined helpers, supporting high extensibility.
* **Open Source Friendly:** The DSL approach allows community contributors to extend mapping logic without modifying the core Java codebase.

### Negative Consequences

* **Learning Curve:** JSLT is less common than JQ; contributors and users may require time to adapt to its XSLT-inspired syntax.
* **Verbosity:** Simple mappings may require more lines of code than equivalent JQ expressions.
* **Complex Use Cases:** Certain scenarios (for example, complex recursive structures or advanced dynamic key generation) can become difficult to read, potentially necessitating custom function extensions.

## Considered Options

1. **Use the JSLT (JSON Standard Transformation Language) Java library**
2. **Use JQ as the mapping DSL**
   * 2.a **Use the Jackson-jq (Java library)**
   * 2.b **Use the JQ binary (via process execution)**

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
* Good, because **Robust Syntax Checking:** JSLT’s Parser.compile() returns detailed metadata (line and column numbers) on failure. This allows us to build a high-quality validation stage that tells the user exactly why and where their custom function is broken, significantly improving the "No-Code" troubleshooting experience. (User experience / Testability)
* Good, because **Helper Functions:** Requires defining helpers (`def function-name(...)`), which makes the main transformation body cleaner and enhances long-term script modularity and debugging. (Maintainability, Extensibility)
* Good, because **Multilayered Extensibility:** Supports function definition at three levels: Java-native (for performance), Imported Library (for platform standards), and Inline DSL (for user-specific logic). This provides a smooth transition from "No-Code" to "Power-User" without hitting an architectural ceiling. (Extensibility / User experience)
* Good, because **Standardized Reusability:** By identifying frequently used transformations, we can centralize them into a "Core IDP Library." This ensures that if a transformation rule changes (for example, a naming convention update), we update it in one place instead of modifying hundreds of mapping scripts. (Maintainability / Extensibility)
* Good, because **User-Driven Extensibility:** Allowing users to contribute their own functions to the library empowers the community to solve niche problems while keeping the core engine clean and high-performing. (Contributing in open source mode)
* Good, because **Runtime Extensibility:** JSLT facilitates the injection of custom logic post-deployment via an import mechanism or dynamic script concatenation. This allows users to provide their own "Standard Library" of functions (via Docker volumes or Database records) that the engine can load at runtime, enabling high-tier extensibility in an Open Source, "No-Code" context. (Extensibility / Contributing in open source mode)
* Bad, because **Verbosity:** More verbose for complex logic due to the requirement for helper definitions. (Complexity of implementation, Readability)
* Bad, because **Limitations:** Becomes overly complex or unreadable for tasks like complex grouping/aggregation, cross-referencing arrays, recursive structures, date/time math, and dynamic key generation. (Complexity of implementation)
* Bad, because **Maintenance Overhead**: To provide a "no-code" experience for end-users, the core team must build and maintain a custom library of helper functions. This creates a permanent maintenance layer that requires versioning, testing, and documentation. (Complexity of implementation / Maintainability)
* Bad, because **Limited Built-ins:** Some advanced math, date or recursive operations may require custom Java function extensions. (Complexity of implementation)
* Bad, because **Smaller Community:** Has a smaller ecosystem and fewer online resources and answers than JQ. (Contributor and end users UX)
* Bad, because **Higher Entry Barrier:** Even with a library, the initial "blank page" experience for a new user is harder with JSLT than with the JQ simple piping syntax. (Contributor UX)
* Bad, because **Configuration Integrity Risk:** Allowing runtime or volume-mounted functions introduces the risk of "broken" configurations preventing system startup. We must implement a validation stage during the application's lifecycle to ensure all external scripts are syntactically correct before they are utilized. (Complexity of implementation / Reliability)

### 2. Use JQ as the mapping DSL

#### Common Good and Bad of JQ (applies to both sub-options)

* Good, because **Conciseness:** Highly expressive and concise, allowing complex transformations in fewer lines. (Readability/Conciseness)
* Good, because **Popularity:** Widely known and used in the open source and DevOps communities, lowering the learning curve for contributors. (Adoption)
* Good, because **Powerful Built-ins:** Provides a rich set of built-in functions for string manipulation, filtering, and aggregation. (Functionality)
* Bad, because **Extensibility:** JQ lacks formal modularity; code reuse is achieved by copy-pasting or composing filters, which can lead to duplication. (Maintainability)
* Bad, because **Readability:** Deeply nested or complex inline logic can compromise immediate readability. (Readability)
* Bad, because **Integration:** Integrating JQ into a Java application is less seamless than JSLT, particularly regarding error handling and type safety. (Integration)

#### 2.a Use the Jackson-jq Java library

* Good, because **Runs in JVM:** No external process required; can be embedded directly in the Java application. (Integration)
* Good, because **No system dependencies:** Works in any environment where Java runs. (Portability)
* Bad, because **Performance:** Jackson-JQ is an emulation and is significantly slower than native JQ or JSLT. (Performance)
* Bad, because **Feature Gaps:** Does not support the full JQ specification; advanced filters may fail.
* Bad, because **Error Handling:** Error messages and debugging are less clear than with native JQ. (Debuggability)

#### 2.b Use the JQ binary via process execution

* Good, because **Full JQ Feature Set:** Access to 100% of the JQ specification and latest updates.. (Functionality)
* Good, because **Decoupled Lyfecycle:** Allows the mapping engine version to be updated independently of the Java application. (Maintenance)
* Bad, because **Perfomance** While native JQ is optimized in C, its performance is negated in a JVM context by Inter-Process Communication (IPC) overhead and the Double-Serialization Tax (marshalling JSON to/from a system pipe). (Performance)
* Bad, because **System Dependency:** Requires handling a JQ binary complicating deployment and portability. (Portability, Maintenance)
* Bad, because **Process Overhead:** Involves spawning external processes, which adds overhead and complexity to error handling and resource management. (Performance)
* Bad, because **Security:** Running external binaries can introduce security risks if not properly sandboxed. (Security)
* Bad, because **Cold Start Latency:** Every execution requires the OS to fork a process and initialize the binary, creating a performance bottleneck. (Performance)
* Bad, because **Process Management:** If the JVM crashes or a Virtual Thread is interrupted while the JQ process is running, there is a risk creating "Zombie Processes". (Integration)
