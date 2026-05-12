---
name: generate-api-contracts
description: Generate and review Aquarius API contract artifacts. Use when backend controllers, request or response DTOs, validation annotations, OpenAPI annotations, endpoint paths, security exposure, or API-facing behavior change and the OpenAPI contract may need to be regenerated.
---

# Generate API Contracts

Use this skill when backend API shape changes may affect `contracts/openapi.json`.

## Project Locations

- API contract output lives at `contracts/openapi.json`.
- SpringDoc OpenAPI generation is configured in `backend/build.gradle.kts`.
- The generation command is:

```powershell
just generate-contracts
```

The command runs:

```powershell
cd backend
./gradlew.bat generateOpenApiDocs
```

Gradle commands require elevated permissions in this environment.

## Workflow

1. Check whether the code change affects public API shape.
2. Run `just generate-contracts` when controllers, DTOs, validation, paths, OpenAPI annotations, or endpoint security exposure changed.
3. Do not edit `contracts/openapi.json` by hand.
4. Review the generated diff in `contracts/openapi.json`.
5. Confirm contract changes match the backend code changes.
6. If the contract did not change when it should have, inspect SpringDoc annotations, controller registration, active profile configuration, and security configuration.

## Review Checklist

Before finishing, confirm:

- `contracts/openapi.json` was generated, not manually edited.
- New, changed, or removed endpoints match the controller changes.
- Request and response schemas match DTO fields and validation annotations.
- Path variables, query parameters, request bodies, and response statuses are represented correctly.
- Security-visible endpoints are intentional.
- Unrelated contract churn is explained or reverted only if it came from the current task.

## Failure Handling

If generation fails:

- Check the Gradle error first.
- Check whether the backend application can start with the local profile used by OpenAPI generation.
- Check whether required local configuration is missing from `backend/.env.local`.
- Report configuration blockers instead of hand-editing the generated contract.
