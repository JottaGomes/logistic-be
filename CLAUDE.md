# logistic-be

Backend of the Dachser logistics assessment: shipments, their income and cost lines,
profit calculation, and the Dachser AI chat assistant.

The frontend is a separate repository, `JottaGomes/logistic`, usually checked out next
to this one as `../logistics-frontend`. A change to an endpoint, DTO field or status
code is a change there too (`src/app/models`, `src/app/services`).

## Stack and commands

Spring Boot 3.1.5, Java 17, Spring Data JPA, MapStruct, Lombok, Spring Security with
JWT (8 h tokens), Bean Validation. H2 in memory by default (MySQL mode); `mariadb` and
`h2-file` profiles exist.

- Run: `mvn spring-boot:run` (http://localhost:8080). A backend change only takes effect
  after a restart; a stale process is the usual cause of "the change does not work".
- Test: `mvn test` (89 tests, including `@DataJpaTest` integration tests).
- Seed size: `app.seed.bulk-shipments` / `APP_SEED_BULK_SHIPMENTS`.

Layout under `src/main/java/com/jgomes/logistics/`: `controller` (HTTP only),
`service` (business rules), `repository` (Spring Data), `entity`, `dto`, `mapper`
(MapStruct), `exception` (`GlobalExceptionHandler`), `security`, `chat`.

## API

| Method | Path | Purpose |
|---|---|---|
| GET / POST | `/api/shipments` | search (`search`, `limit` up to 100) / create |
| GET | `/api/shipments/{reference}` | shipment with income and cost lines and running totals |
| POST | `/api/shipments/{reference}/incomes` and `/costs` | record an amount |
| PUT | `/api/shipments/{reference}/incomes/{id}` and `/costs/{id}` | correct an amount; the body carries `version`, a stale version answers 409 |
| POST | `/api/profit-calculations` | calculate; 201 when stored, 200 with `created: false` when the totals equal the latest calculation |
| GET | `/api/profit-calculations` | paged history (size up to 100, sort whitelist) |
| POST | `/api/chat` | Dachser AI assistant |
| POST | `/api/auth/login`, `/api/auth/register` | JWT |

Error mapping (`GlobalExceptionHandler`): database unavailable 503, stale version 409,
malformed body or wrong parameter type 400, unsupported verb 405 with `Allow`.

When an endpoint changes, update `README.md`, `http/*.http` and the Postman collection
with it. `DATABASE_QUESTIONS.md` answers the assessment questions by pointing at this
code; keep it true.

## Design points the written answers depend on

- Money is `DECIMAL(12,2)` / `BigDecimal`; compare amounts with `compareTo`, never `equals`
  (100.0 and 100.00 are not `equals`).
- `ProfitCalculationService.calculate` locks the shipment row (`findByReferenceForUpdate`,
  `SELECT ... FOR UPDATE`), sums in the database, and stores a new row only when the
  totals differ from the latest calculation.
- Amount corrections are a single conditional `UPDATE ... WHERE version = ?`
  (`IncomeRepository` / `CostRepository.correctAmount`): no row changed means 409.
- The chat assistant (`chat/`) calls Groq (`openai/gpt-oss-120b`, OpenAI-compatible tool
  calling) with four read-only tools in `ShipmentChatTools`. The model never writes SQL,
  results are capped at 20 rows, and the system prompt in `ChatService` refuses questions
  outside shipments, incomes, costs and profit.

## Secrets

- `src/main/resources/application.yml` is committed and must never contain a secret.
  Real values go in `application-local.yml` at the repository root (gitignored, loaded via
  `spring.config.import`; template in `application-local.yml.example`) or in environment
  variables: `GROQ_API_KEY`, `JWT_SECRET`, `CORS_ALLOWED_ORIGINS`.
- The working copy of `application.yml` often carries local, uncommitted edits (a smaller
  `bulk-shipments`, sometimes a key). Never stage it with `git add -A`; stage only the
  intended change, and never print or read a key's value.
- Before every push: `git log origin/main..HEAD -p | grep -cE 'gsk_[A-Za-z0-9]{20,}'`
  must print 0.

CORS default is `http://localhost:4200,https://*.vercel.app`, because the usual setup is
this backend on a developer machine and the frontend on Vercel.

## Working rules

- Code comments follow the `code-comments` skill: English, explain why, self-contained.
- Commit and push only when asked; end commit messages with the Co-Authored-By line.
