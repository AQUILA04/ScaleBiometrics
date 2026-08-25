# Keycloak realm BioEngine (scalebio)

1. Copy `deploy/keycloak/scalebio-realm.json` to `optimize-common-infra/images/keycloak/realms/scalebio-realm.json`
2. Rebuild Keycloak image and run `install.sh --force-update keycloak` on the VPS
3. Verify issuer: https://auth.optimizesolux.com/realms/scalebio

Clients: `scalebio-web` (public PKCE), `scalebio-api` (confidential).
Redirects include https://scalebio.optimizesolux.com/*

Redis DB index reserved for this product: **8** (prefix `scalebio:`).
Kafka: enable common-infra profile `kafka` (bootstrap `kafka:9092`).
