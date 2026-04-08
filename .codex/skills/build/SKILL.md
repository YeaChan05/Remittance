---
name: build
description: Implement an approved plan in the Remittance repo by following the BUILD stage:pick the smallest failing test, write minimal code, and iterate with narrow verification. Use when the team says /build.
---

# Build

## Language

모든 응답은 한국어로 작성한다.

## 목적

`/build` 단계는 승인된 spec / plan을 코드로 옮기는 단계다.
이 단계는 "많이 바꾸기"가 아니라 "필요한 최소 구현 + 좁은 검증"에 집중한다.

## 먼저 확인할 것

1. `AGENTS.md`
2. `.codex/agents/WORKFLOW.md`
3. `.codex/rules/agent-coding-discipline.md`
4. `docs/rule/module.md`
5. `docs/rule/dependencies.md`
6. `docs/rule/code_convention.md`
7. 가장 가까운 기존 구현 1~3개
8. 가장 가까운 기존 테스트 1~3개

## 전제 조건

- DEFINE / PLAN 산출물이 있다.
- 바꿔야 할 acceptance criterion이 식별됐다.

## 수행 순서

1. 가장 먼저 검증할 acceptance criterion 하나를 고른다.
2. 그 기준을 드러내는 가장 작은 failing test를 작성하거나 보강한다.
3. 그 테스트를 통과시키는 최소 프로덕션 코드만 수정한다.
4. 가장 좁은 관련 검증 명령을 실행한다.
5. 실패하면 같은 범위 안에서 구현과 검증을 반복한다.
6. green 이후에만 아주 작은 정리를 허용한다.

## 금지

- plan 밖 리팩터링
- 구조 규칙을 모른 채 우회 구현
- 실행하지 않은 검증을 실행한 것처럼 보고

## 보고 형식

- 먼저 어떤 acceptance criterion을 구현했는지
- 추가/수정한 failing test
- 수정한 파일
- 실행한 검증 명령
- 남은 blocker 또는 gap
