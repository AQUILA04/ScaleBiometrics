#!/usr/bin/env bash
# =============================================================================
# update-deploy.sh — Sync atomique de deploy/ depuis GitHub (BioEngine / scalebio)
# =============================================================================
# SCALEBIO_GITHUB_REPO=owner/repo (défaut: AQUILA04/ScaleBiometrics)
# =============================================================================

REPO="${SCALEBIO_GITHUB_REPO:-AQUILA04/ScaleBiometrics}"
ROOT="/opt/scalebio"

echo ">>> [update-deploy] Fetching latest deploy scripts from GitHub ($REPO)..."
rm -rf /tmp/scalebio_src
git clone --depth 1 "https://github.com/${REPO}.git" /tmp/scalebio_src > /dev/null 2>&1

echo ">>> [update-deploy] Applying new scripts..."
rm -rf "$ROOT/deploy.new"
cp -r /tmp/scalebio_src/deploy "$ROOT/deploy.new"
rm -rf /tmp/scalebio_src

chmod +x "$ROOT/deploy.new"/*.sh 2>/dev/null || true

BACKUP_DIR="$ROOT/deploy.old_$(date +%s)"
if [[ -d "$ROOT/deploy" ]]; then
  mv "$ROOT/deploy" "$BACKUP_DIR"
  echo ">>> [update-deploy] Old scripts backed up in $BACKUP_DIR"
fi
mv "$ROOT/deploy.new" "$ROOT/deploy"

echo ">>> [update-deploy] Update complete!"
