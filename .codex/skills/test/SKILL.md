---
name: test
description: Write repo-style unit or integration tests, place them in the correct source set, and run the smallest relevant Gradle test task. Use when adding or updating tests for service, API, JPA, security, MQ, BeanRegistrarDsl wiring, or aggregate flows.
---

# Test

## Language

모든 응답은 한국어로 작성한다.

## 수행 규칙

- 가장 가까운 기존 테스트를 복제한다.
- 테스트 위치와 source set을 먼저 정한다.
- 실행하지 않은 테스트는 실행했다고 적지 않는다.

## 참고 코드 사용 규칙

- 테스트 패턴으로 언급하는 소스 경로는 현재 시점의 참고 예시다.
- 예시 파일은 언제든 이동하거나 바뀔 수 있으므로, 고정 템플릿처럼 사용하지 않는다.
- 항상 작업 시점의 실제 테스트 코드에서 가장 가까운 예시를 다시 찾는다.

## source set 선택

- `src/test`
    - 순수 비즈니스 로직
    - 계산
    - 예외 분기
    - Spring 없이 직접 생성 가능한 대상

- `src/integrationTest`
    - Spring MVC
    - Security
    - JPA / DB
    - MQ
    - Testcontainers
    - BeanRegistrarDsl wiring
    - 직렬화 / 설정 / 전체 흐름

## 절차

1. 가장 가까운 기존 테스트 파일을 찾는다.
2. 같은 패키지, fixture, 메서드 스타일을 복제한다.
3. 실패하는 테스트를 먼저 작성한다.
4. 필요한 경우 최소 구현 변경과 함께 유지한다.
5. 가장 좁은 Gradle 태스크를 실행한다.

## 실행 규칙

- 단위 테스트는 대상 모듈 `test`를 우선 실행한다.
- 통합 테스트는 대상 모듈 `integrationTest`를 우선 실행한다.
- 예:
    - `./gradlew :member:service:test`
    - `./gradlew :transfer:repository-jpa:integrationTest`
    - `./gradlew :aggregate:integrationTest`

## 빠른 패턴

- 테스트 메서드명은 백틱으로 감싼 한국어 설명을 사용한다.
- 단위 테스트는 Spring 없이 fake, stub, fixture를 우선 사용한다.
- 통합 테스트는 repository, MVC, security, 전체 흐름 검증에만 사용한다.
- aggregate API 흐름은 `aggregate/src/integrationTest` 패턴을 우선 복제한다.

## 최소 보고

- 추가 또는 수정한 테스트 파일
- 실행한 Gradle 명령
- 실패한 assertion 또는 미실행 항목
