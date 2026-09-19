package com.jgomes.logistics.seed;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Fills the database with generated shipments, so the application can be exercised
 * at the volume NFR2 asks for — 10,000 shipments a day.
 *
 * Inert unless {@code app.seed.bulk-shipments} is set above zero, so it never runs
 * by accident. Every row it writes has a {@code BULK-} reference, which keeps the
 * generated data recognisable and lets a second run detect that it already ran.
 *
 * Everything is written with JDBC batches rather than JPA: this is 60,000 rows,
 * and persisting them one entity at a time would take minutes instead of seconds.
 * All values are computed in Java rather than in SQL so the script stays portable
 * between H2 and MariaDB.
 */
@Slf4j
@Component
public class BulkDataSeeder implements CommandLineRunner {

    private static final int BATCH_SIZE = 1000;
    private static final String REFERENCE_PREFIX = "BULK-";

    private final JdbcTemplate jdbc;
    private final int shipmentsToCreate;

    public BulkDataSeeder(
            JdbcTemplate jdbc,
            @Value("${app.seed.bulk-shipments:0}") int shipmentsToCreate) {
        this.jdbc = jdbc;
        this.shipmentsToCreate = shipmentsToCreate;
    }

    @Override
    public void run(String... args) {

        if (shipmentsToCreate <= 0) {
            return;
        }

        Integer existing = jdbc.queryForObject(
                "SELECT COUNT(*) FROM shipment WHERE reference LIKE ?", Integer.class, REFERENCE_PREFIX + "%");

        if (existing != null && existing > 0) {
            log.info("bulk_seed_skipped reason=already_present shipments={}", existing);
            return;
        }

        long startedAt = System.currentTimeMillis();

        insertShipments();
        List<Long> ids = generatedShipmentIds();
        insertIncomes(ids);
        insertCosts(ids);
        insertCalculations(ids);

        log.info("bulk_seed_done shipments={} incomes={} costs={} calculations={} tookMs={}",
                ids.size(), ids.size() * 3, ids.size() * 2, ids.size(),
                System.currentTimeMillis() - startedAt);
    }

    private void insertShipments() {

        List<Object[]> rows = new ArrayList<>(shipmentsToCreate);

        for (int n = 1; n <= shipmentsToCreate; n++) {
            rows.add(new Object[] {
                    REFERENCE_PREFIX + String.format("%06d", n),
                    "Customer " + String.format("%03d", n % 500),
            });
        }

        batch("INSERT INTO shipment (reference, customer) VALUES (?, ?)", rows);
    }

    private List<Long> generatedShipmentIds() {
        return jdbc.queryForList(
                "SELECT id FROM shipment WHERE reference LIKE ? ORDER BY id",
                Long.class, REFERENCE_PREFIX + "%");
    }

    private void insertIncomes(List<Long> ids) {

        List<Object[]> rows = new ArrayList<>(ids.size() * 3);

        for (Long id : ids) {
            rows.add(new Object[] { id, "CUSTOMER_PAYMENT", mainInvoice(id), "Main carriage invoice" });
            rows.add(new Object[] { id, "CUSTOMER_PAYMENT", fuelSurcharge(id), "Fuel surcharge" });
            rows.add(new Object[] { id, "AGENT_INCOME", agentShare(id), "Destination agent share" });
        }

        batch("INSERT INTO income (shipment_id, income_type, amount, description) VALUES (?, ?, ?, ?)", rows);
    }

    private void insertCosts(List<Long> ids) {

        List<Object[]> rows = new ArrayList<>(ids.size() * 2);

        for (Long id : ids) {
            rows.add(new Object[] { id, "MAIN_CARRIAGE", lineHaul(id), "Line haul" });
            rows.add(new Object[] { id, "HANDLING", handling(id), "Terminal handling" });
        }

        batch("INSERT INTO cost (shipment_id, cost_type, amount, description) VALUES (?, ?, ?, ?)", rows);
    }

    /**
     * A calculation per shipment, so the history has something to page and sort
     * through. The timestamps are spread over the past day rather than all being
     * "now", otherwise sorting by date has nothing to sort.
     */
    private void insertCalculations(List<Long> ids) {

        LocalDateTime now = LocalDateTime.now();
        List<Object[]> rows = new ArrayList<>(ids.size());

        for (Long id : ids) {

            BigDecimal income = mainInvoice(id).add(fuelSurcharge(id)).add(agentShare(id));
            BigDecimal costs = lineHaul(id).add(handling(id));

            rows.add(new Object[] {
                    id, income, costs, income.subtract(costs), "seed",
                    Timestamp.valueOf(now.minusSeconds(id % 86_400)),
            });
        }

        batch("""
                INSERT INTO profit_calculation
                    (shipment_id, total_income, total_costs, profit_or_loss, calculated_by, calculated_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """, rows);
    }

    /** Amounts vary with the id so the data is not uniform, and stay reproducible. */
    private BigDecimal mainInvoice(Long id) {
        return amount(3000 + id % 900);
    }

    private BigDecimal fuelSurcharge(Long id) {
        return amount(500 + id % 300);
    }

    private BigDecimal agentShare(Long id) {
        return amount(100 + id % 200);
    }

    private BigDecimal lineHaul(Long id) {
        return amount(2500 + id % 800);
    }

    private BigDecimal handling(Long id) {
        return amount(150 + id % 250);
    }

    private BigDecimal amount(long value) {
        return BigDecimal.valueOf(value).setScale(2);
    }

    private void batch(String sql, List<Object[]> rows) {

        for (int from = 0; from < rows.size(); from += BATCH_SIZE) {
            jdbc.batchUpdate(sql, rows.subList(from, Math.min(from + BATCH_SIZE, rows.size())));
        }
    }
}
