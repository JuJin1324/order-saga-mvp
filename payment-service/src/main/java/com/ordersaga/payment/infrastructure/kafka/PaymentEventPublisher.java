package com.ordersaga.payment.infrastructure.kafka;

import com.ordersaga.saga.SagaTopics;
import com.ordersaga.saga.event.PaymentCompletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.saga.mode", havingValue = "kafka")
public class PaymentEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(PaymentEventPublisher.class);

    private final KafkaOperations<Object, Object> kafkaOperations;

    public PaymentEventPublisher(KafkaOperations<Object, Object> kafkaOperations) {
        this.kafkaOperations = kafkaOperations;
    }

    public void publishPaymentCompleted(PaymentCompletedEvent event) {
        log.info("Publishing payment-completed event for orderId={} paymentId={}", event.orderId(), event.paymentId());
        kafkaOperations.send(SagaTopics.PAYMENT_COMPLETED, event.orderId(), event);
    }
}
