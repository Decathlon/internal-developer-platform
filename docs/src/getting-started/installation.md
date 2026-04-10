---
title: Installation
description: Install IDP-Core to test the product
---

This guide covers multiple ways to install and run IDP-Core in your local environment for testing and evaluation.

---

## Using Maven (standard method)

The recommended way to get started with IDP-Core is using Maven.

1. **Clone the Repository**

    ```bash
    git clone https://github.com/dktunited/idp-core.git
    cd idp-core
    ```

2. **Start the PG database with Docker Compose**

    ```bash
    docker-compose up -d
    ```

3. **Run the IDP-Core Application**

    Once the database runs, start the IDP-Core app by building and running it with Maven:

    ```bash
    mvn spring-boot:run -Dspring-boot.run.profiles=local,secret
    ```

The app will be accessible at `http://localhost:8084`. In particular, the OpenAPI documentation will be available at `http://localhost:8084/swagger-ui.html`.

---

## Next Steps

- **[Quick Start](quickstart.md)** - Create your first Entity Template
- **[Configuration](configuration.md)** - Customize your installation
