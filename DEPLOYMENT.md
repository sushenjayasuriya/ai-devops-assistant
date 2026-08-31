# Production Deployment Guide

## Container Deployment via Docker Compose

### Start the Full Production Stack
```bash
docker-compose up -d --build
```

The stack provisions:
- `devops-postgres`: PostgreSQL 16 on port 5432
- `devops-redis`: Redis 7.2 on port 6379
- `devops-prometheus`: Prometheus Server on port 9090
- `devops-backend`: Spring Boot 3 Java 21 backend on port 8080
- `devops-frontend`: Nginx + React 18 production bundle on port 80

## Production Hardening
1. **Secrets**: Inject secrets (`APP_JWT_SECRET`, `DATABASE_PASSWORD`, integration credentials) via environment variables or cloud secret managers (AWS Secrets Manager / HashiCorp Vault).
2. **TLS / HTTPS**: Terminate TLS at the Ingress controller or reverse proxy.
3. **Non-Root Containers**: Docker images run under non-root unprivileged users (`appuser` / `nginx`).
