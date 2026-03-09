# Epic 5.1 : Architecture & Socle Frontend (BFF) - Résumé d'Implémentation

**Date:** 2026-03-09  
**Status:** ✅ Complété

---

## Overview

Ce document détaille l'implémentation des 4 stories de l'Epic 5.1 relatives à l'architecture frontend Next.js avec le pattern BFF (Backend for Frontend).

---

## 5.1.1 : Initialisation du projet Next.js et du Design System

### Travail effectué

1. **Configuration du projet**
   - Next.js 14.2.33 avec App Router
   - TypeScript
   - Tailwind CSS 3.4. **Design System**
1

2.   - Configuration complète de Tailwind avec variables CSS pour les couleurs (Shadcn/UI)
   - Support natif du mode sombre (Dark Mode) via `next-themes`
   - Police Geist configurée (sans et mono)

3. **Utilitaires**
   - `lib/utils.ts` : Fonction `cn()` pour className conditionnelle
   - CVA (Class Variance Authority) pour les variants de composants

4. **Composants Shadcn/UI installés**
   - Button (avec variants)
   - Card (Header, Content, Footer, Title, Description)
   - Input
   - Label
   - Avatar
   - Dropdown Menu
   - Toast

---

## 5.1.2 : Intégration de l'authentification Keycloak (NextAuth)

### Travail effectué

1. **Configuration NextAuth.js v5**
   - Provider Keycloak configuré dans `lib/auth.ts`
   - Gestion des tokens (access, refresh, id)
   - Ajout des claims `role` et `tenantId` dans le token JWT

2. **Types TypeScript**
   - Extension du type `Session` dans `lib/auth.d.ts`
   - Propriétés : `accessToken`, `refreshToken`, `idToken`, `user.role`, `user.tenantId`

3. **Middleware de protection**
   - `middleware.ts` : Protège toutes les routes sauf `/login` et `/superadmin/login`

4. **Pages de login**
   - `/app/login/page.tsx` : Page de connexion Tenant
   - `/app/superadmin/login/page.tsx` : Page de connexion SuperAdmin
   - Utilisation de Suspense pour `useSearchParams`

5. **Redirections basées sur les rôles**
   - Layout `(dashboard)` : Redirige vers `/superadmin` si rôle `super_admin`
   - Layout `superadmin` : Redirige vers `/dashboard` si pas `super_admin`

---

## 5.1.3 : Configuration du client API et Proxy BFF

### Travail effectué

1. **Client API**
   - `lib/api-client.ts` : Wrapper fetch typé avec méthodes GET, POST, PUT, DELETE, PATCH
   - Gestion des erreurs (ApiError avec status HTTP)
   - Support du content-type JSON

2. **Proxy BFF**
   - `app/api/proxy/[...path]/route.ts` : Route catch-all qui transfère les requêtes
   - Attachment automatique du Bearer JWT depuis la session serveur
   - Forward des headers et du body
   - Configuration de l'URL du Gateway via `NEXT_PUBLIC_API_URL`

3. **TanStack Query v5**
   - `components/providers.tsx` : Provider QueryClient
   - Configuration du cache et staleTime

4. **Variables d'environnement**
   - `.env.example` créé avec les variables nécessaires :
     - `AUTH_SECRET`
     - `KEYCLOAK_CLIENT_ID`
     - `KEYCLOAK_CLIENT_SECRET`
     - `KEYCLOAK_ISSUER`
     - `NEXT_PUBLIC_API_URL`

---

## 5.1.4 : Implémentation des Layouts Principaux

### Travail effectué

1. **Header**
   - `components/layout/header.tsx`
   - Logo et navigation principale
   - Menu de notifications (icône avec badge)
   - Menu déroulant utilisateur (Profile, Settings, Sign Out)
   - Responsive : bouton menu burger sur mobile

