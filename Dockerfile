# Multi-stage Dockerfile for IAPD (Investment Adviser Public Disclosure Parser)
# Stage 1: Build the application
FROM maven:3.9-eclipse-temurin-21 AS builder

# Set working directory
WORKDIR /build

# Copy Maven configuration files first (for better layer caching)
COPY pom.xml .
COPY MANIFEST.MF .

# Download dependencies (this layer will be cached unless pom.xml changes)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src/ ./src/

# Build the application (creates the fat JAR)
RUN mvn clean package -DskipTests -B

# Verify the JAR was created
RUN ls -la target/ && test -f target/iapd-1.0.0-SNAPSHOT-all.jar

# Stage 2: Create the runtime image
FROM openjdk:21-jdk-slim

# Install necessary packages
RUN apt-get update && apt-get install -y \
    curl \
    wget \
    && rm -rf /var/lib/apt/lists/*

# Create a non-root user for security
RUN groupadd -r iapd && useradd -r -g iapd -d /app -s /bin/bash iapd

# Set working directory
WORKDIR /app

# Copy the JAR from the builder stage
COPY --from=builder /build/target/iapd-1.0.0-SNAPSHOT-all.jar ./iapd.jar

# Create the Data directory structure
#RUN mkdir -p Data/Downloads Data/Output Data/Input Data/FirmFiles Data/Logs

# Copy the entrypoint script
COPY ./scripts/docker-entrypoint.sh ./
RUN chmod +x docker-entrypoint.sh

# Change ownership to the iapd user
RUN chown -R iapd:iapd /app

# Switch to non-root user
USER iapd

# Set default Java options (can be overridden with environment variables)
ENV JAVA_OPTS="-Xmx2g -Xms512m -XX:+UseG1GC -XX:+UseStringDeduplication"

# Expose the Data directory as a volume
VOLUME ["/app/Data"]

# Set the entrypoint
ENTRYPOINT ["./docker-entrypoint.sh"]

# Default command (can be overridden)
CMD []

# Add labels for metadata
LABEL maintainer="IAPD Team"
LABEL description="Investment Adviser Public Disclosure Parser - Containerized"
LABEL version="1.0.0"
