# Related Entity Data Exposition

* Status: Proposed
* Deciders:
  * maintainers team: Andrés BRAND, Matthieu WALTERSPIELER, Eve BERNHARD, Ferial OUKOUKES, Renny VANDOMBER
  * contributors team: `N/A`
* Consulted: Étienne JACQUOT
* maintainers team: IDP Core Maintainers
* contributors team: N/A


## Context and Problem Statement

Our Internal Developer Platform (IDP) utilizes an Entity-Attribute-Value (EAV) storage layout to maintain a highly dynamic component catalog. Frontend graph visualizations and catalog dashboards frequently need to present properties belonging to *related* entities alongside a primary entity (for example, showing a `repository_url` from a linked `GitHubRepository` node while inspecting a `Software` component).

This ADR is for deciding how we will provide clients with programmatic access to these deeply nested, linked properties while ensuring an outstanding frontend Developer Experience (DX), maintaining real-time data consistency, and strictly protecting our 150ms backend query performance baseline.

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

## Considered Options

1. Linked (Mirror) Properties

1.a. Linked (Mirror) Stored Properties Feature (Data Duplication via Templates)

1.b. Linked (Mirror) Calculated-at-Request Properties Feature (Dynamic Injected Mapping)

2. Dedicated Property Projection REST Endpoint (Smart Backend, Flat Tree Harvest)

3. Native GraphQL Server Layer (EAV Schema Compilation)

## Decision Outcome

Chosen option: Option 2, "Dedicated Property Projection REST Endpoint," because it comes out best. It delivers the precise payload-trimming advantages of GraphQL and the real-time data accuracy of live fetches, while completely preserving our bounded, single-pass 150ms database execution layout (O(1) database calls) and avoiding complex framework abstractions.

Unlike Option 1.a, it introduces zero data consistency risks, and unlike Option 1.b, it completely separates graph structural retrieval from standard entity reads, ensuring our primary CRUD APIs stay lean and unburdened by heavy runtime graph lookup logic.

### Positive Consequences

* **Sub-Millisecond Object Traversal:** The frontend receives an optimized, pre-flattened key-value payload layout. Developers can access deeply nested attributes instantly using direct paths without writing defensive loops.
* **Bounded Resource Utilization:** Database workloads remain completely predictable. The data pipeline always executes exactly 3 database queries total per batch execution window, protecting database resources.
* **Guaranteed Data Integrity:** Zero data replication means there is no risk of sync delay or stale data states.

### Negative Consequences

* **Fixed Response Shape Envelope:** The JSON response layout structure is defined by backend DTO signatures, meaning individual metadata structural keys cannot be arbitrarily pruned out of serialization streams on a whim per client call.

---

## Pros and Cons of the Options

### 1.a. Linked (Mirror) Stored Properties Feature

Systematically copy and physically store specified properties from a target related entity onto the source entity row in the EAV database based on configuration rules defined inside the system templates.

* Good, because read performance is exceptionally fast since all required properties are pulled directly out of a single primary entity row layout (**Performance & Latency**).
* Good, because standard flat REST payloads automatically return everything inside a traditional, predictable dictionary model (**User experience**).
* Bad, because updates require cascade background worker processes to sync mutated strings across the EAV database, risking partial failures and state drift (**Data consistency**).
* Bad, because duplicate data strings are scattered across database collections, wasting disk space allocation (**FinOps**).

### 1.b. Linked (Mirror) Calculated-at-Request Properties Feature

Do not store duplicate values in the database. Instead, whenever a standard entity is requested, the system automatically uses the template configuration to dynamically look up the related entity's properties during the runtime processing cycle and injects them directly into the source entity's payload data model before returning it.

* Good, because it provides the client with a single flat entity structure containing nested data without storing redundant records in the database (**Data consistency**, **User experience**).
* Good, because values are computed live from their true sources, ensuring the frontend never displays stale data (**Data consistency**).
* Bad, because it couples every standard entity read to a runtime relationship evaluation, which degrades the performance of basic database lookups even when users do not need the extra graph properties (**Performance & Latency**).
* Bad, because it risks triggering massive N+1 sequential database loops or complex custom query interceptors during normal entity fetching workflows (**Complexity of implementation**).

### 2. Dedicated Property Projection REST Endpoint

Introduce a specialized `POST` query endpoint that accepts a search payload. It uses the `EntityGraphService` to fetch the complete graph tree in memory, harvests the target attributes via a clean recursive pass, and flattens the response into a direct key-value dictionary before serialization.

We could have an request contract like this:

```json
{
  "roots": [
    {
      "templateIdentifier": "microservice",
      "identifier": "ordering-api"
    },
    {
      "templateIdentifier": "worker-pool",
      "identifier": "payment-worker"
    }
  ],
  "depth": 2,
  "mode": "STRICT_LINEAGE",
  "propertyProjections": [
    {
      "relationName": "has_repository",
      "targetTemplateIdentifier": "github-repository",
      "propertyNames": ["repository_url", "default_branch"]
    },
    {
      "relationName": "monitored_by",
      "targetTemplateIdentifier": "sonarqube-project",
      "propertyNames": ["quality_gate_status"]
    }
  ]
}
```

* Good, because it transforms complex nested tree maps into a direct key-value dictionary schema before serialization, eliminating frontend parsing overhead (**User experience**).
* Good, because values are fetched directly from their original source entries in real time with zero data replication drift (**Data consistency**).
* Good, because it maps directly onto our optimized, single-pass batch recursive CTE query structure without adding extra heavy frameworks (**Complexity of implementation**, **Performance & Latency**).
* Good, because it trims away unrequested EAV properties and structural graph nodes before serialization, lowering payload sizes over the wire (**FinOps**).
* Bad, because the final layout wrapper structure is locked by backend DTO signatures rather than dynamic client string parsing text fields (**User experience**).

### 3. Native GraphQL Server Layer

Introduce a formal GraphQL server framework interface on top of our EAV model, enabling clients to pick exact fields and navigate cross-entity pathways using nested query strings.

* Good, because self-documenting graph schemas with built-in autocomplete utilities offer high operational discovery flexibility (**User experience**).
* Bad, because its field-by-field execution model breaks our single bulk CTE query, requiring iterative batch queries per depth level (O(depth) execution footprint) (**Performance & Latency**).
* Bad, because data is returned matching the query shape, forcing UI developers to safely parse nested arrays of edges, targets, and properties in JavaScript (**User experience**).
* Bad, because it requires building a complex schema compiler to dynamically register dynamic GraphQL schemas out of runtime EAV template rules (**Complexity of implementation**).

---

## More Information

To support complex projection setups, this implementation will support an optional dot-notation key flattening format (e.g., `"github-repository.repository_url"`) right within the response builder layer to further simplify frontend state mapping integration. All query execution metrics will be piped to our standard catalog dashboard trackers to monitor performance stability over time.