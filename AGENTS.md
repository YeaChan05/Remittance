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
도메인 특화 규칙은 더 가까운 디렉터리의 `AGENTS.md`가 우선한다.

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
2. 더 가까운 하위 `AGENTS.md`
3. 이 `AGENTS.md`
4. `.codex/agents/WORKFLOW.md`
5. `docs/rule/*`
6. `docs/filter_arch.md`, `docs/flow/*`, `docs/api/*`
7. `README.md`, `docs/requirements.md`

문서와 코드가 어긋나면 임의 해석으로 메우지 말고 차이를 드러낸다.
`docs/rule/*` 내부 충돌 시에는 **의존 규칙 > 모듈 규칙 > 코드 컨벤션** 순으로 본다.

## 3. 문서 라우팅
- 구조/모듈/의존: `docs/rule/module.md`, `docs/rule/dependencies.md`
- 인증/내부 호출: `docs/filter_arch.md`
- 코드 스타일/네이밍: `docs/rule/code_convention.md`
- 흐름: 관련 `docs/flow/*`
- API 변경: 관련 `docs/api/*.md`

문서가 있는 영역은 코드 탐색보다 문서 라우팅이 우선이다.
문서가 오래됐거나 비어 보이면 문서 확인 후 관련 코드/테스트로 교차 검증한다.

## 4. 공통 응답 / 근거 규칙
- 기본 응답 언어는 한국어다. 사용자가 다른 언어를 명시하면 그 지시를 따른다.
- 추측으로 빈칸을 메우지 않는다. 미확인 내용은 `미확인`, `가정`, `추가 확인 필요`로 분리한다.
- 설명, 설계 판단, 리뷰 평결은 문서 / 코드 / diff / 테스트 결과 중 어떤 근거에 기반하는지 드러낸다.
- 검증을 실행했다면 명령과 핵심 결과를 같이 남기고, 실행하지 않았다면 미실행으로 표시한다.
- 완료 판정은 `변경 반영 + 필요한 검증 근거 + 남은 리스크 공개`까지 포함한다.

## 5. 전역 금지 규칙
- `service -> other-domain:infrastructure` 금지
- `service -> other-domain:api-internal` 금지
- `service`에서 구현 모듈 직접 참조 금지
- `api`에서 repository 직접 호출 금지
- 문서 없이 새 도메인 용어/계약을 임의로 만들지 않는다.
- 이유 없는 새 dependency를 추가하지 않는다.

## 6. 최소 실행 규약
- 먼저 문서를 읽고 지배 문서를 짧게 고정한다.
- 가장 가까운 기존 코드/테스트를 먼저 복제한다.
- 새 abstraction보다 삭제 / 재사용 / 경계 정리를 우선한다.
- behavior가 보호되지 않으면 테스트를 먼저 추가한다.
- 문서 계약이 바뀌면 관련 docs도 같이 수정한다.
- 확신이 낮으면 구조 변경보다 최소 수정 우선이다.
- 실패 시 해결 시도보다 먼저 실패 원인을 짧게 요약한다.
- 검증은 가장 좁은 범위(단일 테스트/모듈 테스트)부터 시작한다.

공식 실행 순서와 stage 의미는 `.codex/agents/WORKFLOW.md`를 따른다.
`ralplan`, `team`, `ralph` 같은 workflow 도구도 해당 문서 기준으로 해석한다.

## 7. 자주 쓰는 명령
- 전체 빌드: `./gradlew build`
- 단위 테스트: `./gradlew test`
- 통합 테스트: `./gradlew integrationTest`
- 전체 검증: `./gradlew check`
- 포맷: `./gradlew ktlintFormat`
- 실행: `./gradlew :aggregate:bootRun`

가능하면 가장 좁은 모듈/테스트 명령부터 실행한다.

## 8. Commit 규칙
- subject는 가능한 한 기존 레포 스타일을 따른다.
  - 예: `refactor(security): ...`, `build: ...`, `docs(security): ...`
- commit body/trailer는 Lore protocol을 따른다.
- 자세한 commit protocol 설명은 이 파일에 반복하지 말고, 필요 시 최근 커밋을 먼저 본다.

## 9. 완료 보고 형식
최종 보고에는 항상 아래를 포함한다.
- changed files
- simplifications made
- tests / lint / diagnostics evidence
- remaining risks

장문 사고과정은 제외하고, 실행 명령 + 핵심 결과 + 남은 리스크 위주로 압축한다.

## 10. 하위 규칙 안내
- `.codex/skills/AGENTS.md`: 로컬 skill 작성/분해/포맷 규칙
- `{domain}/AGENTS.md`: 도메인 특화 internal call / assembly / internal user context 규칙

<!-- OMX:RUNTIME:START -->
<!-- OMX:RUNTIME:END -->
<!-- OMX:TEAM:WORKER:START -->
<!-- OMX:TEAM:WORKER:END -->
