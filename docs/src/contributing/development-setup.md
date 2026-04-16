---
title: Development Setup
description: Set up your local development environment for the Internal Developer Platform
---

This guide focuses on using the devcontainer so you don’t need local Java/Maven setup.

## Prerequisites

- Docker 20.10+
- Git 2.30+
- VS Code (recommended) or JetBrains IntelliJ IDEA

---

## Clone Repository

```bash
# Clone your fork
git clone https://github.com/YOUR_USERNAME/internal-developer-platform.git
cd internal-developer-platform

# Add upstream remote
git remote add upstream https://github.com/decathlon/internal-developer-platform.git
```

---

## Devcontainer Setup

The devcontainer provides Java 25, Maven 3.9.9, Docker command-line tool, and recommended IDE extensions.

### VS Code

1. Install [VS Code](https://code.visualstudio.com/) and the **DevContainers** extension
2. Ensure Docker is running
3. Open the project folder
4. Select **Reopen in Container** (or run **DevContainers: Reopen in Container**)
5. Wait for the container build and Maven import

### JetBrains IDEs

1. Ensure Docker is running
2. Open the project folder
3. Choose **DevContainer** / **Open in DevContainer**
4. Wait for the container build and Maven import

---

## Start the project

Once the devcontainer is running, you have a fully set up environment. At startup, the `docker compose up` command will run the PostgreSQL database. As well, in the `docs` folder, the `uv` virtual environment is already set up.

### Included Tools

The devcontainer includes:

- Pre-commit tool (run `pre-commit install` to enable hooks)
- GH command-line tool
- Python with UV and utilities for docs site generation
- PostgreSQL database running with `docker compose`

---

## Database Setup

### Automatic Migration

Flyway runs migrations automatically on startup. Check the migrations in:

```bash
src/main/resources/db/migration/
├── V1_1__Create_property_rules_table.sql
├── V1_2__Create_property_definition_table.sql
├── V1_3__Create_relation_definition_table.sql
├── V1_4__Create_entity_template_table.sql
└── V1_5__Create_junction_tables.sql
```

### Sample Data

For local development, the system inserts sample data:

```bash
src/main/resources/db/local/
└── R__1_Insert_sample_data.sql
```

### Reset Database

Within the devcontainer, you can reset the database with:

```bash
# Drop and recreate
docker compose down -v
docker compose up -d postgres

# Or manually
psql -c "DROP DATABASE idp; CREATE DATABASE idp;"
```

---

## Development Workflow

### 1. Sync with Upstream

```bash
git fetch upstream
git checkout main
git merge upstream/main
```

### 2. Create Feature Branch

```bash
git checkout -b feature/my-feature
```

### 3. Make Changes

Edit code, write tests.

### 4. Run Checks

```bash
# Run tests
mvn test -Dspring.profiles.active=test

# Check for issues
mvn verify

#Run pre-commit hooks (if installed)
git add .
pre-commit run --all-files
```

### 5. Commit

```bash
git add .
git commit -m "feat(scope): add my feature"
```

### 6. Push & Create PR

```bash
git push origin feature/my-feature
# Open PR on GitHub
```

---

## Next Steps

- **[Architecture](architecture.md)** - Understand the codebase
- **[Code Conventions](code/code-conventions.md)** - Follow coding standards
- **[Testing](testing.md)** - Write effective tests
