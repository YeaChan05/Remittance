---
name: implementation
description: Modify Java or Gradle code in the remittance repo by copying existing package layout, naming, BeanRegistrarDsl wiring, and module-boundary patterns. Use when writing production code changes inside existing modules.
---

# Implementation

## 먼저 읽을 문서

- [../../rules/agent-coding-discipline.md](../../rules/agent-coding-discipline.md)

## Language

모든 응답은 한국어로 작성한다.

## 절차

1. 먼저 `agent-coding-discipline.md`를 읽고 더 엄격한 규율을 고정한다.
2. `rg`로 가장 가까운 기존 구현 1~3개를 찾는다.
3. package, 파일 배치, 클래스 이름, 생성자 형태를 그대로 복제한다.
4. 필요한 코드만 수정한다.
5. bean 노출이 바뀌면 해당 AutoConfiguration/BeanRegistrar wiring도 같이 수정한다.
6. 검증은 관련 skill 또는 호출자 지시에 따른다.

## 참고 코드 사용 규칙

- 아래 코드 블록은 현재 시점의 참고 예시다.
- 실제 소스 파일은 언제든 바뀔 수 있으므로, 고정 규칙처럼 인용하지 않는다.
- 항상 작업 시점의 실제 코드베이스에서 가장 가까운 구현을 다시 찾아 확인한다.
- 가장 가까운 구현보다 이 문서나 `agent-coding-discipline.md`가 더 엄격하면 더 엄격한 쪽을 따른다.

## 빠른 패턴

### api

- Controller는 `{Domain}Api` 인터페이스를 구현한다.
- DTO는 `dto` 패키지에 둔다.
- 비즈니스 로직을 Controller에 두지 않는다.
- controller import 전용 설정은 `{Domain}ApiRegistrar` 이름을 우선 본다.
- web helper는 `UseCase`보다 `Handler` / `Registry` / `Adapter` 이름을 우선 본다.

### service

- UseCase 인터페이스와 구현을 같은 파일에 두는 기존 패턴을 우선 본다.
- 트랜잭션 경계와 오케스트레이션은 service에 둔다.

### model

- 모델은 `interface`로 둔다.
- `Props`, `Identifier` 인터페이스 조합을 유지한다.

### repository-jpa

- 포트 구현체는 `*RepositoryImpl`로 둔다.
- 기술 리포지토리는 별도 인터페이스로 분리한다.

### BeanRegistrarDsl

- controller import 전용 클래스와 bean wiring 클래스는 분리한다.
- controller import 전용 클래스는 `{Domain}ApiRegistrar`를 우선 사용한다.
- wiring은 `class {Domain}BeanRegistrar : BeanRegistrarDsl({ ... })` 안에서 처리한다.
- api wiring은 `class {Domain}ApiBeanRegistrar : BeanRegistrarDsl({ ... })` 안에서 처리한다.
- internal adapter wiring은 `class {Domain}InternalApiBeanRegistrar : BeanRegistrarDsl({ ... })` 안에서
  처리한다.
- 기본 형태는 `registerBean<Type> { Impl(bean(), bean<SpecificType>()) }` 이다.
- 타입 추론이 모호하면 `bean<AccountRepository>()`처럼 명시 타입을 쓴다.
- 조건부 bean은 `whenPropertyEnabled(...)`를 사용한다.
- `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`에 registrar /
  bean registrar 등록을 같이 맞춘다.
- `repository-jpa` auto-configuration은 `@Import({Domain}RepositoryBeanRegistrar::class)` +
  `@AutoConfiguration(before = [...])` 예외 패턴을 유지한다.

```kotlin
@Import(AccountController::class, NotificationApiController::class)
class AccountApiRegistrar

@AutoConfiguration
class AccountApiBeanRegistrar :
    BeanRegistrarDsl({
        registerBean<NotificationSessionRegistry> {
            NotificationSessionRegistry()
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

## 최소 보고

- 수정한 모듈과 파일
- BeanRegistrarDsl 수정 여부
- imports 파일 수정 여부
- 남은 blocker 또는 assumption
