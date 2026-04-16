---
applyTo: "**/*.*"
---

# Internal Developer Platform - AI Agent Instructions

## Project

- The default branch is `main`.

## Overview

**idp-core** is the backbone of the Internal Developer Platform. It provides a modern, scalable, and extendable backend to build your software catalog, track engineering excellence through scorecards, and empower teams with self-service actions.

## Architecture Fundamentals

### Hexagonal Architecture

- Follow hexagonal architecture principles to ensure a clean separation of concerns.
- Organize code into distinct layers: Domain, Application, and Infrastructure.

## Common Pitfalls

- **DO NOT** edit or refer to generated files in `docs/site/`, `.venv/` and `.vscode/` - they're git-ignored
- **ALWAYS** run build and tests after code changes. Use `pre-commit` hooks for linting and validation.
- **REMEMBER** to update the documentation in `docs/src/` when adding features or changing behavior
