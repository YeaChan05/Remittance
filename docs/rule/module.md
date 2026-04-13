### 모듈 구조 및 규칙

---

## 1. 최상위 원칙

* Driving -> Core -> Driven 방향만 허용한다.
* 기술(JPA, Web, MQ 등)은 Driven 모듈에 둔다.
* 다른 도메인과의 내부 연결은 `service -> own infrastructure -> provider:api-internal.internal.contract` 경로만
  허용한다.
* 인증/인가 책임은 역할별로 분리한다.
    * 로그인/토큰 발급: `member:service`
    * 자격 검증: `member:service` + `member:api-internal`
    * 토큰 검증/필터: `common:security`
* 예외: 암호화/토큰 관련 로직은 `common:security`를 통해 `service`에서도 사용할 수 있다.

---

## 2. 모듈 매핑

### 2.1 Driving

* 사용자 요청이나 내부 호출을 받아 유스케이스를 실행한다.
* Controller, 내부 API 어댑터, 배치 트리거가 여기에 해당한다.

대상 모듈

* `{domain}:api`
* `{domain}:api-internal` (필요 시)

### 2.2 Core

* 비즈니스 규칙과 유스케이스를 담는다.
* 외부 기술 의존 없이 포트만 정의한다.

대상 모듈

* `{domain}:model`
* `{domain}:service`
* `{domain}:infrastructure`
* `{domain}:exception`

### 2.3 Driven

* 외부 시스템/기술에 대한 구현체를 제공한다.
* 저장소, 외부 API 클라이언트, 보안 필터가 여기에 해당한다.

대상 모듈

* `{domain}:repository-{type}`
* `{domain}:mq-{type}`
* `{domain}:schema`
* `common:repository-{type}`
* `common:security`

### 2.4 Assembly

* 여러 도메인을 조합해 실행하는 모듈이다.
* 도메인별 runnable application도 Assembly에 포함한다.

대상 모듈

* `aggregate`
* `{domain}:application`

---

## 3. 도메인 모듈 트리 규칙

```text
{domain}
 ├── model
 ├── infrastructure
 ├── service
 ├── exception
 ├── api
 ├── api-internal (optional)
 ├── repository-{type}
 ├── schema
 └── mq-{type}
```

애플리케이션 모듈이 필요한 도메인은 아래를 추가한다.

```text
{domain}
 └── application
```

* `repository-{type}`: 현재 `jpa` 사용
* `schema`: Liquibase changelog 보관
* `mq-{type}`: MQ 기반 비동기 연동
* `api-internal`: 내부 계약과 provider-side adapter 제공
    * 현재 기본 패키지 구조는 `internal.contract`, `internal.adapter`

---

## 4. Core 모듈 규칙

### 4.1 model

* 도메인 핵심 개념(상태/규칙)을 담는다.
* 프레임워크/외부기술 의존 금지.
* 원칙적으로 다른 하위 모듈 의존 금지(필요 시 다른 도메인 model 의존 가능).

의존

* (가능하면 없음)

노출(Gradle)

* 외부에 노출해도 되는 도메인 타입만 포함

---

### 4.2 infrastructure

* service가 사용할 외부 연결 경계를 둔다.
* 기본은 out-port interface를 두는 모듈이다.
* 같은 프로세스 내부 통신을 채택한 consumer 도메인은 여기서 provider `api-internal` 계약을 감싸는 client adapter를 둘 수 있다.
* repository, external-client, internal-client, event-publisher 같은 경계를 둔다.

의존

* `model`
* (consumer-side internal client일 때) 상대 도메인의 `api-internal`

노출(Gradle)

* `api(project(":{domain}:model"))` 형태로 도메인 타입은 노출한다.
* 상대 도메인 `api-internal` 의존이 필요하면 `implementation(...)` 으로 숨긴다.

---

### 4.3 service (use-case)

* 유스케이스 단위의 비즈니스 로직을 담는다.
* 트랜잭션/오케스트레이션은 여기서 끝낸다.
* 외부 의존은 반드시 `infrastructure`에 정의된 포트를 통해서만 한다.

의존

* `model`
* `infrastructure`
* `exception`
* (인증 필요 시) `common:security`

금지

* `repository-*`, `api` 등 구현체 직접 의존 x
* 다른 도메인의 `infrastructure`, `api-internal` 직접 의존 x

---

### 4.4 exception

* 모든 비즈니스 예외는 반드시 `BusinessException`을 상속해야 한다.
* 기술 예외를 그대로 던지지 않는다(필요 시 변환).

의존

* `common:exception` (또는 동일 역할 공통 모듈)

금지

* 스프링 웹/DB 예외 타입 직접 의존 x

---

## 5. Driving 모듈 규칙

### 5.1 api (Inbound Adapter)

* Controller + request/response DTO만 포함한다.
* 비즈니스 로직 금지.
* 호출 대상은 `service` 또는 `api-internal`로 제한한다.
* Spring MVC controller import와 bean 등록 wiring은 분리한다.

의존

* `common:api`
* `service`
* `exception`

금지

* `repository-*` 직접 의존 x
* `model`을 그대로 응답으로 노출 x(필요 시 api DTO로 변환)

Spring bean / 설정 규칙

