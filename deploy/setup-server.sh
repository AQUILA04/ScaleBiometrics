#!/usr/bin/env bash
# =============================================================================
# setup-server.sh — One-time Contabo setup for BioEngine (scalebio)
# Prérequis: shared-traefik + optimize-common-infra (optimizesolux-common).
# =============================================================================
set -euo pipefail
set +H

DEPLOY_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT="/opt/scalebio"

echo "=== BioEngine (scalebio) Server Setup ==="

echo "[1/5] Checking Docker installation..."
if ! command -v docker &>/dev/null; then
  echo "      Docker not found. Installing..."
  apt-get update -y
  apt-get install -y ca-certificates curl gnupg git
  install -m 0755 -d /etc/apt/keyrings
  curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
  chmod a+r /etc/apt/keyrings/docker.gpg
  echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" \
    | tee /etc/apt/sources.list.d/docker.list > /dev/null
  apt-get update -y
  apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
  echo "      Docker installed successfully."
else
  echo "      Docker already installed."
fi

echo "[2/5] Creating directory structure..."
mkdir -p "$ROOT/prod/releases"
mkdir -p "$ROOT/deploy"
if [[ "$DEPLOY_DIR" != "$ROOT/deploy" ]]; then
  cp -a "$DEPLOY_DIR"/. "$ROOT/deploy/"
  chmod +x "$ROOT/deploy"/*.sh 2>/dev/null || true
fi
echo "      Directories created."

echo "[3/5] Creating Docker networks..."
for net in traefik-public optimizesolux-common; do
  if docker network inspect "$net" > /dev/null 2>&1; then
    echo "      Network '$net' already exists, skipping."
  else
    docker network create "$net"
    echo "      Network '$net' created."
  fi
done

if [[ ! -d /opt/optimizesolux/common-infra ]]; then
  echo "      WARNING: /opt/optimizesolux/common-infra not found."
  echo "      Install optimize-common-infra before deploying this product."
  echo "      Ensure Kafka profile is enabled (ScaleBiometrics requires kafka:9092)."
else
  echo "      Tip: ensure common-infra is up: sudo /opt/optimizesolux/common-infra/install.sh"
  echo "      Tip: enable kafka profile if not already active."
fi

env_quote() {
  local val="$1"
  if [[ "$val" =~ ^[A-Za-z0-9._:/+-]+$ ]]; then
    printf '%s' "$val"
  else
    local escaped="${val//\\/\\\\}"
    escaped="${escaped//\"/\\\"}"
    escaped="${escaped//\$/\\$}"
    escaped="${escaped//\`/\\\`}"
    printf '"%s"' "$escaped"
  fi
}

echo "[4/5] Creating .env files..."

_db_user="${SB_DB_USER:-scalebio}"
_db_pass="${SB_DB_PASSWORD:-CHANGE_ME_prod_db_password}"
_db_name="${SB_DB_NAME:-scalebio}"
_app_host="${SB_APP_HOSTNAME_PROD:-scalebio.optimizesolux.com}"
_api_host="${SB_API_HOSTNAME_PROD:-scalebio-api.optimizesolux.com}"
_oidc="${SB_OIDC_ISSUER_URI:-https://auth.optimizesolux.com/realms/scalebio}"
_oidc_jwk="${SB_OIDC_JWK_URI:-https://auth.optimizesolux.com/realms/scalebio/protocol/openid-connect/certs}"
_redis_db="${SB_REDIS_DATABASE:-8}"
_auth_secret="${SB_AUTH_SECRET:-CHANGE_ME_auth_secret_min_32_chars_xxxxxxxx}"

ENV_FILE="$ROOT/prod/.env"
if [[ ! -f "$ENV_FILE" ]]; then
  cat > "$ENV_FILE" <<EOF
DB_USER=$(env_quote "$_db_user")
DB_PASSWORD=$(env_quote "$_db_pass")
DB_NAME=$(env_quote "$_db_name")
APP_HOSTNAME=$(env_quote "$_app_host")
API_HOSTNAME=$(env_quote "$_api_host")
OIDC_ISSUER_URI=$(env_quote "$_oidc")
OIDC_JWK_URI=$(env_quote "$_oidc_jwk")
REDIS_DATABASE=$(env_quote "$_redis_db")
AUTH_SECRET=$(env_quote "$_auth_secret")
AUTH_URL=$(env_quote "https://${_app_host}")
KEYCLOAK_URL=https://auth.optimizesolux.com
KEYCLOAK_REALM=scalebio
KEYCLOAK_CLIENT_ID=scalebio-web
API_BASE_URL=$(env_quote "https://${_api_host}/api")
NEXT_PUBLIC_API_URL=$(env_quote "https://${_api_host}/api")
NEXT_PUBLIC_KEYCLOAK_URL=https://auth.optimizesolux.com
NEXT_PUBLIC_KEYCLOAK_REALM=scalebio
NEXT_PUBLIC_KEYCLOAK_CLIENT_ID=scalebio-web
KAFKA_BOOTSTRAP_SERVERS=kafka:9092
MINIO_URL=http://minio:9000
MINIO_BUCKET=biometric-images
MASTER_ADDRESS=static://master:9091
WORKER_ADDRESS=static://worker:9093
EOF
  chmod 600 "$ENV_FILE"
  echo "      Created $ENV_FILE"
else
  echo "      $ENV_FILE already exists, leaving intact."
fi

echo "[5/5] Marking server initialized..."
touch "$ROOT/.server_initialized"
echo "=== Setup complete ==="
