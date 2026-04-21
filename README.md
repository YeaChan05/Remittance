# Remittance Service

계좌 등록/삭제, 입금/출금/이체, 거래내역 조회를 제공하는 송금 서비스 과제를 구현하는 프로젝트입니다.

## 문서

- [[docs/rule/code_convention|코드 컨벤션]]
- [[docs/rule/module|모듈 구조]]
- [[dependencies|의존성 규칙]]
- [[docs/opensource|외부 라이브러리 / 오픈소스]]
- [[api_docs|API 문서]]
- [[architecture_guide.puml|모듈 및 코드 의존 가이드라인]]
- 도메인 명세
	- [[transfer/docs/domain|transport]]
	- [[member/docs/domain|member]]
	- [[account/docs/domain|account]]
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

- 전체 조합 로컬 실행: `./gradlew :aggregate:bootRun`
- 전체 조합 통합 테스트: `./gradlew :aggregate:integrationTest`
- 회원 도메인 로컬 실행: `./gradlew :member:application:bootRun`
- 계좌 도메인 로컬 실행: `./gradlew :account:application:bootRun`
- 송금 도메인 로컬 실행: `./gradlew :transfer:application:bootRun`
- 로컬 공용 RabbitMQ는 루트 [[compose.yml]]을 Spring Docker Compose support로 띄운다.
- `aggregate`, `account:application`, `transfer:application`의 `bootRun`은 lifecycle을 `start-only`
  로 사용하므로, 애플리케이션 종료 시 RabbitMQ는 자동으로 내려가지 않는다.
- `test`는 단위/모듈 테스트 경로로 유지하고, 통합 검증은 `integrationTest`를 별도로 실행한다.
- CI는 `test`와 `integrationTest`를 분리 실행하며, `aggregate:integrationTest`는 통합 테스트 경로에 항상 포함된다.

## AOT 실행

- runnable application 모듈(`aggregate`, `member:application`, `account:application`, `transfer:application`)에는 Spring Boot AOT가 적용된다.
- AOT 코드 생성: `./gradlew :aggregate:processAot`
- AOT 테스트 코드 생성: `./gradlew :aggregate:processTestAot`
- AOT 포함 jar 빌드: `./gradlew :aggregate:bootJar`
- AOT 포함 jar 실행: `java -Dspring.aot.enabled=true -jar aggregate/build/libs/aggregate.jar`
- 다른 runnable module도 같은 방식으로 `:{module}:processAot`, `:{module}:processTestAot`, `:{module}:bootJar`를 사용한다.
- Spring AOT는 build 시점 환경을 기준으로 조건을 고정하므로, 프로필이나 조건부 빈에 의존하는 설정은 AOT 빌드 시점 값으로 검증한다.

## 애플리케이션 모듈

- `aggregate`는 여러 도메인을 한 번에 띄워 보는 로컬 조립용 runnable application입니다.
- 정규 API 통합 테스트는 `member:application`, `account:application`, `transfer:application`이 각각 소유합니다.
- 로그인과 회원 인증 책임은 `member` 도메인이 소유합니다.
