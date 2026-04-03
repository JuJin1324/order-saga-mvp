# inventory-service 기능 구조

이 문서는 Step 2a 기준의 `inventory-service`가 무엇을 하는 서비스인지 정리한다.  
근거는 [c4-container-structure.md](../c4-container-structure.md)와 [problem-solving-structure.md](../problem-solving-structure.md)다.

---

## 1. 한 줄 정의

`inventory-service`는 결제 완료 이벤트를 받아 재고를 실제로 차감하고 최종 성공 이벤트를 발행하는 마지막 단계다.

- `payment-completed` 이벤트를 받으면 재고를 차감한다.
- 차감이 성공하면 `inventory-deducted` 이벤트를 발행한다.
- 다른 서비스를 직접 호출하지 않는다.

---

## 2. 왜 이렇게 설계했는가

`inventory-service`는 Step 2a에서 순방향 플로우의 마지막 판정 지점이다.

- 결제가 끝났다는 사실만으로 주문을 확정할 수는 없고, 실제 재고 차감까지 성공해야 비로소 주문을 완료할 수 있다.
- 그래서 `inventory-service`는 단순 조회 서비스가 아니라, 순방향 플로우의 마지막 성공 조건을 결정하는 서비스가 된다.
- Step 1에서도 재고 차감은 마지막 단계였고, Step 2a도 그 비즈니스 순서를 유지한다.

즉, Step 2a는 "재고를 언제 차감할 것인가"를 다시 설계하는 단계가 아니다.  
기존 순서를 유지한 채, 마지막 성공 사실을 HTTP 응답 대신 이벤트로 넘기도록 바꾸는 단계다.

이 설계 때문에 `inventory-service`는 성공 시에는 가장 명확한 이벤트를 발행하지만,  
실패 시에는 시스템의 한계도 가장 선명하게 드러낸다.

---

## 3. 인터페이스

### 받는 요청

| 종류 | 경로/토픽 | 호출자 | 목적 |
|---|---|---|---|
| Kafka Consume | `payment-completed` | payment-service | 재고 차감 시작 |
| HTTP GET | `/api/inventory/{sku}` | Client | 재고 조회 |

### 보내는 요청

| 종류 | 경로/토픽 | 대상 | 목적 |
|---|---|---|---|
| Kafka Publish | `inventory-deducted` | order-service | 주문 확정 알림 |

Step 2a 기준으로 `inventory-service`는 순방향 플로우의 마지막 처리 서비스다.

---

## 4. 재고 상태

재고는 별도 상태 머신이 없다. 수량(`availableQuantity`)이 직접 변한다.

- 차감 성공 → 수량 감소 + `inventory-deducted` 발행
- 차감 실패 → 수량 변화 없음 + 이벤트 발행 없음

Step 2a에는 실패 이벤트가 없기 때문에, 여기서 실패가 나면 상위 서비스는 자동으로 되돌아가지 않는다.

---

## 5. API / 이벤트 스펙

### 5.1 수신 이벤트

#### `payment-completed`

| 필드 | 타입 | 설명 |
|---|---|---|
| orderId | String | 재고 차감 대상 주문 ID |
| paymentId | String | 이미 완료된 결제 ID |
| amount | BigDecimal | 결제 금액 |
| sku | String | 차감할 상품 식별자 |
| quantity | Integer | 차감 수량 |

### 5.2 발행 이벤트

#### `inventory-deducted`

| 필드 | 타입 | 설명 |
|---|---|---|
| orderId | String | 확정 가능한 주문 ID |
| sku | String | 차감된 상품 식별자 |
| deductedQuantity | Integer | 실제 차감 수량 |
| remainingQuantity | Integer | 차감 후 남은 수량 |

### 5.3 재고 조회

```
GET /api/inventory/{sku}
```

**Path Parameter**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| sku | String | 조회할 상품 식별자 |

**Response Body**

| 필드 | 타입 | 설명 |
|---|---|---|
| sku | String | 상품 식별자 |
| availableQuantity | Integer | 현재 가용 수량 |

### 5.4 헬스 체크

```
GET /api/inventory/health
```

서비스 생존 확인용. 별도 파라미터 없음.

---

## 6. Step 2a에서 남는 문제

`inventory-service`는 성공 시에는 최종 이벤트를 발행하지만, 실패 시에는 거기서 흐름이 멈춘다.

- 주문은 `CREATED`에 머무를 수 있다
- 결제는 `COMPLETED`로 남을 수 있다
- 재고는 차감되지 않는다

즉, 이 서비스는 순방향 플로우의 마지막 단계이면서 동시에 Step 2b 보상 흐름이 왜 필요한지를 가장 분명하게 보여주는 지점이다.
