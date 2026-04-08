<!-- OMX:AGENTS-INIT:MANAGED -->
<!-- Parent: ../AGENTS.md -->
# aggregate

이 파일은 `aggregate` 하위 작업에서 루트 `AGENTS.md`보다 우선한다.
aggregate 조립 예외 규칙만 둔다.
상세 stage 운영은 `.codex/agents/WORKFLOW.md`를 따른다.

## 먼저 볼 문서
- `docs/rule/module.md`
- `docs/rule/dependencies.md`
- `docs/filter_arch.md`
- 관련 `docs/flow/*`

## aggregate 구조 규칙
- `aggregate`는 조립용 runnable application이다.
- business logic을 구현하지 않는다.
- same-process provider-side `api-internal` bean을 직접 조합할 수 있다.
- 이 예외를 core/service/infrastructure 규칙 완화로 해석하지 않는다.

## 구현/검증 규칙
- 공통 bean 조립, application 설정, integration 환경만 다룬다.
- internal call 변경의 핵심 회귀 검증은 `aggregate:integrationTest`다.
- aggregate에서 돌아간다고 standalone application 구조까지 자동 정당화하지 않는다.

<!-- OMX:TEAM:WORKER:START -->
<!-- OMX:TEAM:WORKER:END -->
