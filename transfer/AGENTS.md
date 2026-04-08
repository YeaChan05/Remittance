<!-- OMX:AGENTS-INIT:MANAGED -->
<!-- Parent: ../AGENTS.md -->
# transfer

이 파일은 `transfer` 하위 작업에서 루트 `AGENTS.md`보다 우선한다.
transfer 특화 internal call / assembly / internal user context 규칙만 둔다.
상세 stage 운영은 `.codex/agents/WORKFLOW.md`를 따른다.

## 먼저 볼 문서
- `docs/rule/module.md`
- `docs/rule/dependencies.md`
- `docs/filter_arch.md`
- `docs/flow/transfer.md`
- `docs/flow/transfer-notification.md`
- 관련 `docs/api/*.md`

## transfer 구조 규칙
- `transfer:service`는 타 도메인 `infrastructure`나 `api-internal`을 직접 참조하지 않는다.
- `transfer:infrastructure`는 consumer port와 transfer-local DTO/identifier에 집중한다.
- cross-domain adapter 선택(HTTP vs same-process)은 가능하면 `transfer:application` 또는 `aggregate`에서 조립한다.

## internal call / assembly 규칙
- `transfer:application`은 internal 호출이 필요하면 HTTP adapter bean을 assembly 레이어에서 조립한다.
- `aggregate`는 예외적으로 same-process provider-side `api-internal` bean을 직접 조합할 수 있다.
- `aggregate` 예외는 bean 조립까지만 허용한다. business logic 경계 참조 허용으로 해석하지 않는다.
- `transfer:infrastructure`가 provider 계약 선택까지 떠안는 방향은 기본안이 아니다.

## internal user context 규칙
- AOP보다 기존 `@LoginUserId` + `LoginUserIdArgumentResolver` 재사용을 우선한다.
- 기본 해법은 `X-Internal-User-Id` header + `/internal/**` 전용 filter + controller parameter 해석이다.
- 외부 공개 경로에서는 internal user header를 절대 사용자 신원으로 해석하지 않는다.
- mixed-mode 전환 중에는 header 없는 기존 internal call 처리 정책을 먼저 고정한다.
- caller identity가 필요한 internal endpoint에만 `@LoginUserId memberId`를 붙인다.

## 구현/검증 규칙
- 가장 가까운 기존 구현/테스트부터 복제한다.
- 경계 변경이면 통합 테스트를 추가한다.
- `transfer:application:integrationTest`와 `aggregate:integrationTest`는 internal call 변경의 핵심 검증 경로다.
- 문서 계약이 바뀌면 `docs/filter_arch.md`, `docs/rule/dependencies.md`, 관련 flow/api 문서를 함께 본다.

<!-- OMX:TEAM:WORKER:START -->
<!-- OMX:TEAM:WORKER:END -->
