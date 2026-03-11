# API Contract - Frontend 5.4 Endpoints

**Generated:** 2026-03-09  
**Purpose:** Contract between Frontend (Epic 5.4) and Backend implementations

---

## Overview

This document defines the OpenAPI 3.0 specifications for all backend endpoints required by the Tenant Settings & Integration (Epic 5.4).

---

## Base Configuration

- **Base URL:** `/api/v1`
- **Authentication:** Bearer Token (JWT from Keycloak)
- **Content-Type:** `application/json`

---

## 1. API Keys Management Endpoints

### 1.1 List API Keys

Retrieve all API keys for a tenant.

**Endpoint:** `GET /tenants/{tenantId}/api-keys`

**Response:**
```json
{
  "keys": [
    {
      "id": "key-001",
      "name": "Production API Key",
      "prefix": "sk_live_",
      "scopes": ["read", "write", "matching"],
      "status": "ACTIVE",
      "expiresAt": "2026-12-31T23:59:59Z",
      "createdAt": "2026-01-15T10:00:00Z",
      "lastUsedAt": "2026-03-09T10:30:00Z"
    }
  ]
}
```

---

### 1.2 Create API Key

Create a new API key.

**Endpoint:** `POST /tenants/{tenantId}/api-keys`

**Request:**
```json
{
  "name": "Development API Key",
  "scopes": ["read", "matching"],
  "expiresIn": "30d"
}
```

**Response:**
```json
{
  "id": "key-002",
  "name": "Development API Key",
  "key": "sk_live_abc123xyz789...",  // Only shown once!
  "scopes": ["read", "matching"],
  "status": "ACTIVE",
  "expiresAt": "2026-04-08T10:00:00Z",
  "createdAt": "2026-03-09T10:00:00Z"
}
```

---

### 1.3 Revoke API Key

Revoke an existing API key.

**Endpoint:** `POST /tenants/{tenantId}/api-keys/{keyId}/revoke`

**Response:**
```json
{
  "success": true,
  "message": "API key revoked successfully"
}
```

---

### 1.4 Delete API Key

Permanently delete an API key.

**Endpoint:** `DELETE /tenants/{tenantId}/api-keys/{keyId}`

**Response:**
```json
{
  "success": true,
  "message": "API key deleted successfully"
}
```

---

## 2. Webhooks Configuration Endpoints

### 2.1 List Webhooks

Retrieve all webhooks for a tenant.

**Endpoint:** `GET /tenants/{tenantId}/webhooks`

**Response:**
```json
{
  "webhooks": [
    {
      "id": "wh-001",
      "name": "Production Webhook",
      "url": "https://api.example.com/webhooks/scalebiometrics",
      "events": ["job.completed", "job.failed"],
      "status": "ACTIVE",
      "healthStatus": "healthy",
      "lastCheckAt": "2026-03-09T10:30:00Z",
      "retryPolicy": {
        "maxRetries": 3,
        "retryDelay": 1000
      },
      "createdAt": "2026-01-15T10:00:00Z",
      "updatedAt": "2026-03-01T14:30:00Z"
    }
  ]
}
```

---

### 2.2 Create Webhook

Create a new webhook.

**Endpoint:** `POST /tenants/{tenantId}/webhooks`

**Request:**
```json
{
  "name": "Production Webhook",
  "url": "https://api.example.com/webhooks/scalebiometrics",
  "events": ["job.completed", "job.failed"],
  "retryPolicy": {
    "maxRetries": 3,
    "retryDelay": 1000
  },
  "secret": "whsec_..."  // Optional: for signature verification
}
```

**Response:**
```json
{
  "id": "wh-001",
  "name": "Production Webhook",
  "url": "https://api.example.com/webhooks/scalebiometrics",
  "events": ["job.completed", "job.failed"],
  "status": "ACTIVE",
  "healthStatus": "healthy",
  "retryPolicy": {
    "maxRetries": 3,
    "retryDelay": 1000
  },
  "createdAt": "2026-03-09T10:00:00Z"
}
```

---

### 2.3 Update Webhook

Update an existing webhook.

**Endpoint:** `PUT /tenants/{tenantId}/webhooks/{webhookId}`

**Request:**
```json
{
  "name": "Updated Webhook Name",
  "url": "https://api.example.com/webhooks/new-endpoint",
  "events": ["job.completed", "job.failed", "job.queued"],
  "retryPolicy": {
    "maxRetries": 5,
    "retryDelay": 2000
  }
}
```

---

### 2.4 Delete Webhook

Delete a webhook.

**Endpoint:** `DELETE /tenants/{tenantId}/webhooks/{webhookId}`

**Response:**
```json
{
  "success": true,
  "message": "Webhook deleted successfully"
}
```

---

### 2.5 Test Webhook

