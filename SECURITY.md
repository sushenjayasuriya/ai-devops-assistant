# Security Architecture & Guardrails

## Authentication & Session Management
- **Stateless JWT**: Short-lived Access Tokens (15m expiry) signed with HMAC-SHA256.
- **Refresh Token Rotation**: Refresh tokens (7-day lifetime) used for transparent renewal.
- **Password Hashing**: BCrypt with work factor 12.

## Role-Based Access Control (RBAC)
- `ADMIN`: Full administrative control, user provisioning, integration management, and critical action resolution.
- `DEVOPS_ENGINEER`: Incident remediation, container restarts, log inspection, and approval resolution.
- `VIEWER`: Read-only queries for metrics, status, logs, and AI chats.

## Linux Host Execution Allowlist
Arbitrary shell execution over SSH is forbidden. The host collector enforces an allowlist:
- `uptime`
- `vmstat`
- `df`
- `free`
- `top`
- `ps`
- `cat /proc/loadavg`

Any attempt by an LLM or user to inject arbitrary shell commands (e.g. `rm -rf`, `curl | bash`, `chmod`) is rejected with an `UnauthorizedActionException` and audited.

## Immutable Audit Trail
Every mutating operation, AI tool execution, and approval resolution generates an immutable record in `audit_logs` storing:
- `correlationId`: Distributed trace ID propagated in MDC and HTTP response headers.
- `action`: Specific operation name.
- `targetResourceType` & `targetResourceId`.
- `environmentName`: `DEVELOPMENT`, `STAGING`, `PRODUCTION`.
- `riskLevel`: `READ_ONLY`, `LOW_RISK`, `MEDIUM_RISK`, `HIGH_RISK`, `CRITICAL`.
- `status`: `SUCCESS`, `FAILURE`, `REJECTED`.
- `user`: Authenticated operator email or SRE Orchestrator.
- `timestamp`: UTC timestamp.
