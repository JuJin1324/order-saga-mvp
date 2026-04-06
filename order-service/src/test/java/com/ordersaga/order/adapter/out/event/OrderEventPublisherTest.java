package com.ordersaga.order.adapter.out.event;

import com.ordersaga.saga.SagaTopics;
import com.ordersaga.saga.event.OrderCreatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaOperations;

import static com.ordersaga.order.fixture.OrderFixtureValues.AMOUNT;
import static com.ordersaga.order.fixture.OrderFixtureValues.ORDER_ID;
import static com.ordersaga.order.fixture.OrderFixtureValues.QUANTITY;
import static com.ordersaga.order.fixture.OrderFixtureValues.SKU;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class OrderEventPublisherTest {

    @Mock
    private KafkaOperations<Object, Object> kafkaOperations;

    private OrderEventPublisher sut;

    @BeforeEach
    void setUp() {
        sut = new OrderEventPublisher(kafkaOperations);
    }

    @Test
    @DisplayName("order-created 이벤트를 지정한 토픽으로 발행한다")
    void publishOrderCreated_sendsEventToTopic() {
        // Given
        OrderCreatedEvent event = new OrderCreatedEvent(
                ORDER_ID,
                SKU,
                QUANTITY,
                AMOUNT
        );

        // When
        sut.publishOrderCreated(event);

        // Then
        then(kafkaOperations).should().send(SagaTopics.ORDER_CREATED, event.orderId(), event);
    }
}
