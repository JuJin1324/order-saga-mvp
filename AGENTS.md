# Repository Guidelines

## 프로젝트 구조와 모듈 구성
이 저장소는 주문, 결제, 재고 Saga 흐름을 다루는 Gradle 멀티 모듈 Spring Boot MVP다. 서비스 모듈은 `order-service/`, `payment-service/`, `inventory-service/`에 있고, 종단 간 검증은 `scenario-test/`에서 수행한다. 각 서비스는 `src/main/java/com/ordersaga/...` 아래에서 `presentation`, `application`, `domain`, `infrastructure` 계층을 공통으로 사용한다. 테스트는 `src/test/java`에 같은 구조로 배치되며, 공용 테스트 데이터 생성 코드는 `fixture/`에 둔다. 설계 문서와 단계별 계획은 `docs/architecture/`, `docs/plans/`를 참고한다.

## 빌드, 테스트, 개발 명령
모든 명령은 저장소 루트에서 Gradle Wrapper로 실행한다.

- `./gradlew build`: 전체 모듈을 컴파일하고 전체 테스트를 실행한다.
- `./gradlew test`: 모듈 전반의 단위 테스트와 통합 테스트를 실행한다.
- `./gradlew :scenario-test:test`: 정상 흐름과 불일치 재현 시나리오를 함께 검증한다.
- `./gradlew bootJar`: 세 서비스의 실행 가능한 JAR를 만든다.
- `./gradlew :order-service:bootRun`
- `./gradlew :payment-service:bootRun`
- `./gradlew :inventory-service:bootRun`
각 서비스의 개발 서버를 개별 실행할 때 사용한다.

기본 포트는 주문 `8081`, 결제 `8082`, 재고 `8083`이다.

## 코딩 스타일과 네이밍 규칙
기존 Java 21, Spring Boot 스타일을 유지한다. 들여쓰기는 공백 4칸, 파일당 public 클래스 1개, 의존성 주입은 생성자 기반을 사용하고 와일드카드 import는 피한다. 패키지 역할은 명확히 분리한다. 컨트롤러는 `presentation`, 흐름 조합 로직은 `application`, 엔티티와 리포지토리는 `domain`, 외부 HTTP 호출은 `infrastructure`에 둔다. DTO 이름은 `CreateOrderRequest`, `OrderResult`처럼 역할이 드러나게 작성하고, 테스트 픽스처도 `CreateOrderCommandFixture`처럼 의도를 드러내는 이름을 사용한다.

## 테스트 가이드
기본 테스트 스택은 JUnit 5, Spring Boot Test, AssertJ, Mockito다. 테스트 이름은 범위에 맞춰 맞춘다. 서비스 레이어 단위 테스트는 `*ApplicationTest`, HTTP 또는 영속성 검증은 `*IntegrationTest`, 서비스 간 흐름 검증은 `*ScenarioTest`를 사용한다. 상태 전이, 서비스 간 호출 계약, 실패 시나리오를 바꾸는 경우에는 반드시 테스트를 추가하거나 갱신한다.

## 커밋과 풀 리퀘스트 가이드
최근 이력은 `Add:`, `Fix:`, `Refactor:`, `Docs:`, `Update:` 같은 짧은 접두어를 사용한다. 커밋은 하나의 변경 목적에 집중하고, 사용자 영향이나 구조 변경이 드러나도록 한 줄로 요약한다. 풀 리퀘스트에는 해결하려는 문제, 영향받는 모듈, 실행한 검증 명령 예시(`./gradlew test`, `./gradlew :scenario-test:test`), API 계약이 바뀐 경우 요청과 응답 예시를 포함한다.

## 설정과 아키텍처 메모
로컬 개발 기본 DB는 H2이며, 서비스 간 호출 주소는 각 모듈의 `application.yml`에서 관리한다. 포트, 서비스 URL, 영속성 설정을 변경하면 해당 서비스 설정만 바꾸지 말고 이를 참조하는 시나리오 테스트와 문서도 함께 수정한다.
