# 인증 / 보안 구조

## 현재 application 기준 개요

현재 `member:application`, `account:application`, `transfer:application`은 공통으로 아래 보안 구조를 사용한다.

- 공개 로그인 엔드포인트: `POST /login`
- 공개 회원가입 엔드포인트: `POST /members`
- 토큰 검증/파싱/필터: `common:security`
- 경로별 인가 정책: 각 모듈의 `AuthorizeHttpRequestsCustomizer`

## 현재 로그인 경로

실제로 로그인 경로를 노출하는 애플리케이션은 `member:application`이다.

1. `member:api/MemberLoginController`가 `POST /login` 을 받는다.
2. `member:service/MemberQueryUseCase`가 이메일/비밀번호를 검증한다.
3. 같은 서비스에서 `TokenGenerator`로 토큰을 발급한다.

즉, 현재 로그인 요청은 `member:api -> member:service -> common:security` 흐름을 탄다.

## member:api-internal 의 역할

`member:api-internal`은 내부 인증 계약을 제공한다.

- 계약 패키지: `member.internal.contract`
- adapter 패키지: `member.internal.adapter`
- 주요 계약: `MemberAuthenticationInternalApi`, `MemberExistenceInternalApi`
- 주요 DTO: `MemberAuthenticationRequest`, `MemberAuthenticationResponse`, `MemberExistsRequest`,
  `MemberExistsResponse`

같은 규칙으로 transfer는
`transfer:service -> transfer:infrastructure -> member|account:api-internal.internal.contract` 구조를
따른다.

현재 `api-internal.internal.contract`는 Spring HTTP interface(`@HttpExchange`)로 정의된다.
provider application은 `/internal/**` endpoint를 노출하고, consumer는 Spring HTTP Service proxy로 호출한다.

## common:security 의 역할

`common:security`는 공통 보안 기술 구현을 제공한다.

- `TokenParser`, `TokenVerifier`, `TokenGenerator`
- `JwtAuthenticationFilter`
- `InternalServiceAuthenticationFilter`
- `AuthenticationEntryPoint`, `AccessDeniedHandler`
- 기본 `SecurityFilterChain`
- 기본 `AuthorizeHttpRequestsCustomizer`

도메인 간 `/internal/**` 호출은 기본적으로 `X-Internal-Token` shared secret을 유지한다.
추가로, 사용자 컨텍스트가 필요한 내부 호출은 `X-Internal-User-Id` 헤더로 caller memberId를 전달한다.

provider 쪽 `/internal/**` 전용 필터는 이 헤더를 읽어 내부 요청의 `Authentication.name`에 memberId를 세팅할 수 있다.
이 값은 `/internal/**` 체인에서만 해석하며, 외부 공개 경로에서는 절대 사용자 신원으로 해석하지 않는다.

기존 internal caller와의 mixed-mode 전환 기간에는 `X-Internal-User-Id`가 없는 호출도 임시 호환하되, 경고 로그를 남긴다.

각 API 모듈은 자신만의 `AuthorizeHttpRequestsCustomizer`를 추가 등록해 공개 경로를 선언한다.

## 경로 정책

현재 중요한 공개 경로는 아래와 같다.

- `POST /login`
- `POST /members`
- Swagger / OpenAPI 관련 경로

그 외 요청은 기본적으로 인증 대상이다.

## 관리 엔드포인트 처리

현재 aggregate는 Actuator 보안 체인 충돌을 피하기 위해 아래 설정을 사용한다.

```yaml
spring:
  autoconfigure:
    exclude: org.springframework.boot.security.autoconfigure.actuate.web.servlet.ManagementWebSecurityAutoConfiguration
```

## 설정 공유

토큰 관련 설정은 aggregate `application.yml` 기준으로 아래 키를 사용한다.

```yaml
auth:
  token:
    salt: remittance-token-salt
    access-expires-in: ${AUTH_TOKEN_ACCESS_EXPIRES_IN:3600}
    refresh-expires-in: ${AUTH_TOKEN_REFRESH_EXPIRES_IN:604800}
```

## 구현 메모

- `aggregate`는 로컬 조합용 runnable application이며, 정규 API 통합 테스트의 기준점은 아니다.
- `/login` 책임과 회원 인증 책임은 모두 `member` 도메인이 가진다.
- 내부 사용자 식별자 전달은 AOP 대신 기존 `@LoginUserId` / `LoginUserIdArgumentResolver` 패턴 재사용을 우선한다.
- 인증 정책 문서를 수정할 때는 `AggregateSecurityConfiguration`, `CommonSecurityAutoConfiguration`,
  `member:api/MemberLoginController`를 함께 확인한다.
