-- V004__add_observability_tables.sql
-- Add observability and monitoring tables for performance tracking

-- Create worker metrics table
CREATE TABLE IF NOT EXISTS public.worker_metrics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    worker_id VARCHAR(255) NOT NULL,
    tenant_id VARCHAR(255),
    total_matches BIGINT DEFAULT 0,
    total_errors BIGINT DEFAULT 0,
    avg_latency_ms FLOAT DEFAULT 0,
    p95_latency_ms FLOAT DEFAULT 0,
    p99_latency_ms FLOAT DEFAULT 0,
    heap_memory_bytes BIGINT DEFAULT 0,
    offheap_memory_bytes BIGINT DEFAULT 0,
    hnsw_index_size_bytes BIGINT DEFAULT 0,
    cpu_usage_percent INT DEFAULT 0,
    grpc_queue_depth INT DEFAULT 0,
    kafka_lag BIGINT DEFAULT 0,
    recorded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_worker_metrics_worker_id ON public.worker_metrics(worker_id);
CREATE INDEX idx_worker_metrics_tenant_id ON public.worker_metrics(tenant_id);
CREATE INDEX idx_worker_metrics_recorded_at ON public.worker_metrics(recorded_at);

-- Create matching performance table
CREATE TABLE IF NOT EXISTS public.matching_performance (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(255) NOT NULL,
    probe_rid VARCHAR(255),
    target_rid VARCHAR(255),
    hnn_score INT,
    exact_score INT,
    final_score INT,
    matching_time_ms BIGINT,
    worker_id VARCHAR(255),
    trace_id VARCHAR(255),
    recorded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_matching_performance_tenant_id ON public.matching_performance(tenant_id);
CREATE INDEX idx_matching_performance_trace_id ON public.matching_performance(trace_id);
CREATE INDEX idx_matching_performance_recorded_at ON public.matching_performance(recorded_at);

-- Create system events table for operational events
CREATE TABLE IF NOT EXISTS public.system_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type VARCHAR(255) NOT NULL,
    tenant_id VARCHAR(255),
    component VARCHAR(255),
    severity VARCHAR(50),
    message TEXT,
    details JSONB,
    trace_id VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_system_events_event_type ON public.system_events(event_type);
CREATE INDEX idx_system_events_tenant_id ON public.system_events(tenant_id);
CREATE INDEX idx_system_events_severity ON public.system_events(severity);
CREATE INDEX idx_system_events_created_at ON public.system_events(created_at);

-- Create SLO tracking table
CREATE TABLE IF NOT EXISTS public.slo_tracking (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(255) NOT NULL,
    metric_name VARCHAR(255) NOT NULL,
    target_value FLOAT,
    actual_value FLOAT,
    is_compliant BOOLEAN,
    period_start TIMESTAMP NOT NULL,
    period_end TIMESTAMP NOT NULL,
    recorded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_slo_tracking_tenant_id ON public.slo_tracking(tenant_id);
CREATE INDEX idx_slo_tracking_metric_name ON public.slo_tracking(metric_name);
CREATE INDEX idx_slo_tracking_period_start ON public.slo_tracking(period_start);

-- Create index statistics table
CREATE TABLE IF NOT EXISTS public.index_statistics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(255) NOT NULL,
    worker_id VARCHAR(255) NOT NULL,
    index_name VARCHAR(255),
    total_vectors BIGINT,
    index_size_bytes BIGINT,
    memory_used_bytes BIGINT,
    query_count BIGINT,
    avg_query_time_ms FLOAT,
    recorded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_index_statistics_tenant_id ON public.index_statistics(tenant_id);
CREATE INDEX idx_index_statistics_worker_id ON public.index_statistics(worker_id);
CREATE INDEX idx_index_statistics_recorded_at ON public.index_statistics(recorded_at);

-- Create retention policy for metrics (keep 90 days)
-- Note: This is a reference. Actual cleanup should be done via scheduled jobs
-- DELETE FROM public.worker_metrics WHERE recorded_at < CURRENT_TIMESTAMP - INTERVAL '90 days';
-- DELETE FROM public.matching_performance WHERE recorded_at < CURRENT_TIMESTAMP - INTERVAL '90 days';
-- DELETE FROM public.system_events WHERE created_at < CURRENT_TIMESTAMP - INTERVAL '90 days';
