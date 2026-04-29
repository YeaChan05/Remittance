# k6 기반 E2E 테스트 계획

## Plan Summary

### 목표

- `k6`를 기준 도구로 사용해 현재 송금 시스템의 E2E 테스트 체계를 구축한다.
- 핵심 검증 축은 아래 3가지다.
    - 조회 성능 검증: `GET /transfers` 거래 내역 조회의 응답 시간, 처리량, 에러율
    - 결제 정합성 검증: 입금/출금/이체 + 멱등 처리 + outbox 처리의 데이터 일관성
    - 서비스 내구성 검증: 장시간 부하, 재시도, 인프라 흔들림 상황에서의 회복성
- 테스트 결과를 실시간으로 관찰하고, 원인 추적이 가능한 모니터링 구성을 함께 마련한다.

### 현재 상태 요약

- 애플리케이션은 `aggregate` 조합 실행 경로를 제공한다.
  근거: [README.md](/Users/shinyechan/IdeaProjects/remittance/README.md:19)
- `aggregate` 애플리케이션은 Actuator와 Prometheus endpoint를 이미 노출한다.
  근거: [aggregate/src/main/resources/application.yml](/Users/shinyechan/IdeaProjects/remittance/aggregate/src/main/resources/application.yml:24)
- `boot application` 계열 공통 의존성에 `spring-boot-starter-actuator`, `micrometer-tracing-bridge-otel`이
  포함돼 있어 메트릭/트레이싱 확장 여지가 있다.
  근거: [build.gradle.kts](/Users/shinyechan/IdeaProjects/remittance/build.gradle.kts:171)
- 현재 루트 `compose.yml`은 RabbitMQ만 제공하고, 부하 시험용 관측 스택과 전용 데이터 저장소는 아직 없다.
  근거: [compose.yml](/Users/shinyechan/IdeaProjects/remittance/compose.yml:1)
- `aggregate` 기본 datasource는 Testcontainers JDBC를 사용하므로, 성능/내구성 시험에는 외부 MySQL을 분리해 쓰는 구성이 더 적합하다.
  근거: [aggregate/src/main/resources/application.yml](/Users/shinyechan/IdeaProjects/remittance/aggregate/src/main/resources/application.yml:10)

### 범위

#### 포함

- `GET /transfers` 조회 E2E 성능 시험
- `POST /deposits/{idempotencyKey}`, `POST /withdrawals/{idempotencyKey}`,
  `POST /transfers/{idempotencyKey}` 정합성 시험
- `POST /login`, `POST /idempotency-keys`를 포함한 실사용 흐름 기반 시나리오
- 애플리케이션, RabbitMQ, DB, k6 결과, 로그, 트레이스를 함께 보는 관측 체계
- 로컬/개발 환경에서 반복 가능한 실행 절차와 이후 CI 확장 경로

#### 제외

- 프로덕션 규모 튜닝 자체
- 멀티 리전/멀티 AZ 장애 복구 검증
- 브라우저 UI 기반 E2E
- 보안 침투 테스트

### 핵심 제약 및 설계 원칙

- 조회 성능은 애플리케이션 지연만이 아니라 DB 정렬/범위 조건의 비용까지 함께 본다.
- 결제 정합성은 HTTP 응답만으로 판정하지 않고, DB 상태와 멱등/이벤트 상태까지 함께 검증한다.
- 내구성은 평균 latency보다 `오래 버티는 동안 backlog와 stuck state가 누적되지 않는지`를 우선 본다.
- 테스트 실행 단위마다 `testid`를 부여해 메트릭/로그/트레이스를 같은 run으로 묶는다.
- 불변식 검증이 어려운 지점은 black-box만 고집하지 않고, read-only verifier를 허용한다.

## 도구 선택

### 1. 부하 및 시나리오 실행

- `k6`
- 선택 이유
    - HTTP 중심 시나리오 작성이 간단하다.
    - threshold, custom metric, tag 기반 분류가 가능하다.
    - Prometheus remote write를 통해 실시간 시계열 수집이 가능하다.

### 2. 메트릭 수집 및 대시보드

- `Prometheus`
- `Grafana`
- `Alertmanager`
- 역할
    - k6 결과 수집
    - Spring Boot Actuator `/actuator/prometheus` 스크랩
    - RabbitMQ / DB / host 수준 지표 집계
    - 부하 시험 중 임계치 초과 알림

### 3. 로그 수집

