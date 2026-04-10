---
name: ship
description: Use when verify and review are complete and you need a release-readiness or handoff summary. Do not use for implementation, review, or real deployment execution unless the user explicitly asks for deployment.
---

# When to Use

* release readiness, go-live handoff, 운영 전달사항을 정리해야 하는 경우
* 변경 범위와 검증 근거를 배포 직전 관점으로 묶어야 하는 경우
* 실제 배포 수행, 구현, 리뷰 단계 자체가 목적이면 쓰지 않는다

# Inputs

* release 대상 범위 또는 변경 diff
* review verdict
* DOCUMENT 완료 여부 또는 no-op 근거
* 실행된 검증 결과
* 문서 반영 여부와 남은 운영 TODO

# Steps

1. REVIEW 결과만이 아니라 DOCUMENT 완료 여부 또는 no-op 근거를 먼저 확인한다.
2. release 범위와 검증 근거를 운영 관점으로 묶는다.
3. 남은 리스크, 미실행 검증, 수동 절차를 구분해 적는다.
4. 실제 배포를 하지 않았다면 readiness 정리까지만 했다고 명시한다.

# Output Format

## Release Scope

## Evidence

## Remaining Risks

## Failure Route

* Input: review verdict, DOCUMENT 확인 결과, 검증 증거
* Output: 운영 handoff 가능한 readiness summary
* Failure Route: DOCUMENT 미완료면 DOCUMENT로, blocking risk가 남으면 REVIEW 또는 BUILD로 되돌린다

# Done

* 운영자가 바로 이어받을 수 있는 handoff 정보가 있다
* 실제 수행한 것과 미수행한 것이 구분돼 있다
