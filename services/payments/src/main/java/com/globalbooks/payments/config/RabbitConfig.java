package com.globalbooks.payments.config; // FIXED: New config package for RabbitMQ topology and listener settings

import java.util.UUID; // FIXED
import org.slf4j.MDC; // FIXED
import org.springframework.amqp.core.Binding; // FIXED
import org.springframework.amqp.core.BindingBuilder; // FIXED
import org.springframework.amqp.core.DirectExchange; // FIXED
import org.springframework.amqp.core.ExchangeBuilder; // FIXED
import org.springframework.amqp.core.Message; // FIXED
import org.springframework.amqp.core.MessagePostProcessor; // FIXED
import org.springframework.amqp.core.MessageProperties; // FIXED
import org.springframework.amqp.core.Queue; // FIXED
import org.springframework.amqp.core.QueueBuilder; // FIXED
import org.springframework.amqp.core.TopicExchange; // FIXED
import org.springframework.amqp.rabbit.annotation.EnableRabbit; // FIXED
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder; // FIXED
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory; // FIXED
import org.springframework.amqp.rabbit.connection.ConnectionFactory; // FIXED
import org.springframework.amqp.rabbit.core.RabbitAdmin; // FIXED
import org.springframework.amqp.rabbit.core.RabbitTemplate; // FIXED
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter; // FIXED
import org.springframework.context.annotation.Bean; // FIXED
import org.springframework.context.annotation.Configuration; // FIXED
import org.springframework.retry.interceptor.RetryOperationsInterceptor; // FIXED
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer; // FIXED

@Configuration // FIXED: Configuration class
@EnableRabbit // FIXED: Enable Rabbit annotations processing
public class RabbitConfig { // FIXED

    // ==== Constants (names and routing keys) ====
    public static final String PAYMENTS_EXCHANGE = "payments.exchange"; // FIXED: topic exchange for payments
    public static final String PAYMENTS_QUEUE = "payments.queue"; // FIXED: main payments queue
    public static final String PAYMENTS_DLX = "payments.dlx"; // FIXED: direct DLX for payments
    public static final String PAYMENTS_DLQ = "payments.dlq"; // FIXED: DLQ queue
    public static final String PAYMENTS_CREATED_ROUTING_KEY = "payments.created"; // FIXED: routing key for inbound events
    public static final String PAYMENTS_DEAD_ROUTING_KEY = "payments.dead"; // FIXED: DLQ routing key

    public static final String SHIPPING_EXCHANGE = "shipping.exchange"; // FIXED: topic exchange for shipping producer
    public static final String SHIPPING_ROUTING_KEY_PROCESS = "shipping.process"; // FIXED: routing key for shipping processing

    public static final String HEADER_CORRELATION_ID = "X-Correlation-Id"; // FIXED: correlation header key

    // ==== Exchanges ====
    @Bean
    public TopicExchange paymentsExchange() {
        return ExchangeBuilder.topicExchange(PAYMENTS_EXCHANGE).durable(true).build(); // FIXED
    }

    @Bean
    public DirectExchange paymentsDlx() {
        return ExchangeBuilder.directExchange(PAYMENTS_DLX).durable(true).build(); // FIXED
    }

    @Bean
    public TopicExchange shippingExchange() {
        return ExchangeBuilder.topicExchange(SHIPPING_EXCHANGE).durable(true).build(); // FIXED
    }

    // ==== Queues ====
    @Bean
    public Queue paymentsQueue() {
        return QueueBuilder.durable(PAYMENTS_QUEUE) // FIXED: durable queue
                .withArgument("x-dead-letter-exchange", PAYMENTS_DLX) // FIXED: route failures to DLX
                .withArgument("x-dead-letter-routing-key", PAYMENTS_DEAD_ROUTING_KEY) // FIXED
                .build();
    }

    @Bean
    public Queue paymentsDlq() {
        return QueueBuilder.durable(PAYMENTS_DLQ).build(); // FIXED
    }

    // ==== Bindings ====
    @Bean
    public Binding paymentsBinding(Queue paymentsQueue, TopicExchange paymentsExchange) {
        return BindingBuilder.bind(paymentsQueue).to(paymentsExchange).with(PAYMENTS_CREATED_ROUTING_KEY); // FIXED
    }

    @Bean
    public Binding paymentsDlqBinding(Queue paymentsDlq, DirectExchange paymentsDlx) {
        return BindingBuilder.bind(paymentsDlq).to(paymentsDlx).with(PAYMENTS_DEAD_ROUTING_KEY); // FIXED
    }

    // ==== Admin to auto-declare topology ====
    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory); // FIXED
        admin.setAutoStartup(true); // FIXED: declare at context startup
        return admin;
    }

    // ==== JSON Message Converter ====
    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter(); // FIXED
    }

    // ==== RabbitTemplate with converter and correlationId passthrough ====
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         Jackson2JsonMessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory); // FIXED
        template.setMessageConverter(messageConverter); // FIXED
        // Ensure correlationId propagation and JSON content-type // FIXED
        template.setBeforePublishPostProcessors(correlationAndContentTypeProcessor()); // FIXED
        return template;
    }

    private MessagePostProcessor correlationAndContentTypeProcessor() { // FIXED
        return (Message message) -> {
            MessageProperties props = message.getMessageProperties(); // FIXED
            // Content type JSON for consumers // FIXED
            props.setContentType(MessageProperties.CONTENT_TYPE_JSON); // FIXED
            // Propagate or generate correlation id // FIXED
            String corr = props.getHeader(HEADER_CORRELATION_ID);
            if (corr == null || corr.isEmpty()) {
                // Try MDC fallback // FIXED
                corr = MDC.get("correlationId");
            }
            if (corr == null || corr.isEmpty()) {
                corr = UUID.randomUUID().toString(); // FIXED
            }
            props.setHeader(HEADER_CORRELATION_ID, corr); // FIXED
            props.setCorrelationId(corr); // FIXED: also set AMQP correlationId property
            return message;
        };
    }

    // ==== Listener container with retry/backoff and no requeue on failure ====
    @Bean(name = "rabbitListenerContainerFactory") // FIXED: explicit factory name used by @RabbitListener
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter messageConverter) {

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory(); // FIXED
        factory.setConnectionFactory(connectionFactory); // FIXED
        factory.setMessageConverter(messageConverter); // FIXED
        factory.setDefaultRequeueRejected(false); // FIXED: send to DLQ via RejectAndDontRequeueRecoverer
        factory.setAdviceChain(retryAdvice()); // FIXED: configure retry interceptor
        return factory;
    }

    @Bean
    public RetryOperationsInterceptor retryAdvice() {
        return RetryInterceptorBuilder.stateless() // FIXED
                .maxAttempts(5) // FIXED: maxAttempts=5
                .backOffOptions(1000L, 2.0, 10000L) // FIXED: initial=1s, multiplier=2, max=10s
                .recoverer(new RejectAndDontRequeueRecoverer()) // FIXED: route to DLQ after retries
                .build();
    }
}