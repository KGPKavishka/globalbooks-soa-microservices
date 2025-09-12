package com.globalbooks.payments; // FIXED: Ensure package remains under com.globalbooks.payments

import com.globalbooks.payments.config.RabbitConfig; // FIXED: Use constants for queue names and headers
import com.globalbooks.payments.model.PaymentEvent; // FIXED: Typed DTO for JSON consumption

import java.time.Duration; // FIXED
import java.util.Map; // FIXED

import org.slf4j.Logger; // FIXED
import org.slf4j.LoggerFactory; // FIXED
import org.slf4j.MDC; // FIXED
import org.springframework.amqp.AmqpRejectAndDontRequeueException; // FIXED: Route failures to DLQ after retries
import org.springframework.amqp.rabbit.annotation.RabbitListener; // FIXED
import org.springframework.data.redis.core.RedisTemplate; // FIXED
import org.springframework.messaging.handler.annotation.Headers; // FIXED
import org.springframework.stereotype.Service; // FIXED

@Service // FIXED
public class PaymentConsumer { // FIXED

    private static final Logger logger = LoggerFactory.getLogger(PaymentConsumer.class); // FIXED
    private static final String IDEMPOTENCY_PREFIX = "payments:processed:"; // FIXED
    private static final Duration IDEMPOTENCY_TTL = Duration.ofSeconds(86400); // FIXED: 24h TTL

    private final PaymentProducer paymentProducer; // FIXED
    private final RedisTemplate<String, String> stringRedisTemplate; // FIXED

    public PaymentConsumer(PaymentProducer paymentProducer,
                           RedisTemplate<String, String> stringRedisTemplate) { // FIXED
        this.paymentProducer = paymentProducer; // FIXED
        this.stringRedisTemplate = stringRedisTemplate; // FIXED
    }

    @RabbitListener(queues = RabbitConfig.PAYMENTS_QUEUE, containerFactory = "rabbitListenerContainerFactory") // FIXED: Robust listener with retry/backoff
    public void handlePaymentEvent(PaymentEvent event, @Headers Map<String, Object> headers) { // FIXED: Typed DTO and headers access
        String correlationId = extractCorrelationId(headers); // FIXED
        putCorrelationIntoMdc(correlationId); // FIXED

        String paymentId = event != null ? event.getPaymentId() : null; // FIXED
        String key = IDEMPOTENCY_PREFIX + paymentId; // FIXED
        try {
            if (paymentId == null || paymentId.isBlank()) { // FIXED
                logger.error("Invalid PaymentEvent: missing paymentId: {}", event);
                // Irrecoverable, send to DLQ (after retries handled by interceptor) // FIXED
                throw new AmqpRejectAndDontRequeueException("Missing paymentId");
            }

            // Idempotency check using Redis: set-if-absent with TTL // FIXED
            Boolean firstTime = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", IDEMPOTENCY_TTL); // FIXED
            if (Boolean.FALSE.equals(firstTime)) {
                logger.warn("Idempotent skip. Payment already processed. paymentId={}", paymentId); // FIXED
                return; // FIXED: Ack the message (no exception) to avoid reprocessing
            }

            logger.info("Processing PaymentEvent: {}", event); // FIXED

            // Process payment and emit shipping event // FIXED
            double amount = event.getAmount() != null ? event.getAmount().doubleValue() : 0.0; // FIXED
            paymentProducer.processPayment(event.getOrderId(), amount, correlationId); // FIXED

            logger.info("PaymentEvent processed successfully. paymentId={}", paymentId); // FIXED
        } catch (AmqpRejectAndDontRequeueException ex) { // FIXED
            throw ex; // FIXED: Ensure it goes to DLQ without requeue
        } catch (Exception ex) {
            logger.error("Error while processing PaymentEvent paymentId={}: {}", paymentId, ex.getMessage(), ex); // FIXED
            // Let retry interceptor handle backoff; if exhausted, go to DLQ // FIXED
            throw ex; // FIXED
        } finally {
            clearCorrelationFromMdc(); // FIXED
        }
    }

    private String extractCorrelationId(Map<String, Object> headers) { // FIXED
        if (headers == null) return null; // FIXED
        Object corr = headers.get(RabbitConfig.HEADER_CORRELATION_ID); // FIXED
        if (corr == null) {
            corr = headers.get("correlationId"); // FIXED: Spring standard correlation header fallback
        }
        return corr != null ? String.valueOf(corr) : null; // FIXED
    }

    private void putCorrelationIntoMdc(String correlationId) { // FIXED
        if (correlationId != null && !correlationId.isEmpty()) {
            MDC.put("correlationId", correlationId); // FIXED
            MDC.put(RabbitConfig.HEADER_CORRELATION_ID, correlationId); // FIXED
        }
    }

    private void clearCorrelationFromMdc() { // FIXED
        MDC.remove("correlationId"); // FIXED
        MDC.remove(RabbitConfig.HEADER_CORRELATION_ID); // FIXED
    }
}