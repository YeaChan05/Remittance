---
name: review
description: Run the REVIEW stage QA gate for the Remittance repo after tests are green. Check findings first, spec conformance, boundary rules, documentation impact, and release risk. Use when the team says /review.
---

# Review

## Language

모든 응답은 한국어로 작성한다.

## 목적

`/review` 단계는 테스트 green 이후 최종 QA gate다.
통과 여부를 결정하는 단계이며, 요약보다 findings가 우선이다.

## 먼저 확인할 것

1. `AGENTS.md`
2. `.codex/agents/WORKFLOW.md`
3. `.codex/rules/agent-coding-discipline.md`
4. 관련 spec / PRD / test-spec
5. 변경 파일 diff
6. 실행된 테스트와 그 결과

## 수행 순서

1. 버그, 회귀, 경계 위반, 누락 테스트를 우선 찾는다.
2. 승인된 spec / acceptance criteria를 실제 코드가 만족하는지 대조한다.
3. 문서 동기화 필요성을 점검한다.
4. 보안 또는 아키텍처 영향이 있으면 그 관점의 추가 검토 필요 여부를 판단한다.
5. 통과 / 부분 통과 / 반려를 명시한다.

## 출력 규칙

- findings first
- 파일/근거를 붙인다
- finding이 없으면 명시적으로 없다고 적는다
- 남은 리스크와 미실행 검증을 숨기지 않는다

## 종료 기준

- 주요 finding이 없거나, 있으면 BUILD로 되돌릴 명확한 이유가 있다.
- spec 적합성에 대한 판단 근거가 있다.
- SHIP으로 넘겨도 되는지 결정됐다.
