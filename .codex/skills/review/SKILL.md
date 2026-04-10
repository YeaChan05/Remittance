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
4. pass / partial / fail 중 하나로 게이트 결론을 낸다.

# Output Format

## Findings

## Gate Decision

## Follow-ups

# Done

* finding 유무와 심각도가 구분돼 있다
* 게이트 결론에 근거가 붙어 있다
* SHIP으로 넘길 수 있는지 여부가 명확하다
