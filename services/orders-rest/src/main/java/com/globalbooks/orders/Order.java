package com.globalbooks.orders;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;

// FIXED: Added Jackson annotations for proper JSON deserialization
public class Order {
    private String id;
    private String customerId;
    private List<OrderItem> items;
    private double totalAmount;
    private String status; // e.g., PENDING, CONFIRMED

    public Order() {}

    public Order(String id, String customerId, List<OrderItem> items, double totalAmount, String status) {
        this.id = id;
        this.customerId = customerId;
        this.items = items;
        this.totalAmount = totalAmount;
        this.status = status;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    // Inner class for OrderItem
    public static class OrderItem {
        private String bookId;
        private int quantity;
        private double price;

        public OrderItem() {}

        public OrderItem(String bookId, int quantity, double price) {
            this.bookId = bookId;
            this.quantity = quantity;
            this.price = price;
        }

        // Getters and setters
        public String getBookId() { return bookId; }
        public void setBookId(String bookId) { this.bookId = bookId; }

        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }

        public double getPrice() { return price; }
        public void setPrice(double price) { this.price = price; }
    }
}