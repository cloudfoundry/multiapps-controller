# multiapps-controller — MTA Deployment Engine

## Project Role

`multiapps-controller` is the core Multi-Target Application (MTA) deployment service for Cloud Foundry. It orchestrates complex multi-app deployments via BPMN-modeled Flowable 7 workflows that drive calls to the CF Cloud Controller API. It depends on the `multiapps` library (`org.cloudfoundry.multiapps`) for MTA model objects and YAML parsing.

## Security Boundary

**OPEN SOURCE** — see the root `CLAUDE.md` security-boundary table. Never introduce proprietary logic, credentials, or internal company context.

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

## BPMN, Process Variables & Liquibase

The root `CLAUDE.md` defers here for the persistence/BPMN conventions:

- **BPMN process definitions** live in
  `multiapps-controller-process/src/main/resources/org/cloudfoundry/multiapps/controller/process/*.bpmn`
  (deploy-app, rollback-mta, execute-tasks, blue-green, etc.).
- **Process variables** are declared in
  `multiapps-controller-process/.../process/variables/Variables.java` as
  `Variable<T>` constants built via `ImmutableSimpleVariable.builder()`.
  These are **serialized into the Flowable DB (ACT_* tables)**: adding a new
  variable is safe, but **renaming or removing one requires a Liquibase migration**
  — a live engine may still hold the old key.
- **Liquibase changelogs** live in
  `multiapps-controller-persistence/.../persistence/db/changelog/`, named
  `db-changelog-<version>-persistence.xml`, and are wired into the master
  `db-changelog.xml` via `<include>`. Add a new versioned file and include it there;
  never edit an already-released changelog.