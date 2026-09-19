#!/bin/zsh
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

build_backend() {
  echo "Building backend..."
  cd "$SCRIPT_DIR" && mvn clean package -DskipTests
}

build_frontend() {
  echo "Building frontend..."
  cd "$SCRIPT_DIR/frontend" && npm install && npm run build
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
