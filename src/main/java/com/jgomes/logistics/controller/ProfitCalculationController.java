package com.jgomes.logistics.controller;

import com.jgomes.logistics.dto.ApiResponse;
import com.jgomes.logistics.dto.CalculateProfitRequestDTO;
import com.jgomes.logistics.dto.ProfitCalculationResponseDTO;
import com.jgomes.logistics.service.ProfitCalculationService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The single controller exposed for the Calculate Profit use case.
 *
 * A profit calculation is its own resource rather than a sub-resource of a
 * shipment: the listing spans every shipment, and each calculation is a stored,
 * point-in-time outcome. Posting to the collection performs the calculation and
 * creates one.
 */
@Slf4j
@RestController
@RequestMapping("/api/profit-calculations")
public class ProfitCalculationController {

    /**
     * Largest page of stored calculations one request may return. Matches the
     * shipment search ceiling in ShipmentAdministrationService, so no endpoint's
     * response size grows with the table.
     */
    private static final int MAX_PAGE_SIZE = 100;

    private final ProfitCalculationService service;

    public ProfitCalculationController(ProfitCalculationService service) {
        this.service = service;
    }

    /**
     * 201 when a new calculation was stored; 200 when nothing changed since the
     * shipment's latest calculation and that one is returned. Repeating the request
     * without changing any amount therefore never adds rows.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ProfitCalculationResponseDTO>> calculate(
            @Valid @RequestBody CalculateProfitRequestDTO request) {

        ProfitCalculationResponseDTO result = service.calculate(request);
        HttpStatus status = Boolean.TRUE.equals(result.getCreated()) ? HttpStatus.CREATED : HttpStatus.OK;

        return ResponseEntity.status(status).body(ApiResponse.ok(result));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProfitCalculationResponseDTO>>> calculations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "calculatedAt") String sort,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(required = false) String search) {

        Sort.Direction order = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;

        PageRequest pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(order, sortProperty(sort)));

        return ResponseEntity.ok(ApiResponse.ok(service.findAll(search, pageable)));
    }

    /**
     * Only sorts by columns that exist, so a stray query parameter cannot turn into
     * a 500 from the persistence layer.
     */
    private String sortProperty(String requested) {

        return switch (requested) {
            case "profitOrLoss", "totalIncome", "totalCosts" -> requested;
            case "shipmentReference" -> "shipment.reference";
            case "customer" -> "shipment.customer";
            default -> "calculatedAt";
        };
    }
}
