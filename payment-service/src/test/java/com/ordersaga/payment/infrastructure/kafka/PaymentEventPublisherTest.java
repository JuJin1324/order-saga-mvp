package com.ordersaga.payment.infrastructure.kafka;

import com.ordersaga.saga.SagaTopics;
import com.ordersaga.saga.event.PaymentCompletedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaOperations;

import static com.ordersaga.payment.fixture.PaymentFixtureValues.AMOUNT;
import static com.ordersaga.payment.fixture.PaymentFixtureValues.ORDER_ID;
import static com.ordersaga.payment.fixture.PaymentFixtureValues.PAYMENT_ID;
import static com.ordersaga.payment.fixture.PaymentFixtureValues.QUANTITY;
import static com.ordersaga.payment.fixture.PaymentFixtureValues.SKU;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class PaymentEventPublisherTest {

    @Mock
    private KafkaOperations<Object, Object> kafkaOperations;

    private PaymentEventPublisher sut;

    @BeforeEach
    void setUp() {
        sut = new PaymentEventPublisher(kafkaOperations);
    }

    @Test
    @DisplayName("payment-completed 이벤트를 지정한 토픽으로 발행한다")
    void publishPaymentCompleted_sendsEventToTopic() {
        // Given
        PaymentCompletedEvent event = new PaymentCompletedEvent(
                ORDER_ID,
                PAYMENT_ID,
                AMOUNT,
                SKU,
                QUANTITY
        );

        // When
        sut.publishPaymentCompleted(event);

        // Then
        then(kafkaOperations).should().send(SagaTopics.PAYMENT_COMPLETED, event.orderId(), event);
    }
}
