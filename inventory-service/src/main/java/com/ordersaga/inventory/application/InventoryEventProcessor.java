package com.ordersaga.inventory.application;

import com.ordersaga.inventory.infrastructure.kafka.InventoryEventPublisher;
import com.ordersaga.saga.event.InventoryDeductedEvent;
import com.ordersaga.saga.event.PaymentCompletedEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnBean(InventoryEventPublisher.class)
public class InventoryEventProcessor {
    private final InventoryApplicationService inventoryApplicationService;
    private final InventoryEventPublisher inventoryEventPublisher;

    public InventoryEventProcessor(
            InventoryApplicationService inventoryApplicationService,
            InventoryEventPublisher inventoryEventPublisher
    ) {
        this.inventoryApplicationService = inventoryApplicationService;
        this.inventoryEventPublisher = inventoryEventPublisher;
    }

    public DeductInventoryResult handlePaymentCompleted(PaymentCompletedEvent event) {
        DeductInventoryCommand command = new DeductInventoryCommand(
                event.sku(),
                event.quantity(),
                false
        );
        DeductInventoryResult result = inventoryApplicationService.deductInventory(command);

        inventoryEventPublisher.publishInventoryDeducted(new InventoryDeductedEvent(
                event.orderId(),
                result.sku(),
                result.deductedQuantity(),
                result.remainingQuantity()
        ));

        return result;
    }
}
