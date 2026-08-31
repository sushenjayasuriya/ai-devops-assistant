-- Flyway Migration: V1 Initial Schema for AI DevOps Assistant

CREATE TABLE organizations (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE users (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    last_login_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE roles (
    name VARCHAR(50) PRIMARY KEY,
    description VARCHAR(255)
);

CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_name VARCHAR(50) NOT NULL REFERENCES roles(name) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_name)
);

CREATE TABLE environments (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    name VARCHAR(50) NOT NULL, -- DEVELOPMENT, STAGING, PRODUCTION
    description VARCHAR(255),
    is_production BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE servers (
    id UUID PRIMARY KEY,
    environment_id UUID NOT NULL REFERENCES environments(id) ON DELETE CASCADE,
    hostname VARCHAR(255) NOT NULL,
    ip_address VARCHAR(100) NOT NULL,
    os_info VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'ONLINE', -- ONLINE, UNREACHABLE, DEGRADED
    metadata TEXT,
    last_seen_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE clusters (
    id UUID PRIMARY KEY,
    environment_id UUID NOT NULL REFERENCES environments(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    provider VARCHAR(50) NOT NULL, -- KUBERNETES, PROXMOX, DOCKER_SWARM
    endpoint_url VARCHAR(500),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE nodes (
    id UUID PRIMARY KEY,
    cluster_id UUID NOT NULL REFERENCES clusters(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'WORKER',
    status VARCHAR(50) NOT NULL DEFAULT 'READY',
    capacity TEXT,
    allocatable TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE containers (
    id UUID PRIMARY KEY,
    server_id UUID REFERENCES servers(id) ON DELETE SET NULL,
    environment_id UUID NOT NULL REFERENCES environments(id) ON DELETE CASCADE,
    container_id VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    image VARCHAR(255) NOT NULL,
    state VARCHAR(50) NOT NULL, -- RUNNING, EXITED, RESTARTING, DEAD
    restart_count INT NOT NULL DEFAULT 0,
    port_mappings TEXT,
    started_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE deployments (
    id UUID PRIMARY KEY,
    environment_id UUID NOT NULL REFERENCES environments(id) ON DELETE CASCADE,
    service_name VARCHAR(255) NOT NULL,
    version_tag VARCHAR(100) NOT NULL,
    commit_sha VARCHAR(100),
    deployed_by VARCHAR(255),
    status VARCHAR(50) NOT NULL, -- SUCCESS, FAILED, IN_PROGRESS, ROLLED_BACK
    changelog TEXT,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE integrations (
    id UUID PRIMARY KEY,
    environment_id UUID NOT NULL REFERENCES environments(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL, -- PROMETHEUS, DOCKER, LINUX_SSH, K8S, LOKI, JENKINS, GITHUB
    name VARCHAR(255) NOT NULL,
    endpoint_url VARCHAR(500) NOT NULL,
    config_encrypted TEXT,
    health_status VARCHAR(50) NOT NULL DEFAULT 'HEALTHY',
    last_synced_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE incidents (
    id UUID PRIMARY KEY,
    environment_id UUID NOT NULL REFERENCES environments(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    severity VARCHAR(50) NOT NULL, -- INFO, LOW, MEDIUM, HIGH, CRITICAL
    status VARCHAR(50) NOT NULL DEFAULT 'OPEN', -- OPEN, INVESTIGATING, MITIGATED, RESOLVED, CLOSED
    affected_resource_type VARCHAR(100),
    affected_resource_id VARCHAR(255),
    root_cause_deployment_id UUID REFERENCES deployments(id) ON DELETE SET NULL,
    root_cause_summary TEXT,
    confidence_score DOUBLE PRECISION,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    resolved_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE incident_events (
    id UUID PRIMARY KEY,
    incident_id UUID NOT NULL REFERENCES incidents(id) ON DELETE CASCADE,
    event_type VARCHAR(100) NOT NULL,
    message TEXT NOT NULL,
    payload TEXT,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE metric_snapshots (
    id UUID PRIMARY KEY,
    incident_id UUID REFERENCES incidents(id) ON DELETE SET NULL,
    metric_name VARCHAR(255) NOT NULL,
    resource_identifier VARCHAR(255) NOT NULL,
    time_series_data TEXT NOT NULL,
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE ai_conversations (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    environment_id UUID REFERENCES environments(id) ON DELETE SET NULL,
    incident_id UUID REFERENCES incidents(id) ON DELETE SET NULL,
    title VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE ai_messages (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL REFERENCES ai_conversations(id) ON DELETE CASCADE,
    sender VARCHAR(20) NOT NULL, -- USER, AI, SYSTEM
    raw_prompt TEXT,
    content TEXT NOT NULL,
    facts TEXT,
    observations TEXT,
    inferences TEXT,
    recommendations TEXT,
    tool_calls TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE ai_actions (
    id UUID PRIMARY KEY,
    message_id UUID NOT NULL REFERENCES ai_messages(id) ON DELETE CASCADE,
    tool_name VARCHAR(100) NOT NULL,
    tool_parameters TEXT NOT NULL,
    risk_level VARCHAR(50) NOT NULL, -- READ_ONLY, LOW_RISK, MEDIUM_RISK, HIGH_RISK, CRITICAL
    execution_status VARCHAR(50) NOT NULL DEFAULT 'PENDING_APPROVAL', -- PENDING_APPROVAL, APPROVED, REJECTED, EXECUTED, FAILED
    execution_result TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    executed_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE approval_requests (
    id UUID PRIMARY KEY,
    ai_action_id UUID NOT NULL REFERENCES ai_actions(id) ON DELETE CASCADE,
    environment_id UUID NOT NULL REFERENCES environments(id) ON DELETE CASCADE,
    requested_by_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    resolved_by_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    action_type VARCHAR(100) NOT NULL,
    rationale TEXT NOT NULL,
    expected_impact TEXT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- PENDING, APPROVED, REJECTED, EXPIRED
    requested_at TIMESTAMP WITH TIME ZONE NOT NULL,
    resolved_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    correlation_id VARCHAR(100) NOT NULL,
    action VARCHAR(255) NOT NULL,
    target_resource_type VARCHAR(100),
    target_resource_id VARCHAR(255),
    environment_name VARCHAR(50),
    risk_level VARCHAR(50),
    sanitized_parameters TEXT,
    status VARCHAR(50) NOT NULL, -- SUCCESS, FAILURE, REJECTED
    error_details TEXT,
    ip_address VARCHAR(100),
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    type VARCHAR(100) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    link_metadata TEXT,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
