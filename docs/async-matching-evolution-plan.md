# Plan d'Évolution : Gestion Identité, Images & MinIO

Ce document détaille l'évolution du système de matching asynchrone pour gérer des identités complètes (multi-empreintes), le stockage des images brutes sur MinIO, et l'extraction de templates à la volée.

## 1. Objectifs
*   **Approche Identité** : Une requête concerne une personne (`probeRid`) possédant N empreintes (doigts).
*   **Gestion des Images** : Les clients envoient des images brutes (MB). Elles doivent être stockées (MinIO) pour archivage/migration future.
*   **Optimisation Réseau** : Seuls les templates extraits (KB) circulent dans Kafka et vers le Master.
*   **Logique Métier** :
    *   **Stop-on-Match** : Si une seule empreinte matche, l'identité est considérée comme doublon.
    *   **Enroll-All** : Si aucune empreinte ne matche, toutes les empreintes sont enrôlées (si demandé).

## 2. Architecture des Flux

### Flux Asynchrone (Évolué)
1.  **Client** -> `POST /api/matching/async/1n` (Multipart: JSON + List<File>) -> **apps/api**
2.  **apps/api** :
    *   Pour chaque fichier image reçu :
        1.  **Upload MinIO** : Sauvegarde l'image brute (`bucket/tenant/rid/image_X`).
        2.  **Extraction** : Convertit l'image en Template biométrique (byte[]).
    *   **Persistance BDD** :
        *   `MatchRequest` (Status: PENDING).
        *   `MatchRequestFingerprint` (Stocke `minio_path` et `template_data`).
    *   **Push Kafka** : Envoie un événement contenant la liste des **Templates** (pas les images).
    *   Retourne `202 Accepted`.
3.  **apps/master** :
    *   Consomme l'événement (Liste de Templates).
    *   **Boucle de Matching** :
        *   Teste les templates un par un.
        *   Si Match trouvé -> Arrêt -> Résultat `DUPLICATE`.
    *   Si aucun Match -> Résultat `NO_MATCH` (et déclenchement Enrôlement si configuré).
    *   Push résultat dans Kafka (`matching-results`).
4.  **apps/api** :
    *   Consomme le résultat.
    *   Met à jour le statut.
    *   Notifie le client (Webhook).

## 3. Modifications Techniques

### 3.1. Infrastructure & Dépendances (`apps/api`)
*   Ajout client **MinIO** (AWS S3 SDK ou MinIO SDK).
*   Ajout librairie biométrique (**SourceAFIS** ou autre) pour l'extraction de templates.

### 3.2. Base de Données (`apps/api`)
Nouvelle table pour stocker les détails des empreintes d'une requête.

**Table `match_request_fingerprints`**
*   `id` (UUID)
*   `match_request_id` (UUID, FK)
*   `image_path` (TEXT) : Chemin dans MinIO.
*   `template_data` (BYTEA) : Le template extrait.
*   `finger_index` (INT) : Ordre/Position du doigt.
*   `created_at` (TIMESTAMP)

### 3.3. API & DTOs
*   **Endpoint** : `POST /api/matching/async/1n` consomme `multipart/form-data`.
*   **DTO Entrée** : `MatchRequestDto` (JSON metadata) + `List<MultipartFile>`.
*   **Event Kafka** :
    ```java
    class AsyncMatchRequestEvent {
        UUID requestId;
        String probeRid;
        List<byte[]> templates; // Liste des templates extraits
        // ...
    }
    ```

### 3.4. Logique Master
*   Refonte du `MatchingRequestConsumer` pour itérer sur la liste des templates.
*   Implémentation de la logique conditionnelle (Stop-on-Match).

## 4. Étapes de Développement

1.  **Dépendances** : Ajouter MinIO et SourceAFIS dans `pom.xml`.
2.  **BDD** : Créer la migration Liquibase pour `match_request_fingerprints`.
3.  **Services API** :
    *   `MinioService` (Upload).
    *   `BiometricService` (Extraction).
    *   Mise à jour `AsyncMatchingService` (Orchestration locale).
4.  **Controller** : Mettre à jour `AsyncMatchingController` pour le Multipart.
5.  **Master** : Mettre à jour la logique de consommation Kafka pour gérer les listes de templates.
