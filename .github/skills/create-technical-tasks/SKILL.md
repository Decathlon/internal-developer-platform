---
name: create-technical-tasks
description: 'Create focused, evidence-based technical tickets from a problem, investigation, or change request. Use when breaking engineering work into actionable tickets, defining acceptance criteria, or assessing technical scope and dependencies.'
argument-hint: 'Describe the problem or technical change to break into tickets'
---

# Create Technical Tasks

Turn a technical problem, investigation, or requested change into clear, actionable engineering tickets grounded in the repository.

This skill creates tickets only. Do not implement the changes or publish tickets to an external system unless the user separately asks.

## Workflow

1. **Clarify the outcome**
   - Identify the problem, desired result, constraints, and requested scope.
   - Ask a concise question only when missing information would materially change the tickets.
   - Otherwise, state reasonable assumptions in the result.

2. **Read repository instructions**
   - Find and read all repository instruction files before analyzing or drafting tickets.
   - Look for root and nested `AGENTS.md`, `copilot-instructions.md`, `*.instructions.md`, and files under `.github/instructions/`.
   - Exclude generated or vendored content when searching.
   - Note which instructions apply to the code paths in scope and follow them when describing proposed work.

3. **Use Graphify when available**
   - Check whether a Graphify skill or integration is available and whether the repository contains a usable `graphify-out/graph.json`.
   - When available, read its usage instructions and query the graph for relevant components, callers, and relationships.
   - Treat graph results as navigation evidence, then verify important findings against source code, tests, and repository instructions.
   - Do not rebuild the graph unless the user asks or the existing graph cannot answer a necessary question.
   - If Graphify is unavailable, continue with repository search and source inspection; mention that briefly in the result.

4. **Inspect the affected behavior**
   - Trace the behavior from its entry point through the code that directly controls it.
   - Check important callers, shared abstractions, persistence or API boundaries, and nearby tests.
   - Assess blast radius across the repository. Do not scope a shared behavior change to the first feature or endpoint where it was noticed.
   - Ground findings in specific files, symbols, and tests. Distinguish observed behavior from hypotheses.

5. **Decompose the work**
   - Create tickets that each describe one coherent, independently verifiable outcome.
   - Split work when it crosses ownership boundaries, has distinct validation needs, or can be delivered independently.
   - Show dependencies and a sensible order when one ticket blocks another.
   - Avoid duplicate tickets, speculative implementation details, and broad tickets that bundle unrelated outcomes.

6. **Check ticket quality**
   - Make each ticket understandable without requiring the reader to repeat the investigation.
   - Include a concise, illustrative code snippet in **Scope** showing the proposed interfaces, signatures, queries, or key logic changes when code changes are clear. Keep snippets focused and aligned with repository instructions rather than pasting full-file implementations.
   - Write acceptance criteria that are observable and testable.
   - Include relevant regression, performance, compatibility, or migration checks when they apply.
   - Call out assumptions, unresolved questions, and out-of-scope work rather than silently deciding them.
   - Label proposed snippets as illustrative unless the repository architecture or evidence requires an exact contract.

## Output

Present the tickets in Markdown. Use only the sections that add useful information:

### Ticket N: Short, outcome-focused title

**Context**  
What is happening and why it matters, with concise repository evidence.

**Outcome**  
The observable result this ticket should deliver.

**Scope**  
- The included work, target files/layers, and explicit exclusions when useful.
- **Proposed code (illustrative):** Concise snippet(s) showing the key method signatures, port/model contracts, query shape, or core algorithmic change to introduce.

**Acceptance criteria**
- Verifiable condition
- Relevant behavior or regression condition

**Validation**  
Tests or checks that demonstrate the criteria are met.

**Dependencies / open questions**  
Include only when applicable.

**Evidence**  
Link to relevant repository files, symbols, or tests.

Group related tickets under a short heading when that makes the sequence clearer. Do not assume a particular ticketing platform or impose platform-specific fields.

## Example usage

```text
/create-technical-tasks Investigate repeated relation-target resolution in EntityPersistenceMapper. Identify whether the shared persistence behavior affects entity updates and graph flows, then propose focused implementation tickets.
```