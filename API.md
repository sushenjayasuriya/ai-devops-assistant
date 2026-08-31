# REST API Documentation

## Base URL
`/api/v1`

## OpenAPI / Swagger UI
Interactive documentation is available at:
`http://localhost:8080/swagger-ui.html`

## Key Endpoints

### Authentication
- `POST /auth/login` - Authenticate with email and password
- `POST /auth/refresh` - Refresh JWT access token
- `GET /auth/me` - Get current authenticated user profile

### Infrastructure & Topology
- `GET /environments` - List environments
- `GET /servers` - List Linux servers
- `GET /servers/{id}/metrics` - Live server CPU, RAM, and load metrics
- `GET /containers` - List Docker containers
- `GET /containers/{id}/logs` - Tail container logs
- `POST /containers/{id}/actions` - Execute restart/stop/start (Production requires approval)
- `GET /deployments` - List CI/CD release history

### Observability & Metrics
- `GET /metrics/prometheus/query` - Execute PromQL expression
- `GET /metrics/prometheus/targets` - Scrape target health

### Incident Management
- `GET /incidents` - List active and resolved incidents
- `GET /incidents/{id}/events` - Chronological anomaly event stream
- `PATCH /incidents/{id}/status` - Update incident status
- `GET /incidents/{id}/investigation` - Correlated deployment and metric root-cause analysis

### AI SRE Copilot
- `GET /ai/tools` - List registered tools and parameter schemas
- `POST /ai/chat` - Send prompt to AI Agent (triggers ReAct loop & FOIR verification)
- `GET /ai/conversations` - List conversation history

### Human-in-the-Loop Approvals
- `GET /approvals` - List pending approval requests
- `POST /approvals/{id}/resolve` - Approve or reject remediation action

### Audit Trail
- `GET /audit` - Query immutable audit logs
