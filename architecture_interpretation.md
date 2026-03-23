# 아키텍처 가이드 해석 문서

## 1. 문서 개요

이 파일은 단순한 PlantUML 예시가 아니라, 헥사고날 아키텍처 기반에서 **모듈 의존 관계**와 **코드 레벨 의존 방향**을 어떻게 유지할지에 대한 설계 가이드로 해석할 수 있다.

파일은 크게 3개의 관점을 담고 있다.

1. **모듈 구조 관점**
   - Inbound / Domain / Outbound 계층을 어떻게 나눌지
   - 애플리케이션 간 연동을 어떤 경계로 수행할지
2. **코드 네이밍 관점**
   - 각 모듈에 어떤 클래스가 위치하는지
   - 어떤 클래스가 어떤 인터페이스를 호출해야 하는지
3. **정제 중인 설계 초안 관점**
   - 다이어그램에 `???`, `optional`, `migration` 같은 표기가 포함되어 있어
   - 이미 확정된 규칙과 아직 논의 중인 규칙이 함께 섞여 있다.

즉, 이 문서는 “현재 팀이 지향하는 헥사고날 표준안 + 일부 미확정 설계 포인트”를 시각화한 문서라고 보는 것이 적절하다.

---

## 2. 범례 해석

다이어그램의 화살표는 단순 연결선이 아니라 **의존 종류**를 뜻한다.

- `-->` : 호출 / 구현 의존
  - 실제 사용, 호출, 구현체 연결
  - Gradle 기준으로는 보통 `implementation` 성격에 가깝다.
- `..>` : 확장 / 인터페이스 구현 / 추상 의존
  - 인터페이스를 구현하거나 계약을 따르는 구조
- `-[dotted]->` : 참조 의존
  - DTO, Model, Exception 등을 참조하는 관계
  - 강한 실행 의존보다는 타입 참조 성격이 강함
- `-[dotted]-` : 간접 관계
  - 직접 호출은 아니지만 의미적으로 연결된 관계

이 기준으로 보면, 이 파일은 **강한 실행 의존**, **인터페이스 의존**, **타입 참조 의존**을 구분해서 설계하려는 의도가 있다.

---

## 3. 전체 구조 해석

전체 구조는 다음과 같이 읽는 것이 자연스럽다.

```text
사용자/관리자
    ↓
API / Internal API / Operation API
    ↓
UseCase(Service)
    ↓
Out Port (Repository 인터페이스, 외부 연동 인터페이스)
    ↓
Repository 구현체 / MQ 구현체 / 외부 시스템 어댑터
```

즉, 핵심은 다음 두 가지다.

첫째, **비즈니스 로직은 service(use-case)에 모으고**, 바깥 계층은 그것을 호출하는 역할만 맡는다.

둘째, **영속성 기술이나 외부 시스템 의존은 infrastructure / repository 구현체 쪽으로 밀어낸다.**

이 구조는 전형적인 헥사고날 아키텍처의 방향성과 거의 일치한다.

---

## 4. 모듈 의존 관계 해석

### 4.1 Inbound 영역

Inbound에는 다음 요소들이 등장한다.

- `api`
- `api-internal`
- `api-operation`
- `api-internal-client`
- `in-port-internal`
- `protocol (optional / migration 대상)`

이들의 역할은 다음과 같이 해석된다.

#### api
외부 사용자 요청을 받는 공개 API 계층이다.

- 일반적인 Controller가 위치한다.
- Request/Response DTO를 가진다.
- 직접 Repository나 Service를 호출하지 않고 UseCase를 호출한다.

#### api-internal
내부 시스템 간 호출을 받는 API 계층이다.

- 같은 조직 내 다른 애플리케이션이나 모듈이 호출할 수 있다.
- 외부 공개 API와 분리하여 보안/계약을 따로 가져가려는 의도 사용된다.

#### api-operation
운영자 또는 백오피스성 요청을 처리하는 계층이다.

- admin actor가 이쪽으로 연결되어 있다.
- 일반 사용자 기능과 운영 기능을 분리하려는 목적이다.

