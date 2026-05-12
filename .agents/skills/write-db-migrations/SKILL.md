---
name: write-db-migrations
description: Create, update, and review Aquarius database migration files. Use when an AI agent needs to add schema changes, generate a Flyway SQL migration, inspect migration naming, align migrations with Kotlin entity classes, or review database integrity rules for module-local and cross-module tables.
---

# Write DB Migrations

Use this skill when creating or reviewing database migration files for Aquarius.

## Project Locations

- Migration files live in `backend/src/main/resources/db/migration/`.
- Backend entity classes live under `backend/src/main/kotlin/com/origincoding/aquarius/`.
- The project migration policy lives in `docs/engineering/database-migrations.md`.
- Migrations target PostgreSQL and run through Flyway.
- The migration helper command is:

```powershell
just migration {description}
```

The helper creates a file named:

```text
V{yyyyMMddHHmmss}__{description}.sql
```

Use lowercase snake_case descriptions, for example:

```powershell
just migration init_iam_tables
just migration add_user_profile_columns
```

## Workflow

1. Inspect existing migration files in `backend/src/main/resources/db/migration/`.
2. Read `docs/engineering/database-migrations.md`.
3. Inspect the entity classes currently being worked on before writing SQL.
4. Identify the owning module of every table involved.
5. Generate the migration with `just migration {description}` unless the user explicitly asks to create the file manually.
6. Write SQL that matches the entity model, aggregate boundaries, and module ownership.
7. Re-check table names, column names, indexes, constraints, nullability, defaults, and data types against the application code.
8. Verify the migration is forward-only and safe to run once in chronological order.

## Database Dialect

Write migrations using PostgreSQL SQL syntax.

- Prefer PostgreSQL-native types when they match the entity model, such as `uuid`, `text`, `varchar(n)`, `boolean`, `integer`, `bigint`, `timestamptz`, and `jsonb`.
- Use `timestamptz` for timezone-aware timestamps unless existing project tables use a different convention.
- Use `gen_random_uuid()` only if the project has enabled the required PostgreSQL extension or existing migrations already rely on it.
- Add PostgreSQL indexes explicitly with `CREATE INDEX` or `CREATE UNIQUE INDEX` when needed.
- Use `CREATE EXTENSION` only when the extension is a deliberate project dependency.
- Avoid MySQL, Oracle, SQL Server, or H2-specific syntax in migration files.

## Entity Inspection

Before writing SQL, inspect relevant Kotlin entity classes and repositories, including work-in-progress files. Do not rely only on committed code.

Check for:

- `@Entity`, `@Table`, `@Column`, `@Enumerated`, relation annotations, and embedded types.
- Base classes such as audit fields, IDs, timestamps, soft-delete fields, or version fields.
- Enum storage strategy and expected column length.
- Unique constraints implied by domain logic, repository methods, or service validation.
- Required indexes for lookup columns, unique identities, foreign-key columns, and frequent filters.

If the entity model is incomplete or internally inconsistent, stop and report the mismatch before inventing schema details.

## Foreign Key Rules

Use foreign keys only inside a single module boundary.

Within the same module:

- Foreign keys are allowed to protect data consistency.
- Prefer explicit constraint names.
- Add indexes for foreign-key columns unless the database automatically creates suitable indexes.
- Do not depend on foreign-key cascade deletes as the business deletion mechanism.

Across modules:

- Do not create foreign keys between tables owned by different modules.
- Store cross-module references as plain identifier columns.
- Enforce cross-module consistency in the application layer, domain services, or integration workflow.
- Prefer names that make ownership clear, such as `user_id`, `tenant_id`, or `{module}_id`, without adding a cross-module constraint.

## Delete Rules

Do not rely on database cascade deletion for business behavior.

- Handle deletes, detachments, cleanup, and invariant checks in the application layer.
- Use `ON DELETE RESTRICT` or the database default for module-local foreign keys unless the project has a documented exception.
- Use soft delete when the entity model or business rules require retention.
- For hard deletes, make the application delete dependent records explicitly in a controlled order.

## SQL Guidelines

- Prefer additive, backward-compatible changes.
- Do not edit an already-applied migration unless the user explicitly requests it and understands the risk.
- Keep one migration focused on one coherent schema change.
- Use stable, descriptive constraint and index names.
- Make `NOT NULL` additions safe for existing data by using defaults, backfill steps, or phased migrations.
- Use PostgreSQL-specific features only when they are intentional, supported by the runtime database, and clearer than portable SQL.
- Use explicit column lengths for strings that represent enums, codes, names, emails, tokens, or external IDs.
- Add unique constraints for natural identities only when the domain requires global uniqueness.
- Avoid storing encrypted or hashed values in columns named like plain text.
- Keep timestamps, audit fields, and optimistic-lock fields consistent with shared entity base classes.

## Review Checklist

Before finishing, confirm:

- The filename matches `V{yyyyMMddHHmmss}__{description}.sql`.
- The file was generated with `just migration {description}` when possible.
- The migration order is newer than existing migrations.
- Every table belongs to a clear module.
- No cross-module foreign keys were added.
- Module-local foreign keys are used only for consistency, not deletion workflow.
- Indexes support foreign keys, unique lookups, and common query paths.
- Nullability and defaults are safe for existing and future data.
- SQL matches the current entity classes, including work-in-progress files.
- The migration can run once on a clean database and on an existing database at the previous version.

## When Unsure

Ask or report a blocker when:

- Module ownership is ambiguous.
- Entity classes and the intended schema disagree.
- A required data backfill cannot be inferred safely.
- A cross-module relationship appears to need referential integrity.
- A destructive change would drop, rename, or rewrite existing data.
