package com.ordersaga.order.adapter.in.web;

import java.util.Map;

import com.ordersaga.order.application.CreateOrderCommand;
import com.ordersaga.order.application.OrderApplicationService;
import com.ordersaga.order.application.OrderResult;
import com.ordersaga.order.application.OrderProcessor;
import com.ordersaga.order.adapter.in.web.dto.CreateOrderRequest;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderProcessor orderProcessor;
    private final OrderApplicationService orderApplicationService;

    public OrderController(OrderProcessor orderProcessor, OrderApplicationService orderApplicationService) {
        this.orderProcessor = orderProcessor;
        this.orderApplicationService = orderApplicationService;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of(
                "service", "order-service",
                "status", "ok"
        );
    }

    @GetMapping("/{orderId}")
    public OrderResult getOrder(@PathVariable String orderId) {
        return orderApplicationService.getOrder(orderId);
    }

    @PostMapping
    public OrderResult createOrder(@Valid @RequestBody CreateOrderRequest request) {
        CreateOrderCommand command = new CreateOrderCommand(
                request.sku(),
                request.quantity(),
                request.amount(),
                request.forceInventoryFailure()
        );
        return orderProcessor.processOrder(command);
    }
}
