package com.jgomes.logistics.service;

import com.jgomes.logistics.dto.CalculateProfitRequestDTO;
import com.jgomes.logistics.dto.ProfitCalculationResponseDTO;
import com.jgomes.logistics.entity.Income;
import com.jgomes.logistics.entity.IncomeType;
import com.jgomes.logistics.mapper.ProfitCalculationMapperImpl;
import com.jgomes.logistics.repository.IncomeRepository;
import com.jgomes.logistics.repository.ProfitCalculationRepository;
import com.jgomes.logistics.repository.ShipmentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the Calculate Profit use case against the database seeded by data.sql, where
 * SHP-2026-0001 already has a stored calculation of 5300.00 income and 3200.00
 * costs, matching its recorded lines. This also runs the SELECT ... FOR UPDATE that
 * locks the shipment, which a mock cannot.
 */
@DataJpaTest
@Import({ProfitCalculationService.class, ProfitCalculationMapperImpl.class})
class ProfitCalculationDeduplicationTest {

    private static final String REFERENCE = "SHP-2026-0001";

    @Autowired
    private ProfitCalculationService service;

    @Autowired
    private ProfitCalculationRepository calculationRepository;

    @Autowired
    private IncomeRepository incomeRepository;

    @Autowired
    private ShipmentRepository shipmentRepository;

    private ProfitCalculationResponseDTO calculate() {
        return service.calculate(new CalculateProfitRequestDTO(REFERENCE));
    }

    @Test
    void repeatingACalculationWithNoChangeAddsNoRows() {

        long before = calculationRepository.count();

        ProfitCalculationResponseDTO first = calculate();
        ProfitCalculationResponseDTO second = calculate();

        assertThat(first.getCreated()).isFalse();
        assertThat(second.getCreated()).isFalse();
        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(calculationRepository.count()).isEqualTo(before);
    }

    @Test
    void aChangedAmountStoresOneNewCalculationAndThenStopsAgain() {

        long before = calculationRepository.count();

        incomeRepository.saveAndFlush(Income.builder()
                .shipment(shipmentRepository.findByReference(REFERENCE).orElseThrow())
                .incomeType(IncomeType.AGENT_INCOME)
                .amount(new BigDecimal("100.00"))
                .build());

        ProfitCalculationResponseDTO changed = calculate();
        ProfitCalculationResponseDTO repeated = calculate();

        assertThat(changed.getCreated()).isTrue();
        assertThat(changed.getProfitOrLoss()).isEqualByComparingTo("2200.00");
        assertThat(repeated.getCreated()).isFalse();
        assertThat(repeated.getId()).isEqualTo(changed.getId());
        assertThat(calculationRepository.count()).isEqualTo(before + 1);
    }
}
