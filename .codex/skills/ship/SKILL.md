---
name: ship
description: Run the SHIP stage for the Remittance repo: finalize docs, summarize release readiness, list risks and verification evidence, and prepare go-live handoff. Use when the team says /ship.
---

# Ship

## Language

모든 응답은 한국어로 작성한다.

## 목적

`/ship` 단계는 최종 go-live readiness를 정리하는 단계다.
기본값은 release handoff이며, 실제 배포 명령 실행은 별도 요청 또는 운영 절차가 있을 때만 한다.

## 전제 조건

- VERIFY green
- REVIEW gate pass 또는 승인된 예외만 남음

## 수행 순서

1. 변경으로 영향받는 문서가 있으면 동기화한다.
2. 최종 검증 근거를 짧게 모은다.
3. 남은 리스크와 미실행 항목을 정리한다.
4. 배포/릴리즈 필요 시 필요한 명령 또는 운영 handoff를 제시한다.
5. 실제 배포를 하지 않았다면 readiness까지만 했다고 명확히 적는다.

## 최종 보고 포함 항목

- ship 범위 요약
- 실행한 검증
- 문서 반영 여부
- 남은 리스크
- 실제 배포 여부

## 종료 기준

- 다른 사람이 그대로 운영 handoff를 받을 수 있다.
- 완료와 미완료가 구분돼 있다.
