## 개요

- 위치: `Driven`
- 역할: RabbitMQ consumer/publisher, payload parsing, auto-configuration
- 범위: 비동기 메세징 구현

## 의존 관계

### 의존 모듈

- `common:boot`
- `{domain}:service`
- `spring-boot-starter-amqp`

### 의존 금지 모듈

- `{domain}:api`
- `{domain}:api-internal`
- `{domain}:application`
- other-domain `mq-rabbitmq`

## 구현

### 구성 요소

- `*Consumer`
- `*Publisher`
- `*PayloadParser`
- `*ConsumerProperties`
- `*AutoConfiguration`
- `*InfrastructureBeanRegistrar`

### 특징적 구현

```kotlin
class {Domain}EventConsumer(
    private val processUseCase: {Domain}ProcessUseCase,
) {
    @RabbitListener(queues = ["{domain}.queue"])
    fun consume(payload: String) {
        processUseCase.process({Domain}PayloadParser.parse(payload))
    }
}
```

### 작명 방식

- consumer: `{Domain}EventConsumer`
- publisher: `{Domain}EventPublisher`
- parser: `{Domain}PayloadParser`
- properties: `{Domain}ConsumerProperties`
- auto-configuration: `{Domain}ConsumerAutoConfiguration`

## gradle.properties

- `type=kotlin-boot`
- `group={base-package}.{domain}`

## Test

- 기본: `:{domain}:mq-rabbitmq:test`
- consumer test
- parser test
