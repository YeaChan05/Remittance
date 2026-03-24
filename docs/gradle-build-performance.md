# Gradle Build Performance: Shared Containers for Parallel Integration Tests

## 1. Goal

Reduce `integrationTest` wall-clock time while keeping integration tests inside `build` and `check`.

- Integration tests remain part of `check`.
- Major integration test tasks should run in parallel.
- Docker is required. If Docker is unavailable, integration tests and any `build` that includes them
  fail immediately.
- Test state must stay isolated even when containers are shared.

The current parallelization target is:

- `:account:repository-jpa:integrationTest`
- `:member:repository-jpa:integrationTest`
- `:transfer:repository-jpa:integrationTest`
- `:aggregate:integrationTest`

## 2. Design Summary

The previous task-scoped container model is replaced with build-wide shared containers.

- One shared MySQL container is created per Gradle invocation.
- One shared RabbitMQ container is created per Gradle invocation.
- MySQL state isolation is handled by creating a dedicated database for each integration test task.
- RabbitMQ currently serves only `:aggregate:integrationTest`, so it uses the default vhost.
- Tasks still receive connection information through Gradle-provided system properties.

This design keeps startup cost near one container boot per build while avoiding DB state sharing
between parallel tasks.

## 3. Build Logic

### 3.1 Root Build

- `build.gradle.kts`
    - Applies the root `buildlogic.testcontainers-support` plugin.
    - Creates `integrationTest` suites only for modules that actually contain `src/integrationTest`.
- `gradle.properties`
    - Enables `org.gradle.parallel=true`.
    - CI-focused application builds keep `bootJar` and disable distribution archive tasks such as
      `bootDistZip`, `distTar`, and `startScripts`.

### 3.2 Shared Environment Services

- `TestcontainersPlugin`
    - Exposes a `testcontainers` DSL to each project.
    - Wires only the declared `Test` tasks from each module's `build.gradle.kts`.
    - Delegates task-specific provider resolution and runtime wiring to `TaskContainerBinder`.
    - Registers one shared container service for the whole build.

- `SharedContainerService`
    - Verifies Docker availability once for the build and fails immediately when Docker is
      unavailable.
    - Lazily starts one shared runtime per declared provider key.
    - Caches runtimes by provider key and Testcontainers runtime coordinates.

- `MySqlSharedContainerProvider`
    - Lazily starts one shared MySQL container.
    - Creates a dedicated database for each task path, for example:
        - `account_repository_jpa_integration_test`
        - `member_repository_jpa_integration_test`
        - `transfer_repository_jpa_integration_test`
        - `aggregate_integration_test`
    - Injects `spring.datasource.*` values that point at the task-specific database.

- `RabbitMqSharedContainerProvider`
    - Lazily starts one shared RabbitMQ container.
    - Injects host, port, username, and password for `:aggregate:integrationTest`.

## 4. Test Wiring

Tests do not create containers directly. They bind only to Spring properties.

- Each module opts in from its own `build.gradle.kts`, for example:

```kotlin
testcontainers {
    task("integrationTest") {
        use("mysql")
    }
}
```

```kotlin
testcontainers {
    task("integrationTest") {
        use("mysql")
        use("rabbitmq")
    }
}
```

- Repository integration tests rely on their existing Liquibase changelogs.
- Aggregate integration tests keep using `IntegrationTestEnvironmentSetup` with
  `@DynamicPropertySource`.
- `IntegrationTestEnvironmentSystemProperties` still requires Gradle-provided system properties and
  fails immediately when they are missing.

Because each integration test task gets its own MySQL database, Liquibase lock tables and change
history stay isolated across parallel tasks.

## 5. Execution Flow

```mermaid
flowchart TD
    A["./gradlew integrationTest / build"] --> B["Apply root integration test plugin"]
    B --> C["Identify target integrationTest tasks"]
    C --> D["Register shared MySQL service"]
    C --> E["Register shared RabbitMQ service"]
    D --> F["Task doFirst"]
    E --> F
    F --> G["Verify Docker availability"]
    G -->|Unavailable| H["Fail immediately"]
    G -->|Available| I["Start shared MySQL lazily"]
    I --> J["Create task-specific database"]
    J --> K["Inject spring.datasource.*"]
    F -->|aggregate only| L["Start shared RabbitMQ lazily"]
    L --> M["Inject spring.rabbitmq.*"]
    K --> N["Spring Boot + Liquibase startup"]
    M --> N
    N --> O["Run tests"]
```

## 6. Expected Tradeoffs

### Benefits

- Container startup cost is paid once per Gradle invocation instead of once per task.
- Parallel execution becomes more effective in CI cold-start environments.
- DB state remains isolated because each task uses its own database.

### Tradeoffs

- Multiple tasks still compete for the same MySQL and RabbitMQ container resources.
- `:aggregate:integrationTest` remains relatively expensive because it boots several Spring
  contexts.
- Docker is mandatory for local and CI execution paths that include integration tests.

## 7. Verification Commands

Use the following commands to validate both correctness and performance:

```bash
./gradlew --parallel integrationTest --rerun-tasks
./gradlew --parallel build --rerun-tasks --profile
./gradlew build jacocoRootReport
```

Profile reports are generated under `build/reports/profile/`.
