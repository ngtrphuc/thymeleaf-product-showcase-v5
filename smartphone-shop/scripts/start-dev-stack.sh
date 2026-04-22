#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
FRONTEND_DIR="${REPO_ROOT}/frontend-next"
LOG_DIR="${REPO_ROOT}/.data/logs"
FRONTEND_LOG="${LOG_DIR}/frontend-live.log"
BACKEND_LOG="${LOG_DIR}/backend-live.log"
mkdir -p "${LOG_DIR}"

is_http_ready() {
  local url="$1"
  curl -fsS --max-time 5 "$url" >/dev/null 2>&1
}

wait_until() {
  local label="$1"
  local timeout_seconds="$2"
  local probe="$3"
  local deadline=$((SECONDS + timeout_seconds))
  while (( SECONDS < deadline )); do
    if eval "$probe"; then
      return 0
    fi
    sleep 2
  done
  echo "${label} did not become ready within ${timeout_seconds} seconds."
  return 1
}

echo "[1/4] Starting Docker infra..."
docker compose -f "${REPO_ROOT}/docker-compose.yml" up -d postgres redis meilisearch

echo "[2/4] Ensuring frontend is healthy on :3000..."
if ! is_http_ready "http://localhost:3000/products"; then
  if [[ ! -d "${FRONTEND_DIR}/node_modules" ]]; then
    echo "Frontend dependencies missing. Running npm install..."
    (cd "${FRONTEND_DIR}" && npm install)
  fi

  (
    cd "${FRONTEND_DIR}"
    npm run dev >>"${FRONTEND_LOG}" 2>&1
  ) &

  wait_until "Frontend" 180 "is_http_ready 'http://localhost:3000/products'" || {
    echo "Frontend failed to boot. Check ${FRONTEND_LOG}"
    exit 1
  }
fi

echo "[3/4] Ensuring backend is healthy on :8080..."
if ! is_http_ready "http://localhost:8080/actuator/health"; then
  (
    cd "${REPO_ROOT}"
    SMARTPHONE_SHOP_DEV_AUTO_START_FRONTEND=false ./mvnw spring-boot:run >>"${BACKEND_LOG}" 2>&1
  ) &

  wait_until "Backend" 180 "is_http_ready 'http://localhost:8080/actuator/health'" || {
    echo "Backend failed to boot. Check ${BACKEND_LOG}"
    exit 1
  }
fi

echo ""
echo "[4/4] Startup verification complete."
echo "- Frontend: http://localhost:3000"
echo "- Backend:  http://localhost:8080"
echo "- API docs: http://localhost:8080/swagger-ui/index.html"
