<!-- OMX:AGENTS-INIT:MANAGED -->
<!-- Parent: ../AGENTS.md -->
# common

이 파일은 `common` 하위 작업에서 루트 `AGENTS.md`보다 우선한다.
공통 인프라/보안/API helper 규칙만 둔다.
상세 stage 운영은 `.codex/agents/WORKFLOW.md`를 따른다.

## 먼저 볼 문서
- `docs/rule/module.md`
- `docs/rule/dependencies.md`
- `docs/rule/code_convention.md`
- `docs/filter_arch.md`

## common 구조 규칙
- `common`에는 공통 기술 구현만 둔다. 도메인 비즈니스 규칙을 넣지 않는다.
- `common:api`는 MVC argument resolver, exception handler 같은 공통 web helper를 둔다.
- `common:security`는 JWT/internal filter 같은 공통 인증 기술 구현을 둔다.

## 공통 규칙
- 특정 도메인 용어나 계약을 `common`에 끌어올리지 않는다.
- 재사용 가능한 기술 규칙만 둔다.
- 내부 사용자 식별 header는 `/internal/**` 체인에서만 해석한다.

## 구현/검증 규칙
- `common:api:test`, `common:security:test`를 우선 검증 경로로 본다.
- 공통 모듈 변경은 하위 도메인 회귀에 영향이 크므로 관련 application/integration 경로까지 확인한다.

<!-- OMX:TEAM:WORKER:START -->
<!-- OMX:TEAM:WORKER:END -->
