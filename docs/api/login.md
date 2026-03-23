# 로그인

## 개요

- goal: 로그인
- endpoint: `POST /login`
- Content-Type: `application/json`

## request

- body
    - `email`: 회원 이메일 (문자열)
    - `password`: 비밀번호 (문자열)

```http request
POST /login
Content-Type: application/json

{
  "email": "test@test.com",
  "password": "password1!"
}
```

## response

- status: `200 OK`
- body
    - `accessToken`: 액세스 토큰 (문자열)
    - `refreshToken`: 리프레시 토큰 (문자열)
    - `expiresIn`: 액세스 토큰 만료 시간(초)

```json
{
  "accessToken": "eyJhbGciOi...",
  "refreshToken": "eyJhbGciOi...",
  "expiresIn": 3600
}
```

## error

- status: `400 Bad Request`
- context
    - `email` is invalid
    - `password` is invalid

- status: `401 Unauthorized`
- context
    - invalid credentials

- status: `500 Internal Server Error`
- context
    - member not found
