# API Contract: Tenant Management

**Version:** `1.0.0`
**Status:** `Draft`
**Last Updated:** `2025-12-07`

This document defines the API contract for managing tenants in the ScaleBiometrics platform. All endpoints are prefixed with `/api/v1/tenants`.

## Endpoints

### 1. Create Tenant

- **Endpoint:** `POST /`
- **Permission:** `superadmin`
- **Description:** Creates a new tenant, which includes creating a new schema in the database and a corresponding client in Keycloak.

**Request Body:**

```json
{
  "name": "New Tenant Name",
  "adminEmail": "admin@newtenant.com",
  "plan": "standard"
}
```

**Response (201 Created):**

```json
{
  "id": "a1b2c3d4-e5f6-7890-1234-567890abcdef",
  "name": "New Tenant Name",
  "schemaName": "t_a1b2c3d4e5f678901234567890abcdef",
  "status": "ACTIVE",
  "createdAt": "2025-12-07T10:00:00Z"
}
```

### 2. Get Tenant by ID

- **Endpoint:** `GET /{tenantId}`
- **Permission:** `superadmin` or `tenant_admin` (if `tenantId` matches user's tenant)
- **Description:** Retrieves details for a specific tenant.

**Response (200 OK):**

```json
{
  "id": "a1b2c3d4-e5f6-7890-1234-567890abcdef",
  "name": "New Tenant Name",
  "schemaName": "t_a1b2c3d4e5f678901234567890abcdef",
  "status": "ACTIVE",
  "createdAt": "2025-12-07T10:00:00Z",
  "config": {
    "maxUsers": 100,
    "maxStorageGb": 100,
    "matchingThreshold": 40
  }
}
```

### 3. List Tenants

- **Endpoint:** `GET /`
- **Permission:** `superadmin`
- **Description:** Retrieves a paginated list of all tenants.

**Query Parameters:**

- `page` (integer, default: 0)
- `size` (integer, default: 20)
- `sort` (string, default: `name,asc`)
- `status` (string, optional)

**Response (200 OK):**

```json
{
  "content": [
    {
      "id": "a1b2c3d4-e5f6-7890-1234-567890abcdef",
      "name": "New Tenant Name",
      "status": "ACTIVE"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

## Changelog

- **v1.0.0 (2025-12-07):** Initial draft.
