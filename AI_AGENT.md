# AI Agent & Safety Architecture

## ReAct Orchestration Engine
The AI Agent operates on a multi-step **Reason + Act (ReAct)** execution model:
1. **Context Ingestion**: Loads environment boundaries, active incidents, and recent releases.
2. **Tool Selection**: Selects matching tools from the strongly-typed registry (`get_server_status`, `query_prometheus`, `get_container_status`, `get_container_logs`, etc.).
3. **Execution & Evidence Gathering**: Executes telemetry queries safely via integration adapters.
4. **FOIR Structuring**: Synthesizes the results into Facts, Observations, Inferences, and Recommendations.
5. **Guardrail Sanitization**: Verifies that facts strictly match telemetry responses.

## Fact-Observation-Inference-Recommendation (FOIR) Model

| Category | Definition | Example |
| :--- | :--- | :--- |
| **FACT** | Verifiable data returned by a tool query | `Prometheus metric container_cpu_usage_percent reports 94.2% CPU.` |
| **OBSERVATION** | System trends and behavioral patterns | `CPU spiked immediately following release v3.6.2-patch184.` |
| **INFERENCE** | Causal hypothesis deduced by AI | `HikariPool connection timeout indicates database thread deadlock.` |
| **RECOMMENDATION** | Remediation action with risk level & approval | `Restart container thingsboard-core-app (HIGH_RISK in Production).` |

## Tool Risk Classification

| Risk Level | Description | Production Approval Required | Allowed Role |
| :--- | :--- | :--- | :--- |
| `READ_ONLY` | Queries metrics, inspects status, reads logs | No | `VIEWER`, `DEVOPS_ENGINEER`, `ADMIN` |
| `LOW_RISK` | Starts development container | No in Dev, Yes in Prod | `DEVOPS_ENGINEER`, `ADMIN` |
| `MEDIUM_RISK` | Restarts staging/production container | **YES in Production** | `DEVOPS_ENGINEER`, `ADMIN` |
| `HIGH_RISK` | Stops production container, rolls back release | **YES** | `DEVOPS_ENGINEER`, `ADMIN` |
| `CRITICAL` | Resource deletion, database drop | **BLOCKED FOR AI (Manual Only)** | `ADMIN` |
