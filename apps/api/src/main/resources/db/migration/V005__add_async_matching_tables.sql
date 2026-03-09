-- V005__add_async_matching_tables.sql
-- Add tables for async matching and update tenants table

-- Add callback_url to tenants table
ALTER TABLE public.tenants ADD COLUMN IF NOT EXISTS callback_url TEXT;

-- Create match_requests table
CREATE TABLE IF NOT EXISTS public.match_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(255) NOT NULL,
    trace_id VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    request_type VARCHAR(50) NOT NULL,
    probe_rid VARCHAR(255),
    target_rid VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    error_message TEXT
);

CREATE INDEX idx_match_requests_tenant_id ON public.match_requests(tenant_id);
CREATE INDEX idx_match_requests_status ON public.match_requests(status);
CREATE INDEX idx_match_requests_trace_id ON public.match_requests(trace_id);
CREATE INDEX idx_match_requests_created_at ON public.match_requests(created_at);
