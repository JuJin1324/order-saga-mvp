package com.ordersaga.order.infrastructure.kafka;

import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;

@Configuration(proxyBeanMethods = false)
@EnableKafka
public class OrderKafkaConfiguration {
}
