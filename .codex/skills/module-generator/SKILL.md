---
name: module-generator
description: 리미턴스 레포에서 새로운 도메인 모듈 또는 하위 모듈을 생성하는 작업에만 사용한다. 모듈 생성, settings.gradle.kts 반영, 기본 스캐폴드 확인까지의 절차에 집중한다.
---

# 모듈 생성기

## 개요
도메인 모듈/하위 모듈 생성에만 집중한다. 문서 참조 없이 코드베이스와 생성 스크립트 기반으로 작업한다.

## 워크플로

### 1) 입력 수집
- 도메인 이름(소문자, 숫자, 하이픈/언더스코어만 허용)을 확인한다.
- 최상위 패키지(예: org.yechan.remittance)를 확인한다.
- 생성할 하위 모듈 목록을 확정한다(기본: model/service/exception/infrastructure/api).
- 선택 모듈 필요 여부를 확인한다(api-internal, repository-jpa, schema, mq-rabbitmq).

### 2) 스캐폴드 생성
- 가능하면 `module_generator.sh`로 기본 구조를 생성한다.
- 스크립트가 없으면 동일 구조를 수동 생성한다.

### 3) settings.gradle.kts 반영
- 신규 모듈이 `settings.gradle.kts`에 포함되었는지 확인한다.
- 누락 시 `include(":{domain}:{module}")` 형식으로 추가한다.

### 4) build.gradle.kts 최소 구성
- 각 모듈의 `build.gradle.kts`가 생성되었는지 확인한다.
- 스캐폴드가 누락한 경우만 최소 템플릿을 보완한다.

### 5) 추가 모듈 처리
- 요청된 선택 모듈만 생성한다.
- 불필요한 모듈은 만들지 않는다.

### 6) 최소 확인
- 디렉터리 구조와 settings 반영만 검증한다.
- 빌드/테스트 실행은 요청 시에만 진행한다.

## 체크리스트
- 모듈 디렉터리 구조가 표준( src/main/kotlin, src/test/java 등 )을 따른다.
- `settings.gradle.kts`에 신규 모듈이 모두 포함된다.
- 각 모듈의 `build.gradle.kts`가 생성/연결되어 있다.
- 선택 모듈만 생성되었고 불필요한 모듈은 없다.

## 비고
- 문서 파일을 참조하지 않는다.
