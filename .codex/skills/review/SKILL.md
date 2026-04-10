---
name: review
description: Use when changed code has already been verified and you need a post-verify QA gate for the whole diff. Do not use for architecture-only review, risk-only review, or plan review.
---

# When to Use

* `/review` 단계처럼 변경 diff 전체에 대해 pass / partial / fail을 판단해야 하는 경우
* acceptance criteria 충족 여부와 blocking finding을 같이 봐야 하는 경우
* 구조만 따로 보거나 release risk만 정리하는 작업이면 다른 review skill을 쓴다

# Inputs

* 변경 diff
* 실행된 테스트 / lint / diagnostics 결과
* 승인된 spec / acceptance criteria
* 문서 동기화 여부 판단에 필요한 파일 목록

# Steps

1. blocking finding 후보를 먼저 찾는다.
2. 승인된 요구사항과 실제 구현을 대조한다.
3. 누락된 검증, 문서 동기화 필요, 후속 review 필요성을 판단한다.
4. pass 또는 partial-pass 후보라면 **반드시 DOCUMENT 확인 대상 문서와 no-op 가능성**을 적는다.
5. pass / partial / fail 중 하나로 게이트 결론을 낸다.

# Output Format

## Findings

## Gate Decision

## Document Check

## Failure Route

* Input: 변경 diff, 검증 결과, 승인된 spec
* Output: DOCUMENT에서 확인할 문서 목록 또는 no-op 근거, 그리고 SHIP 가능 여부
* Failure Route: blocking issue면 BUILD로 되돌리고, 문서/계약 mismatch면 DOCUMENT 후 REVIEW reopen 경로를 적는다

# Done

* finding 유무와 심각도가 구분돼 있다
* 게이트 결론에 근거가 붙어 있다
* REVIEW 뒤 DOCUMENT 확인 필요성이 명시돼 있다
* SHIP으로 넘길 수 있는지 여부가 명확하다
