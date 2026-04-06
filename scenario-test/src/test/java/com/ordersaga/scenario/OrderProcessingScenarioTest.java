package com.ordersaga.scenario;

import com.ordersaga.inventory.InventoryServiceApplication;
import com.ordersaga.inventory.domain.Inventory;
import com.ordersaga.inventory.domain.InventoryRepository;
import com.ordersaga.order.OrderServiceApplication;
import com.ordersaga.order.adapter.in.web.dto.CreateOrderRequest;
import com.ordersaga.order.application.OrderResult;
import com.ordersaga.order.domain.Order;
import com.ordersaga.order.domain.OrderRepository;
import com.ordersaga.order.domain.OrderStatus;
import com.ordersaga.payment.PaymentServiceApplication;
import com.ordersaga.payment.domain.Payment;
import com.ordersaga.payment.domain.PaymentRepository;
import com.ordersaga.payment.domain.PaymentStatus;
import com.ordersaga.scenario.fixture.CreateOrderRequestFixture;
import com.ordersaga.scenario.fixture.ScenarioFixtureValues;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OrderProcessingScenarioTest {

    private ConfigurableApplicationContext inventoryContext;
    private ConfigurableApplicationContext paymentContext;
    private ConfigurableApplicationContext orderContext;

    private InventoryRepository inventoryRepository;
    private PaymentRepository paymentRepository;
    private OrderRepository orderRepository;
    private RestClient orderClient;

    @BeforeAll
    void startServices() {
        try {
            inventoryContext = new SpringApplicationBuilder(InventoryServiceApplication.class)
                    .run(commonArgs("inventory-scenario-db"));
            int inventoryPort = localPort(inventoryContext);

            paymentContext = new SpringApplicationBuilder(PaymentServiceApplication.class)
                    .run(serviceArgs("payment-scenario-db", "--inventory-service.url=http://localhost:" + inventoryPort));
            int paymentPort = localPort(paymentContext);

            orderContext = new SpringApplicationBuilder(OrderServiceApplication.class)
                    .run(serviceArgs("order-scenario-db", "--payment-service.url=http://localhost:" + paymentPort));
            int orderPort = localPort(orderContext);

            inventoryRepository = inventoryContext.getBean(InventoryRepository.class);
            paymentRepository = paymentContext.getBean(PaymentRepository.class);
            orderRepository = orderContext.getBean(OrderRepository.class);
            orderClient = RestClient.builder()
                    .baseUrl("http://localhost:" + orderPort)
                    .build();
        } catch (RuntimeException | Error e) {
            stopServices();
            throw e;
        }
    }

    @AfterAll
    void stopServices() {
        closeContext(orderContext);
        orderContext = null;

        closeContext(paymentContext);
        paymentContext = null;

        closeContext(inventoryContext);
        inventoryContext = null;
    }

    @BeforeEach
    void resetState() {
        orderRepository.deleteAll();
        paymentRepository.deleteAll();
        inventoryRepository.deleteAll();
        inventoryRepository.save(new Inventory(
                ScenarioFixtureValues.SKU,
                ScenarioFixtureValues.INITIAL_INVENTORY_QUANTITY
        ));
    }

    @Test
    @DisplayName("정상 주문은 주문 결제 재고 상태를 같은 방향으로 맞춘다")
    void successfulOrder_alignsOrderPaymentInventoryStates() {
        // Given
        CreateOrderRequest request = CreateOrderRequestFixture.normal();

        // When
        OrderResult response = createOrder(request);

        // Then
        assertSuccessfulState(response);
    }

    @Test
    @DisplayName("재고 차감 실패는 상태 불일치를 드러낸다")
    void inventoryFailure_leavesPaymentCompletedAndOrderFailed() {
        // Given
        CreateOrderRequest request = CreateOrderRequestFixture.withInventoryFailure();

        // When
        OrderResult response = createOrder(request);

        // Then
        assertExposedInconsistency(response);
    }

    private OrderResult createOrder(CreateOrderRequest request) {
        return orderClient.post()
                .uri("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(OrderResult.class);
    }

    private void assertSuccessfulState(OrderResult response) {
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(OrderStatus.CONFIRMED);

        Order storedOrder = orderRepository.findByOrderId(response.orderId()).orElseThrow();
        Payment storedPayment = paymentRepository.findByOrderId(response.orderId()).orElseThrow();
        Inventory storedInventory = inventoryRepository.findBySku(ScenarioFixtureValues.SKU).orElseThrow();

        assertThat(storedOrder.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(storedPayment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(storedInventory.getAvailableQuantity()).isEqualTo(ScenarioFixtureValues.REMAINING_INVENTORY_QUANTITY);
    }

    private void assertExposedInconsistency(OrderResult response) {
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(OrderStatus.FAILED);

        Order storedOrder = orderRepository.findByOrderId(response.orderId()).orElseThrow();
        Payment storedPayment = paymentRepository.findByOrderId(response.orderId()).orElseThrow();
        Inventory storedInventory = inventoryRepository.findBySku(ScenarioFixtureValues.SKU).orElseThrow();

        assertThat(storedOrder.getStatus()).isEqualTo(OrderStatus.FAILED);
        assertThat(storedPayment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(storedInventory.getAvailableQuantity()).isEqualTo(ScenarioFixtureValues.INITIAL_INVENTORY_QUANTITY);
    }

    private String[] commonArgs(String databaseName) {
        return new String[]{
                "--server.port=0",
                "--spring.datasource.url=jdbc:h2:mem:" + databaseName + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                "--spring.datasource.driver-class-name=org.h2.Driver",
                "--spring.datasource.username=sa",
                "--spring.datasource.password=",
                "--spring.jpa.hibernate.ddl-auto=create-drop",
                "--spring.sql.init.mode=never",
                "--logging.level.root=WARN",
                "--logging.level.org.springframework=WARN",
                "--logging.level.org.hibernate.SQL=OFF",
                "--logging.level.org.hibernate.orm.jdbc.bind=OFF"
        };
    }

    private String[] serviceArgs(String databaseName, String... extraArgs) {
        String[] commonArgs = commonArgs(databaseName);
        String[] mergedArgs = new String[commonArgs.length + extraArgs.length];
        System.arraycopy(commonArgs, 0, mergedArgs, 0, commonArgs.length);
        System.arraycopy(extraArgs, 0, mergedArgs, commonArgs.length, extraArgs.length);
        return mergedArgs;
    }

    private int localPort(ConfigurableApplicationContext context) {
        return context.getEnvironment().getProperty("local.server.port", Integer.class, 0);
    }

    private void closeContext(ConfigurableApplicationContext context) {
        if (context != null) {
            context.close();
        }
    }
}
