# k6 기반 E2E 테스트 스펙

## 테스트 대상

- 인증: `POST /login`
- 멱등키 발급: `POST /idempotency-keys`
- 입금: `POST /deposits/{idempotencyKey}`
- 출금: `POST /withdrawals/{idempotencyKey}`
- 이체: `POST /transfers/{idempotencyKey}`
- 거래 내역 조회: `GET /transfers`

근거 문서:

- [docs/api/login.md](/Users/shinyechan/IdeaProjects/remittance/docs/api/login.md:1)
- [docs/api/create-idemkey.md](/Users/shinyechan/IdeaProjects/remittance/docs/api/create-idemkey.md:1)
- [docs/api/deposit.md](/Users/shinyechan/IdeaProjects/remittance/docs/api/deposit.md:1)
- [docs/api/withdraw.md](/Users/shinyechan/IdeaProjects/remittance/docs/api/withdraw.md:1)
- [docs/api/transfer.md](/Users/shinyechan/IdeaProjects/remittance/docs/api/transfer.md:1)
- [docs/api/transfer-history.md](/Users/shinyechan/IdeaProjects/remittance/docs/api/transfer-history.md:1)

## 환경 전제

- 애플리케이션은 `aggregate` 조합 실행을 기준으로 한다.
- datasource는 E2E 전용 외부 MySQL을 사용한다.
- RabbitMQ는 별도 container로 구성한다.
- 메트릭, 로그, 트레이스 수집 스택은 같은 compose 네트워크 안에 둔다.
- 각 테스트 run은 고유 `testid`를 가진다.

## 공통 데이터 전제

- 테스트 회원 3종
    - `hot-user`: 조회와 이체 집중 사용자
    - `normal-user`: 일반 부하 사용자
    - `conflict-user`: 정합성/경합 실험용 사용자
- 계좌 pool
    - hot account 10개
    - normal account 100개
    - conflict account 20개
- 거래 데이터
    - 조회 성능 시험 전 hot account당 충분한 과거 거래 이력 사전 적재
    - 정합성 시험 전 conflict account 초기 잔액 고정

## 시나리오 목록

### 1. Smoke

#### 목적

- 환경 기동과 인증/멱등/결제/조회의 기본 연동을 빠르게 확인한다.

#### 흐름

1. 로그인
2. 멱등키 발급
3. 이체 1건 실행
4. 거래 내역 조회
5. verifier로 transfer row 확인

#### 기준

- 전체 오류율 `0%`
- `GET /transfers` p95 < `500ms`
- `POST /transfers/*` p95 < `500ms`

### 2. 조회 성능 Baseline

#### 목적

- 거래 내역 조회의 정상 부하 기준선을 확보한다.

#### 부하 모델

- warm-up 2분
- steady 10분
- VU 또는 arrival-rate 방식 중 하나로 고정
- `accountType=hot|normal` 분리

#### 요청 조합

- `limit=20` 기본 조회
- `limit=100` 중간 조회
- `limit=500` 대용량 조회
- `from/to` 기간 필터 조회

#### 수집 지표

- `http_req_duration`
- `http_req_failed`
- iteration rate
- application request duration
- DB query duration

#### 기준

- `limit=20` 조회 p95 < `300ms`
- `limit=100` 조회 p95 < `500ms`
- 전체 HTTP 실패율 < `1%`

위 수치는 초안 기준선이다. 실제 하드웨어와 seed 규모를 확정한 뒤 재조정한다.

### 3. 조회 Stress

#### 목적

- 임계 부하 구간에서 어느 시점부터 latency와 에러율이 급격히 악화되는지 파악한다.

#### 부하 모델

- 단계 증가형 ramp
- 5분 간격으로 목표 arrival rate 상승
- 마지막 단계에서 grace period 포함

#### 기준

- 한계점 이전 단계의 안정 구간 확보
- 한계점 이후에도 app crash 없이 graceful degradation
- 장애 구간의 trace/log 확보

### 4. 결제 정합성: 동시 이체

#### 목적

