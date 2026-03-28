# Agent Coding Discipline

이 문서는 `Remittance` 레포에서 코드 작성형 agent와 skill이 따라야 할 강한 실행 규율을 정의한다.
`AGENTS.md`는 라우팅과 문서 우선순위를 설명하고, 실제 구현 행동은 이 문서가 더 구체적으로 고정한다.

이 문서는 write-capable agent, 구현 skill, 테스트 skill, 스타일 정렬 skill에 공통으로 적용한다.

공식 agent orchestration 흐름은 `.codex/agents/WORKFLOW.md`를 따른다.

## 1. 적용 범위와 우선순위

- 이 문서는 `Remittance` 레포에서 코드를 읽거나 수정하는 모든 agent/skill의 행동 규율이다.
- 우선순위는 다음과 같다.
  1. system / developer / user 지시
  2. `AGENTS.md`
  3. 이 문서
  4. 개별 skill / agent 문서
- 개별 skill이나 agent가 이 문서보다 느슨한 규칙을 갖고 있으면 이 문서를 우선한다.
- 개별 skill이나 agent가 더 엄격한 규칙을 갖고 있으면 더 엄격한 쪽을 우선한다.

## 2. 시작 전 필수 확인

코드 수정 전 반드시 아래 순서대로 사실관계를 고정한다.

1. `AGENTS.md`
2. `docs/rule/module.md`
3. `docs/rule/dependencies.md`
4. `docs/rule/code_convention.md`
5. 관련 `docs/api/*` 또는 `docs/flow/*`
6. 가장 가까운 기존 구현 1~3개
7. 가장 가까운 기존 테스트 1~3개

필수 규칙:

- 문서 요약만 보고 판단하지 않는다. 실제 파일을 연다.
- 가장 가까운 구현을 찾지 못했는데 새 패턴을 만들지 않는다.
- 문서와 코드가 충돌하면 차이를 명시하고, 임의 해석으로 메우지 않는다.
- 구조 규칙을 모른 채 구현부터 시작하지 않는다.

## 3. 구현 기본 원칙

- 새 스타일을 만들지 않는다.
- 가장 가까운 기존 구현을 복제한다.
- 필요한 최소 범위만 수정한다.
- 작업 범위 밖 리팩터링은 금지한다.
- unrelated rename, mass formatting, import 정리, package 재배치는 금지한다.
- 실행하지 않은 검증은 실행한 것처럼 보고하지 않는다.

다음과 같은 행동은 금지한다.

- 문서 없이 새 도메인 용어를 만드는 것
- 기존 계층 규칙을 깨는 빠른 우회
- service에 구현 세부사항을 밀어 넣는 것
- aggregate에 조립 외 로직을 넣는 것
- 기존 테스트 패턴과 무관한 새로운 테스트 구조를 들여오는 것

## 4. TDD 실행 규율

기본 실행 순서는 항상 아래와 같다.

1. approval된 requirement 또는 acceptance criterion을 고른다.
2. 그 기준을 검증하는 가장 작은 failing test를 작성하거나 수정한다.
3. 그 테스트를 통과시키는 최소 프로덕션 코드를 수정한다.
4. 필요할 때만 마지막에 작은 리팩터링을 한다.
5. 가장 좁은 관련 검증 명령을 실행한다.

예외:

- 문서 수정만 있는 작업
- wiring-only 변경인데 기존 테스트 구조상 failing test를 만들 가치가 낮은 경우
- 사용자 요청이 분석/설계 전용인 경우

예외 상황에서도 다음은 유지한다.

- 기존 테스트 패턴 탐색
- 가장 좁은 검증
- 실행 결과에 대한 정직한 보고

## 5. 구조 규율

### 5.1 계층 책임

- `aggregate`는 조립만 담당한다.
- `api`는 요청/응답 변환과 handler 연결만 담당한다.
- `service`는 유스케이스 오케스트레이션과 트랜잭션 경계를 담당한다.
- `infrastructure`는 포트와 외부 연결 계약을 담당한다.
- `repository-jpa`, `mq-rabbitmq`, `schema`는 구현 adapter다.
- `model`은 domain interface와 식별자/props를 유지한다.

### 5.2 의존 금지

- `service`는 `repository-jpa`, `mq-rabbitmq`, `schema`에 직접 의존하지 않는다.
- `service`는 다른 도메인의 `infrastructure`나 `api-internal` 구현에 직접 의존하지 않는다.
- `api`는 repository를 직접 호출하지 않는다.
- `aggregate`는 비즈니스 판단을 하지 않는다.
- domain model 구현 타입을 내부 계약 DTO에 직접 노출하지 않는다.

### 5.3 cross-domain 호출

- cross-domain 호출은 consumer 쪽 `infrastructure`가 provider 내부 계약을 감싸는 형태로만 둔다.
- provider 쪽은 `internal.contract`와 `internal.adapter` 책임을 섞지 않는다.
- consumer service가 provider adapter 구현을 직접 보게 만들지 않는다.
- 내부 계약은 transport-neutral DTO로 유지한다.

## 6. Bean / Wiring 규율

### 6.1 기본 원칙

