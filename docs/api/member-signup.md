# 회원가입

## 개요

- goal: 신규 회원 등록
- endpoint: `POST /members`
- Content-Type: `application/json`

## request

- body
    - `name`: 회원 이름 (문자열)
    - `email`: 회원 이메일 (문자열)
    - `password`: 비밀번호 (문자열)

```http request
POST /members
Content-Type: application/json

{
  "name": "홍길동",
  "email": "test@test.com",
  "password": "password1!"
}
```

## response

- status: `200 OK`
- body
    - `name`: 등록된 회원 이름 (문자열)

```json
{
  "name": "홍길동"
}
```

## error

- status: `400 Bad Request`
- context
    - `email` is invalid
    - `password` is invalid

- status: `500 Internal Server Error`
- context
    - duplicate email
    - invalid password during hashing
