-- V002__init_tenant_schema_template.sql
-- Template schema for tenant-specific tables (schema-per-tenant pattern)
-- This is a reference template. Actual tenant schemas are created dynamically.

-- Create identities table (per tenant)
CREATE TABLE IF NOT EXISTS public.identities_template (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rid VARCHAR(255) UNIQUE NOT NULL,
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    email VARCHAR(255),
    phone_number VARCHAR(20),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    metadata JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_identities_rid ON public.identities_template(rid);
CREATE INDEX idx_identities_status ON public.identities_template(status);
CREATE INDEX idx_identities_email ON public.identities_template(email);

-- Create fingerprints table (per tenant)
CREATE TABLE IF NOT EXISTS public.fingerprints_template (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rid VARCHAR(255) NOT NULL,
    finger_index INT NOT NULL,
    image_url VARCHAR(512),
    binary_template BYTEA,
    embedding_vector FLOAT8[],
    quality INT,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(rid, finger_index)
);

CREATE INDEX idx_fingerprints_rid ON public.fingerprints_template(rid);
CREATE INDEX idx_fingerprints_status ON public.fingerprints_template(status);
CREATE INDEX idx_fingerprints_finger_index ON public.fingerprints_template(finger_index);

-- Create match results table (per tenant)
CREATE TABLE IF NOT EXISTS public.match_results_template (
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
);

CREATE INDEX idx_match_results_probe_rid ON public.match_results_template(probe_rid);
CREATE INDEX idx_match_results_target_rid ON public.match_results_template(target_rid);
CREATE INDEX idx_match_results_status ON public.match_results_template(match_status);
CREATE INDEX idx_match_results_trace_id ON public.match_results_template(trace_id);
CREATE INDEX idx_match_results_created_at ON public.match_results_template(created_at);

-- Create worker assignments table (per tenant)
CREATE TABLE IF NOT EXISTS public.worker_assignments_template (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    worker_id VARCHAR(255) NOT NULL,
    shard_id INT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    total_templates BIGINT DEFAULT 0,
    hnsw_index_size_bytes BIGINT DEFAULT 0,
    offheap_memory_bytes BIGINT DEFAULT 0,
    last_heartbeat TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_worker_assignments_worker_id ON public.worker_assignments_template(worker_id);
CREATE INDEX idx_worker_assignments_shard_id ON public.worker_assignments_template(shard_id);
CREATE INDEX idx_worker_assignments_status ON public.worker_assignments_template(status);

-- Drop template tables (they're just for reference)
DROP TABLE IF EXISTS public.identities_template;
DROP TABLE IF EXISTS public.fingerprints_template;
DROP TABLE IF EXISTS public.match_results_template;
DROP TABLE IF EXISTS public.worker_assignments_template;
