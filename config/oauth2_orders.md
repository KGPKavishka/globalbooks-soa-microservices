# OAuth2 Setup for OrdersService

## Task 13: OAuth2 Configuration

### Overview
OrdersService uses OAuth2 with JWT for secure REST API access, protecting endpoints from unauthorized clients.

### Configuration
- **Spring Security**: Enable OAuth2 resource server.
- **JWT Validation**: Validate tokens from authorization server.
- **application.yml**: Configure issuer URI, JWK set.

### Implementation
1. **SecurityConfig Class**: @EnableWebSecurity, configure HTTP security.
2. **Annotations**: @PreAuthorize on endpoints.
3. **Test**: Use Postman with Bearer token.

### Viva Defense Points
- Ensures secure access for new clients (ILO3).
- JWT is stateless, scalable for microservices.
- Integrates with Spring Boot for easy setup.