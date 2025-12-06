#!/usr/bin/env bash
set -euo pipefail

echo "[deploy] Pulling latest images (if any)..."
docker compose pull || true

echo "[deploy] Building images..."
docker compose build

echo "[deploy] Starting containers..."
docker compose up -d

echo "[deploy] Done."

