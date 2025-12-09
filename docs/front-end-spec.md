# UI/UX Specification: ScaleBiometrics Console

## 1. Introduction
Admin console for Superadmins and Tenant Admins to monitor queues, fraud, and system health.

## 2. Technology & Design
* **Framework:** Next.js 14+ (App Router).
* **UI Kit:** Shadcn/UI + Tailwind CSS.
* **Data Viz:** Recharts.
* **Real-time:** Server-Sent Events (SSE).

## 3. Information Architecture
* **Dashboard:** Synthetic view (Health, Throughput).
* **Queue:** Live Data Grid of pending jobs.
* **History:** Audit logs.
* **Configuration:** API Keys, Webhooks.

## 4. Key Screens
* **Global Dashboard:** Metrics (Pending count, Latency P95), Throughput Chart.
* **Queue Manager:** Table with actions (Escalate, Retry).
* **Biometric Result:** Split view (Probe vs Candidates) with match score.

## 5. Accessibility
* WCAG AA Compliance.
* Dark Mode native support.