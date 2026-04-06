package com.ordersaga.order.application.port.out;

import java.math.BigDecimal;

public interface ChargePaymentPort {
    boolean chargePayment(
            String orderId,
            BigDecimal amount,
            String sku,
            Integer quantity,
            boolean forceInventoryFailure
    );
}
