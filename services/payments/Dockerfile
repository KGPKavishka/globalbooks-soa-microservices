# FIXED: Dockerfile for PaymentsService
# Use Java 17 runtime to match pom.xml (java.version=17)
FROM openjdk:17-jre-slim

WORKDIR /app

# Copy the JAR file
COPY target/payments-1.0.0.jar app.jar

# Expose port
EXPOSE 8080

# Run the application
CMD ["java", "-jar", "app.jar"]