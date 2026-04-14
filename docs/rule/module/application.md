## 개요

- 위치: `Assembly`
- 역할: 실행 entrypoint, bean 조립, integration test 소유
- 범위: runtime assembly

## 의존 관계

### 의존 모듈

- `common:security`
- `{domain}:api`
- `{domain}:api-internal` (필요 시)
- `{domain}:repository-jpa`
- `{domain}:schema`
- `{domain}:mq-rabbitmq` (필요 시)

### 의존 금지 모듈

- other-domain `service`
- other-domain `infrastructure`
- domain 경계 우회 조립
- business logic 소유

## 구현

### 구성 요소

- `*ApiApplication`
- `*ApplicationSecurityBeanRegistrar`
- `application.yml`
- integration test fixture
- integration test spec

### 특징적 구현

```kotlin
@SpringBootApplication
class {Domain}ApiApplication

fun main(args: Array<String>) {
    runApplication<{Domain}ApiApplication>(*args)
}
```

### 작명 방식

- entrypoint: `{Domain}ApiApplication`
- registrar: `{Domain}ApplicationSecurityBeanRegistrar`
- fixture: `{Domain}TestFixtures`, `{Domain}TestFixturesConfig`

## gradle.properties

- `type=kotlin-boot-mvc-application`
- `group={base-package}.{domain}`
- `label=docker`
- `testcontainers.stack={domain}`

## Test

- 기본: `:{domain}:application:integrationTest`
- registrar test
- API spec test
