plugins {
	kotlin("jvm") version "2.3.20"
	kotlin("plugin.spring") version "2.3.20"
	id("org.springframework.boot") version "4.0.6"
	id("io.spring.dependency-management") version "1.1.7"
	kotlin("plugin.jpa") version "2.3.20"

	// Browse ksp versions at: https://repo.maven.apache.org/maven2/com/google/devtools/ksp/symbol-processing-api
	id("com.google.devtools.ksp") version "2.3.6"
}

group = "com.origincoding"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(25)
	}
}

repositories {
	maven { url = uri("https://maven.aliyun.com/repository/central") }
	maven { url = uri("https://maven.aliyun.com/repository/public") }
	maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
	maven { url = uri("https://maven.aliyun.com/repository/apache-snapshots") }
	mavenCentral()
	mavenLocal()
}

extra["springModulithVersion"] = "2.0.6"
extra["awsSdkVersion"] = "2.42.40"

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-cache")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-data-redis")
	implementation("org.springframework.boot:spring-boot-starter-flyway")
	implementation("org.springframework.boot:spring-boot-starter-opentelemetry")
	implementation("org.springframework.boot:spring-boot-starter-restclient")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.flywaydb:flyway-database-postgresql")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-api:3.0.2")
	implementation("org.springframework.modulith:spring-modulith-starter-core")
	implementation("org.springframework.modulith:spring-modulith-starter-jpa")
	implementation("tools.jackson.module:jackson-module-kotlin")
	developmentOnly("org.springframework.boot:spring-boot-devtools")
	runtimeOnly("org.postgresql:postgresql")
	runtimeOnly("org.springframework.modulith:spring-modulith-actuator")
	runtimeOnly("org.springframework.modulith:spring-modulith-observability")
	runtimeOnly("org.springframework.modulith:spring-modulith-runtime")
	annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
	testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")
	testImplementation("org.springframework.boot:spring-boot-starter-cache-test")
	testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
	testImplementation("org.springframework.boot:spring-boot-starter-data-redis-test")
	testImplementation("org.springframework.boot:spring-boot-starter-flyway-test")
	testImplementation("org.springframework.boot:spring-boot-starter-opentelemetry-test")
	testImplementation("org.springframework.boot:spring-boot-starter-restclient-test")
	testImplementation("org.springframework.boot:spring-boot-starter-security-test")
	testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testImplementation("org.springframework.modulith:spring-modulith-starter-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")

	// Kotlin logging
	implementation("io.github.oshai:kotlin-logging-jvm:8.0.01")
	// Redisson
	implementation("org.redisson:redisson-spring-boot-starter:4.3.0")
	// QueryDSL
	implementation("io.github.openfeign.querydsl:querydsl-core:7.1")
	implementation("io.github.openfeign.querydsl:querydsl-jpa:7.1")
	ksp("io.github.openfeign.querydsl:querydsl-ksp-codegen:7.1")
	// Easy Captcha & nashorn-core for arithmetic expression
	implementation("com.github.whvcse:easy-captcha:1.6.2")
	implementation("org.openjdk.nashorn:nashorn-core:15.7")
	// Logback otlp appender
	implementation("io.opentelemetry.instrumentation:opentelemetry-logback-appender-1.0:2.21.0-alpha")
	// Jetbrains annotations
	compileOnly("org.jetbrains:annotations:26.1.0")
	// AWS SDK for Java v2
	implementation("software.amazon.awssdk:s3")
}

dependencyManagement {
	imports {
		mavenBom("org.springframework.modulith:spring-modulith-bom:${property("springModulithVersion")}")
		mavenBom("software.amazon.awssdk:bom:${property("awsSdkVersion")}")
	}
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
	}
}

allOpen {
	annotation("jakarta.persistence.Entity")
	annotation("jakarta.persistence.MappedSuperclass")
	annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
	useJUnitPlatform()
}
