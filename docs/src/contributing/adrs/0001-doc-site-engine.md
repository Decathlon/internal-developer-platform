# 0001 - Choose the engine for the documentation site

* Status: Accepted
* Deciders:
  * maintainers team: Étienne JACQUOT
  * contributors team: Eve BERNHARD
* Consulted: Ahmed KACI
* Informed: `N/A`
* Date: 2026-01-16

## Context and Problem Statement

To ease the usage and contribution to the project, we want to provide a documentation site. We need to choose the engine that will power this documentation site.
It has to rely on markdown files as source, be easy to use and maintain, and provide good performance for end users. It will be hosted on GitHub Pages.

## Decision Drivers

* Security
* User experience
* Complexity of implementation
* FinOps

## Considered Options

1. Use `Material for MkDocs`
2. Use `Docusaurus`
3. Use `Hugo`
4. Use `Zensical`

## Decision Outcome

Chosen option: option 4, "Use `Zensical`," because Decathlon team has already habits with `Material for MkDocs` and `Zensical` is the next supported version of it.

### Positive Consequences

* Faster on boarding for new contributors already familiar with Material for MkDocs
* Access to Decathlon's internal support and best practices for Zensical based on `Material for MkDocs` knowledge

### Negative Consequences

* Potential learning curve for external contributors unfamiliar with the ecosystem

## Pros and Cons of the Options

### 1. Use `Material for MkDocs`

Material for MkDocs is a popular theme built on top of MkDocs, providing a modern and responsive documentation site with rich features out of the box.

* Good, because it has a large community and extensive documentation (User experience, Complexity of implementation)
* Good, because it is Python-based, easy to set up with minimal configuration (Complexity of implementation)
* Good, because it provides built-in search, dark mode, and responsive design (User experience)
* Good, because it is free for basic features and self-hosted on GitHub Pages (FinOps)
* Bad, because some advanced features require the Insiders (paid) version (FinOps)
* Bad, because it relies on Python ecosystem which may conflict with Java-focused CI pipelines (Complexity of implementation)
* Bad, because it entered in maintenance mode in favor of Zensical (Security)

### 2. Use `Docusaurus`

Docusaurus is a static site generator developed by Meta, designed for building documentation websites with React components.

* Good, because it provides localization out of the box (User experience)
* Good, because it has strong community support and is actively maintained by Meta (Security)
* Good, because it allows embedding React components for interactive documentation (User experience, Security)
* Bad, because it requires Node.js and npm knowledge to customize (Complexity of implementation)
* Bad, because initial setup and configuration is more complex than MkDocs-based solutions (Complexity of implementation)
* Bad, because bundle size can be larger, impacting page load performance (User experience)

### 3. Use `Hugo`

Hugo is a fast static site generator written in Go, known for its speed and flexibility.

* Good, because it is extremely fast at building large documentation sites (User experience)
* Good, because it has no runtime dependencies, single binary distribution (Complexity of implementation)
* Good, because it is completely free and open source (FinOps)
* Bad, because it requires learning Go templating language for customization (Complexity of implementation)
* Bad, because documentation-specific themes require more configuration effort (Complexity of implementation)
* Bad, because the IDP core project doesn't seems to need extended capabilities of Hugo (User experience)

### 4. Use `Zensical`

Zensical is the new supported evolution of Material for MkDocs, providing enterprise-grade features.

* Good, because the team already has habits with Material for MkDocs, minimizing learning curve (Complexity of implementation)
* Good, because it is Python-based, easy to set up with minimal configuration (Complexity of implementation)
* Good, because it provides built-in search, dark mode, and responsive design (User experience)
* Good, because it is free for basic features and self-hosted on GitHub Pages (FinOps)
* Bad, because external contributors may not be familiar with Zensical-specific features (Complexity of implementation)
* Bad, because documentation and community resources are smaller compared to Material for MkDocs (User experience)

## More Information

* [Zensical documentation](https://zensical.org/docs/get-started/)
* [Material for MkDocs documentation](https://squidfunk.github.io/mkdocs-material/)
* [Docusaurus documentation](https://docusaurus.io/docs)
* [Hugo documentation](https://gohugo.io/documentation/)
