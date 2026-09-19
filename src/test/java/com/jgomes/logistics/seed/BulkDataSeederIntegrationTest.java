package com.jgomes.logistics.seed;

import com.jgomes.logistics.dto.CalculateProfitRequestDTO;
import com.jgomes.logistics.dto.ProfitCalculationResponseDTO;
import com.jgomes.logistics.service.ProfitCalculationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Boots the application with the seeder switched on and checks it really writes
 * the volume NFR2 asks for, against a real database rather than a mock.
 *
 * The unit tests beside this one prove the seeder's decisions — when it runs, when
 * it skips, how many rows per shipment. This proves the SQL is valid and the data
 * it leaves behind adds up.
 *
 * Note the database is H2 in memory, so the rows exist only for the duration of
 * this test. To keep them, run the application itself with
 * {@code APP_SEED_BULK_SHIPMENTS=10000}, pointed at MariaDB if they need to
 * survive a restart.
 */
@SpringBootTest(properties = {
        "app.seed.bulk-shipments=10000",
        "app.security.enable-login=false",
})
class BulkDataSeederIntegrationTest {

    private static final int EXPECTED = 10_000;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ProfitCalculationService service;

    private int count(String table) {
        Integer rows = jdbc.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE shipment_id IN "
                        + "(SELECT id FROM shipment WHERE reference LIKE 'BULK-%')", Integer.class);
        return rows == null ? 0 : rows;
    }

    @Test
    void writesTenThousandShipmentsWithTheirIncomesAndCosts() {

        Integer shipments = jdbc.queryForObject(
                "SELECT COUNT(*) FROM shipment WHERE reference LIKE 'BULK-%'", Integer.class);

        assertThat(shipments).isEqualTo(EXPECTED);
        assertThat(count("income")).isEqualTo(EXPECTED * 3);
        assertThat(count("cost")).isEqualTo(EXPECTED * 2);
        assertThat(count("profit_calculation")).isEqualTo(EXPECTED);
    }

    @Test
    void leavesTheHandWrittenSeedDataAlone() {

        Integer original = jdbc.queryForObject(
                "SELECT COUNT(*) FROM shipment WHERE reference LIKE 'SHP-%'", Integer.class);

        assertThat(original).isEqualTo(4);
    }

    /** The generated amounts have to be real data, not placeholders that all match. */
    @Test
    void generatesVariedAmountsRatherThanIdenticalRows() {

        Integer distinct = jdbc.queryForObject(
                "SELECT COUNT(DISTINCT profit_or_loss) FROM profit_calculation", Integer.class);

        assertThat(distinct).isGreaterThan(100);
    }

    /** What the seeder stored must match what the use case computes from the rows. */
    @Test
    void theStoredTotalsAgreeWithARealCalculation() {

        ProfitCalculationResponseDTO calculated =
                service.calculate(new CalculateProfitRequestDTO("BULK-007777"));

        BigDecimal storedProfit = jdbc.queryForObject("""
                SELECT c.profit_or_loss FROM profit_calculation c
                JOIN shipment s ON s.id = c.shipment_id
                WHERE s.reference = 'BULK-007777' AND c.calculated_by = 'seed'
                """, BigDecimal.class);

        assertThat(calculated.getIncomes()).hasSize(3);
        assertThat(calculated.getCosts()).hasSize(2);
        assertThat(calculated.getProfitOrLoss())
                .isEqualByComparingTo(calculated.getTotalIncome().subtract(calculated.getTotalCosts()));
        assertThat(storedProfit).isEqualByComparingTo(calculated.getProfitOrLoss());
    }
}
