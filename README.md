# Logistics — Income and Cost Evaluation System (Backend)

Evaluates the income and costs generated during main carriage, and calculates the
profit or loss for each shipment.

This repository covers **Example 1 — Java Backend Implementation**. The Angular
frontend (Example 2) is at https://github.com/JottaGomes/logistic.

## Stack

| Layer | Technology |
|---|---|
| Backend | Java 17, Spring Boot 3.1.5, Spring Security, Spring Data JPA |
| Mapping | MapStruct |
| Database | H2, embedded and in-memory (default) · MariaDB (optional profile) |
| Auth | JWT, stateless, 8h expiry |
| Tests | JUnit 5, Mockito, AssertJ |

## Run it

No database to install — H2 runs in memory inside the application:

```bash
mvn spring-boot:run
```

- API: http://localhost:8080
- H2 console: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:logisticsdb`, user `sa`, no password)

The schema and seed data are applied at startup from `schema.sql` and `data.sql`.

To skip authentication while trying the endpoints out:

```bash
APP_SECURITY_ENABLE_LOGIN=false mvn spring-boot:run
```

Otherwise log in first — the seeded account is `joao` / `logistics2026`.

### Optional: MariaDB instead of H2

Everything works without this. It exists to show the model is not tied to H2:

```bash
docker compose up -d --build
```

Brings up MariaDB with a named volume plus the backend on the `mariadb` profile.
Data survives `docker compose down`; `docker compose down -v` wipes it.

## The Calculate Profit use case

The Finance Department names a shipment. The system reads the income and cost
records already held against it, sums them, calculates the difference, stores the
outcome and returns it.

The amounts are never supplied by the caller — that is what makes the result
reproducible, and it is what the use case document describes in steps 2 to 4.

### Recording the data it depends on

The use case pre-supposes that income and cost data is already in the system.
Putting it there is what the requirements call customer payment administration and
operational cost administration (section 1.4), and it lives on its own endpoints
and its own screen so the use case under assessment stays self-contained.

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/api/shipments` | required | Registers a shipment; `409` if the reference is taken |
| `GET` | `/api/shipments/{reference}` | required | What is recorded, with running totals |
| `POST` | `/api/shipments/{reference}/incomes` | required | Records a customer payment or agent income |
| `POST` | `/api/shipments/{reference}/costs` | required | Records an operational cost |

### Evaluating it

| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/api/shipments` | required | Shipments to evaluate; `search` and `limit`, capped at 100 |
| `POST` | `/api/shipments/calculate` | required | Calculates and stores profit or loss |
| `GET` | `/api/shipments/calculations` | required | Stored calculations, paged, sorted and filtered |

`/api/shipments/calculations` takes `page`, `size`, `sort`, `direction` and
`search`. Sorting and filtering are done in the database rather than the browser,
because only one page is ever loaded — filtering client-side would silently ignore
every match on another page. `sort` is matched against a fixed set of columns, so
an unexpected value falls back to the date instead of reaching the persistence
layer.
| `POST` | `/api/auth/login` | public | Returns a JWT |
| `POST` | `/api/auth/register` | public | Creates an account, returns a JWT |
| `GET` | `/api/auth/config` | public | Whether login is enabled |

```bash
curl -X POST http://localhost:8080/api/shipments/calculate \
  -H 'Content-Type: application/json' \
  -d '{"shipmentReference":"SHP-2026-0001"}'
```

```json
{
  "success": true,
  "data": {
    "shipmentReference": "SHP-2026-0001",
    "customer": "Sonae Distribuicao",
    "totalIncome": 5300.00,
    "totalCosts": 3200.00,
    "profitOrLoss": 2100.00,
    "incomes": [ "..." ],
    "costs": [ "..." ]
  }
}
```

Errors: unknown shipment → `404`; blank reference → `400`. Both are logged before
the caller is told, which is alternative flow I in the use case document.

## Data model

```
shipment (id, reference UNIQUE, customer, created_at)
   |--< income  (id, shipment_id FK, income_type, amount, description)
   |--< cost    (id, shipment_id FK, cost_type,   amount, description)
   |--< profit_calculation (id, shipment_id FK, total_income, total_costs,
                            profit_or_loss, calculated_by, calculated_at)
```

Normalised to 3NF: one row per amount received or spent, totals derived rather
than stored, and the calculation kept as a dated record instead of overwriting the
shipment. `income_type` separates customer payments from agent income, which the
use case sums together. Indexes on all foreign keys and on `calculated_at`.

Full reasoning in **[DATABASE_QUESTIONS.md](DATABASE_QUESTIONS.md)**.

## Structure

```
entity/       Shipment, Income, Cost, ProfitCalculation, User
repository/   Spring Data interfaces; SUM aggregation lives here
dto/          request and response objects; entities never leave the service
mapper/       MapStruct, entity <-> DTO
service/      ProfitCalculationService - the use case
              ShipmentAdministrationService - recording the data it reads
controller/   ProfitCalculationController - the Calculate Profit use case
              ShipmentAdministrationController - payment and cost administration
