package com.adrianojlt.logistics.controller;

import com.adrianojlt.logistics.dto.ApiResponse;
import com.adrianojlt.logistics.dto.CalculateProfitRequestDTO;
import com.adrianojlt.logistics.dto.ProfitCalculationResponseDTO;
import com.adrianojlt.logistics.dto.ShipmentResponseDTO;
import com.adrianojlt.logistics.service.ProfitCalculationService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** The single controller exposed for the Calculate Profit use case. */
@Slf4j
@RestController
@RequestMapping("/api/shipments")
public class ProfitCalculationController {

    private final ProfitCalculationService service;

    public ProfitCalculationController(ProfitCalculationService service) {
        this.service = service;
    }

    /**
     * The shipments the Finance Department can ask to evaluate: a bounded slice,
     * narrowed by {@code search}, so the response size does not grow with the table.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ShipmentResponseDTO>>> shipments(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "20") int limit) {

        return ResponseEntity.ok(ApiResponse.ok(service.findShipments(search, limit)));
    }

    @PostMapping("/calculate")
    public ResponseEntity<ApiResponse<ProfitCalculationResponseDTO>> calculate(
            @Valid @RequestBody CalculateProfitRequestDTO request) {
        return ResponseEntity.ok(ApiResponse.ok(service.calculate(request)));
    }

    @GetMapping("/calculations")
    public ResponseEntity<ApiResponse<Page<ProfitCalculationResponseDTO>>> calculations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "calculatedAt") String sort,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(required = false) String search) {

        Sort.Direction order = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;

        PageRequest pageable = PageRequest.of(page, size, Sort.by(order, sortProperty(sort)));

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
