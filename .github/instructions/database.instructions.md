---
applyTo: "**/db/migration/**/*.sql"
---

# Database Development

## General Guidelines

- The project uses **PostgreSQL** as the primary database.
- Schema management is handled by **Flyway** for versioned, repeatable migrations.
- **Always** use a Flyway migration script when adding, modifying, or removing fields or constraints in database entities.
- Do not rely on JPA/Hibernate auto-DDL for schema changes.
- JPA/Hibernate `ddl-auto` is set to `none`—all schema changes must go through Flyway migrations.
- The default schema is `idp_core`.
- Use JPA annotations for clarity, but rely on Flyway for actual schema enforcement.

## Flyway Migrations

### Migration Location

All migration scripts are stored in:

```text
src/main/resources/db/migration/
```

### Naming Convention

Migration files must follow Flyway's naming convention:

```text
V<major>_<minor>__<description>.sql
```

| Component         | Description                                              | Example              |
|-------------------|----------------------------------------------------------|----------------------|
| `V`               | Prefix for versioned migrations (required)               | `V`                  |
| `<major>_<minor>` | Version number using underscore separator                | `2_1`                |
| `__`              | Double underscore separator (required)                   | `__`                 |
| `<description>`   | Lowercase snake_case description of the change           | `create_entity_table`|
| `.sql`            | File extension                                           | `.sql`               |

**Examples:**

- `V1_1__create_property_rules_table.sql`
- `V2_3__create_entity_properties_table.sql`
- `V3_1__add_column_to_entity.sql`

### Versioning Strategy

- **Major version**: Increment for new feature domains or breaking changes
- **Minor version**: Increment for additions within a feature domain
- Always check existing migrations to determine the next version number
- Never modify an existing migration that has been applied to any environment

## Writing Migration Scripts

### Required Elements

Every migration script should include:

1. **Header comment** explaining the purpose
2. **DDL statements** for schema changes
3. **Indexes** for foreign keys and frequently queried columns
4. **Comments** on tables and columns for documentation

### Script Template

```sql
-- Flyway migration script: <brief description>
-- Purpose: <detailed explanation of what this migration does>

-- Create table
CREATE TABLE table_name (
    id UUID PRIMARY KEY,
    column_name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better performance
CREATE INDEX idx_table_name_column ON table_name(column_name);

-- Add table comment
COMMENT ON TABLE table_name IS 'Description of the table purpose';

-- Add column comments
COMMENT ON COLUMN table_name.id IS 'Unique UUID identifier';
COMMENT ON COLUMN table_name.column_name IS 'Description of the column';
```

### SQL Style Guidelines

- Use **UPPERCASE** for SQL keywords (`CREATE`, `TABLE`, `NOT NULL`)
- Use **lowercase** with underscores for table and column names (`entity_template`, `created_at`)
- Use **UUID** for primary keys
- Always specify `NOT NULL` constraints explicitly where applicable
- Use `VARCHAR(255)` for string columns unless a specific length is required
- Use `TEXT` for unbounded text content
- Use `TIMESTAMP WITH TIME ZONE` for date/time columns
- Prefer `BOOLEAN` over integer flags

### Constraints and Indexes

- Use meaningful naming for the constraints. Do not use technical or automated naming.

```sql
-- Primary key (defined inline)
id UUID PRIMARY KEY

-- Unique constraint
CONSTRAINT constraint_name UNIQUE (column1, column2)

-- Foreign key
CONSTRAINT fk_name FOREIGN KEY (column_name)
    REFERENCES other_table(id) ON DELETE CASCADE

-- Index on foreign key (always create)
CREATE INDEX idx_table_fk_column ON table_name(fk_column);
```

### Common Patterns

**Adding a column:**

```sql
ALTER TABLE table_name
ADD COLUMN new_column VARCHAR(255);
```

**Adding a NOT NULL column with default:**

```sql
ALTER TABLE table_name
ADD COLUMN new_column VARCHAR(255) NOT NULL DEFAULT 'default_value';
```

**Creating a junction table:**

```sql
CREATE TABLE parent_child (
    parent_id UUID NOT NULL REFERENCES parent(id) ON DELETE CASCADE,
    child_id UUID NOT NULL REFERENCES child(id) ON DELETE CASCADE,
    PRIMARY KEY (parent_id, child_id)
);

CREATE INDEX idx_parent_child_parent ON parent_child(parent_id);
CREATE INDEX idx_parent_child_child ON parent_child(child_id);
```

## Rules

**DO:**

- Test migrations locally before committing
- Write idempotent migrations when possible
- Include rollback comments (even if not automated)
- Create indexes for all foreign key columns
- Add comments to tables and columns
- Use transactions implicitly (Flyway wraps each migration)

**DON'T:**

- Modify migrations already created and applied in any environment
- Use `DROP` statements without careful consideration
- Include DML (INSERT, UPDATE, DELETE) in DDL migrations unless necessary
- Use database-specific syntax that reduces portability
- Create migrations that depend on application code state

## Testing Migrations

1. Run the application locally with `mvn spring-boot:run -Dspring-boot.run.profiles=local`
2. Verify Flyway applies the migration successfully in the logs
3. Connect to the database and verify the schema changes
4. Run the full test suite: `mvn clean verify`

## Flyway Commands

| Command                           | Description                              |
|-----------------------------------|------------------------------------------|
| `mvn flyway:info`                 | Show migration status                    |
| `mvn flyway:validate`             | Validate applied migrations              |
| `mvn flyway:migrate`              | Apply pending migrations                 |
| `mvn flyway:repair`               | Repair the schema history table          |

## Troubleshooting

### Migration Checksum Mismatch

If you see a checksum validation error:

1. **Never** modify the original migration in production
2. For local development only, you can use `mvn flyway:repair`
3. For production, create a new migration to fix the issue

### Migration Already Applied

If a migration was partially applied:

1. Fix the database state manually if needed
2. Run `mvn flyway:repair` to update the schema history
3. Create a new migration if additional changes are needed