security/     JWT filter, token utility, security configuration
exception/    GlobalExceptionHandler and ShipmentNotFoundException
seed/         BulkDataSeeder - generated data for load testing, off by default
```

## Tests

```bash
mvn test
```

54 tests, covering the arithmetic (profit, loss, zero), the unknown-shipment path,
recording amounts and rejecting bad ones, duplicate references, pagination and
sorting, the security layer and the error handler.

## Testing the endpoints

- **Postman**: import `postman/Logistics-Calculate-Profit.postman_collection.json`.
  24 requests with 42 assertions, including a folder that walks the whole path —
  create a shipment, record its amounts, then calculate it. Run it with
  `npx newman run postman/Logistics-Calculate-Profit.postman_collection.json`.
- **IntelliJ / VS Code**: the `.http` files in `http/`.

## Generating test data

Two ways, and they are for different things.

### To check the seeder works — `mvn test`

`BulkDataSeederIntegrationTest` boots the application against H2, seeds 10,000
shipments for real and asserts the counts and the sums. It runs as part of the
normal suite and adds about two seconds.

```bash
mvn test -Dtest=BulkDataSeederIntegrationTest
```

The rows exist only for the length of that test: H2 lives inside the JVM, so they
go when it exits. Use it to prove the seeder is correct, not to fill a database
you then want to click around in.

### To fill a database you can use — run the app

To exercise the app at the volume NFR2 asks for, start it with a count:

```bash
APP_SEED_BULK_SHIPMENTS=10000 mvn spring-boot:run
```

or, as an argument:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments=--app.seed.bulk-shipments=10000
```

That writes 10,000 shipments, 30,000 income rows, 20,000 cost rows and 10,000
calculations — around a second on H2. **Wait for the line before using the app**,
because it accepts requests while the seeding is still running:

```
bulk_seed_done shipments=10000 incomes=30000 costs=20000 calculations=10000 tookMs=821
```

The generated references all start with `BULK-`, which keeps them recognisable and
lets a second run detect that it already seeded — so against MariaDB you can
restart with the flag still set without doubling the data. On H2 the database is
in memory, so every restart starts empty and seeds again.

Against H2 the data still disappears on restart, because the database is in
memory. For 10,000 shipments that survive, point the app at MariaDB:

```bash
docker compose up -d mariadb
SPRING_PROFILES_ACTIVE=mariadb APP_SEED_BULK_SHIPMENTS=10000 mvn spring-boot:run
```

Restart it afterwards without the flag and the data is still there; the seeder
recognises its own previous run, so leaving the flag set does not double it
either.

Without the property the seeder does nothing at all, so it cannot fire by
accident; `src/main/resources/data.sql` still provides the four hand-written
shipments used by the tests and the Postman collection.

## Non-functional requirements

Measured against a MariaDB loaded with **10,004 shipments, 50,014 income and cost
rows and 10,004 stored calculations** — the daily volume NFR2 asks for. Averages
of five calls each.

| Operation | Time | Response |
|---|---|---|
| `POST /calculate` one shipment | 109 ms | — |
| `GET /calculations` first page | 26 ms | 2.4 kB |
| `GET /calculations` page 500 | 15 ms | 2.4 kB |
| `GET /calculations` sorted by profit | 14 ms | 2.4 kB |
| `GET /calculations` filtered | 12 ms | 2.4 kB |
| `GET /shipments` | 12 ms | 1.3 kB |

**NFR1** (calculate within 0.5 minutes) — met with room to spare: 109 ms against a
30,000 ms budget.

**NFR2** (10,000 shipments a day) — met. Nothing grows with the row count: the
totals are two indexed `SUM` queries, and every listing is a bounded page, which
is why the last page costs the same as the first.

This is also what the load test caught: `GET /shipments` used to return the whole
table — 639 kB and 177 ms at this volume, feeding a dropdown with ten thousand
options. It is now a bounded, searchable slice, capped at 100 server-side, and the
UI narrows it with a typeahead instead of holding the list.

**NFR3** (99.9% uptime) — not something the code can claim on its own. It needs a
deployment with health checks, restart policies and more than one instance. What
is here that helps: the app is stateless (JWT, no session), so it scales
horizontally, and `docker-compose.yml` gives the database a health check the
backend waits on.

**NFR4** (under two hours of training) — not measured. Two screens, one action
each, and every amount labelled; that is the intent, but only real users can
confirm it.

## Configuration

| Variable | Default | Purpose |
|---|---|---|
| `JWT_SECRET` | the development key in `application.yml` | key used to sign tokens |
| `APP_SECURITY_ENABLE_LOGIN` | `true` | `false` bypasses authentication |
| `DB_HOST` / `DB_PORT` / `DB_NAME` | `localhost` / `3306` / `logisticsdb` | MariaDB profile only |
| `DB_USERNAME` / `DB_PASSWORD` | `logistics` / `logistics1pass` | MariaDB profile only |

## Assessment deliverables

| Required | Where |
|---|---|
| Source code that builds and runs | this repository, `mvn spring-boot:run` |
| Full SQL structure — tables, relations, indexes | `src/main/resources/schema.sql` |
| Insertion scripts, 3–4 records per entity | `src/main/resources/data.sql` |
| Embedded in-memory database | H2, the default profile |
| Entities / Repositories / DTO / Mapper / Service / Controller | `src/main/java/...`; one controller for the use case, a second for administration |
| Endpoint collection | `postman/`, plus `http/` |
| Database question answers | [DATABASE_QUESTIONS.md](DATABASE_QUESTIONS.md) |
| Unit tests (optional) | `mvn test`, 54 tests |