Send a test payload to the webhook URL.

**Endpoint:** `POST /tenants/{tenantId}/webhooks/{webhookId}/test`

**Response:**
```json
{
  "success": true,
  "statusCode": 200,
  "responseTime": 145,
  "message": "Webhook test successful"
}
```

---

### 2.6 Get Webhook Delivery History

Retrieve delivery history for a webhook.

**Endpoint:** `GET /tenants/{tenantId}/webhooks/{webhookId}/deliveries`

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| page | integer | Page number |
| size | integer | Page size |

**Response:**
```json
{
  "deliveries": [
    {
      "id": "del-001",
      "event": "job.completed",
      "statusCode": 200,
      "status": "SUCCESS",
      "payload": {...},
      "response": "OK",
      "attempt": 1,
      "sentAt": "2026-03-09T10:30:00Z",
      "receivedAt": "2026-03-09T10:30:01Z"
    }
  ],
  "totalElements": 150,
  "totalPages": 15
}
```

---

## 3. Tenant Settings Endpoints

### 3.1 Get Tenant Settings

Retrieve all tenant settings.

**Endpoint:** `GET /tenants/{tenantId}/settings`

**Response:**
```json
{
  "general": {
    "name": "Acme Corporation",
    "logo": "https://storage.example.com/logos/acme.png",
    "contactEmail": "admin@acme.com",
    "contactPhone": "+1234567890"
  },
  "matching": {
    "confidenceThreshold": 30,
    "topK": 10,
    "annEnabled": true,
    "algorithm": "SOURCEAFIS"
  },
  "storage": {
    "retentionDays": 90,
    "compressionEnabled": true,
    "autoArchiveEnabled": false
  }
}
```

---

### 3.2 Update Tenant Settings

Update tenant settings.

**Endpoint:** `PUT /tenants/{tenantId}/settings`

**Request:**
```json
{
  "general": {
    "name": "Acme Corporation",
    "logo": "https://storage.example.com/logos/acme.png",
    "contactEmail": "admin@acme.com",
    "contactPhone": "+1234567890"
  },
  "matching": {
    "confidenceThreshold": 35,
    "topK": 15,
    "annEnabled": true,
    "algorithm": "SOURCEAFIS"
  },
  "storage": {
    "retentionDays": 60,
    "compressionEnabled": true,
    "autoArchiveEnabled": true
  }
}
```

**Response:**
```json
{
  "success": true,
  "message": "Settings updated successfully",
  "updatedAt": "2026-03-09T10:00:00Z"
}
```

---

### 3.3 Upload Logo

Upload tenant logo.

**Endpoint:** `POST /tenants/{tenantId}/settings/logo`

**Content-Type:** `multipart/form-data`

**Request:**
- `file`: Image file (PNG, JPG, max 2MB)

**Response:**
```json
{
  "success": true,
  "logoUrl": "https://storage.example.com/logos/tenant-001/new-logo.png"
}
```

---

## Available Scopes for API Keys

| Scope | Description |
|-------|-------------|
| `read` | Read access to records and matching results |
| `write` | Create and update records |
| `delete` | Delete records |
| `matching` | Submit matching requests |
| `admin` | Full administrative access |

---

## Available Webhook Events

| Event | Description |
|-------|-------------|
| `job.queued` | Job added to queue |
| `job.processing` | Job started processing |
| `job.completed` | Job completed successfully |
| `job.failed` | Job failed |
| `job.cancelled` | Job was cancelled |
| `record.created` | New record created |
| `record.deleted` | Record deleted |

---

## OpenAPI Specification Summary

```yaml
paths:
  /tenants/{tenantId}/api-keys:
    get:
      summary: List API keys
    post:
      summary: Create API key

  /tenants/{tenantId}/api-keys/{keyId}/revoke:
    post:
      summary: Revoke API key

  /tenants/{tenantId}/webhooks:
    get:
      summary: List webhooks
    post:
      summary: Create webhook

  /tenants/{tenantId}/webhooks/{webhookId}:
    put:
      summary: Update webhook
    delete:
      summary: Delete webhook

  /tenants/{tenantId}/webhooks/{webhookId}/test:
    post:
      summary: Test webhook

  /tenants/{tenantId}/webhooks/{webhookId}/deliveries:
    get:
      summary: Get delivery history

  /tenants/{tenantId}/settings:
    get:
      summary: Get tenant settings
    put:
      summary: Update tenant settings
```

---

## Implementation Notes

1. **API Key Security**: The key value is only returned once during creation
2. **Webhook Health**: Backend should periodically check webhook URL health
3. **Settings Validation**: Confidence threshold (0-100), Top-K (1-100)
4. **Logo Upload**: Max file size 2MB, formats: PNG, JPG, SVG
5. **Retention Policy**: Min 1 day, max 365 days
