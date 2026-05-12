# Aquarius Agent Guide

Read this file before making changes in this repository.

## Project Agent Assets

- Project-level reusable skills live under `.agents/skills/`.
- Human-readable engineering guidelines should live under `docs/engineering/`.
- Database migration policy lives in `docs/engineering/database-migrations.md`.
- Database migration work should use `.agents/skills/write-db-migrations/SKILL.md`.

## Tool Permissions

Some development tools require elevated execution in this environment.

- Run Python through `py` with elevated permissions. The non-elevated sandbox may resolve `python`, `python3`, or `py` to the WindowsApps launcher and fail before the interpreter starts.
- Run Gradle commands with elevated permissions. Gradle may need to write caches outside the workspace and may need network access for dependencies.
- For Codex shell commands, use `sandbox_permissions: "require_escalated"` when running Python or Gradle commands.

Examples:

```powershell
py --version
py C:/Users/origin-coding/.codex/skills/.system/skill-creator/scripts/quick_validate.py .agents/skills/write-db-migrations
```

```powershell
cd backend
./gradlew.bat test
```

## Repository Rules

- Do not revert user changes unless explicitly asked.
- Keep IAM implementation changes separate from documentation and agent-guideline commits.
- Before creating or editing database migrations, read `docs/engineering/database-migrations.md` and `.agents/skills/write-db-migrations/SKILL.md`.
- Before creating or editing database migrations, inspect current entity classes and existing migration files.
- Do not add foreign keys across module boundaries.
