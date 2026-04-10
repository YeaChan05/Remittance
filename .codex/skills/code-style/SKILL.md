---
name: code-style
description: Use when the task is to align already-changed files with nearby Remittance naming, package, suffix, and wiring style patterns. Do not use for architecture decisions, feature implementation, or repo-wide rule lookup; those belong to docs and AGENTS.
---

# When to Use

* 수정한 파일의 이름, package, 클래스 suffix, wiring 형태를 주변 패턴에 맞춰야 하는 경우
* 리뷰에서 style drift만 따로 정리해야 하는 경우
* 기능 구현 자체, 아키텍처 판단, 전역 규칙 설명이 목적이면 쓰지 않는다

# Inputs

* 스타일을 맞출 대상 파일 또는 diff
* 가장 가까운 기존 구현/테스트 1~3개
* 이름/배치 판단에 영향을 주는 관련 문서 경로

# Steps

1. 대상과 가장 가까운 reference 파일 1~3개를 고른다.
2. 이름, package, 클래스 suffix, 생성자 형태, bean 등록 모양의 차이를 비교한다.
3. 작업 범위 안에서 style mismatch만 수정한다.
4. reference끼리도 충돌하면 어느 쪽을 따랐는지와 남은 불확실성을 적는다.

# Output Format

## Reference Files

## Alignment Changes

## Remaining Mismatches

# Done

* 대상 파일이 주변 패턴과 같은 naming/package/wiring 모양을 따른다
* 새 스타일 규칙을 임의로 만들지 않았다
* 남은 style ambiguity가 있으면 분리해서 적었다
