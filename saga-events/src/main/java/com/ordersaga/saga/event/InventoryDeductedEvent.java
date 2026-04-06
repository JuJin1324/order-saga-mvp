package com.ordersaga.saga.event;

public record InventoryDeductedEvent(
        String orderId,
        String sku,
        Integer deductedQuantity,
        Integer remainingQuantity
) {
}
