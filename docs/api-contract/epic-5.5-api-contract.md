# API Contract - Frontend 5.5 Endpoints

**Generated:** 2026-03-09  
**Purpose:** Contract between Frontend (Epic 5.5) and Backend implementations

---

## Overview

This document defines the OpenAPI 3.0 specifications for all backend endpoints required by the Infrastructure Monitoring (Epic 5.5).

---

## Base Configuration

- **Base URL:** `/api/v1/superadmin`
- **Authentication:** Bearer Token (JWT with super_admin role)
- **Content-Type:** `application/json`

---

## 1. Infrastructure Monitoring Endpoints

### 1.1 Get PostgreSQL Metrics

**Endpoint:** `GET /infrastructure/postgres/metrics`

**Response:**
```json
{
  "activeConnections": 45,
  "maxConnections": 100,
  "qps": 1250,
  "avgLatencyMs": 8,
  "dbSize": 2500000000,
  "slowQueries": [
    {
      "query": "SELECT * FROM biometric_records WHERE...",
      "duration": 2500,
      "timestamp": "2026-03-09T10:30:00Z"
    }
  ],
  "replicationLag": 0,
  "uptime": "99.99%"
}
```

---

### 1.2 Get Redis Metrics

**Endpoint:** `GET /infrastructure/redis/metrics`

**Response:**
```json
{
  "memoryUsed": 524288000,
  "memoryTotal": 1073741824,
  "hitRate": 98.5,
  "connectedClients": 25,
  "keysCount": 15000,
  "opsPerSecond": 5000,
  "uptime": "99.99%"
}
```

---

### 1.3 Get Kafka Metrics

**Endpoint:** `GET /infrastructure/kafka/metrics`

**Response:**
```json
{
  "brokers": [
    {
      "id": 0,
      "host": "kafka-0",
      "port": 9092,
      "status": "ONLINE"
    }
  ],
  "consumerLag": 150,
  "messagesPerSecond": 2500,
  "topics": [
    {
      "name": "biometric-jobs",
      "partitions": 12,
      "replicationFactor": 3,
      "messages": 150000
    }
  ],
  "underReplicatedPartitions": 0,
  "offlinePartitions": 0
}
```

---

### 1.4 Get MinIO Metrics

**Endpoint:** `GET /infrastructure/minio/metrics`

**Response:**
```json
{
  "storageUsed": 156000000000,
  "storageTotal": 500000000000,
  "objectsCount": 250000,
  "bucketsCount": 25,
  "uploadSpeed": 50000000,
  "downloadSpeed": 100000000,
  "s3RequestsTotal": 1500000,
  "s3RequestsFailed": 150
}
```

---

### 1.5 Get Cluster Health

**Endpoint:** `GET /infrastructure/cluster/health`

**Response:**
```json
{
  "overallStatus": "healthy",
  "score": 98,
  "services": [
    {
      "name": "PostgreSQL",
      "status": "healthy",
      "uptime": 99.99
    },
    {
      "name": "Redis",
      "status": "healthy",
      "uptime": 99.99
    },
    {
      "name": "Kafka",
      "status": "healthy",
      "uptime": 99.95
    },
    {
      "name": "MinIO",
      "status": "degraded",
      "uptime": 99.50
    }
  ],
  "lastChecked": "2026-03-09T10:30:00Z"
}
```

---

### 1.6 Get Infrastructure Alerts

**Endpoint:** `GET /infrastructure/alerts`

**Response:**
```json
{
  "alerts": [
    {
      "id": "alert-001",
      "severity": "warning",
      "service": "MinIO",
      "message": "High disk usage detected",
      "timestamp": "2026-03-09T10:30:00Z",
      "acknowledged": false
    }
  ]
}
```

---

## 2. Worker Nodes Endpoints

### 2.1 List Worker Nodes

**Endpoint:** `GET /workers`

**Response:**
```json
{
  "workers": [
    {
      "id": "worker-001",
      "hostname": "worker-node-1",
      "ip": "10.0.1.10",
      "status": "HEALTHY",
      "cpu": 45,
      "memory": 62,
      "memoryTotal": 16000000000,
      "templatesLoaded": 50000,
      "requestsProcessed": 150000,
      "uptime": "99.99%",
      "lastHeartbeat": "2026-03-09T10:30:00Z"
    }
  ]
}
```

---

### 2.2 Get Worker Details

**Endpoint:** `GET /workers/{workerId}`

