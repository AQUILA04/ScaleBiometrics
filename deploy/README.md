# BioEngine Contabo deploy (scalebio)

Product brand: **BioEngine**. Technical slug: `scalebio`.

## Layout on VPS

```text
/opt/scalebio/
  init.sh
  deploy/          # synced from GitHub
  prod/.env        # secrets (not in git)
  prod/releases/
/opt/optimizesolux/scalebio-landing/   # marketing landing
```

## Prerequisites

1. `shared-traefik` on `traefik-public`
2. `optimize-common-infra` on `optimizesolux-common` with **kafka** profile, redis, minio, keycloak
3. Realm `scalebio` imported into shared Keycloak
4. DNS A (grey cloud): `scalebio`, `scalebio-api` → Contabo IP

## Manual first deploy

```bash
# After images exist on GHCR and secrets are set:
# GitHub Actions → CD → workflow_dispatch → promote
```

Or SSH:

```bash
curl -sSL https://raw.githubusercontent.com/AQUILA04/ScaleBiometrics/main/deploy/init.sh \
  -o /opt/scalebio/init.sh && chmod +x /opt/scalebio/init.sh
sudo /opt/scalebio/init.sh prod \
  ghcr.io/aquila04/scalebiometrics/web:TAG \
  ghcr.io/aquila04/scalebiometrics/api:TAG \
  ghcr.io/aquila04/scalebiometrics/master:TAG \
  ghcr.io/aquila04/scalebiometrics/worker:TAG \
  --ghcr-username ... --ghcr-token ... \
  --db-user scalebio --db-password ... --db-name scalebio \
  --app-hostname-prod scalebio.optimizesolux.com \
  --api-hostname-prod scalebio-api.optimizesolux.com \
  --auth-secret ... --redis-database 8 \
  --github-repo AQUILA04/ScaleBiometrics
```

See `GITHUB-SECRETS-CONTABO.md` and `.env.prod.example`.
