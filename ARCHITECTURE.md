# System Architecture

## Architectural Philosophy
The AI DevOps Assistant is designed according to three core tenets:
1. **Zero Trust & Tool Determinism**: The LLM is never given direct shell access, raw network sockets, or unrestricted query endpoints. Every infrastructure interaction is mediated by a strongly-typed tool adapter with predefined schemas and RBAC authorization.
2. **Human-in-the-Loop Mutation Safety**: Actions categorized as `MEDIUM_RISK`, `HIGH_RISK`, or `CRITICAL` on production environments trigger an asynchronous approval workflow and require explicit operator confirmation before execution.
3. **Fact-First Telemetry Guardrails**: Telemetry output returned to the operator is validated against actual tool execution results, distinguishing between **FACT**, **OBSERVATION**, **INFERENCE**, and **RECOMMENDATION**.

```mermaid
graph TD
    Client[React 18 Dashboard] -->|REST / WebSocket| Gateway[Spring Boot 3 API Gateway]
    Gateway --> SecurityFilter[JWT & RBAC Security Filter]
    SecurityFilter --> RateLimiter[Rate Limiter & Correlation MDC]
    
    subgraph Core Engine
        AuthService[Authentication Service]
        InfraService[Infrastructure Management]
        IncidentEngine[Incident Detection & Correlation Engine]
        ApprovalEngine[Approval Workflow Engine]
        AuditService[Immutable Audit Logger]
    end

    subgraph AI Framework
        Orchestrator[Agent Orchestrator ReAct Loop]
        Guardrail[FOIR Guardrail Verification Engine]
        ToolRegistry[Strongly Typed Tool Registry]
    end

    subgraph Adapters
        PromAdapter[Prometheus PromQL Adapter]
        DockerAdapter[Docker Engine Adapter]
        LinuxAdapter[Linux SSH Host Collector]
        K8sAdapter[Kubernetes Client Adapter]
    end

    SecurityFilter --> Core Engine
    SecurityFilter --> AI Framework
    Orchestrator --> ToolRegistry
    ToolRegistry --> Adapters
    Orchestrator --> Guardrail
    ToolRegistry -.->|Mutation Check| ApprovalEngine
    ApprovalEngine --> AuditService
    AuditService --> DB[(PostgreSQL 16)]
```