**Response:**
```json
{
  "id": "worker-001",
  "hostname": "worker-node-1",
  "ip": "10.0.1.10",
  "status": "HEALTHY",
  "cpu": 45,
  "memory": 62,
  "memoryTotal": 16000000000,
  "templatesLoaded": 50000,
  "requestsProcessed": 150000,
  "currentJobs": 3,
  "uptime": "99.99%",
  "lastHeartbeat": "2026-03-09T10:30:00Z",
  "metrics": {
    "avgLatency": 125,
    "p95Latency": 250,
    "successRate": 98.5
  }
}
```

---

### 2.3 Drain Worker

Mark a worker for graceful shutdown.

**Endpoint:** `POST /workers/{workerId}/drain`

**Response:**
```json
{
  "success": true,
  "message": "Worker marked for draining"
}
```

---

### 2.4 Restart Worker

Restart a worker node.

**Endpoint:** `POST /workers/{workerId}/restart`

**Response:**
```json
{
  "success": true,
  "message": "Worker restart initiated"
}
```

---

## 3. Audit Logs Endpoints

### 3.1 List Audit Logs

**Endpoint:** `GET /audit-logs`

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| page | integer | Page number |
| size | integer | Page size |
| actor | string | Filter by actor |
| action | string | Filter by action |
| tenantId | string | Filter by tenant |
| fromDate | string | Filter from date |
| toDate | string | Filter to date |

**Response:**
```json
{
  "logs": [
    {
      "id": "log-001",
      "actor": "admin@scalebiometrics.com",
      "actorRole": "SUPER_ADMIN",
      "action": "TENANT_CREATED",
      "tenantId": "acme-corp",
      "resource": "tenant",
      "resourceId": "acme-corp",
      "details": {
        "name": "Acme Corp"
      },
      "ipAddress": "10.0.0.50",
      "timestamp": "2026-03-09T10:30:00Z"
    }
  ],
  "totalElements": 5000,
  "totalPages": 500,
  "size": 10,
  "number": 0
}
```

---

### 3.2 Get Audit Log Details

**Endpoint:** `GET /audit-logs/{logId}`

**Response:**
```json
{
  "id": "log-001",
  "actor": "admin@scalebiometrics.com",
  "actorRole": "SUPER_ADMIN",
  "action": "TENANT_UPDATED",
  "tenantId": "acme-corp",
  "resource": "tenant",
  "resourceId": "acme-corp",
  "details": {
    "name": "Acme Corp",
    "updatedFields": ["maxRecords"]
  },
  "changes": {
    "before": {
      "maxRecords": 100000
    },
    "after": {
      "maxRecords": 250000
    }
  },
  "ipAddress": "10.0.0.50",
  "timestamp": "2026-03-09T10:30:00Z"
}
```

---

### 3.3 Export Audit Logs

**Endpoint:** `GET /audit-logs/export`

**Query Parameters:** Same as list

**Response:** CSV file download

---

## Available Audit Actions

| Action | Description |
|--------|-------------|
| TENANT_CREATED | New tenant created |
| TENANT_UPDATED | Tenant configuration updated |
| TENANT_SUSPENDED | Tenant suspended |
| TENANT_DELETED | Tenant deleted |
| WORKER_DRAINED | Worker node drained |
| WORKER_RESTARTED | Worker node restarted |
| SETTINGS_UPDATED | Platform settings updated |
| API_KEY_CREATED | API key created |
| API_KEY_REVOKED | API key revoked |
| WEBHOOK_CREATED | Webhook created |
| WEBHOOK_DELETED | Webhook deleted |

---

## OpenAPI Specification Summary

```yaml
paths:
  /infrastructure/postgres/metrics:
    get:
      summary: Get PostgreSQL metrics

  /infrastructure/redis/metrics:
    get:
      summary: Get Redis metrics

  /infrastructure/kafka/metrics:
    get:
      summary: Get Kafka metrics

  /infrastructure/minio/metrics:
    get:
      summary: Get MinIO metrics

  /infrastructure/cluster/health:
    get:
      summary: Get cluster health status

  /infrastructure/alerts:
    get:
      summary: Get infrastructure alerts

  /workers:
    get:
      summary: List worker nodes

  /workers/{workerId}:
    get:
      summary: Get worker details

  /workers/{workerId}/drain:
    post:
      summary: Drain worker node

  /workers/{workerId}/restart:
    post:
      summary: Restart worker node

  /audit-logs:
    get:
      summary: List audit logs

  /audit-logs/{logId}:
    get:
      summary: Get audit log details

  /audit-logs/export:
    get:
      summary: Export audit logs as CSV
```

---

## Implementation Notes

1. **Metrics Refresh**: Frontend should poll every 10-30 seconds
2. **Real-time**: Consider SSE for live metric updates
3. **Slow Queries**: Return last 10 slowest queries
4. **Worker Actions**: Require confirmation dialog
5. **Audit Log Retention**: Consider pagination for large datasets
