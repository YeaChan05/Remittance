<!-- AUTONOMY DIRECTIVE — DO NOT REMOVE -->
YOU ARE AN AUTONOMOUS CODING AGENT. EXECUTE TASKS TO COMPLETION WITHOUT ASKING FOR PERMISSION.
DO NOT STOP TO ASK "SHOULD I PROCEED?" — PROCEED. DO NOT WAIT FOR CONFIRMATION ON OBVIOUS NEXT STEPS.
IF BLOCKED, TRY AN ALTERNATIVE APPROACH. ONLY ASK WHEN TRULY AMBIGUOUS OR DESTRUCTIVE.
USE CODEX NATIVE SUBAGENTS FOR INDEPENDENT PARALLEL SUBTASKS WHEN THAT IMPROVES THROUGHPUT. THIS IS COMPLEMENTARY TO OMX TEAM MODE.
<!-- END AUTONOMY DIRECTIVE -->
<!-- omx:generated:agents-md -->

# AGENTS.md

이 파일은 Codex/OMX가 작업 시작 전에 읽는 **경량 런타임 계약서**다.
상세 workflow와 stage 운영은 `.codex/agents/WORKFLOW.md`를 따른다.
여기에는 **판단 우선순위 / 문서 라우팅 / 핵심 금지 규칙 / 최소 실행 규약**만 둔다.

## 1. 먼저 볼 문서
- `README.md`
- `docs/requirements.md`
- `docs/rule/module.md`
- `docs/rule/dependencies.md`
- `docs/rule/code_convention.md`
- `docs/filter_arch.md`
- `docs/flow/transfer.md`
- `docs/flow/transfer-notification.md`
- `docs/api/*.md`
- `.codex/agents/WORKFLOW.md`

## 2. 우선순위
1. user / developer / system
2. 이 `AGENTS.md`
3. `.codex/agents/WORKFLOW.md`
4. `docs/rule/*`
5. `docs/filter_arch.md`, `docs/flow/*`, `docs/api/*`
6. `README.md`, `docs/requirements.md`

문서와 코드가 어긋나면 임의 해석으로 메우지 말고 차이를 드러낸다.
`docs/rule/*` 내부 충돌 시에는 **의존 규칙 > 모듈 규칙 > 코드 컨벤션** 순으로 본다.

## 3. 문서 라우팅
- 구조/모듈/의존: `docs/rule/module.md`, `docs/rule/dependencies.md`
- 인증/내부 호출: `docs/filter_arch.md`
- 코드 스타일/네이밍: `docs/rule/code_convention.md`
- 송금 흐름: `docs/flow/transfer.md`, `docs/flow/transfer-notification.md`
- API 변경: 해당 `docs/api/*.md`

문서가 있는 영역은 코드 탐색보다 문서 라우팅이 우선이다.
문서가 오래됐거나 비어 보이면 문서 확인 후 관련 코드/테스트로 교차 검증한다.

## 4. 핵심 구조 규칙
- `aggregate`는 조립만 담당한다.
- Core는 구현 기술에 직접 의존하지 않는다.
- `service -> other-domain:infrastructure` 금지
- `service -> other-domain:api-internal` 금지
- 내부 통신은 `consumer:infrastructure -> provider:api-internal.internal.contract` 경로만 허용한다.
- `BeanRegistrarDsl`과 가장 가까운 기존 wiring 패턴을 우선 복제한다.
- controller import용 설정은 `{Domain}ApiRegistrar` 이름을 우선 사용한다.

## 5. internal call / assembly 규칙
- 단독 application은 internal 호출이 필요하면 HTTP adapter bean을 assembly 레이어에서 조립한다.
- `aggregate`는 예외적으로 same-process provider-side `api-internal` bean을 직접 조합할 수 있다.
- HTTP vs same-process adapter 선택은 가능하면 application/aggregate에서 한다.
- `{domain}:infrastructure`가 provider 계약 선택까지 떠안지 않도록 한다.
- `aggregate` 예외는 **bean 조립까지만** 허용한다. business logic 참조 허용으로 해석하지 않는다.

## 6. internal user context 규칙
- AOP보다 기존 `@LoginUserId` + `LoginUserIdArgumentResolver` 재사용을 우선한다.
- 기본 해법은 `X-Internal-User-Id` header + `/internal/**` filter + controller parameter 해석이다.
- 외부 공개 경로에서는 internal user header를 절대 사용자 신원으로 해석하지 않는다.
- mixed-mode 전환 중에는 header 없는 기존 internal call 처리 정책을 먼저 고정한다.
- caller identity가 필요한 internal endpoint에만 `@LoginUserId memberId`를 붙인다.

## 7. 최소 실행 규약
- 먼저 문서를 읽고 지배 문서를 짧게 고정한다.
- 가장 가까운 기존 코드/테스트를 먼저 복제한다.
- 새 abstraction보다 삭제 / 재사용 / 경계 정리를 우선한다.
- cleanup/refactor/deslop 작업은 cleanup plan 먼저 작성한다.
- behavior가 보호되지 않으면 테스트를 먼저 추가한다.
- 문서 계약이 바뀌면 관련 docs도 같이 수정한다.
- 확신이 낮으면 구조 변경보다 최소 수정 우선이다.
- 실패 시 해결 시도보다 먼저 실패 원인을 짧게 요약한다.
- 검증은 가장 좁은 범위(단일 테스트/모듈 테스트)부터 시작한다.

공식 실행 순서와 stage 의미는 `.codex/agents/WORKFLOW.md`를 따른다.
`ralplan`, `team`, `ralph` 같은 workflow 도구도 해당 문서 기준으로 해석한다.

## 8. 자주 쓰는 명령
- 전체 빌드: `./gradlew build`
- 단위 테스트: `./gradlew test`
- 통합 테스트: `./gradlew integrationTest`
- 전체 검증: `./gradlew check`
- 포맷: `./gradlew ktlintFormat`
- 실행: `./gradlew :aggregate:bootRun`

가능하면 가장 좁은 모듈/테스트 명령부터 실행한다.

## 9. Commit 규칙
- subject는 가능한 한 기존 레포 스타일을 따른다.
  - 예: `refactor(security): ...`, `build: ...`, `docs(security): ...`
- commit body/trailer는 Lore protocol을 따른다.
- 자세한 commit protocol 설명은 이 파일에 반복하지 말고, 필요 시 기존 관례와 최근 커밋을 먼저 본다.

## 10. 완료 보고 형식
최종 보고에는 항상 아래를 포함한다.
- changed files
- simplifications made
- tests / lint / diagnostics evidence
- remaining risks

장문 사고과정은 제외하고, 실행 명령 + 핵심 결과 + 남은 리스크 위주로 압축한다.

## 11. 하지 말 것
- 문서 없이 새로운 도메인 용어/계약을 임의로 만들지 않는다.
- `service`에서 구현 모듈이나 타 도메인 `api-internal`을 직접 참조하지 않는다.
- `api`에서 repository를 직접 호출하지 않는다.
- 이유 없는 새 dependency를 추가하지 않는다.
- 장문 설명으로 이 파일을 다시 백과사전화하지 않는다. 상세 설명은 문서로 넘긴다.

<!-- OMX:RUNTIME:START -->
<!-- OMX:RUNTIME:END -->
<!-- OMX:TEAM:WORKER:START -->
<!-- OMX:TEAM:WORKER:END -->
