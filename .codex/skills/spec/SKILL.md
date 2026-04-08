---
name: spec
description: Refine an idea into a clear problem statement, scope, constraints, acceptance criteria, and PRD handoff for the Remittance repo. Use for the DEFINE stage or when the team says /spec.
---

# Spec

## Language

모든 응답은 한국어로 작성한다.

## 목적

`/spec` 단계는 아이디어를 구현 가능한 입력으로 정리하는 단계다.
바로 설계나 구현으로 넘어가지 말고 아래를 먼저 고정한다.

- 문제 정의
- 목표 / 비목표
- 사용자 또는 호출 주체
- 제약과 가정
- acceptance criteria
- open question

## 먼저 확인할 것

1. `AGENTS.md`
2. `.codex/agents/WORKFLOW.md`
3. `.codex/rules/agent-coding-discipline.md`
4. 관련 `docs/api/*`, `docs/flow/*`, `docs/rule/*`
5. 가장 가까운 기존 구현 또는 운영 문서

## 수행 순서

1. 요청을 구현 관점이 아니라 문제 관점으로 다시 정리한다.
2. 범위를 `in scope` / `out of scope`로 분리한다.
3. 사용자 가치와 실패 조건을 분명히 적는다.
4. acceptance criteria를 검증 가능한 문장으로 만든다.
5. unresolved question이 있으면 숨기지 말고 별도 섹션으로 남긴다.
6. 다음 단계 `/plan`이 바로 사용할 수 있도록 PRD 초안 또는 동등한 산출물을 남긴다.

## 산출물

- 문제 정의
- 목표 / 비목표
- acceptance criteria
- open question
- `/plan` 단계로 넘길 요약

## 종료 기준

- 구현자가 "무엇을 만들지"를 다시 묻지 않아도 된다.
- acceptance criteria가 테스트 가능한 수준이다.
- 큰 모호점이 남아 있으면 그대로 표시했다.