#### api-internal-client
다른 애플리케이션의 internal API를 호출하기 위한 클라이언트 계층이다.

- Feign Client, RestClient, WebClient, gRPC Client 등으로 치환 가능하다.
- 중요한 점은 이 클라이언트가 도메인 핵심 로직을 직접 참조하는 것이 아니라, **in-port 또는 protocol 계약에 맞춰 동작**하도록 유도하고 있다는 것이다.

#### in-port-internal
애플리케이션 외부 또는 타 애플리케이션이 기대하는 **내부 진입 계약**이다.

- 내부 연동용 인터페이스
- 애플리케이션 경계를 넘을 때 사용할 추상 계약
- 내부 API Controller나 Internal Client가 의존하는 대상

---

### 4.2 Domain 영역

중앙 영역에는 다음 요소들이 놓여 있다.

- `model`
- `service (use-case)`
- `exception`

#### model
도메인 상태와 식별자, 속성을 표현하는 중심 모델이다.

두 번째 다이어그램에서는 아래처럼 더 구체화된다.

- `DomainIdentity`
- `DomainProps`
- `DomainModel`
- `Domain`

즉, 단순 엔티티 하나가 아니라

- 식별자
- 속성 집합
- 도메인 모델
- 실제 도메인 객체

를 나눈다.

이 구조는 다음 목적에 유리하다.

- 도메인 의미를 명시적으로 표현
- JPA 엔티티와 순수 도메인 모델 분리
- 테스트와 모델 재사용성 향상

#### service (use-case)
비즈니스 유스케이스를 담당하는 핵심 계층이다.

예시 클래스:

- `DomainQueryUseCase`
- `DomainCreateUseCase`
- `DomainReadUseCase`
- `DomainService`
- `DomainQueryService`

즉,

- 외부에는 UseCase 인터페이스를 노출하고
- 내부에서는 Service 구현체가 이를 수행하는 구조

를 의도하고 있다.

이 방식의 장점은 다음과 같다.

- Controller가 구현체 대신 인터페이스에 의존 가능
- 기능 단위로 책임을 명확히 분리 가능
- 테스트 대역 구성 용이

UseCase는 각각 다음에서 구현한다.
- `DomainService`
  - `DomainCreateUseCase`
  - `DomainReadUseCase`

- `DomainQueryService`
  - `DomainQueryUseCase`

Query와 Command를 분리한다.

#### exception
도메인 또는 애플리케이션 전용 예외 집합이다.

- API 계층도 이를 참조한다.
- Service도 이를 참조한다.
- 즉, 공통 예외 체계를 계층 전반에서 공유한다.

---

### 4.3 Outbound 영역

Outbound에는 다음 요소들이 등장한다.

- `infrastructure (out-port)`
- `repository-{type}`
- `message-queue-{type}`

#### infrastructure (out-port)
이름은 infrastructure지만 실제 설명은 **out-port**다.
즉, 이 계층은 기술 구현체가 아니라 **도메인에서 필요로 하는 외부 의존 계약**을 뜻한다.

예:

- `DomainRepository`
- `PaymentGatewayPort`
- `NotificationPort`

즉, 비즈니스 로직은 구체 JPA 구현체를 모르는 상태로,
필요한 저장/조회/외부 호출 계약만 알고 있어야 한다는 뜻이다.

#### repository-{type}
실제 저장소 기술에 종속되는 구현 계층이다.

예:

- `repository-jpa`
- `repository-r2dbc`
- `repository-mongo`

두 번째 다이어그램 기준으로는

- `DomainJpaRepository`
- `DomainJpaEntity`
- `DomainRepositoryImpl`

구조를 의미한다.

즉,

- Spring Data JPA Repository
- DB 엔티티
- 도메인 포트를 구현하는 어댑터

로 분리하는 전형적인 형태다.

#### message-queue-{type}
이벤트 발행/소비, 메시징 연동 구현 계층이다.

예:

- `message-queue-kafka`
- `message-queue-rabbitmq`

