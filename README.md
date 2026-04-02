# order-saga-mvp

MSA 환경에서 "결제는 됐는데 재고가 안 빠지는" 문제를 Saga 패턴으로 해결하는 MVP.

## 도메인 문제

주문·결제·재고가 각각 독립 DB를 가지면, 중간 단계 실패 시 서비스 간 데이터 불일치가 발생한다.
보상 트랜잭션으로 실패한 단계를 자동 되돌려 결과적 일관성을 확보한다.

## 기술 스택

- Spring Boot 4.0.1 / Java 21
- H2 기반 로컬 실행 및 시나리오 테스트 검증
- REST 체이닝 기반 현재 구현
- `scenario-test` 모듈을 통한 end-to-end 상태 검증
- Kafka 기반 Choreography Saga는 다음 단계에서 추가 예정

## 현재 범위

- 현재 구현: REST 호출 체이닝으로 주문 → 결제 → 재고 차감
- 현재 목표: 재고 차감이 실패해도 결제는 이미 완료된 상태로 남는 불일치 재현
- 다음 단계: Kafka 기반 Choreography Saga + 보상 트랜잭션
- 현재 단계에서는 Docker Compose를 채택하지 않음

## 프로젝트 구조

- `order-service/`: 주문 생성, 결제 서비스 호출, 최종 주문 상태 저장
- `payment-service/`: 결제 레코드 저장 후 재고 서비스 호출
- `inventory-service/`: 재고 차감과 실패 시뮬레이션
- `scenario-test/`: 3개 서비스를 함께 띄워 정상 흐름과 불일치 재현을 검증하는 시나리오 테스트
- `docs/architecture/problem-solving-structure.md`: 문제 발생/해결 흐름 구조
- `docs/architecture/c4-container-structure.md`: 서비스와 DB 배치 구조

## 실행 방법

### 1. 로컬 빠른 검증(H2 기본값)

Docker 없이도 현재 흐름을 바로 확인할 수 있도록, 각 서비스는 기본적으로 H2를 사용한다.

```bash
./gradlew bootJar
```

JDK 21 이상이 기본 Java로 잡혀 있으면 위 명령만으로 충분하다.

각 서비스 실행:

```bash
java -jar inventory-service/build/libs/inventory-service.jar
java -jar payment-service/build/libs/payment-service.jar
java -jar order-service/build/libs/order-service.jar
```

### 2. 시나리오 테스트로 흐름 검증

세 서비스를 함께 띄워 정상 흐름과 불일치 재현을 검증하려면 아래 테스트를 실행한다.

```bash
./gradlew :scenario-test:test
```

### 3. 정상 시나리오

```bash
curl -s http://localhost:8081/api/orders \
  -H 'Content-Type: application/json' \
  -d '{
    "sku": "sku-001",
    "quantity": 2,
    "amount": 15000,
    "forceInventoryFailure": false
  }'
```

정상 시나리오에서는:
- Order 상태가 `CONFIRMED`
- Payment 상태가 `COMPLETED`
- Inventory 수량이 10 → 8

### 4. 불일치 재현 시나리오

```bash
curl -s http://localhost:8081/api/orders \
  -H 'Content-Type: application/json' \
  -d '{
    "sku": "sku-001",
    "quantity": 2,
    "amount": 15000,
    "forceInventoryFailure": true
  }'
```

실패 시나리오에서는:
- Order 상태가 `FAILED`
- Payment 상태가 `COMPLETED`
- Inventory 수량은 그대로 유지

즉, "주문은 실패로 처리됐지만 결제는 이미 끝난 상태"라는 분산 환경의 불일치가 그대로 남는다.

## 상태 확인

현재 단계에서는 DB를 직접 조회하는 방식보다 시나리오 테스트로 상태를 검증한다.

- 정상 흐름 검증: `Order=CONFIRMED`, `Payment=COMPLETED`, `Inventory 감소`
- 불일치 재현 검증: `Order=FAILED`, `Payment=COMPLETED`, `Inventory unchanged`
- 검증 위치: `scenario-test/src/test/java/com/ordersaga/scenario/OrderProcessingScenarioTest.java`

## 관련 문서

- [실행 계획](./docs/order-payment-saga-mvp.md)
- [문제 해결 흐름 다이어그램](./docs/architecture/problem-solving-structure.md)
- [서비스/DB 배치 구조](./docs/architecture/c4-container-structure.md)
