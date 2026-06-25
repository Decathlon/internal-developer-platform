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
    StartRelease([GitHub Release Published])

    %% PR-only checks
    subgraph PR_Checks [Pull Request — Additional Checks]
        direction TB
        LintTitle[Lint PR Title]
        LintCode[Lint & Pre-Commit Hooks]
        OpenAPI[OpenAPI Swagger Update Check]
        BreakingChange{Breaking Change?}
        CheckCommit{Announced?}
        AllowBreaking[⚠️ Allow with Warning]
        FailPipeline[❌ Fail Pipeline]

        LintCode --> License2[License Compliance]
        OpenAPI --> BreakingChange
        BreakingChange -->|Yes| CheckCommit
        CheckCommit -->|Yes| AllowBreaking
        CheckCommit -->|No| FailPipeline
        BreakingChange -->|No| PassBreaking[✅ No Breaking Change]
    end

    %% Shared checks — run on both PR and push to main
    subgraph SharedChecks [Basic Code Checks — PR & Main]
        direction TB
        License[License Compliance] --> Build
        Build[Build & Test\nmvn clean verify] --> Sonar[SonarCloud Analysis]
        Build --> Coverage[GitHub Coverage Report]
        Build --> Security[Security Scan\nSpotBugs / OWASP]
        Build --> DockerTest[Docker Image Build Test]
    end

    %% Release / deployment
    subgraph Deploy_Flow [Deployment Workflow — Release only]
        direction TB
        BuildProd[Build Docker Image] --> TagImage[Tag with version & latest]
        TagImage --> PushReg[Push to Docker Hub]
    end

    %% Connections
    StartPR --> LintTitle
    StartPR --> LintCode
    StartPR --> SharedChecks
    SharedChecks --> OpenAPI

    StartMain --> SharedChecks

    StartRelease --> BuildProd

    %% Styling
    classDef trigger fill:#9575cd,stroke:#333,stroke-width:2px,color:white;
    classDef step fill:#e1f5fe,stroke:#01579b,stroke-width:2px;
    classDef decision fill:#fff9c4,stroke:#f57f17,stroke-width:2px;
    classDef success fill:#c8e6c9,stroke:#2e7d32,stroke-width:2px;
    classDef failure fill:#ffcdd2,stroke:#c62828,stroke-width:2px;
    classDef container fill:#fff3e0,stroke:#ff6f00,stroke-width:2px,stroke-dasharray: 5 5;

    class StartPR,StartMain,StartRelease trigger;
    class LintTitle,LintCode,License,License2,Build,Sonar,Coverage,Security,DockerTest,OpenAPI,BuildProd,TagImage,PushReg step;
    class BreakingChange,CheckCommit decision;
    class PassBreaking,AllowBreaking success;
    class FailPipeline failure;
    class SharedChecks,PR_Checks,Deploy_Flow container;
```

## Key Checks

The `Basic Code Checks` workflow runs on every **pull request** targeting `main` and on every **push to `main`**. Some jobs are PR-only.

| Job | Pull Request | Push to `main` |
| --- | :---: | :---: |
| Lint & Pre-Commit Hooks | ✅ | — |
| License Compliance | ✅ | ✅ |
| Build, Test & SonarCloud Analysis | ✅ | ✅ |
| GitHub Coverage Report | ✅ | ✅ |
| Security Scan (SpotBugs / OWASP) | ✅ | ✅ |
| OpenAPI Breaking Change Check | ✅ | — |
| Docker Image Build Test | ✅ | ✅ |

1. **Conventional Commits**: PR titles must follow `feat:`, `fix:`, etc. We also control the scope, for example `feat(domain): ...`.
2. **Breaking Changes**: We use `oasdiff` to check for API breaking changes.
    * **Announce** breaking changes in the commit message, for example `feat(domain)!: remove endpoint`.
    * Unannounced breaking changes **fail the pipeline**.
3. **Tests**: Unit tests and integration tests using Testcontainers must pass. Coverage should be at least 80%.
4. **Coverage reporting**: SonarCloud and GitHub quality both receive coverage data on every push to `main` and on every PR.

## Release Process

1. Create a stable GitHub release with a semantic version tag, for example `v1.4.0`.
2. The `Build and Push to Docker Hub` workflow starts on the `release` event (type `released`).
3. The pipeline runs `mvn clean verify`, then builds and pushes Docker images:
    * `decathlon/internal-developer-platform:<release-tag>`
    * `decathlon/internal-developer-platform:latest`
4. Pull the versioned image when you need reproducible deployments and use `latest` for quick evaluation.

## Next Steps

* **[Contributing Overview](../contributing/index.md)** - Learn how to contribute to IDP-Core.
* **[Development Setup](development-setup.md)** - Set up your local environment for development and testing.