도메인 서비스가 메시지 브로커 기술에 직접 의존하지 않도록,
이 역시 Outbound Adapter로 분리하려는 설계다.

---

## 5. 핵심 의존 방향

이 파일에서 가장 중요한 규칙은 의존 방향이다.

### 5.1 허용되는 기본 방향

허용되는 흐름은 아래와 같다.

```text
API -> UseCase/Service -> Out-Port -> Adapter 구현체
```

조금 더 상세히 쓰면 다음과 같다.

```text
api / api-internal / api-operation
    -> use-case interface or service
    -> infrastructure(out-port)
    -> repository-{type}, message-queue-{type}
```

### 5.2 참조 가능한 공통 요소

여러 계층이 공통으로 참조하는 대상은 다음으로 보인다.

- exception
- model

다만 model 참조는 문서에 `optional`이라고 적혀 있으므로,
팀 내에서 완전히 합의된 규칙은 아닌 것으로 보인다.

특히 다음 문구가 중요하다.

> `inport ..> model : optional, 새로운 repo는 연결을 끊는 것이 default?`

이 문구는 다음처럼 해석된다.

- 예전 구조에서는 in-port가 model을 직접 참조했을 수 있음
- 하지만 앞으로는 in-port와 model의 결합을 줄이는 방향을 기본값으로 삼고 싶음
- 즉, **신규 구조에서는 더 느슨한 계약 중심 설계**를 지향함

이는 매우 중요한 설계 방향성이다.

---

## 6. 코드 레벨 의존 관계 해석

실제 클래스 명명 규칙과 의존 방식을 보여준다.

### 6.1 API 계층

예시:

- `DomainApiController`
- `DomainApiRequest`
- `DomainApiResponse`
- `DomainInternalApiController`
- `DomainOperationApiController`

해석은 명확하다.

- Controller는 요청을 받고
- Request/Response DTO를 변환하고
- UseCase를 호출한다.

즉, Controller는 진입점이지 비즈니스 처리 주체가 아니다.

### 6.2 Internal Client 계층

예시:

- `DomainInternalApiClient`
- `DomainInternalApiDto`

이 구조는 타 시스템 내부 API를 호출할 때,
외부 API DTO와는 별개의 내부 통신 DTO를 두려는 설계다.

왜냐하면

- 외부 공개 스펙 변화와
- 내부 시스템 간 계약 변화를

서로 분리할 수 있기 때문이다.


### 6.3 UseCase / Service 계층

예시:

- `DomainCreateUseCase`
- `DomainReadUseCase`
- `DomainQueryUseCase`
- `DomainService`
- `DomainQueryService`
- `HexagonalDomainXXServiceImpl`

이 계층은 실제 비즈니스 로직이 위치하는 곳이다.

즉,

- UseCase는 외부에 보이는 계약
- ServiceImpl은 내부 실행체

로 정리된다.

현재 다이어그램에는 `????` 표기가 있으므로, 어떤 인터페이스를 어떤 구현체가 구현해야 하는지에 대한 표준 명명은 완전히 닫히지 않은 상태다.

하지만 방향성 자체는 명확하다.

- API는 구현체를 직접 알더라도 장기적으로는 UseCase 인터페이스를 바라보게 만들고 싶다.

### 6.4 Repository 계층

예시:

- `DomainRepository`
- `DomainRepositoryImpl`
- `DomainJpaRepository`
- `DomainJpaEntity`

이 구조의 해석은 다음과 같다.

`DomainRepository`
: 도메인 서비스가 의존하는 저장 포트

`DomainRepositoryImpl`
: 포트를 구현하는 어댑터

`DomainJpaRepository`
: Spring Data JPA 인터페이스

`DomainJpaEntity`
: DB 매핑 전용 엔티티

이는 도메인 모델과 영속성 모델을 분리하려는 전형적인 패턴이다.

또한 `DomainJpaEntity ..> DomainModel : (Optional)` 으로, 도메인 모델이 반드시 JPA Entity가 되는것은 아니다

다만 `DomainJpaEntity`는 `DomainModel`을 참조한다.

---

