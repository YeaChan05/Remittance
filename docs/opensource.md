# 외부 라이브러리 및 오픈소스 사용 현황

## 애플리케이션 의존성

- Springdoc OpenAPI UI: `org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.1` - API 문서/Swagger
  UI 제공
- Micrometer Tracing(Otel Bridge): `io.micrometer:micrometer-tracing-bridge-otel` - 분산 추적
- JSpecify: `org.jspecify:jspecify` - null 표기 등 타입 주석 지원
- MySQL Connector/J: `com.mysql:mysql-connector-j` - MySQL DB 연결 드라이버

## 테스트/개발 도구

- Testcontainers: `org.testcontainers:testcontainers`, `org.testcontainers:junit-jupiter`,
  `org.testcontainers:mysql` - 통합 테스트용 컨테이너 기반 DB 환경

## 빌드 플러그인

- ktlint Gradle Plugin: `org.jlleitschuh.gradle.ktlint` - Kotlin 코드 lint/포맷
- Jacoco: `jacoco` - 코드 커버리지 측정
- Build Recipe Plugin: `com.linecorp.build-recipe-plugin` - 멀티모듈 공통 Gradle 설정

## CI 오픈소스 액션

- sdkman/sdkman-action - SDKMAN 기반 Java 설치
- madrapps/jacoco-report - Jacoco 리포트 생성

## Message Queue

- Spring AMQP :`org.springframework.boot:spring-boot-starter-amqp` - AMQP 프로토콜 기반 메시지 큐 지원
- RabbitMQ : 메시지 브로커로 RabbitMQ 사용
