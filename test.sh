#!/usr/bin/env bash
set -euo pipefail

BASE_URL="http://localhost:8080"
API_VERSION="v1"
TS="$(date +%s)"

EMAIL="curl-demo-${TS}@test.com"
PASSWORD="password1!"
NAME="curl-demo"

COMMON_HEADERS=(
  -H "Content-Type: application/json"
  -H "X-API-Version: ${API_VERSION}"
)

echo "1) 회원가입"
curl -sS -X POST "${BASE_URL}/members" \
  "${COMMON_HEADERS[@]}" \
  -d "{
    \"name\": \"${NAME}\",
    \"email\": \"${EMAIL}\",
    \"password\": \"${PASSWORD}\"
  }" | jq

echo
echo "2) 로그인"
LOGIN_JSON="$(curl -sS -X POST "${BASE_URL}/login" \
  "${COMMON_HEADERS[@]}" \
  -d "{
    \"email\": \"${EMAIL}\",
    \"password\": \"${PASSWORD}\"
  }")"
echo "${LOGIN_JSON}" | jq

ACCESS_TOKEN="$(echo "${LOGIN_JSON}" | jq -r '.accessToken')"
AUTH_HEADERS=(
  -H "Content-Type: application/json"
  -H "X-API-Version: ${API_VERSION}"
  -H "Authorization: Bearer ${ACCESS_TOKEN}"
)

echo
echo "3) 출금 계좌 생성"
FROM_ACCOUNT_JSON="$(curl -sS -X POST "${BASE_URL}/accounts" \
  "${AUTH_HEADERS[@]}" \
  -d "{
    \"bankCode\": \"090\",
    \"accountNumber\": \"100-${TS}-001\",
    \"accountName\": \"송금계좌\"
  }")"
echo "${FROM_ACCOUNT_JSON}" | jq
FROM_ACCOUNT_ID="$(echo "${FROM_ACCOUNT_JSON}" | jq -r '.accountId')"

echo
echo "4) 입금 계좌 생성"
TO_ACCOUNT_JSON="$(curl -sS -X POST "${BASE_URL}/accounts" \
  "${AUTH_HEADERS[@]}" \
  -d "{
    \"bankCode\": \"090\",
    \"accountNumber\": \"100-${TS}-002\",
    \"accountName\": \"수취계좌\"
  }")"
echo "${TO_ACCOUNT_JSON}" | jq
TO_ACCOUNT_ID="$(echo "${TO_ACCOUNT_JSON}" | jq -r '.accountId')"

echo
echo "5) 입금용 멱등키 발급"
DEPOSIT_KEY_JSON="$(curl -sS -X POST "${BASE_URL}/idempotency-keys?scope=DEPOSIT" \
  "${AUTH_HEADERS[@]}")"
echo "${DEPOSIT_KEY_JSON}" | jq
DEPOSIT_KEY="$(echo "${DEPOSIT_KEY_JSON}" | jq -r '.idempotencyKey')"

echo
echo "6) 출금 계좌에 100000원 입금"
curl -sS -X POST "${BASE_URL}/deposits/${DEPOSIT_KEY}" \
  "${AUTH_HEADERS[@]}" \
  -d "{
    \"accountId\": ${FROM_ACCOUNT_ID},
    \"amount\": 100000
  }" | jq

echo
echo "7) 송금용 멱등키 발급"
TRANSFER_KEY_JSON="$(curl -sS -X POST "${BASE_URL}/idempotency-keys?scope=TRANSFER" \
  "${AUTH_HEADERS[@]}")"
echo "${TRANSFER_KEY_JSON}" | jq
TRANSFER_KEY="$(echo "${TRANSFER_KEY_JSON}" | jq -r '.idempotencyKey')"

echo
echo "8) 30000원 송금"
FIRST_TRANSFER_JSON="$(curl -sS -X POST "${BASE_URL}/transfers/${TRANSFER_KEY}" \
  "${AUTH_HEADERS[@]}" \
  -d "{
    \"fromAccountId\": ${FROM_ACCOUNT_ID},
    \"toAccountId\": ${TO_ACCOUNT_ID},
    \"amount\": 30000
  }")"
echo "${FIRST_TRANSFER_JSON}" | jq

echo
echo "9) 같은 멱등키로 재시도"
curl -sS -X POST "${BASE_URL}/transfers/${TRANSFER_KEY}" \
  "${AUTH_HEADERS[@]}" \
  -d "{
    \"fromAccountId\": ${FROM_ACCOUNT_ID},
    \"toAccountId\": ${TO_ACCOUNT_ID},
    \"amount\": 30000
  }" | jq

echo
echo "10) 송금 거래내역 조회"
curl -sS "${BASE_URL}/transfers?accountId=${FROM_ACCOUNT_ID}&limit=10" \
  -H "X-API-Version: ${API_VERSION}" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" | jq