2. **Sidebar**
   - `components/layout/sidebar.tsx`
   - Navigation pour Tenant (Dashboard, Queue, Records, Settings)
   - Navigation pour SuperAdmin (Dashboard, Tenants, Infrastructure, Workers, Audit Logs)
   - Section Settings pour les tenants (API Keys, Webhooks)

3. **AppLayout**
   - `components/layout/app-layout.tsx`
   - Composition : Header + Sidebar + Main content
   - Détection du type d'utilisateur via `useSession`

4. **Structure des routes**
   - `(dashboard)/` : Routes protégées Tenant avec AppLayout
   - `superadmin/` : Routes protégées SuperAdmin avec AppLayout

---

## Fichiers créés/modifiés

```
apps/web/
├── app/
│   ├── layout.tsx                    # Root layout avec providers
│   ├── page.tsx                      # Page d'accueil (redirect)
│   ├── login/
│   │   └── page.tsx                  # Page login tenant
│   ├── (dashboard)/
│   │   ├── layout.tsx                # Layout avec protection + AppLayout
│   │   └── page.tsx                  # Dashboard tenant
│   ├── superadmin/
│   │   ├── layout.tsx                # Layout avec protection + AppLayout
│   │   ├── page.tsx                  # Dashboard superadmin
│   │   └── login/
│   │       └── page.tsx              # Page login superadmin
│   └── api/
│       ├── auth/[...nextauth]/
│       │   └── route.ts              # Endpoints NextAuth
│       └── proxy/[...path]/
│           └── route.ts               # Proxy BFF
├── components/
│   ├── layout/
│   │   ├── app-layout.tsx            # Layout principal
│   │   ├── header.tsx                # Header avec navigation
│   │   └── sidebar.tsx               # Sidebar navigation
│   ├── providers.tsx                 # TanStack Query provider
│   ├── session-provider.tsx          # NextAuth session provider
│   ├── theme-provider.tsx            # Dark mode provider
│   └── ui/                           # Composants Shadcn
│       ├── avatar.tsx
│       ├── button.tsx
│       ├── card.tsx
│       ├── dropdown-menu.tsx
│       ├── input.tsx
│       ├── label.tsx
│       └── toast.tsx
├── lib/
│   ├── api-client.ts                  # Client API fetch
│   ├── auth.ts                       # Configuration NextAuth
│   ├── auth.d.ts                      # Types Session augmentés
│   └── utils.ts                      # Utilitaire cn()
├── middleware.ts                      # Protection des routes
├── package.json                      # Dépendances
├── tailwind.config.ts                # Configuration Tailwind
└── .env.example                      # Variables d'environnement
```

---

## Dépendances installées

```json
{
  "dependencies": {
    "next": "14.2.33",
    "next-auth": "^5.0.0-beta.25",
    "@auth/core": "^0.30.0",
    "@tanstack/react-query": "^5.51.1",
    "class-variance-authority": "^0.7.0",
    "clsx": "^2.1.1",
    "tailwind-merge": "^2.4.0",
    "lucide-react": "^0.408.0",
    "next-themes": "^0.3.0",
    "sonner": "^1.5.0",
    "zod": "^3.23.8",
    "react-hook-form": "^7.52.1",
    "@hookform/resolvers": "^3.9.0",
    "@radix-ui/react-*": "^1.1.1"
  }
}
```

---

## Prochaines étapes

Pour continuer vers l'Epic 5.2 (SuperAdmin Console) :

1. **5.2.1** : Dashboard SuperAdmin avec KPIs et graphiques (Recharts)
2. **5.2.2** : Liste des Tenants avec DataTable
3. **5.2.3** : Wizard de création de Tenant
4. **5.2.4** : Page de détails d'un Tenant

---

## Notes

- Le build a été validé avec succès (`npm run build`)
- L'erreur de copy pour le standalone mode est un warning non-bloquant
- Les pages utilisant `useSearchParams` sont wrappées dans Suspense
- Les types NextAuth ont été ajustés avec des assertions manuelles en raison de limitations de TypeScript avec les modules Augmentation
