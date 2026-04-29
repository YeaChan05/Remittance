# E2E 실행 런북

## 목적

이 문서는 Remittance 시스템의 E2E 테스트와 observability를 실제로 실행하는 절차를 한 곳에 정리한 런북이다.

관련 문서:

- [observability-stack.md](/Users/shinyechan/IdeaProjects/remittance/docs/testing/observability-stack.md)
- [k6-scenarios.md](/Users/shinyechan/IdeaProjects/remittance/docs/testing/k6-scenarios.md)
- [k6-e2e-plan.md](/Users/shinyechan/IdeaProjects/remittance/docs/testing/k6-e2e-plan.md)

## 1. 애플리케이션 실행

기본 실행:

```bash
cd /Users/shinyechan/IdeaProjects/remittance
./gradlew :aggregate:bootRun
```

`e2e` profile 실행:

```bash
cd /Users/shinyechan/IdeaProjects/remittance
SPRING_PROFILES_ACTIVE=e2e ./gradlew :aggregate:bootRun
```

정리:

- 메트릭만 보려면 `e2e` profile이 필수는 아니다.
- 외부 MySQL, trace export, 로그 파일까지 포함해 E2E 관측을 맞추려면 `e2e` profile이 권장된다.
- 로그 수집은 기본 profile에서도 `.logs/aggregate.log`를 통해 가능하다.

## 2. observability stack 실행

기동:

```bash
cd /Users/shinyechan/IdeaProjects/remittance
docker compose -f compose.e2e.yml up -d
```

종료:

```bash
cd /Users/shinyechan/IdeaProjects/remittance
docker compose -f compose.e2e.yml down
```

## 3. 상태 확인

앱 메트릭 확인:

```bash
curl http://localhost:8080/actuator/prometheus | head
```

Prometheus target 확인:

```bash
curl http://localhost:9090/api/v1/targets
```

컨테이너 상태 확인:

```bash
docker compose -f compose.e2e.yml ps
```

## 4. Grafana 사용

- URL: `http://localhost:3000`
- id: `admin`
- password: `admin`

기본 대시보드:

- `Spring Boot Observability`
- Grafana.com dashboard id: `17175`

사용 절차:

1. Grafana 접속
2. `Spring Boot Observability` 열기
3. 상단 `Application Name`에서 `aggregate` 선택

대시보드가 비어 보이면 먼저 아래를 확인한다.

1. `curl http://localhost:8080/actuator/prometheus | head`
2. `curl http://localhost:9090/api/v1/targets`
3. `docker logs remittance-grafana-bootstrap-1`

## 5. k6 시나리오 실행

모든 public API 요청은 `X-API-Version: v1` 헤더를 전제로 한다. 현재 k6 helper는 기본값으로 `API_VERSION=v1`를 넣도록 맞춰 두었다.

### smoke

```bash
BASE_URL=http://localhost:8080 \
API_VERSION=v1 \
TEST_ID=local-e2e \
K6_PROMETHEUS_RW_SERVER_URL=http://localhost:9090/api/v1/write \
K6_PROMETHEUS_RW_TREND_STATS='p(95),p(99),avg,max' \
k6 run -o experimental-prometheus-rw /Users/shinyechan/IdeaProjects/remittance/k6/scenarios/smoke.js
```

### transfer-correctness

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

### concurrent-transfer-contention

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

### read-history-load

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

### read-history-stress

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

### fan-in-transfer-to-one-account

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

최종 금액 검증 SQL 예시:

```sql
use core;
select id, balance from account where id = <receiver_account_id>;
select sum(amount) from transfer where to_account_id = <receiver_account_id> and status = 'SUCCEEDED';
```

권장 순서:

1. `smoke.js`
2. `transfer-correctness.js`
3. `concurrent-transfer-contention.js`
4. `read-history-load.js`
5. `read-history-stress.js`

## 6. 자주 보는 문제

### `your-script.js couldn't be found`

원인:

- 예시 placeholder를 그대로 실행했거나 실제 스크립트 경로가 틀린 경우

해결:

- `/Users/shinyechan/IdeaProjects/remittance/k6/scenarios/*.js` 실제 경로를 사용한다.

### `Failed to publish metrics to OTLP receiver ... /v1/metrics 404`

원인:

- 현재 메트릭 수집은 OTLP push가 아니라 Prometheus scrape 방식

대응:

- 애플리케이션을 최신 설정으로 재시작한다.

### Grafana `No data`

원인 후보:

- 앱이 `/actuator/prometheus`를 정상 노출하지 않음
- Prometheus에서 `aggregate` target이 `down`
- Grafana 대시보드에서 `Application Name=aggregate`를 선택하지 않음

### Prometheus target `401`

원인:

- 실행 중인 앱 프로세스가 최신 보안 설정을 반영하지 않은 경우

대응:

- `aggregate`를 재시작한다.

### `/members` 또는 `/login` 이 `401`

원인:

- `X-API-Version: v1` 헤더 없이 public API를 호출한 경우

대응:

- curl, k6, 기타 클라이언트 요청에 `X-API-Version: v1`를 넣는다.

## 7. 참고

- `Spring Boot Observability` 대시보드는 `application` 메트릭 태그를 기준으로 조회한다.
- 현재 각 application 모듈에는 `management.metrics.tags.application=${spring.application.name}`가 설정돼 있다.
- `zsh`에서는 `K6_PROMETHEUS_RW_TREND_STATS`를 작은따옴표로 감싸는 편이 안전하다.
