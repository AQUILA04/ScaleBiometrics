# Frontend Implementation Plan

**Status:** Draft
**Target Module:** `apps/web`
**Date:** 2025-12-25

## Overview
The `apps/web` module is the administrative console for the ScaleBiometrics platform. It provides real-time visibility into system performance, queue management, and configuration. Currently, it is a default Next.js starter project.

## 1. Project Setup & Dependencies
- **UI Framework**: Initialize Shadcn/UI.
  - Components: Button, Card, Table, Dialog, Input, Form, Badge, Alert, Skeleton.
- **Icons**: Install `lucide-react`.
- **Charts**: Install `recharts` for metrics visualization.
- **State Management**: Install `@tanstack/react-query` for server state and `zustand` for client state.
- **HTTP Client**: Install `axios` for API communication.
- **Auth**: Install `next-auth` for Keycloak integration.
- **Utilities**: `clsx`, `tailwind-merge`.

## 2. Directory Structure
Refactor `app/` and create `src/` (optional but recommended) or organize inside `app/`:
- `components/ui`: Shadcn components.
- `components/dashboard`: Dashboard-specific widgets.
- `components/queue`: Queue management tables.
- `components/layout`: Sidebar, Header, Shell.
- `lib`: Utility functions, API client setup.
- `hooks`: Custom React hooks (useAuth, useMetrics).
- `types`: TypeScript interfaces mirroring Backend DTOs.

## 3. Authentication (Keycloak)
- Configure `next-auth` with OIDC provider.
- Create `middleware.ts` to protect routes.
- Implement Login/Logout flow.
- Handle token refresh.

## 4. Core Pages Implementation

### Dashboard (`/dashboard`)
- **Summary Cards**: Total Requests, P95 Latency, Error Rate, Active Workers.
- **Throughput Chart**: Line chart showing req/sec over time (Recharts).
- **Health Status**: Visual indicators for API, Master, and Worker nodes.

### Queue Management (`/queue`)
- **Data Grid**: Table showing pending requests.
- **Columns**: Trace ID, Tenant, Status, Duration, Priority.
- **Actions**: "Escalate Priority", "Cancel Request".
- **Real-time**: Poll or SSE for updates.

### History & Audit (`/history`)
- **Searchable Table**: Filter by Trace ID, Date, Status.
- **Detail View**: Modal showing full match results (Probe vs Candidates).

### Settings (`/settings`)
- **API Keys**: Generate/Revoke keys.
- **Webhooks**: Configure callback URLs.

## 5. Biometric Visualization
- **Image Comparison**: Component to display Probe image side-by-side with Candidate image.
- **Minutiae Overlay**: (Optional) Canvas overlay to show matched points.

## 6. API Integration
- Create typed API client.
- Define endpoints:
  - `GET /api/v1/metrics`
  - `GET /api/v1/health`
  - `GET /api/v1/queue`
  - `POST /api/v1/matching/1n` (for testing from console)

## 7. Development Phases
1. **Phase 1**: Setup & Auth. Get a protected blank page.
2. **Phase 2**: Layout & Navigation. Sidebar and Header.
3. **Phase 3**: Dashboard with Mock Data.
4. **Phase 4**: Real API Integration.
5. **Phase 5**: Queue & History Views.
