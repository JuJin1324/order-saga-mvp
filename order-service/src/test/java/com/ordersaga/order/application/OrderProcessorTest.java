package com.ordersaga.order.application;

import com.ordersaga.order.application.port.out.ChargePaymentPort;
import com.ordersaga.order.fixture.CreateOrderCommandFixture;
import com.ordersaga.order.domain.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class OrderProcessorTest {

    @Mock
    private OrderApplicationService orderApplicationService;

    @Mock
    private ChargePaymentPort chargePaymentPort;

    private OrderProcessor sut;

    private static final String ORDER_ID = "test-order-id";

    @BeforeEach
    void setUp() {
        sut = new OrderProcessor(orderApplicationService, chargePaymentPort);
    }

    @Test
    @DisplayName("결제 성공 시 주문 상태는 CONFIRMED")
    void paymentSuccess_orderStatusConfirmed() {
        // Given
        CreateOrderCommand command = CreateOrderCommandFixture.normal();
        OrderResult created = new OrderResult(ORDER_ID, OrderStatus.CREATED, command.sku(), command.quantity(), command.amount());
        OrderResult confirmed = new OrderResult(ORDER_ID, OrderStatus.CONFIRMED, command.sku(), command.quantity(), command.amount());

        given(orderApplicationService.createOrder(command)).willReturn(created);
        given(chargePaymentPort.chargePayment(eq(ORDER_ID), eq(command.amount()), eq(command.sku()), eq(command.quantity()), eq(false)))
                .willReturn(true);
        given(orderApplicationService.confirmOrder(ORDER_ID)).willReturn(confirmed);

        // When
        OrderResult result = sut.processOrder(command);

        // Then
        assertThat(result.status()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    @DisplayName("결제 실패 시 주문 상태는 FAILED")
    void paymentFailure_orderStatusFailed() {
        // Given
        CreateOrderCommand command = CreateOrderCommandFixture.withForceInventoryFailure();
        OrderResult created = new OrderResult(ORDER_ID, OrderStatus.CREATED, command.sku(), command.quantity(), command.amount());
        OrderResult failed = new OrderResult(ORDER_ID, OrderStatus.FAILED, command.sku(), command.quantity(), command.amount());

        given(orderApplicationService.createOrder(command)).willReturn(created);
        given(chargePaymentPort.chargePayment(eq(ORDER_ID), eq(command.amount()), eq(command.sku()), eq(command.quantity()), eq(true)))
                .willReturn(false);
        given(orderApplicationService.failOrder(ORDER_ID)).willReturn(failed);

        // When
        OrderResult result = sut.processOrder(command);

        // Then
        assertThat(result.status()).isEqualTo(OrderStatus.FAILED);
    }
}
