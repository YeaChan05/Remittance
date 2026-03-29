# Gradle 빌드 성능 문서

## 1. 목적

이 문서는 현재 레포의 Gradle 통합 테스트가 어떤 단위로 병렬화되고, Testcontainers를 어떤 생명주기로 공유하는지 설명한다.

- `integrationTest`는 여전히 `check`, `build`에 포함된다.
- 도메인 간 task는 병렬로 실행한다.
- 같은 도메인 안의 `integrationTest`는 `repository-jpa -> application` 순서로 직렬 실행한다.
- 같은 도메인의 `integrationTest`가 진행되는 동안에는 도메인 stack 단위로 컨테이너를 공유한다.
- 해당 도메인의 마지막 `integrationTest`가 끝나면 컨테이너를 즉시 정리한다.

## 2. 현재 적용 대상

루트 [build.gradle.kts](/build.gradle.kts) 는 `src/integrationTest`가 있는 모듈에만 `integrationTest` 스위트를
생성한다.

현재 대상:

- `account:repository-jpa`
- `account:application`
- `member:repository-jpa`
- `member:application`
- `transfer:repository-jpa`
- `transfer:application`

`aggregate`는 현재 `src/integrationTest`가 없으므로 이 정책의 직접 대상이 아니다.

## 3. 도메인 stack 규칙

각 모듈은 자기 `integrationTest`에 사용할 shared container stack을 명시한다.

- `account`
    - `account:repository-jpa:integrationTest` -> `use("mysql")`
    - `account:application:integrationTest` -> `use("mysql")`
- `member`
    - `member:repository-jpa:integrationTest` -> `use("mysql")`
    - `member:application:integrationTest` -> `use("mysql")`
- `transfer`
    - `transfer:repository-jpa:integrationTest` -> `use("mysql")`
    - `transfer:application:integrationTest` -> `use("mysql")`, `use("rabbitmq")`

stack key는 도메인명과 동일하다.

예:

```kotlin
testcontainers {
    bom("org.testcontainers:testcontainers-bom:${libs.versions.testcontainers.get()}")
    task("integrationTest") {
        stack("member")
        use("mysql")
    }
}
```

## 4. 컨테이너 공유 방식

### 4.1 MySQL

MySQL은 이제 task별 `jdbc:tc:` 컨테이너를 직접 띄우지 않고, 도메인 stack당 1개 shared container를 사용한다.

-
provider: [build-logic/src/main/kotlin/org/yechan/remittance/buildlogic/MySqlSharedContainerProvider.kt](/build-logic/src/main/kotlin/org/yechan/remittance/buildlogic/MySqlSharedContainerProvider.kt)
-
registry: [build-logic/src/main/kotlin/org/yechan/remittance/buildlogic/SharedContainerContracts.kt](/build-logic/src/main/kotlin/org/yechan/remittance/buildlogic/SharedContainerContracts.kt)

다만 DB state는 task 간에 공유하지 않는다.

- 컨테이너는 도메인당 1개
- 데이터베이스는 task path 기준으로 분리

예:

- `member_repository_jpa_integrationtest`
- `member_application_integrationtest`

테스트 task에는 다음 system property를 주입한다.

- `spring.datasource.url`
- `spring.datasource.username`
- `spring.datasource.password`
- `spring.datasource.driver-class-name`

기존 `application.yml`의 `jdbc:tc:mysql:...` 값은 fallback으로 남아 있다. shared stack 경로에서는 Gradle이 주입한 system
property가 우선 적용된다.

### 4.2 RabbitMQ

RabbitMQ는 `transfer` 도메인 stack에서만 공유한다.

-
provider: [build-logic/src/main/kotlin/org/yechan/remittance/buildlogic/RabbitMqSharedContainerProvider.kt](/build-logic/src/main/kotlin/org/yechan/remittance/buildlogic/RabbitMqSharedContainerProvider.kt)
- `transfer:application:integrationTest`는 `mysql`, `rabbitmq`를 모두 사용한다.

이 구조 덕분에 `transfer` 도메인은 같은 stack lifetime 안에서 MySQL과 RabbitMQ를 함께 재사용한다.

## 5. 실행 순서

