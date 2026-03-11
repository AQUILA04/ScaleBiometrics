# API Contract - Frontend 5.3 Endpoints

**Generated:** 2026-03-09  
**Purpose:** Contract between Frontend (Epic 5.3) and Backend implementations

---

## Overview

This document defines the OpenAPI 3.0 specifications for all backend endpoints required by the Tenant Dashboard (Epic 5.3). These endpoints should be implemented in the `apps/api` module.

---

## Base Configuration

- **Base URL:** `/api/v1`
- **Authentication:** Bearer Token (JWT from Keycloak)
- **Content-Type:** `application/json`

---

## 1. Tenant Dashboard Endpoints

### 1.1 Get Tenant Dashboard KPIs

Retrieve current metrics for the tenant dashboard.

**Endpoint:** `GET /tenants/{tenantId}/dashboard/kpis`

**Response:**
```json
{
  "pendingQueue": 42,
  "processed24h": 15820,
  "successRate": 98.5,
  "avgWaitTime": 125,
  "activeWorkers": 4,
  "storageUsed": 15600000000,
  "recordsCount": 25000,
  "trends": {
    "queue": -12,
    "processed": 8,
    "successRate": 0.3,
    "waitTime": -5
  }
}
```

---

### 1.2 Get Throughput Data

Retrieve throughput metrics for charts.

**Endpoint:** `GET /tenants/{tenantId}/dashboard/throughput?hours=24`

**Response:**
```json
{
  "data": [
    {
      "timestamp": "2026-03-09T10:00:00Z",
      "totalRequests": 150,
      "successfulRequests": 148,
      "failedRequests": 2,
      "avgLatencyMs": 85
    }
  ]
}
```

---

### 1.3 Get Recent Jobs

Retrieve the 10 most recent jobs.

**Endpoint:** `GET /tenants/{tenantId}/jobs/recent`

**Response:**
```json
{
  "jobs": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "type": "1:N",
      "status": "COMPLETED",
      "priority": "NORMAL",
      "probeRid": "RID-001",
      "createdAt": "2026-03-09T10:30:00Z",
      "completedAt": "2026-03-09T10:30:45Z",
      "duration": 45,
      "result": {
        "matchFound": true,
        "candidateCount": 3
      }
    }
  ]
}
```

---

## 2. Queue Management Endpoints

### 2.1 List Jobs

Paginated list of jobs with filtering.

**Endpoint:** `GET /tenants/{tenantId}/jobs`

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| page | integer | Page number (0-based) |
| size | integer | Page size |
| status | string | Filter by status |
| type | string | Filter by type (1:N, 1:1) |
| priority | string | Filter by priority |
| sort | string | Sort field |

**Response:**
```json
{
  "content": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "type": "1:N",
      "status": "PENDING",
      "priority": "HIGH",
      "probeRid": "RID-001",
      "probeFilename": "fingerprint_001.png",
      "candidateCount": 1000,
      "createdAt": "2026-03-09T10:30:00Z",
      "updatedAt": "2026-03-09T10:30:00Z"
    }
  ],
  "totalElements": 150,
  "totalPages": 15,
  "size": 10,
  "number": 0
}
```

---

### 2.2 Get Job Details

Retrieve detailed job information.

**Endpoint:** `GET /tenants/{tenantId}/jobs/{jobId}`

