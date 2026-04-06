package com.ordersaga.saga.event;

import java.math.BigDecimal;

public record PaymentCompletedEvent(
        String orderId,
        String paymentId,
        BigDecimal amount,
        String sku,
        Integer quantity
) {
}
