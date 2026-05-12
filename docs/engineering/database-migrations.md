# Database Migration Guidelines

This document defines how Aquarius database migrations are created, reviewed, and maintained.

## Scope

Aquarius uses Flyway SQL migrations for PostgreSQL. Migration files live in:

```text
backend/src/main/resources/db/migration/
```

Use this guideline for any change that creates, changes, or reviews database schema, indexes, constraints, seed data, or data backfill steps.

## File Naming

Migration filenames must follow:

```text
V{yyyyMMddHHmmss}__{description}.sql
```

Examples:

```text
V20260512105422__init_iam_tables.sql
V20260512143010__add_user_profile_columns.sql
```

Use lowercase snake_case for `{description}`. Keep the description short and specific.

Create files with:

```powershell
just migration {description}
```

Examples:

```powershell
just migration init_iam_tables
just migration add_user_profile_columns
```

The helper creates the timestamped file in the Flyway migration directory. Prefer the helper over manual file creation.

## Before Writing SQL

Before writing or changing a migration:

1. Inspect existing migration files in chronological order.
2. Inspect the current Kotlin entity classes, including work-in-progress files.
3. Check repositories and service/domain code for uniqueness, lookup, and lifecycle assumptions.
4. Identify the owning module of every table involved.
5. Confirm whether the change is additive, destructive, or requires data backfill.

Do not infer schema details when the entity model is incomplete or contradictory. Resolve the model first.

## PostgreSQL Dialect

Write PostgreSQL SQL, executed by Flyway.

Preferred type choices:

- Use `VARCHAR(n)` for bounded strings such as names, codes, enum names, emails, token identifiers, and external IDs.
- Use `TEXT` for unbounded text payloads.
- Use `BOOLEAN` for flags.
- Use `INTEGER` or `BIGINT` for numeric counters and versions.
- Use `TIMESTAMP WITH TIME ZONE` for timezone-aware timestamps.
- Use `JSONB` only when structured data is intentionally stored as JSON.
- Use `UUID` only when the application model and existing table conventions store IDs as PostgreSQL UUID values.

Avoid MySQL, Oracle, SQL Server, or H2-specific syntax in migration files.

Use PostgreSQL-specific features only when they are intentional and supported by the runtime database. Examples include partial indexes, `JSONB`, expression indexes, and PostgreSQL extensions.

Do not add `CREATE EXTENSION` casually. Extensions are runtime dependencies and must be an explicit project decision.

## Entity Alignment

Migration SQL must match the entity model.

Check:

- `@Entity`, `@Table`, `@Column`, `@Enumerated`, and relation annotations.
- Shared base classes for IDs, audit columns, soft delete, timestamps, and optimistic locking.
- Enum persistence strategy and column length.
- Repository methods that imply unique lookups or frequently filtered columns.
- Application-level validation that implies database constraints.

If the application uses string IDs for an entity, keep the migration consistent unless the entity model is changed deliberately.

## Module Boundaries

Every table must have a clear owning module.

Tables owned by the same module may use foreign keys to protect data consistency. Those foreign keys are not business workflow mechanisms.

Tables owned by different modules must not use foreign keys between each other. Cross-module references should be plain identifier columns, with consistency enforced by application code, domain services, or integration workflows.

Use table and index names that make module ownership clear. For module-local IAM tables, names such as `iam_user`, `iam_identity`, and `iam_credential` are preferred.

## Foreign Keys

Module-local foreign keys are allowed when they make invariants clearer and safer.

Rules:

- Use explicit constraint names, such as `iam_identity_user_fk`.
- Index foreign-key columns unless an equivalent index already exists.
- Prefer default restrict behavior or explicit `ON DELETE RESTRICT`.
- Do not use cascade deletes to implement business deletion.
- Do not create cross-module foreign keys.

## Delete Strategy

Deletion behavior belongs in the application layer.

Use database constraints to prevent invalid data, not to decide business lifecycle behavior.

For soft-deletable tables:

- Keep `deleted` flags consistent with shared entity base classes.
- Use partial unique indexes when uniqueness should apply only to active rows.
- Ensure queries and repositories account for soft-deleted rows.

For hard deletes:

- Delete dependent rows explicitly in application code.
- Keep the delete order visible in services or domain workflows.
- Use database constraints to catch mistakes, not to hide deletion behavior.

## Constraints And Indexes

Add constraints and indexes deliberately.

Use:

- Primary keys for every table.
- Unique constraints or unique indexes for domain identities.
- Partial unique indexes when soft delete changes uniqueness semantics.
- Indexes for foreign-key columns.
- Indexes for common query filters and ordering paths.

Prefer descriptive names:

```text
{table}_{column}_idx
{table}_{column_a}_{column_b}_uidx
{table}_{referenced_table}_fk
```

Examples:

```sql
CREATE INDEX iam_user_status_idx ON iam_user (status);

CREATE UNIQUE INDEX iam_identity_type_normalized_identity_uidx
    ON iam_identity (identity_type, normalized_identity)
    WHERE deleted = FALSE;
```

Do not add indexes speculatively. Every index should support a known constraint, lookup, filter, or ordering path.

## Nullability And Defaults

Use `NOT NULL` when the domain requires a value.

When adding a `NOT NULL` column to a table with existing data, use a safe migration path:

1. Add the column as nullable or with a safe default.
2. Backfill existing rows.
3. Add the `NOT NULL` constraint.
4. Remove temporary defaults if they are not part of the domain model.

For new tables, define final nullability directly.

Defaults should represent real domain defaults, not merely make migration writing easier.

## Changing Existing Migrations

Do not edit a migration that may already have been applied by another environment.

Allowed cases:

- The migration is local-only and has not been shared or applied elsewhere.
- The user explicitly asks to rewrite history and accepts the risk.

Otherwise, create a new migration that changes the schema from the previous version to the new version.

## Destructive Changes

Treat these as high-risk:

- Dropping tables.
- Dropping columns.
- Renaming tables or columns.
- Rewriting column types.
- Changing primary keys.
- Rebuilding uniqueness rules.
- Backfilling or deleting production data.

Before making a destructive change, document the intent and migration path. Prefer phased migrations that keep the application deployable between versions.

## Review Checklist

Before merging or finishing a migration change, confirm:

- The file is in `backend/src/main/resources/db/migration/`.
- The filename matches `V{yyyyMMddHHmmss}__{description}.sql`.
- The file was generated with `just migration {description}` when practical.
- The migration version is newer than existing migrations.
- The SQL uses PostgreSQL syntax.
- Entity classes and migration columns match.
- Module ownership is clear for every table.
- No cross-module foreign keys were added.
- Module-local foreign keys are indexed and explicitly named.
- Deletes are handled by application logic, not cascade behavior.
- `NOT NULL` additions are safe for existing data.
- Indexes and constraints support real application behavior.
- Destructive changes have an explicit migration strategy.

## Agent Usage

AI agents working on database migrations must read:

```text
.agents/skills/write-db-migrations/SKILL.md
```

The Skill contains the operational checklist. This document is the project policy.
