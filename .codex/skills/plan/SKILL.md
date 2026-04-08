---
name: plan
description: Turn a defined problem into an approved PRD, task breakdown, architecture-safe execution order, and test-spec for the Remittance repo. Use for the PLAN stage or when the team says /plan.
---

# Plan

## Language

모든 응답은 한국어로 작성한다.

## 목적

`/plan` 단계는 DEFINE 결과를 구현 가능한 실행 계획으로 바꾸는 단계다.
이 단계의 핵심은 "무엇을 만들지"가 아니라 "어떤 순서와 경계로 만들지"를 확정하는 것이다.

## 먼저 확인할 것

1. `AGENTS.md`
2. `.codex/agents/WORKFLOW.md`
3. `.codex/rules/agent-coding-discipline.md`
4. `docs/rule/module.md`
5. `docs/rule/dependencies.md`
6. `docs/rule/code_convention.md`
7. 관련 `docs/api/*`, `docs/flow/*`

## 수행 순서

1. DEFINE 산출물을 받아 범위와 acceptance criteria를 다시 고정한다.
2. PRD 수준의 목표, 비목표, 제약, 구현 단위를 정리한다.
3. 모듈 경계, BeanRegistrarDsl, `api-internal`, 테스트 위치를 먼저 설계한다.
4. BUILD가 바로 착수할 수 있도록 작업 순서와 검증 순서를 정한다.
5. 필요하면 `.omx/plans/prd-*.md`, `.omx/plans/test-spec-*.md` 형태의 아티팩트로 남긴다.

## 종료 기준

- BUILD가 바로 시작 가능한 task sequence가 있다.
- 구조 위반 가능성이 미리 드러났다.
- 어떤 테스트부터 실패시켜야 하는지 설명 가능하다.
