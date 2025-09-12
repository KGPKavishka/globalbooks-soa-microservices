# Governance Policy for GlobalBooks SOA

## Task 15: Versioning, SLA, Deprecation

### Versioning Strategy
- **URL Conventions**: /v1/orders, /v2/orders for REST.
- **Namespace Conventions**: http://globalbooks.com/catalog/v1 for SOAP.
- **Backward Compatibility**: Maintain v1 for 12 months post-v2 release.

### SLA Targets
- **Availability**: 99.5% uptime.
- **Response Time**: Sub-200 ms for 95% of requests.
- **Monitoring**: Use tools like Prometheus for metrics.

### Deprecation Plan
- **Notice Period**: 6 months advance notice via UDDI and documentation.
- **Sunset Process**: Mark as deprecated, remove after grace period.
- **Migration Support**: Provide migration guides.

### Viva Defense Points
- Ensures long-term maintainability (ILO1).
- SLA supports business reliability.
- Deprecation prevents technical debt.