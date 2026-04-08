<!-- OMX:AGENTS-INIT:MANAGED -->
<!-- Parent: ../AGENTS.md -->
# account

이 파일은 `account` 하위 작업에서 루트 `AGENTS.md`보다 우선한다.
account 특화 internal/provider 규칙만 둔다.
상세 stage 운영은 `.codex/agents/WORKFLOW.md`를 따른다.

## 먼저 볼 문서
- `docs/rule/module.md`
- `docs/rule/dependencies.md`
- `docs/filter_arch.md`
- `docs/api/account-create.md`
- `docs/api/account-delete.md`
- `docs/flow/transfer-notification.md`

## account 구조 규칙
- `account:api-internal`은 provider-side internal endpoint를 노출하는 모듈이다.
- `account:service`는 계좌 규칙과 내부 조회/갱신 유스케이스를 가진다.
- `account:application`은 `account:api`, `account:api-internal`, `repository-jpa`, `schema`, `mq-rabbitmq`를 조립한다.

## internal/provider 규칙
- `account:service`는 타 도메인 `infrastructure`나 `api-internal`을 직접 참조하지 않는다.
- caller identity가 실제로 필요한 internal endpoint에만 `@LoginUserId memberId`를 붙인다.
- internal controller는 transport만 처리하고 규칙은 service/use-case에 위임한다.

## 구현/검증 규칙
- 가장 가까운 기존 구현/테스트부터 복제한다.
- `account:service:test`, `account:api-internal:test`, 필요 시 `account:application:integrationTest`를 우선 검증 경로로 본다.
- 문서 계약이 바뀌면 `docs/filter_arch.md`와 관련 API 문서를 함께 본다.

<!-- OMX:TEAM:WORKER:START -->
<!-- OMX:TEAM:WORKER:END -->
