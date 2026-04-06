package com.ordersaga.inventory.application;

import com.ordersaga.inventory.fixture.InventoryFixtureValues;
import com.ordersaga.inventory.infrastructure.kafka.InventoryEventPublisher;
import com.ordersaga.saga.event.InventoryDeductedEvent;
import com.ordersaga.saga.event.PaymentCompletedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.ordersaga.inventory.fixture.InventoryFixtureValues.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class InventoryEventProcessorTest {

    @Mock
    private InventoryApplicationService inventoryApplicationService;

    @Mock
    private InventoryEventPublisher inventoryEventPublisher;

    private InventoryEventProcessor sut;

    @BeforeEach
    void setUp() {
        sut = new InventoryEventProcessor(inventoryApplicationService, inventoryEventPublisher);
    }

    @Test
    @DisplayName("payment-completed 이벤트를 재고 차감으로 처리하고 inventory-deducted 이벤트를 발행한다")
    void handlePaymentCompleted_deductsInventoryAndPublishesNextEvent() {
        // Given
        PaymentCompletedEvent event = new PaymentCompletedEvent(
                ORDER_ID,
                PAYMENT_ID,
                AMOUNT,
                SKU,
                DEDUCT_QUANTITY
        );
        DeductInventoryResult deductInventoryResult = new DeductInventoryResult(SKU, DEDUCT_QUANTITY, REMAINING_QUANTITY);

        given(inventoryApplicationService.deductInventory(any(DeductInventoryCommand.class)))
                .willReturn(deductInventoryResult);

        // When
        DeductInventoryResult result = sut.handlePaymentCompleted(event);

        // Then
        assertThat(result).isEqualTo(deductInventoryResult);
        then(inventoryApplicationService).should().deductInventory(new DeductInventoryCommand(
                SKU,
                DEDUCT_QUANTITY,
                false
        ));
        then(inventoryEventPublisher).should().publishInventoryDeducted(new InventoryDeductedEvent(
                ORDER_ID,
                SKU,
                DEDUCT_QUANTITY,
                REMAINING_QUANTITY
        ));
    }
}
