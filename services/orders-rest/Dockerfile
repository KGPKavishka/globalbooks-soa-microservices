# FIXED: Dockerfile for OrdersService
FROM openjdk:11-jre-slim

WORKDIR /app

# Copy the JAR file
COPY target/orders-rest-1.0.0.jar app.jar

# Expose port
EXPOSE 8080

# Run the application
CMD ["java", "-jar", "app.jar"]