- 동일 출금 계좌에 동시 요청이 들어올 때 잔액, 수수료, transfer 결과가 일관되는지 확인한다.

#### 흐름

1. 동일 출금 계좌에 대해 여러 target account로 동시 이체
2. 성공/실패 응답 집계
3. run 종료 후 SQL verifier 수행

#### 불변식

- 잔액은 음수가 되지 않아야 한다.
- 성공 건 합계와 최종 잔액 감소분이 일치해야 한다.
- 수수료 반영분이 기대값과 맞아야 한다.
- 중복 transfer row가 없어야 한다.

### 5. 결제 정합성: 멱등 재시도

#### 목적

- 같은 키 재시도와 body mismatch를 구분해서 처리하는지 확인한다.

#### 흐름

1. 동일 멱등키 + 동일 body 재요청
2. 동일 멱등키 + 다른 body 재요청
3. run 종료 후 상태 검증

#### 기준

- 동일 body는 동일 결과 snapshot으로 귀결
- 다른 body는 conflict 또는 명시적 실패 처리
- transfer row는 중복 생성되지 않음

### 6. 결제 정합성: 실패 경로

#### 목적

- `INSUFFICIENT_BALANCE`, `DAILY_LIMIT_EXCEEDED`, 잘못된 계좌 등 실패 시 side effect가 남지 않는지 검증한다.

#### 기준

- 실패 응답 건은 transfer / 잔액 / outbox 측면에서 기대된 실패 상태만 남김
- 부분 성공 상태가 없어야 함

### 7. 내구성: Soak

#### 목적

- 장시간 steady-state 부하에서 성능 드리프트와 backlog 누적을 감시한다.

#### 부하 모델

- 2시간 이상 고정 부하
- 조회 70%, 결제 30%

#### 수집 지표

- p95/p99 변화
- JVM heap / GC
- queue depth
- outbox `NEW` 개수
- stuck `IN_PROGRESS`

#### 기준

- 오류율과 latency가 지속 악화되지 않음
- backlog가 지속 증가 곡선을 그리지 않음
- run 종료 후 stuck 상태가 허용 범위 이내

### 8. 내구성: 재시작 회복

#### 목적

- 서비스 재시작 또는 RabbitMQ 재기동 이후 회복 시간을 측정한다.

#### 흐름

1. steady traffic 유지
2. 앱 또는 RabbitMQ 재시작
3. readiness 복귀 후 정상 처리량 회복 시간 측정

#### 기준

- readiness 복귀 시간 기록
- 복귀 후 5분 이내 baseline 근처로 회복
- 영구 backlog나 stuck 상태를 남기지 않음

## 관측 스펙

### 메트릭

- k6
    - `http_req_duration`
    - `http_req_failed`
    - `iterations`
    - custom counters
- application
    - request latency
    - JVM
    - DB pool
- broker
    - queue depth
    - publish / ack rate
- correctness
    - verifier mismatch count
    - stuck `IN_PROGRESS`
    - outbox `NEW`

### 로그

- application error log
- idempotency / transfer / outbox 관련 warn log
- RabbitMQ container log
- run tag: `testid`

### 트레이스

- history query sampled traces
- transfer success / failure traces
- restart 전후 degraded traces

## 알림 스펙

- 조회 p95 임계치 초과
- HTTP 실패율 초과
- verifier mismatch 발생
- stuck `IN_PROGRESS` 증가
- outbox `NEW` backlog 증가
- readiness 회복 지연

## 산출 리포트

각 run 종료 후 아래 산출물을 남긴다.

- k6 summary
- Grafana dashboard snapshot 또는 링크
- verifier 결과
- 주요 error log 샘플
- 대표 trace 3건
- pass/fail 판정
- 다음 액션

## 미결정 항목

- 정합성 verifier를 SQL 기반으로 둘지, test-only API로 둘지
- soak duration의 기본값을 2시간으로 고정할지, nightly와 release 후보를 다르게 둘지
- DB와 RabbitMQ exporter의 최종 선택
- 기준 임계치의 초기값을 어떤 환경 스펙에서 확정할지
