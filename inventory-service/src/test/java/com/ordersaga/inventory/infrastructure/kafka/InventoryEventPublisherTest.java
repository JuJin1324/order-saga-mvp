package com.ordersaga.inventory.infrastructure.kafka;

import com.ordersaga.saga.SagaTopics;
import com.ordersaga.saga.event.InventoryDeductedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaOperations;

import static com.ordersaga.inventory.fixture.InventoryFixtureValues.DEDUCT_QUANTITY;
import static com.ordersaga.inventory.fixture.InventoryFixtureValues.ORDER_ID;
import static com.ordersaga.inventory.fixture.InventoryFixtureValues.REMAINING_QUANTITY;
import static com.ordersaga.inventory.fixture.InventoryFixtureValues.SKU;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class InventoryEventPublisherTest {

    @Mock
    private KafkaOperations<Object, Object> kafkaOperations;

    private InventoryEventPublisher sut;

    @BeforeEach
    void setUp() {
        sut = new InventoryEventPublisher(kafkaOperations);
    }

    @Test
    @DisplayName("inventory-deducted 이벤트를 지정한 토픽으로 발행한다")
    void publishInventoryDeducted_sendsEventToTopic() {
        // Given
        InventoryDeductedEvent event = new InventoryDeductedEvent(
                ORDER_ID,
                SKU,
                DEDUCT_QUANTITY,
                REMAINING_QUANTITY
        );

        // When
        sut.publishInventoryDeducted(event);

        // Then
        then(kafkaOperations).should().send(SagaTopics.INVENTORY_DEDUCTED, event.orderId(), event);
    }
}
