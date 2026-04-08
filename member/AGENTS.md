<!-- OMX:AGENTS-INIT:MANAGED -->
<!-- Parent: ../AGENTS.md -->
# member

이 파일은 `member` 하위 작업에서 루트 `AGENTS.md`보다 우선한다.
member 특화 인증/provider 규칙만 둔다.
상세 stage 운영은 `.codex/agents/WORKFLOW.md`를 따른다.

## 먼저 볼 문서
- `docs/rule/module.md`
- `docs/rule/dependencies.md`
- `docs/filter_arch.md`
- `docs/api/member-signup.md`
- `docs/api/login.md`

## member 구조 규칙
- `member`는 로그인/토큰 발급 책임을 가진다.
- `member:api-internal`은 내부 인증/존재 확인 계약을 제공한다.
- `member:application`은 `member:api`, `member:api-internal`, `repository-jpa`, `schema`를 조립한다.

## 인증/provider 규칙
- internal 인증 transport는 얇게 유지하고 규칙은 `member:service`에 둔다.
- `MemberAuthenticationInternalApi`, `MemberExistenceInternalApi`는 provider-side 계약으로 본다.
- 새 인증 규칙을 추가할 때는 `docs/filter_arch.md`와 함께 본다.

## 구현/검증 규칙
- 가장 가까운 구현/테스트부터 복제한다.
- `member:service:test`, `member:api-internal:test`, 필요 시 `member:application:integrationTest`를 우선 검증 경로로 본다.
- 로그인/토큰 정책을 바꾸면 관련 API 문서도 함께 수정한다.

<!-- OMX:TEAM:WORKER:START -->
<!-- OMX:TEAM:WORKER:END -->