## 7. 애플리케이션 간 의존 관계 해석

첫 번째 다이어그램과 두 번째 다이어그램에는 `Another Application`이 함께 등장한다.

핵심 연결은 다음이다.

```text
현재 애플리케이션의 infrastructure
    -> 다른 애플리케이션의 internal 진입 계약(in-port)
```

또는

```text
AnotherDomainService ..> AnotherDomainProtocolService
```

이것이 의미하는 바는 명확하다.

### 7.1 직접 DB 공유보다 계약 기반 연동을 선호

현재 애플리케이션이 다른 애플리케이션의 데이터가 필요하더라도,
다른 애플리케이션의 Repository나 DB에 직접 의존하지 않고

- internal API client
- internal in-port
- protocol service

같은 계약을 통해 접근하려는 구조다.

즉,

- 앱 A가 앱 B의 DB를 직접 조회하지 않는다.
- 앱 A는 앱 B가 제공하는 내부 계약을 호출한다.

라는 원칙을 내포한다.

### 7.2 애플리케이션 경계를 명시적으로 유지

이 설계는 MSA나 모듈러 모놀리스 양쪽 모두에서 장점이 있다.

- 나중에 분리 배포하기 쉬움
- 경계가 문서상으로 선명함
- 테스트에서 대역 치환이 쉬움
- 팀 간 계약 관리가 수월함

즉, 이 문서는 단순 모듈 설명이 아니라 **경계 보존 전략**까지 포함한다.

---

## 8. 이 파일이 암묵적으로 요구하는 패키지/모듈 규칙

이 가이드를 실무 모듈 구조로 옮기면 대체로 다음처럼 정리할 수 있다.

```text
:domain:model
:domain:service
:domain:exception
:domain:in-port
:domain:out-port
:api
:api-internal
:api-operation
:api-internal-client
:repository-jpa
:mq-kafka
```

또는 도메인 기준으로 묶으면

```text
:member:model
:member:service
:member:in-port
:member:out-port
:member:api
:member:api-internal
:member:repository-jpa
:member:mq-kafka
 
```

같은 형태로도 전개 가능하다.

즉, 이 문서는 최소한 다음 분리를 권장한다고 볼 수 있다.

- 진입 계층 분리
- 도메인 로직 분리
- 저장 기술 분리
- 외부 연동 분리
- 내부 시스템 간 연동 계약 분리

---

## 9. 문서 내 모호한 부분 해석

다이어그램에는 `???`, `????`, `optional`, `migration` 같은 표현이 존재한다.
이는 설계가 아직 완전히 닫히지 않았다는 뜻이며, 아래 해석은 **파일 내부 문맥을 기준으로 한 추론**이다. 작성자의 별도 설명이 없으므로 확정 의미라고 단정할 수는 없다.

### 9.1 `migration`의 의미 추론

`protocol (in-port로 마이그레이션 - optional)` 문구 때문에 이 표현은 비교적 해석이 명확하다.

문맥상 `migration`은 다음 뜻으로 보는 것이 자연스럽다.

- 기존에 `protocol`이라는 이름 또는 계층을 사용해 왔다.
- 하지만 장기적으로는 이를 `in-port` 개념으로 정리하려 한다.
- 즉, 현재 문서는 **과도기 구조**를 설명하고 있다.

따라서 이 문서에서의 `migration`은 “기존 protocol 계층을 새 표준인 in-port 체계로 점진 전환한다”는 의미로 해석하는 것이 가장 타당하다.

### 9.2 `optional`의 의미 추론

`optional`은 문맥상 “허용은 하지만 기본값은 아니다”로 읽는 것이 가장 자연스럽다. 위치별 의미는 조금씩 다르다.

#### 9.2.1 `inport ..> model : optional`

이 표기는 in-port가 model을 직접 참조할 수도 있지만, 그것이 반드시 표준은 아니라는 의미로 보인다.

바로 뒤에 `새로운 repo는 연결을 끊는 것이 default?`라는 메모가 있으므로, 작성자는 신규 구조에서는 다음 방향을 고민하고 있었던 것으로 읽힌다.

