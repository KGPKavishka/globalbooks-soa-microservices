# SOA Design Document for GlobalBooks Inc. Refactoring

## Task 1: SOA Design Principles Applied in Decomposing the Monolith

### Overview
In refactoring GlobalBooks Inc.'s legacy Java monolith into a Service-Oriented Architecture (SOA), we applied key SOA design principles to ensure the system becomes modular, scalable, and maintainable. The monolith was decomposed into four autonomous services: Catalog, Orders, Payments, and Shipping. This document outlines the principles applied and the rationale for decomposition.

### SOA Design Principles Applied

1. **Service Autonomy**:
   - Each service (Catalog, Orders, Payments, Shipping) operates independently with its own data store and business logic.
   - Rationale: This allows independent scaling and deployment, addressing the monolith's issue of full regression tests for minor changes. For example, updating the Payments service doesn't affect the Catalog service.

2. **Loose Coupling**:
   - Services communicate via standardized interfaces (SOAP for Catalog, REST for Orders) and asynchronous messaging (RabbitMQ for Payments and Shipping).
   - Rationale: Reduces dependencies, enabling services to evolve without impacting others. This mitigates the tight coupling in the monolith where changes in one module risked system-wide downtime.

3. **Reusability**:
   - Services are designed as reusable components, e.g., Catalog service can be invoked by multiple clients beyond Orders.
   - Rationale: Promotes efficiency in a global e-commerce platform, allowing reuse across different workflows like promotions or inventory checks.

4. **Discoverability**:
   - Services are registered in a UDDI-based registry for dynamic discovery.
   - Rationale: Supports dynamic binding, essential for a distributed system where services may be deployed across different environments.

5. **Composability**:
   - Services are composed using BPEL orchestration for the "PlaceOrder" workflow, combining Catalog lookup, Orders creation, and async notifications to Payments/Shipping.
   - Rationale: Enables complex business processes without embedding logic in a single codebase, improving flexibility for peak loads like holiday promotions.

6. **Interoperability**:
   - Use of standard protocols (SOAP, REST, WS-Security, OAuth2) ensures compatibility with legacy partners and new clients.
   - Rationale: Facilitates integration across North America, Europe, and Asia, addressing the monolith's limitations in supporting diverse client types.

### Decomposition Rationale
- **Business Domain-Driven**: Services align with core business capabilities (catalog management, order processing, payment handling, shipping logistics). This follows domain-driven design (DDD) principles within SOA.
- **Data Separation**: Each service has its own database to prevent shared state issues, reducing contention during peak loads.
- **Scalability and Fault Isolation**: Independent services allow horizontal scaling (e.g., multiple instances of Orders during promotions) and isolated failures (e.g., Shipping outage doesn't halt Orders).
- **Governance and Security**: Centralized governance policies ensure consistency, while security is tailored per interface (WS-Security for SOAP, OAuth2 for REST).

### Benefits Addressed
- **Scalability**: Services can be scaled independently to handle millions of users across regions.
- **Maintainability**: Modular structure reduces regression testing scope.
- **Reliability**: Fault isolation prevents cascading failures.

This design adheres to SOA best practices, transforming the monolith into a resilient, distributed system suitable for GlobalBooks' global operations.

### Viva Defense Points
- Emphasize how principles like autonomy and loose coupling directly solve the monolith's pain points (e.g., downtime during updates).
- Reference real-world SOA frameworks (e.g., OASIS standards for WS-*).
- Discuss trade-offs: While decomposition increases complexity, it enables agility in a high-stakes e-commerce environment.