package com.ordersaga.order.application;

import com.ordersaga.order.application.port.out.ChargePaymentPort;
import org.springframework.stereotype.Service;

@Service
public class OrderProcessor {
    private final OrderApplicationService orderApplicationService;
    private final ChargePaymentPort chargePaymentPort;

    public OrderProcessor(OrderApplicationService orderApplicationService, ChargePaymentPort chargePaymentPort) {
        this.orderApplicationService = orderApplicationService;
        this.chargePaymentPort = chargePaymentPort;
    }

    public OrderResult processOrder(CreateOrderCommand command) {
        OrderResult created = orderApplicationService.createOrder(command);

        boolean paymentSuccess = chargePaymentPort.chargePayment(
                created.orderId(), command.amount(), command.sku(), command.quantity(), command.forceInventoryFailure()
        );

        if (paymentSuccess) {
            return orderApplicationService.confirmOrder(created.orderId());
        } else {
            return orderApplicationService.failOrder(created.orderId());
        }
    }
}
