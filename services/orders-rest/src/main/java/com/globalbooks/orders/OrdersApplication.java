package com.globalbooks.orders;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Task 7: OrdersService REST API Design
// Main Spring Boot application class.
// Viva Explanation: Bootstraps REST service; @SpringBootApplication enables auto-config.
// Endpoints: POST /orders (create), GET /orders/{id} (retrieve).

@SpringBootApplication
public class OrdersApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrdersApplication.class, args);
    }
}