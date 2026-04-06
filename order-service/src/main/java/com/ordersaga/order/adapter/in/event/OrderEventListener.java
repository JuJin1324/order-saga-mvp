package com.ordersaga.order.adapter.in.event;

import com.ordersaga.saga.SagaTopics;
import com.ordersaga.saga.event.InventoryDeductedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.kafka.listeners.enabled", havingValue = "true")
public class OrderEventListener {
    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);

    @KafkaListener(topics = SagaTopics.INVENTORY_DEDUCTED, groupId = "${spring.application.name}")
    public void onInventoryDeducted(InventoryDeductedEvent event) {
        log.info("Received inventory-deducted event skeleton for orderId={} sku={}", event.orderId(), event.sku());
    }
}
