# Plan d'Implémentation : Matching Asynchrone & Webhooks

Ce document détaille le plan technique pour introduire un flux de traitement asynchrone (FIFO) dans l'architecture ScaleBiometrics, tout en conservant les endpoints synchrones existants.

## 1. Objectifs
*   **Haute Disponibilité & Résilience** : Gérer des pics de charge ("millions de requêtes") sans saturer les workers.
*   **Traitement Séquentiel (FIFO)** : Garantir que les requêtes lourdes sont traitées l'une après l'autre par le Master.
*   **Notification Webhook** : Notifier le Tenant via une `callbackUrl` une fois le traitement terminé.
*   **Approche Hybride** :
    *   `/api/matching/*` : Reste synchrone (gRPC direct) pour les besoins temps réel critiques.
    *   `/api/matching/async/*` : Nouveaux endpoints pour le traitement en file d'attente.

## 2. Architecture des Flux

### Flux Asynchrone
1.  **Client** -> `POST /api/matching/async/1n` -> **apps/api**
2.  **apps/api** :
    *   Récupère le Tenant via JWT.
    *   Valide la requête.
    *   Persiste la demande en BDD (`MatchRequest`, status: `PENDING`).
    *   Push l'événement dans Kafka (`matching-requests`).
    *   Retourne `202 Accepted` + `requestId`.
3.  **apps/master** :
    *   Consomme Kafka (`matching-requests`) de manière séquentielle (Blocking Consumer).
    *   Appelle `ScatterGatherOrchestrator` (traitement métier).
    *   Push le résultat dans Kafka (`matching-results`).
4.  **apps/api** :
    *   Consomme Kafka (`matching-results`).
    *   Met à jour la BDD (`MatchRequest`, status: `COMPLETED`).
    *   Récupère la `callbackUrl` du Tenant.
    *   Envoie le résultat via POST (Webhook).

## 3. Modifications de la Base de Données (`apps/api`)

### 3.1. Entité `Tenant` (Mise à jour)
Ajout du champ pour configurer l'URL de retour.
*   `callback_url` (String, nullable) : L'URL où envoyer les webhooks.

### 3.2. Nouvelle Entité `MatchRequest`
Pour suivre l'état des demandes asynchrones.
*   `id` (UUID) : Identifiant unique retourné au client.
*   `tenant_id` (String) : Lien vers le tenant.
*   `trace_id` (String) : Pour l'observabilité.
*   `request_type` (Enum: MATCH_1_N, MATCH_1_1).
*   `status` (Enum: PENDING, PROCESSING, COMPLETED, FAILED).
*   `probe_rid` (String).
*   `created_at`, `updated_at`, `completed_at`.
*   `result_payload` (JSON/Text, optionnel) : Stockage temporaire du résultat si le webhook échoue.

## 4. Implémentation `apps/api`

### 4.1. Nouveaux Endpoints (`AsyncMatchingController`)
*   `POST /api/matching/async/1n`
*   `POST /api/matching/async/1to1`
*   `GET /api/matching/async/{requestId}` (Polling optionnel pour le client).

### 4.2. Service `AsyncMatchingService`
*   Responsable de la création de l'entité `MatchRequest`.
*   Responsable de la production du message Kafka.

### 4.3. Consumer des Résultats (`MatchingResultConsumer`)
*   Écoute le topic `matching-results`.
*   Met à jour le statut de la requête en base.
*   Déclenche le `WebhookService`.

### 4.4. Service Webhook (`WebhookService`)
*   Récupère la config du Tenant (`callbackUrl`).
*   Effectue un appel HTTP POST vers le client avec le payload du résultat.
*   Gestion basique des erreurs (log en cas d'échec).

## 5. Implémentation `apps/master`

### 5.1. Consumer des Requêtes (`MatchingRequestConsumer`)
*   Écoute le topic `matching-requests`.
*   **Configuration FIFO** : Le listener doit être configuré pour traiter les messages un par un (ou avec une concurrence contrôlée) et ne pas committer l'offset avant la fin du traitement.
*   Appelle `ScatterGatherOrchestrator.match1N(...)`.
*   En cas de succès : Publie sur `matching-results`.
*   En cas d'erreur technique : Publie un statut FAILED sur `matching-results` (ou utilise une Dead Letter Queue).

## 6. Infrastructure & Configuration (Kafka)

### 6.1. Topics
*   `matching-requests` :
    *   Partitions : 1 (pour un FIFO global strict) OU N (pour un FIFO par Tenant si partitionné par `tenantId`).
    *   Retention : Courte (ex: 24h).
*   `matching-results` :
    *   Partitions : N.

### 6.2. Configuration Spring
*   `apps/api` : Producer (`requests`), Consumer (`results`).
*   `apps/master` : Consumer (`requests`), Producer (`results`).

## 7. Étapes de Développement

1.  **Modèle de données** : Finaliser `Tenant` et `MatchRequest` dans `apps/api`.
2.  **API Async** : Créer le Controller et le Service Producer dans `apps/api`.
3.  **Master Consumer** : Implémenter le listener Kafka dans `apps/master` et le connecter à l'orchestrateur.
4.  **Retour Résultats** : Implémenter le Producer de résultats dans `apps/master`.
5.  **Webhook** : Implémenter le Consumer de résultats et le client HTTP dans `apps/api`.