- 레거시 또는 단순한 경우에는 in-port가 model을 직접 참조할 수 있다.
- 하지만 신규 구조에서는 in-port와 model의 직접 결합을 줄이거나 끊는 쪽을 기본값으로 삼고 싶다.

즉, `optional`은 “가능은 하지만 신규 권장안은 아닐 수 있음”이라는 뜻에 가깝다.

#### 9.2.2 `DomainJpaEntity ..> DomainModel : (Optional)`

이 표기는 JPA Entity가 Domain Model을 직접 참조하는 구조도 가능하지만 필수는 아니라는 의미로 읽힌다.

즉 다음 두 방식이 모두 열려 있다는 뜻이다.

- JPA Entity가 Domain Model을 직접 참조하거나 변환한다.
- Mapper/Assembler를 별도로 두어 Entity와 Domain Model을 더 강하게 분리한다.

여기서도 `optional`은 “허용은 하지만 강제 아님”에 가깝다.

### 9.3 `???` / `????`의 의미 추론

이 표기는 전부 같은 뜻이라기보다, **라벨을 아직 확정하지 못한 채 관계만 먼저 그려둔 흔적**으로 보는 편이 자연스럽다. 따라서 각 위치별로 따로 해석해야 한다.

#### 9.3.1 `infrastructure --> internal : ????`

바로 아래에 `internal --> another_inport`가 이어지므로, 현재 애플리케이션의 outbound 성격 계층이 다른 애플리케이션의 내부 진입점으로 연결되는 흐름을 표현하려던 것으로 보인다.

따라서 이 표기는 대체로 다음 뜻으로 읽힌다.

- 앱 간 내부 연동
- cross-application call
- 다른 앱의 in-port 또는 internal contract를 통한 호출

즉, 다른 애플리케이션의 DB나 구현체를 직접 건드리지 않고, 상대가 외부에 공개한 내부 계약을 통해 연결한다는 의미다.

#### 9.3.2 `HexagonalDomainXXServiceImpl ..> HexagonalDomainUseCase : ????`

이 표기는 두 번째 다이어그램의 `DomainService ..> DomainCreateUseCase`, `DomainQueryService ..> DomainQueryUseCase`와 같은 위치에 있다. 구조상 가장 자연스러운 해석은 다음과 같다.

- 구현체가 UseCase 인터페이스를 구현한다.
- 구현체가 외부에 노출되는 계약을 제공한다.

즉 이 `????`는 사실상 `implements` 또는 “use case contract 제공”을 뜻한다고 보는 것이 가장 타당하다.

#### 9.3.3 `AnotherDomainService ..> AnotherDomainProtocolService : ????`

이 관계는 현재 애플리케이션의 서비스가 다른 애플리케이션 또는 다른 도메인이 공개한 `protocol` 또는 `in-port` 계약을 바라보는 구조로 보인다.

따라서 의미는 다음과 같이 해석하는 것이 자연스럽다.

- 다른 도메인의 구현체를 직접 참조하지 않는다.
- 다른 도메인이 외부에 공개한 포트/프로토콜만 의존한다.
- cross-domain dependency를 contract 중심으로 제한한다.

즉, 이 `????`는 “다른 도메인의 공개 프로토콜을 통한 호출”이라는 뜻으로 이해하는 것이 가장 적절하다.

### 9.4 protocol vs in-port-internal

가장 먼저 정리해야 할 포인트다.

현재 문맥상으로는 `protocol`과 `in-port-internal`이 역할이 일부 겹친다.

권장 해석은 다음과 같다.

- `in-port-internal`: 내부 호출용 공식 진입 계약
- `protocol`: 과거 인터페이스 또는 전환 중인 레이어

즉, 장기적으로는 하나로 정리하는 편이 좋다.

### 9.5 service와 use-case 명명 통일

그림마다

- `DomainService`
- `DomainXXService`
- `DomainXXServiceImpl`
- `DomainCreateUseCase`

등이 혼재한다.

