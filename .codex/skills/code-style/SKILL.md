---
name: code-style
description: Keep new or modified Remittance code aligned with the repo's naming, package placement, class suffixes, and BeanRegistrarDsl wiring style. Use when writing or reviewing Kotlin, Spring Boot, AutoConfiguration, controller, service, repository-jpa, or api-internal code in this repository.
---

# Code Style

## 먼저 읽을 문서

- [../../rules/agent-coding-discipline.md](../../rules/agent-coding-discipline.md)

## Language

모든 응답은 한국어로 작성한다.

## 먼저 볼 기준

- 스타일 기준 문서: [docs/rule/code_convention.md](../../../docs/rule/code_convention.md)
- 모듈/의존 규칙이 이름과 배치에 영향을 줄 때:
    - [docs/rule/module.md](../../../docs/rule/module.md)
    - [docs/rule/dependencies.md](../../../docs/rule/dependencies.md)

## 수행 규칙

- 가장 가까운 기존 파일을 먼저 찾고 그대로 복제한다.
- 새 스타일을 만들지 않는다.
- 문서 요약보다 실제 코드 패턴을 우선 본다.
- 스타일 판단과 구조 판단이 충돌하면 구조 문서를 우선한다.
- `agent-coding-discipline.md`가 더 엄격한 제한을 두면 그 규율을 우선한다.

## 참고 코드 사용 규칙

- 아래 코드 블록은 현재 시점의 스타일 예시다.
- 실제 소스 파일 경로와 구현은 언제든 바뀔 수 있다.
- 스타일 판단은 예시를 그대로 암기하는 방식이 아니라, 작업 시점의 실제 코드에서 가장 가까운 패턴을 다시 확인하는 방식으로 수행한다.

## 빠른 체크리스트

### 네이밍

- 역할과 책임이 이름으로 드러나게 유지한다.
- 기술 이름을 클래스명에 넣지 않는다.
- 같은 역할이면 같은 접미사를 유지한다.

### Core

- `model`: `DomainModel`, `DomainProps`, `DomainIdentifier`
- `service`: `DomainCreateUseCase`, `DomainReadUseCase`, `DomainUpdateUseCase`, `DomainQueryUseCase`
- 구현체: `DomainService`, `DomainQueryService`
- `infrastructure`: `DomainRepository`, `DomainEventPublisher`, `DomainExternalClient`
- 예외는 `BusinessException` 계층을 따른다.

### Inbound / Outbound

- `api`: `{Domain}Controller` 또는 `{Domain}ApiController`
- DTO: `{Domain}{Action}Request`, `{Domain}{Action}Response`
- `api-internal`: `DomainInternalApi`, `DomainInternalAdapter`
- `repository-jpa`: `DomainRepositoryImpl`, `DomainJpaRepository`, `DomainEntity`

## BeanRegistrarDsl

- `@AutoConfiguration` + `@Import({Domain}BeanRegistrar::class)` 형태를 우선 사용한다.
- wiring은 `class {Domain}BeanRegistrar : BeanRegistrarDsl({ ... })` 안에서 처리한다.
- 기본 형태는 `registerBean<Type> { Impl(bean(), bean<SpecificType>()) }` 이다.
- 타입 추론이 모호하면 `bean<AccountRepository>()`처럼 명시 타입을 쓴다.
- 조건부 bean은 `whenPropertyEnabled(...)`를 사용한다.

```kotlin
@Import(AccountBeanRegistrar::class)
@AutoConfiguration
class AccountAutoConfiguration

class AccountBeanRegistrar :
    BeanRegistrarDsl({
        registerBean<AccountCreateUseCase> {
            AccountService(bean())
        }
    })
```

```kotlin
fun BeanRegistrarDsl.whenPropertyEnabled(
    prefix: String,
    name: String,
    havingValue: String = "true",
    matchIfMissing: Boolean = false,
    block: BeanRegistrarDsl.() -> Unit,
) {
    val key = "$prefix.$name"
    val value = env.getProperty(key)
    val enabled = value?.equals(havingValue, ignoreCase = true) ?: matchIfMissing

    if (enabled) {
        block()
    }
}
```

## 적용 순서

1. 수정 대상과 가장 가까운 기존 파일 1~3개를 찾는다.
2. 이름, package, 생성자 모양, wiring 방식을 그대로 맞춘다.
3. `BeanRegistrarDsl`이 있는 영역이면 bean 등록 파일도 같이 수정한다.
4. 스타일을 설명하지 말고 diff로 드러나게 만든다.

## 최소 보고

- 어떤 파일을 기준으로 스타일을 맞췄는지
- `BeanRegistrarDsl` 수정 여부
- 남은 스타일 불확실성
