#!/usr/bin/env bash
set -euo pipefail
set +H

# Usage:
#   deploy.sh [--force-update | -fu] <env> <web> <api> <master> <worker>
#   env = prod

if [ "$#" -lt 1 ]; then
  echo "Usage: $0 [--force-update | -fu] <env> <web> <api> <master> <worker>" >&2
  exit 2
fi

if [[ "$1" == "--force-update" || "$1" == "-fu" ]]; then
  echo ">>> [deploy] Force update requested. Updating deploy scripts from GitHub..."
  REPO="${SCALEBIO_GITHUB_REPO:-AQUILA04/ScaleBiometrics}"
  curl -sSL "https://raw.githubusercontent.com/${REPO}/main/deploy/update-deploy.sh" | bash
  shift
  if [ "$#" -lt 5 ]; then
    echo "Error: Missing arguments after --force-update." >&2
    exit 2
  fi
  echo ">>> [deploy] Re-executing updated deploy.sh..."
  exec /opt/scalebio/deploy/deploy.sh "$@"
fi

ENV="$1"
WEB_ARG="${2:-}"
API_ARG="${3:-}"
MASTER_ARG="${4:-}"
WORKER_ARG="${5:-}"

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
COMPOSE_FILE="$ROOT_DIR/docker-compose.$ENV.yml"
STACK_DIR="/opt/scalebio/$ENV"
ENV_FILE="$STACK_DIR/.env"
RELEASES_DIR="$STACK_DIR/releases"
PROJECT_NAME="scalebio-$ENV"
mkdir -p "$RELEASES_DIR"

