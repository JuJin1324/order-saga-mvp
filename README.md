# order-saga-mvp

MSA 환경에서 "결제는 됐는데 재고가 안 빠지는" 문제를 Saga 패턴으로 해결하는 MVP.

## 도메인 문제

주문·결제·재고가 각각 독립 DB를 가지면, 중간 단계 실패 시 서비스 간 데이터 불일치가 발생한다.
보상 트랜잭션으로 실패한 단계를 자동 되돌려 결과적 일관성을 확보한다.

## 기술 스택

- Spring Boot (주문·결제·재고 3개 서비스)
- Apache Kafka (이벤트 기반 통신)
- Docker Compose (로컬 환경)

## 관련 문서

- [실행 계획](../../plan-executions/order-payment-saga-mvp.md)
- [Saga 4-Level 조망](../architecture-leveling/backend/order-payment-saga.md)
- [MVP 스코프](../architecture-leveling/backend/order-payment-saga-mvp.md)
