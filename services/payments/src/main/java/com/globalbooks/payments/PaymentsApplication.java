package com.globalbooks.payments;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Task 10: PaymentsService with RabbitMQ Producer
// Main application class.
// Viva Explanation: Produces payment messages to RabbitMQ queue for async processing.

@SpringBootApplication
public class PaymentsApplication {
    public static void main(String[] args) {
        SpringApplication.run(PaymentsApplication.class, args);
    }
}