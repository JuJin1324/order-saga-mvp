package com.ordersaga.order.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;

@Configuration(proxyBeanMethods = false)
@EnableKafka
public class OrderKafkaConfiguration {
}
