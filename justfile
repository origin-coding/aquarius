set windows-shell := ["pwsh.exe", "-NoLogo","-Command"]

[windows]
generate-contracts:
   @cd backend; ./gradlew.bat generateOpenApiDocs
   @node scripts/merge-openapi.mjs

[windows]
migration description="fill_in_description":
   $dir = "backend/src/main/resources/db/migration"; New-Item -ItemType Directory -Force -Path $dir | Out-Null; $safeDescription = "{{description}}" -replace "[^A-Za-z0-9_]+", "_"; $version = Get-Date -Format "yyyyMMddHHmmss"; $path = Join-Path $dir "V$version`__$safeDescription.sql"; New-Item -ItemType File -Path $path -ErrorAction Stop | Out-Null; Write-Output $path
