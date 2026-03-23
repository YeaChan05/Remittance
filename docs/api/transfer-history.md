# 거래내역 조회 API

## 개요

- goal: 지정된 계좌의 송금/수취 내역을 최신순으로 조회한다.
- endpoint: `GET /transfers`
- Content-Type: `application/json`
- Authorization: `Bearer {accessToken}`

## request

- query
    - `accountId` (필수): 조회 대상 계좌 ID
    - `from` (선택): 조회 시작 시각(ISO-8601)
    - `to` (선택): 조회 종료 시각(ISO-8601)
    - `limit` (선택): 최대 조회 건수

```http request
GET /transfers?accountId=1&from=2025-01-01T00:00:00&to=2025-01-31T23:59:59&limit=20
Authorization: Bearer {accessToken}
```

## response

- status: `200 OK`
- body
    - `transfers`: 거래 목록(최신순)

```json
{
  "transfers": [
    {
      "transferId": 456,
      "fromAccountId": 1,
      "toAccountId": 2,
      "amount": 10000,
      "scope": "TRANSFER",
      "status": "SUCCEEDED",
      "requestedAt": "2025-01-10T10:00:00",
      "completedAt": "2025-01-10T10:00:00"
    }
  ]
}
```

## error

- status: `400 Bad Request`
- context
    - `INVALID_REQUEST`
    - `ACCOUNT_NOT_FOUND`

- status: `401 Unauthorized`
- context
    - authentication required
