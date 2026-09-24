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

## Development Container

The repository includes a development container with Java 25, Maven, Docker, uv,
pre-commit, the GitHub command-line tool, and the PostgreSQL client. It starts the
PostgreSQL service, the documentation server, and Spring Boot automatically.

### Supported Hosts

| Host | Status | Requirement |
| ---- | ------ | ----------- |
| Debian with Docker Engine | Supported | Use a Docker daemon with root access |
| Windows with Docker Desktop | Supported | Keep the clone inside the WSL2 filesystem |
| WSL2 with Docker Engine | Supported | Use a Docker daemon with root access |
| macOS with Docker Desktop or OrbStack | Supported | Use a multi-architecture Docker runtime |
| Cloud development environment | Supported | Allocate at least 4 processors and 8 GB of memory |

Docker rootless mode and other container management tools are not supported because the container uses
Docker-in-Docker for Testcontainers and the local Compose service.

### Open the Container

1. Install [Docker Desktop](https://www.docker.com/products/docker-desktop/)
   on Windows or macOS, or install Docker Engine on Debian or WSL2.
2. Install [VS Code](https://code.visualstudio.com/) and the
   [development container extension](https://marketplace.visualstudio.com/items?itemName=ms-vscode-remote.remote-containers).
3. Open the repository in VS Code.
4. Run **development containers: reopen in container**.

The first build downloads the pinned tool images and dependencies. Later
rebuilds reuse named volumes for the Maven and Docker caches.
The container sets `UV_NATIVE_TLS=true` so uv trusts the operating system's
certificate authorities, including local certificates for inspecting proxies.

After startup, open <http://localhost:8084> for the API and
<http://localhost:8000> for the documentation site. PostgreSQL is available on
port `5437` with the local credentials used by the `local` Spring profile.
Check the **Ports** panel for the actual forwarded addresses: vs code can
choose another host port when the requested port is occupied.

On Windows, clone the repository from a WSL2 terminal and open it from that
filesystem. Avoid `/mnt/c` because file sharing there significantly slows
Maven and file watching.

### Watch and Reload

Zensical runs with `serve`, which watches documentation sources and refreshes
the browser after changes. No separate watch command is required.

Spring Boot starts with the `local` profile. Wait for VS Code to finish
importing the Maven project, then save your Java changes. Java auto-build
compiles them into `target/classes`, and DevTools restarts the app
after successful compilation. Resolve compilation errors in the **Problems**
panel before expecting a reload. Resource changes also trigger DevTools when
the build copies them to the class path.

This workflow depends on VS Code Java auto-build. For terminal-only editing,
run `./mvnw compile` after changes. The separate `dev` profile retains its
existing `.reloadtrigger` requirement; the automatic launch does not use it.
Spring browser LiveReload is off for `local`; app restarts and
Zensical browser refresh remain enabled.

### Service Controls

Run these commands from the repository root:

```bash
bash .devcontainer/scripts/dev-services.sh status all
bash .devcontainer/scripts/dev-services.sh logs app
bash .devcontainer/scripts/dev-services.sh stop app
bash .devcontainer/scripts/dev-services.sh start app
bash .devcontainer/scripts/dev-services.sh restart docs
```

Each command accepts `docs`, `app`, or `all`. The `all` option covers only docs
and Spring Boot; PostgreSQL remains under Docker Compose control. The `logs`
command prints recent output and the full log path. Use `tail -f` with that
path to follow output. Runtime state and logs live in a workspace-specific
directory under `/tmp`, outside the repository.

Startup waits for HTTP readiness and reports failures with log output. The
first Spring Boot launch can take several minutes to compile. Docs startup
does not depend on database readiness. Repeated starts reuse managed services;
occupied ports belonging to other processes remain untouched.

### Run Spring Boot Another Way

Stop the managed app before launching it through VS Code **Run and Debug** or
your terminal:

```bash
bash .devcontainer/scripts/dev-services.sh stop app
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Replace `local` with your required profile and supply its environment variables.
For VS Code, set `--spring.profiles.active=local` in your Java launch arguments,
or select your required profile instead. The container does not impose a
global Spring profile.

The managed app stays stopped until you explicitly start it or restart the
container. Stop a manual launch with **Stop Debugging** or `Ctrl+C` before
running `start app` again. The helper never stops a manually launched process.

### Troubleshoot Startup and Ports

Use `status all` and `logs app` or `logs docs` to investigate startup failures.
For database failures, run `docker compose logs postgres`. Fix the reported
problem, then restart the affected service. Service commands are backed by
`supervisord`, which owns each process directly and never signals an
unrelated process.

If a reload fails with unresolved generated mapper types after switching
branches or importing the project, stop the app, rebuild its generated
classes, then restart:

```bash
bash .devcontainer/scripts/dev-services.sh stop app
./mvnw clean compile
bash .devcontainer/scripts/dev-services.sh start app
```

If IDE compilation errors persist, run **Java: clean Java Language Server
workspace** and wait for the Maven import to finish.

Only ports `8000`, `8084`, and `5437` are automatically forwarded. Additional
listeners can belong to VS Code, Java tooling, or tests; a forwarded port does
not prove that an app is healthy. Run `ss -ltnp` to identify listeners
before stopping anything. Use **Stop Forwarding** to remove old entries from
the **Ports** panel, and forward debug ports manually when needed.

After changing the container configuration, run **development containers: rebuild
container** to apply editor defaults and forwarding rules. Existing restored
forwarding entries might need removal once. Do not delete database volumes or
stop unrelated editor processes to clean up this list.

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

For your development setup, you can refer to the getting started documentation here: [getting started](../getting-started/index.md)

Configure the app.yml security credentials and secrets locally using environment variables or a local, untracked configuration file.

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

- **[Architecture](code/domain-infrastructure.md)** - Understand the codebase
- **[Code Conventions](code/code-conventions.md)** - Follow coding standards
- **[Testing](testing.md)** - Write effective tests
