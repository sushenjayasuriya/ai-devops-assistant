-- ============================================================================
-- V5__infrastructure_integrations.sql
-- Infrastructure integration schema enhancements for real data plane
-- ============================================================================

-- 1. Enhance integrations table
ALTER TABLE integrations ADD COLUMN IF NOT EXISTS auth_type VARCHAR(50) DEFAULT 'NONE';
ALTER TABLE integrations ADD COLUMN IF NOT EXISTS timeout_ms INT DEFAULT 5000;
ALTER TABLE integrations ADD COLUMN IF NOT EXISTS max_retries INT DEFAULT 2;
ALTER TABLE integrations ADD COLUMN IF NOT EXISTS metadata_json TEXT;
ALTER TABLE integrations ADD COLUMN IF NOT EXISTS enabled BOOLEAN DEFAULT true;

-- 2. Enhance servers table for SSH connectivity
ALTER TABLE servers ADD COLUMN IF NOT EXISTS ssh_port INT DEFAULT 22;
ALTER TABLE servers ADD COLUMN IF NOT EXISTS ssh_user VARCHAR(100) DEFAULT 'devops';
ALTER TABLE servers ADD COLUMN IF NOT EXISTS ssh_credential_encrypted TEXT;
ALTER TABLE servers ADD COLUMN IF NOT EXISTS integration_id UUID REFERENCES integrations(id) ON DELETE SET NULL;

-- 3. Create index for faster tenant and type lookups
CREATE INDEX IF NOT EXISTS idx_integrations_env_type ON integrations(environment_id, type);
CREATE INDEX IF NOT EXISTS idx_integrations_enabled ON integrations(enabled);
CREATE INDEX IF NOT EXISTS idx_servers_integration ON servers(integration_id);
