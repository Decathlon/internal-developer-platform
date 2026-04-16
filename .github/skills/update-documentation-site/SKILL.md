---
name: update-documentation-site
description: 'Create or update documentation pages for the IDP-Core documentation site. Use this when asked to write, edit, or restructure content in the docs/src/ folder.'
---

# Update Documentation Site

Create or update documentation for IDP-Core's public documentation site, located in `docs/src/`.

The site is powered by **Zensical** (MkDocs-compatible) and published as an open source project documentation. All content targets developers who build, deploy, or contribute to IDP-Core.

> Writing style, formatting rules, and markdown standards are defined in the `documentation.instructions.md` instruction file and apply automatically when editing `docs/src/**/*.md`. This skill focuses on the **workflow** and **validation** steps.

## Inputs

- **Action**: `${input:Action}`—One of: `create` (new page), `update` (edit existing page), `restructure` (move/reorganize pages)
- **Section**: `${input:Section}`—Target section: `getting-started`, `concepts`, `features`, `api`, `deployment`, `contributing`
- **Topic**: `${input:Topic}`—Subject of the documentation page or change

## Input Validation

If the action, section, or topic cannot be determined from the conversation, ask the user before proceeding. Verify the target file exists before updating; verify it does not exist before creating.

## Site Architecture

### Directory Layout

```text
docs/
├── zensical.toml          # Site configuration and navigation
├── pyproject.toml          # Python dependencies (Zensical, plugins)
└── src/                    # All documentation source files
    ├── index.md            # Homepage
    ├── getting-started/    # Installation, quickstart, configuration
    ├── concepts/           # Entity templates, entities, properties, relations
    ├── features/           # Data integration, scorecards, self-service actions
    ├── api/                # API reference (Swagger UI)
    ├── deployment/         # Docker, Kubernetes, observability, configuration
    ├── contributing/       # Development setup, code architecture, ADRs, testing
    │   ├── code/           # Code conventions, best practices, exception handling
    │   └── adrs/           # Architecture Decision Records
    ├── javascripts/        # MathJax, Mermaid initialization
    └── static/             # swagger.yaml and other static assets
```

### Navigation

Navigation is defined in `docs/zensical.toml` under the `nav` key. When creating a new page, you **must** add it to the `nav` array in the correct section.

### Technology Stack

| Tool                 | Purpose                                    |
| -------------------- | ------------------------------------------ |
| Zensical             | Static site generator (MkDocs-compatible)  |
| Markdown             | Content format                             |
| Mermaid              | Diagrams (flowchart, sequence, ER, class)  |
| Swagger UI           | Embedded API documentation                 |
| MathJax              | Mathematical equations                     |
| Vale (Google style)  | Prose linting                              |

## Workflow

### Creating a New Page

1. Create the markdown file in the appropriate `docs/src/<section>/` directory
2. Write content following the writing instructions (auto-applied from `documentation.instructions.md`)
3. Add the page to the `nav` array in `docs/zensical.toml` under the correct section
4. Verify internal links to and from the new page
5. Run the validation steps below

### Updating an Existing Page

1. Read the current content of the target page
2. Apply changes while preserving the existing structure and style
3. If new sections are added, verify heading hierarchy
4. If the page was renamed or moved, update all internal links and the `nav` in `zensical.toml`
5. Run the validation steps below

### Restructuring

1. Identify all affected pages and their cross-references
2. Move or rename files in `docs/src/`
3. Update the `nav` in `docs/zensical.toml`
4. Update all internal links across the documentation
5. Run the validation steps below

## Validation

After any documentation change, run the following validation steps **in order**. Fix issues before moving to the next step.

The project uses [pre-commit](https://pre-commit.com/) (configured in `/.pre-commit-config.yaml`) as the standard way to run all linters. Hooks cover trailing whitespace, YAML/JSON validity, markdownlint, and Vale.

### Step 1: Markdown and Prose Linting

Run pre-commit hooks on the changed documentation files:

```bash
# Run all hooks on specific changed files
pre-commit run --files docs/src/path/to/changed-file.md

# Run all hooks on all documentation files
pre-commit run --files $(git ls-files docs/src/)
```

To run only a specific hook:

```bash
# Markdown linting only (/.markdownlint.yaml rules)
pre-commit run markdownlint --files docs/src/path/to/changed-file.md

# Vale prose linting only (Google style + IDP vocabulary from /.vale.ini)
pre-commit run vale --files docs/src/path/to/changed-file.md
```

Common issues and fixes:

- **Trailing whitespace** → removed automatically by the `trailing-whitespace` hook on re-run
- **Heading increment** → ensure heading levels increase by one at a time
- **Passive voice** → rewrite in active voice
- **Future tense ("will")** → use present tense
- **"the user"** → replace with "you"
- **Unknown project term** → add it to `.vale/styles/Vocab/IDP/accept.txt`

To run all hooks on the entire repository (useful before opening a PR):

```bash
pre-commit run --all-files
```

### Step 2: Site Build Verification

Build the full site in strict mode to catch broken links, missing nav entries, and rendering errors:

```bash
cd docs

# Install dependencies if not done
uv sync

# Build in strict mode (treats warnings as errors)
uv run zensical build --strict
```

Common build errors and fixes:

- **"Page not in nav"** → add the file to `docs/zensical.toml` under the `nav` key
- **"Link to … not found"** → fix the relative path in the markdown link
- **"File not found"** → verify the file exists at the referenced path

### Step 3: Visual Review (optional)

For significant changes, preview the site locally:

```bash
cd docs
uv run zensical serve
```

Open <http://localhost:8000> and verify:

- Page renders correctly
- Mermaid diagrams display
- Admonitions and tabs work
- Navigation links are correct

## Validation Checklist

- [ ] `pre-commit run --files <changed-files>` passes all hooks
- [ ] Site build succeeds in strict mode (`uv run zensical build --strict`)
- [ ] Navigation entry exists in `zensical.toml` (for new pages)
- [ ] Front matter includes `title` and `description`
- [ ] Internal links resolve correctly
- [ ] Code examples are syntactically valid
