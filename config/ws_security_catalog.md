# WS-Security Configuration for CatalogService

## Task 12: WS-Security Setup (UsernameToken)

### Overview
CatalogService uses WS-Security with UsernameToken for authentication, ensuring secure SOAP communications with legacy partners.

### Configuration
- **Handler Chain**: Custom handler validates UsernameToken in SOAP headers.
- **web.xml**: Enable security interceptor.
- **Keystore**: For demo, use simple username/password validation.

### Implementation
1. **Handler Class**: Implement SOAPHandler to check username/password.
2. **Annotation**: @HandlerChain on service class.
3. **Test**: Use SOAP UI with WS-Security headers.

### Viva Defense Points
- Protects against unauthorized access (ILO2).
- UsernameToken is lightweight for demo; X.509 for production.
- Integrates with JAX-WS runtime for seamless security.