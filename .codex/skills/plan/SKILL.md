---
name: plan
description: Use when requirements are already clear enough to turn into an execution plan for the `/plan` stage. Do not use for vague idea discovery, expert-consensus planning, or plan review; use `spec`, `plan-consensus`, or `review-plan` instead.
---

# When to Use

* DEFINE 산출물이 있고 구현 순서와 검증 전략만 정리하면 되는 경우
* 하나의 작업 범위를 실행 가능한 단계로 나눠야 하는 경우
* 요구사항이 아직 모호하거나 기존 plan을 평가하는 작업이면 쓰지 않는다

# Inputs

* spec / PRD / acceptance criteria
* 관련 문서와 영향 범위
* 구현 제약, 리스크, 이미 알려진 open question

# Steps

1. 목표, 범위, 제약을 실행 관점으로 다시 정리한다.
2. 구현 단계를 실제 파일/모듈 단위로 순서화한다.
3. 각 단계에 필요한 검증 전략과 선행 조건을 붙인다.
4. 남은 리스크와 open question을 plan 밖으로 분리한다.

# Output Format

## Plan Summary

## Implementation Steps

## Verification Strategy

## Next Handoff

* Input: spec / PRD / acceptance criteria
* Output: `/build`가 바로 사용할 구현 순서와 `/test` 기준 검증 전략
* Failure Route: 범위/리스크가 정리되지 않으면 DEFINE 또는 plan-consensus / review-plan으로 되돌린다

# Done

* 구현자가 다시 범위를 묻지 않고 바로 착수할 수 있다
* 단계별 검증 방법이 명시돼 있다
* 남은 리스크와 open question이 숨겨지지 않았다
