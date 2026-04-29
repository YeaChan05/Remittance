# k6 Smoke 실행 가이드

## 스크립트

- [smoke.js](/Users/shinyechan/IdeaProjects/remittance/k6/scenarios/smoke.js)

## 시나리오

아래 흐름을 한 번 수행한다.

1. 회원가입
2. 로그인
3. 계좌 2개 생성
4. 출금 계좌에 입금
5. 멱등키 발급 후 이체
6. 거래내역 조회로 방금 이체한 건 확인

## 실행 예시

```bash
BASE_URL=http://localhost:8080 \
TEST_ID=local-e2e \
K6_PROMETHEUS_RW_SERVER_URL=http://localhost:9090/api/v1/write \
K6_PROMETHEUS_RW_TREND_STATS='p(95),p(99),avg,max' \
k6 run -o experimental-prometheus-rw /Users/shinyechan/IdeaProjects/remittance/k6/scenarios/smoke.js
```

## 선택 환경 변수

- `BASE_URL`: 기본값 `http://localhost:8080`
- `TEST_ID`: 기본값 `local-e2e`
- `BANK_CODE`: 기본값 `090`
- `DEPOSIT_AMOUNT`: 기본값 `200000`
- `TRANSFER_AMOUNT`: 기본값 `10000`
- `HISTORY_LIMIT`: 기본값 `20`
- `K6_VUS`: 기본값 `1`
- `K6_ITERATIONS`: 기본값 `1`
- `SLEEP_SECONDS`: 기본값 `1`

## 주의

- `zsh`에서는 `K6_PROMETHEUS_RW_TREND_STATS` 값을 작은따옴표로 감싸는 편이 안전하다.
- 앱이 `/actuator/prometheus`를 정상 노출하고 Prometheus가 scrape 가능한 상태여야 Grafana 대시보드에 k6 메트릭이 함께 보인다.
