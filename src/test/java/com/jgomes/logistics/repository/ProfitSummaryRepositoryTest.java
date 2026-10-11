package com.jgomes.logistics.repository;

import com.jgomes.logistics.entity.ProfitCalculation;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the portfolio summary query against the database seeded by data.sql:
 * four shipments with one calculation each, at 2100.00, 650.00, -550.00 and
 * -1500.00.
 */
@DataJpaTest
class ProfitSummaryRepositoryTest {

    @Autowired
    private ProfitCalculationRepository calculationRepository;

    @Autowired
    private ShipmentRepository shipmentRepository;

    @Test
    void summarizeLatest_countsEachShipmentOnce() {

        Object[] row = calculationRepository.summarizeLatest().get(0);

        assertThat(((Number) row[0]).longValue()).isEqualTo(4);
        assertThat(((Number) row[1]).longValue()).isEqualTo(2);
        assertThat(new BigDecimal(row[2].toString())).isEqualByComparingTo("700.00");
    }

    /** A newer calculation replaces the older one of the same shipment rather than adding to it. */
    @Test
    void summarizeLatest_usesOnlyTheMostRecentCalculationOfAShipment() {

        calculationRepository.saveAndFlush(ProfitCalculation.builder()
                .shipment(shipmentRepository.findByReference("SHP-2026-0001").orElseThrow())
                .totalIncome(new BigDecimal("5300.00"))
                .totalCosts(new BigDecimal("5400.00"))
                .profitOrLoss(new BigDecimal("-100.00"))
                .calculatedBy("test")
                .build());

        Object[] row = calculationRepository.summarizeLatest().get(0);

        assertThat(((Number) row[0]).longValue()).isEqualTo(4);
        assertThat(((Number) row[1]).longValue()).isEqualTo(3);
        assertThat(new BigDecimal(row[2].toString())).isEqualByComparingTo("-1500.00");
    }
}
