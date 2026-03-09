# Plan d'Implémentation : Distributed Short-Circuit (Arrêt Distribué)

Ce document détaille l'optimisation du système de matching distribué pour permettre aux workers d'arrêter leur traitement dès qu'un doublon est trouvé par n'importe quel autre worker.

## 1. Problématique
Dans le scénario actuel "Stop-on-Match", le Master arrête d'envoyer des requêtes une fois un match trouvé. Cependant, les requêtes déjà envoyées aux Workers continuent d'être traitées jusqu'au bout, gaspillant des ressources CPU (inutile si l'identité est déjà identifiée).

## 2. Solution : État Partagé via Redis
Utiliser Redis comme "tableau noir" partagé où l'état de la requête est maintenu en temps réel.

### 2.1. États de la Requête
*   `RUNNING` : La recherche est en cours, aucun doublon trouvé.
*   `FOUND` : Un doublon a été trouvé par un worker, tous les autres doivent s'arrêter.

### 2.2. Clé Redis
*   **Format** : `req:status:{traceId}`
*   **TTL** : 30 secondes (durée de vie maximale estimée d'une requête).

## 3. Architecture des Flux

### Phase 1 : Initialisation (Master)
1.  Le Master reçoit la requête asynchrone.
2.  Avant de lancer le `Scatter` (envoi aux workers), il initialise la clé Redis :
    ```bash
    SET req:status:{traceId} "RUNNING" EX 30
    ```

### Phase 2 : Exécution & Vérification (Worker)
Le Worker reçoit la requête contenant N empreintes.

**Boucle de traitement (pour chaque empreinte) :**
1.  **Check-Point** : Le Worker interroge Redis.
    *   `GET req:status:{traceId}`
    *   Si valeur == `FOUND` ou clé inexistante (expiration) -> **ABORT** (Arrêt immédiat, retour vide).
2.  **Matching** : Exécution de la recherche (HNSW / SourceAFIS).
3.  **Signalement (Si Match)** :
    *   Si un candidat valide est trouvé (Score > Threshold) :
    *   Le Worker met à jour Redis : `SET req:status:{traceId} "FOUND"`.
    *   Il retourne le résultat au Master.

### Phase 3 : Nettoyage (Master)
Une fois l'agrégation terminée et le résultat envoyé, le Master peut supprimer la clé explicitement (optionnel, car TTL court).

## 4. Implémentation Technique

### 4.1. Master (`apps/master`)
*   **Service** : `ScatterGatherOrchestrator`
*   **Action** : Injecter `RedisTemplate`. Ajouter l'initialisation de la clé avant la boucle de soumission des tâches `ExecutorService`.

### 4.2. Worker (`apps/worker`)
*   **Service** : `HybridMatchingEngine` (ou `MatcherServiceImpl`)
*   **Action** :
    *   Injecter `RedisTemplate`.
    *   Dans la méthode `match1N`, ajouter la logique de vérification avant le traitement de chaque empreinte (si traitement par lot) ou au début de la méthode.
    *   Ajouter la logique d'écriture en cas de succès.

## 5. Considérations de Performance
*   **Latence Redis** : L'appel Redis prend ~1ms. C'est négligeable par rapport à un matching biométrique (100ms+), mais il ne faut pas le faire dans la boucle interne de comparaison de vecteurs (millions d'itérations).
*   **Fréquence** : Vérifier l'état :
    *   Au début de la requête.
    *   Entre chaque empreinte (si liste).
    *   (Optionnel) Toutes les N millisecondes si le traitement d'une seule empreinte est très long.

## 6. Gestion des Conflits
*   Si deux workers trouvent un match simultanément, les deux écrivent `FOUND` (idempotent) et renvoient leur résultat. Le Master gérera la déduplication des réponses via sa logique d'agrégation existante (Top-K).
