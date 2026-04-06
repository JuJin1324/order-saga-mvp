package com.ordersaga.order.fixture;

import com.ordersaga.order.adapter.in.web.dto.CreateOrderRequest;

import static com.ordersaga.order.fixture.OrderFixtureValues.*;

public class CreateOrderRequestFixture {

    public static CreateOrderRequest normal() {
        return new CreateOrderRequest(SKU, QUANTITY, AMOUNT, false);
    }

    public static CreateOrderRequest withForceInventoryFailure() {
        return new CreateOrderRequest(SKU, QUANTITY, AMOUNT, true);
    }

    public static CreateOrderRequest missingSku() {
        return new CreateOrderRequest(null, QUANTITY, AMOUNT, false);
    }
}
