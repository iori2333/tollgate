package com.example.tollgate.billing;

import org.apache.commons.logging.LogFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;

import java.util.function.Consumer;

@SpringBootApplication
public class BillingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BillingServiceApplication.class, args);
    }

    @Bean
    public Consumer<Message<String>> heartbeatConsumer() {
        return message -> LogFactory.getLog(BillingServiceApplication.class).info("Received " + message.getPayload());
    }
}