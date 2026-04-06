# Step 2a Kafka 마이그레이션 PR 계획

이 문서는 "현재 REST 체이닝 기반 구현을 Kafka 기반 Step 2a로 어떻게 단계적으로 옮길 것인가"를 정리한다.

---

## 1. 왜 단계적 마이그레이션이 필요한가

현재 구현은 세 가지가 강하게 묶여 있다.

1. 서비스 간 동기 호출
2. 주문 API 계약
3. 시나리오 테스트 계약

구체적으로는 아래가 함께 바뀐다.

- `order-service`는 `PaymentClient`를 직접 호출한다.
- `payment-service`는 `InventoryClient`를 직접 호출한다.
- `scenario-test`는 `POST /api/orders`가 최종 상태를 즉시 반환하는 현재 계약을 검증한다.

이 상태에서 한 번에 Kafka로 바꾸면:

- 서비스 코드만 깨지는 것이 아니라
- API 응답 계약과
- 테스트 계약과
- 내부 REST endpoint 의존까지 동시에 깨진다.

그래서 Step 2a는 **"새 Kafka 경로를 먼저 세우고, 마지막에 기존 REST 경로를 걷어내는 방식"**으로 가는 것이 안전하다.

---

## 2. 마이그레이션 원칙

- 먼저 `transport`와 `business logic`를 분리한다.
- 먼저 downstream 서비스를 Kafka 대응시킨다.
- `order-service` 컷오버는 마지막에 한다.
- 기존 테스트를 바로 덮어쓰지 않고, Kafka 전용 검증을 추가한 뒤 나중에 제거한다.
- Step 2a에서는 happy path만 메인 계약으로 삼고, 실패 보상은 Step 2b로 넘긴다.

---

## 3. 고정 결정

- 전환 중에는 `app.saga.mode=rest|kafka` 플래그를 두고, `PR 6`에서 제거한다.
- `/internal/payments`, `/internal/inventory/deduct` 는 `PR 5`까지 compatibility endpoint로 유지하고 `PR 6`에서 제거한다.
- `forceInventoryFailure`, `forceFailure` 는 legacy REST 경로에서만 유지하고 `PR 6`에서 제거한다.
- `POST /api/orders` 기본 계약은 `PR 6`에서 `CREATED 즉시 반환`으로 전환한다.
- Kafka 시나리오 테스트는 `PR 5`에서 별도 추가하고, 기존 REST 시나리오는 `PR 6`에서 제거한다.

---

## 4. 권장 PR 순서

### PR 1. 서비스 로직과 전송 방식을 분리

### 목표

REST 호출과 Kafka listener가 나중에 같은 유스케이스를 재사용할 수 있도록, application service를 transport-agnostic 하게 정리한다.

여기서 `transport-agnostic`는 애플리케이션 로직이 HTTP 호출, Kafka listener, 내부 이벤트 발행 같은 전송 방식에 직접 묶이지 않는다는 뜻이다.
즉, application service는 "주문 생성", "결제 저장", "재고 차감" 같은 유스케이스만 책임지고, REST controller나 Kafka listener는 그 유스케이스를 호출하는 바깥 어댑터가 된다.

### 핵심 작업

- `order-service`
  - 주문 생성 로직과 주문 확정 로직을 분리한다.
  - `createOrder` 안에 묶여 있는 "저장 + 하위 호출 + 상태 결정" 구조를 분해할 준비를 한다.
- `payment-service`
  - 결제 저장 로직을 독립 유스케이스로 정리한다.
- `inventory-service`
  - 재고 차감 로직을 독립 유스케이스로 정리한다.

### 이 PR에서 하지 않을 것

- Kafka 의존성 추가
- 이벤트 발행/수신
- API 계약 변경
- 시나리오 테스트 계약 변경

### 테스트 게이트

- 기존 단위 테스트 전부 통과
- 기존 integration 테스트 전부 통과
- 기존 `scenario-test` 전부 통과

### 머지 조건

동작은 완전히 같아야 한다.  
이 PR은 구조 정리만 하고 외부 계약은 바꾸지 않는다.

---

### PR 2. Kafka 인프라 추가, 기본 실행 경로는 그대로 유지

### 목표

Kafka 전환에 필요한 공통 인프라를 먼저 넣되, 기본 동작은 여전히 REST 체이닝으로 유지한다.

### 핵심 작업

- `saga-events` 모듈 추가
- `spring-kafka` 의존성 추가
- 각 서비스 `application.yml`에 Kafka 설정 추가
- `docker-compose.yml` 추가
- 이벤트 record 추가
- `OrderEventPublisher`, `PaymentEventPublisher`, `InventoryEventPublisher` 추가
- `PaymentEventListener`, `InventoryEventListener`, `OrderEventListener` 골격 추가
- 필요하면 `app.saga.mode=rest|kafka` 같은 전환 플래그 추가
- Kafka infra 테스트는 publisher 중심의 얇은 단위 테스트 또는 context load 수준으로 먼저 추가

### 이 PR에서 하지 않을 것

