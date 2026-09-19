# Database questions

Answers to the five questions in the assessment, written against the code in this
repository rather than in the abstract. File references point at the real thing.

---

## 1. How would you design the database schema for this system?

Four tables, in 3NF:

```
shipment (id PK, reference UNIQUE, customer, created_at)
   │
   ├──< income  (id PK, shipment_id FK, income_type, amount, description)
   ├──< cost    (id PK, shipment_id FK, cost_type,   amount, description)
   └──< profit_calculation (id PK, shipment_id FK, total_income, total_costs,
                            profit_or_loss, calculated_by, calculated_at)
```

See `src/main/resources/schema.sql`.

The shape follows from the requirements rather than from convenience:

- **FR1** says the system must handle customer *payments* for each shipment —
  plural. A shipment can be invoiced more than once (main carriage, fuel
  surcharge), so income cannot be a column on `shipment`; it is its own table
  with a row per amount received.
- **FR2** says *all* costs related to service provision. Same reasoning: line
  haul, handling and customs are separate facts, each with its own amount.
- The use case defines total income as "the sum of all customer payments **and
  any additional income from agents**". Those two are the same kind of fact
  differing only in origin, so they share a table and are told apart by
  `income_type` (`CUSTOMER_PAYMENT` / `AGENT_INCOME`). A separate `agent_income`
  table would have had identical columns — that is duplication, not normalisation.
- `profit_calculation` is deliberately **not** a set of columns on `shipment`. A
  calculation is an event with a timestamp and an author, not an attribute of the
  shipment. Keeping it separate means a recalculation after a late invoice
  arrives adds a row instead of destroying the number that was already reported.

Indexes: every foreign key gets one (`idx_income_shipment`, `idx_cost_shipment`,
`idx_calculation_shipment`), because the whole use case is "find the rows for this
shipment". `profit_calculation.calculated_at` is indexed too, since the listing is
ordered by it. `shipment.reference` is UNIQUE, which gives both the business rule
and the index used to look a shipment up.

## 2. What was the most relevant criteria to get this database design?

**Storing facts once, and never storing what can be derived.**

The tempting shortcut is to keep `total_income` and `total_costs` on `shipment` and
update them whenever something is added. That is a denormalisation, and it creates
a second source of truth: the moment one code path inserts an income without
updating the total, the database contains two contradictory answers and no way to
tell which is right. Deriving the totals with `SUM` means they cannot disagree with
the rows they come from.

The one place this repository *does* store a derived value is
`profit_calculation`, and that is a conscious exception, for the reason in the
previous answer: it records what was reported at a point in time. It is an audit
record, not a cache.

Secondary criteria, in order of weight: the granularity the requirements demand
(one row per payment, not per shipment); keeping the calculation cheap as data
grows; and making invalid states unrepresentable — `NOT NULL` on every amount, a
foreign key so a cost cannot exist without a shipment, and `DECIMAL(12,2)` so no
amount is ever a float.

## 3. How would you ensure the accuracy and consistency of the profit data?

Five things, all of them in the code:

**`DECIMAL` in the schema, `BigDecimal` in Java.** `Income.amount` and every
other money field is `DECIMAL(12,2)` mapped to `BigDecimal`. A `double` cannot
represent 0.10 exactly; summing a few hundred of them produces a number that is
visibly wrong to an accountant. This is the single most common way money code
breaks.

**The caller cannot supply the amounts.** `CalculateProfitRequestDTO` carries a
shipment reference and nothing else. The service reads the amounts from the
database itself (`ProfitCalculationService.calculate`), so two people asking for
the same shipment necessarily get the same answer. If the endpoint accepted
income and cost figures, the number would only be as accurate as whoever typed it.

**One transaction.** `calculate` is `@Transactional`: the reads that produce the
totals and the write that stores the result are one unit. A calculation cannot be
stored against amounts that changed halfway through.

**Sum in the database.** `IncomeRepository.sumAmountByShipmentId` uses
`SUM` with `COALESCE(..., 0)` rather than loading rows and adding them in Java.
The database reads a consistent snapshot, and a shipment with no income yet
yields zero instead of a null that would later blow up.

**Constraints, not conventions.** `NOT NULL` on the amounts, foreign keys to
`shipment`, and `UNIQUE` on `reference`. A bug in application code cannot leave an
orphaned cost or a second shipment with the same reference.

And the calculation itself is covered by tests that assert the arithmetic, not
just that a value came back — profit, loss, zero, and the unknown-shipment case
(`ProfitCalculationServiceTest`).

