package com.ordersaga.order.adapter.in.web;

import com.ordersaga.order.adapter.in.web.dto.CreateOrderRequest;
import com.ordersaga.order.application.port.out.ChargePaymentPort;
import com.ordersaga.order.domain.OrderStatus;
import com.ordersaga.order.fixture.CreateOrderRequestFixture;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CreateOrderIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ChargePaymentPort chargePaymentPort;

    @Test
    @DisplayName("주문 생성 성공 시 CONFIRMED 상태로 응답한다")
    void paymentSuccess_returnsConfirmed() throws Exception {
        // Given
        CreateOrderRequest request = CreateOrderRequestFixture.normal();
        given(chargePaymentPort.chargePayment(any(), any(), any(), anyInt(), anyBoolean())).willReturn(true);

        // When & Then
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").exists())
                .andExpect(jsonPath("$.status").value(OrderStatus.CONFIRMED.name()))
                .andExpect(jsonPath("$.sku").value(request.sku()))
                .andExpect(jsonPath("$.quantity").value(request.quantity()))
                .andExpect(jsonPath("$.amount").value(request.amount().intValue()));
    }

    @Test
    @DisplayName("결제 실패 시 FAILED 상태로 응답한다")
    void paymentFailure_returnsFailed() throws Exception {
        // Given
        CreateOrderRequest request = CreateOrderRequestFixture.withForceInventoryFailure();
        given(chargePaymentPort.chargePayment(any(), any(), any(), anyInt(), anyBoolean())).willReturn(false);

        // When & Then
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").exists())
                .andExpect(jsonPath("$.status").value(OrderStatus.FAILED.name()));
    }

    @Test
    @DisplayName("필수 필드 누락 시 400 응답을 반환한다")
    void missingRequiredField_returns400() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(CreateOrderRequestFixture.missingSku())))
                .andExpect(status().isBadRequest());
    }
}
