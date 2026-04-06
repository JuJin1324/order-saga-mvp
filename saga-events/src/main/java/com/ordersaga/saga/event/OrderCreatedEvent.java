package com.ordersaga.saga.event;

import java.math.BigDecimal;

public record OrderCreatedEvent(
        String orderId,
        String sku,
        Integer quantity,
        BigDecimal amount
) {
}
