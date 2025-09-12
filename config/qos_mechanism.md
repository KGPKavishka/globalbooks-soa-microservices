# QoS Mechanism for Reliable Messaging

## Task 14: Persistent Messages

### Overview
For reliable messaging in RabbitMQ, messages are configured as persistent to survive broker restarts.

### Configuration
- **Message Properties**: Set delivery mode to 2 (persistent).
- **Queues/Exchanges**: Declare as durable.
- **Spring AMQP**: Use MessageProperties.PERSISTENT_TEXT_PLAIN.

### Benefits
- Ensures messages are not lost during failures.
- Supports 99.5% uptime SLA.

### Viva Defense Points
- Critical for async workflows; prevents data loss.
- Balances performance with reliability.