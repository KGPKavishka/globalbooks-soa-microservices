# Cloud Deployment for GlobalBooks Services

## Task 16: Deploy to Cloud Platform

### Platform: AWS ECS with Docker
- **Containerization**: Each service (Catalog, Orders, Payments, Shipping) in Docker containers.
- **Orchestration**: ECS for scaling and management.
- **Steps**:
  1. Build Docker images with Maven/Gradle.
  2. Push to ECR.
  3. Deploy to ECS clusters.
  4. Configure ALB for load balancing.
- **Benefits**: Scalable, supports ILO4 (cloud implementation).

### Viva Defense Points
- Demonstrates microservices in cloud.
- Ensures high availability and scalability.