---
name: module-generator
description: Generate standard Remittance domain modules and submodules with the repo scaffold, sync settings.gradle.kts, and create only the requested folders and files. Use when adding a new domain module or optional submodule.
---

# Module Generator

## Language

모든 응답은 한국어로 작성한다.

## 입력

- 도메인 이름
- 최상위 패키지
- 생성할 하위 모듈 목록
- 선택 모듈 목록: `api-internal`, `repository-jpa`, `schema`, `mq-rabbitmq`

## 절차

1. 가능하면 `module_generator.sh`를 사용한다.
2. 요청된 모듈만 생성한다.
3. `settings.gradle.kts`에 `include(":{domain}:{module}")`를 맞춘다.
4. 각 모듈에 `build.gradle.kts`가 있는지 확인한다.
5. 표준 소스셋만 만든다.
- `src/main/kotlin`
- `src/test/kotlin`
- 필요 시 `src/integrationTest/kotlin`, `src/integrationTest/resources`
6. 여기서 멈춘다.
- 빌드와 테스트는 요청이 있을 때만 실행한다.

## 제한

- 표준 모듈 이름 외의 디렉터리를 만들지 않는다.
- 불필요한 기술 모듈을 미리 만들지 않는다.
- 비즈니스 로직이나 예제 코드를 넣지 않는다.

## 최소 보고

- 생성한 모듈 목록
- 수정한 `settings.gradle.kts` 항목
- 미실행 검증 항목
