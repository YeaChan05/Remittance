# Remittance Service

계좌 등록/삭제, 입금/출금/이체, 거래내역 조회를 제공하는 송금 서비스 과제를 구현하는 프로젝트입니다.

## 문서

- [코드 컨벤션](docs/rule/code_convention.md)
- [모듈 구조](docs/rule/module.md)
- [의존성 규칙](docs/rule/dependencies.md)
- [외부 라이브러리|오픈소스](docs/opensource.md)
- [API 문서](docs/api)
- [모듈 및 코드 의존 가이드라인](architecture_guide.puml)
## API 요약

- 회원가입 `POST /members`
- 로그인 `POST /login`
- 멱등키 발급 `POST /idempotency-keys`
- 계좌 등록/삭제
- 출금 `POST /withdrawals/{idempotencyKey}` (일 한도 1,000,000원)
- 입금 `POST /deposits/{idempotencyKey}`
- 이체 `POST /transfers/{idempotencyKey}` (수수료 1%, 일 한도 3,000,000원)
- 거래내역 조회 `GET /transfers?accountId=...` (최신순)
- 계좌 이체 알림(SSE) `GET /notification/subscribe`

## 실행 방법

- `./gradlew :aggregate:bootRun`
- Docker Compose 파일은 `aggregate/src/main/resources/docker-compose.yml`에 있습니다.
- aggregate 모듈은 여러 도메인을 한 곳에 묶어 실행하는 통합 실행용 모듈입니다.
