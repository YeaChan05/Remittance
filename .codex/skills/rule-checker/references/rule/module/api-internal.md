## 개요

- 위치: `Driving`
- 역할: internal endpoint, internal contract, provider adapter, registrar
- 범위: provider-side internal transport

## 의존 관계

### 의존 모듈

- `common:api`
- `{domain}:service`
- `spring-tx` (필요 시)

### 의존 금지 모듈

- `{domain}:repository-*`
- `{domain}:mq-*`
- other-domain `api-internal` 구현
- `internal.adapter` 외부 노출

## 구현

### 구성 요소

- `internal.contract`
- `internal.adapter`
- `*InternalApi`
- `*Request`
- `*Response`
- `*SnapshotResponse`
- `*InternalAdapter`
- `*InternalApiBeanRegistrar`

### 특징적 구현

```kotlin
@HttpExchange("/internal/{domains}")
interface {Domain}InternalApi {
    @GetExchange("/{id}")
    fun get(@PathVariable id: Long): {Domain}SnapshotResponse
}

class {Domain}InternalAdapter(
    private val getUseCase: {Domain}GetUseCase,
) : {Domain}InternalApi {
    override fun get(id: Long): {Domain}SnapshotResponse =
        {Domain}SnapshotResponse.from(getUseCase.get(id))
}
```

### 작명 방식

- 계약: `{Domain}InternalApi`
- DTO: `{Domain}{Action}Request`, `{Domain}{Action}Response`, `{Domain}SnapshotResponse`
- 구현: `{Domain}InternalAdapter`
- registrar: `{Domain}InternalApiBeanRegistrar`

## gradle.properties

- `type=kotlin-boot-mvc`
- `group={base-package}.{domain}`

## Test

- 기본: `:{domain}:api-internal:test`
- adapter test
- registrar test
