# Secrets GitHub + DNS — BioEngine / scalebio (OptimizeSolux Contabo)

Prérequis VPS : **shared-traefik** + **optimize-common-infra** (réseau `optimizesolux-common`,
Keycloak `auth.optimizesolux.com`, realm `scalebio`, profil **kafka** activé, MinIO).

Nom commercial : **BioEngine**. Slug technique : `scalebio`.

## 1. DNS Cloudflare

| Type | Name | Content | Proxy |
|------|------|---------|-------|
| A | `scalebio` | `169.58.127.90` | DNS only |
| A | `scalebio-api` | `169.58.127.90` | DNS only |

Auth partagée : `auth.optimizesolux.com` (common-infra) — **pas** de `scalebio-auth`.

Email Routing (manuel) : `contact.bioengine@optimizesolux.com` → forward habituel.

## 2. Secrets repo (Actions) + environment `prod`

| Secret | Valeur |
|--------|--------|
| `SSH_PRIVATE_KEY` | contenu de `~\.ssh\optimizesolux_vps_ed25519` |
| `PROD_SERVER_HOST` | `169.58.127.90` |
| `PROD_SERVER_USER` | `root` |
| `GHCR_USERNAME` | user GitHub |
| `GHCR_TOKEN` | PAT `read:packages` (+ `write:packages` pour CI) — **manuel** |
| `DB_USER` | `scalebio` |
| `PROD_DB_PASSWORD` | mot de passe fort (Postgres métier) |
| `PROD_DB_NAME` | `scalebio` |
| `PROD_APP_HOSTNAME` | `scalebio.optimizesolux.com` |
| `PROD_API_HOSTNAME` | `scalebio-api.optimizesolux.com` |
| `AUTH_SECRET` | secret NextAuth (≥32 chars) |
| `REDIS_PASSWORD` | même valeur que common-infra Redis (si protégé) |
| `MINIO_ACCESS_KEY` / `MINIO_SECRET_KEY` | common-infra MinIO |

Créer aussi l’**environment** GitHub Actions nommé `prod`.

## 3. Hosts runtime

| URL | Rôle |
|-----|------|
| https://scalebio.optimizesolux.com/ | Landing BioEngine (`Path(/)`) |
| https://scalebio.optimizesolux.com/login | Console Next.js |
| https://scalebio-api.optimizesolux.com | API Spring Boot |
| https://auth.optimizesolux.com/realms/scalebio | Keycloak (common-infra) |

## 4. Pipelines

| Workflow | Trigger |
|----------|---------|
| **Docker Build** | push `develop` / `release/**` → images GHCR |
| **CD** | Docker Build success sur `release/**` **ou** `workflow_dispatch` (promote) → SSH Contabo |
| **Landing** | push `landing-page/**` sur `main` **ou** `workflow_dispatch` |

## 5. Common-infra

- Redis DB index réservé : **8** (`scalebio:`)
- Kafka : hostname `kafka:9092` (profil kafka)
- MinIO : `http://minio:9000`
- Realm Keycloak : déposer `scalebio-realm.json` puis rebuild Keycloak image

## 6. Source de vérité

- Runtime Docker = `deploy/` du dépôt
- Secrets hors git : `/opt/scalebio/prod/.env`
- Template : `deploy/.env.prod.example`
