package com.jgomes.logistics.controller;

import com.jgomes.logistics.dto.ApiResponse;
import com.jgomes.logistics.dto.ShipmentResponseDTO;
import com.jgomes.logistics.service.ShipmentAdministrationService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShipmentAdministrationControllerTest {

    @Mock
    private ShipmentAdministrationService service;

    @InjectMocks
    private ShipmentAdministrationController controller;

    @Test
    void search_returnsTheListToChooseFrom() {
        when(service.findShipments(null, 20)).thenReturn(List.of(
                new ShipmentResponseDTO(1L, "SHP-2026-0001", "Sonae")));

        ResponseEntity<ApiResponse<List<ShipmentResponseDTO>>> response = controller.search(null, 20);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertNotNull(response.getBody());
        assertThat(response.getBody().data()).hasSize(1);
        assertThat(response.getBody().data().get(0).getReference()).isEqualTo("SHP-2026-0001");
    }

    @Test
    void search_passesTheSearchTermAndLimitThrough() {
        when(service.findShipments("sonae", 5)).thenReturn(List.of());

        controller.search("sonae", 5);

        verify(service).findShipments("sonae", 5);
    }
}
