-- Flyway Migration: V3 Performance Indexes

CREATE INDEX idx_servers_env_id ON servers(environment_id);
CREATE INDEX idx_servers_status ON servers(status);

CREATE INDEX idx_containers_env_id ON containers(environment_id);
CREATE INDEX idx_containers_server_id ON containers(server_id);
CREATE INDEX idx_containers_state ON containers(state);

CREATE INDEX idx_deployments_env_id ON deployments(environment_id);
CREATE INDEX idx_deployments_started_at ON deployments(started_at);

CREATE INDEX idx_incidents_env_id ON incidents(environment_id);
CREATE INDEX idx_incidents_status ON incidents(status);
CREATE INDEX idx_incidents_severity ON incidents(severity);
CREATE INDEX idx_incidents_started_at ON incidents(started_at);

CREATE INDEX idx_incident_events_incident_id ON incident_events(incident_id);
CREATE INDEX idx_incident_events_timestamp ON incident_events(timestamp);

CREATE INDEX idx_ai_conversations_user_id ON ai_conversations(user_id);
CREATE INDEX idx_ai_conversations_env_id ON ai_conversations(environment_id);

CREATE INDEX idx_ai_messages_conv_id ON ai_messages(conversation_id);
CREATE INDEX idx_ai_actions_message_id ON ai_actions(message_id);

CREATE INDEX idx_approval_requests_env_id ON approval_requests(environment_id);
CREATE INDEX idx_approval_requests_status ON approval_requests(status);

CREATE INDEX idx_audit_logs_user_id ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_timestamp ON audit_logs(timestamp);
CREATE INDEX idx_audit_logs_correlation_id ON audit_logs(correlation_id);
CREATE INDEX idx_audit_logs_env_name ON audit_logs(environment_name);

CREATE INDEX idx_notifications_org_read ON notifications(organization_id, is_read);