- `Loki`
- `Grafana Alloy` 또는 `Promtail`
- 역할
    - 애플리케이션 로그와 컨테이너 로그를 run 단위(`testid`)로 조회
    - 에러율 상승 시 해당 시점 로그를 즉시 drill-down

### 4. 트레이싱

- `OpenTelemetry Collector`
- `Tempo`
- 역할
    - API latency 악화 시 어떤 구간(DB, MQ publish, internal call)이 병목인지 추적
    - 조회/결제 흐름의 span correlation 확보

### 5. 데이터 검증 보조 수단

- 1순위: read-only SQL verifier
- 2순위: test profile 전용 admin/read endpoint

현재 공개 API 문서만으로는 계좌 잔액/멱등/outbox 상태를 모두 검증하기 어렵다. 따라서 정합성 검증은 별도의 읽기 검증 채널이 필요하다.
근거: [docs/api/account-create.md](/Users/shinyechan/IdeaProjects/remittance/docs/api/account-create.md:1), [docs/api/account-delete.md](/Users/shinyechan/IdeaProjects/remittance/docs/api/account-delete.md:1), [docs/api/transfer.md](/Users/shinyechan/IdeaProjects/remittance/docs/api/transfer.md:1)

## Implementation Steps

### 1. 시험 환경 분리

- 목적
    - 성능/내구성 측정값이 Testcontainers JDBC 시작 비용이나 로컬 개발 편의 설정에 오염되지 않게 한다.
- 작업
    - `aggregate`용 E2E profile을 추가한다.
    - datasource를 외부 MySQL로 분리한다.
    - RabbitMQ, MySQL, Prometheus, Grafana, Loki, Tempo, OTel Collector를 별도 compose로 묶는다.
    - 테스트 실행 주체인 k6는 로컬 CLI 또는 별도 container 중 하나로 통일한다.
- 산출물
    - `compose.e2e.yml`
    - `application-e2e.yml`
    - 관측 스택 기본 설정 파일

### 2. 데이터 준비 전략 수립

- 목적
    - 조회 성능과 정합성 검증이 재현 가능한 데이터 집합 위에서 돌아가게 한다.
- 작업
    - 회원/계좌 seed 전략 정의
    - 계좌별 거래량 분포 정의
        - hot account
        - normal account
        - cold account
    - 조회 전용 대용량 거래 내역 seed
    - 결제 정합성 시험용 계좌 pool과 초기 잔액 표준화
- 산출물
    - seed script 또는 seed runner
    - run 시작 전 baseline snapshot

### 3. k6 공통 실행 프레임 구성

- 목적
    - 개별 스크립트를 늘리기 전에 인증, 멱등키 발급, 공통 검증, 결과 태깅을 공용화한다.
- 작업
    - 공통 client 모듈 작성
    - 로그인/멱등키 발급 helper 작성
    - `testid`, `scenario`, `accountType`, `businessFlow` 태그 표준화
    - threshold와 summary 출력 포맷 통일
- 산출물
    - `k6/lib/*`
    - `k6/scenarios/*`
    - 공통 env 규약 문서

### 4. 조회 성능 시나리오 구축

- 목적
    - 거래 내역 조회의 정렬/필터/limit 조합에서 latency와 throughput 특성을 확인한다.
- 작업
    - 기본 조회: `GET /transfers?accountId=...`
    - 기간 필터 조회: `from`, `to`
    - `limit` 변화 시나리오: `20`, `100`, `500`
    - hot account 집중 조회와 다계정 분산 조회 분리
    - steady load, stress, spike를 단계적으로 실행
- 관측 포인트
    - `http_req_duration`, `http_req_failed`
    - 애플리케이션 request latency
    - DB 쿼리 시간
    - GC, CPU, 메모리

### 5. 결제 정합성 시나리오 구축

- 목적
    - 부하가 걸린 상태에서도 잔액, 이체 결과, 멱등 상태, outbox 상태가 일관되는지 검증한다.
- 작업
    - 입금 단독 경쟁 시나리오
    - 출금 단독 경쟁 시나리오
    - 동일 계좌 동시 이체 경쟁 시나리오
    - 동일 멱등키 재시도 시나리오
    - `FAILED`, `IN_PROGRESS`, `SUCCEEDED` 응답 혼합 시나리오
- 검증 불변식
    - 동일 멱등키 + 동일 요청은 결과가 재현 가능해야 한다.
    - 동일 멱등키 + 다른 요청은 conflict 또는 실패로 분리돼야 한다.
    - 성공한 이체는 source/target 계좌와 transfer row가 일치해야 한다.
    - 수수료를 포함한 출금 계좌 감소분이 기대값과 맞아야 한다.
    - `FAILED` 응답은 partial side effect를 남기지 않아야 한다.
    - `TRANSFER` scope 성공 건은 outbox 상태와 연결돼야 한다.
