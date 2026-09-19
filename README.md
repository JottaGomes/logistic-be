# Logistics - Income and Cost Evaluation System

Web application for calculating and tracking shipment profit/loss.

## Stack

| Layer | Technology |
|---|---|
| Backend | Java 17, Spring Boot 3.1.5, Spring Security, JPA |
| Frontend | Angular 17, Angular Material, Bootstrap 5.3 |
| Auth | JWT (stateless, 8h expiry) |
| Database | MariaDB (persistent, via Docker volume) |

## Run locally

Everything comes up with Docker Compose — MariaDB plus the backend:

```bash
docker compose up -d --build
```

- Backend: http://localhost:8080
- MariaDB: `localhost:3306`, database `logisticsdb`, user `logistics`

The schema and seed data are applied on every start (`schema.sql` uses
`CREATE TABLE IF NOT EXISTS`, `data.sql` uses `INSERT IGNORE`, so re-running is safe).
Data lives in the `mariadb-data` volume and survives `docker compose down`.
To wipe it: `docker compose down -v`.

### Running the backend from the IDE

Start only the database, then run `LogisticsApplication`:

```bash
docker compose up -d mariadb
```

The defaults in `application.yml` already point at `localhost:3306`. One variable
is required, because there is no fallback for it:

```
JWT_SECRET=dachser-logistics-super-secret-key-32-chars-min
```

### Frontend

The UI is deployed separately from https://github.com/JottaGomes/logistic.
To run the copy in this repo:

```bash
cd frontend && npm install && ng serve
```

Open http://localhost:4200/dachser.

### Configuration

| Variable | Default | Purpose |
|---|---|---|
| `DB_HOST` | `localhost` | database host |
| `DB_PORT` | `3306` | database port |
| `DB_NAME` | `logisticsdb` | database name |
| `DB_USERNAME` | `logistics` | database user |
| `DB_PASSWORD` | `logistics1pass` | database password |
| `JWT_SECRET` | *(none — required)* | key used to sign tokens |
| `APP_SECURITY_ENABLE_LOGIN` | `true` | set `false` to bypass authentication |
| `APP_SECURITY_CORS_ALLOWED_ORIGINS_<n>` | see `application.yml` | allowed browser origins |

## API endpoints

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/api/auth/register` | public | Creates an account, returns JWT token |
| `POST` | `/api/auth/login` | public | Returns JWT token |
| `GET` | `/api/auth/config` | public | Returns `loginEnabled` flag |
| `POST` | `/api/shipments/calculate` | required | Calculates and saves profit/loss |
| `GET` | `/api/shipments` | required | Paginated list of calculations |

## Tests

```bash
mvn test
```

---

## Deploying images

Build and push to the registry:

```bash
sudo docker buildx build --platform linux/amd64 -t dachser-backend .
sudo docker tag dachser-backend 192.168.1.3:5005/dachser-backend:latest
sudo docker push 192.168.1.3:5005/dachser-backend:latest
```

On the host, `docker compose up -d` brings up the database and the backend together.