**Response:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "type": "1:N",
  "status": "COMPLETED",
  "priority": "NORMAL",
  "probeRid": "RID-001",
  "probeFilename": "fingerprint_001.png",
  "candidateCount": 1000,
  "threshold": 30,
  "createdAt": "2026-03-09T10:30:00Z",
  "startedAt": "2026-03-09T10:30:05Z",
  "completedAt": "2026-03-09T10:30:45Z",
  "duration": 40,
  "payload": {
    "algorithm": "SOURCEAFIS",
    "useAnn": true,
    "topK": 10
  },
  "timeline": [
    {
      "status": "QUEUED",
      "timestamp": "2026-03-09T10:30:00Z"
    },
    {
      "status": "PROCESSING",
      "timestamp": "2026-03-09T10:30:05Z"
    },
    {
      "status": "COMPLETED",
      "timestamp": "2026-03-09T10:30:45Z"
    }
  ],
  "result": {
    "matchFound": true,
    "candidates": [
      {
        "candidateRid": "RID-002",
        "score": 45,
        "confidence": 92.5
      }
    ]
  }
}
```

---

### 2.3 Escalate Job Priority

Increase job priority.

**Endpoint:** `POST /tenants/{tenantId}/jobs/{jobId}/escalate`

**Response:**
```json
{
  "success": true,
  "message": "Job priority escalated to HIGH"
}
```

---

### 2.4 Cancel Job

Cancel a pending or processing job.

**Endpoint:** `POST /tenants/{tenantId}/jobs/{jobId}/cancel`

**Response:**
```json
{
  "success": true,
  "message": "Job cancelled successfully"
}
```

---

## 3. Matching Results Endpoints

### 3.1 List Matching Results

Historical list of matching results.

**Endpoint:** `GET /tenants/{tenantId}/matching/results`

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| page | integer | Page number |
| size | integer | Page size |
| result | string | Filter by result (MATCH_FOUND, NO_MATCH) |
| fromDate | string | Filter from date |
| toDate | string | Filter to date |

**Response:**
```json
{
  "content": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "jobId": "660e8400-e29b-41d4-a716-446655440001",
      "type": "1:N",
      "probeRid": "RID-001",
      "probeFilename": "fingerprint_001.png",
      "matchFound": true,
      "candidateCount": 3,
      "topScore": 45,
      "topConfidence": 92.5,
      "processedAt": "2026-03-09T10:30:45Z"
    }
  ],
  "totalElements": 5000,
  "totalPages": 500,
  "size": 10,
  "number": 0
}
```

---

### 3.2 Get Matching Result Details

Detailed view of a matching result with probe and candidates.

**Endpoint:** `GET /tenants/{tenantId}/matching/results/{resultId}`

**Response:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "jobId": "660e8400-e29b-41d4-a716-446655440001",
  "type": "1:N",
  "probe": {
    "rid": "RID-001",
    "filename": "fingerprint_001.png",
    "imageUrl": "/api/v1/tenants/{tenantId}/records/RID-001/fingerprints/fp1/image",
    "metadata": {
      "width": 500,
      "height": 500,
      "dpi": 500,
      "quality": 85
    }
  },
  "candidates": [
    {
      "rid": "RID-002",
      "name": "John Doe",
      "score": 45,
      "confidence": 92.5,
      "imageUrl": "/api/v1/tenants/{tenantId}/records/RID-002/fingerprints/fp1/image",
      "metadata": {
        "fingerPosition": "RIGHT_INDEX",
        "quality": 90
      }
    }
  ],
  "processing": {
    "algorithm": "SOURCEAFIS",
    "threshold": 30,
    "useAnn": true,
    "processingTime": 40,
    "candidatesScanned": 1000
  },
  "createdAt": "2026-03-09T10:30:45Z"
}
```

---

## 4. Records Explorer Endpoints

### 4.1 List Records

Search and list biometric records.

**Endpoint:** `GET /tenants/{tenantId}/records`

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| page | integer | Page number |
| size | integer | Page size |
| search | string | Search by RID or name |
| sort | string | Sort field |

**Response:**
```json
{
  "content": [
    {
      "rid": "RID-001",
      "name": "John Doe",
      "email": "john@example.com",
      "createdAt": "2025-01-15T10:00:00Z",
      "updatedAt": "2026-03-01T14:30:00Z",
      "fingerprintCount": 10,
      "lastMatchAt": "2026-03-09T10:30:45Z"
    }
  ],
  "totalElements": 25000,
  "totalPages": 2500,
  "size": 10,
  "number": 0
}
```

---

### 4.2 Get Record Details

Detailed view of a biometric record.

**Endpoint:** `GET /tenants/{tenantId}/records/{rid}`

**Response:**
```json
{
  "rid": "RID-001",
  "name": "John Doe",
  "email": "john@example.com",
  "phone": "+1234567890",
  "metadata": {
    "department": "HR",
    "employeeId": "EMP-001"
  },
  "fingerprints": [
    {
      "id": "fp1",
      "position": "RIGHT_INDEX",
      "imageUrl": "/api/v1/tenants/{tenantId}/records/RID-001/fingerprints/fp1/image",
      "templateUrl": "/api/v1/tenants/{tenantId}/records/RID-001/fingerprints/fp1/template",
      "quality": 90,
      "createdAt": "2025-01-15T10:00:00Z"
    }
  ],
  "createdAt": "2025-01-15T10:00:00Z",
  "updatedAt": "2026-03-01T14:30:00Z"
}
```

---

