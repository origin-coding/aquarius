set windows-shell := ["pwsh.exe", "-NoLogo","-Command"]

[windows]
generate-contracts:
   @cd backend; ./gradlew.bat generateOpenApiDocs
