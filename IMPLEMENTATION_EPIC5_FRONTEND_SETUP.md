# Epic 5 — Frontend Setup Summary

**Date:** 2026-04-21  
**Branch:** `develop`  
**Scope:** Story 5.1 — Frontend Project Setup & Architecture

---

## 1. What Was Done

### 1.1 Project Structure

The `apps/web/` Next.js application has been fully restructured following the BFF (Backend for Frontend) pattern defined in `docs/front-end-architecture.md`:

```
apps/web/
├── app/
│   ├── (auth)/
│   │   ├── layout.tsx              # Auth group layout (no sidebar)
│   │   └── login/page.tsx          # Login page → redirects to Keycloak
│   ├── (dashboard)/
│   │   ├── layout.tsx              # Protected layout (auth guard)
│   │   ├── dashboard/page.tsx      # Main dashboard with metric cards
│   │   ├── queue/page.tsx          # Queue management placeholder
│   │   ├── history/page.tsx        # Audit log placeholder
│   │   ├── configuration/page.tsx  # Config (API keys, webhooks) placeholder
│   │   ├── tenants/page.tsx        # Tenant management (superadmin only)
│   │   └── health/page.tsx         # System health placeholder
│   ├── api/
│   │   ├── auth/[...nextauth]/route.ts  # NextAuth.js v5 handler
│   │   └── proxy/[...path]/route.ts     # BFF proxy → Java backend
│   ├── layout.tsx                  # Root layout (Inter font, providers)
│   ├── globals.css                 # Design tokens + Tailwind layers
│   └── page.tsx                    # Root → redirect to /dashboard
├── components/
│   ├── ui/
│   │   ├── button.tsx              # Button with variants (CVA)
│   │   ├── card.tsx                # Card + CardHeader/Content/Footer
│   │   ├── input.tsx               # Input with error state
│   │   ├── badge.tsx               # Badge with semantic variants
│   │   ├── avatar.tsx              # Avatar (Radix UI)
│   │   ├── separator.tsx           # Separator (Radix UI)
│   │   ├── dropdown-menu.tsx       # DropdownMenu (Radix UI)
│   │   └── skeleton.tsx            # Skeleton loading state
│   ├── layout/
│   │   ├── sidebar.tsx             # Collapsible sidebar with nav
│   │   ├── topbar.tsx              # Header with user menu
│   │   └── dashboard-layout.tsx    # Shell: sidebar + topbar + content
│   └── providers/
│       └── query-provider.tsx      # TanStack Query provider
├── lib/
│   ├── utils.ts                    # cn(), formatNumber(), formatBytes()…
│   ├── auth.ts                     # NextAuth.js v5 + Keycloak config
│   └── api-client.ts               # Typed fetch wrapper (client + server)
├── types/
│   ├── index.ts                    # AppUser, Tenant, QueueJob, Metrics…
│   └── next-auth.d.ts              # Session type augmentation
├── middleware.ts                   # Route protection (public/private)
├── tailwind.config.ts              # Extended design tokens
├── .env.local.example              # Environment variables template
└── next.config.mjs                 # Standalone output + image config
```

---

## 2. Design System

### Color Palette

| Token | Value | Usage |
|-------|-------|-------|
| `--primary` | `#2563EB` | Actions, links, active states |
| `--primary-dark` | `#1E40AF` | Hover states |
| `--secondary` | `#10B981` | Success, positive indicators |
| `--accent` | `#F97316` | Warnings, highlights |
| `--background` | `#F8FAFC` | Page background |
| `--card` | `#FFFFFF` | Card surfaces |
| `--sidebar-bg` | `#0F172A` | Sidebar background |

### Typography

- **Font:** Inter (via `next/font/google`, CSS variable `--font-inter`)
- **Scale:** 2xs (10px) → 2xl (24px)
- **Weight:** 300, 400, 500, 600, 700

### Dark Mode

Full dark mode support via `.dark` class on `<html>` with semantic CSS variables.

---

## 3. Authentication (BFF Pattern)

- **Provider:** NextAuth.js v5 beta + Keycloak OIDC
- **Client:** `scalebiometrics-web` (public, PKCE S256)
- **Realm:** `scalebiometrics`
- **Session:** JWT strategy — access token stored server-side only
- **Proxy:** `/api/proxy/[...path]` attaches Bearer token before forwarding to Java API
- **Protection:** Middleware redirects unauthenticated users to `/login`

### Custom Claims Extracted

- `tenant_id` → `session.user.tenantId`
- `roles` → `session.user.roles` (`superadmin` | `tenant_admin` | `api_user`)

---

## 4. Keycloak Theme

**Location:** `infrastructure/keycloak/themes/scalebiometrics/`

### Login Theme (`login/`)

- Modern card-over-gradient design
- Blue gradient background (`#1E40AF` → `#0F172A`)
- White card with top accent bar (blue → emerald gradient)
- Fingerprint SVG icon in header
- Polished form controls with focus rings
- Alert styles (error, success, warning, info)
- Fully responsive (375px, 520px+)
- Respects `prefers-reduced-motion`

### Account Theme (`account/`)

- Consistent with login theme
- Inter font, blue primary color

---

## 5. Admin Template

### Sidebar

- Fixed left sidebar (`260px` / collapsible to `64px`)
- Dark background (`#0F172A`) with active state highlight
- Navigation items: Dashboard, File d'attente, Historique, Configuration, Tenants (superadmin), Santé système
- Collapse toggle button
- Role-based visibility (Tenants only for `superadmin`)

### TopBar

- Fixed header with page title
- Notification bell with indicator
- User avatar with dropdown menu (profile, settings, sign out)
- Displays user name, email, and role badge

---

## 6. Dependencies Added

| Package | Version | Purpose |
|---------|---------|---------|
| `next-auth` | `5.0.0-beta.31` | Authentication (Keycloak OIDC) |
| `@auth/core` | `0.34.3` | Auth core |
| `@tanstack/react-query` | `5.99.2` | Server state management |
| `lucide-react` | `1.8.0` | SVG icon library |
| `class-variance-authority` | `0.7.1` | Component variants |
| `clsx` | `2.1.1` | Class merging |
| `tailwind-merge` | `3.5.0` | Tailwind conflict resolution |
| `@radix-ui/react-avatar` | `1.1.11` | Accessible avatar |
| `@radix-ui/react-dropdown-menu` | `2.1.16` | Accessible dropdown |
| `@radix-ui/react-slot` | `1.2.4` | Polymorphic component slot |
| `@radix-ui/react-separator` | `1.1.8` | Accessible separator |
| `@radix-ui/react-tooltip` | `1.2.8` | Accessible tooltip |
| `@radix-ui/react-dialog` | `1.1.15` | Accessible dialog/modal |
| `recharts` | `3.8.1` | Data visualization (Epic 5.2) |

---

## 7. Next Steps (Epic 5.2+)

- [ ] Connect dashboard metrics to real API via TanStack Query
- [ ] Implement real-time queue data grid with SSE
- [ ] Add throughput and latency charts (Recharts)
- [ ] Implement audit log table with pagination
- [ ] Build API key management UI
- [ ] Build tenant onboarding form (superadmin)
- [ ] Add dark mode toggle
- [ ] Add MSW mocks for local development without backend
