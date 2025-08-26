# IAPD Docker Container

This document provides instructions for building and running the IAPD (Investment Adviser Public Disclosure Parser) application using Docker.

## Overview

The IAPD Docker container provides a consistent, isolated environment for running the IAPD parser application. It includes all necessary dependencies and is configured to work with your existing data directory structure.

## Prerequisites

- Docker installed on your system
- Docker Compose (optional, but recommended)
- At least 4GB of available RAM for optimal performance
- Your IAPD data directory at `~/Work/IAPD/`

## Quick Start

### 1. Build the Docker Image

```bash
# Build the image
docker build -t iapd:latest .

# Or using Docker Compose
docker-compose build
```

### 2. Run the Container

```bash
# Basic run with data directory mounted
docker run -v ~/Work/IAPD:/app/Data iapd:latest

# Run with custom memory settings
docker run -e JAVA_OPTS='-Xmx4g -Xms1g' -v ~/Work/IAPD:/app/Data iapd:latest

# Run with command line arguments
docker run -v ~/Work/IAPD:/app/Data iapd:latest --index-limit 1000 --verbose
```

### 3. Using Docker Compose (Recommended)

```bash
# Run the main service
docker-compose up iapd

# Run in development mode
docker-compose --profile dev up iapd-dev

# Run in background
docker-compose up -d iapd
```

## Container Configuration

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `JAVA_OPTS` | `-Xmx2g -Xms512m -XX:+UseG1GC -XX:+UseStringDeduplication` | Java JVM options |
| `LOG_LEVEL` | `INFO` | Application logging level |

### Volume Mounts

| Host Path | Container Path | Description |
|-----------|----------------|-------------|
| `~/Work/IAPD` | `/app/Data` | Main data directory containing all input/output files |

### Directory Structure

The container expects the following directory structure in the mounted volume:

```
~/Work/IAPD/
├── Downloads/     # Downloaded brochure files
├── Output/        # Processed output files
├── Input/         # Input data files
├── FirmFiles/     # SEC firm data files
└── Logs/          # Application log files
```

## Usage Examples

### Basic Processing

```bash
# Run with default settings
docker run -v ~/Work/IAPD:/app/Data iapd:latest

# Run with index limit
docker run -v ~/Work/IAPD:/app/Data iapd:latest --index-limit 500

# Run with verbose logging
docker run -v ~/Work/IAPD:/app/Data iapd:latest --verbose
```

### Resume Operations

```bash
# Resume downloads
docker run -v ~/Work/IAPD:/app/Data iapd:latest --resume-downloads

# Resume processing
docker run -v ~/Work/IAPD:/app/Data iapd:latest --resume-processing

# Resume URL extraction
docker run -v ~/Work/IAPD:/app/Data iapd:latest --resume-urlextraction
```

### Incremental Updates

```bash
# Incremental mode with baseline file
docker run -v ~/Work/IAPD:/app/Data iapd:latest \
  --incremental \
  --baseline-file /app/Data/Output/IAPD_Data.csv

# Monthly incremental updates
docker run -v ~/Work/IAPD:/app/Data iapd:latest \
  --incremental \
  --month january
```

### Custom Memory Settings

```bash
# High memory configuration (8GB)
docker run -e JAVA_OPTS='-Xmx8g -Xms2g' \
  -v ~/Work/IAPD:/app/Data iapd:latest

# Low memory configuration (1GB)
docker run -e JAVA_OPTS='-Xmx1g -Xms256m' \
  -v ~/Work/IAPD:/app/Data iapd:latest
```

## Docker Compose Usage

### Basic Usage

```bash
# Start the container
docker-compose up iapd

# Start in background
docker-compose up -d iapd

# View logs
docker-compose logs -f iapd

# Stop the container
docker-compose down
```

### Development Mode

```bash
# Run development configuration (limited processing)
docker-compose --profile dev up iapd-dev

# Override command for development
docker-compose run --rm iapd --index-limit 10 --verbose
```

### Custom Commands

```bash
# Run with custom arguments
docker-compose run --rm iapd --help

# Run incremental mode
docker-compose run --rm iapd --incremental --baseline-file /app/Data/Output/IAPD_Data.csv

# Run with force restart
docker-compose run --rm iapd --force-restart
```

## Troubleshooting

### Common Issues

1. **Permission Denied Errors**
   ```bash
   # Fix permissions on the data directory
   sudo chown -R $(id -u):$(id -g) ~/Work/IAPD
   ```

2. **Out of Memory Errors**
   ```bash
   # Increase memory allocation
   docker run -e JAVA_OPTS='-Xmx6g -Xms2g' -v ~/Work/IAPD:/app/Data iapd:latest
   ```

3. **Data Directory Not Found**
   ```bash
   # Create the data directory if it doesn't exist
   mkdir -p ~/Work/IAPD/{Downloads,Output,Input,FirmFiles,Logs}
   ```

### Debugging

```bash
# Run container interactively
docker run -it --entrypoint /bin/bash -v ~/Work/IAPD:/app/Data iapd:latest

# Check container logs
docker logs <container-id>

# Inspect the container
docker inspect iapd:latest
```

## Performance Tuning

### Memory Settings

- **Small datasets (< 1000 firms)**: `-Xmx2g -Xms512m`
- **Medium datasets (1000-5000 firms)**: `-Xmx4g -Xms1g`
- **Large datasets (> 5000 firms)**: `-Xmx8g -Xms2g`

### Rate Limiting

```bash
# Limit API calls and downloads
docker run -v ~/Work/IAPD:/app/Data iapd:latest \
  --url-rate 2 \
  --download-rate 5
```

## Building from Source

```bash
# Build with custom tag
docker build -t iapd:v1.0.0 .

# Build with build arguments
docker build --build-arg JAVA_VERSION=21 -t iapd:latest .

# Build without cache
docker build --no-cache -t iapd:latest .
```

## Security Considerations

- The container runs as a non-root user (`iapd`) for security
- Only necessary ports are exposed
- Sensitive data should be stored in the mounted volume, not in the container
- Use environment variables for configuration, not hardcoded values

## Maintenance

### Updating the Container

```bash
# Pull latest base images and rebuild
docker-compose build --pull

# Remove old images
docker image prune -f
```

### Cleanup

```bash
# Remove stopped containers
docker container prune -f

# Remove unused images
docker image prune -f

# Remove unused volumes
docker volume prune -f
```

## Support

For issues related to:
- **Docker configuration**: Check this README and Docker logs
- **IAPD application**: Run `docker run -v ~/Work/IAPD:/app/Data iapd:latest --help`
- **Performance**: Adjust memory settings and rate limits as described above

## Version Information

- **Container Version**: 1.0.0
- **Java Version**: OpenJDK 21
- **Base Image**: openjdk:21-jdk-slim
- **IAPD Version**: 1.0.0-SNAPSHOT
