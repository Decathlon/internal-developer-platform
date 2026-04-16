---
name: create-architectural-decision-record
description: 'Create an Architectural Decision Record (ADR) document for decision documentation. Use this when asked to document a new architectural decision.'
---

# Create Architectural Decision Record

Create an ADR document for `${input:DecisionTitle}` using structured formatting optimized for AI consumption and human readability.

## Inputs

- **Decision**: `${input:Decision}`
- **Options**: `${input:Options}`
- **Stakeholders**: `${input:Stakeholders}`

## Input Validation

If any of the required inputs are not provided or cannot be determined from the conversation history, ask the user to provide the missing information before proceeding with ADR generation.

## Requirements

- Use precise, unambiguous language
- Follow standardized ADR format with front matter
- Include both positive and negative consequences
- Document alternatives with rejection rationale
- Structure for machine parsing and human reference
- Tag each pros/cons bullet point with the relevant decision drivers in parentheses (for example, `(Security)`, `(User experience, Complexity of implementation)`)

The ADR must be saved in the `/docs/src/contributing/adrs` directory using the naming convention: `NNNN-[title-slug].md`, where NNNN is the next sequential 4-digit number (for example, `0001-database-selection.md`).

## Required Documentation Structure

The documentation file must follow the template below, ensuring that all sections are filled out appropriately. The front matter for the markdown should be structured correctly as per the example following:

```md

# NNNN - [Decision Title]

* Status: Accepted | Rejected | Proposed | Draft | Superseded by [NNNN - New Decision Title](link-to-new-decision)
* Deciders:
  * maintainers team: [Name(s)]
  * contributors team: [Name(s)]
* Consulted: [Name(s) or N/A]
* Informed: [Name(s) or N/A]
* Date: [YYYY-MM-DD] ← use today's date

## Context and Problem Statement

[Problem statement, technical constraints, business requirements, and environmental factors requiring this decision.]

## Decision Drivers

* Security [MANDATORY DRIVER]
* User experience [MANDATORY DRIVER]
* Complexity of implementation [MANDATORY DRIVER]
* FinOps [MANDATORY DRIVER]
* [Additional drivers specific to the decision]

## Considered Options

[Numbered list of option titles only. Detailed analysis for each option appears in the "Pros and Cons of the Options" section.]

1. Use `[Option 1]`
2. Use `[Option 2]`
N. Use `[Option N]`

## Decision Outcome

Chosen option: option [N], "[Decision Title]," because [Rationale for the decision, including how it addresses the drivers and constraints].

### Positive Consequences

[List of positive consequences resulting from the decision. Think about opportunities for the future. Do not repeat pros mentioned in the "Pros and Cons of the Options" section.]

### Negative Consequences

[List of negative consequences resulting from the decision. Think about risks and trade-offs. Do not repeat cons mentioned in the "Pros and Cons of the Options" section.]

## Pros and Cons of the Options

### 1. Use `[Option 1]`

[Complete description of option 1. Add Mermaid diagrams if needed to illustrate the option.]

* Good, because [reason] ([Driver])
* Bad, because [reason] ([Driver])

### 2. Use `[Option 2]`

[Complete description of option 2. Add Mermaid diagrams if needed to illustrate the option.]

* Good, because [reason] ([Driver])
* Bad, because [reason] ([Driver])

### N. Use `[Option N]`

[Complete description of option N. Add Mermaid diagrams if needed to illustrate the option.]

* Good, because [reason] ([Driver])
* Bad, because [reason] ([Driver])


## More Information

[Links to relevant documentation, discussions, or resources related to the decision.]

```

## Additional actions

- Update the ADR index file (`/docs/src/contributing/adrs/index.md`) to include a reference to the new ADR with its title and a link to the document.