- Spring bean 등록은 기존 `BeanRegistrarDsl` 패턴을 우선한다.
- bean 노출이 바뀌면 wiring 변경을 반쪽만 하지 않는다.
- 관련 `AutoConfiguration`, registrar class, `AutoConfiguration.imports`, registrar test를 함께 점검한다.

### 6.2 책임 분리

- controller import 전용 class와 bean wiring class는 분리한다.
- controller import 전용 class는 `{Domain}ApiRegistrar`를 우선 사용한다.
- bean wiring은 `{Domain}BeanRegistrar`, `{Domain}ApiBeanRegistrar`, `{Domain}InternalApiBeanRegistrar` 규칙을 따른다.
- 설정 annotation과 bean wiring은 책임 기준으로 분리한다.

### 6.3 허용 예외

- bootstrap application class는 inline registrar 예외를 허용한다.
- `repository-jpa` auto-configuration은 기존 `@AutoConfiguration(before = [...])` 패턴을 유지한다.
- JPA 등록 순서, test slice, imports 경로를 깨는 blanket refactor는 금지한다.

### 6.4 구현 패턴

- 기본 형태는 `registerBean<Type> { Impl(bean(), bean<SpecificType>()) }` 를 우선 본다.
- 타입 추론이 불안정하면 명시 타입 `bean<AccountRepository>()`를 사용한다.
- 조건부 bean은 `whenPropertyEnabled(...)`를 우선 사용한다.
- `AutoConfiguration.imports`에는 실제 부팅에 필요한 registrar / bean registrar를 모두 맞춘다.

## 7. 네이밍 / 파일 배치 규율

- package, 파일 배치, 클래스 suffix, 생성자 형태는 가장 가까운 기존 구현을 그대로 따른다.
- 역할이 같으면 같은 suffix를 사용한다.
- 기술 이름이 책임을 덮지 않도록 class name에 기술 구현 이름을 불필요하게 넣지 않는다.
- web helper는 `UseCase`보다 `Handler`, `Registry`, `Adapter`를 우선 사용한다.
- model은 `interface`, `Props`, `Identifier` 조합을 유지한다.
- repository adapter 구현은 `*RepositoryImpl` 패턴을 우선 사용한다.

## 8. 테스트 규율

### 8.1 테스트 위치

- `src/test`
  - 순수 비즈니스 로직
  - 계산
  - 예외 분기
  - Spring 없이 직접 생성 가능한 대상
- `src/integrationTest`
  - Spring MVC
  - Security
  - JPA / DB
  - MQ
  - Testcontainers
  - BeanRegistrarDsl wiring
  - 설정 / 직렬화 / 전체 흐름

### 8.2 테스트 작성

- 가장 가까운 기존 테스트를 먼저 찾고 복제한다.
- 메서드 스타일, fixture 구조, test application 패턴을 새로 만들지 않는다.
- registrar 테스트는 직접 `register(...)` 호출보다 테스트용 `@Configuration` + `@Import(...)` 패턴을 우선 사용한다.
- registrar-only 변경이라도 imports 등록 여부까지 같이 확인한다.

### 8.3 검증 실행

- 항상 가장 좁은 Gradle 명령부터 실행한다.
- 우선순위는 대상 모듈 `test` 또는 `integrationTest`다.
- 전체 `check`는 정말 필요할 때만 넓힌다.
- JPA, MVC, Security, MQ 경로를 건드렸다면 `integrationTest` 필요성을 반드시 판단하고 보고한다.

## 9. 문서 동기화 규율

다음이 바뀌면 문서를 같이 수정한다.

- 구조 규칙
- 내부 계약
- API request / response / error contract
- 주요 흐름
- 도메인 용어 또는 상태 전이

문서 수정 없이 코드만 먼저 밀어 넣는 행동은 금지한다.

## 10. 보고 규율

최소 보고에는 아래를 포함한다.

- 어떤 acceptance criterion 또는 문제를 먼저 검증했는지
- 추가/수정한 failing test
- 수정한 모듈과 파일
- 변경한 wiring 범위
- 실행한 Gradle 명령
- 미실행 검증 또는 남은 gap

다음 보고는 금지한다.

- 실행하지 않은 테스트를 “문제없음”으로 쓰는 것
- 근거 없는 구조 적합 판정
- 취향 차이를 버그처럼 포장하는 리뷰

## 11. 중단 및 에스컬레이션 조건

아래 상황이면 임의 우회하지 말고 중단 후 이유와 대안을 보고한다.

- 구조 규칙을 깨야만 구현이 가능한 경우
- 문서와 코드가 크게 충돌해 어느 쪽을 따라야 할지 불명확한 경우
- cross-domain 계약 경계가 현재 규칙과 맞지 않는 경우
- 기존 패턴이 없는 영역인데 새 패턴 도입이 필요한 경우
- 사용자가 요구한 변경이 aggregate에 비즈니스 로직을 강요하는 경우

보고 형식:

- 왜 규율과 충돌하는지
- 어떤 파일/모듈에서 충돌하는지
- 가능한 대안 1~2개
- 구현 지속 여부 판단에 필요한 추가 결정

## 12. 한 줄 기준

이 레포에서 agent는 “새로운 정답”을 만들지 않는다. 문서와 가장 가까운 기존 구현을 바탕으로, 가장 작은 failing test부터 시작해, 구조 규칙을 깨지 않는 최소 변경만 수행한다.
