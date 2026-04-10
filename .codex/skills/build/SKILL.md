---
name: build
description: Use when an approved spec or plan already exists and one acceptance criterion must be implemented with a smallest-test-first BUILD loop. Do not use for vague discovery work, broad refactors without approved scope, or pure verification/review tasks.
---

# When to Use

* 승인된 spec, PRD, plan 중 하나가 이미 있고 구현만 남은 경우
* `/build` 단계처럼 하나의 acceptance criterion을 최소 수정으로 옮겨야 하는 경우
* 테스트-구현-좁은 검증 루프를 반복해야 하는 경우
* 요구사항 정리, 리뷰, 배포 handoff만 필요한 경우에는 쓰지 않는다

# Inputs

* 승인된 spec / plan / acceptance criteria
* 구현할 기준이 드러나는 기존 테스트 또는 새 failing test 대상
* 수정 대상 코드와 가장 가까운 기존 구현 예시

# Steps

1. 이번 루프에서 구현할 acceptance criterion 하나를 고른다.
2. 그 기준을 드러내는 가장 작은 failing test를 추가하거나 기존 테스트를 보강한다.
3. 테스트를 통과시키는 최소 프로덕션 코드만 수정한다.
4. bean 노출이 바뀌면 필요한 wiring 파일만 같은 범위에서 같이 맞춘다.
5. 가장 좁은 검증 명령을 실행하고, 실패하면 같은 범위 안에서 다시 반복한다.

# Output Format

## Target Criterion

## Changed Files

## Verification

## Next Handoff

* Input: 승인된 plan과 이번 루프의 acceptance criterion
* Output: `/test`가 검증할 코드 변경과 좁은 검증 결과
* Failure Route: 구현이 검증을 통과하지 못하면 BUILD 단계에서 같은 criterion으로 반복한다

# Done

* 선택한 acceptance criterion이 코드와 테스트에 반영됐다
* 가장 좁은 관련 검증 결과가 확보됐다
* 남은 blocker나 후속 criterion이 있으면 분리해서 적었다
