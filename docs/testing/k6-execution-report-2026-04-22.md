# k6 실행 결과 보고서 (2026-04-22)

## 개요

이 문서는 Remittance 시스템의 정식 k6 시나리오 5개를 **순차로 다시 실행한 최종 결과**를 요약한다. 본 버전은 기본 profile 로그 수집 포맷 정비 후 재실행한
최신 결과를 반영한다.

실행 대상:

- `smoke.js`
- `transfer-correctness.js`
- `concurrent-transfer-contention.js`
- `read-history-load.js`
- `read-history-stress.js`

공통 실행 기준:

- `BASE_URL=http://localhost:8080`
- `API_VERSION=v1`
- `K6_PROMETHEUS_RW_SERVER_URL=http://localhost:9090/api/v1/write`
- `K6_PROMETHEUS_RW_TREND_STATS='p(95),p(99),avg,max'`

## 최종 요약

| 시나리오                                | 실행 결과 | 핵심 지표                                                               | 특이사항                            |
|-------------------------------------|-------|---------------------------------------------------------------------|---------------------------------|
| `smoke.js`                          | PASS  | p95 `102.64ms`, 실패율 `0.00%`, HTTP reqs `9`                          | 기본 사용자 플로우 정상                   |
| `transfer-correctness.js`           | PASS  | p95 `87.53ms`, `expected_status:200` 실패율 `0.00%`, HTTP reqs `12`    | 의도된 `400 conflict` 1건 포함        |
| `concurrent-transfer-contention.js` | PASS  | p95 `65.90ms`, 실패율 `0.00%`, HTTP reqs `28`                          | `contention_business_failure=0` |
| `read-history-load.js`              | PASS  | p95 `21.78ms`, 실패율 `0.00%`, HTTP reqs `216`, iterations `150`       | 30초 고정 부하에서 안정적                 |
| `read-history-stress.js`            | PASS  | p95 `29.61ms`, 실패율 `0.00%`, HTTP reqs `158217`, req/s `1043.807186` | 단계적 stress에서도 안정적               |

## 시나리오별 결과

### 1. smoke

- 상태: PASS
- 목적: 회원가입, 로그인, 계좌 생성, 입금, 이체, 거래내역 조회까지 기본 플로우 확인
- 결과
    - checks: `20/20` 성공
    - `http_req_failed`: `0.00%`
    - `http_req_duration`: avg `34.08ms`, p90 `79.72ms`, p95 `102.64ms`, max `125.56ms`
    - `iteration_duration`: avg `1.31s`

### 2. transfer-correctness

- 상태: PASS
- 목적: 멱등 재시도, body mismatch, history 반영 확인
- 결과
    - checks: `28/28` 성공
    - `http_req_failed{expected_status:200}`: `0.00%`
    - `http_req_duration`: avg `24.28ms`, p90 `64.04ms`, p95 `87.53ms`, max `111.04ms`
    - `http_req_failed` 전체값은 `8.33%`였지만, 이는 의도적으로 발생시킨 `400 Idempotency key conflict` 1건이 포함된 값이다
- 해석
    - 성공 요청 기준으로는 실패가 없었다
    - replay 시 동일 `transferId` 유지, mismatch 시 conflict text 반환이 기대대로 동작했다

### 3. concurrent-transfer-contention

- 상태: PASS
- 목적: 동일 출금 계좌에 동시 요청이 몰릴 때 정합성 확인
- 결과
    - checks: `66/66` 성공
    - `contention_business_failure`: `0`
    - `contention_success`: `10`
    - `http_req_failed`: `0.00%`
    - `http_req_duration`: avg `24.88ms`, p90 `58.93ms`, p95 `65.90ms`, max `121.27ms`
- 해석
    - 기본 경쟁 시나리오에서 비즈니스 실패 없이 모두 성공

### 4. read-history-load

- 상태: PASS
- 목적: 기본 조회 부하 성능 확인
- 실행 파라미터
    - `SEED_TRANSFERS=30`
    - `K6_VUS=5`
    - `K6_DURATION=30s`
