package com.jgomes.logistics.repository;

import com.jgomes.logistics.entity.Cost;
import com.jgomes.logistics.entity.Income;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the conditional UPDATE behind amount corrections against a real database,
 * seeded by schema.sql and data.sql. Shipment 1 owns incomes 1 to 3 and costs 1
 * and 2, all at version 0.
 *
 * A mock cannot prove this part: the guarantee comes from the WHERE clause being
 * evaluated by the database in the same statement that writes the row.
 */
@DataJpaTest
class AmountCorrectionRepositoryTest {

    private static final long SHIPMENT = 1L;

    @Autowired
    private IncomeRepository incomeRepository;

    @Autowired
    private CostRepository costRepository;

    @Test
    void correctAmount_withTheCurrentVersion_changesTheRowAndBumpsTheVersion() {

        int changed = incomeRepository.correctAmount(1L, SHIPMENT, new BigDecimal("4350.00"), "Re-invoiced", 0L);

        Income income = incomeRepository.findById(1L).orElseThrow();
        assertThat(changed).isEqualTo(1);
        assertThat(income.getAmount()).isEqualByComparingTo("4350.00");
        assertThat(income.getDescription()).isEqualTo("Re-invoiced");
        assertThat(income.getVersion()).isEqualTo(1L);
    }

    /**
     * Two people read the same cost at version 0 and both submit a correction. The
     * first one wins; the second changes nothing, so its value cannot overwrite the
     * first without anyone noticing.
     */
    @Test
    void correctAmount_secondCorrectionFromTheSameVersion_changesNothing() {

        int first = costRepository.correctAmount(1L, SHIPMENT, new BigDecimal("3200.00"), "Line haul", 0L);
        int second = costRepository.correctAmount(1L, SHIPMENT, new BigDecimal("2900.00"), "Line haul", 0L);

        Cost cost = costRepository.findById(1L).orElseThrow();
        assertThat(first).isEqualTo(1);
        assertThat(second).isZero();
        assertThat(cost.getAmount()).isEqualByComparingTo("3200.00");
        assertThat(cost.getVersion()).isEqualTo(1L);
    }

    @Test
    void correctAmount_throughAnotherShipment_changesNothing() {

        int changed = incomeRepository.correctAmount(1L, 2L, new BigDecimal("1.00"), null, 0L);

        assertThat(changed).isZero();
        assertThat(incomeRepository.findById(1L).orElseThrow().getAmount()).isEqualByComparingTo("4200.00");
    }

    @Test
    void existsByIdAndShipmentId_tellsAStaleVersionApartFromAMissingLine() {

        assertThat(incomeRepository.existsByIdAndShipmentId(1L, SHIPMENT)).isTrue();
        assertThat(incomeRepository.existsByIdAndShipmentId(1L, 2L)).isFalse();
        assertThat(incomeRepository.existsByIdAndShipmentId(999L, SHIPMENT)).isFalse();
    }
}