- `POST /api/orders` 응답 계약 변경
- 기존 REST client 삭제
- 시나리오 테스트를 Kafka 계약으로 교체
- `saga-events`용 shared test support 또는 `java-test-fixtures` 도입

### 테스트 게이트

- 기존 테스트 전부 통과
- Kafka 관련 신규 단위 테스트 또는 context load 확인

### 머지 조건

기본값이 `rest`일 때 아무 것도 깨지지 않아야 한다.  
즉, Kafka 코드는 들어오지만 아직 시스템의 기본 경로가 바뀌면 안 된다.

추가로 이 단계에서는 이벤트 fixture 공유 구조를 미리 일반화하지 않는다.  
현재는 각 모듈 테스트가 자기 fixture 값을 재사용하는 선에서 멈추고, 공용 test support 필요성은 다음 PR에서 실제 중복이 생기는지 보고 판단한다.

---

### PR 3. `payment-service`, `inventory-service`를 먼저 Kafka 대응

### 목표

downstream 두 서비스를 먼저 Kafka 입력/출력에 대응시켜, 나중에 `order-service`가 Kafka로 전환돼도 받을 준비가 된 상태를 만든다.

### 핵심 작업

- `payment-service`
  - `order-created` listener 추가
  - 결제 저장 후 `payment-completed` 발행
  - 기존 `/internal/payments` endpoint는 잠시 유지
- `inventory-service`
  - `payment-completed` listener 추가
  - 재고 차감 후 `inventory-deducted` 발행
  - 기존 `/internal/inventory/deduct` endpoint는 잠시 유지

### 설계 포인트

- 이 단계에서는 **REST path와 Kafka path가 같은 유스케이스를 호출해야 한다**
- 즉, controller와 listener가 각자 다른 로직을 가지면 안 된다
- 내부 REST endpoint는 runtime main path가 아니라 compatibility path로 남긴다
- 이 단계부터 producer 테스트와 consumer 테스트가 함께 생기므로, 이벤트 샘플 생성 코드 중복이 실제로 늘어나는지 확인한다
- `OrderCreatedEvent`, `PaymentCompletedEvent` 같은 이벤트 fixture가 여러 모듈 테스트에 반복되면 그때 `saga-events` shared test support 또는 별도 `test-support` 모듈 도입을 검토한다

### 테스트 게이트

- `:payment-service:test` 통과
- `:inventory-service:test` 통과
- 기존 `scenario-test`는 여전히 통과
- 공용 test support 필요 여부를 코드 중복 기준으로 판단할 수 있어야 한다

### 머지 조건

Kafka 수신 코드가 들어와도, 아직 `order-service`는 REST 체인을 쓰므로 전체 시스템 기본 계약은 유지되어야 한다.

---

### PR 3.5. `order-service` 컷오버 전 구조 정리 PR

### 목표

`order-service` Kafka 컷오버 전에, 현재 layered package 이름과 실제 책임 사이의 어긋남을 최소 범위로 정리한다.  
핵심은 PR 순서를 뒤엎는 것이 아니라, PR 4의 위험도를 낮추는 작은 prep PR을 하나 끼우는 것이다.

### 핵심 작업

- `order-service`의 HTTP 진입점, Kafka listener, publisher, usecase 경계를 더 읽기 쉽게 정리
- 필요하면 naming 정리
  - 예: `Processor`, `ApplicationService` 중 무엇이 유스케이스인지 드러나게 조정
- transport-specific 매핑과 비즈니스 흐름 조합 책임을 분리
- 가능하면 패키지 구조도 인바운드 / 아웃바운드 어댑터 관점으로 읽히게 보강

### 설계 포인트

- 이 PR은 구조 정리 PR이지 기능 PR이 아니다
- `POST /api/orders` 계약, Kafka 기본 경로, 시나리오 테스트 계약은 바꾸지 않는다
- 범위를 크게 잡아 전 서비스 패키지 재배치를 하는 것은 피한다
- 기존 PR 4, PR 5, PR 6 번호는 유지하고, 이 PR은 사실상 `PR 3.5` 또는 prep PR 성격으로 다룬다

### 테스트 게이트

- `order-service` 기존 단위/integration 테스트 통과
- 기존 `scenario-test` 통과
- 동작 변경 없이 구조만 정리됐다는 것을 설명할 수 있어야 한다

### 머지 조건

이 PR이 끝나면 PR 4에서 `order-service`의 Kafka 경로를 붙일 때,  
"controller도 입력 어댑터, listener도 입력 어댑터"라는 관점으로 읽을 수 있어야 한다.

---

### PR 4. `order-service` Kafka 컷오버 준비

### 목표

`order-service`가 REST 체이닝 대신 이벤트로 흐름을 시작하고 마무리할 수 있도록 만든다.  
다만 이 단계에서는 전환 플래그 또는 별도 경로를 통해 점진적으로 켠다.

### 핵심 작업

- 주문 생성 시 `order-created` 발행 경로 추가
- `inventory-deducted` listener로 `confirmOrder` 연결
- 필요하면 `rest` / `kafka` 모드 분기 추가
- 주문 조회는 그대로 유지

