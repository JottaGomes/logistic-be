package com.jgomes.logistics.service;

import com.jgomes.logistics.dto.CorrectAmountRequestDTO;
import com.jgomes.logistics.dto.CreateShipmentRequestDTO;
import com.jgomes.logistics.dto.RecordAmountRequestDTO;
import com.jgomes.logistics.dto.ShipmentResponseDTO;
import com.jgomes.logistics.entity.Cost;
import com.jgomes.logistics.entity.CostType;
import com.jgomes.logistics.entity.Income;
import com.jgomes.logistics.entity.IncomeType;
import com.jgomes.logistics.entity.Shipment;
import com.jgomes.logistics.exception.AmountLineNotFoundException;
import com.jgomes.logistics.exception.DuplicateShipmentException;
import com.jgomes.logistics.exception.ShipmentNotFoundException;
import com.jgomes.logistics.exception.StaleAmountLineException;
import com.jgomes.logistics.mapper.ProfitCalculationMapper;
import com.jgomes.logistics.repository.CostRepository;
import com.jgomes.logistics.repository.IncomeRepository;
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
class ShipmentAdministrationServiceTest {

    private static final String REFERENCE = "SHP-2026-0001";

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private IncomeRepository incomeRepository;

    @Mock
    private CostRepository costRepository;

    @Mock
    private ProfitCalculationMapper mapper;

    @InjectMocks
    private ShipmentAdministrationService service;

    private Shipment shipment() {
        return Shipment.builder().id(1L).reference(REFERENCE).customer("Sonae").build();
    }

    private void stubDetail() {
        when(shipmentRepository.findByReference(REFERENCE)).thenReturn(Optional.of(shipment()));
        when(incomeRepository.findByShipmentId(1L)).thenReturn(List.of());
        when(costRepository.findByShipmentId(1L)).thenReturn(List.of());
        when(incomeRepository.sumAmountByShipmentId(1L)).thenReturn(BigDecimal.ZERO);
        when(costRepository.sumAmountByShipmentId(1L)).thenReturn(BigDecimal.ZERO);
    }

    @Test
    void createShipment_trimsAndSaves() {
        when(shipmentRepository.findByReference(REFERENCE)).thenReturn(Optional.empty());
        when(shipmentRepository.save(any())).thenAnswer(call -> call.getArgument(0));
        when(mapper.toShipmentDTO(any())).thenReturn(new ShipmentResponseDTO());

        service.createShipment(new CreateShipmentRequestDTO("  " + REFERENCE + " ", " Sonae "));

        ArgumentCaptor<Shipment> saved = ArgumentCaptor.forClass(Shipment.class);
        verify(shipmentRepository).save(saved.capture());
        assertThat(saved.getValue().getReference()).isEqualTo(REFERENCE);
        assertThat(saved.getValue().getCustomer()).isEqualTo("Sonae");
    }

    @Test
    void createShipment_rejectsADuplicateReference() {
        when(shipmentRepository.findByReference(REFERENCE)).thenReturn(Optional.of(shipment()));

        assertThatThrownBy(() -> service.createShipment(new CreateShipmentRequestDTO(REFERENCE, "Sonae")))
                .isInstanceOf(DuplicateShipmentException.class);

        verify(shipmentRepository, never()).save(any());
    }

    @Test
    void recordIncome_storesTheAmountAgainstTheShipment() {
        stubDetail();

        service.recordIncome(REFERENCE,
                new RecordAmountRequestDTO("AGENT_INCOME", new BigDecimal("300.00"), "Agent share"));

        ArgumentCaptor<Income> saved = ArgumentCaptor.forClass(Income.class);
        verify(incomeRepository).save(saved.capture());
        assertThat(saved.getValue().getIncomeType()).isEqualTo(IncomeType.AGENT_INCOME);
        assertThat(saved.getValue().getAmount()).isEqualByComparingTo("300.00");
        assertThat(saved.getValue().getShipment().getReference()).isEqualTo(REFERENCE);
    }

    @Test
    void recordCost_acceptsTheTypeInAnyCase() {
        stubDetail();

        service.recordCost(REFERENCE,
                new RecordAmountRequestDTO("  handling  ", new BigDecimal("200.00"), null));

        ArgumentCaptor<Cost> saved = ArgumentCaptor.forClass(Cost.class);
        verify(costRepository).save(saved.capture());
        assertThat(saved.getValue().getCostType()).isEqualTo(CostType.HANDLING);
    }

    @Test
    void recordIncome_withAnUnknownTypeExplainsWhatIsAccepted() {
        when(shipmentRepository.findByReference(REFERENCE)).thenReturn(Optional.of(shipment()));

        assertThatThrownBy(() -> service.recordIncome(REFERENCE,
                new RecordAmountRequestDTO("DONATION", BigDecimal.ONE, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CUSTOMER_PAYMENT")
                .hasMessageContaining("AGENT_INCOME");

        verify(incomeRepository, never()).save(any());
    }

    @Test
    void recordCost_againstAnUnknownShipmentFails() {
        when(shipmentRepository.findByReference("SHP-NOPE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.recordCost("SHP-NOPE",
                new RecordAmountRequestDTO("HANDLING", BigDecimal.ONE, null)))
                .isInstanceOf(ShipmentNotFoundException.class);

        verify(costRepository, never()).save(any());
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

    private CorrectAmountRequestDTO correction() {
        return new CorrectAmountRequestDTO(new BigDecimal("4350.00"), "Re-invoiced", 0L);
    }

    @Test
    void correctIncome_withTheCurrentVersion_returnsTheUpdatedShipment() {
        stubDetail();
        when(incomeRepository.correctAmount(7L, 1L, new BigDecimal("4350.00"), "Re-invoiced", 0L)).thenReturn(1);

        assertThat(service.correctIncome(REFERENCE, 7L, correction()).getReference()).isEqualTo(REFERENCE);
    }

    /** The row exists on this shipment but the update matched nothing: someone changed it first. */
    @Test
    void correctIncome_whenTheVersionIsStale_reportsAConflict() {
        when(shipmentRepository.findByReference(REFERENCE)).thenReturn(Optional.of(shipment()));
        when(incomeRepository.correctAmount(any(), any(), any(), any(), any())).thenReturn(0);
        when(incomeRepository.existsByIdAndShipmentId(7L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.correctIncome(REFERENCE, 7L, correction()))
                .isInstanceOf(StaleAmountLineException.class);
    }

    @Test
    void correctCost_whenTheLineIsNotOnTheShipment_reportsNotFound() {
        when(shipmentRepository.findByReference(REFERENCE)).thenReturn(Optional.of(shipment()));
        when(costRepository.correctAmount(any(), any(), any(), any(), any())).thenReturn(0);
        when(costRepository.existsByIdAndShipmentId(7L, 1L)).thenReturn(false);

        assertThatThrownBy(() -> service.correctCost(REFERENCE, 7L, correction()))
                .isInstanceOf(AmountLineNotFoundException.class)
                .hasMessageContaining(REFERENCE);
    }
}
