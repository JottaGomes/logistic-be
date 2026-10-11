#!/bin/zsh
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# The Angular front end is its own repository (github.com/JottaGomes/logistic).
# Point FRONTEND_DIR at its clone; by default it is expected next to this one.
FRONTEND_DIR="${FRONTEND_DIR:-$SCRIPT_DIR/../logistics-frontend}"

require_frontend() {
  if [ ! -f "$FRONTEND_DIR/package.json" ]; then
    echo "Front end not found at $FRONTEND_DIR. Clone github.com/JottaGomes/logistic there, or set FRONTEND_DIR." >&2
    exit 1
  fi
}

build_backend() {
  echo "Building backend..."
  cd "$SCRIPT_DIR" && mvn clean package -DskipTests
}

build_frontend() {
  echo "Building frontend..."
  require_frontend
  cd "$FRONTEND_DIR" && npm install && npm run build
}

case "${1:-all}" in
  all)
    build_backend
    build_frontend
    ;;
  backend)
    build_backend
    ;;
  frontend)
    build_frontend
    ;;
  *)
    echo "Usage: $0 [all | backend | frontend]"
    exit 1
    ;;
esac

echo "Build complete."
