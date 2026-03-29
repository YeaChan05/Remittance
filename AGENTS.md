# AGENTS.md

## Project Overview

이 저장소는 송금 서비스 구현 레포입니다. 전체 목적과 과제 배경은 [README.md](README.md), 원
요구사항은 [docs/requirements.md](docs/requirements.md)를 먼저 봅니다.

프로젝트 구조를 파악할 때는 아래 문서로 라우팅합니다.

- 전체 개요 / 실행: [README.md](README.md)
- 요구사항: [docs/requirements.md](docs/requirements.md)
- 모듈 및 코드 규칙: [architecture_guide.puml](architecture_guide.puml)
- 모듈 구조: [docs/rule/module.md](docs/rule/module.md)
- 의존 규칙: [docs/rule/dependencies.md](docs/rule/dependencies.md)
- 코드 규칙: [docs/rule/code_convention.md](docs/rule/code_convention.md)
- 인증/필터 구조: [docs/filter_arch.md](docs/filter_arch.md)
- 송금 흐름: [docs/flow/transfer.md](docs/flow/transfer.md)
- 송금 알림 흐름: [docs/flow/transfer-notification.md](docs/flow/transfer-notification.md)

API 작업은 관련 API 문서를 직접 봅니다.

- 회원가입: [docs/api/member-signup.md](docs/api/member-signup.md)
- 로그인: [docs/api/login.md](docs/api/login.md)
- 계좌 등록: [docs/api/account-create.md](docs/api/account-create.md)
- 계좌 삭제: [docs/api/account-delete.md](docs/api/account-delete.md)
- 멱등키 발급: [docs/api/create-idemkey.md](docs/api/create-idemkey.md)
- 입금: [docs/api/deposit.md](docs/api/deposit.md)
- 출금: [docs/api/withdraw.md](docs/api/withdraw.md)
- 이체: [docs/api/transfer.md](docs/api/transfer.md)
- 거래내역 조회: [docs/api/transfer-history.md](docs/api/transfer-history.md)
- 새 API 문서 템플릿: [docs/api/template.md](docs/api/template.md)

문서가 충돌하면 다음 우선순위를 따릅니다.

1. 이 [AGENTS.md](AGENTS.md)
2. 구조 규칙
   문서: [docs/rule/module.md](docs/rule/module.md), [docs/rule/dependencies.md](docs/rule/dependencies.md), [docs/rule/code_convention.md](docs/rule/code_convention.md)
3. 도메인/흐름
   문서: [docs/filter_arch.md](docs/filter_arch.md), [docs/flow/transfer.md](docs/flow/transfer.md), [docs/flow/transfer-notification.md](docs/flow/transfer-notification.md)
4. API 문서: [docs/api](docs/api)
5. 배경 문서: [README.md](README.md), [docs/requirements.md](docs/requirements.md)

## Architecture Rules

구조 판단은 본문 요약보다 아래 문서를 직접 읽고 따릅니다.

- 모듈 트리, Driving/Core/Driven/Assembly: [docs/rule/module.md](docs/rule/module.md)
- 모듈 간 의존 방향, `api` / `implementation` 사용 기준: [docs/rule/dependencies.md](docs/rule/dependencies.md)
- 인증/인가 경계와 `api-internal` 협력: [docs/filter_arch.md](docs/filter_arch.md)
- 내부 통신 기본 경로: `service -> own infrastructure -> provider:api-internal.internal.contract`

코드 파일 레벨에서 wiring 패턴을 확인할 때는 아래를 직접 봅니다.

- 서비스 bean
  wiring: [account/service/AccountAutoConfiguration.kt](account/service/src/main/kotlin/org/yechan/remittance/account/AccountBeanRegistrar.kt)
- 송금 bean
  wiring: [transfer/service/TransferAutoConfiguration.kt](transfer/service/src/main/kotlin/org/yechan/remittance/transfer/TransferBeanRegistrar.kt)
- API controller import
  registrar: [account/api/AccountApiAutoConfiguration.kt](account/api/src/main/kotlin/org/yechan/remittance/account/AccountApiRegistrar.kt)
- API bean
  wiring: [account/api/AccountApiBeanRegistrar.kt](account/api/src/main/kotlin/org/yechan/remittance/account/AccountApiRegistrar.kt)
- 조건부 bean
  등록: [common/boot/BeanRegistrarExtensions.kt](common/boot/src/main/kotlin/org/yechan/remittance/BeanRegistrarExtensions.kt)

핵심 구조 원칙만 짧게 요약하면 다음과 같습니다.

- `aggregate`는 조립만 담당합니다.
- Core는 구현 기술에 직접 의존하지 않습니다.
- 도메인 간 직접 의존은 매우 제한적으로만 허용됩니다.
- cross-domain 내부 호출은 consumer `infrastructure`가 provider `api-internal` 계약을 감싸는 방식으로만 둡니다.
- Spring bean 등록은 이 레포의 기존 `BeanRegistrarDsl` 패턴을 우선 따릅니다.
- controller import 전용 설정은 `{Domain}ApiRegistrar` 이름을 사용합니다.
- `BeanRegistrarDsl` wiring이 필요하면 `{Domain}BeanRegistrar`, `{Domain}ApiBeanRegistrar`,
  `{Domain}InternalApiBeanRegistrar`를 별도로 둡니다.
- `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`에는 실제로 부팅에 필요한
  registrar / bean registrar를 모두 등록합니다.
- api 모듈의 web helper는 `UseCase` 대신 `Handler` / `Registry` / `Adapter` 이름을 사용합니다.

## Coding Rules

코딩 스타일과 네이밍은 [docs/rule/code_convention.md](docs/rule/code_convention.md)를 기준으로 합니다.

