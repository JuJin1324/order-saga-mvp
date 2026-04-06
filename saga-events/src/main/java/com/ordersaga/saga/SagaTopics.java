package com.ordersaga.saga;

public final class SagaTopics {
    public static final String ORDER_CREATED = "order-created";
    public static final String PAYMENT_COMPLETED = "payment-completed";
    public static final String INVENTORY_DEDUCTED = "inventory-deducted";

    private SagaTopics() {
    }
}