## 4. How would you handle large datasets efficiently?

NFR2 asks for 10,000 shipments a day. That is roughly 3.6M shipments a year and,
at a handful of income and cost rows each, some tens of millions of rows. What
matters is that no operation's cost grows with that total.

**Aggregate in the database.** The calculation is two `SUM` queries touching only
the rows for one shipment, via the index on `shipment_id`. It never loads a
collection into memory, so a shipment with 3 income rows and one with 300 cost the
same order of time.

**Never return an unbounded list.** `/api/shipments/calculations` is paged
(`Page<ProfitCalculation>`, `PageRequest`), so the response size is fixed
regardless of how much history exists. The sort column is indexed, so the database
does not sort millions of rows to return ten.

**Avoid N+1.** Listing calculations needs each row's shipment reference. Loading
them lazily would issue one extra query per row; `ProfitCalculationRepository`
declares `@EntityGraph(attributePaths = "shipment")` so the page is one join.
`spring.jpa.open-in-view` is set to `false`, which makes a lazy load outside the
service fail loudly instead of quietly issuing queries from the view layer.

**Keep relations lazy.** `Income.shipment` and `Cost.shipment` are
`FetchType.LAZY`; the default `EAGER` on `@ManyToOne` would drag a shipment along
with every row read.

**Measured, not assumed.** Against a MariaDB holding 10,004 shipments, 50,014
income and cost rows and 10,004 calculations: a calculation takes 109 ms, the
first page of history 26 ms, and page 500 of that history 15 ms — the last page
costs the same as the first, which is the property that matters.

That load test also found the one place this had been got wrong: `GET /shipments`
returned every row, 639 kB and 177 ms at this volume, to populate a dropdown. It
now takes a search term and a limit capped at 100, and answers in 12 ms with
1.3 kB. Worth stating plainly, because it is the failure mode this whole answer is
about, and reasoning alone had not caught it.

Beyond what is implemented, the next steps in order would be: a composite index on
`(shipment_id, income_type)` if reporting starts slicing by type; range
partitioning `profit_calculation` by `calculated_at`, since queries are
overwhelmingly recent-first; and a read replica before any caching, because a
cached total is exactly the second source of truth that answer 2 avoids.

## 5. Explain how you would implement the profit calculation logic in Java, and why that way

It lives in `ProfitCalculationService.calculate`, and follows the use case step by
step:

```java
Shipment shipment = shipmentRepository.findByReference(reference)
        .orElseThrow(() -> new ShipmentNotFoundException(reference));   // step 1

BigDecimal totalIncome = incomeRepository.sumAmountByShipmentId(shipment.getId()); // step 2
BigDecimal totalCosts  = costRepository.sumAmountByShipmentId(shipment.getId());   // step 3

BigDecimal profitOrLoss = totalIncome.subtract(totalCosts);             // step 4

calculationRepository.save(...);                                        // step 5
return mapper.toResponseDTO(saved);                                     // step 6
```

**Why in the service layer.** The controller's job is HTTP and the repository's is
data access. Putting the arithmetic in either makes it unreachable from anywhere
else — a scheduled job or a batch import would have to go through a web request
to reuse it. The service is the one place with no framework in the way, which is
also why it is the layer the tests target.

**Why `BigDecimal`.** As in answer 3: money is decimal, `double` is binary, and
the error is silent until someone reconciles a report.

**Why the amounts are fetched, not received.** This is the part the use case is
explicit about — "System retrieves income data", "System retrieves cost data" —
and it is what makes the result reproducible. It also means there is no
`if (costs > income)` branch anywhere: a loss is simply a negative result of one
subtraction, so profit and loss cannot drift apart in behaviour.

**Why a new row per calculation.** `save` always inserts. Re-evaluating a shipment
after a late cost arrives does not overwrite the figure someone already acted on.

**SOLID, concretely.** Single responsibility: the service calculates, the mapper
translates, the repositories fetch. Dependency inversion: the service depends on
the Spring Data interfaces, which is what lets the tests run against mocks with no
database. Open/closed: adding a new `IncomeType` needs no change here, because the
sum does not enumerate types — the enum exists for reporting, not for arithmetic.

**What I deliberately did not do.** No strategy pattern or rules engine for a
subtraction. The use case defines exactly one formula; abstracting it would add
indirection to a single line and make the thing harder to read, not more flexible.
If per-customer rules ever appear, that is the moment to introduce the seam.
