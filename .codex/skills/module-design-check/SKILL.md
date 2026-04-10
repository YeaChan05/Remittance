---
name: module-design-check
description: Use when the job is to audit module boundaries, Gradle dependency direction, and cross-domain exposure for specific modules or changed files. Do not use for behavior review, code-style cleanup, or feature implementation.
---

# When to Use

* 특정 모듈이나 diff의 구조/의존 규칙 위반 여부를 확인해야 하는 경우
* aggregate 조립 범위, api-internal 노출, cross-domain 참조를 점검해야 하는 경우
* 기능 구현, 테스트 작성, 일반 코드 리뷰가 목적이면 쓰지 않는다

# Inputs

* 검사 범위(모듈, 디렉터리, diff, build 파일)
* `settings.gradle.kts`
* 영향받는 모듈의 `build.gradle.kts`
* 관련 구조/의존 문서

# Steps

1. 검사 범위의 모듈 트리와 선택 모듈 유무를 확인한다.
2. Gradle 의존 방향이 문서 규칙과 맞는지 확인한다.
3. cross-domain 참조, aggregate purity, api-internal 노출을 점검한다.
4. 위반 항목만 파일 경로와 깨진 규칙 이름으로 정리한다.

# Output Format

## Checked Scope

## Findings

## Rule Mapping

# Done

* 범위 내 구조/의존 위반이 있으면 근거와 함께 적었다
* 위반이 없으면 `문제 없음`과 확인 범위를 명시했다
