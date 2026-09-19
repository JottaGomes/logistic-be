package com.adrianojlt.logistics.controller;

import com.adrianojlt.logistics.dto.ApiResponse;
import com.adrianojlt.logistics.dto.CalculateProfitRequestDTO;
import com.adrianojlt.logistics.dto.ProfitCalculationResponseDTO;
import com.adrianojlt.logistics.dto.ShipmentResponseDTO;
import com.adrianojlt.logistics.service.ProfitCalculationService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfitCalculationControllerTest {

    @Mock
    private ProfitCalculationService service;

    @InjectMocks
    private ProfitCalculationController controller;

    @Test
    void calculate_returnsTheStoredResult() {
        ProfitCalculationResponseDTO dto = new ProfitCalculationResponseDTO();
        dto.setShipmentReference("SHP-2026-0001");
        dto.setProfitOrLoss(new BigDecimal("2100.00"));
        when(service.calculate(any())).thenReturn(dto);

        ResponseEntity<ApiResponse<ProfitCalculationResponseDTO>> response =
                controller.calculate(new CalculateProfitRequestDTO("SHP-2026-0001"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertNotNull(response.getBody());
        assertThat(response.getBody().success()).isTrue();
        assertThat(response.getBody().data().getProfitOrLoss()).isEqualByComparingTo("2100.00");
    }

    @Test
    void shipments_returnsTheListToChooseFrom() {
        when(service.findAllShipments()).thenReturn(List.of(
                new ShipmentResponseDTO(1L, "SHP-2026-0001", "Sonae")));

        ResponseEntity<ApiResponse<List<ShipmentResponseDTO>>> response = controller.shipments();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertNotNull(response.getBody());
        assertThat(response.getBody().data()).hasSize(1);
        assertThat(response.getBody().data().get(0).getReference()).isEqualTo("SHP-2026-0001");
    }

    @Test
    void calculations_requestsTheGivenPageSortedByMostRecent() {
        Page<ProfitCalculationResponseDTO> page = new PageImpl<>(List.of());
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        when(service.findAll(isNull(), pageable.capture())).thenReturn(page);

        controller.calculations(2, 25, "calculatedAt", "desc", null);

        assertThat(pageable.getValue().getPageNumber()).isEqualTo(2);
        assertThat(pageable.getValue().getPageSize()).isEqualTo(25);
        assertThat(pageable.getValue().getSort().getOrderFor("calculatedAt")).isNotNull();
        assertThat(pageable.getValue().getSort().getOrderFor("calculatedAt").isDescending()).isTrue();
    }

    @Test
    void calculations_sortsByTheRequestedColumnWhenItExists() {
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        when(service.findAll(isNull(), pageable.capture())).thenReturn(new PageImpl<>(List.of()));

        controller.calculations(0, 10, "profitOrLoss", "asc", null);

        Sort.Order order = pageable.getValue().getSort().getOrderFor("profitOrLoss");
        assertThat(order).isNotNull();
        assertThat(order.isAscending()).isTrue();
    }

    @Test
    void calculations_fallsBackToTheDateWhenTheColumnIsUnknown() {
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        when(service.findAll(isNull(), pageable.capture())).thenReturn(new PageImpl<>(List.of()));

        controller.calculations(0, 10, "; DROP TABLE shipment", "desc", null);

        assertThat(pageable.getValue().getSort().getOrderFor("calculatedAt")).isNotNull();
    }

    @Test
    void calculations_passesTheSearchTermThrough() {
        when(service.findAll(eq("sonae"), any())).thenReturn(new PageImpl<>(List.of()));

        controller.calculations(0, 10, "calculatedAt", "desc", "sonae");

        verify(service).findAll(eq("sonae"), any());
    }
}
