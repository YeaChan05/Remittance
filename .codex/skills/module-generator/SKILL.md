---
name: module-generator
description: Use when a new Remittance domain module scaffold must be created and `settings.gradle.kts` must be synced without adding business logic. Do not use for changing existing module architecture, writing production code, or running broad validation.
---

# When to Use

* 새 도메인이나 표준 하위 모듈 scaffold를 생성해야 하는 경우
* `module_generator.sh` 또는 동등한 최소 scaffold 작업만 필요한 경우
* 기존 모듈 리팩터링, 비즈니스 로직 작성, 검증 확대가 목적이면 쓰지 않는다

# Inputs

* 도메인 이름
* 최상위 package/group 정보
* application 타입 또는 생성할 표준 하위 모듈 목록
* 선택 adapter 모듈 목록(`api-internal`, `repository-jpa`, `schema`, `mq-rabbitmq` 등)

# Steps

1. 요청한 모듈 구성이 기존 표준 구조와 맞는지 확인한다.
2. 가능하면 `module_generator.sh`를 사용해 요청된 scaffold만 만든다.
3. `settings.gradle.kts`, 각 모듈 디렉터리, `build.gradle.kts` 생성 여부를 확인한다.
4. 추가 dependency 판단이 필요하면 `references/dependencies.md`를 기준으로 누락만 기록한다.

# Output Format

## Generated Modules

## Settings Changes

## Deferred Work

# Done

* 요청된 모듈/디렉터리/기본 build 파일이 생성됐다
* `settings.gradle.kts` include가 동기화됐다
* 비즈니스 로직이나 예제 코드를 추가하지 않았다
