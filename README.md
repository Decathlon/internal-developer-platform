# 🚀 idp-core

**idp-core** is the backbone of the Internal Developer Platform. It provides an extensible, API-first backend to build your software catalog and keep it up to date from your existing tools. Scorecards and self-service actions are on the roadmap.

---

## ✨ Key Features

| Feature | Status | Description |
|---------|--------|-------------|
| 🏗️ **Software Catalog** | ✅ Available | Model your services, resources, and their relationships with entity templates, search them, and explore them as a graph. |
| 🧾 **Audit History** | ✅ Available | Every change to templates and entities is recorded and can be queried. |
| 🔌 **Data Integration** | 🚧 In progress | Inbound webhooks with JSLT mappings are available. Pull-based connectors and Kafka aren't available yet. |
| 📊 **Scorecards** | 🕐 Planned | Monitor and improve technical standards. See the [design](docs/src/features/scorecards.md). |
| 🛠️ **Self-Service Actions** | 🕐 Planned | Automate routine tasks for developers. See the [design](docs/src/features/self-service-actions.md). |

## 📚 Documentation

Detailed documentation is available in the [docs](docs/src/index.md) folder:

- 🚀 [Quick Start](docs/src/getting-started/quickstart.md) - Get up and running in minutes.
- 📖 [Core Concepts](docs/src/concepts/index.md) - Learn about entities, templates, and relations.
- ⚙️ [Features](docs/src/features/index.md) - Discover what idp-core can do for you.
- 🔌 [API Reference](docs/src/api/index.md) - OpenAPI documentation.
- 🐳 [Deployment Guide](docs/src/deployment/index.md) - Docker, K8s, and observability.
- 🤝 [Contributing](docs/src/contributing/index.md) - How to help improve idp-core.

---

## 🛠️ Technology Stack

- **Java 25** & **Spring Boot**
- **Maven** for build and dependency management
- **PostgreSQL** with Flyway for migrations
- **OpenAPI / Swagger** for API documentation
- **Zensical** for documentation