### 4.3 Get Record Matching History

History of matching requests for a specific record.

**Endpoint:** `GET /tenants/{tenantId}/records/{rid}/history`

**Response:**
```json
{
  "rid": "RID-001",
  "matchingHistory": [
    {
      "resultId": "550e8400-e29b-41d4-a716-446655440000",
      "jobId": "660e8400-e29b-41d4-a716-446655440001",
      "role": "PROBE",
      "matchFound": true,
      "score": 45,
      "matchedWith": "RID-002",
      "processedAt": "2026-03-09T10:30:45Z"
    }
  ]
}
```

---

### 4.4 Delete Record

Securely delete a biometric record (GDPR compliance).

**Endpoint:** `DELETE / tenants/{tenantId}/records/{rid}`

**Request Body:**
```json
{
  "confirmedRid": "RID-001",
  "reason": "User request - GDPR right to erasure"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Record RID-001 deleted successfully",
  "deletedAt": "2026-03-09T10:30:00Z"
}
```

---

## 5. SSE (Server-Sent Events) Endpoints

### 5.1 Tenant Dashboard Events

Real-time updates for tenant dashboard.

**Endpoint:** `GET /tenants/{tenantId}/events`

**Event Types:**
- `job.completed` - New job completed
- `job.failed` - Job failed
- `queue.update` - Queue size changed
- `metrics.update` - Metrics updated

**Event Format:**
```json
{
  "type": "job.completed",
  "data": {
    "jobId": "550e8400-e29b-41d4-a716-446655440000",
    "status": "COMPLETED"
  },
  "timestamp": "2026-03-09T10:30:45Z"
}
```

---

## Implementation Notes

1. **Pagination:** All list endpoints use zero-based pagination
2. **Date Format:** ISO 8601 format (`YYYY-MM-DDTHH:mm:ssZ`)
3. **Error Handling:** Standard HTTP status codes (200, 400, 401, 403, 404, 500)
4. **Rate Limiting:** Headers: `X-RateLimit-Limit`, `X-RateLimit-Remaining`
5. **CORS:** Configure CORS for frontend origin

---

## OpenAPI Specification File

```yaml
openapi: 3.0.3
info:
  title: ScaleBiometrics Tenant API
  version: 1.0.0
  description: API for Tenant Dashboard - Epic 5.3

servers:
  - url: http://localhost:8080/api/v1
    description: Development server

paths:
  /tenants/{tenantId}/dashboard/kpis:
    get:
      summary: Get tenant dashboard KPIs
      parameters:
        - name: tenantId
          in: path
          required: true
          schema:
            type: string
      responses:
        '200':
          description: Successful response
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/DashboardKPIs'

  /tenants/{tenantId}/jobs:
    get:
      summary: List tenant jobs
      parameters:
        - name: tenantId
          in: path
          required: true
          schema:
            type: string
        - name: page
          in: query
          schema:
            type: integer
            default: 0
        - name: size
          in: query
          schema:
            type: integer
            default: 10
        - name: status
          in: query
          schema:
            type: string
        - name: type
          in: query
          schema:
            type: string
            enum: [1:N, 1:1]
        - name: priority
          in: query
          schema:
            type: string
            enum: [LOW, NORMAL, HIGH, URGENT]
      responses:
        '200':
          description: Paginated job list

  /tenants/{tenantId}/matching/results:
    get:
      summary: List matching results
      parameters:
        - name: tenantId
          in: path
          required: true
          schema:
            type: string
        - name: result
          in: query
          schema:
            type: string
            enum: [MATCH_FOUND, NO_MATCH]
      responses:
        '200':
          description: Paginated results list

  /tenants/{tenantId}/records:
    get:
      summary: List biometric records
      parameters:
        - name: tenantId
          in: path
          required: true
          schema:
            type: string
        - name: search
          in: query
          schema:
            type: string
      responses:
        '200':
          description: Paginated records list

  /tenants/{tenantId}/records/{rid}:
    get:
      summary: Get record details
      delete:
        summary: Delete record
        requestBody:
          content:
            application/json:
              schema:
                type: object
                properties:
                  confirmedRid:
                    type: string
                  reason:
                    type: string

components:
  schemas:
    DashboardKPIs:
      type: object
      properties:
        pendingQueue:
          type: integer
        processed24h:
          type: integer
        successRate:
          type: number
        avgWaitTime:
          type: integer
        activeWorkers:
          type: integer
```