env_quote() {
  local val="$1"
  if [[ "$val" == *[$'\n\r']* ]]; then
    echo "Error: newline in env value for key" >&2
    return 1
  fi
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

safe_source_env() {
  local file="$1"
  [[ -f "$file" ]] || return 0
  set +u
  set -a
  # shellcheck disable=SC1090
  source "$file"
  set +a
  set -u
}

set_env_var() {
  local key="$1"
  local val="$2"
  local file="$ENV_FILE"
  local stored line found=false
  stored="$(env_quote "$val")"
  local tmp
  tmp=$(mktemp)

  if [[ -f "$file" ]]; then
    while IFS= read -r line || [[ -n "$line" ]]; do
      if [[ "$line" == "${key}="* ]] && [[ "$found" == false ]]; then
        printf '%s=%s\n' "$key" "$stored"
        found=true
      else
        printf '%s\n' "$line"
      fi
    done < "$file" > "$tmp"
  fi

  if [[ "$found" == false ]]; then
    printf '%s=%s\n' "$key" "$stored" >> "$tmp"
  fi

  cat "$tmp" > "$file"
  rm -f "$tmp"
}

set_env_var_if_missing() {
  local key="$1"
  local val="$2"
  if ! grep -q -E "^${key}=" "$ENV_FILE" 2>/dev/null; then
    set_env_var "$key" "$val"
    echo "  + added missing $key"
  elif grep -q -E "^${key}=$" "$ENV_FILE" 2>/dev/null && [[ -n "$val" ]]; then
    set_env_var "$key" "$val"
    echo "  + filled empty $key"
  fi
}

if [[ ! -f "$COMPOSE_FILE" ]]; then
  echo "Error: compose file not found: $COMPOSE_FILE" >&2
  exit 1
fi

if [[ -f "$ENV_FILE" ]]; then
  safe_source_env "$ENV_FILE"
else
  echo "Warning: $ENV_FILE not found. Run setup-server.sh or init.sh first." >&2
fi

WEB_IMAGE="${WEB_ARG:-${WEB_IMAGE:-}}"
API_IMAGE="${API_ARG:-${API_IMAGE:-}}"
MASTER_IMAGE="${MASTER_ARG:-${MASTER_IMAGE:-}}"
WORKER_IMAGE="${WORKER_ARG:-${WORKER_IMAGE:-}}"

if [[ -z "$WEB_IMAGE" || -z "$API_IMAGE" || -z "$MASTER_IMAGE" || -z "$WORKER_IMAGE" ]]; then
  echo "Error: WEB/API/MASTER/WORKER images required" >&2
  exit 1
fi

touch "$ENV_FILE"
chmod 600 "$ENV_FILE" || true

[[ -n "$WEB_ARG" ]] && set_env_var "WEB_IMAGE" "$WEB_IMAGE"
[[ -n "$API_ARG" ]] && set_env_var "API_IMAGE" "$API_IMAGE"
[[ -n "$MASTER_ARG" ]] && set_env_var "MASTER_IMAGE" "$MASTER_IMAGE"
[[ -n "$WORKER_ARG" ]] && set_env_var "WORKER_IMAGE" "$WORKER_IMAGE"

echo "Ensuring $ENV_FILE has required keys..."
if [[ "$ENV" == "prod" ]]; then
  set_env_var_if_missing APP_HOSTNAME "scalebio.optimizesolux.com"
  set_env_var_if_missing API_HOSTNAME "scalebio-api.optimizesolux.com"
  set_env_var_if_missing DB_USER "scalebio"
  set_env_var_if_missing DB_NAME "scalebio"
  set_env_var_if_missing REDIS_DATABASE "8"
  set_env_var_if_missing OIDC_ISSUER_URI "https://auth.optimizesolux.com/realms/scalebio"
  set_env_var_if_missing OIDC_JWK_URI "https://auth.optimizesolux.com/realms/scalebio/protocol/openid-connect/certs"
fi

if [[ "${SB_UPDATE_ENV_SECRETS:-}" == "true" ]]; then
  echo "SB_UPDATE_ENV_SECRETS=true — applying secret overrides from init/CD..."
  [[ -n "${SB_APP_HOSTNAME_PROD:-}" ]] && set_env_var APP_HOSTNAME "$SB_APP_HOSTNAME_PROD"
  [[ -n "${SB_API_HOSTNAME_PROD:-}" ]] && set_env_var API_HOSTNAME "$SB_API_HOSTNAME_PROD"
  [[ -n "${SB_DB_USER:-}" ]] && set_env_var DB_USER "$SB_DB_USER"
  [[ -n "${SB_DB_PASSWORD:-}" ]] && set_env_var DB_PASSWORD "$SB_DB_PASSWORD"
  [[ -n "${SB_DB_NAME:-}" ]] && set_env_var DB_NAME "$SB_DB_NAME"
  [[ -n "${SB_OIDC_ISSUER_URI:-}" ]] && set_env_var OIDC_ISSUER_URI "$SB_OIDC_ISSUER_URI"
  [[ -n "${SB_OIDC_JWK_URI:-}" ]] && set_env_var OIDC_JWK_URI "$SB_OIDC_JWK_URI"
  [[ -n "${SB_REDIS_DATABASE:-}" ]] && set_env_var REDIS_DATABASE "$SB_REDIS_DATABASE"
  [[ -n "${SB_AUTH_SECRET:-}" ]] && set_env_var AUTH_SECRET "$SB_AUTH_SECRET"
  [[ -n "${SB_REDIS_PASSWORD:-}" ]] && set_env_var REDIS_PASSWORD "$SB_REDIS_PASSWORD"
  [[ -n "${SB_MINIO_ACCESS_KEY:-}" ]] && set_env_var MINIO_ACCESS_KEY "$SB_MINIO_ACCESS_KEY"
  [[ -n "${SB_MINIO_SECRET_KEY:-}" ]] && set_env_var MINIO_SECRET_KEY "$SB_MINIO_SECRET_KEY"
fi

TIMESTAMP=$(date -u +"%Y%m%dT%H%M%SZ")
RELEASE_FILE="$RELEASES_DIR/${ENV}_${TIMESTAMP}.txt"

echo "DEPLOY: env=$ENV"
echo "Using compose file: $COMPOSE_FILE"
echo "Using env file:     $ENV_FILE"
echo "Saving release metadata to $RELEASE_FILE"
{
  echo "WEB_IMAGE=$WEB_IMAGE"
  echo "API_IMAGE=$API_IMAGE"
  echo "MASTER_IMAGE=$MASTER_IMAGE"
  echo "WORKER_IMAGE=$WORKER_IMAGE"
  echo "TIMESTAMP=$TIMESTAMP"
} > "$RELEASE_FILE"

echo "Pulling images..."
if [ -n "${GHCR_USERNAME:-}" ] && [ -n "${GHCR_TOKEN:-}" ]; then
  echo "Logging in to ghcr.io as $GHCR_USERNAME"
  echo "$GHCR_TOKEN" | docker login ghcr.io -u "$GHCR_USERNAME" --password-stdin
fi

GAI=/etc/gai.conf
if [[ -w "$GAI" ]] || [[ -w /etc ]]; then
  if ! grep -qE '^precedence ::ffff:0:0/96[[:space:]]+100' "$GAI" 2>/dev/null; then
    echo "precedence ::ffff:0:0/96  100" >> "$GAI"
    echo "Preferred IPv4 via /etc/gai.conf (GHCR IPv6 resets on Contabo)"
  fi
fi

PULL_MAX=5
PULL_DELAY=5
PULL_OK=false
for attempt in $(seq 1 "$PULL_MAX"); do
  echo "Pull attempt ${attempt}/${PULL_MAX}..."
  if docker compose \
    -f "$COMPOSE_FILE" \
    --project-name "$PROJECT_NAME" \
    --env-file "$ENV_FILE" \
    pull; then
    PULL_OK=true
    break
  fi
  if [[ "$attempt" -lt "$PULL_MAX" ]]; then
    echo "Pull failed (attempt ${attempt}/${PULL_MAX}); retrying in ${PULL_DELAY}s..."
    sleep "$PULL_DELAY"
    PULL_DELAY=$((PULL_DELAY * 2))
  fi
done
if [[ "$PULL_OK" != "true" ]]; then
  echo "ERROR: docker compose pull failed after ${PULL_MAX} attempts" >&2
  exit 1
fi

echo "Starting services..."
set +e
docker compose \
  -f "$COMPOSE_FILE" \
  --project-name "$PROJECT_NAME" \
  --env-file "$ENV_FILE" \
  up -d --wait --wait-timeout 300
UP_RC=$?
set -e
if [[ "$UP_RC" -ne 0 ]]; then
  echo "ERROR: compose up failed (exit $UP_RC). Status and logs:" >&2
  docker compose \
    -f "$COMPOSE_FILE" \
    --project-name "$PROJECT_NAME" \
    --env-file "$ENV_FILE" \
    ps -a || true
  docker compose \
    -f "$COMPOSE_FILE" \
    --project-name "$PROJECT_NAME" \
    --env-file "$ENV_FILE" \
    logs --no-color --tail=200 || true
  exit "$UP_RC"
fi

ln -sfn "$RELEASE_FILE" "$RELEASES_DIR/${ENV}_current.txt"

if [[ "$ENV" == "prod" ]]; then
  safe_source_env "$ENV_FILE"
  APP_URL="https://${APP_HOSTNAME:-scalebio.optimizesolux.com}"
  API_URL="https://${API_HOSTNAME:-scalebio-api.optimizesolux.com}"
  echo "HTTP smoke: app=$APP_URL api=$API_URL"
  sleep 5
  curl -sk -o /dev/null -w "app=%{http_code}\n" "$APP_URL/login" || echo "WARN: app HTTP check failed"
  curl -sk -o /dev/null -w "api=%{http_code}\n" "$API_URL/actuator/health" || echo "WARN: api HTTP check failed"
fi

echo "Deployment finished."
cat "$RELEASE_FILE"
echo "Done"
