# Infrastructure Integrations

## Supported Connectors

### 1. Prometheus
- **Purpose**: Telemetry scraping, PromQL execution, time-series anomaly detection.
- **Tools**: `query_prometheus`, `get_prometheus_targets`.
- **Security**: Controlled PromQL execution with parameterized range limits.

### 2. Docker
- **Purpose**: Local and remote Docker daemon monitoring and container lifecycle operations.
- **Tools**: `get_container_status`, `get_container_metrics`, `get_container_logs`, `restart_container`, `stop_container`, `start_container`.
- **Security**: Container actions gated behind human approvals when targeted at `PRODUCTION`.

### 3. Linux Server (SSH)
- **Purpose**: Host OS metrics, memory fragmentation, disk capacity, and load average inspection.
- **Tools**: `get_server_status`, `get_server_metrics`.
- **Security**: Strict allowlist execution (`uptime`, `vmstat`, `df`, `free`, `ps`).

### 4. Kubernetes
- **Purpose**: Pod status, namespaces, deployment replicas, and restart loops.
- **Tools**: Pod mesh inspector, crashloop backtrace extractor.

### 5. CI/CD (Jenkins / GitHub)
- **Purpose**: Correlation of releases, commits, authors, and changelogs with telemetry anomalies.
- **Tools**: `get_recent_deployments`.
