# Frontend Architecture: Next.js BFF

## 1. Summary
Next.js Application implementing **Backend for Frontend (BFF)** pattern. Proxies requests to Java Backend, managing Keycloak tokens via server-side sessions/cookies.

## 2. Tech Stack
* Next.js 14 (App Router)
* TypeScript
* NextAuth.js v5 (Keycloak Provider)
* TanStack Query v5
* Tailwind CSS / Shadcn UI

## 3. Security (BFF Pattern)
* Browser holds **HttpOnly Cookie** (encrypted session).
* Next.js API Routes decrypt cookie, attach **Bearer JWT**, and forward to Java Gateway.
* Tokens are NEVER exposed to client-side JS.

## 4. Project Structure
`apps/web/`
* `app/(dashboard)/`: Protected routes.
* `app/api/auth/`: NextAuth endpoints.
* `app/api/proxy/`: BFF Proxy endpoints.
* `lib/api-client.ts`: Typed fetch wrapper.

## 5. Development
* **Mocking:** MSW for local dev without backend.
* **Codegen:** OpenAPI to TypeScript generation.