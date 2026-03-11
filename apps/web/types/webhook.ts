export type WebhookStatus = 'ACTIVE' | 'INACTIVE';
export type WebhookHealthStatus = 'healthy' | 'degraded' | 'down' | 'unknown';
export type WebhookEvent = 'job.queued' | 'job.processing' | 'job.completed' | 'job.failed' | 'job.cancelled' | 'record.created' | 'record.deleted';

export interface RetryPolicy {
  maxRetries: number;
  retryDelay: number;
}

export interface Webhook {
  id: string;
  name: string;
  url: string;
  events: WebhookEvent[];
  status: WebhookStatus;
  healthStatus: WebhookHealthStatus;
  lastCheckAt?: string;
  retryPolicy: RetryPolicy;
  createdAt: string;
  updatedAt: string;
}

export interface CreateWebhookRequest {
  name: string;
  url: string;
  events: WebhookEvent[];
  retryPolicy: RetryPolicy;
  secret?: string;
}

export interface WebhookDelivery {
  id: string;
  event: WebhookEvent;
  statusCode: number;
  status: 'SUCCESS' | 'FAILED' | 'PENDING';
  payload: Record<string, unknown>;
  response?: string;
  attempt: number;
  sentAt: string;
  receivedAt?: string;
}

export interface WebhookTestResult {
  success: boolean;
  statusCode?: number;
  responseTime?: number;
  message: string;
}

export interface PaginatedDeliveries {
  deliveries: WebhookDelivery[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}
