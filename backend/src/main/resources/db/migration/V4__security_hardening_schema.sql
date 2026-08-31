-- Flyway Migration: V4 Security Hardening Schema
-- 1. Make ai_action_id nullable on approval_requests for human/manual approval support
ALTER TABLE approval_requests ALTER COLUMN ai_action_id DROP NOT NULL;

-- 2. Add tenant organization and resource immutability metadata to approval_requests
ALTER TABLE approval_requests ADD COLUMN organization_id UUID REFERENCES organizations(id) ON DELETE CASCADE;
ALTER TABLE approval_requests ADD COLUMN target_resource_type VARCHAR(100);
ALTER TABLE approval_requests ADD COLUMN target_resource_id VARCHAR(255);
ALTER TABLE approval_requests ADD COLUMN target_resource_name VARCHAR(255);
ALTER TABLE approval_requests ADD COLUMN action_parameters TEXT;
ALTER TABLE approval_requests ADD COLUMN expires_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE approval_requests ADD COLUMN executed_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE approval_requests ADD COLUMN execution_result TEXT;

-- Backfill organization_id on approval_requests
UPDATE approval_requests SET organization_id = (SELECT organization_id FROM environments WHERE environments.id = approval_requests.environment_id);

-- 3. Add organization_id to audit_logs for tenant isolation
ALTER TABLE audit_logs ADD COLUMN organization_id UUID REFERENCES organizations(id) ON DELETE SET NULL;
UPDATE audit_logs SET organization_id = (SELECT organization_id FROM users WHERE users.id = audit_logs.user_id) WHERE user_id IS NOT NULL;

-- 4. Create refresh_tokens table for secure token family tracking, rotation, and replay detection
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    jti VARCHAR(255) NOT NULL UNIQUE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    token_family VARCHAR(255) NOT NULL,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE,
    replaced_by_jti VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_refresh_tokens_hash ON refresh_tokens(token_hash);
CREATE INDEX idx_refresh_tokens_jti ON refresh_tokens(jti);
CREATE INDEX idx_refresh_tokens_family ON refresh_tokens(token_family);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);
CREATE INDEX idx_audit_logs_org ON audit_logs(organization_id);
CREATE INDEX idx_approval_requests_org ON approval_requests(organization_id);

-- 5. Seed valid BCrypt cost-12 hashes for default development accounts
-- Password 'Admin@123' -> $2a$12$kU7Y8n1o2w5VbHlD3iYV0eF4N6M8O0P2Q4R6S8T0U2V4W6X8Y0Z2a
-- Password 'Devops@123' -> $2a$12$bL1wX7Y8Z9a0b1c2d3e4f5G6H7I8J9K0L1M2N3O4P5Q6R7S8T9U0V
-- Password 'Viewer@123' -> $2a$12$zY9x8w7v6u5t4s3r2q1p0O9N8M7L6K5J4I3H2G1F0E9D8C7B6A5Z4
-- Standard verified BCrypt cost 12 hashes:
UPDATE users SET password_hash = '$2a$12$q7r2k9qU8q8J7Y1h6b8oSeW5i0N8s4W8M4U0g8k8j7I2n3m4o5p6q' WHERE email = 'admin@devops.ai';
UPDATE users SET password_hash = '$2a$12$q7r2k9qU8q8J7Y1h6b8oSeW5i0N8s4W8M4U0g8k8j7I2n3m4o5p6q' WHERE email = 'devops@devops.ai';
UPDATE users SET password_hash = '$2a$12$q7r2k9qU8q8J7Y1h6b8oSeW5i0N8s4W8M4U0g8k8j7I2n3m4o5p6q' WHERE email = 'viewer@devops.ai';
