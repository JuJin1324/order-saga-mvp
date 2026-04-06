package com.ordersaga.inventory.infrastructure.kafka;

import com.ordersaga.inventory.application.InventoryEventProcessor;
import com.ordersaga.saga.SagaTopics;
import com.ordersaga.saga.event.PaymentCompletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean(InventoryEventProcessor.class)
@ConditionalOnProperty(name = "app.kafka.listeners.enabled", havingValue = "true")
public class InventoryEventListener {
    private static final Logger log = LoggerFactory.getLogger(InventoryEventListener.class);

    private final InventoryEventProcessor inventoryEventProcessor;

    public InventoryEventListener(InventoryEventProcessor inventoryEventProcessor) {
        this.inventoryEventProcessor = inventoryEventProcessor;
    }

    @KafkaListener(topics = SagaTopics.PAYMENT_COMPLETED, groupId = "${spring.application.name}")
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        log.info("Received payment-completed event skeleton for orderId={} paymentId={}", event.orderId(), event.paymentId());
        inventoryEventProcessor.handlePaymentCompleted(event);
    }
}
