set windows-shell := ["pwsh.exe", "-NoLogo","-Command"]

[linux]
generate-contracts:
  @cd backend && ./gradlew generateOpenApiDocs

[windows]
generate-contracts:
   @cd backend; ./gradlew.bat generateOpenApiDocs
