# ScaleBiometrics - Frontend Epics & User Stories

**Version:** 1.0  
**Date:** 2026-02-05  
**Author:** Manus AI  

Ce document définit les Epics et User Stories nécessaires pour le développement du frontend de la plateforme ScaleBiometrics, basé sur la spécification visuelle, le PRD et l'architecture BFF (Next.js).

---

## Epic 5.1 : Architecture & Socle Frontend (BFF)
*Mise en place de l'architecture Next.js, du design system et de la gestion de l'authentification via Keycloak.*

### Story 5.1.1 : Initialisation du projet Next.js et du Design System
**En tant que** développeur frontend,
**Je veux** configurer le projet Next.js avec Tailwind CSS et Shadcn/UI,
**Afin de** disposer d'une base solide et conforme à la spécification visuelle pour le développement des composants.

**Critères d'acceptation :**
- [ ] Projet Next.js 14+ (App Router) initialisé dans `apps/web`.
- [ ] Tailwind CSS configuré avec les design tokens (couleurs, typographie Geist, espacements).
- [ ] Utilitaires `cn()` et CVA configurés.
- [ ] Composants Shadcn/UI de base installés (Button, Card, Input, Label).
- [ ] Support du mode sombre (Dark Mode) natif implémenté.

### Story 5.1.2 : Intégration de l'authentification Keycloak (NextAuth)
**En tant que** développeur frontend,
**Je veux** intégrer NextAuth.js avec le provider Keycloak,
**Afin de** sécuriser l'application via le pattern BFF (Backend for Frontend) sans exposer les tokens au client.

**Critères d'acceptation :**
- [ ] Configuration de NextAuth.js avec Keycloak.
- [ ] Gestion des sessions via cookies HttpOnly cryptés.
- [ ] Création du middleware pour protéger les routes (`app/(dashboard)`).
- [ ] Implémentation des pages de login (`/login` et `/superadmin/login`).
- [ ] Redirection basée sur les rôles (Tenant Admin vs SuperAdmin).

### Story 5.1.3 : Configuration du client API et Proxy BFF
**En tant que** développeur frontend,
**Je veux** configurer un client API (TanStack Query) et des routes proxy Next.js,
**Afin de** communiquer avec le backend Java en attachant automatiquement le token JWT.

**Critères d'acceptation :**
- [ ] Création de `lib/api-client.ts` avec fetch typé.
- [ ] Configuration des API Routes dans `app/api/proxy/` pour transférer les requêtes vers le Gateway.
- [ ] Attachement automatique du Bearer JWT récupéré depuis la session serveur.
- [ ] Configuration de TanStack Query v5 pour la gestion de l'état asynchrone et du cache.

### Story 5.1.4 : Implémentation des Layouts Principaux
**En tant que** développeur frontend,
**Je veux** implémenter les layouts globaux (SuperAdmin et Tenant),
**Afin de** fournir la structure de navigation (Header, Sidebars) pour toutes les pages.

**Critères d'acceptation :**
- [ ] Création du `Top Header` avec Logo, Breadcrumb, Notifications et Profile Menu.
- [ ] Création de la `Primary Sidebar` (icônes) et `Secondary Sidebar` (contextuelle).
- [ ] Implémentation du responsive design (mobile-first) avec un drawer pour la navigation sur mobile.
- [ ] Création du composant `AppLayout` réutilisable.

---

## Epic 5.2 : SuperAdmin Console
*Développement des interfaces dédiées aux administrateurs globaux pour gérer la plateforme et les tenants.*

### Story 5.2.1 : Dashboard Global SuperAdmin
**En tant que** SuperAdmin,
**Je veux** visualiser un tableau de bord global,
**Afin de** surveiller la santé du système, les performances de matching et l'état des tenants en temps réel.

**Critères d'acceptation :**
- [ ] Affichage des KPI cards (Total Tenants, Active Workers, Latency P95, Throughput).
- [ ] Intégration de Recharts pour les graphiques de throughput et de latence.
- [ ] Connexion aux Server-Sent Events (SSE) pour les mises à jour en temps réel.
- [ ] Affichage du statut de santé des services (API Gateway, PostgreSQL, Kafka).

### Story 5.2.2 : Liste et Gestion des Tenants
**En tant que** SuperAdmin,
**Je veux** consulter et gérer la liste des tenants,
**Afin de** pouvoir les suspendre, les supprimer ou consulter leurs détails.

