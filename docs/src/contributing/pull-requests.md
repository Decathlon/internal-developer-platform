---
title: Pull Requests
description: Guidelines for submitting pull requests to IDP-Core
---

This guide covers the process for submitting quality pull requests to IDP-Core.

## Before You Start

### Check Existing Work

1. Search [existing issues](https://github.com/decathlon/internal-developer-platform/issues) for related work
2. Check [open PRs](https://github.com/decathlon/internal-developer-platform/pulls) for similar changes
3. For large changes, open an issue first to discuss

### Sync Your Fork

```bash
git fetch upstream
git checkout main
git merge upstream/main
git push origin main
```

---

## PR Workflow

### 1. Create Feature Branch

```bash
# From updated main
git checkout main
git pull upstream main

# Create branch
git checkout -b feature/my-feature
```

### Branch Naming

| Prefix      | Purpose          | Example                     |
| ----------- | ---------------- | --------------------------- |
| `feature/`  | New features     | `feature/webhook-retry`     |
| `fix/`      | Bug fixes        | `fix/entity-validation`     |
| `docs/`     | Documentation    | `docs/api-examples`         |
| `refactor/` | Code refactoring | `refactor/repository-layer` |
| `test/`     | Test additions   | `test/entity-service`       |

### 2. Make Changes

- Write clean, well-documented code
- Follow [code conventions](code-conventions.md)
- Add/update tests
- Update documentation if needed following [documentation guidelines](documentation.md)

### 3. Commit Changes

#### Commit Message Format

```text
<type>(<scope>): <subject>

<body>

<footer>
```

#### Types

| Type       | Description                |
| ---------- | -------------------------- |
| `feat`     | New feature                |
| `fix`      | Bug fix                    |
| `docs`     | Documentation              |
| `style`    | Formatting, no code change |
| `refactor` | Refactoring                |
| `test`     | Adding tests               |
| `chore`    | Maintenance                |

### 4. Run Checks

Use `mvn` commands to run tests and checks locally. Install `pre-commit` hooks for style checks and other checks that run in the CI the same way.

### 5. Push and Create PR

```bash
git push origin feature/my-feature
```

Then create PR on GitHub.

---

## PR Template

When creating a PR, fill out the template provided to help reviewers understand your changes. Non compliance may delay review.

---

## PR Best Practices

### Keep PRs Small

| PR Size | Lines Changed | Review Time      |
| ------- | ------------- | ---------------- |
| Small   | < 200         | Within the week  |
| Medium  | 200-500       | Within the month |
| Large   | 500+          | No engagement    |

> [!TIP] "Small PRs Get Merged Faster"
> Smaller PRs are easier to review, have fewer bugs, and merge faster.
> Split large changes into multiple PRs when possible.

### One Concern Per PR

```text
✅ Good PR: "Add webhook retry mechanism"
❌ Bad PR: "Add webhook retry, fix entity validation, update docs"
```

### Write Good PR Descriptions

**Include:**

- What changes were made
- Why the changes were made
- How to test the changes

---

## Review Process

### What Reviewers Look For

1. **Correctness** - Does the code work as intended?
2. **Design** - Does it fit the architecture?
3. **Readability** - Is it easy to understand?
4. **Tests** - Are changes adequately tested?
5. **Documentation** - Are docs updated?

### Responding to Reviews

```markdown
# Addressing feedback

✅ "Good point, fixed in abc123"
✅ "I disagree because X. What do you think?"
❌ Ignoring comments
❌ Defensive responses
```

### Review Etiquette

**For Authors:**

- Respond to all comments
- Push fixes as new commits (easier to re-review)
- Request re-review when ready

**For Reviewers:**

- Be constructive and specific
- Explain the "why" behind suggestions
- Approve when ready, don't block on nitpicks

---

## CI/CD Checks

PRs must pass all the automated checks before merging. The review process will not start until checks pass.

---

## After Merge

### Delete Branch

Branches are deleted in the remote repository after merging to keep the repository clean. To keep your local repository tidy, delete the branch locally:

```bash
git checkout main
git pull upstream main
git branch -d feature/my-feature
```

### Sync Fork

```bash
git push origin main
```

---

## Common Issues

### Merge Conflicts

```bash
# Update from upstream
git fetch upstream
git checkout feature/my-feature
git rebase upstream/main

# Resolve conflicts
# Edit conflicted files
git add .
git rebase --continue

# Force push (only on your branch!)
git push origin feature/my-feature --force-with-lease
```

### Updating from Main

```bash
git fetch upstream
git rebase upstream/main
git push origin feature/my-feature --force-with-lease
```

---

## Next Steps

- **[Testing](testing.md)** - Write effective tests
- **[Code Conventions](code/code-conventions.md)** - Follow standards
- **[Development Setup](development-setup.md)** - Set up environment