- 결과
    - checks: `462/462` 성공
    - `http_req_failed`: `0.00%`
    - `http_req_duration`: avg `16.01ms`, p90 `21.48ms`, p95 `21.78ms`, max `111.82ms`
    - `http_reqs`: `216`
    - `iterations`: `150`
- 해석
    - 기본 조회 부하에서는 매우 안정적

### 5. read-history-stress

- 상태: PASS
- 목적: 단계적 조회 stress에서 임계점 확인
- 실행 파라미터
    - 기본 script 파라미터 사용
    - `SEED_TRANSFERS=100`
    - stage targets: `5 -> 10 -> 20 -> 30 -> 0`
- 결과
    - checks: `316534/316534` 성공
    - `http_req_failed`: `0.00%`
    - `http_req_duration`: avg `11.8ms`, p90 `23.2ms`, p95 `29.61ms`, max `459.34ms`
    - `http_reqs`: `158217`
    - `iterations`: `158011`
    - `req/s`: `1043.807186`
- 해석
    - 기본 stress 구간에서는 뚜렷한 병목 없이 안정적이었다

## 실행 중 관찰 사항

- 최종 결과는 모두 **순차 실행 기준**이다.
- 로그 포맷을 `trace_id`, `span_id`, `trace_flags` 형태로 정비한 뒤 다시 실행했다.
- Loki에서 `transfer.process.start`, `account.create.start`, `transfer.success` 같은 비즈니스 로그에 trace 정보가
  실제로 붙는 것을 확인했다.
- 이전에 병렬 실행 시 `k6` 내부 API 포트 충돌(`127.0.0.1:6565 bind: address already in use`)이 있었으므로, 이 보고서는 그 결과를
  제외하고 다시 실행한 결과만 반영했다.
- `transfer-correctness`의 `400 conflict` 응답은 의도된 검증 경로다.
- Prometheus remote write는 최종 순차 실행 기준으로 결과 수집에 영향을 줄 수준의 오류를 보이지 않았다.

## 결론

- 정식 5개 시나리오는 모두 최종 기준으로 PASS다.
- 현재 시스템은 기본 기능, 멱등성, 동시성, 조회 부하, 조회 stress에 대해 안정적으로 동작했다.
- 조회 성능은 현재 파라미터 범위에서 우수한 편이며, stress에서도 실패 없이 높은 처리량을 유지했다.
- 기본 profile에서도 Loki 로그 수집이 가능하고, 비즈니스 로그 기준 tracing correlation이 확인됐다.

## 후속 권장 액션

1. `mixed-workload` 시나리오 추가
    - 조회 + 입금 + 이체를 섞어 실제 사용 패턴에 가까운 부하 검증
2. `restart-recovery` 시나리오 추가
    - 부하 중 앱 재기동 후 회복 시간 확인
3. `mq-disruption` 시나리오 추가
    - RabbitMQ 중단/복구 시 outbox 및 후처리 안정성 검증

## 추가 실행: Higher-RPS Stress

정식 5개 시나리오 재실행 이후, 조회 stress를 더 공격적인 파라미터로 한 번 더 수행했다.

실행 파라미터:

- `SEED_TRANSFERS=300`
- `HISTORY_LIMIT=300`
- stage targets: `20 -> 40 -> 80 -> 120 -> 0`

결과:

- 상태: PASS
- `http_req_failed`: `0.00%`
- `http_req_duration`: avg `63.93ms`, p90 `133.56ms`, p95 `166.07ms`, max `810.71ms`
- `http_reqs`: `120312`
- `iterations`: `119706`
- `req/s`: `782.543148`
- `checks`: `240924/240924` 성공

해석:

- 120 VU까지 ramp-up 하는 더 강한 조회 stress에서도 실패 없이 버텼다.
- 기본 stress 대비 tail latency는 상승했지만, p95가 여전히 `200ms` 이내로 유지돼 현재 범위에서는 안정적이다.
