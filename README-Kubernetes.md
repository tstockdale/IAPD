# IAPD Kubernetes Deployment Guide

This guide covers the deployment and management of the Investment Adviser Public Disclosure (IAPD) Parser application on Kubernetes using Minikube.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Architecture Overview](#architecture-overview)
- [Deployment](#deployment)
- [Job Management](#job-management)
- [Monitoring](#monitoring)
- [Configuration](#configuration)
- [Troubleshooting](#troubleshooting)
- [Advanced Usage](#advanced-usage)

## Prerequisites

### Required Software

1. **Docker** - For building container images
2. **Minikube** - Local Kubernetes cluster
3. **kubectl** - Kubernetes command-line tool

### Installation Commands

```bash
# Install Minikube (macOS with Homebrew)
brew install minikube

# Install kubectl (if not already installed)
brew install kubernetes-cli

# Verify installations
minikube version
kubectl version --client
```

### System Requirements

- **Memory**: 4GB+ available for Minikube
- **CPU**: 2+ cores recommended
- **Disk**: 10GB+ free space
- **Docker**: Running and accessible

## Quick Start

### 1. Start Minikube

```bash
minikube start --memory=4096 --cpus=2
minikube addons enable metrics-server
```

### 2. Deploy IAPD Application

```bash
# Make deployment script executable
chmod +x scripts/deploy-to-k8s.sh

# Deploy everything (application + monitoring)
./scripts/deploy-to-k8s.sh

# Or deploy without monitoring
./scripts/deploy-to-k8s.sh --skip-monitoring
```

### 3. Run Your First Job

```bash
# Make job management script executable
chmod +x scripts/manage-jobs.sh

# Run a job with default settings
./scripts/manage-jobs.sh run

# Run a job with custom arguments
./scripts/manage-jobs.sh run my-job --index-limit 100 --verbose
```

### 4. Monitor the Job

```bash
# Check job status
./scripts/manage-jobs.sh list

# View job logs
./scripts/manage-jobs.sh logs <job-name>

# Access monitoring dashboards
minikube service list
```

## Architecture Overview

### Components

```
┌─────────────────────────────────────────────────────────────┐
│                    Kubernetes Cluster                       │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │    IAPD     │  │ Monitoring  │  │     Storage         │  │
│  │ Namespace   │  │ Namespace   │  │                     │  │
│  │             │  │             │  │                     │  │
│  │ ┌─────────┐ │  │ ┌─────────┐ │  │ ┌─────────────────┐ │  │
│  │ │  Jobs   │ │  │ │Prometheus│ │  │ │ Persistent      │ │  │
│  │ │CronJobs │ │  │ │ Grafana │ │  │ │ Volume          │ │  │
│  │ │ConfigMap│ │  │ └─────────┘ │  │ │ (Data Storage)  │ │  │
│  │ └─────────┘ │  └─────────────┘  │ └─────────────────┘ │  │
│  └─────────────┘                   └─────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### Key Resources

- **Namespace**: `iapd` - Isolated environment for IAPD resources
- **PersistentVolume**: 10GB storage for data persistence
- **ConfigMap**: Application configuration and environment variables
- **Job**: Batch processing workload
- **CronJob**: Scheduled batch processing
- **Monitoring**: Prometheus + Grafana stack

## Deployment

### Deployment Script Options

```bash
# Full deployment
./scripts/deploy-to-k8s.sh

# Skip Docker image rebuild
./scripts/deploy-to-k8s.sh --skip-build

# Skip monitoring deployment
./scripts/deploy-to-k8s.sh --skip-monitoring

# Show help
./scripts/deploy-to-k8s.sh --help
```

### Manual Deployment Steps

If you prefer manual deployment:

```bash
# 1. Build and load Docker image
docker build -t iapd:latest .
minikube image load iapd:latest

# 2. Deploy Kubernetes resources
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/persistent-volume.yaml
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/cronjob.yaml

# 3. Deploy monitoring (optional)
kubectl apply -f k8s/monitoring/prometheus.yaml
kubectl apply -f k8s/monitoring/grafana.yaml
```

## Job Management

### Using the Management Script

```bash
# Run a new job
./scripts/manage-jobs.sh run [job-name] [args...]

# List all jobs
./scripts/manage-jobs.sh list

# Show job status
./scripts/manage-jobs.sh status <job-name>

# View job logs
./scripts/manage-jobs.sh logs <job-name>

# Delete a job
./scripts/manage-jobs.sh delete <job-name>

# Clean up completed jobs
./scripts/manage-jobs.sh cleanup

# Enable/disable scheduled jobs
./scripts/manage-jobs.sh enable-schedule
./scripts/manage-jobs.sh disable-schedule
```

### Manual Job Management

```bash
# Create a job from the CronJob template
kubectl create job iapd-manual-job --from=cronjob/iapd-scheduled-job -n iapd

# List jobs
kubectl get jobs -n iapd

# View job details
kubectl describe job <job-name> -n iapd

# View job logs
kubectl logs -f job/<job-name> -n iapd

# Delete a job
kubectl delete job <job-name> -n iapd
```

### Job Arguments

You can customize job behavior with command-line arguments:

```bash
# Common arguments
--verbose                    # Enable verbose logging
--index-limit 1000          # Limit number of firms to process
--incremental               # Run in incremental mode
--baseline-file <path>      # Specify baseline file for incremental mode

# Example job with custom arguments
./scripts/manage-jobs.sh run test-job --index-limit 100 --verbose
```

## Monitoring

### Access Monitoring Services

```bash
# Get Minikube IP
minikube ip

# Access services
# Prometheus: http://<minikube-ip>:30000
# Grafana: http://<minikube-ip>:30001
```

### Grafana Dashboards

**Default Login**: admin/admin123

**Available Dashboards**:
- **Kubernetes Cluster Monitoring**: Overall cluster metrics
- **IAPD Job Monitoring**: Job-specific metrics and status

### Prometheus Metrics

Key metrics to monitor:
- `kube_job_status_succeeded{namespace="iapd"}` - Successful jobs
- `kube_job_status_failed{namespace="iapd"}` - Failed jobs
- `kube_job_status_active{namespace="iapd"}` - Active jobs
- `container_cpu_usage_seconds_total{namespace="iapd"}` - CPU usage
- `container_memory_usage_bytes{namespace="iapd"}` - Memory usage

## Configuration

### Environment Variables (ConfigMap)

```yaml
JAVA_OPTS: "-Xmx4g -Xms1g -XX:+UseG1GC -XX:+UseStringDeduplication"
LOG_LEVEL: "INFO"
LOG_PATH: "/app/Data/Logs"
PROCESSING_MODE: "batch"
DEFAULT_ARGS: "--verbose"
```

### Resource Limits

```yaml
resources:
  requests:
    memory: "1Gi"
    cpu: "500m"
  limits:
    memory: "4Gi"
    cpu: "2000m"
```

### Scheduled Jobs

The CronJob is configured to run daily at 2 AM UTC but starts suspended:

```bash
# Enable scheduled jobs
kubectl patch cronjob iapd-scheduled-job -n iapd -p '{"spec":{"suspend":false}}'

# Modify schedule (example: every 6 hours)
kubectl patch cronjob iapd-scheduled-job -n iapd -p '{"spec":{"schedule":"0 */6 * * *"}}'
```

## Troubleshooting

### Common Issues

#### 1. Image Pull Errors

```bash
# Ensure image is loaded in Minikube
minikube image load iapd:latest

# Verify image exists
minikube image ls | grep iapd
```

#### 2. Persistent Volume Issues

```bash
# Check PV/PVC status
kubectl get pv,pvc -n iapd

# If PVC is pending, check events
kubectl describe pvc iapd-data-pvc -n iapd
```

#### 3. Job Failures

```bash
# Check job status
kubectl describe job <job-name> -n iapd

# Check pod logs
kubectl logs <pod-name> -n iapd

# Check events
kubectl get events -n iapd --sort-by='.lastTimestamp'
```

#### 4. Resource Constraints

```bash
# Check node resources
kubectl top nodes

# Check pod resources
kubectl top pods -n iapd

# Adjust resource limits in job.yaml if needed
```

### Debugging Commands

```bash
# Get all resources in IAPD namespace
kubectl get all -n iapd

# Describe all resources
kubectl describe all -n iapd

# Check cluster events
kubectl get events --all-namespaces --sort-by='.lastTimestamp'

# Access pod shell (if running)
kubectl exec -it <pod-name> -n iapd -- /bin/bash
```

## Advanced Usage

### Custom Job Templates

Create custom job configurations:

```bash
# Generate job template
kubectl create job my-custom-job --from=cronjob/iapd-scheduled-job -n iapd --dry-run=client -o yaml > custom-job.yaml

# Edit the template
# Apply custom job
kubectl apply -f custom-job.yaml
```

### Scaling and Performance

```bash
# Increase Minikube resources
minikube stop
minikube start --memory=8192 --cpus=4

# Adjust job resource limits
kubectl patch cronjob iapd-scheduled-job -n iapd -p '{"spec":{"jobTemplate":{"spec":{"template":{"spec":{"containers":[{"name":"iapd-parser","resources":{"limits":{"memory":"8Gi","cpu":"4000m"}}}]}}}}}}'
```

### Data Backup

```bash
# Access persistent volume data
minikube ssh
sudo ls -la /tmp/iapd-data/

# Copy data from Minikube
minikube cp minikube:/tmp/iapd-data ./backup-data
```

### Multi-Environment Setup

For different environments, create separate namespaces:

```bash
# Create staging environment
kubectl create namespace iapd-staging

# Deploy to staging
sed 's/namespace: iapd/namespace: iapd-staging/g' k8s/*.yaml | kubectl apply -f -
```

## Migration from Docker Compose

### Key Differences

| Docker Compose | Kubernetes |
|----------------|------------|
| `docker-compose up` | `kubectl apply -f k8s/` |
| `docker-compose logs` | `kubectl logs` |
| `docker-compose down` | `kubectl delete` |
| Volume mounts | PersistentVolumes |
| Environment variables | ConfigMaps/Secrets |
| Service discovery | Services |

### Migration Steps

1. **Stop Docker Compose**: `docker-compose down`
2. **Deploy to Kubernetes**: `./scripts/deploy-to-k8s.sh`
3. **Migrate data** (if needed): Copy from Docker volumes to PV
4. **Update monitoring**: Use Kubernetes-native monitoring

## Support and Maintenance

### Regular Maintenance Tasks

```bash
# Clean up completed jobs (weekly)
./scripts/manage-jobs.sh cleanup

# Update Docker image
docker build -t iapd:latest .
minikube image load iapd:latest

# Restart deployments to use new image
kubectl rollout restart deployment/prometheus -n monitoring
kubectl rollout restart deployment/grafana -n monitoring
```

### Backup Strategy

1. **Application Data**: Backup PersistentVolume contents
2. **Configuration**: Version control all YAML files
3. **Monitoring Data**: Export Grafana dashboards

### Monitoring Health

```bash
# Check cluster health
kubectl get nodes
kubectl get pods --all-namespaces

# Check resource usage
kubectl top nodes
kubectl top pods --all-namespaces
```

---

For additional support or questions, refer to the main project documentation or create an issue in the project repository.
