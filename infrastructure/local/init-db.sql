-- ScaleBiometrics Database Initialization Script

-- Create Keycloak schema
CREATE SCHEMA IF NOT EXISTS keycloak;

-- Create public schema tables
CREATE TABLE IF NOT EXISTS tenants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL UNIQUE,
    schema_name VARCHAR(63) NOT NULL UNIQUE,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'DELETED'))
);

CREATE TABLE IF NOT EXISTS tenant_config (
    tenant_id UUID PRIMARY KEY REFERENCES tenants(id) ON DELETE CASCADE,
    max_users INTEGER NOT NULL DEFAULT 100,
    max_storage_gb INTEGER NOT NULL DEFAULT 100,
    matching_threshold INTEGER NOT NULL DEFAULT 40,
    features JSONB NOT NULL DEFAULT '{}',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_tenants_status ON tenants(status);
CREATE INDEX IF NOT EXISTS idx_tenants_schema_name ON tenants(schema_name);

-- Create audit log table
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGSERIAL PRIMARY KEY,
    tenant_id UUID REFERENCES tenants(id),
    user_id VARCHAR(255),
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100),
    entity_id VARCHAR(255),
    details JSONB,
    ip_address INET,
    user_agent TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_audit_logs_tenant_id ON audit_logs(tenant_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_created_at ON audit_logs(created_at);
CREATE INDEX IF NOT EXISTS idx_audit_logs_action ON audit_logs(action);

-- Function to create tenant schema
CREATE OR REPLACE FUNCTION create_tenant_schema(p_schema_name VARCHAR)
RETURNS VOID AS $$
BEGIN
    EXECUTE format('CREATE SCHEMA IF NOT EXISTS %I', p_schema_name);
    
    -- Create identities table
    EXECUTE format('
        CREATE TABLE IF NOT EXISTS %I.identities (
            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
            client_rid VARCHAR(255) NOT NULL UNIQUE,
            metadata JSONB,
            status VARCHAR(50) NOT NULL DEFAULT ''ACTIVE'',
            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
            CONSTRAINT chk_status CHECK (status IN (''ACTIVE'', ''FLAGGED'', ''DELETED''))
        )', p_schema_name);
    
    -- Create fingerprints table
    EXECUTE format('
        CREATE TABLE IF NOT EXISTS %I.fingerprints (
            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
            identity_id UUID NOT NULL REFERENCES %I.identities(id) ON DELETE CASCADE,
            finger_position INTEGER NOT NULL,
            image_path VARCHAR(500) NOT NULL,
            template BYTEA NOT NULL,
            quality_score INTEGER,
            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
            CONSTRAINT chk_finger_position CHECK (finger_position BETWEEN 1 AND 10),
            CONSTRAINT chk_quality_score CHECK (quality_score IS NULL OR quality_score BETWEEN 0 AND 100)
        )', p_schema_name, p_schema_name);
    
    -- Create match_results table
    EXECUTE format('
        CREATE TABLE IF NOT EXISTS %I.match_results (
            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
            probe_identity_id UUID REFERENCES %I.identities(id),
            matched_identity_id UUID REFERENCES %I.identities(id),
            match_score DOUBLE PRECISION NOT NULL,
            match_type VARCHAR(50) NOT NULL,
            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
            CONSTRAINT chk_match_type CHECK (match_type IN (''DEDUPLICATION'', ''VERIFICATION''))
        )', p_schema_name, p_schema_name, p_schema_name);
    
    -- Create indexes
    EXECUTE format('CREATE INDEX IF NOT EXISTS idx_identities_client_rid ON %I.identities(client_rid)', p_schema_name);
    EXECUTE format('CREATE INDEX IF NOT EXISTS idx_identities_status ON %I.identities(status)', p_schema_name);
    EXECUTE format('CREATE INDEX IF NOT EXISTS idx_fingerprints_identity_id ON %I.fingerprints(identity_id)', p_schema_name);
    EXECUTE format('CREATE INDEX IF NOT EXISTS idx_match_results_probe_id ON %I.match_results(probe_identity_id)', p_schema_name);
    EXECUTE format('CREATE INDEX IF NOT EXISTS idx_match_results_matched_id ON %I.match_results(matched_identity_id)', p_schema_name);
END;
$$ LANGUAGE plpgsql;

-- Insert default superadmin tenant
INSERT INTO tenants (id, name, schema_name, status)
VALUES ('00000000-0000-0000-0000-000000000000', 'System', 'system', 'ACTIVE')
ON CONFLICT (name) DO NOTHING;

INSERT INTO tenant_config (tenant_id, max_users, max_storage_gb, matching_threshold)
VALUES ('00000000-0000-0000-0000-000000000000', 999999, 999999, 40)
ON CONFLICT (tenant_id) DO NOTHING;

-- Create system schema
SELECT create_tenant_schema('system');

COMMENT ON TABLE tenants IS 'Global tenant registry';
COMMENT ON TABLE tenant_config IS 'Tenant-specific configuration';
COMMENT ON TABLE audit_logs IS 'Immutable audit trail for all actions';
COMMENT ON FUNCTION create_tenant_schema IS 'Creates a new tenant schema with all required tables';
