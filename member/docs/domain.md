# Domain

## Member

---

### Member

- 서비스 사용자
- 이름, 이메일, 비밀번호 해시 기반 식별 정보를 가진다
- 로그인과 회원가입의 직접 대상이다
- 도메인 핵심 개념 (core)

---

### Authentication

- 사용자가 자신이 맞는지 검증하는 규칙
- 이메일/비밀번호를 검증하고 성공 시 토큰 발급 흐름으로 이어진다
- 외부 공개 로그인과 내부 인증 계약 모두의 중심이다
- 도메인 규칙 (core)

---

### Existence

- 특정 `memberId`가 실제 회원으로 존재하는가
- transfer 같은 다른 도메인이 수신자/소유자 검증에 사용한다
- 내부 호출에서 자주 소비되는 provider-side 규칙이다
- 도메인 규칙 (core)

---

### Token Issuance

- 인증 성공 후 access/refresh token을 발급하는 흐름
- 토큰 기술 구현은 `common:security`에 있지만, 발급 책임은 member 도메인이 가진다
- 로그인 유스케이스의 결과물이다
- domain + security boundary

---

### Internal Authentication / Existence API

- 다른 도메인이 member 규칙을 소비하기 위한 provider-side internal 계약
- `MemberAuthenticationInternalApi`, `MemberExistenceInternalApi`가 여기에 해당한다
- transport는 internal 호출용이지만 판단 규칙은 `member:service`에 남는다
- provider integration 성격

---

### 한 줄 요약

- Member: 누구인가
- Authentication: 이 사용자가 진짜 본인인가
- Existence: 이 회원이 실제 존재하는가
- Token Issuance: 검증된 사용자를 어떤 토큰으로 표현할 것인가
- Internal API: 다른 도메인이 member 규칙을 읽는 provider-side 진입점

---

## Related Docs

- `docs/api/member-signup.md`
- `docs/api/login.md`
- `docs/filter_arch.md`
