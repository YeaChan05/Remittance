# 입금 API

## 개요

- goal: 특정 계좌에 입금한다.
- endpoint: `POST /deposits/{idempotencyKey}`
- Content-Type: `application/json`
- Authorization: `Bearer {accessToken}`

## request

- path
    - `idempotencyKey`: 멱등키
- body
    - `accountId`: 입금 계좌 ID
    - `amount`: 입금 금액

```http request
POST /deposits/{idempotencyKey}
Content-Type: application/json
Authorization: Bearer {accessToken}

{
  "accountId": 1,
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

```json
{
  "status": "SUCCEEDED",
  "transferId": 789,
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
