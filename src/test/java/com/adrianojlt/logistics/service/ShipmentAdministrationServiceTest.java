package com.adrianojlt.logistics.service;

import com.adrianojlt.logistics.dto.CreateShipmentRequestDTO;
import com.adrianojlt.logistics.dto.RecordAmountRequestDTO;
import com.adrianojlt.logistics.dto.ShipmentResponseDTO;
import com.adrianojlt.logistics.entity.Cost;
import com.adrianojlt.logistics.entity.CostType;
import com.adrianojlt.logistics.entity.Income;
import com.adrianojlt.logistics.entity.IncomeType;
import com.adrianojlt.logistics.entity.Shipment;
import com.adrianojlt.logistics.exception.DuplicateShipmentException;
import com.adrianojlt.logistics.exception.ShipmentNotFoundException;
import com.adrianojlt.logistics.mapper.ProfitCalculationMapper;
import com.adrianojlt.logistics.repository.CostRepository;
import com.adrianojlt.logistics.repository.IncomeRepository;
import com.adrianojlt.logistics.repository.ShipmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
}
