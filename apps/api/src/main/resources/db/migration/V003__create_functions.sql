-- V003__create_functions.sql
-- PostgreSQL functions for tenant management and multi-tenancy operations

-- Function to create a new tenant schema
CREATE OR REPLACE FUNCTION public.create_tenant_schema(p_tenant_id VARCHAR)
RETURNS BOOLEAN AS $$
DECLARE
    v_schema_name VARCHAR;
BEGIN
    v_schema_name := 't_' || REPLACE(p_tenant_id, '-', '_');
    
    -- Create schema
    EXECUTE 'CREATE SCHEMA IF NOT EXISTS ' || v_schema_name;
    
    -- Create identities table
    EXECUTE 'CREATE TABLE IF NOT EXISTS ' || v_schema_name || '.identities (
        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        rid VARCHAR(255) UNIQUE NOT NULL,
        first_name VARCHAR(255),
        last_name VARCHAR(255),
        email VARCHAR(255),
        phone_number VARCHAR(20),
        status VARCHAR(50) NOT NULL DEFAULT ''ACTIVE'',
        metadata JSONB,
        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    )';
    
    -- Create indexes for identities
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_' || v_schema_name || '_identities_rid ON ' || v_schema_name || '.identities(rid)';
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_' || v_schema_name || '_identities_status ON ' || v_schema_name || '.identities(status)';
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_' || v_schema_name || '_identities_email ON ' || v_schema_name || '.identities(email)';
    
    -- Create fingerprints table
    EXECUTE 'CREATE TABLE IF NOT EXISTS ' || v_schema_name || '.fingerprints (
        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        rid VARCHAR(255) NOT NULL,
        finger_index INT NOT NULL,
        image_url VARCHAR(512),
        binary_template BYTEA,
        embedding_vector FLOAT8[],
        quality INT,
        status VARCHAR(50) NOT NULL DEFAULT ''ACTIVE'',
        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        UNIQUE(rid, finger_index)
    )';
    
    -- Create indexes for fingerprints
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_' || v_schema_name || '_fingerprints_rid ON ' || v_schema_name || '.fingerprints(rid)';
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_' || v_schema_name || '_fingerprints_status ON ' || v_schema_name || '.fingerprints(status)';
    
    -- Create match_results table
    EXECUTE 'CREATE TABLE IF NOT EXISTS ' || v_schema_name || '.match_results (
        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        probe_rid VARCHAR(255) NOT NULL,
        target_rid VARCHAR(255),
        match_status VARCHAR(50) NOT NULL,
        hnn_score INT,
        exact_score INT,
        final_score INT,
        is_match BOOLEAN,
        matching_time_ms BIGINT,
        trace_id VARCHAR(255),
        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    )';
    
    -- Create indexes for match_results
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_' || v_schema_name || '_match_results_probe_rid ON ' || v_schema_name || '.match_results(probe_rid)';
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_' || v_schema_name || '_match_results_trace_id ON ' || v_schema_name || '.match_results(trace_id)';
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_' || v_schema_name || '_match_results_created_at ON ' || v_schema_name || '.match_results(created_at)';
    
    -- Create worker_assignments table
    EXECUTE 'CREATE TABLE IF NOT EXISTS ' || v_schema_name || '.worker_assignments (
        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        worker_id VARCHAR(255) NOT NULL,
        shard_id INT NOT NULL,
        status VARCHAR(50) NOT NULL DEFAULT ''ACTIVE'',
        total_templates BIGINT DEFAULT 0,
        hnsw_index_size_bytes BIGINT DEFAULT 0,
        offheap_memory_bytes BIGINT DEFAULT 0,
        last_heartbeat TIMESTAMP,
        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    )';
    
    -- Create indexes for worker_assignments
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_' || v_schema_name || '_worker_assignments_worker_id ON ' || v_schema_name || '.worker_assignments(worker_id)';
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_' || v_schema_name || '_worker_assignments_shard_id ON ' || v_schema_name || '.worker_assignments(shard_id)';
    
    RETURN TRUE;
EXCEPTION WHEN OTHERS THEN
    RAISE WARNING 'Error creating tenant schema: %', SQLERRM;
    RETURN FALSE;
END;
$$ LANGUAGE plpgsql;

-- Function to drop a tenant schema
CREATE OR REPLACE FUNCTION public.drop_tenant_schema(p_tenant_id VARCHAR)
RETURNS BOOLEAN AS $$
DECLARE
    v_schema_name VARCHAR;
BEGIN
    v_schema_name := 't_' || REPLACE(p_tenant_id, '-', '_');
    
    EXECUTE 'DROP SCHEMA IF EXISTS ' || v_schema_name || ' CASCADE';
    
    RETURN TRUE;
EXCEPTION WHEN OTHERS THEN
    RAISE WARNING 'Error dropping tenant schema: %', SQLERRM;
    RETURN FALSE;
END;
$$ LANGUAGE plpgsql;

-- Function to get tenant schema name
CREATE OR REPLACE FUNCTION public.get_tenant_schema_name(p_tenant_id VARCHAR)
RETURNS VARCHAR AS $$
BEGIN
    RETURN 't_' || REPLACE(p_tenant_id, '-', '_');
END;
$$ LANGUAGE plpgsql;

-- Function to check if tenant schema exists
CREATE OR REPLACE FUNCTION public.tenant_schema_exists(p_tenant_id VARCHAR)
RETURNS BOOLEAN AS $$
DECLARE
    v_schema_name VARCHAR;
    v_exists BOOLEAN;
BEGIN
    v_schema_name := 't_' || REPLACE(p_tenant_id, '-', '_');
    
    SELECT EXISTS (
        SELECT 1 FROM information_schema.schemata 
        WHERE schema_name = v_schema_name
    ) INTO v_exists;
    
    RETURN v_exists;
END;
$$ LANGUAGE plpgsql;

-- Function to get tenant statistics
CREATE OR REPLACE FUNCTION public.get_tenant_stats(p_tenant_id VARCHAR)
RETURNS TABLE (
    total_identities BIGINT,
    total_fingerprints BIGINT,
    total_match_results BIGINT,
    schema_size_bytes BIGINT
) AS $$
DECLARE
    v_schema_name VARCHAR;
BEGIN
    v_schema_name := 't_' || REPLACE(p_tenant_id, '-', '_');
    
    RETURN QUERY EXECUTE
        'SELECT 
            (SELECT COUNT(*) FROM ' || v_schema_name || '.identities)::BIGINT,
            (SELECT COUNT(*) FROM ' || v_schema_name || '.fingerprints)::BIGINT,
            (SELECT COUNT(*) FROM ' || v_schema_name || '.match_results)::BIGINT,
            pg_total_relation_size(''' || v_schema_name || ''')::BIGINT';
END;
$$ LANGUAGE plpgsql;
