---
title: Development Setup
description: Set up your local development environment for IDP-Core
---

This guide walks you through setting up a complete development environment for IDP-Core.

## Prerequisites

| Tool   | Version | Purpose         |
| ------ | ------- | --------------- |
| Java   | 25+     | Runtime         |
| Maven  | 3.9+    | Build tool      |
| Docker | 20.10+  | Local services  |
| Git    | 2.30+   | Version control |
| IDE    | -       | Development     |

### Verify Installation

```bash
java -version   # Should show 25+
mvn -version    # Should show 3.9+
docker --version
git --version
pre-commit --version # If using pre-commit (recommended)
```

---

## Clone Repository

```bash
# Clone your fork
git clone https://github.com/YOUR_USERNAME/internal-developer-platform.git
cd internal-developer-platform

# Add upstream remote
git remote add upstream https://github.com/Decathlon/internal-developer-platform.git
```

---

## IDE Setup

### VS Code (Recommended)

1. Install [VS Code](https://code.visualstudio.com/)
2. Install extensions:
   - Java Extension Pack
   - Maven for Java
   - Docker
3. Open the project folder in VS Code
4. Import Maven projects when prompted

---

## Start the project

For your development setup, you can refer to the Getting started documentation here: [Getting Started](getting-started.md)

### Pre-Commit Hooks (Optional)

Install pre-commit hooks for code quality:

```bash
pre-commit install
```

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
git commit -m "feat: add my feature"
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