**Critères d'acceptation :**
- [ ] Implémentation du composant `DataTable` avec pagination, tri et filtres côté serveur.
- [ ] Affichage de la liste des tenants avec leurs statuts (badges).
- [ ] Implémentation des actions par ligne (View, Edit, Suspend, Delete).
- [ ] Création des modales de confirmation pour les actions destructrices (Suspend, Delete).

### Story 5.2.3 : Wizard de Création de Tenant (Saga)
**En tant que** SuperAdmin,
**Je veux** utiliser un assistant (wizard) pour créer un nouveau tenant,
**Afin de** provisionner correctement toutes les ressources nécessaires (DB, Storage, Keycloak).

**Critères d'acceptation :**
- [ ] Création du composant Wizard multi-étapes (Basic Info, Tech Config, Limits, Review).
- [ ] Validation des formulaires à chaque étape avec `react-hook-form` et `zod`.
- [ ] Intégration de l'appel API pour lancer la Saga d'onboarding.
- [ ] Affichage d'une modale de progression avec les étapes de la Saga (Creating DB schema, Provisioning bucket, etc.).

### Story 5.2.4 : Détails et Configuration d'un Tenant
**En tant que** SuperAdmin,
**Je veux** consulter les détails complets d'un tenant spécifique,
**Afin de** surveiller son utilisation, ses limites et modifier sa configuration.

**Critères d'acceptation :**
- [ ] Création de la page de détails avec des onglets (Overview, Statistics, Configuration, Users).
- [ ] Affichage des jauges d'utilisation (Requests, Storage, Users).
- [ ] Formulaire d'édition de la configuration technique et des quotas.

---

## Epic 5.3 : Tenant Dashboard - Gestion Opérationnelle
*Développement des interfaces pour les administrateurs de tenant afin de gérer leurs opérations biométriques.*

### Story 5.3.1 : Dashboard Tenant
**En tant qu'** administrateur de tenant,
**Je veux** visualiser un tableau de bord spécifique à mon organisation,
**Afin de** suivre ma file d'attente, mon taux de succès et mes performances.

**Critères d'acceptation :**
- [ ] Affichage des KPI spécifiques au tenant (Pending Queue, Processed 24h, Success Rate).
- [ ] Graphiques de throughput et de temps d'attente moyen.
- [ ] Liste des 10 derniers jobs traités.
- [ ] Données mises à jour en temps réel via SSE.

### Story 5.3.2 : Gestion de la File d'Attente (Queue)
**En tant qu'** administrateur de tenant,
**Je veux** visualiser et gérer la file d'attente des requêtes biométriques,
**Afin de** pouvoir escalader la priorité des jobs importants ou annuler des requêtes bloquées.

**Critères d'acceptation :**
- [ ] Grille de données (DataTable) affichant les jobs en attente et en cours.
- [ ] Filtres par statut, type (1:N, 1:1) et priorité.
- [ ] Modale de détails d'un job avec le payload JSON et la timeline des statuts.
- [ ] Action d'escalade de priorité (modale de confirmation).

### Story 5.3.3 : Visualisation des Résultats de Matching
**En tant qu'** administrateur de tenant,
**Je veux** consulter l'historique et les détails des résultats de matching,
**Afin de** vérifier la précision de l'algorithme et examiner les faux positifs/négatifs.

**Critères d'acceptation :**
- [ ] Liste historique des résultats avec filtres (Match Found, No Match).
- [ ] Page de détails d'un résultat (Split view : Probe vs Candidates).
- [ ] Affichage des images d'empreintes digitales (chargement lazy depuis MinIO).
- [ ] Affichage du score de confiance et des métadonnées de traitement.

### Story 5.3.4 : Explorateur d'Enregistrements Biométriques (Records)
**En tant qu'** administrateur de tenant,
**Je veux** naviguer dans la base de données des identités enregistrées,
**Afin de** consulter ou supprimer des données biométriques pour des raisons de conformité (RGPD).

**Critères d'acceptation :**
- [ ] Liste des enregistrements avec recherche par RID.
- [ ] Page de détails d'un enregistrement affichant toutes les empreintes associées.
- [ ] Historique des requêtes de matching liées à ce RID.
- [ ] Modale de suppression sécurisée avec confirmation explicite du RID.