빌드/테스트/포맷 명령은 아래를 기준으로 합니다.

- 전체 빌드: `./gradlew build`
- 단위 테스트: `./gradlew test`
- 통합 테스트: `./gradlew integrationTest`
- 전체 검증: `./gradlew check`
- 포맷: `./gradlew ktlintFormat`
- 실행: `./gradlew :aggregate:bootRun`

구현 시에는 규칙 문서보다 먼저 "가장 가까운 기존 코드"를 복제 대상으로 봅니다.

- API
  패턴: [account/api](account/api/src/main/kotlin/org/yechan/remittance/account), [transfer/api](transfer/api/src/main/kotlin/org/yechan/remittance/transfer)
- 서비스
  패턴: [account/service](account/service/src/main/kotlin/org/yechan/remittance/account), [transfer/service](transfer/service/src/main/kotlin/org/yechan/remittance/transfer)
- 저장소
  패턴: [account/repository-jpa](account/repository-jpa/src/main/kotlin/org/yechan/remittance/account/repository), [transfer/repository-jpa](transfer/repository-jpa/src/main/kotlin/org/yechan/remittance/transfer/repository)
- 테스트
  패턴: [aggregate/src/integrationTest](aggregate/src/integrationTest/kotlin/org/yechan/remittance), [transfer/repository-jpa/src/integrationTest](transfer/repository-jpa/src/integrationTest/kotlin/org/yechan/remittance/transfer/repository)

## Domain Rules

도메인 규칙은 기능별 문서로 직접 라우팅합니다.

- 과제의 원 요구사항: [docs/requirements.md](docs/requirements.md)
- 송금/ledger/outbox/idempotency 흐름: [docs/flow/transfer.md](docs/flow/transfer.md)
- 이체 알림, RabbitMQ, SSE 흐름: [docs/flow/transfer-notification.md](docs/flow/transfer-notification.md)
- 인증/로그인/토큰/내부 인증 계약: [docs/filter_arch.md](docs/filter_arch.md)

API별 비즈니스 규칙은 해당 API 문서를 직접 읽습니다.

- [docs/api/account-create.md](docs/api/account-create.md)
- [docs/api/account-delete.md](docs/api/account-delete.md)
- [docs/api/deposit.md](docs/api/deposit.md)
- [docs/api/withdraw.md](docs/api/withdraw.md)
- [docs/api/transfer.md](docs/api/transfer.md)
- [docs/api/transfer-history.md](docs/api/transfer-history.md)
- [docs/api/member-signup.md](docs/api/member-signup.md)
- [docs/api/login.md](docs/api/login.md)
- [docs/api/create-idemkey.md](docs/api/create-idemkey.md)

규칙이 코드와 달라 보이면 임의로 새 해석을 만들지 말고, 관련 문서와 기존 구현을 함께 확인한 뒤 차이를 명시합니다.

## Do / Don't

### Do

- 작업 시작 전에 관련 문서를 먼저 찾고, 그 문서 링크를 기준으로 판단합니다.
- 구조 판단은 [docs/rule/module.md](docs/rule/module.md)
  와 [docs/rule/dependencies.md](docs/rule/dependencies.md)를 먼저 봅니다.
- 기능 구현은 테스트 우선으로 진행하고, 기존 테스트 패턴을 먼저 복제합니다.
- bean 등록 변경 시 기존 `BeanRegistrarDsl` wiring 파일도 함께 확인합니다.
- registrar 테스트는 registrar 클래스를 직접 `register(...)` 하기보다, 테스트용 `@Configuration`에서
  `@Import(Registrar::class)`로 올리는 현재 패턴을 우선 따릅니다.
- API/흐름/구조 계약이 바뀌면 해당 문서도 같이 갱신합니다.
- 내부 통신 계약을 추가하거나 변경할 때는 provider `api-internal`의 `internal.contract` / `internal.adapter` 분리를
  유지합니다.

### Don't

- 애플리케이션에 비즈니스 로직을 넣지 않습니다.
- `service`에서 `repository-jpa` 같은 구현 모듈에 직접 의존하지 않습니다.
- `service`에서 다른 도메인의 `infrastructure`나 `api-internal`을 직접 참조하지 않습니다.
- `api`에서 repository를 직접 호출하지 않습니다.
- 문서 없이 새로운 도메인 용어, API 응답 형태, 이벤트 계약을 임의로 만들지 않습니다.
- 기존 패턴과 무관한 Spring bean 등록 방식이나 테스트 구조를 새로 들여오지 않습니다.

## Workflow

작업 절차는 문서 라우팅 중심으로 아래 순서를 따릅니다.

1. 요구사항 확인  
   [README.md](README.md), [docs/requirements.md](docs/requirements.md),
   관련 [docs/api](docs/api), [docs/flow](docs/flow)

2. 구조 확인  
   [docs/rule/module.md](docs/rule/module.md), [docs/rule/dependencies.md](docs/rule/dependencies.md), [docs/filter_arch.md](docs/filter_arch.md)

3. 기존 구현 확인  
   가장 가까운 모듈/패키지/테스트/AutoConfiguration 파일 확인

4. 테스트 설계 및 선작성  
   관련 테스트 패턴을 복제하고 failing test부터 작성

5. 최소 구현  
   기존 코드 패턴과 `BeanRegistrarDsl` wiring을 유지하며 최소 수정

6. 검증  
   가장 좁은 관련 Gradle 명령부터 실행  
   예: `:module:test`, `:module:integrationTest`, 필요 시 `./gradlew check`

7. 문서 동기화  
   계약이 바뀌면 관련 [docs/api](docs/api), [docs/flow](docs/flow), [docs/rule](docs/rule) 중 필요한 문서를 같이 수정
