package com.globalbooks.orders;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

// FIXED: Added explicit import for Order class to resolve potential compilation issues
import com.globalbooks.orders.Order;

// REST Controller for OrdersService
// Endpoints: POST /orders (create order), GET /orders/{id} (get order)
// Viva Explanation: Demonstrates REST architecture (ILO3); uses Spring MVC for HTTP methods.
// Sample JSON: See comments below.

@RestController
@RequestMapping("/orders")
public class OrderController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    // FIXED: Made RabbitMQ optional for demo
    @Autowired(required = false)
    private RabbitTemplate rabbitTemplate;

    // Demo data store - FIXED: In production, use database
    private static final Map<String, Order> orders = new HashMap<>();
    private static int orderCounter = 1;

    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody Order order) {
        String correlationId = UUID.randomUUID().toString();
        logger.info("Creating order with correlationId: {}", correlationId);

        order.setId(String.valueOf(orderCounter++));
        order.setStatus("PENDING");
        orders.put(order.getId(), order);

        // FIXED: Publish order created event to RabbitMQ (if available)
        if (rabbitTemplate != null) {
            rabbitTemplate.convertAndSend("globalbooks.exchange", "order.created", order, message -> {
                message.getMessageProperties().setCorrelationId(correlationId);
                return message;
            });
        }

        logger.info("Order created: {}", order.getId());
        return ResponseEntity.ok(order);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrder(@PathVariable String id) {
        logger.info("Retrieving order: {}", id);
        Order order = orders.get(id);
        if (order != null) {
            return ResponseEntity.ok(order);
        } else {
            logger.warn("Order not found: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    /*
    Sample JSON Request for POST /orders:
    {
        "customerId": "cust123",
        "items": [
            {
                "bookId": "1",
                "quantity": 2,
                "price": 29.99
            }
        ],
        "totalAmount": 59.98
    }

    Sample JSON Response for POST /orders:
    {
        "id": "1",
        "customerId": "cust123",
        "items": [
            {
                "bookId": "1",
                "quantity": 2,
                "price": 29.99
            }
        ],
        "totalAmount": 59.98,
        "status": "PENDING"
    }

    Sample JSON Response for GET /orders/1:
    {
        "id": "1",
        "customerId": "cust123",
        "items": [...],
        "totalAmount": 59.98,
        "status": "PENDING"
    }
    */
}

