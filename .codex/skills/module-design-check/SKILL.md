---
name: module-design-check
description: Execute the Remittance module-boundary checklist against specified modules or changed files. Use when verifying module layout, Gradle dependency direction, aggregate purity, and api-internal exposure.
---

# Module Design Check

## Language

모든 응답은 한국어로 작성한다.

## 수행 규칙

- 코드를 수정하지 않는다.
- API 계약, 비즈니스 로직, 스타일 평가는 하지 않는다.
- 지정된 범위가 없으면 변경된 `build.gradle.kts`, `settings.gradle.kts`, 해당 모듈 디렉터리를 본다.

## 먼저 읽을 파일

- `AGENTS.md`
- `docs/rule/module.md`
- `docs/rule/dependencies.md`
- `docs/rule/code_convention.md`
- `settings.gradle.kts`
- 영향받는 모듈의 `build.gradle.kts`

## 체크리스트

1. 모듈 트리를 확인한다.
- 기대 구조: `{domain}/model`, `infrastructure`, `service`, `exception`, `api`
- 선택 구조: `api-internal`, `repository-jpa`, `schema`, `mq-rabbitmq`
- 비표준 디렉터리를 표시한다.

2. Gradle 의존 방향을 확인한다.
- `model`은 구현 모듈에 의존하지 않아야 한다.
- `infrastructure`는 `{domain}:model`만 본다.
- `service`는 `{domain}:model`, `infrastructure`, `exception` 중심이어야 한다.
- `api`는 `service`, `exception`, `common:api`만 본다.
- `repository-jpa`는 `{domain}:infrastructure`, `common:repository-jpa`만 본다.
- `aggregate`는 조립 전용이어야 한다.

3. cross-domain 노출을 확인한다.
- 직접 의존은 `model -> model` 또는 명시된 `api-internal` 계약만 본다.
- 상대 도메인의 `service`, `repository-jpa`, `api` 직접 참조를 표시한다.

4. 네이밍 흔들림을 확인한다.
- 기술 이름이 클래스 책임을 덮는 경우만 표시한다.

## 결과 작성

- 위 체크리스트에서 어긋난 항목만 적는다.
- 각 항목에 파일 경로와 깨진 규칙을 같이 적는다.
- 문제가 없으면 `문제 없음`과 확인한 범위만 적는다.
