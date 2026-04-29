# E2E Observability Stack

## 개요

이 문서는 `aggregate` 애플리케이션을 기준으로 로컬 E2E 테스트에서 사용할 observability 스택을 정리한다.

실행 절차와 문제 해결
순서는 [e2e-runbook.md](/Users/shinyechan/IdeaProjects/remittance/docs/testing/e2e-runbook.md) 를 따른다.

구성 요소:

- Prometheus: 애플리케이션/인프라 메트릭 수집
- Grafana: 대시보드와 탐색 UI
- Loki + Promtail: 애플리케이션 로그 수집
- Tempo: OTLP trace 수집 및 조회
- MySQL Exporter / RabbitMQ Exporter: 인프라 메트릭 노출
- Node Exporter: 호스트 CPU / 메모리 등 시스템 메트릭
- cAdvisor: 컨테이너별 CPU / 메모리 / 네트워크 메트릭

## Spring Boot 4 적용 방향

현재 레포는 Spring Boot `4.0.1`을 사용한다. OTLP tracing은 `spring-boot-starter-opentelemetry` 기준으로 붙였고,
Prometheus scrape는 `micrometer-registry-prometheus`를 사용한다.

참고:

- Spring Boot 공식 문서는 OTLP tracing에 `spring-boot-starter-opentelemetry` 사용을 안내한다.
- Prometheus endpoint는 `micrometer-registry-prometheus` 의존성이 있어야 활성화된다.

## 추가된 파일

- `compose.e2e.yml`
- `observability/prometheus/prometheus.yml`
- `observability/loki/loki-config.yml`
- `observability/promtail/promtail-config.yml`
- `observability/tempo/tempo.yml`
- `observability/grafana/bootstrap-grafana.sh`
- `observability/grafana/dashboards/remittance-e2e-overview.json`
- `aggregate/src/main/resources/application-e2e.yml`

## 실행 방법

### 1. observability stack 기동

```bash
docker compose -f compose.e2e.yml up -d
```

`grafana-bootstrap` 서비스가 Grafana API에 접속해 datasource와 Grafana.com dashboard `17175`를 자동 등록한다.

### 2. aggregate 애플리케이션 기동

```bash
SPRING_PROFILES_ACTIVE=e2e ./gradlew :aggregate:bootRun
```

메트릭만 관찰하는 목적이라면 `e2e` profile이 필수는 아니다. 다만 외부 MySQL, trace export, 로그 파일까지 포함해 E2E 환경을 맞추려면 `e2e`
profile 실행을 권장한다.

기본 접속 정보:

- app: `http://localhost:8080`
- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000`
- Tempo: `http://localhost:3200`
- Loki: `http://localhost:3100`
- RabbitMQ Management: `http://localhost:15672`
- MySQL: `localhost:13306`

Grafana 기본 계정:

- id: `admin`
- password: `admin`

Grafana dashboard가 비어 보이면 먼저 아래를 확인한다.

- `docker compose -f compose.e2e.yml ps`
- `docker logs remittance-grafana-bootstrap-1`
- `curl http://localhost:8080/actuator/prometheus`

## 메트릭

Prometheus는 아래 대상을 scrape한다.

- `aggregate` 앱의 `/actuator/prometheus`
- Prometheus 자기 자신
- MySQL exporter
- node exporter
- cadvisor
- RabbitMQ exporter
- Tempo

`aggregate`는 `e2e` profile에서 HTTP server request histogram을 켠다. 그래서 p95/p99 latency를 Prometheus에서 직접
계산할 수 있다.

MySQL CPU 사용량을 보려면:

- host 전체 CPU: `node_exporter`
- MySQL 컨테이너 CPU: `cAdvisor`

를 함께 본다. Grafana overview dashboard에는 `MySQL Container CPU Usage`, `Host CPU Usage` 패널을 추가해 두었다.

macOS Docker Desktop에서는 Linux의 mount propagation(`rslave`) 제약 때문에 `node_exporter` host rootfs 마운트가
제한적일 수 있다. 이 경우:

- `node_exporter`는 완전한 Linux 호스트 메트릭 대신 제한된 파일시스템 관점만 제공할 수 있다.
- MySQL 컨테이너 CPU 사용량 확인 자체는 `cAdvisor` 패널만으로도 충분하다.

## 로그

`aggregate`는 기본 profile에서 `.logs/aggregate.log`, `e2e` profile에서 `.logs/aggregate-e2e.log` 파일로 로그를
남긴다.

로그 패턴에는 아래 값이 포함된다.

- `traceId`
- `spanId`

Promtail은 이 파일을 수집해 Loki로 보낸다. Grafana Loki datasource에는 `traceId=...` 정규식 기반 derived field를 넣어 두었기
때문에, 로그에서 Tempo trace로 바로 이동할 수 있다.

## 트레이스

`aggregate`는 `e2e` profile에서 OTLP trace export를 활성화한다.

기본 endpoint:

- `http://127.0.0.1:4318/v1/traces`

이 endpoint는 `compose.e2e.yml`의 Tempo HTTP OTLP receiver와 연결된다.

샘플링은 E2E 분석 편의를 위해 `1.0`으로 설정했다.

## k6 연동

k6 메트릭을 Prometheus로 보내려면 remote write를 사용한다.

예시:

```bash
K6_PROMETHEUS_RW_SERVER_URL=http://localhost:9090/api/v1/write \
K6_PROMETHEUS_RW_TREND_STATS='p(95),p(99),avg,max' \
k6 run -o experimental-prometheus-rw /Users/shinyechan/IdeaProjects/remittance/k6/scenarios/smoke.js
```

이 구성은 k6 공식 문서의 Prometheus remote write 방식에 맞춘다.

## 주의 사항

- Prometheus는 Docker 내부에서 호스트 앱을 `host.docker.internal:8080`으로 scrape한다.
- 앱이 기동되지 않았거나 `e2e` profile이 아니면 Prometheus scrape는 실패한다.
- `.logs/aggregate.log`, `.logs/aggregate-e2e.log`는 Git에 포함되지 않도록 `.gitignore`에 제외했다.
- 현재 `aggregate`는 `health`, `info`, `prometheus`를 공개해 Prometheus scrape가 가능하도록 맞춰 두었다.

## 다음 확장 후보

- Alertmanager와 경보 규칙 추가
- k6 전용 Grafana dashboard 추가
- RabbitMQ queue depth와 outbox backlog에 대한 경보
- tracing span enrichment(`accountId`, `memberId`, `idempotencyKey` 등) 추가