---

## Epic 5.4 : Tenant Dashboard - Intégration & Configuration
*Développement des interfaces pour gérer l'intégration API et la configuration du tenant.*

### Story 5.4.1 : Gestion des Clés API
**En tant qu'** administrateur de tenant,
**Je veux** créer et révoquer des clés API,
**Afin de** permettre à mes applications de communiquer avec la plateforme ScaleBiometrics.

**Critères d'acceptation :**
- [ ] Liste des clés API actives et révoquées.
- [ ] Formulaire de création avec sélection des permissions (scopes) et expiration.
- [ ] Affichage de la clé générée une seule fois avec obligation de la copier.
- [ ] Action de révocation immédiate avec modale de confirmation.

### Story 5.4.2 : Configuration des Webhooks
**En tant qu'** administrateur de tenant,
**Je veux** configurer des webhooks,
**Afin de** recevoir des notifications asynchrones lors de la complétion des jobs de matching.

**Critères d'acceptation :**
- [ ] Formulaire de création de webhook (URL, événements abonnés, politique de retry).
- [ ] Liste des webhooks avec leur statut de santé.
- [ ] Modale de test permettant d'envoyer un payload fictif et de voir la réponse HTTP.
- [ ] Historique des appels webhook (Delivery History) avec les codes de réponse.

### Story 5.4.3 : Paramètres du Tenant (Settings)
**En tant qu'** administrateur de tenant,
**Je veux** configurer les paramètres globaux de mon environnement,
**Afin de** personnaliser le comportement de l'algorithme et la politique de rétention.

**Critères d'acceptation :**
- [ ] Formulaire pour les paramètres généraux (Nom, Logo, Contact).
- [ ] Formulaire de configuration du matching (Seuil de confiance, Top-K, Activation ANN).
- [ ] Formulaire de configuration du stockage (Politique de rétention, Compression).
- [ ] Sauvegarde avec validation des données.

---

## Epic 5.5 : Monitoring de l'Infrastructure (SuperAdmin)
*Développement des interfaces de surveillance de l'infrastructure technique de la plateforme.*

### Story 5.5.1 : Monitoring de l'Infrastructure (System Health)
**En tant que** SuperAdmin,
**Je veux** surveiller l'état de l'infrastructure en temps réel,
**Afin de** détecter et diagnostiquer les incidents de performance sur les services critiques.

**Critères d'acceptation :**
- [ ] Affichage des métriques PostgreSQL (Active Connections, QPS, Avg Latency, DB Size).
- [ ] Affichage des métriques Redis (Memory Usage, Hit Rate, Connected Clients).
- [ ] Affichage des métriques Kafka (Consumer Lag, Messages/s, Topic Partitions).
- [ ] Affichage des métriques MinIO (Storage Used, Objects Count, Bandwidth).
- [ ] Navigation par onglets entre les différents services.
- [ ] Graphiques de throughput (barres) et de latence (courbes) sur les dernières 24h.
- [ ] Affichage du Cluster Health (gauge circulaire) et de l'état des réplicas.
- [ ] Liste des Slow Queries et des alertes récentes.

### Story 5.5.2 : Monitoring du Biometric Engine (SuperAdmin)
**En tant que** SuperAdmin,
**Je veux** surveiller les noeuds de matching (Workers),
**Afin de** gérer la capacité et détecter les noeuds défaillants.

**Critères d'acceptation :**
- [ ] Liste des Worker Nodes avec leur statut (Healthy, Degraded, Offline).
- [ ] Métriques par noeud (CPU, RAM, Templates chargés, Requêtes traitées).
- [ ] Graphique de distribution de la charge entre les noeuds.
- [ ] Action de drain/restart d'un noeud avec confirmation.

### Story 5.5.3 : Audit Logs (SuperAdmin)
**En tant que** SuperAdmin,
**Je veux** consulter les journaux d'audit de la plateforme,
**Afin de** retracer les actions des administrateurs et détecter les activités suspectes.

**Critères d'acceptation :**
- [ ] Table de logs avec filtres (Actor, Action, Tenant, Date Range).
- [ ] Affichage du détail d'un log (Before/After state en JSON).
- [ ] Export CSV des logs filtrés.
- [ ] Pagination côté serveur.

---

## Epic 5.6 : Composants Transversaux & Qualité
*Développement des composants partagés et mise en place de la stratégie de tests frontend.*

