---
title: Installation
description: Install the Internal Developer Platform to test the product
---

This guide covers multiple ways to install and run the Internal Developer Platform in your local environment for testing and evaluation.

---

## Using Docker (standard method)

The recommended way to get started with the Internal Developer Platform is using Docker and Docker Compose.

1. **Clone the Repository**

    ```bash
    git clone https://github.com/decathlon/internal-developer-platform.git
    cd internal-developer-platform
    ```

2. **Start the PG database with Docker Compose**

    ```bash
    docker-compose up -d
    ```

3. **Run the Internal Developer Platform Application**

    Once the database runs, start the Internal Developer Platform app by building and running it with Docker:

    ```bash
    mvn clean package -DskipTests
    docker build -t idp-core .
    docker run -p 8084:8084 --env SPRING_PROFILES_ACTIVE=local idp-core
    ```

The app will be accessible at `http://localhost:8084`. In particular, the OpenAPI documentation will be available at `http://localhost:8084/swagger-ui.html`.

---

## Next Steps

- **[Quick Start](quickstart.md)** - Create your first Entity Template
- **[Configuration](configuration.md)** - Customize your installation
