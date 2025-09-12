# Error-Handling and Dead-Letter Routing Strategy

## Task 11: Error-Handling and Dead-Letter Routing

### Overview
In the GlobalBooks SOA, RabbitMQ is used for async messaging between Payments and Shipping services. Robust error-handling ensures reliability under failure scenarios.

### Dead-Letter Queues (DLQ)
- **Purpose**: Messages that cannot be processed (e.g., invalid format, service down) are routed to DLQ for later inspection.
- **Configuration**: Each main queue (payments.queue, shipping.queue) has a corresponding DLQ (payments.dlq, shipping.dlq).
- **Routing**: If max retries exceeded, message sent to DLQ with original routing key.

### Retry Mechanism
- **Exponential Backoff**: Failed messages retried with increasing delays (e.g., 1s, 2s, 4s).
- **Max Retries**: Set to 3 attempts before DLQ.
- **Implementation**: Use Spring Retry in consumers.

### Error Scenarios
- **Network Failures**: Messages persist in queue until reconnection.
- **Processing Errors**: Log errors, send to DLQ if unrecoverable.
- **Poison Messages**: Messages causing repeated failures moved to DLQ.

### Monitoring
- **Tools**: RabbitMQ Management UI for queue status, message counts.
- **Alerts**: Notify on high DLQ sizes.
- **Logs**: Consumer logs include error details for debugging.

### Viva Defense Points
- Ensures 99.5% uptime SLA by preventing message loss.
- Balances reliability with performance; DLQ prevents blocking main queues.