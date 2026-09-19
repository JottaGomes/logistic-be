package com.jgomes.logistics.controller;

import com.jgomes.logistics.dto.ApiResponse;
import com.jgomes.logistics.dto.CreateShipmentRequestDTO;
import com.jgomes.logistics.dto.RecordAmountRequestDTO;
import com.jgomes.logistics.dto.ShipmentDetailDTO;
import com.jgomes.logistics.dto.ShipmentResponseDTO;
import com.jgomes.logistics.service.ShipmentAdministrationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Customer payment and operational cost administration (requirements 1.4).
 *
 * Kept apart from {@link ProfitCalculationController}, which exposes the Calculate
 * Profit use case: this one only records the data that use case pre-supposes.
 */
@RestController
@RequestMapping("/api/shipments")
public class ShipmentAdministrationController {

    private final ShipmentAdministrationService service;

    public ShipmentAdministrationController(ShipmentAdministrationService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ShipmentResponseDTO>> create(
            @Valid @RequestBody CreateShipmentRequestDTO request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(service.createShipment(request)));
    }

    @GetMapping("/{reference}")
    public ResponseEntity<ApiResponse<ShipmentDetailDTO>> detail(@PathVariable String reference) {
        return ResponseEntity.ok(ApiResponse.ok(service.findDetail(reference)));
    }

    @PostMapping("/{reference}/incomes")
    public ResponseEntity<ApiResponse<ShipmentDetailDTO>> recordIncome(
            @PathVariable String reference,
            @Valid @RequestBody RecordAmountRequestDTO request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(service.recordIncome(reference, request)));
    }

    @PostMapping("/{reference}/costs")
    public ResponseEntity<ApiResponse<ShipmentDetailDTO>> recordCost(
            @PathVariable String reference,
            @Valid @RequestBody RecordAmountRequestDTO request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(service.recordCost(reference, request)));
    }
}
