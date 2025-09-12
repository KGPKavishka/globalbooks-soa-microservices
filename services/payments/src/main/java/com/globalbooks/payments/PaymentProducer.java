package com.globalbooks.payments; // FIXED: Keep package consistent

import com.globalbooks.payments.config.RabbitConfig; // FIXED: Use exchange/routing key constants
import org.slf4j.Logger; // FIXED
import org.slf4j.LoggerFactory; // FIXED
import org.slf4j.MDC; // FIXED
import org.springframework.amqp.core.MessagePostProcessor; // FIXED
import org.springframework.amqp.core.MessageProperties; // FIXED
import org.springframework.amqp.rabbit.core.RabbitTemplate; // FIXED
import org.springframework.stereotype.Service; // FIXED

import java.util.HashMap; // FIXED
import java.util.Map; // FIXED
import java.util.UUID; // FIXED

@Service // FIXED
public class PaymentProducer { // FIXED

    private static final Logger logger = LoggerFactory.getLogger(PaymentProducer.class); // FIXED
    private final RabbitTemplate rabbitTemplate; // FIXED

    public PaymentProducer(RabbitTemplate rabbitTemplate) { // FIXED
        this.rabbitTemplate = rabbitTemplate; // FIXED
    }

    // FIXED: Publish to shipping.exchange with routing key shipping.process and propagate correlationId
    public void processPayment(String orderId, double amount, String correlationId) { // FIXED
        String corr = (correlationId == null || correlationId.isEmpty())
                ? UUID.randomUUID().toString()
                : correlationId; // FIXED

        // Put correlationId into MDC for structured logs // FIXED
        if (corr != null) {
            MDC.put("correlationId", corr); // FIXED
            MDC.put(RabbitConfig.HEADER_CORRELATION_ID, corr); // FIXED
        }

        try {
            // Construct a simple shipping event payload; serialized as JSON by Jackson2JsonMessageConverter // FIXED
            Map<String, Object> shippingEvent = new HashMap<>(); // FIXED
            shippingEvent.put("orderId", orderId); // FIXED
            shippingEvent.put("amount", amount); // FIXED
            shippingEvent.put("status", "PAID"); // FIXED
            shippingEvent.put("source", "payments"); // FIXED

            logger.info("Publishing shipping event for orderId={} amount={} to exchange={} rk={}",
                    orderId, amount, RabbitConfig.SHIPPING_EXCHANGE, RabbitConfig.SHIPPING_ROUTING_KEY_PROCESS); // FIXED

            rabbitTemplate.convertAndSend(
                    RabbitConfig.SHIPPING_EXCHANGE,
                    RabbitConfig.SHIPPING_ROUTING_KEY_PROCESS,
                    shippingEvent,
                    correlationAndJsonHeaders(corr) // FIXED
            );

            logger.debug("Shipping event published with correlationId={}", corr); // FIXED
        } finally {
            MDC.remove("correlationId"); // FIXED
            MDC.remove(RabbitConfig.HEADER_CORRELATION_ID); // FIXED
        }
    }

    private MessagePostProcessor correlationAndJsonHeaders(String corr) { // FIXED
        return message -> {
            MessageProperties props = message.getMessageProperties(); // FIXED
            props.setContentType(MessageProperties.CONTENT_TYPE_JSON); // FIXED: application/json header
            props.setCorrelationId(corr); // FIXED: AMQP correlationId property
            props.setHeader(RabbitConfig.HEADER_CORRELATION_ID, corr); // FIXED: custom header for propagation
            return message; // FIXED
        };
    }
}