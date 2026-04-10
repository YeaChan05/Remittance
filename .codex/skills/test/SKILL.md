---
name: test
description: Use when the job is to add or update repo-style tests and run the smallest relevant Gradle test task. Do not use for full code review, release handoff, or pure production-code implementation without test work.
---

# When to Use

* 버그 재현, 회귀 방지, wiring 확인을 위해 테스트를 추가/수정해야 하는 경우
* 가장 좁은 `test` 또는 `integrationTest` 명령으로 증거를 만들어야 하는 경우
* 테스트 없이 구현만 하거나 release summary만 작성하는 경우에는 쓰지 않는다

# Inputs

* 검증할 동작 또는 버그 설명
* 대상 모듈/클래스/flow
* 가장 가까운 기존 테스트 파일
* 필요 시 관련 프로덕션 diff

# Steps

1. 순수 로직인지 통합 흐름인지 판단해 source set을 고른다.
2. 가장 가까운 기존 테스트 패턴을 복제해 테스트를 추가/수정한다.
3. 필요한 최소 구현 변경이 있으면 테스트와 같은 범위 안에서만 반영한다.
4. 가장 좁은 Gradle 테스트 명령을 실행하고 결과를 기록한다.

# Output Format

## Test Scope

## Test Changes

## Execution Result

## Next Handoff

* Input: 검증 대상 동작, 관련 테스트 파일, 필요 시 프로덕션 diff
* Output: `/review` 또는 후속 설명이 사용할 테스트 결과와 남은 gap
* Failure Route: 테스트 실패 원인이 구현 문제면 BUILD로, 검증 자체가 부족하면 VERIFY 안에서 보강한다

# Done

* 필요한 테스트가 올바른 source set에 배치됐다
* 가장 좁은 관련 Gradle 검증 결과가 있다
* 미실행 검증이 있으면 분리해서 적었다
