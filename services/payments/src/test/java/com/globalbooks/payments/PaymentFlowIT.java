package com.globalbooks.payments; // FIXED: IT under payments package

import com.globalbooks.payments.config.RabbitConfig; // FIXED
import com.globalbooks.payments.model.PaymentEvent; // FIXED
import org.junit.jupiter.api.AfterAll; // FIXED
import org.junit.jupiter.api.BeforeAll; // FIXED
import org.junit.jupiter.api.Test; // FIXED
import org.junit.jupiter.api.Assertions; // FIXED
import org.springframework.amqp.core.MessagePostProcessor; // FIXED
import org.springframework.amqp.core.MessageProperties; // FIXED
import org.springframework.amqp.core.QueueInformation; // FIXED
import org.springframework.amqp.rabbit.core.RabbitAdmin; // FIXED
import org.springframework.amqp.rabbit.core.RabbitTemplate; // FIXED
import org.springframework.beans.factory.annotation.Autowired; // FIXED
import org.springframework.boot.test.context.SpringBootTest; // FIXED
import org.springframework.test.context.DynamicPropertyRegistry; // FIXED
import org.springframework.test.context.DynamicPropertySource; // FIXED

import org.testcontainers.containers.RabbitMQContainer; // FIXED
import org.testcontainers.containers.GenericContainer; // FIXED
import org.testcontainers.utility.DockerImageName; // FIXED
import org.testcontainers.junit.jupiter.Container; // FIXED
import org.testcontainers.junit.jupiter.Testcontainers; // FIXED

import java.math.BigDecimal; // FIXED
import java.time.Duration; // FIXED
import java.util.UUID; // FIXED

@SpringBootTest // FIXED: Load full Spring context including our Rabbit/Redis config
@Testcontainers // FIXED: Use Testcontainers for RabbitMQ and Redis
public class PaymentFlowIT { // FIXED

    @Container // FIXED: RabbitMQ container for AMQP broker
    static final RabbitMQContainer rabbit = new RabbitMQContainer(DockerImageName.parse("rabbitmq:3.11-management")); // FIXED

    @Container // FIXED: Redis container for idempotency store
    static final GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379); // FIXED

    @DynamicPropertySource // FIXED: Wire container ports to Spring properties
    static void overrideProps(DynamicPropertyRegistry registry) { // FIXED
        registry.add("spring.rabbitmq.host", rabbit::getHost); // FIXED
        registry.add("spring.rabbitmq.port", rabbit::getAmqpPort); // FIXED
        registry.add("spring.rabbitmq.username", rabbit::getAdminUsername); // FIXED
        registry.add("spring.rabbitmq.password", rabbit::getAdminPassword); // FIXED

        registry.add("spring.redis.host", redis::getHost); // FIXED
        registry.add("spring.redis.port", () -> redis.getMappedPort(6379)); // FIXED
    }

    @Autowired
    RabbitTemplate rabbitTemplate; // FIXED

    @Autowired
    RabbitAdmin rabbitAdmin; // FIXED

    @Autowired
    org.springframework.data.redis.core.RedisTemplate<String, String> redisTemplate; // FIXED

    @BeforeAll
    static void startContainers() {
        // Containers are started automatically by @Container // FIXED
    }

    @AfterAll
    static void stopContainers() {
        // Containers are stopped automatically // FIXED
    }

    @Test
    void paymentProcessedOnce_idempotent_andNoDlq() throws Exception { // FIXED
        String paymentId = "p-" + UUID.randomUUID(); // FIXED
        String orderId = "o-" + UUID.randomUUID(); // FIXED
        String corr = UUID.randomUUID().toString(); // FIXED

        // Arrange: build event // FIXED
        PaymentEvent event = new PaymentEvent(paymentId, orderId, new BigDecimal("49.99"), "USD", "CREATED"); // FIXED

        // Act: send event to payments.exchange with routing key payments.created // FIXED
        rabbitTemplate.convertAndSend(
                RabbitConfig.PAYMENTS_EXCHANGE,
                RabbitConfig.PAYMENTS_CREATED_ROUTING_KEY,
                event,
                correlationAndJsonHeaders(corr) // FIXED
        );

        // Wait until Redis idempotency key appears // FIXED
        boolean processed = waitUntil(() -> existsIdempotencyKey(paymentId), Duration.ofSeconds(15), Duration.ofMillis(250)); // FIXED
        Assertions.assertTrue(processed, "Payment did not process within timeout"); // FIXED

        // Send same event again: should be idempotently skipped // FIXED
        rabbitTemplate.convertAndSend(
                RabbitConfig.PAYMENTS_EXCHANGE,
                RabbitConfig.PAYMENTS_CREATED_ROUTING_KEY,
                event,
                correlationAndJsonHeaders(corr)
        );

        // Assert: idempotency key still exists // FIXED
        Assertions.assertTrue(existsIdempotencyKey(paymentId), "Idempotency key missing after duplicate send"); // FIXED

        // Assert: DLQ has no messages in normal case // FIXED
        QueueInformation dlqInfo = rabbitAdmin.getQueueInfo(RabbitConfig.PAYMENTS_DLQ); // FIXED
        int dlqCount = (dlqInfo != null) ? dlqInfo.getMessageCount() : 0; // FIXED
        Assertions.assertEquals(0, dlqCount, "Unexpected messages in DLQ"); // FIXED
    }

    private boolean existsIdempotencyKey(String paymentId) { // FIXED
        String key = "payments:processed:" + paymentId; // FIXED
        String val = redisTemplate.opsForValue().get(key); // FIXED
        return val != null; // FIXED
    }

    private boolean waitUntil(Check check, Duration timeout, Duration poll) throws InterruptedException { // FIXED
        long end = System.currentTimeMillis() + timeout.toMillis(); // FIXED
        while (System.currentTimeMillis() < end) { // FIXED
            if (check.ok()) return true; // FIXED
            Thread.sleep(poll.toMillis()); // FIXED
        }
        return false; // FIXED
    }

    private interface Check { // FIXED
        boolean ok(); // FIXED
    }

    private MessagePostProcessor correlationAndJsonHeaders(String corr) { // FIXED
        return m -> {
            MessageProperties p = m.getMessageProperties(); // FIXED
            p.setContentType(MessageProperties.CONTENT_TYPE_JSON); // FIXED
            p.setCorrelationId(corr); // FIXED
            p.setHeader(RabbitConfig.HEADER_CORRELATION_ID, corr); // FIXED
            return m; // FIXED
        };
    }
}