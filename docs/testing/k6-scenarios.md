# k6 시나리오 가이드

전체 실행 순서와 observability 확인
절차는 [e2e-runbook.md](/Users/shinyechan/IdeaProjects/remittance/docs/testing/e2e-runbook.md) 를 따른다.

## 시나리오 목록

- [smoke.js](/Users/shinyechan/IdeaProjects/remittance/k6/scenarios/smoke.js)
    - 회원가입, 로그인, 계좌 생성, 입금, 이체, 거래내역 조회까지 한 번 검증
- [read-history-load.js](/Users/shinyechan/IdeaProjects/remittance/k6/scenarios/read-history-load.js)
    - 거래내역을 미리 seed한 뒤 `GET /transfers` 조회 부하 측정
- [read-history-stress.js](/Users/shinyechan/IdeaProjects/remittance/k6/scenarios/read-history-stress.js)
    - 거래내역 조회 부하를 단계적으로 올리며 stress 구간을 확인
- [transfer-correctness.js](/Users/shinyechan/IdeaProjects/remittance/k6/scenarios/transfer-correctness.js)
    - 같은 멱등키 재시도와 body mismatch 처리, 양쪽 계좌 거래내역 반영 확인
- [concurrent-transfer-contention.js](/Users/shinyechan/IdeaProjects/remittance/k6/scenarios/concurrent-transfer-contention.js)
    - 동일 출금 계좌에 여러 VU가 동시에 이체를 시도하는 경쟁 시나리오
- [fan-in-transfer-to-one-account.js](/Users/shinyechan/IdeaProjects/remittance/k6/scenarios/fan-in-transfer-to-one-account.js)
    - 다수의 송금자가 하나의 수취 계좌로 송금하고, 각 송금자는 송금 전후 본인 잔고를 확인하는 시나리오

## 실행 예시

### 1. smoke

```bash
BASE_URL=http://localhost:8080 \
API_VERSION=v1 \
TEST_ID=local-e2e \
K6_PROMETHEUS_RW_SERVER_URL=http://localhost:9090/api/v1/write \
K6_PROMETHEUS_RW_TREND_STATS='p(95),p(99),avg,max' \
k6 run -o experimental-prometheus-rw /Users/shinyechan/IdeaProjects/remittance/k6/scenarios/smoke.js
```

### 2. read-history-load

```bash
BASE_URL=http://localhost:8080 \
API_VERSION=v1 \
TEST_ID=history-load \
SEED_TRANSFERS=30 \
K6_VUS=5 \
K6_DURATION=30s \
K6_PROMETHEUS_RW_SERVER_URL=http://localhost:9090/api/v1/write \
K6_PROMETHEUS_RW_TREND_STATS='p(95),p(99),avg,max' \
k6 run -o experimental-prometheus-rw /Users/shinyechan/IdeaProjects/remittance/k6/scenarios/read-history-load.js
```

### 3. transfer-correctness

```bash
BASE_URL=http://localhost:8080 \
API_VERSION=v1 \
TEST_ID=transfer-correctness \
DEPOSIT_AMOUNT=500000 \
TRANSFER_AMOUNT=10000 \
K6_PROMETHEUS_RW_SERVER_URL=http://localhost:9090/api/v1/write \
K6_PROMETHEUS_RW_TREND_STATS='p(95),p(99),avg,max' \
k6 run -o experimental-prometheus-rw /Users/shinyechan/IdeaProjects/remittance/k6/scenarios/transfer-correctness.js
```

### 4. concurrent-transfer-contention

```bash
BASE_URL=http://localhost:8080 \
API_VERSION=v1 \
TEST_ID=transfer-contention \
K6_VUS=5 \
K6_ITERATIONS=10 \
TARGET_ACCOUNT_COUNT=3 \
TRANSFER_AMOUNT=10000 \
K6_PROMETHEUS_RW_SERVER_URL=http://localhost:9090/api/v1/write \
K6_PROMETHEUS_RW_TREND_STATS='p(95),p(99),avg,max' \
k6 run -o experimental-prometheus-rw /Users/shinyechan/IdeaProjects/remittance/k6/scenarios/concurrent-transfer-contention.js
```

### 5. read-history-stress

```bash
BASE_URL=http://localhost:8080 \
API_VERSION=v1 \
TEST_ID=history-stress \
SEED_TRANSFERS=100 \
HISTORY_LIMIT=100 \
STAGE_1_DURATION=30s \
STAGE_1_TARGET=5 \
STAGE_2_DURATION=30s \
STAGE_2_TARGET=10 \
STAGE_3_DURATION=30s \
STAGE_3_TARGET=20 \
STAGE_4_DURATION=30s \
STAGE_4_TARGET=30 \
STAGE_5_DURATION=30s \
STAGE_5_TARGET=0 \
K6_PROMETHEUS_RW_SERVER_URL=http://localhost:9090/api/v1/write \
K6_PROMETHEUS_RW_TREND_STATS='p(95),p(99),avg,max' \
k6 run -o experimental-prometheus-rw /Users/shinyechan/IdeaProjects/remittance/k6/scenarios/read-history-stress.js
```

### 6. fan-in-transfer-to-one-account

```bash
BASE_URL=http://localhost:8080 \
API_VERSION=v1 \
AUTH_INTERNAL_TOKEN=remittance-internal-token \
TEST_ID=fan-in-transfer \
SENDER_COUNT=10000 \
K6_VUS=50 \
K6_ITERATIONS=10000 \
INITIAL_DEPOSIT=200000 \
TRANSFER_AMOUNT=1000 \
K6_PROMETHEUS_RW_SERVER_URL=http://localhost:9090/api/v1/write \
K6_PROMETHEUS_RW_TREND_STATS='p(95),p(99),avg,max' \
k6 run -o experimental-prometheus-rw /Users/shinyechan/IdeaProjects/remittance/k6/scenarios/fan-in-transfer-to-one-account.js
```

## 공통 주의

- 앱이 실행 중이어야 한다.
- Prometheus가 `/actuator/prometheus`를 scrape 가능한 상태여야 Grafana에서 메트릭을 바로 볼 수 있다.
- public API는 `X-API-Version: v1` 헤더가 필요하며, k6 helper는 기본값으로 `API_VERSION=v1`를 사용한다.
- `zsh`에서는 `K6_PROMETHEUS_RW_TREND_STATS`를 작은따옴표로 감싸는 편이 안전하다.
- `concurrent-transfer-contention.js`는 기본값 기준으로 일 한도 아래에서 동작하도록 잡혀 있다. `K6_VUS`, `K6_ITERATIONS`,
  `TRANSFER_AMOUNT`를 크게 올리면 비즈니스 한도 실패가 섞일 수 있다.
- `read-history-stress.js`는 단계별로 VU를 올리므로, 어떤 구간부터 p95/p99가 급등하는지 Grafana에서 같이 보는 것이 중요하다.
- `fan-in-transfer-to-one-account.js`는 송금 전후 잔고 확인을 위해 internal account query endpoint를 사용한다. 최종 수취
  계좌 금액 검증은 DB 조회로 확인하는 것이 안전하다.
