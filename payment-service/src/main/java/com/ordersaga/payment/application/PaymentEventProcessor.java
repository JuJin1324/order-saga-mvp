package com.ordersaga.payment.application;

import com.ordersaga.payment.infrastructure.kafka.PaymentEventPublisher;
import com.ordersaga.saga.event.OrderCreatedEvent;
import com.ordersaga.saga.event.PaymentCompletedEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnBean(PaymentEventPublisher.class)
public class PaymentEventProcessor {
    private final PaymentApplicationService paymentApplicationService;
    private final PaymentEventPublisher paymentEventPublisher;

    public PaymentEventProcessor(
            PaymentApplicationService paymentApplicationService,
            PaymentEventPublisher paymentEventPublisher
    ) {
        this.paymentApplicationService = paymentApplicationService;
        this.paymentEventPublisher = paymentEventPublisher;
    }

    public PaymentResult handleOrderCreated(OrderCreatedEvent event) {
        ChargePaymentCommand command = new ChargePaymentCommand(
                event.orderId(),
                event.amount(),
                event.sku(),
                event.quantity(),
                false
        );
        PaymentResult paymentResult = paymentApplicationService.processPayment(command);

        paymentEventPublisher.publishPaymentCompleted(new PaymentCompletedEvent(
                paymentResult.orderId(),
                paymentResult.paymentId(),
                paymentResult.amount(),
                event.sku(),
                event.quantity()
        ));

        return paymentResult;
    }
}
