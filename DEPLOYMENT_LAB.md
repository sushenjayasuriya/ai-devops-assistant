# AI DevOps Assistant — Low-Memory Deployment & Lab Verification Guide

## 1. Overview & Architecture

The **AI DevOps Assistant** is an agentic SRE and infrastructure operations platform. This guide specifies the deployment procedure for **resource-constrained environments (e.g. 1 GB RAM cloud VMs / t3.micro / e2-micro instances)**.

### Constrained Lab Topology (1 GB RAM Footprint)
```
+-------------------------------------------------------------------------------+
|                           Application VM (1 GB RAM)                           |
|                                                                               |
|  [Host Linux OS + 2 GB Swap Space]                                            |
|                                                                               |
|  +--------------------+   +-------------------+   +------------------------+  |
|  | Nginx / Frontend   |   | PostgreSQL 16     |   | Redis 7.2 Cache        |  |
|  | Mem: ~20 MB        |   | Mem Limit: 128 MB |   | Mem Limit: 48 MB       |  |
|  +--------------------+   +-------------------+   +------------------------+  |
|            |                        |                          |              |
|            +------------------------+--------------------------+              |
|                                     |                                         |
|                       +----------------------------+                          |
|                       | Spring Boot 3.3 Backend    |                          |
|                       | Java 21 (SerialGC)         |                          |
|                       | Heap: -Xms128m -Xmx384m    |                          |
|                       | Mem Limit: 512 MB          |                          |
|                       +----------------------------+                          |
+-------------------------------------------------------------------------------+
                                      |
                     [Encrypted Network Integrations]
                                      |
       +------------------------------+-------------------------------+
       |                              |                               |
+--------------+              +---------------+               +---------------+
| External     |              | External      |               | External      |
| Prometheus   |              | Docker Host   |               | Kubernetes    |
| Server       |              | (TCP/Socket)  |               | Cluster       |
| :9090        |              | :2375         |               | (Fabric8/K8s) |
+--------------+              +---------------+               +---------------+
```

---

## 2. VM Resource Budget & Memory Sizing

| Component | Minimum Reservation | Maximum Memory Limit | Configuration Highlights |
| :--- | :--- | :--- | :--- |
| **Linux OS & Page Cache** | 128 MB | ~200 MB | Kernel, sshd, systemd |
| **PostgreSQL 16** | 48 MB | 128 MB | `shared_buffers=32MB`, `max_connections=30`, `work_mem=2MB` |
| **Redis 7.2** | 16 MB | 48 MB | `--maxmemory 32mb --maxmemory-policy allkeys-lru --save ""` |
| **Spring Boot Backend** | 192 MB | 512 MB | `-Xms128m -Xmx384m -XX:+UseSerialGC`, Hikari pool max=8 |
| **Nginx Frontend** | 16 MB | 48 MB | Alpine static server + API/WebSocket reverse proxy |
| **Total Peak Budget** | **400 MB** | **~936 MB** | **Fits within 1 GB RAM (Safe buffer < 1024 MB)** |

---

## 3. Host Swap Setup (Mandatory for 1 GB RAM)

Before starting the containers, configure a 2 GB swapfile on the target Linux host to absorb memory bursts during Spring Boot startup and Flyway migration execution:

```bash
# 1. Allocate a 2 GB swapfile
sudo fallocate -l 2G /swapfile || sudo dd if=/dev/zero of=/swapfile bs=1M count=2048

# 2. Secure file permissions
sudo chmod 600 /swapfile

# 3. Format as swap space
sudo mkswap /swapfile

# 4. Activate swap
sudo swapon /swapfile

# 5. Persist across reboots
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab

# 6. Optimize swappiness (prioritize physical RAM, swap only on demand)
sudo sysctl vm.swappiness=10
echo 'vm.swappiness=10' | sudo tee -a /etc/sysctl.conf

# 7. Verify active swap
free -h
```

---

## 4. Environment Variables (`.env`)

Create `.env` in the project root:

```ini
# ==============================================================================
# LAB DEPLOYMENT CONFIGURATION (.env)
# ==============================================================================

# Application Profile
SPRING_PROFILES_ACTIVE=lab

# PostgreSQL Configuration
DATABASE_URL=jdbc:postgresql://postgres:5432/devopsdb
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=devopspassword
POSTGRES_DB=devopsdb

# Redis Cache Configuration
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_PASSWORD=

# Cryptographic Keys (256-bit AES & HMAC-SHA256)
JWT_SECRET=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
APP_ENCRYPTION_KEY=W3u+9sX/5J2v8N1q4Y7z0A3c6E9g2I5l8O1r4U7x0A3=

# CORS Allowed Origins
CORS_ALLOWED_ORIGINS=http://localhost,http://localhost:80,http://YOUR_VM_PUBLIC_IP

# External Infrastructure Integrations (Defaults)
PROMETHEUS_URL=http://prometheus.external.local:9090
DOCKER_HOST=tcp://docker.external.local:2375
```

---

## 5. Startup & Deployment Commands

### Build & Start Stack
```bash
# 1. Build and start low-memory container stack
docker compose -f docker-compose.dev.yml up -d --build

# 2. Inspect container status
docker compose -f docker-compose.dev.yml ps

# 3. Monitor live memory and CPU consumption
docker stats
```

