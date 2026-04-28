# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Role

`multiapps-controller` is the core Multi-Target Application (MTA) deployment service for Cloud Foundry. It orchestrates complex multi-app deployments via BPMN-modeled Flowable 7 workflows that drive calls to the CF Cloud Controller API. It depends on the `multiapps` library (`org.cloudfoundry.multiapps`) for MTA model objects and YAML parsing.

## Security Boundary

This is an **OPEN SOURCE** repository. Never introduce proprietary logic, credentials, or internal company context into this codebase.

## Tech Stack

- **Java 25**, multi-module Maven (11 modules), Spring 6 / Spring Security 6, deployed as a WAR
- **Flowable 7** for BPMN process orchestration (deploy, undeploy, blue-green deploy workflows)
- **EclipseLink 4** (JPA) with static weaving via `staticweave-maven-plugin` at compile time
- **Liquibase** for DB schema migrations; **HikariCP** for connection pooling
- **Immutables** for generated value objects — IDE annotation processors must be enabled
- Cloud object store integrations: AWS S3, Azure Blob, GCP, Alibaba OSS (via Apache jclouds + native SDKs)
- **JaCoCo** + **SonarCloud** for coverage/quality (profiles: `coverage`, `sonar`)

## Module Map

| Module | Purpose |
|---|---|
| `multiapps-controller-api` | Swagger-generated REST API models and endpoint interfaces |
| `multiapps-controller-web` | REST controllers; builds the deployable WAR |
| `multiapps-controller-process` | Flowable BPMN definitions and step implementations |
| `multiapps-controller-core` | Domain model, CF client wrappers, core services |
| `multiapps-controller-persistence` | File artifact storage (DB + object store) |
| `multiapps-controller-client` | Extends cf-java-client with OAuth, retries, extra models |
| `multiapps-controller-database-migration` | Tooling to migrate data between DB instances |
| `multiapps-controller-shutdown-client` | Client for the graceful shutdown API |
| `multiapps-controller-core-test` | Shared test fixtures for core |
| `multiapps-controller-persistence-test` | Shared test fixtures for persistence |
| `multiapps-controller-coverage` | Aggregates JaCoCo reports across all modules |

## Build & Test Commands

```bash
# Full build (compiles, runs unit tests, packages WAR)
mvn clean install

# Skip tests for faster packaging
mvn clean install -DskipTests

# Run unit tests only (integration tests excluded by surefire config)
mvn clean test

# Run a single test class
mvn test -pl multiapps-controller-process -Dtest=MyStepTest

# Build a specific module and its dependencies
mvn clean install -pl multiapps-controller-web --also-make

# Coverage report (aggregate in multiapps-controller-coverage/target/)
mvn clean install -P coverage

# Sonar analysis
mvn verify sonar:sonar -P sonar
```

The deployable WAR is at `multiapps-controller-web/target/multiapps-controller-web-<version>.war`.

Integration tests (`**/*IntegrationTest`) are excluded from the default surefire run.

## Formatting Rule

Before completing any task or committing code, you **MUST** run:

```bash
mvn spotless:apply
```