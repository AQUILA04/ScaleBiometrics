#!/usr/bin/env bash
# =============================================================================
# init.sh — Bootstrap Contabo BioEngine / scalebio (shared-traefik + common-infra)
# =============================================================================
# Usage (CD via SSH):
#   ./init.sh prod <web_image> <api_image> <master_image> <worker_image> [options...]
# =============================================================================
set -euo pipefail
set +H

DEPLOY_DIR="/opt/scalebio/deploy"
GITHUB_REPO="${SCALEBIO_GITHUB_REPO:-AQUILA04/ScaleBiometrics}"
GITHUB_RAW="https://raw.githubusercontent.com/${GITHUB_REPO}/main/deploy"

ORIG_ARGS=("$@")

ENV=""
WEB_IMAGE=""
API_IMAGE=""
MASTER_IMAGE=""
WORKER_IMAGE=""
FORCE_UPDATE=false

DB_USER=""
DB_PASSWORD=""
DB_NAME=""
APP_HOSTNAME_PROD=""
API_HOSTNAME_PROD=""
OIDC_ISSUER_URI=""
OIDC_JWK_URI=""
REDIS_DATABASE=""
AUTH_SECRET=""
REDIS_PASSWORD=""
MINIO_ACCESS_KEY=""
MINIO_SECRET_KEY=""
GHCR_USERNAME=""
GHCR_TOKEN=""

if [[ "$#" -ge 1 && "$1" != --* && "$1" != -* ]]; then
  ENV="$1"; shift
fi
if [[ "$#" -ge 1 && "$1" != --* && "$1" != -* ]]; then
  WEB_IMAGE="$1"; shift
fi
if [[ "$#" -ge 1 && "$1" != --* && "$1" != -* ]]; then
  API_IMAGE="$1"; shift
fi
if [[ "$#" -ge 1 && "$1" != --* && "$1" != -* ]]; then
  MASTER_IMAGE="$1"; shift
fi
if [[ "$#" -ge 1 && "$1" != --* && "$1" != -* ]]; then
  WORKER_IMAGE="$1"; shift
fi

while [[ "$#" -gt 0 ]]; do
  case "$1" in
    --force-update|-fu) FORCE_UPDATE=true ;;
    --db-user)                    DB_USER="$2";                    shift ;;
    --db-password)                DB_PASSWORD="$2";                shift ;;
    --db-name)                    DB_NAME="$2";                    shift ;;
    --app-hostname-prod)          APP_HOSTNAME_PROD="$2";          shift ;;
    --api-hostname-prod)          API_HOSTNAME_PROD="$2";          shift ;;
    --oidc-issuer-uri)            OIDC_ISSUER_URI="$2";            shift ;;
    --oidc-jwk-uri)               OIDC_JWK_URI="$2";               shift ;;
    --redis-database)             REDIS_DATABASE="$2";             shift ;;
    --auth-secret)                AUTH_SECRET="$2";                shift ;;
    --redis-password)             REDIS_PASSWORD="$2";             shift ;;
    --minio-access-key)           MINIO_ACCESS_KEY="$2";           shift ;;
    --minio-secret-key)           MINIO_SECRET_KEY="$2";           shift ;;
    --ghcr-username)              GHCR_USERNAME="$2";              shift ;;
    --ghcr-token)                 GHCR_TOKEN="$2";                 shift ;;
    --github-repo)
      GITHUB_REPO="$2"
      export SCALEBIO_GITHUB_REPO="$2"
      GITHUB_RAW="https://raw.githubusercontent.com/${GITHUB_REPO}/main/deploy"
      shift
      ;;
    *) echo "Unknown parameter: $1" >&2; exit 1 ;;
  esac
  shift
done

if [[ -z "$ENV" || -z "$WEB_IMAGE" || -z "$API_IMAGE" || -z "$MASTER_IMAGE" || -z "$WORKER_IMAGE" ]]; then
  echo "Error: env and four images (web api master worker) are required." >&2
  echo "Usage: $0 <env> <web> <api> <master> <worker> [options...]" >&2
  exit 1
fi

if [[ "${SCALEBIO_INIT_SYNCED:-}" != "1" ]]; then
  echo ">>> [init] Syncing /opt/scalebio/deploy from GitHub (repo is source of truth)..."
  mkdir -p /opt/scalebio
  export SCALEBIO_GITHUB_REPO="$GITHUB_REPO"
  bash <(curl -sSL "$GITHUB_RAW/update-deploy.sh")

  curl -sSL "$GITHUB_RAW/init.sh" -o /opt/scalebio/init.sh
  chmod +x /opt/scalebio/init.sh

  export SCALEBIO_INIT_SYNCED=1
  exec /opt/scalebio/init.sh "${ORIG_ARGS[@]}"
fi

if [[ ! -f /opt/scalebio/.server_initialized ]] || [[ "$FORCE_UPDATE" == true ]]; then
  echo ">>> [init] Running setup-server.sh..."
  export SB_DB_USER="$DB_USER"
  export SB_DB_PASSWORD="$DB_PASSWORD"
  export SB_DB_NAME="$DB_NAME"
  export SB_APP_HOSTNAME_PROD="$APP_HOSTNAME_PROD"
  export SB_API_HOSTNAME_PROD="$API_HOSTNAME_PROD"
  export SB_OIDC_ISSUER_URI="$OIDC_ISSUER_URI"
  export SB_OIDC_JWK_URI="$OIDC_JWK_URI"
  export SB_REDIS_DATABASE="${REDIS_DATABASE:-8}"
  export SB_AUTH_SECRET="$AUTH_SECRET"
  bash "$DEPLOY_DIR/setup-server.sh"
fi

export GHCR_USERNAME GHCR_TOKEN
export SB_UPDATE_ENV_SECRETS=true
export SB_APP_HOSTNAME_PROD="$APP_HOSTNAME_PROD"
export SB_API_HOSTNAME_PROD="$API_HOSTNAME_PROD"
export SB_DB_USER="$DB_USER"
export SB_DB_PASSWORD="$DB_PASSWORD"
export SB_DB_NAME="$DB_NAME"
export SB_OIDC_ISSUER_URI="$OIDC_ISSUER_URI"
export SB_OIDC_JWK_URI="$OIDC_JWK_URI"
export SB_REDIS_DATABASE="${REDIS_DATABASE:-8}"
export SB_AUTH_SECRET="$AUTH_SECRET"
export SB_REDIS_PASSWORD="$REDIS_PASSWORD"
export SB_MINIO_ACCESS_KEY="$MINIO_ACCESS_KEY"
export SB_MINIO_SECRET_KEY="$MINIO_SECRET_KEY"

echo ">>> [init] Calling deploy.sh..."
exec "$DEPLOY_DIR/deploy.sh" "$ENV" "$WEB_IMAGE" "$API_IMAGE" "$MASTER_IMAGE" "$WORKER_IMAGE"
