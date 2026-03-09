-- V006__add_match_request_fingerprints.sql
-- Add table for storing fingerprint details linked to match requests

CREATE TABLE IF NOT EXISTS public.match_request_fingerprints (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    match_request_id UUID NOT NULL REFERENCES public.match_requests(id) ON DELETE CASCADE,
    image_path TEXT,
    template_data BYTEA,
    finger_index INT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_match_request_fingerprints_request_id ON public.match_request_fingerprints(match_request_id);