- 특별 주의
    - 현재 문서상 watchdog 없이 `IN_PROGRESS`가 남을 수 있으므로, stuck 건수 추적을 정식 지표로 포함한다.
      근거: [docs/flow/transfer.md](/Users/shinyechan/IdeaProjects/remittance/docs/flow/transfer.md:121)

### 6. 서비스 내구성 시나리오 구축

- 목적
    - 평균 처리량보다 장시간 안정성과 장애 후 회복성을 확인한다.
- 작업
    - 2시간 이상 steady-state soak test
    - 애플리케이션 재시작 중 지속 트래픽
    - RabbitMQ 재기동 중 transfer traffic 유지
    - DB connection pool 압박 상태에서 회복 여부 관찰
    - backlog 증가와 감소 속도 측정
- 통과 기준 예시
    - 에러율이 일정 수준 아래 유지
    - RabbitMQ backlog가 지속적으로 누적되지 않음
    - stuck `IN_PROGRESS`가 run 종료 후 허용 범위를 넘지 않음
    - 재기동 후 일정 시간 내 latency가 정상 범위로 복귀

### 7. 모니터링 대시보드 및 알림 구성

- 기본 대시보드
    - E2E Overview
    - Query Performance
    - Payment Correctness
    - Application Runtime
    - RabbitMQ / Outbox Backlog
    - Trace Drill-down
- 기본 알림
    - 조회 p95/p99 급등
    - HTTP 실패율 초과
    - stuck `IN_PROGRESS` 초과
    - outbox `NEW` backlog 증가
    - RabbitMQ queue depth 급증
    - 앱 재시작 이후 readiness 회복 지연

### 8. 실행 파이프라인 도입

- 1단계
    - 로컬 수동 실행
    - smoke + small load + correctness verifier
- 2단계
    - 개발 서버 nightly run
    - soak test 분리 실행
- 3단계
    - 릴리즈 후보에 대해 baseline 비교 리포트 생성

## Verification Strategy

### 조회 성능

- 기준 지표
    - `p50`, `p95`, `p99`
    - request/sec
    - HTTP error rate
    - timeout rate
- 부가 근거
    - 애플리케이션 Prometheus 메트릭
    - DB slow query 또는 query latency
    - trace 샘플

### 결제 정합성

- 기준 지표
    - HTTP 응답 분포
    - 멱등 상태 분포
    - 성공/실패 건별 transfer row 검증
    - 계좌 잔액 불변식 검증
    - outbox 상태 검증
- 검증 방식
    - k6 custom counter
    - run 종료 후 SQL verifier
    - 이상 케이스 샘플 trace/log 수집

### 서비스 내구성

- 기준 지표
    - 장시간 latency 드리프트
    - queue backlog
    - JVM 메모리/GC 안정성
    - readiness 회복 시간
    - stuck 상태 누적량
- 검증 방식
    - soak test timeline
    - 알림 발생 기록
    - 장애 유도 전후 시계열 비교

## 리스크 및 Open Question

- 계좌 잔액을 공개 API만으로 읽을 수 없는 경우, 정합성 검증 채널을 어디까지 허용할지 결정이 필요하다.
- 조회 성능 목표치(`p95`, `p99`)는 하드웨어와 데이터 규모 기준 없이 확정하면 의미가 약하다.
- 현재 `aggregate`가 Testcontainers JDBC를 기본값으로 쓰므로, 성능 수치 비교는 반드시 E2E 전용 profile에서 해야 한다.
- 트레이싱 bridge는 있으나 exporter/collector wiring은 별도 확인이 필요하다.
- RabbitMQ/DB exporter 선택은 운영 환경과 로컬 편의성 사이 trade-off가 있다.

## Next Handoff

### build 착수 순서

1. E2E 전용 compose + application profile 추가
2. Prometheus/Grafana 최소 관측 스택 도입
3. k6 공통 라이브러리와 smoke 시나리오 작성
4. 거래 조회 성능 시나리오 작성
5. 정합성 verifier 작성
6. soak / restart 시나리오 확장

### 첫 검증 단위

- `login -> idempotency key -> transfer -> history query` 단일 happy-path smoke
- `GET /transfers` 소규모 부하 테스트
- transfer 성공 1건에 대한 DB 정합성 검증 1세트
