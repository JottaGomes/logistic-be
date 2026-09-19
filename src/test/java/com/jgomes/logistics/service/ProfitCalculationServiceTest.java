package com.jgomes.logistics.service;

import com.jgomes.logistics.dto.CalculateProfitRequestDTO;
import com.jgomes.logistics.dto.ProfitCalculationResponseDTO;
import com.jgomes.logistics.entity.ProfitCalculation;
import com.jgomes.logistics.entity.Shipment;
import com.jgomes.logistics.exception.ShipmentNotFoundException;
import com.jgomes.logistics.mapper.ProfitCalculationMapper;
import com.jgomes.logistics.repository.CostRepository;
import com.jgomes.logistics.repository.IncomeRepository;
import com.jgomes.logistics.repository.ProfitCalculationRepository;
import com.jgomes.logistics.repository.ShipmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfitCalculationServiceTest {

    private static final String REFERENCE = "SHP-2026-0001";

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private IncomeRepository incomeRepository;

    @Mock
    private CostRepository costRepository;

    @Mock
    private ProfitCalculationRepository calculationRepository;

    @Mock
    private ProfitCalculationMapper mapper;

    @InjectMocks
    private ProfitCalculationService service;

    private Shipment shipment() {
        return Shipment.builder().id(1L).reference(REFERENCE).customer("Sonae").build();
    }

    private void stubCalculation(BigDecimal income, BigDecimal costs) {
        when(shipmentRepository.findByReference(REFERENCE)).thenReturn(Optional.of(shipment()));
        when(incomeRepository.sumAmountByShipmentId(1L)).thenReturn(income);
        when(costRepository.sumAmountByShipmentId(1L)).thenReturn(costs);
        when(calculationRepository.save(any())).thenAnswer(call -> call.getArgument(0));
        when(mapper.toResponseDTO(any())).thenReturn(new ProfitCalculationResponseDTO());
        when(incomeRepository.findByShipmentId(1L)).thenReturn(List.of());
        when(costRepository.findByShipmentId(1L)).thenReturn(List.of());
    }

    @Test
    void calculate_whenIncomeExceedsCosts_storesAProfit() {
        stubCalculation(new BigDecimal("5300.00"), new BigDecimal("3200.00"));

        service.calculate(new CalculateProfitRequestDTO(REFERENCE));

        ArgumentCaptor<ProfitCalculation> saved = ArgumentCaptor.forClass(ProfitCalculation.class);
        verify(calculationRepository).save(saved.capture());
        assertThat(saved.getValue().getTotalIncome()).isEqualByComparingTo("5300.00");
        assertThat(saved.getValue().getTotalCosts()).isEqualByComparingTo("3200.00");
        assertThat(saved.getValue().getProfitOrLoss()).isEqualByComparingTo("2100.00");
    }

    @Test
    void calculate_whenCostsExceedIncome_storesALoss() {
        stubCalculation(new BigDecimal("3000.00"), new BigDecimal("4500.00"));

        service.calculate(new CalculateProfitRequestDTO(REFERENCE));

        ArgumentCaptor<ProfitCalculation> saved = ArgumentCaptor.forClass(ProfitCalculation.class);
        verify(calculationRepository).save(saved.capture());
        assertThat(saved.getValue().getProfitOrLoss()).isEqualByComparingTo("-1500.00");
    }

    @Test
    void calculate_withNoRecordedAmounts_isZero() {
        stubCalculation(BigDecimal.ZERO, BigDecimal.ZERO);

        service.calculate(new CalculateProfitRequestDTO(REFERENCE));

        ArgumentCaptor<ProfitCalculation> saved = ArgumentCaptor.forClass(ProfitCalculation.class);
        verify(calculationRepository).save(saved.capture());
        assertThat(saved.getValue().getProfitOrLoss()).isEqualByComparingTo("0");
    }

    @Test
    void calculate_trimsTheReferenceBeforeLookingItUp() {
        stubCalculation(new BigDecimal("100.00"), new BigDecimal("40.00"));

        service.calculate(new CalculateProfitRequestDTO("  " + REFERENCE + "  "));

        verify(shipmentRepository).findByReference(REFERENCE);
    }

    @Test
    void calculate_withUnknownShipment_throwsAndSavesNothing() {
        when(shipmentRepository.findByReference("SHP-NOPE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.calculate(new CalculateProfitRequestDTO("SHP-NOPE")))
                .isInstanceOf(ShipmentNotFoundException.class)
                .hasMessageContaining("SHP-NOPE");

        verify(calculationRepository, never()).save(any());
    }

    @Test
    void findShipments_withoutASearchReturnsABoundedSlice() {
        when(shipmentRepository.findSlice(any())).thenReturn(List.of(shipment()));
        when(mapper.toShipmentDTOs(any())).thenReturn(List.of());

        service.findShipments(null, 20);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(shipmentRepository).findSlice(pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(20);
    }

    /** A crafted limit must not turn into "give me the whole table". */
    @Test
    void findShipments_capsTheLimit() {
        when(shipmentRepository.findSlice(any())).thenReturn(List.of());
        when(mapper.toShipmentDTOs(any())).thenReturn(List.of());

        service.findShipments(null, 100000);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(shipmentRepository).findSlice(pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(100);
    }

    @Test
    void findShipments_withASearchMatchesReferenceOrCustomer() {
        when(shipmentRepository.search(any(), any())).thenReturn(List.of(shipment()));
        when(mapper.toShipmentDTOs(any())).thenReturn(List.of());

        service.findShipments("  Sonae ", 20);

        verify(shipmentRepository).search(eq("%sonae%"), any());
    }
}
