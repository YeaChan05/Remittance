# 멱등키 발급 API

## 개요

- goal: 송금/입금/출금 요청에 사용할 멱등키를 사전에 발급한다.
- endpoint: `POST /idempotency-keys`
- Content-Type: `application/json`
- Authorization: `Bearer {accessToken}`

현재 aggregate 기본 설정에서 멱등키 만료 시간은 `24h`이다.

## request

- query
    - `scope` (선택): `TRANSFER | DEPOSIT | WITHDRAW`
    - 미지정 시 기본값: `TRANSFER`

```http request
POST /idempotency-keys?scope=TRANSFER
Content-Type: application/json
Authorization: Bearer {accessToken}
```

## response

- status: `200 OK`
- body
    - `idempotencyKey`: 발급된 멱등키(UUID)
    - `expiresAt`: 멱등키 유효 만료 시각(ISO-8601)

```json
{
  "idempotencyKey": "3f9c2b1e-8b2a-4c7d-9a4e-6a9d8c2f1e44",
  "expiresAt": "2026-01-08T10:30:00"
}
```

## error

- status: `401 Unauthorized`
- context
    - authentication required

- status: `500 Internal Server Error`
- context: 멱등키 생성 또는 저장 중 서버 내부 오류
