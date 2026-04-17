#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

echo "[1/3] Starting Docker infra..."
docker compose -f "${REPO_ROOT}/docker-compose.yml" up -d postgres redis

echo "[2/3] Starting backend..."
(
  cd "${REPO_ROOT}"
  ./mvnw spring-boot:run
) &

echo "[3/3] Starting frontend..."
(
  cd "${REPO_ROOT}/frontend-next"
  npm run dev
) &

echo ""
echo "Full stack is booting:"
echo "- Backend:  http://localhost:8080"
echo "- Frontend: http://localhost:3000"
wait
