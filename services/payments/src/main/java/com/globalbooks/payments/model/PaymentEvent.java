package com.globalbooks.payments.model; // FIXED: New DTO for payment events

import java.math.BigDecimal; // FIXED

// FIXED: Simple POJO used for JSON (de)serialization via Jackson2JsonMessageConverter
public class PaymentEvent { // FIXED

    private String paymentId; // FIXED
    private String orderId; // FIXED
    private BigDecimal amount; // FIXED
    private String currency; // FIXED
    private String status; // FIXED

    public PaymentEvent() { // FIXED: no-args constructor required by Jackson
    }

    public PaymentEvent(String paymentId, String orderId, BigDecimal amount, String currency, String status) { // FIXED: all-args constructor
        this.paymentId = paymentId; // FIXED
        this.orderId = orderId; // FIXED
        this.amount = amount; // FIXED
        this.currency = currency; // FIXED
        this.status = status; // FIXED
    }

    // FIXED: getters/setters for all fields

    public String getPaymentId() { // FIXED
        return paymentId; // FIXED
    }

    public void setPaymentId(String paymentId) { // FIXED
        this.paymentId = paymentId; // FIXED
    }

    public String getOrderId() { // FIXED
        return orderId; // FIXED
    }

    public void setOrderId(String orderId) { // FIXED
        this.orderId = orderId; // FIXED
    }

    public BigDecimal getAmount() { // FIXED
        return amount; // FIXED
    }

    public void setAmount(BigDecimal amount) { // FIXED
        this.amount = amount; // FIXED
    }

    public String getCurrency() { // FIXED
        return currency; // FIXED
    }

    public void setCurrency(String currency) { // FIXED
        this.currency = currency; // FIXED
    }

    public String getStatus() { // FIXED
        return status; // FIXED
    }

    public void setStatus(String status) { // FIXED
        this.status = status; // FIXED
    }

    @Override
    public String toString() { // FIXED
        return "PaymentEvent{" +
                "paymentId='" + paymentId + '\'' +
                ", orderId='" + orderId + '\'' +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                ", status='" + status + '\'' +
                '}'; // FIXED
    }
}