이 경우 실무에서 가장 중요한 것은 “무엇이 계약이고 무엇이 구현인가”를 통일하는 것이다.

예:

- `CreateOrderUseCase`
- `ReadOrderUseCase`
- `OrderCommandService`
- `OrderQueryService`

처럼 계약과 구현의 레벨을 구분하는 편이 낫다.

### 9.6 model 직접 참조 허용 범위

현재 그림에서는 여러 계층이 model을 참조할 수 있게 되어 있으나,
신규 구조에서는 in-port와 model 결합을 끊고 싶다는 힌트가 있다.

실무적으로는 다음 규칙이 가장 안정적이다.

- API는 API DTO를 사용
- UseCase는 Command/Result 또는 Domain Model을 사용
- Repository Adapter는 Domain Model ↔ Entity 변환 담당
- 외부 시스템용 Client는 내부 통신 DTO 사용

즉, 모든 계층이 동일한 Model 하나를 공유하는 방식은 장기적으로 피하는 편이 낫다.

---

## 10. 최종 해석 요약

이 파일이 말하고자 하는 핵심은 아래와 같이 정리할 수 있다.

### 10.1 구조 원칙

- 애플리케이션은 Inbound / Domain / Outbound로 나눈다.
- 비즈니스 로직은 Service/UseCase에 둔다.
- 저장소, MQ, 외부 연동은 Adapter 계층으로 분리한다.

### 10.2 의존 원칙

- API는 Service/UseCase를 호출한다.
- Service는 Out-Port 인터페이스에 의존한다.
- 구현 기술은 Adapter에서 감춘다.
- 다른 애플리케이션과의 연동은 직접 구현체가 아니라 내부 계약을 통해 수행한다.

### 10.3 경계 원칙

- 외부 공개 API와 내부 API를 분리한다.
- 운영용 API도 별도 경계로 둔다.
- 내부 연동용 Client/DTO를 별도로 둔다.

### 10.4 설계 성숙도

- 전체 방향은 명확하다.
- 다만 naming, protocol 계층, model 참조 범위는 아직 정리 중이다.
- 즉, 이 문서는 완성 규격서라기보다 **표준안을 만들기 위한 설계 초안 + 원칙 문서**에 가깝다.

---

## 11. 권장 문장으로 재정의한 설계 규칙

이 파일의 의도를 문장형 규칙으로 바꾸면 다음과 같다.

1. 모든 진입점은 API 계층에서 시작하며, 비즈니스 로직은 직접 가지지 않는다.
2. 비즈니스 로직은 UseCase 또는 Service 계층에서 수행한다.
3. 도메인 서비스는 영속 기술이나 외부 시스템 구현체를 직접 참조하지 않는다.
4. 저장소, MQ, 외부 시스템 연동은 Out-Port와 Adapter 구조로 분리한다.
5. 다른 애플리케이션과의 연동은 Repository 직접 접근이 아니라 internal API 또는 in-port 계약을 통해 수행한다.
6. 외부 공개 API, 내부 API, 운영 API는 서로 다른 경계로 취급한다.
7. DTO, Domain Model, Persistence Entity는 가능하면 역할을 분리한다.
8. 신규 구조에서는 불필요한 직접 model 참조를 줄이고 계약 중심으로 이동한다.

---

## 12. 결론

이 PlantUML 파일은 헥사고날 아키텍처를 기반으로 한 **모듈 경계 정의서**이자 **코드 네이밍/의존 규칙 초안**이다.

가장 중요한 메시지는 다음 한 줄로 요약할 수 있다.

> 애플리케이션의 핵심 비즈니스 로직은 중심에 두고, API/DB/MQ/타 시스템 연동은 모두 경계 바깥 어댑터로 밀어내며, 애플리케이션 간 연결도 계약 중심으로 유지한다.

즉, 이 문서는 단순 예제 그림이 아니라,

- 모듈 분리 기준
- 클래스 배치 기준
- 애플리케이션 경계 규칙
- 기술 의존 차단 원칙

을 함께 담은 아키텍처 가이드 문서로 해석하는 것이 가장 적절하다.
