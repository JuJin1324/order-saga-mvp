package com.ordersaga.inventory.infrastructure.kafka;

import com.ordersaga.saga.SagaTopics;
import com.ordersaga.saga.event.InventoryDeductedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.saga.mode", havingValue = "kafka")
public class InventoryEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(InventoryEventPublisher.class);

    private final KafkaOperations<Object, Object> kafkaOperations;

    public InventoryEventPublisher(KafkaOperations<Object, Object> kafkaOperations) {
        this.kafkaOperations = kafkaOperations;
    }

    public void publishInventoryDeducted(InventoryDeductedEvent event) {
        log.info("Publishing inventory-deducted event for orderId={} sku={}", event.orderId(), event.sku());
        kafkaOperations.send(SagaTopics.INVENTORY_DEDUCTED, event.orderId(), event);
    }
}