### Shutdown & Clean Up
```bash
# Graceful shutdown
docker compose -f docker-compose.dev.yml down

# Full clean up (including database volumes)
docker compose -f docker-compose.dev.yml down -v
```

---

## 6. Accessing the Application

- **Web Dashboard**: `http://<VM_IP>/`
- **Backend API**: `http://<VM_IP>:8080/api/v1`
- **Swagger / OpenAPI Specs**: `http://<VM_IP>:8080/swagger-ui/index.html`
- **Health Endpoint**: `http://<VM_IP>:8080/actuator/health`

### Initial Admin Credentials
- **Email**: `admin@devops.ai`
- **Password**: `DevopsAdmin2026!`

---

## 7. Connecting External Infrastructure

All integrations run **outside** the application VM to preserve RAM. Use the Web UI at `/integrations` or the REST API:

### 1. Prometheus (External Server)
```http
POST /api/v1/integrations
Content-Type: application/json
Authorization: Bearer <JWT_TOKEN>

{
  "environmentId": "<ENV_UUID>",
  "type": "PROMETHEUS",
  "name": "Cloud Observability Prometheus",
  "endpointUrl": "http://10.0.1.50:9090",
  "authType": "BEARER_TOKEN",
  "configRaw": "prom-access-token-xyz",
  "timeoutMs": 5000
}
```

### 2. Docker Engine (External Remote Host)
Configure `/etc/docker/daemon.json` on the remote Docker node to listen on TCP `tcp://0.0.0.0:2375`, then register:
```http
POST /api/v1/integrations
Content-Type: application/json
Authorization: Bearer <JWT_TOKEN>

{
  "environmentId": "<ENV_UUID>",
  "type": "DOCKER",
  "name": "Worker Node Docker Daemon",
  "endpointUrl": "tcp://10.0.1.60:2375",
  "authType": "NONE",
  "timeoutMs": 5000
}
```

### 3. Linux SSH Host
```http
POST /api/v1/integrations
Content-Type: application/json
Authorization: Bearer <JWT_TOKEN>

{
  "environmentId": "<ENV_UUID>",
  "type": "LINUX_SSH",
  "name": "Database Host Node",
  "endpointUrl": "10.0.1.70:22",
  "authType": "SSH_KEY",
  "configRaw": "{\"user\":\"devops\",\"privateKey\":\"-----BEGIN RSA PRIVATE KEY-----\\n...\"}",
  "timeoutMs": 5000
}
```

### 4. Kubernetes Cluster (Fabric8)
```http
POST /api/v1/integrations
Content-Type: application/json
Authorization: Bearer <JWT_TOKEN>

{
  "environmentId": "<ENV_UUID>",
  "type": "KUBERNETES",
  "name": "Production EKS / GKE Cluster",
  "endpointUrl": "https://k8s-api.example.com:6443",
  "authType": "KUBECONFIG",
  "configRaw": "apiVersion: v1\nclusters:\n- cluster: ...\n",
  "timeoutMs": 8000
}
```

---

## 8. Verification Matrix & Security Sign-Off

| Verification Item | Automated Test | Real Infrastructure Verification | Result |
| :--- | :--- | :--- | :--- |
| **Authentication & Token Rotation** | `AuthServiceTest` | Web Login / Token Refresh | **PASSED** |
| **Tenant Isolation Boundary** | `TenantIsolationTest` | Cross-tenant API queries blocked | **PASSED** |
| **Zero-Bypass Production Approval** | `ContainerApprovalTest` | Mutating actions produce `APPROVAL_REQUIRED` | **PASSED** |
| **Approval Lifecycle & Replay** | `ApprovalWorkflowTest` | Expired / double-approval rejected | **PASSED** |
| **Prometheus PromQL Range & Instant** | `PrometheusHttpClientTest` | Live metric queries (`up`, `process_cpu_usage`) | **PASSED** |
| **Docker Stats & Log Multiplexing** | `DockerEngineClientTest` | Live CPU/Memory delta calculations | **PASSED** |
| **Linux SSH Parsers & Injection Defense** | `LinuxSshClientTest` | Arbitrary shell commands rejected (`rm -rf /`) | **PASSED** |
| **Kubernetes Fabric8 Integration** | `KubernetesClientServiceTest` | Cluster version & Rollout restart | **PASSED** |
| **SSRF Metadata Protection** | `PrometheusHttpClientTest` | Blocked `169.254.169.254` | **PASSED** |
| **1 GB RAM Profile Compatibility** | `docker-compose.dev.yml` | Strict 936MB peak footprint | **PASSED** |

---

## 9. Troubleshooting & Diagnostics

1. **Spring Boot Container Exited (Exit Code 137)**:
   - Indicates out-of-memory. Confirm 2 GB swapfile is active (`free -h`).
   - Check `JAVA_TOOL_OPTIONS`: Ensure `-Xmx384m -XX:+UseSerialGC` is present.
2. **PostgreSQL Connection Refused**:
   - Verify `pg_isready -U postgres -d devopsdb` inside the postgres container.
3. **Integration Timeout**:
   - Check firewall and network security group between the application VM and external target host ports (`9090`, `2375`, `22`, `6443`).
