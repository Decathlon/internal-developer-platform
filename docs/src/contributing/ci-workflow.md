---
title: CI Workflow
description: Pipeline checks and build process for IDP-Core
---

## 📈 Visual Workflow

```mermaid
flowchart TD
    %% Triggers
    StartPR([PR Created/Updated])
    StartMain([Push to Main])

    %% PR Workflow
    subgraph PR_Checks [Pull Request Checks]
        direction TB
        LintTitle[Lint PR Title]
        Sonar[SonarCloud Analysis]

        subgraph BasicChecks [Basic Code Checks Workflow]
            LintCode[Lint & Pre-Commit Hooks] --> License[License Compliance Check]
            License --> UnitTests[Run Unit Tests]
            UnitTests --> IntTests[Run Integration Tests]
            IntTests --> BuildApp[Build Application]
            BuildApp --> BreakingChange{Breaking Change?}
            BreakingChange -->|Yes| CheckCommit{Announced?}
            CheckCommit -->|Yes| AllowBreaking[⚠️ Allow with Warning]
            CheckCommit -->|No| FailPipeline[❌ Fail Pipeline]
            BreakingChange -->|No Breaking| PassCheck[✅ Pass Check]
        end
    end

    %% Main Workflow
    subgraph Deploy_Flow [Deployment Workflow]
        direction TB
        BuildProd[Build Docker Image] --> TagImage[Tag with SHA & Latest]
        TagImage --> PushReg[Push to Registry]
    end

    %% Connections
    StartPR --> LintTitle
    StartPR --> Sonar
    StartPR --> LintCode

    StartMain --> BuildProd

    %% Styling
    classDef trigger fill:#9575cd,stroke:#333,stroke-width:2px,color:white;
    classDef step fill:#e1f5fe,stroke:#01579b,stroke-width:2px;
    classDef decision fill:#fff9c4,stroke:#f57f17,stroke-width:2px;
    classDef success fill:#c8e6c9,stroke:#2e7d32,stroke-width:2px;
    classDef failure fill:#ffcdd2,stroke:#c62828,stroke-width:2px;
    classDef container fill:#fff3e0,stroke:#ff6f00,stroke-width:2px,stroke-dasharray: 5 5;

    class StartPR,StartMain trigger;
    class LintTitle,Sonar,LintCode,License,UnitTests,IntTests,BuildApp,BuildProd,TagImage,PushReg step;
    class BreakingChange,CheckCommit decision;
    class PassCheck,AllowBreaking success;
    class FailPipeline failure;
    class BasicChecks,Deploy_Flow container;
```

## Key Checks

1. **Conventional Commits**: PR titles must follow `feat:`, `fix:`, etc. We also control the scope, for example `feat(domain): ...`.
2. **Breaking Changes**: We use `oasdiff` to check for API breaking changes.
    * **Announce** breaking changes in the commit message, for example `feat(domain)!: remove endpoint`.
    * Unannounced breaking changes **fail the pipeline**.
3. **Tests**: Unit tests and Integration tests using Testcontainers must pass. Coverage should be at least 80%.

## Release Process

1. Create a stable GitHub release with a semantic version tag, for example `v1.4.0`.
2. The `Build and Push to Docker Hub` workflow starts on the `released` event.
3. The pipeline runs `mvn clean verify`, then builds and pushes Docker images:
    * `decathlon/internal-developer-platform:<release-tag>`
    * `decathlon/internal-developer-platform:latest`
4. Pull the versioned image when you need reproducible deployments and use `latest` for quick evaluation.
