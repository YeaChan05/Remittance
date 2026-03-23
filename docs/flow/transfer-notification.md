# 계좌 이체 알림 흐름

계좌 이체 알림은 outbox + RabbitMQ + SSE를 사용해
송금 성공 이후 수신자에게 실시간 알림을 제공한다.
알림 실패는 송금 성공에 영향을 주지 않는다.

---

## 0. 현재 구현 범위

- 알림 대상은 `TRANSFER_COMPLETED` 이벤트다.
- 이벤트 발행은 `transfer` 도메인의 outbox publisher가 담당한다.
- 이벤트 소비와 중복 방지는 `account:mq-rabbitmq` + `processed_events`가 담당한다.
- 실시간 전송은 `account:api`의 SSE 세션 레지스트리가 담당한다.

---

## 1. 이벤트 원본

송금이 성공하면 `integration.outbox_events`에 이벤트가 저장된다.
현재 알림 처리기가 기대하는 payload 필드는 아래와 같다.

```json
{
  "transferId": 1,
  "toAccountId": 2,
  "fromAccountId": 3,
  "amount": 10000,
  "completedAt": "2026-01-01T10:00:00"
}
```

컨슈머는 RabbitMQ 헤더에서도 아래 값을 기대한다.

- `eventId`
- `eventType = TRANSFER_COMPLETED`

---

## 2. Outbox Publisher

`transfer:mq-rabbitmq`의 publisher는 스케줄 기반으로 `NEW` outbox를 발행한다.

1. `status = NEW` 이벤트 조회
2. RabbitMQ로 publish
3. 성공 시 `status = SENT`
4. 실패 시 상태 유지 후 다음 스케줄에서 재시도

중복 publish 가능성은 허용한다.

---

## 3. Consumer 멱등 처리

`account:mq-rabbitmq`의 컨슈머는 아래 순서로 동작한다.

1. 헤더의 `eventId`, `eventType`를 확인한다.
2. `eventType != TRANSFER_COMPLETED` 이거나 `eventId == null` 이면 무시한다.
3. `processed_events`에서 동일 `eventId` 처리 여부를 확인한다.
4. 이미 처리된 이벤트면 종료한다.
5. 최초 처리 이벤트면 payload를 파싱해 알림 처리로 넘긴다.

---

## 4. 알림 처리

실제 알림 로직은 `account:service`의 `TransferNotificationService`가 수행한다.

1. payload에서 `toAccountId`를 읽는다.
2. 계좌를 조회해 수신 회원 `memberId`를 찾는다.
3. 아래 형태의 메시지를 만든다.

```json
{
  "type": "TRANSFER_RECEIVED",
  "transferId": 1,
  "amount": 10000,
  "fromAccountId": 3,
  "occurredAt": "2026-01-01T10:00:00"
}
```

4. `NotificationPushPort`로 push를 시도한다.

push 중 예외가 발생해도 송금 성공은 유지된다.
현재 구현은 push 실패 후에도 해당 이벤트를 `processed_events`에 기록하므로 자동 재시도하지 않는다.

---

## 5. SSE 연결과 전송

현재 SSE 엔드포인트는 아래와 같다.

- `GET /notification/subscribe`
- 인증된 사용자만 연결 가능

세션은 메모리의 `Map<memberId, SseEmitter>`로 관리한다.

- 세션이 있으면 즉시 전송 시도
- 세션이 없으면 push는 무시
- `completion`, `timeout`, `error` 시 세션 정리

---

## 6. 장애/운영 특성

- RabbitMQ 장애 시 outbox는 `NEW` 상태로 남고 이후 스케줄에서 재시도한다.
- consumer 중복 수신은 `processed_events`로 흡수한다.
- SSE 미연결 또는 push 실패는 유실 허용이다.
- 알림은 사용자 UX 개선용 부가 기능이며, 송금 성공 여부를 되돌리지 않는다.

---
