# Domain

## Account

---

### Account

- 회원이 소유하는 계좌
- 은행 코드, 계좌 번호, 별칭, 현재 잔액을 가진다
- `memberId`와 강하게 연결되는 계정 단위다
- 계좌 등록/삭제의 직접 대상이다
- 도메인 핵심 개념 (core)

---

### Balance

- 계좌의 현재 잔액
- 입금/출금/이체 결과가 최종 반영되는 값이다
- 송금 도메인의 요청을 account 도메인이 실제 잔액 변화로 확정한다
- 도메인 핵심 개념 (core)

---

### Ownership

- 계좌를 누가 소유하는가
- 외부 API에서는 JWT의 `memberId`, 내부 호출에서는 caller user context와 비교된다
- 계좌 삭제/조회/송금 가능 여부 판단의 기준이다
- 도메인 규칙 (core)

---

### Internal Query / Update

- 송금 도메인이 계좌를 조회/잠그고 잔액을 갱신하기 위한 provider-side internal 진입점
- `api-internal`이 노출하고, 실제 규칙은 `service`가 가진다
- transport는 내부 호출용이지만 규칙 자체는 account 도메인에 남는다
- provider integration 성격

---

### Notification Subscription

- 수신 알림(SSE) 연결 관리
- transfer 완료 사실을 계좌 소유자에게 전달하기 위한 연결 상태
- 송금 알림 흐름과 맞물리지만 account 쪽 공개 API에서 다룬다
- adapter 성격이 강한 기능

---

### 한 줄 요약

- Account: 누가 가진 어떤 계좌인가
- Balance: 현재 얼마가 남아 있는가
- Ownership: 이 계좌를 조작할 권한이 누구에게 있는가
- Internal Query / Update: 송금 도메인이 계좌 상태를 안전하게 읽고 바꾸는 provider-side 진입점
- Notification Subscription: 수신 알림을 전달하기 위한 연결 상태

---

## Related Docs

- `docs/api/account-create.md`
- `docs/api/account-delete.md`
- `docs/filter_arch.md`
- `docs/flow/transfer-notification.md`
