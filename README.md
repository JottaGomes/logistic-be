# Logistics - Income and Cost Evaluation System

Web application for calculating and tracking shipment profit/loss.

## Stack

| Layer | Technology |
|---|---|
| Backend | Java 17, Spring Boot 3.1.5, Spring Security, JPA |
| Frontend | Angular 17, Angular Material, Bootstrap 5.3 |
| Auth | JWT (stateless, 8h expiry) |
| Database | H2 in-memory (dev) · MariaDB (prod) |

## Run locally

**Backend** (port 8080, H2 in-memory):
```bash
mvn spring-boot:run
```

**Frontend** (port 4200):
```bash
cd frontend && npm install && ng serve
```

Open http://localhost:4200/dachser and log in with the account that was sent to you.

H2 console available at http://localhost:8080/h2-console (JDBC URL: `jdbc:h2:mem:logisticsdb`).

## API endpoints

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/api/auth/login` | public | Returns JWT token |
| `GET` | `/api/auth/config` | public | Returns `loginEnabled` flag |
| `POST` | `/api/shipments/calculate` | required | Calculates and saves profit/loss |
| `GET` | `/api/shipments` | required | Paginated list of calculations |

## Tests

```bash
mvn test
```

---

## docker

Docker commands used for build and deploy:

Build: `all`, `frontend` or `backend`

```Bash
./builder.sh all
```

### BackEnd

```Bash
sudo docker buildx build --platform linux/amd64 -t dachser-backend .
```
```Bash
sudo docker tag dachser-backend 192.168.1.3:5005/dachser-backend:latest
```
```Bash
sudo docker push 192.168.1.3:5005/dachser-backend:latest
```
Command to run on the machine that will run the backend app:
```Bash
sudo docker run -e SPRING_PROFILES_ACTIVE=prod -e DB_USERNAME=logistics -e DB_PASSWORD=pass -e JWT_SECRET=secret --network mariadb_cdc_default -d -p 8333:8333 --name dachser-backend 192.168.1.3:5005/dachser-backend
```
### FrontEnd

```Bash
sudo docker buildx build --platform linux/amd64 -t dachser-frontend ./frontend
```
```Bash
sudo docker tag dachser-frontend 192.168.1.3:5005/dachser-frontend:latest
```
```Bash
sudo docker push 192.168.1.3:5005/dachser-frontend:latest
```
Command to run on the machine that will run the frontend app:
```Bash
sudo docker run -d -p 4200:80 --name dachser-frontend 192.168.1.3:5005/dachser-frontend
```