도메인 내부 ordering은 build logic가 중앙에서 구성한다.

- 같은 stack에 `repository-jpa:integrationTest`와 `application:integrationTest`가 모두 있으면
- `application:integrationTest.mustRunAfter(repository-jpa:integrationTest)`를 적용한다.
- `dependsOn`은 사용하지 않는다.

즉, 단독 실행은 그대로 가능하다.

- `:member:application:integrationTest`만 실행 가능
- `:member:repository-jpa:integrationTest`만 실행 가능

하지만 둘 다 graph에 들어오면 순서는 항상 아래와 같다.

```text
member:repository-jpa:integrationTest
member:application:integrationTest
```

## 6. stack lifecycle

shared runtime의 생명주기는 build 전체가 아니라 stack 기준으로 관리한다.

- 실행 graph가 준비되면 stack별 selected `integrationTest` task 목록을 계산한다.
- stack의 첫 task가 시작될 때 필요한 provider runtime을 lazy start 한다.
- 각 task가 끝날 때 stack 진행 상태를 기록한다.
- 마지막 selected task가 끝나면 해당 stack의 runtime을 즉시 close 한다.
- `buildFinished` close는 비정상 종료 시 안전망으로만 남긴다.

관련 구현:

- [build-logic/src/main/kotlin/org/yechan/remittance/buildlogic/TestcontainersPlugin.kt](/build-logic/src/main/kotlin/org/yechan/remittance/buildlogic/TestcontainersPlugin.kt)
- [build-logic/src/main/kotlin/org/yechan/remittance/buildlogic/TaskContainerBinder.kt](/build-logic/src/main/kotlin/org/yechan/remittance/buildlogic/TaskContainerBinder.kt)
- [build-logic/src/main/kotlin/org/yechan/remittance/buildlogic/SharedContainerService.kt](/build-logic/src/main/kotlin/org/yechan/remittance/buildlogic/SharedContainerService.kt)

## 7. 실행 흐름

```mermaid
flowchart TD
    A["./gradlew build 또는 integrationTest"] --> B["integrationTest graph 계산"]
    B --> C["stack별 selected task 목록 결정"]
    C --> D["같은 stack에서는 repository-jpa -> application 순서 적용"]
    D --> E["첫 task doFirst에서 shared runtime lazy start"]
    E --> F["MySQL은 stack당 1개 container 생성"]
    E --> G["transfer는 RabbitMQ도 stack lifetime으로 공유"]
    F --> H["task별 datasource system property 주입"]
    G --> I["spring.rabbitmq.* system property 주입"]
    H --> J["task 종료 시 stack 진행 상태 기록"]
    I --> J
    J --> K["마지막 selected task 종료"]
    K --> L["stack runtime 즉시 close"]
```

## 8. 트레이드오프

### 장점

- 도메인 간 병렬성은 유지한다.
- 같은 도메인 안에서는 컨테이너 기동 비용을 한 번만 지불한다.
- task별 DB 분리로 `repository-jpa`와 `application` 사이의 상태 오염을 막는다.
- `transfer`는 MySQL과 RabbitMQ를 같은 stack lifetime으로 재사용할 수 있다.

### 한계

- 현재 ordering 규칙은 `repository-jpa`와 `application` 두 종류의 `integrationTest`만 전제로 한다.
- `buildFinished`와 task graph listener 기반 구현이라 Gradle의 최신 configuration cache 친화적 구조는 아니다.
- `aggregate`에 `integrationTest`가 추가되면 같은 규칙을 적용할지 별도 판단이 필요하다.

## 9. 검증 명령

권장 명령:

```bash
./gradlew -p build-logic test
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew --parallel :member:repository-jpa:integrationTest :member:application:integrationTest --dry-run
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew --parallel :member:repository-jpa:integrationTest :member:application:integrationTest --rerun-tasks
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew :transfer:application:integrationTest --rerun-tasks
```

참고:

- 현재 기본 `java`가 JDK 26이면 Gradle script parsing 단계에서 `Unsupported class file major version 70`가 발생할 수
  있다.
- 로컬 검증은 JDK 24 또는 25로 `JAVA_HOME`을 내려 실행하는 것이 안전하다.
