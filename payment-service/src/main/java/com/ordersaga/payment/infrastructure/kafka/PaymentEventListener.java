package com.ordersaga.payment.infrastructure.kafka;

import com.ordersaga.saga.SagaTopics;
import com.ordersaga.saga.event.OrderCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.kafka.listeners.enabled", havingValue = "true")
public class PaymentEventListener {
    private static final Logger log = LoggerFactory.getLogger(PaymentEventListener.class);

    @KafkaListener(topics = SagaTopics.ORDER_CREATED, groupId = "${spring.application.name}")
    public void onOrderCreated(OrderCreatedEvent event) {
        log.info("Received order-created event skeleton for orderId={} sku={}", event.orderId(), event.sku());
    }
}