### Story 5.6.1 : Bibliothèque de Composants Partagés
**En tant que** développeur frontend,
**Je veux** disposer d'une bibliothèque de composants réutilisables,
**Afin de** garantir la cohérence visuelle et accélérer le développement de nouvelles pages.

**Critères d'acceptation :**
- [ ] Composant `MetricCard` (KPI Card avec trend).
- [ ] Composant `StatusBadge` (Active, Pending, Error, Processing).
- [ ] Composant `DataTable` (tri, pagination, filtres, actions par ligne, sélection multiple).
- [ ] Composant `Wizard` (multi-étapes avec stepper horizontal/vertical).
- [ ] Composant `ConfirmationModal` (générique pour les actions destructrices).
- [ ] Composant `EmptyState` (illustration + message + CTA).
- [ ] Composant `PageHeader` (titre, description, actions).

### Story 5.6.2 : Gestion des Notifications (Toast & Alerts)
**En tant qu'** utilisateur,
**Je veux** recevoir des retours visuels clairs pour toutes mes actions,
**Afin de** savoir si mes opérations ont réussi ou échoué.

**Critères d'acceptation :**
- [ ] Intégration du système de Toast (Sonner) pour les confirmations et erreurs.
- [ ] Composant `AlertBanner` pour les alertes persistantes en haut de page.
- [ ] Gestion des erreurs API globale avec affichage de toasts d'erreur.
- [ ] Composant `ErrorBoundary` pour les erreurs de rendu React.

### Story 5.6.3 : Internationalisation (i18n)
**En tant qu'** utilisateur,
**Je veux** utiliser l'application dans ma langue,
**Afin de** bénéficier d'une expérience localisée.

**Critères d'acceptation :**
- [ ] Configuration de `next-intl` avec les locales `en` et `fr`.
- [ ] Extraction de toutes les chaînes de caractères dans des fichiers de traduction.
- [ ] Sélecteur de langue dans le header.
- [ ] Formatage des dates, nombres et monnaies selon la locale.

### Story 5.6.4 : Tests Frontend et CI
**En tant que** développeur frontend,
**Je veux** disposer d'une suite de tests automatisés,
**Afin de** garantir la non-régression lors des évolutions de l'application.

**Critères d'acceptation :**
- [ ] Configuration de Vitest et React Testing Library.
- [ ] Tests unitaires pour les composants partagés (MetricCard, StatusBadge, DataTable).
- [ ] Tests d'intégration pour les formulaires (Wizard de création de tenant).
- [ ] Configuration de Playwright pour les tests E2E des parcours critiques (Login, Create Tenant).
- [ ] Intégration dans le workflow CI GitHub Actions (`frontend-ci.yml`).
- [ ] Couverture de code cible : 70%.

---

## Récapitulatif des Epics

| Epic | Titre | Nb. Stories | Priorité |
|------|-------|-------------|----------|
| **5.1** | Architecture & Socle Frontend (BFF) | 4 | Critique |
| **5.2** | SuperAdmin Console | 4 | Haute |
| **5.3** | Tenant Dashboard - Gestion Opérationnelle | 4 | Haute |
| **5.4** | Tenant Dashboard - Intégration & Configuration | 3 | Moyenne |
| **5.5** | Monitoring de l'Infrastructure (SuperAdmin) | 3 | Moyenne |
| **5.6** | Composants Transversaux & Qualité | 4 | Haute |
| **Total** | | **22 stories** | |

---

## Dépendances et Ordre de Développement Recommandé

Le développement doit suivre l'ordre suivant pour maximiser la valeur livrée et minimiser les blocages :

**Sprint 1 (Fondations) :** 5.1.1 → 5.1.2 → 5.1.3 → 5.1.4 → 5.6.1
**Sprint 2 (SuperAdmin Core) :** 5.2.1 → 5.2.2 → 5.2.3 → 5.6.2
**Sprint 3 (Tenant Core) :** 5.3.1 → 5.3.2 → 5.2.4 → 5.3.3
**Sprint 4 (Intégration & Monitoring) :** 5.4.1 → 5.4.2 → 5.5.1 → 5.3.4
**Sprint 5 (Finalisation) :** 5.4.3 → 5.5.2 → 5.5.3 → 5.6.3 → 5.6.4