* controller import 전용 클래스는 `{Domain}ApiRegistrar` 이름을 사용한다.
* api 모듈에서 `BeanRegistrarDsl` wiring이 필요하면 `{Domain}ApiBeanRegistrar`를 별도로 둔다.
* `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`에는 실제로 로딩해야 하는
  registrar / bean registrar 클래스를 모두 등록한다.
* web 기술 타입(`SseEmitter` 등)을 다루는 helper는 api 모듈에 두되, `UseCase` 대신 `Handler` / `Registry` / `Adapter`
  이름을 사용한다.

---

### 5.2 api-internal (Internal Inbound Adapter)

* 내부 계약과 provider-side adapter를 함께 둔다.
* 현재 기본 패키지 분리는 아래와 같다.
    * `internal.contract`: 타 도메인이 import 하는 계약/DTO
    * `internal.adapter`: provider use case를 호출하는 adapter와 auto-configuration

의존

* `service`

금지

* `repository-*` 직접 의존 x

Spring bean / 설정 규칙

* provider-side adapter bean 등록은 `{Domain}InternalApiBeanRegistrar`로 둔다.
* `api-internal` 모듈이 별도 설정 클래스를 두지 않아도 되면 registrar 단독 구조를 허용한다.

---

### 5.3 aggregate (조립/실행)

* 실행 가능한 애플리케이션 모듈.
* 각 adapter들의 Bean을 조립하기 위해 auto-configuration 의존을 둔다.
* 현재는 로컬에서 전체 도메인을 함께 띄워 보는 조립용 런타임 역할만 가진다.
* 정규 API 통합 테스트의 소유자는 각 도메인 `application` 모듈이다.
* 같은 프로세스 조합이 필요한 경우 provider-side `api-internal` bean을 직접 조립할 수 있다.

의존(일반)

* `common:security`
* `{domain}:api`
* `{domain}:api-internal` (해당 application이 internal endpoint를 직접 노출할 때)
* `{domain}:repository-jpa`
* `{domain}:schema`
* `{domain}:mq-{type}`

금지

* 도메인 규칙/로직 구현 x
* auto-configuration을 통한 bean 등록을 위해 의존만
* same-process 조립 예외를 core/service/infrastructure 규칙 완화로 확장 x

Spring bean / 설정 규칙

* 실행 모듈에서 필요한 공통 bean(`Clock` 등)은 `BeanRegistrarDsl` 기반 registrar로 등록한다.
* 단독 application은 HTTP client adapter bean을 조립할 수 있고, `aggregate`는 same-process provider-side bean을 조합할 수 있다.
* `repository-jpa` auto-configuration은 JPA ordering과 direct `@Import` 테스트 경로 때문에
  `@Import({Domain}RepositoryBeanRegistrar::class)` + `@AutoConfiguration(before = [...])` 패턴을 예외로
  유지한다.

---

## 6. Driven 모듈 규칙

### 6.1 repository-{type} (Outbound Adapter)

* `infrastructure`에 정의된 persistence 포트를 구현한다.
* JPA Entity / Spring Data Repository / Mapper는 여기만 존재한다.
* Domain ↔ Entity 변환 책임은 여기(Impl)에 둔다.

의존

* `common:repository-{type}`
* `infrastructure`

금지

* `api` 의존 x
* `service` 의존 x (service가 포트를 호출해야 함)

---

### 6.2 common:security

* 토큰 검증/파싱, 인증 필터 등 보안 기술 구현을 포함한다.
* Core 포트를 구현하거나 공통 필터를 제공한다.

의존

* `common:exception`

---

## 7. 애플리케이션 간 연동 규칙

* 직접 참조 최소

    * 가능하면 다른 도메인의 `model/service/repository`를 의존 x
* 허용

    * `consumer:infrastructure` -> `provider:api-internal`
    * 단, consumer는 provider의 `internal.contract` 패키지만 import
* 연결 방식

    * 런타임(HTTP/MQ) 연결은 간접(Indirect) 관계로만 표현

---

## 8. Gradle 노출 규칙(api vs implementation)

* `api` 사용 대상

    * 계약(인터페이스, 타입)을 외부 모듈이 컴파일 타임에 알아야 할 때
    * 예: `infrastructure -> model`을 `api(...)`로 노출
* `implementation` 사용 대상

    * 내부 구현 세부사항(교체 가능해야 하는 것)
    * 예: `service -> infrastructure`, `api -> service`, `repository-* -> infrastructure`

---

## 9. 인증/인가 모듈 의존 예시

### 9.1 member (로그인 + 내부 인증 제공)

* `member:service`
    - 의존: `member:model`, `member:infrastructure`, `member:exception`
* `member:api-internal`
    - 의존: `member:service`
    - 패키지: `internal.contract`, `internal.adapter`

### 9.2 transfer (consumer-side internal client)

* `transfer:service`
    - 의존: `transfer:model`, `transfer:infrastructure`, `transfer:exception`
* `transfer:infrastructure`
    - 의존: `transfer:model`, `account:api-internal`, `member:api-internal`
    - 역할: `TransferAccountClient`, `TransferMemberClient` adapter 제공

### 9.4 common/aggregate

* `common:security`
    - 의존: `common:exception`
* `aggregate`
    - 의존: `common:security`, `account:api`, `transfer:api`, `member:api`,
      `account|member:api-internal`, `account|transfer|member:repository-jpa`,
      `account|transfer|member:schema`,
      `account|transfer:mq-rabbitmq`


### Reference
[[docs/rule/architecture_guide.puml]]