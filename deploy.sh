#!/bin/zsh
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REGISTRY="192.168.1.3:5005"
SERVER="adriano@192.168.1.3"

deploy_backend() {
  echo "Building backend ..."
  #cd "$SCRIPT_DIR" && mvn clean package -DskipTests -q

  echo "Building and pushing backend image ..."
  sudo docker buildx build --platform linux/amd64 -t "$REGISTRY/dachser-backend:latest" "$SCRIPT_DIR"
  sudo docker push "$REGISTRY/dachser-backend:latest"

  echo "Deploying backend on server ..."
  ssh "$SERVER" "
    sudo docker stop dachser-backend 2>/dev/null || true
    sudo docker rm dachser-backend 2>/dev/null || true
    sudo docker rmi $REGISTRY/dachser-backend:latest 2>/dev/null || true
    sudo docker pull $REGISTRY/dachser-backend:latest
    sudo docker run \
      -e SPRING_PROFILES_ACTIVE=prod \
      -e DB_USERNAME=logistics \
      -e DB_PASSWORD=logistics1pass \
      -e JWT_SECRET=dachser-logistics-super-secret-key-32-chars-min \
      --network mariadb_cdc_default \
      -d -p 8333:8333 \
      --name dachser-backend \
      $REGISTRY/dachser-backend:latest
  "
  echo "==> Backend deployed."
}

deploy_frontend() {
  echo "Building frontend ..."
  cd "$SCRIPT_DIR/frontend" && npm install && npm run build

  echo "Building and pushing frontend image ..."
  sudo docker buildx build --platform linux/amd64 -t "$REGISTRY/dachser-frontend:latest" "$SCRIPT_DIR/frontend"
  sudo docker push "$REGISTRY/dachser-frontend:latest"

  echo "Deploying frontend on server ..."
  ssh "$SERVER" "
    sudo docker stop dachser-frontend 2>/dev/null || true
    sudo docker rm dachser-frontend 2>/dev/null || true
    sudo docker rmi $REGISTRY/dachser-frontend:latest 2>/dev/null || true
    sudo docker pull $REGISTRY/dachser-frontend:latest
    sudo docker run -d -p 4200:80 --name dachser-frontend $REGISTRY/dachser-frontend:latest
  "
  echo "Frontend deployed."
}

case "${1:-all}" in
  all)
    deploy_backend
    deploy_frontend
    ;;
  backend)
    deploy_backend
    ;;
  frontend)
    deploy_frontend
    ;;
  *)
    echo "Usage: $0 [all | backend | frontend]"
    exit 1
    ;;
esac

echo "Deploy complete."