### 설계 포인트

- 가장 위험한 변경은 `POST /api/orders` 계약 변경이다
- 따라서 이 PR에서 중요한 것은 **Kafka 경로를 추가하는 것**이지, 곧바로 기본 계약을 뒤집는 것이 아니다
- 가능하면 PR 3.5에서 정리한 구조 위에 얹는다
- 만약 PR 3 시점에 이벤트 fixture 중복이 명확해졌다면, 이 PR 초반에 `saga-events` shared test support 정리를 할 수 있다
- 단, shared fixture 정리는 `order-service` 컷오버보다 우선순위가 높지 않다. 중복이 작으면 계속 미룬다

### 테스트 게이트

- `order-service` 단위/integration 테스트 통과
- `rest` 모드에서는 기존 테스트 유지 가능
- `kafka` 모드에서는 신규 단위 테스트 또는 slice 테스트 추가

### 머지 조건

REST 기본 계약을 아직 유지한 채, Kafka 경로가 독립적으로 동작 가능한 상태여야 한다.

---

### PR 5. Kafka 시나리오 테스트 추가

### 목표

기존 시나리오 테스트를 덮어쓰지 않고, Kafka용 end-to-end 검증을 별도로 추가한다.

### 핵심 작업

- `scenario-test`에 Testcontainers Kafka 추가
- Awaitility 기반 비동기 검증 추가
- 새 Kafka 시나리오 테스트 추가
  - 주문 생성 즉시 `CREATED`
  - 이후 비동기적으로 `CONFIRMED`
  - `payment_db = COMPLETED`
  - `inventory_db = quantity deducted`

### 왜 별도 테스트로 가는가

- 기존 테스트는 "REST 체인 기반 현재 계약"을 지키는 안전망이다
- 새 Kafka 테스트는 "앞으로 메인으로 삼을 계약"을 검증한다
- 둘을 같은 테스트 파일에서 동시에 다루면 컷오버 시점이 모호해진다

### 테스트 게이트

- 기존 REST 시나리오 테스트 통과
- 신규 Kafka 시나리오 테스트 통과

### 머지 조건

이 PR이 머지되면, 팀은 처음으로 "Kafka 경로가 실제로 끝까지 돈다"는 증거를 가진다.  
그 전까지는 Kafka 코드는 있어도 메인 계약으로 간주하지 않는다.

---

### PR 6. 기본 경로 전환과 정리

### 목표

Kafka 경로를 메인 계약으로 승격하고, 이제 불필요해진 REST 체이닝 코드와 테스트를 제거한다.

### 핵심 작업

- `POST /api/orders` 기본 계약을 `CREATED 즉시 반환`으로 전환
- 기존 REST client 제거
  - `PaymentClient`
  - `InventoryClient`
- Step 1 전용 테스트/fixture 정리
  - 동기 `CONFIRMED`/`FAILED` 응답 기대 테스트 제거
  - `forceInventoryFailure`, `forceFailure` 같은 Step 1 장치 제거
- Kafka 시나리오를 메인 시나리오로 승격

### 이 PR이 의미하는 것

이 PR이 끝나면 Step 1 계약은 더 이상 시스템의 기본 동작이 아니다.  
즉, 이 시점부터는 "호환 유지"가 아니라 "컷오버 완료" 상태로 본다.

### 테스트 게이트

- Kafka 기준 단위 테스트 통과
- Kafka 기준 integration 테스트 통과
- Kafka 기준 `scenario-test` 통과

### 머지 조건

이 PR 이후 README, architecture docs, scenario-test 계약이 모두 Step 2a 기준으로 정렬되어야 한다.

---

## 5. 컷오버 판단 기준

아래 조건이 만족되기 전에는 기본 계약을 Kafka로 바꾸지 않는다.

- downstream 서비스가 Kafka 입력을 이미 처리할 수 있어야 한다
- Kafka end-to-end 시나리오 테스트가 안정적으로 통과해야 한다
- `POST /api/orders`의 새 계약(`CREATED` 즉시 반환)을 검증하는 테스트가 있어야 한다
- 기존 REST 체이닝 제거 전, Kafka 경로가 실제로 주문 확정까지 도는 것이 확인되어야 한다

---

## 6. 이 계획에서 의도적으로 뒤로 미룬 것

이 문서는 Step 2a 마이그레이션 계획이다. 아래는 이번 단계의 기본 머지 조건에 넣지 않는다.

- 재고 실패 시 주문/결제를 어떻게 되돌릴 것인가
- 실패 이벤트를 어떤 이름으로 둘 것인가
- retry, DLT, idempotency를 어떤 정책으로 넣을 것인가
- 운영 환경에서 토픽을 코드로 생성할 것인가, 수동으로 만들 것인가

이 항목들은 Step 2b 또는 운영 하드닝 문서에서 다룬다.

---

## 7. 추천 순서

1. 구조 분리
2. Kafka 인프라 추가
3. downstream Kafka 대응
4. `order-service` 전환 준비
5. Kafka 시나리오 추가
6. 마지막에 컷오버 + 정리
