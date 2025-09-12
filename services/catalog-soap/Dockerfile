# FIXED: Dockerfile for CatalogService (WAR)
FROM tomcat:9-jre11

# Copy the WAR file
COPY target/catalog-soap-1.0.0.war /usr/local/tomcat/webapps/catalog.war

# Expose port
EXPOSE 8080

# Start Tomcat
CMD ["catalina.sh", "run"]