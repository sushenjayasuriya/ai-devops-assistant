-- Flyway Migration: V2 Seed Initial Data

-- Roles
INSERT INTO roles (name, description) VALUES
('ADMIN', 'Full system administrative access, user management, and critical infrastructure control'),
('DEVOPS_ENGINEER', 'Operations, incident remediation, metric analysis, and container control'),
('VIEWER', 'Read-only access to infrastructure, metrics, logs, and AI conversations');

-- Default Organization
INSERT INTO organizations (id, name, slug, created_at, updated_at) VALUES
('a0000000-0000-0000-0000-000000000001', 'Acme Global Infrastructure', 'acme-global', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Default Environments
INSERT INTO environments (id, organization_id, name, description, is_production, created_at, updated_at) VALUES
('e0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', 'DEVELOPMENT', 'Internal developer testbed', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('e0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000001', 'STAGING', 'Pre-production staging cluster', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('e0000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000001', 'PRODUCTION', 'Customer-facing live production environment', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Initial Users (Password: Admin@123 for admin, Devops@123 for devops, Viewer@123 for viewer)
-- BCrypt cost 10 hash for 'Admin@123': $2a$10$7z7wD8cI.yB5dJ0o2z/jQOCmK2U1q/7p/Xf6fJ6n3vF7C3.X5p7U. (we'll also seed standardized BCrypt)
INSERT INTO users (id, organization_id, email, password_hash, full_name, enabled, created_at, updated_at) VALUES
('u0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', 'admin@devops.ai', '$2a$12$4bZ1YQZfE3cEw3u4/K3Iqe3Lg2Y2/r2G4B5V2M7Q5b8A2P4c5D6e7', 'Lead Architect & Admin', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('u0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000001', 'devops@devops.ai', '$2a$12$4bZ1YQZfE3cEw3u4/K3Iqe3Lg2Y2/r2G4B5V2M7Q5b8A2P4c5D6e7', 'Senior SRE Engineer', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('u0000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000001', 'viewer@devops.ai', '$2a$12$4bZ1YQZfE3cEw3u4/K3Iqe3Lg2Y2/r2G4B5V2M7Q5b8A2P4c5D6e7', 'Operations Observer', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Assign User Roles
INSERT INTO user_roles (user_id, role_name) VALUES
('u0000000-0000-0000-0000-000000000001', 'ADMIN'),
('u0000000-0000-0000-0000-000000000002', 'DEVOPS_ENGINEER'),
('u0000000-0000-0000-0000-000000000003', 'VIEWER');

-- Seed Host Infrastructure
INSERT INTO servers (id, environment_id, hostname, ip_address, os_info, status, metadata, last_seen_at, created_at, updated_at) VALUES
('s0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000003', 'prod-core-node-01.acme.internal', '10.0.10.15', 'Ubuntu 22.04 LTS (Kernel 5.15.0-91-generic)', 'DEGRADED', '{"cores": 16, "ram_gb": 64, "disk_gb": 500, "region": "us-east-1"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('s0000000-0000-0000-0000-000000000002', 'e0000000-0000-0000-0000-000000000003', 'prod-db-node-01.acme.internal', '10.0.10.20', 'Debian 12 Bookworm (Kernel 6.1.0)', 'ONLINE', '{"cores": 32, "ram_gb": 128, "disk_gb": 2000, "region": "us-east-1"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('s0000000-0000-0000-0000-000000000003', 'e0000000-0000-0000-0000-000000000002', 'staging-cluster-node-01.acme.internal', '10.0.20.10', 'Ubuntu 22.04 LTS', 'ONLINE', '{"cores": 8, "ram_gb": 32, "disk_gb": 250, "region": "us-east-1"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Seed Containers (Simulating ThingsBoard Server Incident Scenario)
INSERT INTO containers (id, server_id, environment_id, container_id, name, image, state, restart_count, port_mappings, started_at, created_at, updated_at) VALUES
('c0000000-0000-0000-0000-000000000001', 's0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000003', 'd9f8e7a6b5c4', 'thingsboard-core-app', 'thingsboard/tb-postgres:3.6.2', 'RESTARTING', 7, '{"8080/tcp": "8080", "1883/tcp": "1883", "5683/udp": "5683"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('c0000000-0000-0000-0000-000000000002', 's0000000-0000-0000-0000-000000000002', 'e0000000-0000-0000-0000-000000000003', 'a1b2c3d4e5f6', 'postgres-production-cluster', 'postgres:16-alpine', 'RUNNING', 0, '{"5432/tcp": "5432"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('c0000000-0000-0000-0000-000000000003', 's0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000003', 'f0e1d2c3b4a5', 'redis-session-cache', 'redis:7.2-alpine', 'RUNNING', 0, '{"6379/tcp": "6379"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Seed Recent Deployments
INSERT INTO deployments (id, environment_id, service_name, version_tag, commit_sha, deployed_by, status, changelog, started_at, completed_at) VALUES
('d0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000003', 'thingsboard-core-app', 'v3.6.2-patch184', 'a9b8c7d', 'jenkins-ci-bot', 'SUCCESS', 'Update telemetry ingestion pipeline and connection pool configuration', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Seed Active Incident for ThingsBoard Slowness
INSERT INTO incidents (id, environment_id, title, description, severity, status, affected_resource_type, affected_resource_id, root_cause_deployment_id, root_cause_summary, confidence_score, started_at, created_at, updated_at) VALUES
('i0000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000003', 'ThingsBoard API High Latency and Container CrashLoop', 'ThingsBoard API response latency spiked 11x with 7 consecutive container restarts detected following deployment v3.6.2-patch184.', 'HIGH', 'OPEN', 'CONTAINER', 'thingsboard-core-app', 'd0000000-0000-0000-0000-000000000001', 'PostgreSQL connection pool deadlock caused by aggressive telemetry thread starvation.', 0.91, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO incident_events (id, incident_id, event_type, message, payload, timestamp) VALUES
('ie000000-0000-0000-0000-000000000001', 'i0000000-0000-0000-0000-000000000001', 'THRESHOLD_BREACH', 'CPU utilization exceeded 90% threshold for 5m', '{"metric": "container_cpu_usage_percent", "value": 94.2}', CURRENT_TIMESTAMP),
('ie000000-0000-0000-0000-000000000002', 'i0000000-0000-0000-0000-000000000001', 'STATE_CHANGE', 'Container state transitioned to RESTARTING (ExitCode: 137 OOMKilled)', '{"restart_count": 7}', CURRENT_TIMESTAMP);

-- Seed Integrations
INSERT INTO integrations (id, environment_id, type, name, endpoint_url, health_status, last_synced_at, created_at, updated_at) VALUES
('in000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000003', 'PROMETHEUS', 'Production Prometheus Server', 'http://localhost:9090', 'HEALTHY', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('in000000-0000-0000-0000-000000000002', 'e0000000-0000-0000-0000-000000000003', 'DOCKER', 'Production Docker Engine', 'tcp://localhost:2375', 'HEALTHY', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('in000000-0000-0000-0000-000000000003', 'e0000000-0000-0000-0000-000000000003', 'LINUX_SSH', 'Prod Core Host SSH Agent', '10.0.10.15:22', 'HEALTHY', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
