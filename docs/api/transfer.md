# 계좌 이체 API

## 개요

- goal: 출금 계좌에서 다른 계좌로 이체한다.
- endpoint: `POST /transfers/{idempotencyKey}`
- Content-Type: `application/json`
- Authorization: `Bearer {accessToken}`
- 수수료: 이체 금액의 1%를 출금 계좌에서 추가 차감한다.

## request

- path
    - `idempotencyKey`: 멱등키
- body
    - `fromAccountId`: 출금 계좌 ID
    - `toAccountId`: 입금 계좌 ID
    - `amount`: 이체 금액

```http request
POST /transfers/{idempotencyKey}
Content-Type: application/json
Authorization: Bearer {accessToken}

{
  "fromAccountId": 1,
  "toAccountId": 2,
  "amount": 10000
}
```

## response

- status: `200 OK`
- body
    - `status`: `SUCCEEDED | FAILED | IN_PROGRESS`
    - `transferId`: 거래 ID
    - `errorCode`: 실패 코드

`FAILED`는 HTTP 에러 대신 응답 body로 반환된다.
현재 구현에서 자주 나오는 `errorCode`는 아래와 같다.

- `INVALID_REQUEST`
- `ACCOUNT_NOT_FOUND`
- `OWNER_NOT_FOUND`
- `MEMBER_NOT_FOUND`
- `INSUFFICIENT_BALANCE`
- `DAILY_LIMIT_EXCEEDED`

```json
{
  "status": "SUCCEEDED",
  "transferId": 456,
  "errorCode": null
}
```

## error

- status: `400 Bad Request`
- context
    - request body validation failure
    - idempotency key expired
    - same idempotency key with different request body

- status: `401 Unauthorized`
- context
    - authentication required

- status: `404 Not Found`
- context
    - idempotency key not found

- status: `500 Internal Server Error`
- context
    - unexpected persistence failure
